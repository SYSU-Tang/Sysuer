package com.sysu.edu.rainClass

import android.content.Context
import android.content.Intent
import android.text.Html
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.R
import com.sysu.edu.api.TargetHost
import com.sysu.edu.browser.BrowserActivity
import com.sysu.edu.rainClass.RainClassModel.Companion.formatTerm
import com.sysu.edu.rainClass.RainClassModel.Companion.formatTimestamp
import com.sysu.edu.rainClass.RainClassModel.Companion.formatTimestampMillis
import com.sysu.edu.rainClass.RainClassModel.Companion.getTermColor

@Preview(showBackground = true) @Composable fun CourseScreenPreview() {
	MaterialTheme {
		CourseScreen()
	}
}

@OptIn(ExperimentalMaterial3Api::class) @Composable
fun CourseScreen(onRequestScrollToAccount: () -> Unit = {}) {
	var searchQuery by remember { mutableStateOf("") }
	var active by remember { mutableStateOf(false) }
	val context = LocalContext.current
	val courseList = remember { mutableStateOf<List<JSONObject>>(emptyList()) }
	val isLoading = remember { mutableStateOf(true) }
	val model = remember { RainClassModel(context) }
	val message by model.message.observeAsState()
	
	LaunchedEffect(message) {
		message?.let { (what, response) ->
			if (what == RainClassModel.GET_COURSE_LIST) {
				isLoading.value = false
				if (response.containsKey("errcode") && response.getInteger("errcode") == 401002) {
					onRequestScrollToAccount()
				}
				else if (response.containsKey("errcode") && response.getInteger("errcode") == 0) {
					val data = response.getJSONObject("data")
					if (data != null) {
						val list = data.getJSONArray("list")
						if (list != null) {
							courseList.value = list.map { it as JSONObject }
						}
					}
				}
			}
		}
	}
	
	fun getCourseList() {
		isLoading.value = true
		model.getCourseList()
	}
	
	LaunchedEffect(Unit) {
		getCourseList()
	}
	val horizontalPadding by animateDpAsState(if (active) 0.dp else 8.dp, label = "padding")
	
	Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
		SearchBar(query = searchQuery,
		          onQueryChange = { searchQuery = it },
		          onSearch = { active = false },
		          active = active,
		          onActiveChange = { active = it },
		          modifier = Modifier
			          .fillMaxWidth()
			          .widthIn(max = 720.dp)
			          .padding(horizontalPadding)
			          .semantics { traversalIndex = 0f },
		          windowInsets = WindowInsets(0.dp),
		          placeholder = { Text("搜索课程") },
		          leadingIcon = {
			          if (active) {
				          IconButton(onClick = { active = false }) {
					          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
				          }
			          }
			          else {
				          Icon(Icons.Default.Search, contentDescription = null)
			          }
		          },
		          trailingIcon = {
			          var showMenu by remember { mutableStateOf(false) }
			          Box {
				          IconButton(onClick = { showMenu = true }) {
					          Icon(Icons.Default.FilterList, contentDescription = "筛选")
				          }
				          DropdownMenu(expanded = showMenu,
				                       onDismissRequest = { showMenu = false }) {
					          DropdownMenuItem(text = { Text("按时间排序") },
					                           onClick = { showMenu = false })
					          DropdownMenuItem(text = { Text("按名称排序") },
					                           onClick = { showMenu = false })
				          }
			          }
		          }) { }
		
		if (isLoading.value) {
			Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
				CircularProgressIndicator()
			}
		}
		else {
			LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 340.dp),
			                 modifier = Modifier.fillMaxSize(),
			                 contentPadding = PaddingValues(16.dp),
			                 horizontalArrangement = Arrangement.spacedBy(16.dp),
			                 verticalArrangement = Arrangement.spacedBy(16.dp)) {
				items(courseList.value.size) { index ->
					val courseItem = courseList.value[index]
					val course = courseItem.getJSONObject("course")
					val teacher = courseItem.getJSONObject("teacher")
					val termColor = getTermColor(courseItem.getInteger("term"))
					Card(modifier = Modifier.fillMaxWidth(),
					     colors = CardDefaults.cardColors(containerColor = termColor,
					                                      contentColor = Color.White)) {
						ListItem(colors = ListItemDefaults.colors(containerColor = Color.Transparent,
						                                          headlineColor = Color.White,
						                                          supportingColor = Color.White.copy(
							                                          alpha = 0.7f),
						                                          overlineColor = Color.White.copy(
							                                          alpha = 0.9f)),
						         overlineContent = {
							         Text(text = formatTerm(courseItem.getInteger("term")),
							              style = MaterialTheme.typography.labelSmall)
						         },
						         headlineContent = {
							         Text(course?.getString("name") ?: "未知课程")
						         },
						         supportingContent = {
							         SelectionContainer {
								         Text("${teacher?.getString("name") ?: "未知教师"} | 课堂号: ${
									         courseItem.getInteger("classroom_id")
								         }")
							         }
						         },
						         leadingContent = {
							         AsyncImage(model = teacher?.getString("avatar"),
							                    contentDescription = "教师头像",
							                    modifier = Modifier
								                    .size(40.dp)
								                    .clip(CircleShape),
							                    contentScale = ContentScale.Crop)
						         },
						         trailingContent = {
							         AsyncImage(model = course?.getString("university_mini_logo"),
							                    contentDescription = "学校Logo",
							                    modifier = Modifier.size(24.dp))
						         })
					}
				}
			}
		}
	}
}

