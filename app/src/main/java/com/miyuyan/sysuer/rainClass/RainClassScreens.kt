package com.miyuyan.sysuer.rainClass

import android.content.Context
import android.content.Intent
import android.text.Html
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import coil.compose.AsyncImage
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.TargetHost
import com.miyuyan.sysuer.browser.BrowserActivity
import com.miyuyan.sysuer.model.RainClassModel
import com.miyuyan.sysuer.nav.RainClassDetail
import com.miyuyan.sysuer.rainClass.RainClassViewModel.Companion.formatTerm
import com.miyuyan.sysuer.rainClass.RainClassViewModel.Companion.formatTimestampMillie
import com.miyuyan.sysuer.rainClass.RainClassViewModel.Companion.getTermColor
import com.miyuyan.sysuer.view.RowData
import com.miyuyan.sysuer.view.SectionCard
import com.miyuyan.sysuer.view.SectionData
import com.miyuyan.sysuer.view.StaggerScreen
import com.miyuyan.sysuer.view.StatePage
import com.miyuyan.sysuer.view.UiState

@OptIn(
		ExperimentalMaterial3Api::class,
		androidx.compose.animation.ExperimentalSharedTransitionApi::class
)
@Composable
fun CourseScreen(
	backStack: MutableList<NavKey>,
	searchQuery: String = "",
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
	val viewModel: RainClassViewModel = viewModel()
	val courseList by viewModel.courseList.collectAsStateWithLifecycle()
	val courseUiState by viewModel.courseUiState.collectAsStateWithLifecycle()

	val filteredList = remember(searchQuery, courseList) {
		if (searchQuery.isBlank()) courseList
		else courseList.filter { item ->
			val course = item.getJSONObject("course")
			val teacher = item.getJSONObject("teacher")
			course?.getString("name")?.contains(searchQuery, ignoreCase = true) == true || teacher
				?.getString("name")?.contains(searchQuery, ignoreCase = true) == true
		}
	}

	StatePage(state = courseUiState) {
		LazyVerticalGrid(
				columns = GridCells.Adaptive(minSize = 340.dp),
				modifier = Modifier.fillMaxSize(),
				contentPadding = PaddingValues(16.dp),
				horizontalArrangement = Arrangement.spacedBy(16.dp),
				verticalArrangement = Arrangement.spacedBy(16.dp)
		) {
			items(filteredList.size) { index ->
				val courseItem = filteredList[index]
				val course = courseItem.getJSONObject("course")
				val teacher = courseItem.getJSONObject("teacher")
				val termColor = getTermColor(courseItem.getInteger("term"))
				val classId = courseItem.getInteger("classroom_id")?.toString() ?: ""
				Card(
						modifier = Modifier
							.fillMaxWidth()
							.then(
									if (sharedTransitionScope != null && animatedVisibilityScope != null) {
								with(sharedTransitionScope) {
									Modifier.sharedBounds(
											sharedContentState = rememberSharedContentState(
													key = "RainClassDetail_$classId"
											),
											animatedVisibilityScope = animatedVisibilityScope
									)
								}
							} else Modifier)
							.clickable { backStack.add(RainClassDetail(classId)) },
						colors = CardDefaults.cardColors(
								containerColor = termColor, contentColor = Color.White
						)
				) {
					ListItem(
							modifier = Modifier,
							leadingContent = {
								AsyncImage(
										model = teacher?.getString("avatar"),
										contentDescription = "教师头像",
										modifier = Modifier
											.size(40.dp)
											.clip(CircleShape),
										contentScale = ContentScale.Crop
								)
							},
							trailingContent = {
								AsyncImage(
										model = course?.getString("university_mini_logo"),
										contentDescription = "学校Logo",
										modifier = Modifier.size(24.dp)
								)
							},
							overlineContent = {
								Text(
										text = formatTerm(courseItem.getInteger("term")),
										style = MaterialTheme.typography.labelSmall
								)
							},
							supportingContent = {
								SelectionContainer {
									Text(
											"${teacher?.getString("name") ?: "未知教师"} | 课堂号: ${
												courseItem.getInteger("classroom_id")
											}"
									)
								}
							},
							colors = ListItemDefaults.colors(
									containerColor = Color.Transparent,
									headlineColor = Color.White,
									supportingColor = Color.White.copy(
											alpha = 0.7f
									),
									overlineColor = Color.White.copy(
											alpha = 0.9f
									)
							),
							content = {
								Text(course?.getString("name") ?: "未知课程")
							},
					)
				}
			}
		}
	}
}

