package com.miyuyan.sysuer.rainClass

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberContainedSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.preference.Preference
import com.miyuyan.preference.PreferenceCategory
import com.miyuyan.preference.PreferenceScreen
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.nav.RainClassDetail
import com.miyuyan.sysuer.nav.navigateBack
import com.miyuyan.sysuer.view.ActivityPager
import com.miyuyan.sysuer.view.MenuItem
import com.miyuyan.sysuer.view.RowData
import com.miyuyan.sysuer.view.SectionData
import com.miyuyan.sysuer.view.SquircleAnimatedIndicator
import com.miyuyan.sysuer.view.StaggerScreen
import com.miyuyan.sysuer.view.StatePage
import com.miyuyan.sysuer.view.UiState
import com.miyuyan.sysuer.view.UnboundedTab
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch

@OptIn(
		ExperimentalMaterial3Api::class,
		ExperimentalSharedTransitionApi::class,
		ExperimentalCoroutinesApi::class
)
@Composable
fun RainClassDetailRoute(
	backStack: MutableList<NavKey>,
	navKey: RainClassDetail = backStack.lastOrNull() as RainClassDetail,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val viewModel: RainClassViewModel = viewModel()
	val activity = LocalActivity.current
	val courseList by viewModel.courseList.collectAsStateWithLifecycle()
	val courseInfo by viewModel.courseInfo.collectAsStateWithLifecycle()

	LaunchedEffect(navKey.classId) {
		viewModel.getCourseInfo(navKey.classId)
	}

	val courseName = courseInfo?.getString("course_name") ?: remember(navKey.classId, courseList) {
		courseList.find { it.getString("classroom_id") == navKey.classId }?.getJSONObject("course")
			?.getString("name") ?: "课程详情"
	}

	// 学习日志子 Tab / Pager 状态
	val studyLogPagerState = rememberPagerState(pageCount = { 5 })
	// 未完成子 Tab 状态
	var unfinishedTab by remember { mutableIntStateOf(0) }
	// 讨论区子 Tab 状态
	var discussionTab by remember { mutableIntStateOf(0) }
	// 讨论区搜索
	val discussionSearchBarState = rememberContainedSearchBarState()
	val discussionTextFieldState = rememberTextFieldState()
	var discussionSearchQuery by remember { mutableStateOf("") }
	// 公告搜索
	val announcementSearchBarState = rememberContainedSearchBarState()
	val announcementTextFieldState = rememberTextFieldState()
	var announcementSearchQuery by remember { mutableStateOf("") }

	// 监听讨论区搜索文本
	LaunchedEffect(discussionTextFieldState) {
		snapshotFlow { discussionTextFieldState.text.toString() }.mapLatest { it }
			.distinctUntilChanged().collect { discussionSearchQuery = it }
	}

	// 监听公告搜索文本
	LaunchedEffect(announcementTextFieldState) {
		snapshotFlow { announcementTextFieldState.text.toString() }.mapLatest { it }
			.distinctUntilChanged().collect { announcementSearchQuery = it }
	}

	val tabs = listOf(
			MenuItem(title = "课程信息"),
			MenuItem(title = "学习日志"),
			MenuItem(title = "学习内容"),
			MenuItem(title = "未完成"),
			MenuItem(title = "讨论区"),
			MenuItem(title = "公告"),
			MenuItem(title = "分组"),
			MenuItem(title = "错题集"),
	)

	ActivityPager(
			title = courseName,
			tabs = tabs,
			isTopBarContentFixed = true,
			sharedKey = "RainClassDetail_${navKey.classId}",
			sharedTransitionScope = sharedTransitionScope,
			animatedVisibilityScope = animatedVisibilityScope,
			onNavigationClick = { backStack.navigateBack(activity) },
			topBarContent = { page ->
				when (page) {
					1 -> StudyLogSubTabs(studyLogPagerState)
					3 -> UnfinishedSubTabs(unfinishedTab) { unfinishedTab = it }
					4 -> DiscussionSubTabsWithSearch(
							selectedTab = discussionTab,
							searchBarState = discussionSearchBarState,
							textFieldState = discussionTextFieldState,
							onTabSelected = { discussionTab = it })

					5 -> AnnouncementSearchBar(
							searchBarState = announcementSearchBarState,
							textFieldState = announcementTextFieldState
					)
				}
			},
			pageContent = { page ->
				when (page) {
					0 -> CourseInfoPage()
					1 -> StudyLogPage(
							classId = navKey.classId, pagerState = studyLogPagerState
					)

					2 -> StudyContentPage(classId = navKey.classId)
					3 -> UnfinishedPage(selectedSubTab = unfinishedTab)
					4 -> DiscussionPage(
							selectedSubTab = discussionTab, searchQuery = discussionSearchQuery
					)

					5 -> AnnouncementPage(searchQuery = announcementSearchQuery)
					6 -> GroupingPage()
					7 -> WrongQuestionPage()
				}
			},
	)
}


