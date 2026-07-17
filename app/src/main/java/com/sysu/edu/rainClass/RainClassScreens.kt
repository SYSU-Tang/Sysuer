package com.sysu.edu.rainClass

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.widget.ImageView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.R
import com.sysu.edu.api.ContextUtil
import com.sysu.edu.api.CookieManager
import com.sysu.edu.api.HttpManager
import com.sysu.edu.api.TargetHost

@OptIn(ExperimentalMaterial3Api::class) @Composable fun CourseScreen(onRequestScrollToAccount: () -> Unit = {}) {
	var searchQuery by remember { mutableStateOf("") }
	var active by remember { mutableStateOf(false) }
	val context = LocalContext.current
	val contextUtil = remember { ContextUtil(context) }
	val courseList = remember { mutableStateOf<List<JSONObject>>(emptyList()) }
	val isLoading = remember { mutableStateOf(true) }
	
	val http = remember {
		HttpManager(object : Handler(Looper.getMainLooper()) {
			override fun handleMessage(msg: Message) {
				super.handleMessage(msg)
				isLoading.value = false
				when (msg.what) {
					0 -> {
						val response = JSONObject.parseObject(msg.obj.toString())
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
					-1 -> {
						contextUtil.toast(R.string.no_net_connected)
					}
				}
			}
		}).apply {
			cookieManager = CookieManager(context)
		}
	}
	
	fun getCourseList() {
		isLoading.value = true
		http.getRequest("https://www.yuketang.cn/v2/api/web/courses/list?identity=2", 0)
	}
	
	LaunchedEffect(Unit) {
		getCourseList()
	}
	
	Column(modifier = Modifier.fillMaxSize()) {
		SearchBar(query = searchQuery,
		          onQueryChange = { searchQuery = it },
		          onSearch = { active = false },
		          active = active,
		          onActiveChange = { active = it },
		          modifier = Modifier
			          .align(Alignment.CenterHorizontally)
			          .semantics { traversalIndex = 0f },
		          placeholder = { Text("搜索课程") },
		          leadingIcon = {
			          if (active) {
				          IconButton(onClick = { active = false }) {
					          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
				          }
			          } else {
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
		} else {
			LazyColumn(
				modifier = Modifier.fillMaxSize(),
				contentPadding = PaddingValues(vertical = 8.dp),
				verticalArrangement = Arrangement.spacedBy(8.dp)
			) {
				items(courseList.value.size) { index ->
					val courseItem = courseList.value[index]
					val course = courseItem.getJSONObject("course")
					val teacher = courseItem.getJSONObject("teacher")
					
					val termColor = getTermColor(courseItem.getInteger("term"))
					Card(
						modifier = Modifier
							.fillMaxWidth()
							.padding(horizontal = 16.dp),
						colors = CardDefaults.cardColors(
							containerColor = termColor,
							contentColor = Color.White
						)
					) {
						ListItem(
							colors = ListItemDefaults.colors(
								containerColor = Color.Transparent,
								headlineColor = Color.White,
								supportingColor = Color.White.copy(alpha = 0.7f),
								overlineColor = Color.White.copy(alpha = 0.9f)
							),
							overlineContent = {
								Text(
									text = formatTerm(courseItem.getInteger("term")),
									style = MaterialTheme.typography.labelSmall
								)
							},
							headlineContent = { Text(course?.getString("name") ?: "未知课程") },
							supportingContent = {
								SelectionContainer {
									Text("${teacher?.getString("name") ?: "未知教师"} | 课堂号: ${courseItem.getInteger("classroom_id")}")
								}
							},
							leadingContent = {
								AsyncImage(
									model = teacher?.getString("avatar"),
									contentDescription = "教师头像",
									modifier = Modifier.size(40.dp).clip(CircleShape),
									contentScale = ContentScale.Crop
								)
							},
							trailingContent = {
								AsyncImage(
									model = course?.getString("university_mini_logo"),
									contentDescription = "学校Logo",
									modifier = Modifier.size(24.dp)
								)
							}
						)
					}
				}
			}
		}
	}
}

@Composable fun AccountScreen() {
	val context = LocalContext.current
	val contextUtil = remember { ContextUtil(context) }
	val userInfo = remember { mutableStateOf<JSONObject?>(null) }
	val isLoginRequired = remember { mutableStateOf(false) }
	val isLoading = remember { mutableStateOf(true) }
	val scrollState = rememberScrollState()
	
	val http = remember {
		HttpManager(object : Handler(Looper.getMainLooper()) {
			override fun handleMessage(msg: Message) {
				super.handleMessage(msg)
				isLoading.value = false
				when (msg.what) {
					0 -> {
						val response = JSONObject.parseObject(msg.obj.toString())
						if (response.containsKey("op") && response.getString("op") == "web_redirect") {
							isLoginRequired.value = true
						}
						else {
							userInfo.value = response.getJSONObject("data")
								.getJSONObject("user_profile")
							isLoginRequired.value = false
						}
					}
					-1 -> {
						contextUtil.toast(R.string.no_net_connected)
					}
				}
			}
		}).apply {
			cookieManager = CookieManager(context)
		}
	}
	
	fun getUserInfo() {
		isLoading.value = true
		println("getUserInfo")
		http.getRequest("https://www.yuketang.cn/v/course_meta/user_info", 0)
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
						contextUtil.loginByQrCode(TargetHost.YU_KE_TANG, this) {
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
			Card(modifier = Modifier.fillMaxWidth(),
			     elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
				Column(modifier = Modifier.padding(16.dp),
				       verticalArrangement = Arrangement.spacedBy(12.dp)) {
					Text(text = "账户详情", style = MaterialTheme.typography.headlineSmall)
					HorizontalDivider()
					AccountInfoRow(label = "姓名", value = info.getString("name") ?: "未知")
					AccountInfoRow(label = "学号",
					               value = info.getString("school_number") ?: "未知")
					AccountInfoRow(label = "学校", value = info.getString("school") ?: "未知")
					AccountInfoRow(label = "手机号",
					               value = info.getString("phone_number") ?: "未知")
					AccountInfoRow(label = "邮箱", value = info.getString("email") ?: "无")
				}
			}
		}
	}
}

@Composable fun AccountInfoRow(label: String, value: String) {
	Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
		Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
		Text(text = value, fontWeight = FontWeight.Bold)
	}
}

fun formatTerm(term: Int?): String {
	if (term == null) return ""
	val year = term / 100
	val semester = term % 100
	val semesterStr = when (semester) {
		1 -> "秋"
		2 -> "春"
		3 -> "夏"
		else -> semester.toString()
	}
	return "$year $semesterStr"
}

fun getTermColor(term: Int?): Color {
	if (term == null) return Color(0xFF212121)
	return when (term % 100) {
		1 -> Color(0xFF1A237E) // 秋季 - 深蓝
		2 -> Color(0xFF1B5E20) // 春季 - 深绿
		3 -> Color(0xFFB71C1C) // 夏季 - 深红
		else -> Color(0xFF424242)
	}
}
