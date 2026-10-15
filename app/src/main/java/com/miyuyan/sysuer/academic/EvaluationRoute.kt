package com.miyuyan.sysuer.academic

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.CommonUtil.extractValue
import com.miyuyan.sysuer.api.CommonUtil.trim
import com.miyuyan.sysuer.nav.navigateBack
import com.miyuyan.sysuer.view.ActivityPager
import com.miyuyan.sysuer.view.MenuItem
import com.miyuyan.sysuer.view.RowData
import com.miyuyan.sysuer.view.SectionData
import com.miyuyan.sysuer.view.StaggerScreen
import com.miyuyan.sysuer.view.StatePage
import kotlinx.coroutines.launch

/**
 * 教学评价（评教）：单个 [ActivityPager] 承载「评价任务 → 被评课程 → 问卷」三级下钻。
 *
 * 原实现由 EvaluationActivity + evaluation_nav.xml + 三个 Fragment 承载，层级靠 Navigation 的
 * popUpToSaveState / restoreState 记忆；迁移到 Compose 后改为页内 level 状态下钻，返回键与顶栏
 * 返回逐级回退，与场馆预订页（GymRoute）的场地下钻同构。
 *
 * 三级都不传 tabs / navs：此时 ActivityPager 只渲染 pageContent，故页面自行接管滚动
 * （isNestedScrollEnabled = false），与 [ExamRoute]、[CETRoute] 一致。
 */
@Composable
fun EvaluationRoute(
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val viewModel: EvaluationViewModel = viewModel()
	val activity = LocalActivity.current
	val snackbarHostState = remember { SnackbarHostState() }

	var level by rememberSaveable { mutableIntStateOf(0) }
	// 顶栏标题跟随当前层级，替代旧版固定的「评教」
	var taskTitle by rememberSaveable { mutableStateOf<String?>(null) }
	var courseTitle by rememberSaveable { mutableStateOf<String?>(null) }

	val root = stringResource(R.string.evaluation)

	LaunchedEffect(Unit) {
		if (viewModel.tasks.isEmpty()) viewModel.loadTasks()
	}

	BackHandler(enabled = level > 0) { level -= 1 }

	ActivityPager(
			title = when (level) {
				0 -> root
				1 -> taskTitle?.takeIf { it.isNotEmpty() } ?: root
				else -> courseTitle?.takeIf { it.isNotEmpty() } ?: root
			},
			snackbar = snackbarHostState,
			isNestedScrollEnabled = false,
			onNavigationClick = {
				if (level > 0) level -= 1 else backStack.navigateBack(activity)
			},
			sharedTransitionScope = sharedTransitionScope,
			animatedVisibilityScope = animatedVisibilityScope,
			sharedKey = "Evaluation",
	) {
		when (level) {
			0 -> TaskPage(
					viewModel = viewModel,
					onOpen = { task ->
						taskTitle = trim(task.getString("rwmc"))
						viewModel.loadCourses(task)
						level = 1
					},
			)

			1 -> CoursePage(
					viewModel = viewModel,
					onOpen = { course ->
						courseTitle = trim(course.getString("kcmc"))
						viewModel.openQuestionnaire(course)
						level = 2
					},
			)

			else -> QuestionnairePage(
					viewModel = viewModel,
					snackbarHostState = snackbarHostState,
			)
		}
	}
}

// ================================================================================================
// 第一级：评价任务
// ================================================================================================