// ── 学习日志子 Tab ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudyLogSubTabs(pagerState: PagerState) {
	val coroutineScope = rememberCoroutineScope()
	val tabs = listOf("全部日志", "课堂", "课件", "试卷", "公告")

	SecondaryScrollableTabRow(
			edgePadding = 0.dp,
			selectedTabIndex = pagerState.currentPage,
			containerColor = Color.Transparent,
			divider = {},
			indicator = {
				SquircleAnimatedIndicator(pagerState.currentPage)
			},
			tabs = {
				tabs.forEachIndexed { index, title ->
					UnboundedTab(
							selected = pagerState.currentPage == index,
							icon = null,
							text = title,
							onClick = {
								coroutineScope.launch { pagerState.animateScrollToPage(index) }
							})
				}
			})
}

// ── 未完成子 Tab ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnfinishedSubTabs(selectedTab: Int, onTabSelected: (Int) -> Unit) {
	val coroutineScope = rememberCoroutineScope()
	val tabs = listOf("全部", "未超时", "已超时")
	val pagerState = rememberPagerState(pageCount = { tabs.size })

	LaunchedEffect(selectedTab) {
		if (pagerState.currentPage != selectedTab) {
			pagerState.animateScrollToPage(selectedTab)
		}
	}
	LaunchedEffect(pagerState.currentPage) {
		onTabSelected(pagerState.currentPage)
	}

	SecondaryTabRow(
			selectedTabIndex = pagerState.currentPage,
			containerColor = Color.Transparent,
			indicator = {
				SquircleAnimatedIndicator(pagerState.currentPage, isTabScrollable = false)
			},
			divider = {},
			tabs = {
				tabs.forEachIndexed { index, title ->
					UnboundedTab(
							selected = pagerState.currentPage == index,
							icon = null,
							text = title,
							onClick = {
								coroutineScope.launch { pagerState.animateScrollToPage(index) }
							})
				}
			})
}

// ── 讨论区子 Tab + SearchBar ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiscussionSubTabsWithSearch(
	selectedTab: Int,
	searchBarState: androidx.compose.material3.SearchBarState,
	textFieldState: androidx.compose.foundation.text.input.TextFieldState,
	onTabSelected: (Int) -> Unit
) {
	val coroutineScope = rememberCoroutineScope()
	val tabs = listOf("全部", "学习讨论", "记分讨论", "普通讨论")
	val pagerState = rememberPagerState(pageCount = { tabs.size })

	LaunchedEffect(selectedTab) {
		if (pagerState.currentPage != selectedTab) {
			pagerState.animateScrollToPage(selectedTab)
		}
	}
	LaunchedEffect(pagerState.currentPage) {
		onTabSelected(pagerState.currentPage)
	}

	Column(modifier = Modifier.fillMaxWidth()) {
		SecondaryScrollableTabRow(
				edgePadding = 0.dp,
				indicator = {
					SquircleAnimatedIndicator(pagerState.currentPage)
				},
				selectedTabIndex = pagerState.currentPage,
				containerColor = Color.Transparent,
				divider = {},
				tabs = {
					tabs.forEachIndexed { index, title ->
						UnboundedTab(
								selected = pagerState.currentPage == index,
								icon = null,
								text = title,
								onClick = {
									coroutineScope.launch { pagerState.animateScrollToPage(index) }
								})
					}
				})

		val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
		val appBarWithSearchColors = SearchBarDefaults.appBarWithSearchColors(
				searchBarColors = SearchBarDefaults.containedColors(state = searchBarState)
		)
		val inputField = @Composable {
			SearchBarDefaults.InputField(
					textFieldState = textFieldState,
					searchBarState = searchBarState,
					colors = appBarWithSearchColors.searchBarColors.inputFieldColors,
					onSearch = { },
					placeholder = {
						Text(
								modifier = Modifier.clearAndSetSemantics {}, text = "搜索讨论内容"
						)
					},
					leadingIcon = {
						Icon(
								Icons.Rounded.Search,
								contentDescription = stringResource(R.string.search)
						)
					},
			)
		}
		AppBarWithSearch(
				scrollBehavior = scrollBehavior,
				windowInsets = SearchBarDefaults.windowInsets.only(WindowInsetsSides.Horizontal),
				state = searchBarState,
				colors = SearchBarDefaults.appBarWithSearchColors(
						appBarContainerColor = Color.Transparent,
				),
				inputField = inputField,
		)
	}
}

