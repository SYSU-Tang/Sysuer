package com.miyuyan.sysuer.academic

import android.app.Application
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.model.PjxtModel
import com.miyuyan.sysuer.view.UiState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 教学评价（评教）的状态持有者。
 *
 * 原实现由 EvaluationCategoryFragment / EvaluationCourseFragment /
 * EvaluationQuestionnaireFragment 各自新建 [PjxtModel] 并各占用请求码，问卷页还额外用
 * ConcatAdapter + 三个自定义 Adapter 承载作答状态；迁移到 Compose 后统一收敛到这里：
 * 请求码集中定义，三级数据与作答状态以 Snapshot / StateFlow 暴露给 UI。
 *
 * 提交报文（pjidlist / pjjglist / pjzt 及其内层 pjxxlist）与旧实现逐字段保持一致，
 * 作答状态直接写回原始 JSONArray，避免拼装环节产生差异。
 */
class EvaluationViewModel(application: Application) : AndroidViewModel(application) {

	private val model = PjxtModel(application)

	private object Code {
		const val TASKS = 0
		const val COURSES = 1
		const val QUESTIONNAIRE = 2
		const val SAVE = 3
		const val SUBMIT = 4
	}

	// ============================================================================================
	// 三级数据
	// ============================================================================================

	/** 第一级：评价任务 */
	val tasks = mutableStateListOf<JSONObject>()

	/** 第二级：任务下的被评课程 */
	val courses = mutableStateListOf<JSONObject>()

	/** 第三级：课程问卷（按被评人分组的题目） */
	val subjects = mutableStateListOf<EvalSubject>()

	private val _taskState = model.getUiState(Code.TASKS)
	val taskState: StateFlow<UiState> = _taskState.asStateFlow()

	private val _courseState = model.getUiState(Code.COURSES)
	val courseState: StateFlow<UiState> = _courseState.asStateFlow()

	private val _questionnaireState = model.getUiState(Code.QUESTIONNAIRE)
	val questionnaireState: StateFlow<UiState> = _questionnaireState.asStateFlow()

	// ============================================================================================
	// 分页
	// ============================================================================================

	private var taskPage = 0
	private var taskTotal = -1
	private var taskLastCount = 0

	private var coursePage = 0
	private var courseTotal = -1
	private var courseLastCount = 0

	/** 当前任务的定位参数，用于第二级课程列表 */
	private var rwid = ""
	private var firstwjid: String? = null
	private var pjrdm: String? = null

	/** 当前问卷的请求 URL，重试时原样复用 */
	private var questionnaireUrl: String? = null

	/**
	 * 是否还有下一页。
	 *
	 * 优先用服务端的 total；缺失时退化为「本页返回条数不足一页即结束」，
	 * 这样返回空页时也能终止，避免反复触底拉同一页。
	 */
	private fun hasMore(page: Int, pageSize: Int, total: Int, lastCount: Int): Boolean =
		if (total >= 0) page * pageSize < total else lastCount >= pageSize

	val hasMoreTasks: Boolean
		get() = hasMore(taskPage, TASK_PAGE_SIZE, taskTotal, taskLastCount)

	val hasMoreCourses: Boolean
		get() = hasMore(coursePage, COURSE_PAGE_SIZE, courseTotal, courseLastCount)

	init {
		viewModelScope.launch {
			model.message.collect { (code, response) ->
				if (response.getIntValue("code", -1) != 200) {
					model.getUiState(code).value = UiState.Error
					return@collect
				}
				when (code) {
					Code.TASKS -> {
						val result = response.getJSONObject("result") ?: return@collect
						if (taskPage == 1) {
							taskTotal = result.getIntValue("total", -1)
							tasks.clear()
						}
						val page = result.getJSONArray("list")
							?.filterIsInstance<JSONObject>() ?: emptyList()
						taskLastCount = page.size
						page.forEach { tasks.add(it) }
						_taskState.value =
							if (tasks.isEmpty()) UiState.Empty else UiState.Content
					}

					Code.COURSES -> {
						val result = response.getJSONObject("result") ?: return@collect
						if (coursePage == 1) {
							courseTotal = result.getIntValue("total", -1)
							courses.clear()
						}
						val page = result.getJSONArray("list")
							?.filterIsInstance<JSONObject>() ?: emptyList()
						courseLastCount = page.size
						page.forEach { courses.add(it) }
						_courseState.value =
							if (courses.isEmpty()) UiState.Empty else UiState.Content
					}

					Code.QUESTIONNAIRE ->
						parseQuestionnaire(response.getJSONObject("result"))

					Code.SAVE -> model.contextUtil.toast(
							R.string.save_successful
					)

					Code.SUBMIT -> model.contextUtil.toast(
							R.string.submit_successful
					)
				}
			}
		}
	}