@Composable
fun ExamScreen(
) {
	val viewModel: RainClassViewModel = viewModel()
	val examList by viewModel.examList.collectAsStateWithLifecycle()
	val examsUiState by viewModel.examsUiState.collectAsStateWithLifecycle()
	var selectedExamJson by rememberSaveable { mutableStateOf<String?>(null) }
	val selectedExam = remember(selectedExamJson) {
		selectedExamJson?.let { JSONObject.parseObject(it) }
	}
	var examStarted by rememberSaveable { mutableStateOf(false) }

	BackHandler(enabled = selectedExam != null) {
		if (examStarted) {
			examStarted = false
		} else {
			selectedExamJson = null
		}
	}

	Box(modifier = Modifier.fillMaxSize()) {
		Column(modifier = Modifier.fillMaxSize()) {
			if (examsUiState == UiState.Loading) {
				Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
					CircularProgressIndicator()
				}
			} else if (examList.isEmpty()) {
				Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
					Text(
							text = stringResource(R.string.no_exam),
							style = MaterialTheme.typography.bodyLarge
					)
				}
			} else {
				LazyVerticalGrid(
						columns = GridCells.Adaptive(minSize = 340.dp),
						modifier = Modifier.fillMaxSize(),
						contentPadding = PaddingValues(16.dp),
						horizontalArrangement = Arrangement.spacedBy(16.dp),
						verticalArrangement = Arrangement.spacedBy(16.dp)
				) {
					items(examList.size) { index ->
						val exam = examList[index]
						ExamItem(exam) {
							selectedExamJson = exam.toJSONString()
						}
					}
				}
			}
		}

		AnimatedVisibility(
				visible = selectedExam != null,
				enter = slideInVertically(initialOffsetY = { it }),
				exit = slideOutVertically(targetOffsetY = { it })
		) {
			selectedExam?.let { exam ->
				if (examStarted) {
					ExamPaperScreen(
							examSummary = exam, onBack = { examStarted = false })
				} else {
					ExamDetailScreen(
							examSummary = exam,
							onBack = { selectedExamJson = null },
							onStartExam = { examStarted = true })
				}
			}
		}
	}
}