// ── 公告 SearchBar ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnnouncementSearchBar(
	searchBarState: androidx.compose.material3.SearchBarState,
	textFieldState: androidx.compose.foundation.text.input.TextFieldState,
) {
	val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
	val appBarWithSearchColors = SearchBarDefaults.appBarWithSearchColors(
			searchBarColors = SearchBarDefaults.containedColors(state = searchBarState)
	)
	val inputField = @Composable {
		SearchBarDefaults.InputField(
				textFieldState = textFieldState,
				searchBarState = searchBarState,
				colors = appBarWithSearchColors.searchBarColors.inputFieldColors,
				onSearch = { },
				placeholder = {
					Text(
							modifier = Modifier.clearAndSetSemantics {}, text = "搜索公告"
					)
				},
				leadingIcon = {
					Icon(
							Icons.Rounded.Search,
							contentDescription = stringResource(R.string.search)
					)
				},
		)
	}
	AppBarWithSearch(
			scrollBehavior = scrollBehavior,
			windowInsets = SearchBarDefaults.windowInsets.only(WindowInsetsSides.Horizontal),
			state = searchBarState,
			colors = SearchBarDefaults.appBarWithSearchColors(
					appBarContainerColor = Color.Transparent,
			),
			inputField = inputField,
	)
}

