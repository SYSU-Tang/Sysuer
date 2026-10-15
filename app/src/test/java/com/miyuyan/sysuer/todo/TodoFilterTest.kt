package com.miyuyan.sysuer.todo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * [buildTodoWhere] 的纯逻辑测试。
 *
 * 固定 [TODAY] 为 2026-03-05（周四），使周 / 月边界可预测。
 */
class TodoFilterTest {

	private val today: LocalDate = LocalDate.of(2026, 3, 5)

	private fun where(f: TodoFilter): String = buildTodoWhere(f, today).first

	private fun args(f: TodoFilter): List<String> = buildTodoWhere(f, today).second

	@Test
	fun `default filter constrains nothing`() {
		val f = TodoFilter()
		assertEquals("1 = 1", where(f))
		assertTrue(args(f).isEmpty())
		assertTrue(f.isDefault)
	}

	@Test
	fun `today uses equality on due_date only`() {
		val f = TodoFilter(range = TodoDateRange.Today)
		assertEquals("due_date = ?", where(f))
		assertEquals(listOf("2026-03-05"), args(f))
	}

	@Test
	fun `selected range falls back to today when unset`() {
		assertEquals(listOf("2026-03-05"), args(TodoFilter(range = TodoDateRange.SelectedRange)))
		val set = TodoFilter(range = TodoDateRange.SelectedRange, selectedDate = "2024-12-31")
		assertEquals(listOf("2024-12-31"), args(set))
	}

	@Test
	fun `selected range with only start is a single day`() {
		// 日历 Range 模式下「点了起点、终点待定」的中间态必须仍能按单日查询
		val f = TodoFilter(range = TodoDateRange.SelectedRange, selectedDate = "2026-03-07")
		assertEquals("due_date = ?", where(f))
		assertEquals(listOf("2026-03-07"), args(f))
	}

	@Test
	fun `selected range with both endpoints uses between`() {
		val f = TodoFilter(
				range = TodoDateRange.SelectedRange,
				selectedDate = "2026-03-07",
				selectedEndDate = "2026-03-12",
		)
		assertEquals("due_date BETWEEN ? AND ?", where(f))
		assertEquals(listOf("2026-03-07", "2026-03-12"), args(f))
	}

	@Test
	fun `reversed range endpoints are swapped`() {
		// 日历允许先点后面那天，这里自对上顺序，不能拼出空区间
		val f = TodoFilter(
				range = TodoDateRange.SelectedRange,
				selectedDate = "2026-03-12",
				selectedEndDate = "2026-03-07",
		)
		assertEquals(listOf("2026-03-07", "2026-03-12"), args(f))
	}

	@Test
	fun `malformed range date is ignored`() {
		val f = TodoFilter(
				range = TodoDateRange.SelectedRange,
				selectedDate = "not-a-date",
				selectedEndDate = "2026-03-12",
		)
		// 起点非法时退化到今天，终点也丢掉，变成单日查询
		assertEquals(listOf("2026-03-05"), args(f))
	}

	@Test
	fun `selected day does not leak into today`() {
		// 先点日历选了别的日子，再切回「今天」，谓词必须是今天而不是残留的选中日
		val f = TodoFilter(
				range = TodoDateRange.Today,
				selectedDate = "2024-12-31",
		)
		assertEquals(listOf("2026-03-05"), args(f))
	}

	@Test
	fun `this week spans monday to sunday`() {
		// 2026-03-05 是周四，本周一为 03-02，周日为 03-08
		val f = TodoFilter(range = TodoDateRange.ThisWeek)
		assertEquals("due_date BETWEEN ? AND ?", where(f))
		assertEquals(listOf("2026-03-02", "2026-03-08"), args(f))
	}

	@Test
	fun `this week crosses year boundary`() {
		// 2026-01-01 是周四，本周一为 2025-12-29
		val week = buildTodoWhere(TodoFilter(range = TodoDateRange.ThisWeek), LocalDate.of(2026, 1, 1)).second
		assertEquals(listOf("2025-12-29", "2026-01-04"), week)
	}

	@Test
	fun `this month handles leap february`() {
		val f = buildTodoWhere(
				TodoFilter(range = TodoDateRange.ThisMonth),
				LocalDate.of(2024, 2, 15)
		).second
		assertEquals(listOf("2024-02-01", "2024-02-29"), f)
	}

	@Test
	fun `both date columns are or-ed with correct arg order`() {
		val f = TodoFilter(
				range = TodoDateRange.ThisWeek,
				useDue = true,
				useDdl = true,
		)
		assertEquals("(due_date BETWEEN ? AND ? OR ddl BETWEEN ? AND ?)", where(f))
		assertEquals(listOf("2026-03-02", "2026-03-08", "2026-03-02", "2026-03-08"), args(f))
	}

	@Test
	fun `all range ignores date columns entirely`() {
		// 范围为「全部」时 useDue / useDdl 不参与构造，不会拼出无意义的日期谓词
		val f = TodoFilter(range = TodoDateRange.All, useDue = true, useDdl = true)
		assertEquals("1 = 1", where(f))
	}