/** 评价任务列表：瀑布流卡片，滚动到底自动加载下一页 */
@Composable
private fun TaskPage(viewModel: EvaluationViewModel, onOpen: (JSONObject) -> Unit) {
	val state by viewModel.taskState.collectAsStateWithLifecycle()
	val context = LocalContext.current
	val open = stringResource(R.string.open)

	val sections = remember(state, viewModel.tasks.size, open) {
		mutableStateListOf<SectionData>().apply {
			viewModel.tasks.forEachIndexed { index, item ->
				add(
						SectionData(
								title = trim(item.getString("rwmc")),
								// 已评满显示已提交图标，否则显示窗口图标（同旧版）
								icon = if ((item.getString("pjsl").toIntOrNull()
										?: 0) <= (item.getString("ypsl").toIntOrNull() ?: 0)
								) R.drawable.submit else R.drawable.window,
								rows = extractValue(
										context, item, intArrayOf(
										R.string.start_time,
										R.string.end_time,
										R.string.total_evaluations,
										R.string.evaluated_count,
								), arrayOf("rwkssj", "rwjssj", "pjsl", "ypsl")
								),
								footerMenus = mutableStateListOf(
										MenuItem(open) { onOpen(item) }
								),
								key = item.getString("rwid") ?: index.toString(),
						)
				)
			}
		}
	}

	StatePage(state = state, onRetry = { viewModel.retryTasks() }) {
		StaggerScreen(
				sections = sections,
				onScrollBottom = { if (viewModel.hasMoreTasks) viewModel.loadMoreTasks() },
		)
	}
}

// ================================================================================================
// 第二级：被评课程
// ================================================================================================

/** 课程列表：瀑布流卡片 + 分页，评价状态按 lsjgzt 映射为本地化文案 */
@Composable
private fun CoursePage(viewModel: EvaluationViewModel, onOpen: (JSONObject) -> Unit) {
	val state by viewModel.courseState.collectAsStateWithLifecycle()
	val context = LocalContext.current
	val open = stringResource(R.string.open)
	val toBeEvaluated = stringResource(R.string.to_be_evaluated)
	val evaluated = stringResource(R.string.evaluated)
	val evalSaved = stringResource(R.string.evaluation_saved)
	val unknown = stringResource(R.string.unknown)
	val statusLabel = stringResource(R.string.evaluation_status)

	// 状态码 → 文案：0 待评价 / 2 已评价 / 3 已保存
	val statusText = remember(toBeEvaluated, evaluated, evalSaved, unknown) {
		mapOf("0" to toBeEvaluated, "2" to evaluated, "3" to evalSaved)
	}

	val sections = remember(state, viewModel.courses.size, open, statusText) {
		mutableStateListOf<SectionData>().apply {
			viewModel.courses.forEachIndexed { index, item ->
				val lsjgzt = item.getString("lsjgzt")
				add(
						SectionData(
								title = trim(item.getString("kcmc")),
								icon = if (lsjgzt == "2") R.drawable.submit else R.drawable.window,
								rows = extractValue(
										context, item, intArrayOf(
										R.string.teacher,
										R.string.course_type,
										R.string.course_college,
										R.string.teaching_class_number,
										R.string.course_code,
										R.string.semester,
								), arrayOf("skjsmc", "kcdlmc", "kkyxmc", "bjmc", "kcdm", "xnxqmc")
								).apply {
									add(RowData(statusLabel, statusText[lsjgzt] ?: unknown))
								},
								footerMenus = mutableStateListOf(
										MenuItem(open) { onOpen(item) }
								),
								key = "${item.getString("wjid")}-${item.getString("bpdm") ?: index}",
						)
				)
			}
		}
	}

	StatePage(state = state, onRetry = { viewModel.retryCourses() }) {
		StaggerScreen(
				sections = sections,
				onScrollBottom = { if (viewModel.hasMoreCourses) viewModel.loadMoreCourses() },
		)
	}
}

// ================================================================================================
// 第三级：问卷
// ================================================================================================

/**
 * 问卷页：按被评人分组的题目，被评人姓名吸附在顶部（对应旧版 ConcatAdapter 中 header = 1 的
 * TitleAdapter 粘性标题）；底部浮层为 保存 / 重置 / 填充 / 提交，对应旧版
 * fragment_questionnaire.xml 的 FloatingToolbarLayout。
 *
 * 浮层用纯 Compose 实现（参照 CrashRoute），窄屏改为可横滑，
 * 替代旧 OverflowLinearLayout 的自动折叠。
 */