	// ============================================================================================
	// 取数
	// ============================================================================================

	/** 加载第一页任务；进入评教首页时调用 */
	fun loadTasks() {
		if (_taskState.value == UiState.Loading) return
		taskPage = 0
		taskTotal = -1
		_taskState.value = UiState.Loading
		fetchTasks(++taskPage)
	}

	/** 任务列表滚动到底时加载下一页 */
	fun loadMoreTasks() {
		if (!hasMoreTasks || _taskState.value != UiState.Content) return
		_taskState.value = UiState.LoadMore
		fetchTasks(++taskPage)
	}

	/** 重试任务列表：尚无数据则从头加载，否则重新请求已失败的那一页 */
	fun retryTasks() {
		if (tasks.isEmpty()) loadTasks() else fetchTasks(taskPage)
	}

	private fun fetchTasks(page: Int) {
		model.enqueue(
				"personnelEvaluation/listObtainPersonnelEvaluationTasks?pageNum=$page&pageSize=$TASK_PAGE_SIZE",
				Code.TASKS
		)
	}

	/** 选定任务，加载其课程列表（第一页） */
	fun loadCourses(task: JSONObject) {
		rwid = task.getString("rwid", "")
		firstwjid = task.getString("firstwjid")
		pjrdm = task.getString("pjrdm")
		coursePage = 1
		courseTotal = -1
		courses.clear()
		// 旧实现同样先判定参数齐备才发请求，避免拼出 pjrdm=null 的无效 URL
		if (rwid.isEmpty() || firstwjid == null || pjrdm == null) {
			_courseState.value = UiState.Empty
			return
		}
		_courseState.value = UiState.Loading
		fetchCourses(coursePage)
	}

	/** 课程列表滚动到底时加载下一页 */
	fun loadMoreCourses() {
		if (!hasMoreCourses || _courseState.value != UiState.Content) return
		_courseState.value = UiState.LoadMore
		fetchCourses(++coursePage)
	}

	private fun fetchCourses(page: Int) {
		model.enqueue(
				"personnelEvaluation/listEcaluationRalationshipEnriry?pjrdm=$pjrdm&wjid=$firstwjid&rwid=$rwid&pageNum=$page&pageSize=$COURSE_PAGE_SIZE",
				Code.COURSES
		)
	}

	/**
	 * 选定课程并加载其问卷。
	 *
	 * 参数取自课程对象的字段，与旧 Fragment 的 arguments 一致；缺省字段沿用旧的
	 * "null" 字面量拼接，保证请求 URL 与迁移前完全相同。
	 */
	fun openQuestionnaire(course: JSONObject) {
		subjects.clear()
		_questionnaireState.value = UiState.Loading
		val pjzt = if (course.getString("lsjgzt") == "2") "1" else ""
		questionnaireUrl = "evaluationPattern/getQuestionnaireTopic?rwid=$rwid" +
				"&wjid=${course.getString("wjid")}" +
				"&sxz=${course.getString("sxz")}" +
				"&pjrdm=$pjrdm" +
				"&bpdm=${course.getString("bpdm")}" +
				"&kcdm=${course.getString("kcdm")}" +
				"&rwh=${course.getString("rwh")?.encodeNonAscii()}" +
				"&pjzt=$pjzt" +
				"&bpmc=${course.getString("bpmc")}"
		model.enqueue(questionnaireUrl!!, Code.QUESTIONNAIRE)
	}

	/** 重试课程列表：尚无数据则从头加载，否则重新请求已失败的那一页 */
	fun retryCourses() {
		if (courses.isEmpty()) fetchCourses(1) else fetchCourses(coursePage)
	}