@Composable fun ExamScreen(onRequestScrollToAccount: () -> Unit = {}) {
	val context = LocalContext.current
	val examList = remember { mutableStateOf<List<JSONObject>>(emptyList()) }
	val isLoading = remember { mutableStateOf(true) }
	val model = remember { RainClassModel(context) }
	val message by model.message.observeAsState()
	var selectedExamJson by rememberSaveable { mutableStateOf<String?>(null) }
	val selectedExam = remember(selectedExamJson) {
		selectedExamJson?.let { JSONObject.parseObject(it) }
	}
	var examStarted by rememberSaveable { mutableStateOf(false) }
	
	BackHandler(enabled = selectedExam != null) {
		if (examStarted) {
			examStarted = false
		}
		else {
			selectedExamJson = null
		}
	}
	
	LaunchedEffect(message) {
		message?.let { (what, response) ->
			if (what == RainClassModel.GET_EXAMS_LIST) {
				isLoading.value = false
				if (response.containsKey("errcode") && response.getInteger("errcode") == 401002) {
					onRequestScrollToAccount()
				}
				else if (response.containsKey("code") && response.getInteger("code") == 0) {
					val data = response.getJSONObject("data")
					if (data != null) {
						val upcoming = data.getJSONArray("upcomingExam")
						if (upcoming != null) {
							examList.value = upcoming.map { it as JSONObject }
						}
					}
				}
			}
		}
	}
	
	fun getExams() {
		isLoading.value = true
		model.getExams()
	}
	
	LaunchedEffect(Unit) {
		getExams()
	}
	
	Box(modifier = Modifier.fillMaxSize()) {
		Column(modifier = Modifier.fillMaxSize()) {
			if (isLoading.value) {
				Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
					CircularProgressIndicator()
				}
			}
			else if (examList.value.isEmpty()) {
				Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
					Text(text = stringResource(R.string.noExam),
					     style = MaterialTheme.typography.bodyLarge)
				}
			}
			else {
				LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 340.dp),
				                 modifier = Modifier.fillMaxSize(),
				                 contentPadding = PaddingValues(16.dp),
				                 horizontalArrangement = Arrangement.spacedBy(16.dp),
				                 verticalArrangement = Arrangement.spacedBy(16.dp)) {
					items(examList.value.size) { index ->
						val exam = examList.value[index]
						ExamItem(exam) {
							selectedExamJson = exam.toJSONString()
						}
					}
				}
			}
		}
		
		AnimatedVisibility(visible = selectedExam != null,
		                   enter = slideInVertically(initialOffsetY = { it }),
		                   exit = slideOutVertically(targetOffsetY = { it })) {
			selectedExam?.let { exam ->
				if (examStarted) {
					ExamPaperScreen(examSummary = exam, onBack = { examStarted = false })
				}
				else {
					ExamDetailScreen(examSummary = exam,
					                 onBack = { selectedExamJson = null },
					                 onStartExam = { examStarted = true })
				}
			}
		}
	}
}