	@Test
	fun `status single selection binds the right value`() {
		// 不引用 TodoInfo.TODO / DONE：那个类带 MutableLiveData 字段，
		// 纯 JVM 测试里没必要依赖 Android 类型
		val onlyTodo = TodoFilter(showDone = false)
		assertEquals("status = ?", where(onlyTodo))
		assertEquals(listOf("0"), args(onlyTodo))

		val onlyDone = TodoFilter(showTodo = false)
		assertEquals(listOf("1"), args(onlyDone))
	}

	@Test
	fun `status both off yields empty set`() {
		val f = TodoFilter(showTodo = false, showDone = false)
		assertEquals("1 = 0", where(f))
		assertTrue(args(f).isEmpty())
	}

	@Test
	fun `keyword matches title and description`() {
		val f = TodoFilter(keyword = "论文")
		assertEquals(
				"(title LIKE ? ESCAPE '\\' OR description LIKE ? ESCAPE '\\')",
				where(f)
		)
		assertEquals(listOf("%论文%", "%论文%"), args(f))
	}

	@Test
	fun `keyword wildcards are escaped`() {
		val f = TodoFilter(keyword = "100%_done")
		assertEquals(listOf("%100\\%\\_done%", "%100\\%\\_done%"), args(f))
	}

	@Test
	fun `blank keyword produces no clause`() {
		assertEquals("1 = 1", where(TodoFilter(keyword = "   ")))
	}

	@Test
	fun `subjects become a sorted in list`() {
		// 绑定值按字典序排列，保证同一筛选条件重复查询时 SQL 文本稳定
		val f = TodoFilter(subjects = setOf("高等数学", "大学英语"))
		assertEquals("subject IN (?, ?)", where(f))
		assertEquals(listOf("大学英语", "高等数学"), args(f))
	}

	@Test
	fun `unclassified sentinel becomes null or empty`() {
		val f = TodoFilter(subjects = setOf(TODO_UNCLASSIFIED))
		assertEquals("(subject IS NULL OR subject = '')", where(f))
		assertTrue(args(f).isEmpty())
	}

	@Test
	fun `unclassified combined with real values is or-ed`() {
		val f = TodoFilter(types = setOf("作业", TODO_UNCLASSIFIED))
		assertEquals("(todo_type IN (?) OR (todo_type IS NULL OR todo_type = ''))", where(f))
		assertEquals(listOf("作业"), args(f))
	}

	@Test
	fun `empty sets never produce illegal IN clause`() {
		val f = TodoFilter(subjects = emptySet(), types = emptySet(), tags = emptySet(), priorities = emptySet())
		assertFalse(where(f).contains("IN ()"))
		assertEquals("1 = 1", where(f))
	}

	@Test
	fun `tags are json quoted and or-ed`() {
		val f = TodoFilter(tags = setOf("急", "重要"))
		assertEquals("(tag LIKE ? ESCAPE '\\' OR tag LIKE ? ESCAPE '\\')", where(f))
		assertEquals(listOf("%\"急\"%", "%\"重要\"%"), args(f))
	}

	@Test
	fun `priorities are sorted and bound as strings`() {
		val f = TodoFilter(priorities = setOf(4, 1))
		assertEquals("priority IN (?, ?)", where(f))
		assertEquals(listOf("1", "4"), args(f))
	}

	@Test
	fun `all clauses are and-ed in stable order`() {
		val f = TodoFilter(
				range = TodoDateRange.ThisMonth,
				useDue = true,
				useDdl = false,
				showTodo = true,
				showDone = false,
				keyword = "期中",
				subjects = setOf("高等数学"),
				tags = setOf("考试"),
				priorities = setOf(4),
		)
		assertEquals(
				listOf(
						"due_date BETWEEN ? AND ?",
						"status = ?",
						"(title LIKE ? ESCAPE '\\' OR description LIKE ? ESCAPE '\\')",
						"subject IN (?)",
						"tag LIKE ? ESCAPE '\\'",
						"priority IN (?)",
				).joinToString(" AND "),
				where(f)
		)
		assertEquals(
				listOf("2026-03-01", "2026-03-31", "0", "%期中%", "%期中%", "高等数学", "%\"考试\"%", "4"),
				args(f)
		)
	}

	@Test
	fun `placeholder count matches arg count for every combination`() {
		// 兜底：任何组合下 `?` 的数量都必须与绑定参数数量一致，否则 Room 会绑定越界
		for (range in TodoDateRange.entries) {
			for (due in booleanArrayOf(true, false)) {
				for (ddl in booleanArrayOf(true, false)) {
					for (todo in booleanArrayOf(true, false)) {
						for (done in booleanArrayOf(true, false)) {
							val f = TodoFilter(
									range = range,
									selectedDate = "2026-03-07",
									selectedEndDate = "2026-03-09",
									useDue = due,
									useDdl = ddl,
									showTodo = todo,
									showDone = done,
									keyword = "k",
									subjects = setOf("s", TODO_UNCLASSIFIED),
									types = setOf("t"),
									tags = setOf("g1", "g2"),
									priorities = setOf(0, 3),
							)
							val (where, args) = buildTodoWhere(f, today)
							assertEquals(
									"placeholder mismatch for $f",
									where.count { it == '?' },
									args.size
							)
						}
					}
				}
			}
		}
	}
}