	/** 重试当前问卷 */
	fun retryQuestionnaire() {
		val url = questionnaireUrl ?: return
		_questionnaireState.value = UiState.Loading
		model.enqueue(url, Code.QUESTIONNAIRE)
	}

	// ============================================================================================
	// 问卷解析与作答
	// ============================================================================================

	/** 作答报文，结构同旧实现：{"pjidlist":[],"pjjglist":[],"pjzt":"2"} */
	private val answers = JSONObject.parseObject(
			"{\"pjidlist\":[],\"pjjglist\":[],\"pjzt\": \"2\"}"
	)

	private fun parseQuestionnaire(result: JSONObject?) {
		subjects.clear()
		answers.getJSONArray("pjjglist").clear()
		result?.getJSONArray("assessedObjList")?.forEach outer@{ assessedObj ->
			val assessed = assessedObj as? JSONObject ?: return@outer
			assessed.getJSONArray("bpdxList")?.forEach { obj ->
				val bpdx = obj as? JSONObject ?: return@forEach
				val pjjg = bpdx.clone().apply {
					remove("dtjgList")
					put("pjxxlist", JSONArray())
				}
				answers.getJSONArray("pjjglist").add(pjjg)
				val questions = mutableListOf<EvalQuestion>()
				bpdx.getJSONArray("dtjgList")?.forEach { topic ->
					val question = topic as? JSONObject ?: return@forEach
					val pjxxlist = JSONObject.parseObject(
							"{\"sjly\": \"1\"," +
									"\"stlx\": \"${question.getString("tmlx", "1")}\"," +
									"\"wjid\": \"${bpdx.getString("wjid")}\"," +
									"\"wjssrwid\": \"${bpdx.getString("wjssrwid")}\"," +
									"\"wjstctid\": \"\"," +
									"\"wjstid\": \"${question.getString("tmid")}\"," +
									"\"xxdalist\": []}"
					)
					val tmxxda = question.getJSONArray("tmxxda") ?: JSONArray()
					pjxxlist["xxdalist"] = tmxxda
					pjjg.getJSONArray("pjxxlist").add(pjxxlist)
					val options = question.getJSONArray("tmxxlist")
							?.filterIsInstance<JSONObject>() ?: emptyList()
					when (question.getString("tmlx")) {
						// 1 选项题：一个题目、多个候选项
						"1" -> questions.add(
								EvalQuestion(
										title = question.getString("tgmc"),
										type = QuestionType.SingleChoice,
										options = options,
										answer = tmxxda,
								)
						)

						// 5 打分题：0-100 滑动
						"5" -> questions.add(
								EvalQuestion(
										title = question.getString("tgmc"),
										type = QuestionType.Rank,
										answer = tmxxda,
								)
						)

						// 6 文本题：每个候选项一个输入框。与旧实现一致，
						// pjxxlist 的 wjstctid / wjstid / xxdalist 会被最后一项覆盖
						"6" -> options.forEach { tmxx ->
							val xxda = tmxx.getJSONArray("xxda") ?: JSONArray()
							pjxxlist["wjstctid"] = tmxx.getString("tmxxid")
							pjxxlist["wjstid"] = tmxx.getString("tmid")
							pjxxlist["xxdalist"] = xxda
							questions.add(
									EvalQuestion(
											title = question.getString("tgmc"),
											type = QuestionType.Text,
											answer = xxda,
									)
							)
						}
					}
				}
				// 与旧实现一致：bprmc 为空时只是不显示分组标题（旧版不 addAdapter TitleAdapter）,
				// 题目仍然正常渲染，否则学生会出现「有题但看不见」而无法作答
				subjects.add(EvalSubject(bpdx.getString("bprmc").orEmpty(), questions))
			}
		}
		_questionnaireState.value =
			if (subjects.isEmpty()) UiState.Empty else UiState.Content
	}

	/** 保存（pjzt=2），可反复修改 */
	fun save() = postEvaluation("2", Code.SAVE)

	/** 提交（pjzt=1），不可更改 */
	fun submit() = postEvaluation("1", Code.SUBMIT)

	private fun postEvaluation(pjzt: String, code: Int) {
		answers["pjzt"] = pjzt
		model.enqueue("evaluationPattern/submitSaveEvaluation", "$answers", code)
	}

