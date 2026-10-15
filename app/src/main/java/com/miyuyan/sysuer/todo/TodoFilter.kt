package com.miyuyan.sysuer.todo

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * 待办列表的时间范围。
 *
 * [All] 表示不按日期过滤，此时 [TodoFilter.useDue] / [TodoFilter.useDdl] 也不参与
 * 查询构造（UI 层的「预定 / 截止」开关同时置灰）。
 *
 * [SelectedRange] 的区间由日历框选得出，单日是它的退化形式（起止相同）。
 */
enum class TodoDateRange {
	All,
	Today,
	ThisWeek,
	ThisMonth,
	SelectedRange,
}

/**
 * 「未分类」哨兵值。
 *
 * 科目 / 类型是自由文本，空串本身就是合法取值，不能拿来当「未分类」的标记，
 * 因此用一个不会出现在真实数据里的字符串占位，由 [buildTodoWhere] 翻译成
 * `IS NULL OR = ''`。
 */
const val TODO_UNCLASSIFIED: String = "\u0000unclassified"

/**
 * 待办页的完整筛选条件。
 *
 * 默认值为「全部」：不限日期、不限状态，列出所有待办。
 */
data class TodoFilter(
		val range: TodoDateRange = TodoDateRange.All,
		/** yyyy-MM-dd，仅 [TodoDateRange.SelectedRange] 时生效，区间起点 */
		val selectedDate: String = "",
		/** yyyy-MM-dd，区间终点；置空表示与起点相同（单日） */
		val selectedEndDate: String = "",
		/** 按预定日期（due_date）过滤 */
		val useDue: Boolean = true,
		/** 按截止日期（ddl）过滤 */
		val useDdl: Boolean = false,
		/** 包含未完成（status = 0） */
		val showTodo: Boolean = true,
		/** 包含已完成（status = 1） */
		val showDone: Boolean = true,
		/** 标题 / 描述关键词 */
		val keyword: String = "",
		val subjects: Set<String> = emptySet(),
		val types: Set<String> = emptySet(),
		val tags: Set<String> = emptySet(),
		/** 优先级 0..4 */
		val priorities: Set<Int> = emptySet(),
) {
	/** 是否处于「未做任何筛选」的默认态，决定空态文案与「清除全部」是否显示 */
	val isDefault: Boolean
		get() = (range == TodoDateRange.All) && useDue && !useDdl && showTodo && showDone &&
				(keyword.isEmpty() && subjects.isEmpty() && types.isEmpty() &&
						tags.isEmpty() && priorities.isEmpty())

}

private val isoDate: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

/**
 * 由筛选条件构造 SQL WHERE 子句与绑定参数。
 *
 * 纯函数，不依赖 Android，[today] 由调用方注入以便测试。无任何约束时返回
 * `("1 = 1", emptyList())`。
 *
 * 日期列均为 `yyyy-MM-dd` 定宽 ISO 格式，字符串比较即等价于时间先后比较，
 * 因此范围过滤直接用 `=` / `BETWEEN`，无需 `strftime`。
 */
fun buildTodoWhere(f: TodoFilter, today: LocalDate): Pair<String, List<String>> {
	val conditions = mutableListOf<String>()
	val args = mutableListOf<String>()

	// 1. 日期：范围 x 日期列
	// 单天范围返回 start == end，上层退化成 `col = ?` 而不是 BETWEEN
	val bounds = resolveDateBounds(f, today)
	val rangePair: Pair<String, String>? =
		bounds?.let { it.first.format(isoDate) to it.second.format(isoDate) }
	if (rangePair != null) {
		val columns = buildList {
			if (f.useDue) add("due_date")
			if (f.useDdl) add("ddl")
		}
		if (columns.isNotEmpty()) {
			val perColumn = columns.map { col ->
				if (rangePair.first == rangePair.second) {
					args.add(rangePair.first)
					"$col = ?"
				} else {
					args.add(rangePair.first)
					args.add(rangePair.second)
					"$col BETWEEN ? AND ?"
				}
			}
			conditions.add(
					if (perColumn.size == 1) perColumn.first()
					else "(${perColumn.joinToString(" OR ")})"
			)
		}
	}

	// 2. 状态
	buildStatusClause(f, args)?.let { conditions.add(it) }

	// 3. 关键词
	if (f.keyword.isNotBlank()) {
		val escaped = escapeLike(f.keyword.trim())
		args.add("%$escaped%")
		args.add("%$escaped%")
		conditions.add(
				"(title LIKE ? ESCAPE '\\' OR description LIKE ? ESCAPE '\\')"
		)
	}

	// 4. 科目 / 类型（含「未分类」哨兵）
	buildSetClause("subject", f.subjects, args)?.let { conditions.add(it) }
	buildSetClause("todo_type", f.types, args)?.let { conditions.add(it) }

	// 5. 标签：tag 列存的是 JSON 数组文本 ["a","b"]，多值取 OR 语义
	if (f.tags.isNotEmpty()) {
		val parts = f.tags.sorted().map { tag ->
			args.add("%\"${escapeLike(tag)}\"%")
			"tag LIKE ? ESCAPE '\\'"
		}
		conditions.add(
				if (parts.size == 1) parts.first() else "(${parts.joinToString(" OR ")})"
		)
	}

	// 6. 优先级
	if (f.priorities.isNotEmpty()) {
		val sorted = f.priorities.sorted()
		sorted.forEach { args.add(it.toString()) }
		conditions.add("priority IN (${sorted.joinToString(", ") { "?" }})")
	}

	return conditions.joinToString(" AND ").ifEmpty { "1 = 1" } to args
}