@Composable
private fun StudyLogPage(
	classId: String,
	pagerState: PagerState,
) {
	val viewModel: RainClassViewModel = viewModel()
	val studyLogList by viewModel.studyLogList.collectAsStateWithLifecycle(null)
//	val studyLogStatus by viewModel.studyLogStatus.collectAsStateWithLifecycle(null)

	val studyLogSections = remember { mutableStateMapOf<Int, SnapshotStateList<SectionData>>() }
	val studyLogUiStates = remember { mutableStateMapOf<Int, UiState>() }
	var requestingPage by remember { mutableIntStateOf(-1) }

	LaunchedEffect(Unit) {
		viewModel.studyLogList.collect {
			if (!studyLogSections.containsKey(pagerState.currentPage)) {
				if (!it.isEmpty()) {
					val leafIds = JSONArray()
					it.forEach { activity ->
						leafIds.add(activity.getString("courseware_id"))
					}
					viewModel.getStudyLogStatus(classId, leafIds)
				} else {
					studyLogSections[requestingPage] = mutableStateListOf()
					studyLogUiStates[requestingPage] = UiState.Empty
				}
			}
		}
	}
	LaunchedEffect(Unit) {
		viewModel.studyLogStatus.collect { studyLogStatus ->
			if (requestingPage >= 0) {
				studyLogSections[requestingPage] = mutableStateListOf<SectionData>().apply {
					studyLogList?.forEach { activity ->
						val rows = mutableStateListOf(
								RowData(
										"类型",
										RainClassViewModel.getStudyLogTypeText(activity.getInteger("type"))
								), RowData(
								"创建时间",
								RainClassViewModel.formatTimestampMillie(activity.getLong("create_time"))
						)
						)
						activity.getString("courseware_id")?.takeIf { it.isNotEmpty() }
							?.let { coursewareId ->
//							rows += RowData("课件ID", coursewareId)
								studyLogStatus.getJSONObject(coursewareId)?.let { status ->
									val totalDone = status.getInteger("total_done")
									val leafStatus =
										activity.getJSONObject("content")?.getString("leaf_id")
											?.let { lid ->
												status.getJSONObject(lid)?.let { obj ->
													val total = obj.getIntValue("total", 0)
													val done = obj.getIntValue("done", 0)
													if (total > 0) "$done / $total" else null
												} ?: status.getInteger(lid)
													?.let { if (it == 1) "已完成" else "未完成" }
											}
									val doneText = leafStatus ?: when (totalDone) {
										-1 -> "未完成"
										0 -> "未完成"
										1 -> "已完成"
										else -> null
									}
									doneText?.let { rows += RowData("完成度", it) }
								}
							}
						activity.getJSONObject("content")?.let { content ->
							content.getLong("score_d")?.takeIf { it > 0 }?.let {
								rows += RowData(
										"截止时间", RainClassViewModel.formatTimestampMillie(it)
								)
							}
//						content.getInteger("leaf_id")?.takeIf { it > 0 }?.let {
//							rows += RowData("Leaf ID", it.toString())
//						}
						}

						when (activity.getInteger("type")) {
							5 -> {
								activity.getInteger("status")?.let {
									rows += RowData(
											"状态", when (it) {
										0 -> "未开始"
										1 -> "进行中"
										2 -> "已提交"
										3 -> "已结束"
										else -> it.toString()
									}
									)
								}
								activity.getLong("deadline")?.takeIf { it > 0 }?.let {
									rows += RowData(
											"截止时间", RainClassViewModel.formatTimestampMillie(it)
									)
								}
								activity.getInteger("total_score")?.let {
									rows += RowData("总分", it.toString())
								}
								activity.getInteger("score")?.let {
									rows += RowData("得分", it.toString())
								}
								activity.getInteger("problem_count")?.let {

									rows += RowData("题目数", it.toString())
								}
								activity.getInteger("limit")?.takeIf { it > 0 }?.let {
									rows += RowData("限时", "$it 分钟")
								}
								activity.getInteger("progress")?.let {
									rows += RowData("进度", "$it")
								}
								activity.getInteger("unfinished")?.let {
									rows += RowData("未完成", if (it == 1) "是" else "否")
								}
								if (activity.containsKey("is_single_paper")) {
									rows += RowData(
											"单卷模式",
											if (activity.getBooleanValue("is_single_paper")) "是" else "否"
									)
								}
								if (activity.containsKey("is_partial_participation")) {
									rows += RowData(
											"部分参与",
											if (activity.getBooleanValue("is_partial_participation")) "是" else "否"
									)
								}
								if (activity.containsKey("is_offline")) {
									rows += RowData(
											"离线考试",
											if (activity.getBooleanValue("is_offline")) "是" else "否"
									)
								}
								activity.getInteger("identity_auth")?.let {
									rows += RowData("身份认证", if (it == 1) "开启" else "关闭")
								}
								activity.getInteger("online_proctor")?.let {
									rows += RowData("线上监考", if (it == 1) "开启" else "关闭")
								}
							}
						}

						add(SectionData(title = activity.getString("title"), rows = rows))
					}
				}
				studyLogUiStates[requestingPage] = UiState.Content
			}
		}
	}

	LaunchedEffect(classId, pagerState.currentPage) {
		val page = pagerState.currentPage
		if (!studyLogSections.containsKey(page)) {
			requestingPage = page
			viewModel.getStudyLog(classId, RainClassViewModel.STUDY_LOG_TYPES[page] ?: -1)
		}
	}

	HorizontalPager(
			state = pagerState,
			modifier = Modifier.fillMaxSize(),
	) { page ->
		StatePage(state = studyLogUiStates[page] ?: UiState.Loading) {
			StaggerScreen(sections = studyLogSections.getOrElse(page) { remember { mutableStateListOf() } })
		}
	}
}

@Composable
private fun StudyContentPage(classId: String) {
	val viewModel: RainClassViewModel = viewModel()
	val chapterList by viewModel.chapterList.collectAsStateWithLifecycle()
	val chapterUiState by viewModel.chapterUiState.collectAsStateWithLifecycle()

	LaunchedEffect(classId) {
		if (chapterUiState == UiState.Unstarted || chapterUiState == UiState.Error) {
			viewModel.getChapter(classId)
		}
	}

	StatePage(state = chapterUiState) {
		PreferenceScreen(modifier = Modifier.fillMaxSize()) {
			chapterList.forEach { chapter ->
				val leaves = chapter.getJSONArray("section_leaf_list")
				if (leaves != null && !leaves.isEmpty()) {
					PreferenceCategory(title = chapter.getString("name")) {
						leaves.filterIsInstance<JSONObject>().forEach { leaf ->
							val leafType = leaf.getInteger("leaf_type")
							val isLocked = leaf.getBooleanValue("is_locked")
							val isScore = leaf.getBooleanValue("is_score")
							val summary = buildList {
								add(RainClassViewModel.getLeafTypeText(leafType))
								if (isLocked) add("已锁定")
								if (isScore) add("记分")
								leaf.getLong("score_deadline")?.takeIf { it > 0 }?.let {
									add("截止 ${RainClassViewModel.formatTimestampMillie(it)}")
								}
							}.joinToString(" · ")
							item {
								Preference(
										onClick = null,
										title = leaf.getString("name"),
										summary = summary,
										icon = {
											Icon(
													painterResource(
															RainClassViewModel.getLeafTypeIcon(
																	leafType
															)
													),
													contentDescription = null,
													tint = MaterialTheme.colorScheme.primary
											)
										})
							}
						}
					}
				}
			}
		}
	}
}