@Composable
private fun QuestionnairePage(
	viewModel: EvaluationViewModel,
	snackbarHostState: SnackbarHostState,
) {
	val state by viewModel.questionnaireState.collectAsStateWithLifecycle()
	val scope = rememberCoroutineScope()
	val keyboard = LocalSoftwareKeyboardController.current

	val save = stringResource(R.string.save)
	val reset = stringResource(R.string.reset)
	val fill = stringResource(R.string.fill)
	val submit = stringResource(R.string.submit)
	val confirm = stringResource(R.string.confirm)
	val unchangeable = stringResource(R.string.unchangeable_after_submission)

	Box(modifier = Modifier.fillMaxSize()) {
		StatePage(
				state = state,
				onRetry = { viewModel.retryQuestionnaire() },
		) {
			LazyColumn(
					modifier = Modifier.fillMaxSize(),
					// 底部预留工具栏高度，避免最后一题被浮层遮挡（旧版 paddingBottom = 64dp）
					contentPadding = PaddingValues(
							start = dimensionResource(R.dimen.horizontal_margin),
							end = dimensionResource(R.dimen.horizontal_margin),
							bottom = 72.dp,
					),
					verticalArrangement = Arrangement.spacedBy(
							dimensionResource(R.dimen.vertical_margin)
					),
			) {
				// 题量极多且作答状态挂在 EvalQuestion 实例上，因此用下标作稳定 key，
				// 避免重组合时因 key 缺失而丢失滑条/输入框的本地状态
				viewModel.subjects.forEachIndexed { subjectIndex, subject ->
					// 被评人姓名为空时不渲染分组标题（旧版同样跳过 TitleAdapter）
					if (subject.name.isNotEmpty()) {
						stickyHeader(key = "subject-$subjectIndex") {
							SubjectHeader(subject.name)
						}
					}
					subject.questions.forEachIndexed { questionIndex, question ->
						item(key = "question-$subjectIndex-$questionIndex") {
							QuestionCard(question)
						}
					}
				}
			}
		}

		Surface(
				modifier = Modifier
					.align(Alignment.BottomCenter)
					.fillMaxWidth()
					.imePadding()
					.padding(
							horizontal = dimensionResource(R.dimen.horizontal_margin),
							vertical = dimensionResource(R.dimen.vertical_margin),
					),
				shape = CircleShape,
				color = MaterialTheme.colorScheme.surfaceContainer,
		) {
			Row(
					modifier = Modifier
						.horizontalScroll(rememberScrollState())
						.padding(vertical = dimensionResource(R.dimen.vertical_margin)),
					horizontalArrangement = Arrangement.SpaceEvenly,
					verticalAlignment = Alignment.CenterVertically,
			) {
				FilledTonalButton(onClick = { viewModel.save() }, shapes = ButtonDefaults.shapes()) {
					ToolbarLabel(R.drawable.save, save)
				}
				FilledTonalButton(
						onClick = {
							keyboard?.hide()
							scope.launch {
								if (snackbarHostState.showSnackbar(
											message = reset,
											actionLabel = confirm,
											duration = SnackbarDuration.Short,
									) == SnackbarResult.ActionPerformed
								) viewModel.reset()
							}
						},
						shapes = ButtonDefaults.shapes(),
				) {
					ToolbarLabel(R.drawable.reset, reset)
				}
				FilledTonalButton(
						onClick = { viewModel.autoFill() },
						shapes = ButtonDefaults.shapes(),
				) {
					ToolbarLabel(R.drawable.edit, fill)
				}
				FilledTonalButton(
						onClick = {
							keyboard?.hide()
							scope.launch {
								if (snackbarHostState.showSnackbar(
											message = unchangeable,
											actionLabel = confirm,
											duration = SnackbarDuration.Long,
									) == SnackbarResult.ActionPerformed
								) viewModel.submit()
							}
						},
						shapes = ButtonDefaults.shapes(),
				) {
					ToolbarLabel(R.drawable.submit, submit)
				}
			}
		}
	}
}