@Composable fun ExamItem(exam: JSONObject, onClick: () -> Unit) {
	val context = LocalContext.current
	ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
		Column(modifier = Modifier.padding(16.dp)) {
			Row(verticalAlignment = Alignment.CenterVertically) {
				AsyncImage(model = exam.getString("user_avatar"),
				           contentDescription = "教师头像",
				           modifier = Modifier
					           .size(32.dp)
					           .clip(CircleShape))
				Spacer(modifier = Modifier.size(8.dp))
				Text(text = exam.getString("classroom_name") ?: "",
				     style = MaterialTheme.typography.labelMedium,
				     color = MaterialTheme.colorScheme.onSurfaceVariant)
				Spacer(modifier = Modifier.weight(1f))
				IconButton(onClick = {
					val examId = exam.getIntValue("id")
					openExamInBrowser(context, examId)
				}) {
					Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "打开网页版", modifier = Modifier.size(20.dp))
				}
			}
			Spacer(modifier = Modifier.height(8.dp))
			Text(text = exam.getString("title") ?: "未知考试",
			     style = MaterialTheme.typography.titleMedium,
			     fontWeight = FontWeight.Bold)
			Spacer(modifier = Modifier.height(8.dp))
			Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
				Column {
					Text(text = "开始时间", style = MaterialTheme.typography.labelSmall)
					Text(text = formatTimestamp(exam.getLong("start_time")),
					     style = MaterialTheme.typography.bodySmall)
				}
				Column {
					Text(text = "结束时间", style = MaterialTheme.typography.labelSmall)
					Text(text = formatTimestamp(exam.getLong("end_time")),
					     style = MaterialTheme.typography.bodySmall)
				}
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class) @Composable fun ExamDetailScreen(examSummary: JSONObject,
                                                                         onBack: () -> Unit,
                                                                         onStartExam: () -> Unit) {
	val context = LocalContext.current
	val examInfo = remember { mutableStateOf<JSONObject?>(null) }
	val isLoading = remember { mutableStateOf(true) }
	val model = remember { RainClassModel(context) }
	val message by model.message.observeAsState()
	
	LaunchedEffect(Unit) {
		model.getExamInfo(examSummary.getIntValue("id"), examSummary.getIntValue("classroom_id"))
	}
	
	LaunchedEffect(message) {
		message?.let { (what, response) ->
			if (what == RainClassModel.GET_EXAM_INFO) {
				isLoading.value = false
				if (response.containsKey("success") && response.getBoolean("success")) {
					examInfo.value = response.getJSONObject("data")
				}
			}
		}
	}
	
	Scaffold(modifier = Modifier
		.fillMaxSize()
		.background(MaterialTheme.colorScheme.surface),
	         topBar = {
		         TopAppBar(title = { Text(examSummary.getString("title") ?: "考试详情") },
		                   navigationIcon = {
			                   IconButton(onClick = onBack) {
				                   Icon(Icons.AutoMirrored.Filled.ArrowBack,
				                        contentDescription = "返回")
			                   }
		                   },
		                   actions = {
			                   IconButton(onClick = {
				                   openExamInBrowser(context, examSummary.getIntValue("id"))
			                   }) {
				                   Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "打开网页版")
			                   }
		                   },
		                   windowInsets = WindowInsets(0),
		                   colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))
	         },
	         bottomBar = {
		         Box(modifier = Modifier
			         .fillMaxWidth()
			         .padding(16.dp)) {
			         Button(onClick = onStartExam,
			                modifier = Modifier.fillMaxWidth(),
			                enabled = !isLoading.value) {
				         Text("开始答题", style = MaterialTheme.typography.titleMedium)
			         }
		         }
	         }) { innerPadding ->
		Box(modifier = Modifier
			.fillMaxSize()
			.padding(top = innerPadding.calculateTopPadding())) {
			if (isLoading.value) {
				CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
			}
			else {
				examInfo.value?.let { info ->
					Column(modifier = Modifier
						.fillMaxSize()
						.verticalScroll(rememberScrollState())
						.padding(horizontal = 16.dp)
						.padding(bottom = innerPadding.calculateBottomPadding() + 16.dp),
					       verticalArrangement = Arrangement.spacedBy(16.dp)) {
						
						Text(text = "考试详情", style = MaterialTheme.typography.titleMedium)
						ElevatedCard(modifier = Modifier.fillMaxWidth()) {
							Column(modifier = Modifier.padding(16.dp),
							       verticalArrangement = Arrangement.spacedBy(12.dp)) {
								DetailRow("当前状态",
								          when (info.getJSONObject("result")
									          ?.getInteger("status")) {
									          0 -> "未开始"
									          1 -> "进行中"
									          2 -> "已提交"
									          else -> "未知"
								          })
								DetailRow("总分", "${info.getString("total_score")} 分")
								DetailRow("题目数量", "${info.getString("problem_count")} 题")
								val limit = info.getInteger("limit")
								if (limit != null && limit > 0) {
									DetailRow("限时", "$limit 分钟")
								}
								DetailRow("计分方式", when (info.getInteger("way_of_score")) {
									1 -> "最高分"
									2 -> "最后一次"
									else -> "普通"
								})
								DetailRow("允许重试", "${info.getInteger("max_retry")} 次")
								DetailRow("手动阅卷",
								          if (info.getInteger("is_manual_review") == 1) "是" else "否")
								DetailRow("强制确认",
								          if (info.getBoolean("force_confirm") == true) "是" else "否")
								HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp),
								                  color = MaterialTheme.colorScheme.outlineVariant.copy(
									                  alpha = 0.5f))
								DetailRow("开始时间",
								          formatTimestampMillis(info.getLong("start_time")))
								DetailRow("截止时间",
								          formatTimestampMillis(info.getLong("deadline")))
								if (info.getBoolean("limit_early_submission") == true) {
									DetailRow("限制早交",
									          "开启 (${info.getInteger("limit_early_submission_time")} 分钟)")
								}
							}
						}
						
						Text(text = "监考规则与限制", style = MaterialTheme.typography.titleMedium)
						ElevatedCard(modifier = Modifier.fillMaxWidth()) {
							Column(modifier = Modifier.padding(16.dp),
							       verticalArrangement = Arrangement.spacedBy(12.dp)) {
								DetailRow("在线监考",
								          if (info.getInteger("online_proctor") == 1) "开启" else "关闭")
								DetailRow("随机人脸",
								          if (info.getInteger("web_random_take_face_photo") == 1) "开启" else "关闭")
								DetailRow("人脸识别",
								          if (info.getJSONObject("face_auth_status")
										          ?.getInteger("online_proctor") == 1) "开启"
								          else "关闭")
								DetailRow("切屏监测",
								          if (info.getInteger("page_switch_detection") == 1) "开启" else "关闭")
								DetailRow("截屏保护",
								          if (info.getInteger("app_capture_screen") == 1 || info.getInteger(
										          "open_screen_cuts") == 1) "开启"
								          else "关闭")
								DetailRow("离线考试",
								          if (info.getBoolean("is_offline") == true) "是" else "否")
								DetailRow("加密传输",
								          if (info.getString("encrypt") == "True") "是" else "否")
								val restriction = info.getString("access_restriction_info")
								if (!restriction.isNullOrBlank()) {
									DetailRow("进入限制", restriction)
								}
							}
						}
						
						Text(text = "考生身份", style = MaterialTheme.typography.titleMedium)
						ElevatedCard(modifier = Modifier.fillMaxWidth()) {
							Row(modifier = Modifier.padding(16.dp),
							    verticalAlignment = Alignment.CenterVertically) {
								val user = info.getJSONObject("user")
								AsyncImage(model = user?.getString("avatar"),
								           contentDescription = "考生头像",
								           modifier = Modifier
									           .size(48.dp)
									           .clip(CircleShape),
								           contentScale = ContentScale.Crop)
								Spacer(modifier = Modifier.width(16.dp))
								Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
									DetailRow("姓名", user?.getString("user_name") ?: "未知")
									DetailRow("学号", user?.getString("school_number") ?: "未知")
								}
							}
						}
						val description = info.getString("description")
						if (!description.isNullOrBlank()) {
							Text(text = "考试说明", style = MaterialTheme.typography.titleMedium)
							Text(text = description,
							     style = MaterialTheme.typography.bodyMedium,
							     color = MaterialTheme.colorScheme.onSurfaceVariant)
						}
						
						Spacer(modifier = Modifier.height(32.dp))
					}
				}
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamPaperScreen(examSummary: JSONObject, onBack: () -> Unit) {
	val context = LocalContext.current
	val problemList = remember { mutableStateOf<List<JSONObject>>(emptyList()) }
	val isLoading = remember { mutableStateOf(true) }
	val model = remember { RainClassModel(context) }
	val message by model.message.observeAsState()
	val answers = remember { mutableStateMapOf<Int, String>() }
	
	LaunchedEffect(Unit) {
		model.getProblem(examSummary.getIntValue("id"))
	}
	
	LaunchedEffect(message) {
		message?.let { (what, response) ->
			if (what == RainClassModel.GET_PROBLEM_INFO) {
				isLoading.value = false
				if (response.containsKey("errcode") && response.getInteger("errcode") == 0) {
					val data = response.getJSONObject("data")
					if (data != null) {
						val problems = data.getJSONArray("problems")
						if (problems != null) {
							problemList.value = problems.map { it as JSONObject }
						}
					}
				}
			}
		}
	}
	
	Scaffold(modifier = Modifier
		.fillMaxSize()
		.background(MaterialTheme.colorScheme.surface),
	         topBar = {
		         TopAppBar(title = { Text(examSummary.getString("title") ?: "正在考试") },
		                   navigationIcon = {
			                   IconButton(onClick = onBack) {
				                   Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
			                   }
		                   },
		                   windowInsets = WindowInsets(0),
		                   colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))
	         },
	         bottomBar = {
		         Box(modifier = Modifier
			         .fillMaxWidth()
			         .padding(16.dp)) {
			         Button(onClick = { /* TODO: Submit exam */ },
			                modifier = Modifier.fillMaxWidth(),
			                enabled = !isLoading.value) {
				         Text("提交试卷", style = MaterialTheme.typography.titleMedium)
			         }
		         }
	         }) { innerPadding ->
		Box(modifier = Modifier
			.fillMaxSize()
			.padding(innerPadding)) {
			if (isLoading.value) {
				CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
			}
			else {
				LazyColumn(modifier = Modifier.fillMaxSize(),
				           contentPadding = PaddingValues(16.dp),
				           verticalArrangement = Arrangement.spacedBy(24.dp)) {
					items(problemList.value) { problem ->
						ProblemItem(problem = problem,
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
			Text(text = "第 ${problem.getIntValue("index") + 1} 题",
			     style = MaterialTheme.typography.titleMedium,
			     fontWeight = FontWeight.Bold)
			Spacer(modifier = Modifier.width(8.dp))
			Text(text = "(${problem.getString("TypeText")})",
			     style = MaterialTheme.typography.labelMedium,
			     color = MaterialTheme.colorScheme.onSurfaceVariant)
			Spacer(modifier = Modifier.weight(1f))
			Text(text = "${problem.getString("Score")} 分",
			     style = MaterialTheme.typography.labelMedium,
			     color = MaterialTheme.colorScheme.primary)
		}
		
		AndroidView(factory = { context ->
			TextView(context).apply {
				text = Html.fromHtml(problem.getString("Body"), Html.FROM_HTML_MODE_COMPACT)
				textSize = 16f
				setTextColor(android.graphics.Color.BLACK)
			}
		}, modifier = Modifier.fillMaxWidth())
		
		if (problem.getString("Type") == "ShortAnswer") {
			OutlinedTextField(value = answer,
			                  onValueChange = onAnswerChange,
			                  modifier = Modifier.fillMaxWidth(),
			                  placeholder = { Text("请输入你的回答") },
			                  minLines = 3)
		}
		else {
			Text(text = "暂不支持该题型答题",
			     style = MaterialTheme.typography.bodySmall,
			     color = MaterialTheme.colorScheme.error)
		}
	}
}


@Composable fun DetailRow(label: String, value: String) {
	Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
		Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
		Text(text = value, fontWeight = FontWeight.Medium)
	}
}

@Composable fun AccountScreen() {
	val context = LocalContext.current
	val userInfo = remember { mutableStateOf<JSONObject?>(null) }
	val isLoginRequired = remember { mutableStateOf(false) }
	val isLoading = remember { mutableStateOf(true) }
	val scrollState = rememberScrollState()
	val model = remember { RainClassModel(context) }
	val message by model.message.observeAsState()
	
	LaunchedEffect(message) {
		message?.let { (what, response) ->
			if (what == RainClassModel.GET_USER_INFO) {
				isLoading.value = false
				if (response.containsKey("op") && response.getString("op") == "web_redirect") {
					isLoginRequired.value = true
				}
				else {
					userInfo.value = response.getJSONObject("data")?.getJSONObject("user_profile")
					isLoginRequired.value = false
				}
			}
		}
	}
	
	fun getUserInfo() {
		isLoading.value = true
		model.getUserInfo()
	}
	
	LaunchedEffect(Unit) {
		getUserInfo()
	}
	
	Column(modifier = Modifier
		.fillMaxSize()
		.verticalScroll(scrollState)
		.padding(16.dp),
	       horizontalAlignment = Alignment.CenterHorizontally) {
		if (isLoading.value) {
			Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
				CircularProgressIndicator()
			}
		}
		else if (isLoginRequired.value) {
			Text(text = "请扫码登录雨课堂", style = MaterialTheme.typography.titleMedium)
			Spacer(modifier = Modifier.height(16.dp))
			Card(elevation = CardDefaults.cardElevation()) {
				AndroidView(factory = { ctx ->
					ImageView(ctx).apply {
						model.contextUtil.loginByQrCode(TargetHost.YU_KE_TANG, this) {
							getUserInfo()
						}
					}
				}, modifier = Modifier.fillMaxSize())
			}
		}
		else if (userInfo.value != null) {
			val info = userInfo.value!!
			AsyncImage(model = info.getString("avatar"),
			           contentDescription = "用户头像",
			           modifier = Modifier
				           .size(80.dp)
				           .clip(CircleShape),
			           contentScale = ContentScale.Crop)
			Spacer(modifier = Modifier.height(16.dp))
			ElevatedCard(modifier = Modifier
				.fillMaxWidth()
				.widthIn(max = 600.dp),
			             elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
				Column(modifier = Modifier.padding(16.dp),
				       verticalArrangement = Arrangement.spacedBy(12.dp)) {
					Text(text = stringResource(R.string.account_info),
					     style = MaterialTheme.typography.titleMedium)
					HorizontalDivider()
					AccountInfoRow(label = stringResource(R.string.name),
					               value = info.getString("name") ?: "未知")
					AccountInfoRow(label = stringResource(R.string.student_id),
					               value = info.getString("school_number") ?: "未知")
					AccountInfoRow(label = stringResource(R.string.university),
					               value = info.getString("school") ?: "未知")
					AccountInfoRow(label = stringResource(R.string.phone),
					               value = info.getString("phone_number") ?: "未知")
					AccountInfoRow(label = stringResource(R.string.email),
					               value = info.getString("email") ?: "无")
				}
			}
		}
	}
}

@Composable fun AccountInfoRow(label: String, value: String) {
	Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
		Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
		SelectionContainer {
			Text(text = value, fontWeight = FontWeight.Bold)
		}
	}
}

fun syncCookiesToWeb(context: Context) {
	val myCm = com.sysu.edu.api.CookieManager(context)
	val webCm = android.webkit.CookieManager.getInstance()
	webCm.setAcceptCookie(true)
	listOf("www.yuketang.cn", "yuketang.cn", "xuetangx.com", "examination.xuetangx.com").forEach { host ->
		myCm.get(host).forEach { cookie ->
			webCm.setCookie(host, cookie)
		}
	}
	webCm.flush()
}

fun openExamInBrowser(context: Context, examId: Int) {
	syncCookiesToWeb(context)
	val url = "https://examination.xuetangx.com/exam/$examId?isFrom=2"
	val intent = Intent(context, BrowserActivity::class.java).apply {
		data = url.toUri()
	}
	context.startActivity(intent)
}