@Composable
private fun UnfinishedPage(selectedSubTab: Int) {
	StatePage {
		StaggerScreen()
	}
}

@Composable
private fun DiscussionPage(selectedSubTab: Int, searchQuery: String) {
	StatePage {
		StaggerScreen()
	}
}

@Composable
private fun AnnouncementPage(searchQuery: String) {
	StatePage {
		StaggerScreen()
	}
}

@Composable
private fun GroupingPage() {
	StatePage {
		StaggerScreen()
	}
}

@Composable
private fun WrongQuestionPage() {
	StatePage {
		StaggerScreen()
	}
}

@Composable
private fun CourseInfoPage() {
	val viewModel: RainClassViewModel = viewModel()
	val courseInfo by viewModel.courseInfo.collectAsStateWithLifecycle()
	val courseInfoUiState by viewModel.courseInfoUiState.collectAsStateWithLifecycle()
	val noneString = stringResource(R.string.none)
	val sections = remember(courseInfo) {
		mutableStateListOf<SectionData>().apply {
			courseInfo?.let { info ->
				val basicRows = mutableStateListOf<RowData>()
				basicRows += RowData("课程名称", info.getString("course_name", noneString))
				info.getString("course_short_name")?.takeIf { it.isNotEmpty() }?.let {
					basicRows += RowData("课程简称", it)
				}
				basicRows += RowData("课堂名称", info.getString("name", noneString))
				info.getString("short_name")?.takeIf { it.isNotEmpty() }?.let {
					basicRows += RowData("课堂简称", it)
				}
				basicRows += RowData("课程ID", info.getString("course_id", noneString))
				basicRows += RowData("课堂ID", info.getString("id", noneString))
				add(SectionData(title = "基本信息", rows = basicRows))

				val teacherRows = mutableStateListOf(
						RowData("授课教师", info.getString("teacher_name", noneString)),
						RowData("选课人数", info.getString("students_count", noneString))
				)
				add(SectionData(title = "教师与选课", rows = teacherRows))

				val timeRows = mutableStateListOf(
						RowData(
								"开课时间",
								RainClassViewModel.formatTimestampMillie(info.getLong("class_start"))
						), RowData(
						"结课时间",
						RainClassViewModel.formatTimestampMillie(info.getLong("class_end"))
				)
				)
				add(SectionData(title = "时间安排", rows = timeRows))

				val platformRows = mutableStateListOf(
						RowData(
								"用户角色", when (info.getIntValue("user_role")) {
							1 -> "管理员"
							2 -> "教师"
							3 -> "助教"
							5 -> "学生"
							else -> info.getIntValue("user_role").toString()
						}
						), RowData(
						"所属平台", when (info.getIntValue("platform")) {
					1 -> "其他"
					2 -> "学堂在线"
					3 -> "雨课堂"
					else -> info.getString("platform", noneString)
				}
				)
				)
				info.getString("university_domain")?.takeIf { it.isNotEmpty() }?.let {
					platformRows += RowData("学校域名", it)
				}
				add(SectionData(title = "平台信息", rows = platformRows))

				info.getJSONObject("settings")?.let { settings ->
					val settingsRows = mutableStateListOf(
							RowData(
									"分组功能",
									if (settings.getBooleanValue("group")) "开启" else "关闭"
							), RowData(
							"讨论功能",
							if (settings.getBooleanValue("discussion")) "开启" else "关闭"
					), RowData(
							"同学功能",
							if (settings.getBooleanValue("classmates")) "开启" else "关闭"
					), RowData(
							"提问功能",
							if (settings.getBooleanValue("questions")) "开启" else "关闭"
					)
					)
					add(SectionData(title = "功能设置", rows = settingsRows))
				}

				info.getJSONObject("extra_info")?.let { extraInfo ->
					val extraRows = mutableStateListOf(
							RowData(
									"已结课",
									if (extraInfo.getBooleanValue("has_classend")) "是" else "否"
							)
					)
					add(SectionData(title = "附加信息", rows = extraRows))
				}
			}
		}
	}
	StatePage(state = courseInfoUiState) {

		StaggerScreen(sections = sections)
	}
}