/**
 * 把时间范围解析成具体的起止日期（含端点），[TodoDateRange.All] 返回 null。
 *
 * 日历高亮与 SQL 谓词都从这里取区间，两者因此不可能不一致。
 * [TodoDateRange.SelectedRange] 在起止缺失或非法时退化为今天，在起止颠倒时自动对调。
 */
fun resolveDateBounds(f: TodoFilter, today: LocalDate): Pair<LocalDate, LocalDate>? =
	when (f.range) {
		TodoDateRange.All -> null
		TodoDateRange.Today -> today to today
		TodoDateRange.ThisWeek -> {
			val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
			monday to monday.plusDays(6)
		}

		TodoDateRange.ThisMonth ->
			today.with(TemporalAdjusters.firstDayOfMonth()) to
					today.with(TemporalAdjusters.lastDayOfMonth())

		TodoDateRange.SelectedRange -> {
			// 起点解析不出来说明整个选择都是脏数据，连终点一起丢掉退化为今天；
			// 否则会出现「今天 ~ 某个终点」这种用户从未选过的区间
			val start = parseIsoDate(f.selectedDate) ?: return today to today
			val end = parseIsoDate(f.selectedEndDate) ?: start
			if (end.isBefore(start)) end to start else start to end
		}
	}

/** 宽松解析 yyyy-MM-dd，非法或空返回 null */
fun parseIsoDate(raw: String?): LocalDate? =
	raw?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it, isoDate) }.getOrNull() }

/**
 * 状态子句，需要时顺带把参数写进 [args]。
 *
 * 两个开关都开 = 不约束（返回 null）；都关 = 恒假条件，与旧实现「什么都不选则列表为空」
 * 的行为一致。
 */
private fun buildStatusClause(f: TodoFilter, args: MutableList<String>): String? = when {
	f.showTodo && f.showDone -> null
	f.showTodo -> {
		args.add(TodoInfo.TODO.toString())
		"status = ?"
	}

	f.showDone -> {
		args.add(TodoInfo.DONE.toString())
		"status = ?"
	}

	else -> "1 = 0"
}

/**
 * 把「集合 + 可选未分类」翻译成 `col IN (?,?) OR col IS NULL OR col = ''`。
 *
 * 集合同时为空返回 null（不产生约束），避免拼出非法的 `IN ()`。
 * 真实值按 [sorted] 后绑定，保证同一筛选条件多次查询的 SQL 文本稳定。
 */
private fun buildSetClause(
	column: String,
	values: Set<String>,
	args: MutableList<String>,
): String? {
	val real = values.filter { it != TODO_UNCLASSIFIED }.sorted()
	val includeEmpty = values.contains(TODO_UNCLASSIFIED)
	if (real.isEmpty() && !includeEmpty) return null
	val parts = mutableListOf<String>()
	if (real.isNotEmpty()) {
		args.addAll(real)
		parts += "$column IN (${real.joinToString(", ") { "?" }})"
	}
	if (includeEmpty) parts += "($column IS NULL OR $column = '')"
	return if (parts.size == 1) parts.first() else "(${parts.joinToString(" OR ")})"
}

/**
 * LIKE 通配符转义。
 *
 * 关键词与标签名都是用户输入，`%` `_` 若不转义会被当成通配符；`\` 是转义符本身，
 * 必须先转义，否则会破坏后续两个替换。
 */
private fun escapeLike(raw: String): String =
	raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