	/** 重置全部作答 */
	fun reset() = subjects.forEach { it.questions.forEach { q -> q.clearAnswer() } }

	/**
	 * 自动填充：选项题选最后一项，打分题打满 100。
	 *
	 * 旧实现对已选末项的题目会因 toggle 语义反向取消，此处统一为「填充为末项」，
	 * 更符合「一键填充」的预期。
	 */
	fun autoFill() = subjects.forEach { it.questions.forEach { q -> q.autoFill() } }

	override fun onCleared() {
		model.dispose()
	}

	companion object {
		private const val TASK_PAGE_SIZE = 10
		private const val COURSE_PAGE_SIZE = 20

		/** 非 ASCII 字符转百分号编码，沿用旧实现的查询串拼装方式 */
		private fun String.encodeNonAscii(): String = buildString {
			this@encodeNonAscii.forEach {
				if (it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9') append(it)
				else it.toString().toByteArray(Charsets.UTF_8).forEach { byte ->
					append("%").append(byte.toUByte().toString(16).uppercase().padStart(2, '0'))
				}
			}
		}
	}
}

/** 题型，对应接口字段 tmlx */
enum class QuestionType { SingleChoice, Rank, Text }

/** 一道题目；作答状态为 Compose 可观察字段，写入时同步回原始 [answer] JSONArray */
class EvalQuestion(
	val title: String?,
	val type: QuestionType,
	val options: List<JSONObject> = emptyList(),
	private val answer: JSONArray,
) {
	/** 选项题选中下标，-1 表示未作答 */
	val selectedIndex: MutableIntState = mutableIntStateOf(
			when (type) {
				QuestionType.SingleChoice -> options.indexOfFirst {
					it.getString("tmxxid") == currentOption
				}

				else -> -1
			}
	)

	/** 打分题分值 */
	val rank: MutableIntState = mutableIntStateOf(
			if (type == QuestionType.Rank) currentRankValue else DEFAULT_RANK
	)

	/** 文本题内容 */
	val text: MutableState<String> = mutableStateOf(
			if (type == QuestionType.Text) currentText else ""
	)

	private val currentOption: String?
		get() = if (answer.isEmpty()) null else answer.getString(0)

	private val currentRankValue: Int
		get() = if (answer.isEmpty()) DEFAULT_RANK else answer.getString(0)?.toIntOrNull() ?: DEFAULT_RANK

	private val currentText: String
		get() = if (answer.isEmpty()) "" else answer.getString(0).orEmpty()

	/** 选中选项；重复点击同一项视为取消作答 */
	fun selectOption(index: Int) {
		if (selectedIndex.intValue == index) {
			clearAnswer()
			return
		}
		selectedIndex.intValue = index
		options.getOrNull(index)?.let { setFirst(it.getString("tmxxid")) }
	}

	fun setRank(value: Int) {
		rank.intValue = value
		setFirst("$value")
	}

	fun setText(value: String) {
		text.value = value
		if (value.isEmpty()) answer.clear() else setFirst(value)
	}

	fun clearAnswer() {
		answer.clear()
		when (type) {
			QuestionType.SingleChoice -> selectedIndex.intValue = -1
			QuestionType.Rank -> rank.intValue = DEFAULT_RANK
			QuestionType.Text -> text.value = ""
		}
	}

	fun autoFill() {
		when (type) {
			QuestionType.SingleChoice -> if (options.isNotEmpty()) {
				val last = options.lastIndex
				selectedIndex.intValue = last
				setFirst(options[last].getString("tmxxid"))
			}

			QuestionType.Rank -> setRank(DEFAULT_RANK)
			QuestionType.Text -> Unit
		}
	}

	/**
	 * 旧实现用 `answer[0] = value` 原位替换，前提是服务端返回的作答数组已预留一个空位；
	 * 数组为空时改用 add，避免越界同时保持单元素报文。
	 */
	private fun setFirst(value: String?) {
		if (answer.isEmpty()) answer.add(value) else answer[0] = value
	}

	companion object {
		const val DEFAULT_RANK = 100
	}
}

/** 一个被评人及其题目，对应问卷的粘性分组标题 */
class EvalSubject(
	val name: String,
	val questions: List<EvalQuestion>,
)