@Composable
fun ExamItem(exam: JSONObject, onClick: () -> Unit) {
	val context = LocalContext.current
	ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
		Column(modifier = Modifier.padding(16.dp)) {
			Row(verticalAlignment = Alignment.CenterVertically) {
				AsyncImage(
						model = exam.getString("user_avatar"),
						contentDescription = stringResource(R.string.user_avatar),
						modifier = Modifier
							.size(32.dp)
							.clip(CircleShape)
				)
				Spacer(modifier = Modifier.size(8.dp))
				Text(
						text = exam.getString("classroom_name", stringResource(R.string.none)),
						style = MaterialTheme.typography.labelMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant
				)
				Spacer(modifier = Modifier.weight(1f))
				IconButton(onClick = {
					val examId = exam.getIntValue("id")
					openExamInBrowser(context, examId)
				}) {
					Icon(
							Icons.AutoMirrored.Filled.OpenInNew,
							contentDescription = stringResource(R.string.open_in_browser),
							modifier = Modifier.size(20.dp)
					)
				}
			}
			Spacer(modifier = Modifier.height(8.dp))
			Text(
					text = exam.getString("title", stringResource(R.string.unknown)),
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.Bold
			)
			Spacer(modifier = Modifier.height(8.dp))
			Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
				Column {
					Text(
							text = stringResource(R.string.start_time),
							style = MaterialTheme.typography.labelSmall
					)
					Text(
							text = formatTimestampMillie(exam.getLong("start_time")),
							style = MaterialTheme.typography.bodySmall
					)
				}
				Column {
					Text(
							text = stringResource(R.string.end_time),
							style = MaterialTheme.typography.labelSmall
					)
					Text(
							text = formatTimestampMillie(exam.getLong("end_time")),
							style = MaterialTheme.typography.bodySmall
					)
				}
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamDetailScreen(
	examSummary: JSONObject, onBack: () -> Unit, onStartExam: () -> Unit
) {
	val viewModel: RainClassViewModel = viewModel()
	val context = LocalContext.current
	val examInfo by viewModel.examInfo.collectAsStateWithLifecycle()
	val examInfoUiState by viewModel.examInfoUiState.collectAsStateWithLifecycle()

	LaunchedEffect(Unit) {
		viewModel.getExamInfo(
				examSummary.getIntValue("id"), examSummary.getIntValue("classroom_id")
		)
	}

	Scaffold(
			modifier = Modifier
				.fillMaxSize()
				.background(MaterialTheme.colorScheme.surface),
			topBar = {
				TopAppBar(
						title = {
					Text(
							examSummary.getString(
									"title", stringResource(R.string.exam_detail)
							)
					)
				},
						navigationIcon = {
							IconButton(onClick = onBack) {
								Icon(
										Icons.AutoMirrored.Filled.ArrowBack,
										contentDescription = stringResource(R.string.back)
								)
							}
						},
						actions = {
							IconButton(onClick = {
								openExamInBrowser(context, examSummary.getIntValue("id"))
							}) {
								Icon(
										Icons.AutoMirrored.Filled.OpenInNew,
										contentDescription = stringResource(R.string.open_in_browser)
								)
							}
						},
						windowInsets = WindowInsets(0),
						colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
				)
			},
			bottomBar = {
				Box(
						modifier = Modifier
							.fillMaxWidth()
							.padding(16.dp)
				) {
					Button(
							onClick = onStartExam,
							modifier = Modifier.fillMaxWidth(),
							enabled = examInfoUiState != UiState.Loading
					) {
						Text("开始答题", style = MaterialTheme.typography.titleMedium)
					}
				}
			}) { innerPadding ->
		Box(
				modifier = Modifier
					.fillMaxSize()
					.padding(top = innerPadding.calculateTopPadding())
		) {
			if (examInfoUiState == UiState.Loading) {
				CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
			} else {
				examInfo?.let { info ->
					val sections = remember { mutableStateListOf<SectionData>() }
					sections.clear()

					val resultStatus = when (info.getJSONObject("result")?.getInteger("status")) {
						0 -> "未开始"
						1 -> "进行中"
						2 -> "已提交"
						else -> "未知"
					}
					val examRows = remember { mutableStateListOf<RowData>() }
					examRows += RowData("当前状态", resultStatus)
					examRows += RowData("总分", "${info.getString("total_score")} 分")
					examRows += RowData("题目数量", "${info.getString("problem_count")} 题")
					info.getInteger("limit")
						?.let { if (it > 0) examRows += RowData("限时", "$it 分钟") }
					examRows += RowData(
							"计分方式", when (info.getInteger("way_of_score")) {
						1 -> "最高分"
						2 -> "最后一次"
						else -> "普通"
					}
					)
					examRows += RowData("允许重试", "${info.getInteger("max_retry")} 次")
					examRows += RowData(
							"手动阅卷", if (info.getInteger("is_manual_review") == 1) "是" else "否"
					)
					examRows += RowData(
							"强制确认", if (info.getBoolean("force_confirm") == true) "是" else "否"
					)
					examRows += RowData(
							"开始时间", formatTimestampMillie(info.getLong("start_time"))
					)
					examRows += RowData(
							"截止时间", formatTimestampMillie(info.getLong("deadline"))
					)
					if (info.getBoolean("limit_early_submission") == true) {
						examRows += RowData(
								"限制早交",
								"开启 (${info.getInteger("limit_early_submission_time")} 分钟)"
						)
					}
					sections += SectionData(
							title = stringResource(R.string.exam_detail), rows = examRows
					)

					val proctorRows = remember { mutableStateListOf<RowData>() }
					proctorRows += RowData(
							"在线监考",
							if (info.getInteger("online_proctor") == 1) "开启" else "关闭"
					)
					proctorRows += RowData(
							"随机人脸",
							if (info.getInteger("web_random_take_face_photo") == 1) "开启" else "关闭"
					)
					proctorRows += RowData(
							"人脸识别",
							if (info.getJSONObject("face_auth_status")
									?.getInteger("online_proctor") == 1
							) "开启"
							else "关闭"
					)
					proctorRows += RowData(
							"切屏监测",
							if (info.getInteger("page_switch_detection") == 1) "开启" else "关闭"
					)
					proctorRows += RowData(
							"截屏保护",
							if (info.getInteger("app_capture_screen") == 1 || info.getInteger(
										"open_screen_cuts"
								) == 1
							) "开启"
							else "关闭"
					)
					proctorRows += RowData(
							"离线考试", if (info.getBoolean("is_offline") == true) "是" else "否"
					)
					proctorRows += RowData(
							"加密传输", if (info.getString("encrypt") == "True") "是" else "否"
					)
					info.getString("access_restriction_info")?.takeIf { it.isNotBlank() }?.let {
						proctorRows += RowData("进入限制", it)
					}
					sections += SectionData(
							title = "监考规则与限制", rows = proctorRows
					)

					val user = info.getJSONObject("user")
					val identityRows = remember { mutableStateListOf<RowData>() }
					identityRows += RowData("姓名", user?.getString("user_name") ?: "未知")
					identityRows += RowData("学号", user?.getString("school_number") ?: "未知")
					sections += SectionData(
							title = "考生身份", rows = identityRows, footer = {
						Row(
								modifier = Modifier
									.fillMaxWidth()
									.padding(
											horizontal = dimensionResource(R.dimen.horizontal_padding),
											vertical = dimensionResource(R.dimen.vertical_padding)
									), verticalAlignment = Alignment.CenterVertically
						) {
							AsyncImage(
									model = user?.getString("avatar"),
									contentDescription = "考生头像",
									modifier = Modifier
										.size(48.dp)
										.clip(CircleShape),
									contentScale = ContentScale.Crop
							)
						}
					})

					info.getString("description")?.takeIf { it.isNotBlank() }?.let { desc ->
						sections += SectionData(
								title = "考试说明", footer = {
							Text(
									text = desc,
									modifier = Modifier.fillMaxWidth(),
									style = MaterialTheme.typography.bodyMedium,
									color = MaterialTheme.colorScheme.onSurfaceVariant
							)
						})
					}

					StaggerScreen(
							modifier = Modifier.padding(
									bottom = innerPadding.calculateBottomPadding()
							), sections = sections
					)
				}
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamPaperScreen(
	examSummary: JSONObject, onBack: () -> Unit
) {
	val viewModel: RainClassViewModel = viewModel()
	val problemList by viewModel.problemList.collectAsStateWithLifecycle()
	val problemUiState by viewModel.problemUiState.collectAsStateWithLifecycle()
	val answers = remember { mutableStateMapOf<Int, String>() }

	LaunchedEffect(Unit) {
		viewModel.getProblem(examSummary.getIntValue("id"))
	}

	Scaffold(
			modifier = Modifier
				.fillMaxSize()
				.background(MaterialTheme.colorScheme.surface),
			topBar = {
				TopAppBar(
						title = { Text(examSummary.getString("title") ?: "正在考试") },
						navigationIcon = {
							IconButton(onClick = onBack) {
								Icon(
										Icons.AutoMirrored.Filled.ArrowBack,
										contentDescription = "返回"
								)
							}
						},
						windowInsets = WindowInsets(0),
						colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
				)
			},
			bottomBar = {
				Box(
						modifier = Modifier
							.fillMaxWidth()
							.padding(16.dp)
				) {
					Button(
							onClick = { /* TODO: Submit exam */ },
							modifier = Modifier.fillMaxWidth(),
							enabled = problemUiState != UiState.Loading
					) {
						Text("提交试卷", style = MaterialTheme.typography.titleMedium)
					}
				}
			}) { innerPadding ->
		Box(
				modifier = Modifier
					.fillMaxSize()
					.padding(innerPadding)
		) {
			if (problemUiState == UiState.Loading) {
				CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
			} else {
				LazyColumn(
						modifier = Modifier.fillMaxSize(),
						contentPadding = PaddingValues(16.dp),
						verticalArrangement = Arrangement.spacedBy(24.dp)
				) {
					items(problemList) { problem ->
						ProblemItem(
								problem = problem,
								answer = answers[problem.getIntValue("index")] ?: "",
								onAnswerChange = { answers[problem.getIntValue("index")] = it })
					}
				}
			}
		}
	}
}

@Composable
fun ProblemItem(problem: JSONObject, answer: String, onAnswerChange: (String) -> Unit) {
	Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
		Row(verticalAlignment = Alignment.CenterVertically) {
			Text(
					text = "第 ${problem.getIntValue("index") + 1} 题",
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.Bold
			)
			Spacer(modifier = Modifier.width(8.dp))
			Text(
					text = "(${problem.getString("TypeText")})",
					style = MaterialTheme.typography.labelMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant
			)
			Spacer(modifier = Modifier.weight(1f))
			Text(
					text = "${problem.getString("Score")} 分",
					style = MaterialTheme.typography.labelMedium,
					color = MaterialTheme.colorScheme.primary
			)
		}

		AndroidView(factory = { context ->
			TextView(context).apply {
				text = Html.fromHtml(problem.getString("Body"), Html.FROM_HTML_MODE_COMPACT)
				textSize = 16f
				setTextColor(android.graphics.Color.BLACK)
			}
		}, modifier = Modifier.fillMaxWidth())

		if (problem.getString("Type") == "ShortAnswer") {
			OutlinedTextField(
					value = answer,
					onValueChange = onAnswerChange,
					modifier = Modifier.fillMaxWidth(),
					placeholder = { Text("请输入你的回答") },
					minLines = 3
			)
		} else {
			Text(
					text = "暂不支持该题型答题",
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.error
			)
		}
	}
}


@Composable
fun AccountScreen() {
	val viewModel: RainClassViewModel = viewModel()
	val userInfo by viewModel.userInfo.collectAsStateWithLifecycle()
	val isLoginRequired by viewModel.isLoginRequired.collectAsStateWithLifecycle()
	val userUiState by viewModel.userUiState.collectAsStateWithLifecycle()
	val scrollState = rememberScrollState()

	Column(
			modifier = Modifier
				.fillMaxSize()
				.verticalScroll(scrollState)
				.padding(16.dp),
			horizontalAlignment = Alignment.CenterHorizontally
	) {
		if (userUiState == UiState.Loading) {
			Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
				CircularProgressIndicator()
			}
		} else if (isLoginRequired) {
			Text(text = "请扫码登录雨课堂", style = MaterialTheme.typography.titleMedium)
			Spacer(modifier = Modifier.height(16.dp))
			Card(elevation = CardDefaults.cardElevation()) {
				AndroidView(factory = { ctx ->
					ImageView(ctx).apply {
						ctx.let {
							val model = RainClassModel(it)
							model.contextUtil.loginByQrCode(TargetHost.YU_KE_TANG, this) {
								viewModel.getUserInfo()
							}
						}
					}
				}, modifier = Modifier.fillMaxSize())
			}
		} else if (userInfo != null) {
			val info = userInfo!!
			val noneString = stringResource(R.string.none)
			val rows = remember {
				mutableStateListOf<RowData>()
			}
			rows.addAll(
					listOf(
							RowData(
									stringResource(R.string.name),
									info.getString("name", noneString)
							), RowData(
							stringResource(R.string.student_id),
							info.getString("school_number", noneString)
					), RowData(
							stringResource(R.string.university),
							info.getString("school", noneString)
					), RowData(
							stringResource(R.string.phone),
							info.getString("phone_number", noneString)
					), RowData(
							stringResource(R.string.email), info.getString("email", noneString)
					)
					)
			)
			AsyncImage(
					model = info.getString("avatar"),
					contentDescription = stringResource(R.string.user_avatar),
					modifier = Modifier
						.size(80.dp)
						.clip(CircleShape),
					contentScale = ContentScale.Crop
			)
			Spacer(modifier = Modifier.height(16.dp))
			SectionCard(
					section = SectionData(
							title = stringResource(R.string.account_info), rows = rows
					), isExpandable = false, defaultExpanded = true
			)
		}
	}
}

@Composable
fun AccountInfoRow(label: String, value: String) {
	Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
		Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
		SelectionContainer {
			Text(text = value, fontWeight = FontWeight.Bold)
		}
	}
}

fun syncCookiesToWeb(context: Context) {
	val myCm = com.miyuyan.sysuer.api.CookieManager(context)
	val webCm = android.webkit.CookieManager.getInstance()
	webCm.setAcceptCookie(true)
	listOf(
			"www.yuketang.cn", "yuketang.cn", "xuetangx.com", "examination.xuetangx.com"
	).forEach { host ->
		myCm.get(host).forEach { cookie ->
			webCm.setCookie(host, cookie)
		}
	}
	webCm.flush()
}

fun openExamInBrowser(context: Context, examId: Int) {
	syncCookiesToWeb(context)
	val intent = Intent(
			context, BrowserActivity::class.java
	).setData("https://examination.xuetangx.com/exam/$examId?isFrom=2".toUri())
	context.startActivity(intent)
}