/** 粘性分组标题：被评人姓名 */
@Composable
private fun SubjectHeader(name: String) {
	Surface(
			modifier = Modifier
				.fillMaxWidth()
				.padding(vertical = dimensionResource(R.dimen.vertical_margin)),
			color = MaterialTheme.colorScheme.surfaceContainerHigh,
			shape = CircleShape,
	) {
		Text(
				text = name,
				style = MaterialTheme.typography.titleMedium,
				color = MaterialTheme.colorScheme.primary,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier.padding(
						horizontal = dimensionResource(R.dimen.horizontal_margin),
						vertical = dimensionResource(R.dimen.vertical_margin),
				),
		)
	}
}

/** 一道题目：标题 + 按题型渲染的作答控件 */
@Composable
private fun QuestionCard(question: EvalQuestion) {
	Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(
						horizontal = dimensionResource(R.dimen.content_padding),
						vertical = dimensionResource(R.dimen.vertical_margin),
				),
	) {
		Text(
				text = question.title.orEmpty(),
				style = MaterialTheme.typography.titleSmall,
				modifier = Modifier.padding(bottom = dimensionResource(R.dimen.vertical_margin)),
		)
		when (question.type) {
			QuestionType.SingleChoice -> SingleChoiceBody(question)
			QuestionType.Rank -> RankBody(question)
			QuestionType.Text -> TextBody(question)
		}
	}
}

/** 选项题：点击已选中项视为取消作答（同旧版 OptionAdapter 的 toggle 行为） */
@Composable
private fun SingleChoiceBody(question: EvalQuestion) {
	question.options.forEachIndexed { index, option ->
		Row(
				verticalAlignment = Alignment.CenterVertically,
				modifier = Modifier
					.fillMaxWidth()
					.padding(vertical = 2.dp),
		) {
			RadioButton(
					selected = question.selectedIndex.intValue == index,
					onClick = { question.selectOption(index) },
			)
			Spacer(modifier = Modifier.width(dimensionResource(R.dimen.horizontal_gap)))
			Text(
					text = option.getString("xxmc").orEmpty(),
					style = MaterialTheme.typography.bodyMedium,
			)
		}
	}
}

/** 打分题：0-100 整档滑动，分值显示在滑条上方（对应旧版 LABEL_VISIBLE 的 Slider） */
@Composable
private fun RankBody(question: EvalQuestion) {
	val sliderState = rememberSliderState(
			value = question.rank.intValue.toFloat(),
			steps = 99,
			trackRange = 0f..100f,
	)
	// 填充 / 重置会直接改 ViewModel 的分值，这里回写到滑条状态
	LaunchedEffect(question.rank.intValue) {
		sliderState.value = question.rank.intValue.toFloat()
	}
	Text(
			text = question.rank.intValue.toString(),
			style = MaterialTheme.typography.titleMedium,
			color = MaterialTheme.colorScheme.primary,
	)
	Slider(
			state = sliderState,
			onValueChange = {
				question.setRank(it.toInt())
				sliderState.value = it
			},
			onValueChangeFinished = null,
			modifier = Modifier.fillMaxWidth(),
			colors = SliderDefaults.colors(),
			interactionSource = remember { MutableInteractionSource() },
	)
}

/** 文本题 */
@Composable
private fun TextBody(question: EvalQuestion) {
	OutlinedTextField(
			value = question.text.value,
			onValueChange = { question.setText(it) },
			placeholder = { Text(stringResource(R.string.please_enter_content)) },
			modifier = Modifier
				.fillMaxWidth()
				.imePadding(),
	)
}

/** 工具栏按钮：图标 + 文案，对应旧版 MaterialButton 的 icon + text */
@Composable
private fun ToolbarLabel(iconResource: Int, text: String) {
	Icon(
			painter = painterResource(iconResource),
			contentDescription = text,
			modifier = Modifier.size(18.dp),
	)
	Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
	Text(text)
}
