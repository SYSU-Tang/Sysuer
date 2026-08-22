package com.sysu.edu.home

import android.app.PendingIntent
import android.content.ClipData
import android.content.Intent
import android.text.TextUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Shortcut
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.KeyboardVoice
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Output
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alibaba.fastjson2.JSONObject
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.rememberMarkdownState
import com.sysu.edu.MainActivity
import com.sysu.edu.R
import com.sysu.edu.academic.AgendaActivity
import com.sysu.edu.academic.CourseDetailActivity
import com.sysu.edu.academic.CourseScheduleActivity
import com.sysu.edu.academic.ExamActivity
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.ContextUtil
import com.sysu.edu.api.PreferenceViewModel
import com.sysu.edu.api.TodoManager
import com.sysu.edu.browser.BrowserActivity
import com.sysu.edu.todo.TodoActivity
import com.sysu.edu.todo.TodoEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable internal fun DashboardScreen(
	dashboardViewModel: DashboardViewModel,
	homeViewModel: HomeViewModel,
	spm: PreferenceViewModel,
	todoManager: TodoManager,
                                        ) {
	val context = LocalContext.current
	val activity = remember { context as FragmentActivity }
	val config = remember { ContextUtil(context) }
	val coroutineScope = rememberCoroutineScope()
	LaunchedEffect(Unit) {
		if (spm.isAgree) dashboardViewModel.getTerm()
	}
	LaunchedEffect(Unit) {
		homeViewModel.updateDashboardShortcut.observeForever {
			if (it == true) dashboardViewModel.loadDashboardShortcuts()
		}
	}
	val term by dashboardViewModel.term.collectAsStateWithLifecycle()
	val finalExamWeek by dashboardViewModel.finalExamWeek.collectAsStateWithLifecycle()
	val navigateToCourseDetail by dashboardViewModel.navigateToCourseDetail.collectAsStateWithLifecycle()
	var showToday by rememberSaveable { mutableStateOf(dashboardViewModel.isShowToday.value) }
	var showWeek18 by rememberSaveable { mutableStateOf(dashboardViewModel.isShowWeek18.value) }
	val todayCourses = dashboardViewModel.todayCourses
	val tomorrowCourses = dashboardViewModel.tomorrowCourses
	val week18Exams = dashboardViewModel.week18Exams
	val week19Exams = dashboardViewModel.week19Exams
	val todayExamIndex by dashboardViewModel.todayExamIndex.collectAsStateWithLifecycle()
	val nextClassIndex by dashboardViewModel.progressCurrent.collectAsStateWithLifecycle()
	val nextClassMarkdown by dashboardViewModel.nextClassMarkdown.collectAsStateWithLifecycle()
	val clipboard = LocalClipboard.current
	val visibleSections by spm.dashboardLiveData.observeAsState((0..5).map { "$it" }.toMutableSet())
	val selectedSet = visibleSections?.mapNotNull { it?.toIntOrNull() }?.toSet().orEmpty()
	var showActionItem by remember { mutableStateOf<JSONObject?>(null) }
	var showOrderDialog by rememberSaveable { mutableStateOf(false) }
	
	LaunchedEffect(navigateToCourseDetail) {
		navigateToCourseDetail?.let { json ->
			context.startActivity(Intent(context, CourseDetailActivity::class.java).putExtra("id", json.getString("teachingClassId")).putExtra("code", json.getString("courseNum")).putExtra("class", json.getString("teachingClassNum")),
			                      ActivityOptionsCompat.makeSceneTransitionAnimation(activity).toBundle())
			dashboardViewModel.onNavigatedToCourseDetail()
		}
	}
	LaunchedEffect(term) {
		if (term.isNotEmpty()) dashboardViewModel.getWeek(term)
	}
	
	DashboardOrderDialog(
		show = showOrderDialog,
		onDismiss = { showOrderDialog = false },
		dashboardViewModel = dashboardViewModel,
	                    )
	
	DashboardActionDialog(
		item = showActionItem,
		onDismiss = { showActionItem = null },
		onShowOrder = { showActionItem = null; showOrderDialog = true },
		dashboardViewModel = dashboardViewModel,
		homeViewModel = homeViewModel,
		config = config,
	                     )
	
	FlowRow(modifier = Modifier
		.fillMaxSize()
		.nestedScroll(rememberNestedScrollInteropConnection())
		.verticalScroll(rememberScrollState())
		.padding(dimensionResource(R.dimen.horizontal_padding), dimensionResource(R.dimen.vertical_padding)),
	        maxItemsInEachRow = 2,
	        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vertical_margin))) {
		if (0 in selectedSet) ShortcutSection(dashboardViewModel, config, activity) { showActionItem = it }
		
		if (1 in selectedSet || 2 in selectedSet) {
			ScheduleSection(nextClassMarkdown = nextClassMarkdown, dateText = dashboardViewModel.dateText, onNextClassClick = {
				context.startActivity(Intent(context, CourseScheduleActivity::class.java), ActivityOptionsCompat.makeSceneTransitionAnimation(activity).toBundle())
			}, onTimeCardClick = {
				context.startActivity(Intent(context, AgendaActivity::class.java), ActivityOptionsCompat.makeSceneTransitionAnimation(activity).toBundle())
			})
		}
		
		if (3 in selectedSet) {
			LaunchedEffect(Unit) {
				dashboardViewModel.getTodayCourses()
			}
			CourseSection(todayCourses = todayCourses, tomorrowCourses = tomorrowCourses, showToday = showToday, nextClassIndex = nextClassIndex, onToggle = { showToday = it; dashboardViewModel.setShowToday(it) }, onCourseClick = { json ->
				context.startActivity(Intent(context, CourseDetailActivity::class.java).putExtra("code", json.getString("courseNum")).putExtra("class", json.getString("classesNum")),
				                      ActivityOptionsCompat.makeSceneTransitionAnimation(activity).toBundle())
			}, onCourseLongClick = { json, key ->
				coroutineScope.launch {
					clipboard.setClipEntry(ClipData.newPlainText(key, json.getString(key)).toClipEntry())
				}
				config.toast(R.string.copy_successfully)
			}, activity = activity)
		}
		
		if (4 in selectedSet) {
			LaunchedEffect(term) {
				if (term.isNotEmpty()) {
					dashboardViewModel.getExamWeekName(term)
				}
			}
			
			LaunchedEffect(finalExamWeek) {
				if (term.isNotEmpty() && finalExamWeek.isNotEmpty()) {
					dashboardViewModel.getExams(term, finalExamWeek)
				}
			}
			
			ExamSection(week18Exams = week18Exams, week19Exams = week19Exams, showWeek18 = showWeek18, todayExamIndex = todayExamIndex, onToggle = { showWeek18 = it; dashboardViewModel.setShowWeek18(it) }, onExamClick = { json ->
				dashboardViewModel.getSelectedCourses(json.getString("examSubjectName"))
			}, onExamLongClick = { text ->
				coroutineScope.launch {
					clipboard.setClipEntry(ClipData.newPlainText("text", text).toClipEntry())
				}
				config.toast(R.string.copy_successfully)
			}, activity = activity, coroutineScope = coroutineScope)
		}
		
		if (5 in selectedSet) {
			LaunchedEffect(Unit) { todoManager.init() }
			val todoList by todoManager.todoModel.todoList.observeAsState(emptyList())
			var todoRefreshKey by rememberSaveable { mutableIntStateOf(0) }
			LaunchedEffect(todoRefreshKey) {
				val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
				todoManager.refresh("(due_date = ? OR ddl = ?)", arrayOf(today, today))
			}
			todoManager.refreshListener = { todoRefreshKey++ }
			TodoSection(todoList = todoList, onViewAllClick = {
				context.startActivity(Intent(context, TodoActivity::class.java), ActivityOptionsCompat.makeSceneTransitionAnimation(activity).toBundle())
			}, todoManager = todoManager)
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class) @Composable private fun DashboardOrderDialog(
	show: Boolean,
	onDismiss: () -> Unit,
	dashboardViewModel: DashboardViewModel,
                                                                                    ) {
	if (!show) return
	val shortcuts = dashboardViewModel.orderShortcuts
	val confirmText = stringResource(R.string.confirm)
	val orderText = stringResource(R.string.service_order)
	ModalBottomSheet(onDismissRequest = onDismiss) {
		Column(modifier = Modifier.fillMaxWidth()) {
			Text(orderText, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp))
			LazyColumn(modifier = Modifier.fillMaxWidth()) {
				itemsIndexed(shortcuts, key = { _, entity -> entity.shortcutId ?: 0 }) { index, entity ->
					val shortcut = remember(entity.shortcutId) { JSONObject.parse(entity.shortcutJson ?: "") }
					val name = shortcut.getString("name") ?: ""
					ListItem(overlineContent = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium) }, leadingContent = {
						Row {
							IconButton(onClick = {
								if (index > 0) dashboardViewModel.moveOrderShortcut(index, index - 1)
							}, enabled = index > 0) {
								Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = null)
							}
							IconButton(onClick = {
								if (index < shortcuts.lastIndex) dashboardViewModel.moveOrderShortcut(index, index + 1)
							}, enabled = index < shortcuts.lastIndex) {
								Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
							}
						}
					}, modifier = Modifier.animateItem()) {}
				}
			}
			Row(modifier = Modifier
				.fillMaxWidth()
				.padding(end = 24.dp, bottom = 24.dp), horizontalArrangement = Arrangement.End) {
				TextButton(onClick = {
					dashboardViewModel.saveOrderShortcuts()
					onDismiss()
				}) { Text(confirmText) }
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class) @Composable private fun DashboardActionDialog(
	item: JSONObject?,
	onDismiss: () -> Unit,
	onShowOrder: () -> Unit,
	dashboardViewModel: DashboardViewModel,
	homeViewModel: HomeViewModel,
	config: ContextUtil,
                                                                                                                       ) {
	if (item == null) return
	val context = LocalContext.current
	val coroutineScope = rememberCoroutineScope()
	val itemId = item.getIntValue("id")
	var isServiceCollected by remember { mutableStateOf(false) }
	var isShortcutCollected by remember { mutableStateOf(false) }
	val name = item.getString("name", "")
	val description = item.getString("description", "")
	val url = item.getString("url", "")
	val markdown = StringBuilder("### $name\n$description")
	if (url.isNotBlank()) markdown.append("\n`$url`")
	
	LaunchedEffect(item) {
		isServiceCollected = dashboardViewModel.isServiceCollected(itemId)
		isShortcutCollected = dashboardViewModel.isDashboardShortcutCollected(itemId)
	}
	
	ModalBottomSheet(onDismissRequest = onDismiss) {
		Column(modifier = Modifier.fillMaxWidth()) {
			Card(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = dimensionResource(R.dimen.horizontal_margin), vertical = dimensionResource(R.dimen.vertical_margin)),
				shape = MaterialTheme.shapes.medium,
			    ) {
				Markdown(
					rememberMarkdownState("$markdown"),
					colors = markdownColor(),
					typography = markdownTypography(h3 = MaterialTheme.typography.titleMediumEmphasized),
					modifier = Modifier.padding(dimensionResource(R.dimen.content_padding)),
				        )
			}
			
			FlowRow(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = dimensionResource(R.dimen.horizontal_margin), vertical = dimensionResource(R.dimen.vertical_margin)),
				horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_gap)),
				verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vertical_gap)),
			       ) {
				GenericTonalButton(image = if (isServiceCollected) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, text = stringResource(if (isServiceCollected) R.string.cancel_collect else R.string.collect)) {
					isServiceCollected = !isServiceCollected
					coroutineScope.launch {
						if (isServiceCollected) {
							dashboardViewModel.addService(itemId, item.toJSONString(), null)
							config.toast(R.string.collect_success)
						}
						else {
							dashboardViewModel.deleteService(itemId)
							config.toast(R.string.cancel_collect_success)
						}
					}
				}
				
				GenericTonalButton(image = if (isShortcutCollected) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.Shortcut, text = stringResource(if (isShortcutCollected) R.string.cancel_add_shortcut else R.string.add_to_dashboard)) {
					isShortcutCollected = !isShortcutCollected
					coroutineScope.launch {
						if (isShortcutCollected) {
							dashboardViewModel.addDashboardShortcut(itemId, item.toJSONString(), null)
							config.toast(R.string.add_shortcut_success)
						}
						else {
							dashboardViewModel.deleteDashboardShortcut(itemId)
							config.toast(R.string.cancel_add_shortcut_success)
						}
						dashboardViewModel.loadDashboardShortcuts()
						homeViewModel.updateDashboardShortcut.value = true
					}
				}
				
				GenericTonalButton(image = Icons.Rounded.Output, text = stringResource(R.string.add_to_launcher)) {
					if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
						val intent = when {
							item.containsKey("activity") -> {
								try {
									Intent(context, Class.forName(context.packageName + item.getString("activity")))
								} catch (_: Exception) {
									Intent(context, MainActivity::class.java)
								}
							}
							item.containsKey("url") -> Intent(context, BrowserActivity::class.java).setData(item.getString("url").toUri())
							else -> Intent(context, MainActivity::class.java)
						}
						val info = ShortcutInfoCompat.Builder(context, "$itemId").setShortLabel(name).setLongLabel(name).setIcon(IconCompat.createWithResource(context, R.mipmap.icon)).setIntent(intent.setAction(Intent.ACTION_VIEW)).build()
						ShortcutManagerCompat.requestPinShortcut(context, info, PendingIntent.getBroadcast(context, 0, ShortcutManagerCompat.createShortcutResultIntent(context, info), PendingIntent.FLAG_IMMUTABLE).intentSender)
					}
					else config.toast(R.string.fail_to_add_shortcut)
				}
				
				GenericTonalButton(image = Icons.Rounded.ClearAll, text = stringResource(R.string.service_order)) {
					onShowOrder()
				}
				
				GenericTonalButton(image = Icons.Rounded.KeyboardVoice, text = stringResource(R.string.feedback)) {
					context.startActivity(Intent(Intent.ACTION_VIEW).setData("https://github.com/SYSU-Tang/Sysuer/issues/new?title=反馈：服务->$name&labels=bug,crash-report".toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
				}
				
				GenericTonalButton(image = Icons.Rounded.Link, text = stringResource(R.string.open_as_url)) {
					val itemUrl = item.getString("url")
					if (!TextUtils.isEmpty(itemUrl)) context.startActivity(Intent(context, BrowserActivity::class.java).setData(itemUrl.toUri()))
				}
				
				GenericTonalButton(image = Icons.Rounded.Book, text = stringResource(R.string.guide)) {
					if (item.containsKey("doc")) context.startActivity(Intent(context, BrowserActivity::class.java).setData("https://sysu-tang.github.io/sysuer-website${CommonUtil.trim(item.getString("doc"))}".toUri()))
					else config.toast(R.string.undeveloped_warning)
				}
			}
		}
	}
}

@Composable private fun ShortcutSection(
	vm: DashboardViewModel,
	config: ContextUtil,
	activity: FragmentActivity,
	onShowActionDialog: (JSONObject) -> Unit,
                                       ) {
	val context = LocalContext.current
	val scan = stringResource(R.string.scan)
	val qrcode = stringResource(R.string.qrcode)
	val courseSchedule = stringResource(R.string.course_schedule)
	val shortcuts = vm.dashboardShortcuts
	LaunchedEffect(Unit) { vm.loadDashboardShortcuts() }
	FlowRow(modifier = Modifier
		.fillMaxWidth()
		.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.Center, verticalArrangement = Arrangement.Center) {
		ButtonGroup(horizontalArrangement = Arrangement.Center, overflowIndicator = { menuState ->
			FilledTonalIconButton(onClick = {
				if (menuState.isShowing) menuState.dismiss()
				else menuState.show()
			}) {
				Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.more))
			}
		}) {
			clickableItem(label = scan, icon = {
				Icon(Icons.Rounded.QrCodeScanner, contentDescription = stringResource(R.string.scan))
			}, onClick = { vm.openWechatScan() })
			clickableItem(label = qrcode, icon = {
				Icon(Icons.Rounded.QrCode2, contentDescription = stringResource(R.string.qrcode))
			}, onClick = { vm.openQrCode() })
			clickableItem(label = courseSchedule, icon = {
				Icon(Icons.Rounded.CalendarMonth, contentDescription = stringResource(R.string.course_schedule))
			}, onClick = { context.startActivity(Intent(context, CourseScheduleActivity::class.java), ActivityOptionsCompat.makeSceneTransitionAnimation(activity).toBundle()) })
		}
		shortcuts.forEach { entity ->
			val shortcutJson = entity.shortcutJson ?: return@forEach
			val shortcut = remember(entity.shortcutId) { JSONObject.parse(shortcutJson) }
			val name = shortcut.getString("name") ?: return@forEach
			LongClickButton(onClick = {
				val act = shortcut.getString("activity")
				val url = shortcut.getString("url")
				when {
					!act.isNullOrEmpty() -> {
						try {
							Intent(context, Class.forName(context.packageName + act)).takeIf {
								it.resolveActivity(context.packageManager) != null
							}?.let {
								context.startActivity(it, ActivityOptionsCompat.makeSceneTransitionAnimation(activity).toBundle())
							}
						} catch (_: Exception) {
							config.toast(R.string.activity_not_found)
						}
					}
					!url.isNullOrEmpty() -> {
						context.startActivity(Intent(context, BrowserActivity::class.java).setData(url.toUri()), ActivityOptionsCompat.makeSceneTransitionAnimation(activity).toBundle())
					}
					else -> config.toast(R.string.undeveloped)
				}
			}, icon = Icons.Rounded.Star, label = name, onLongClick = { onShowActionDialog(shortcut) })
		}
	}
}

@Composable private fun ScheduleSection(
	nextClassMarkdown: String,
	dateText: String,
	onNextClassClick: () -> Unit,
	onTimeCardClick: () -> Unit,
                                       ) {
	Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_padding))) {
		OutlinedCard(border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.weight(1f), onClick = onNextClassClick) {
			if (nextClassMarkdown.isNotEmpty()) {
				Markdown(rememberMarkdownState(nextClassMarkdown), colors = markdownColor(), typography = markdownTypography(h3 = MaterialTheme.typography.titleMediumEmphasized), modifier = Modifier.padding(12.dp))
			}
		}
		OutlinedCard(border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.weight(1f), onClick = onTimeCardClick) {
			Column(modifier = Modifier.padding(dimensionResource(R.dimen.content_padding))) {
				Text(text = dateText, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
			}
		}
	}
}

@OptIn(ExperimentalFoundationApi::class) @Composable private fun CourseSection(
	todayCourses: SnapshotStateList<JSONObject>,
	tomorrowCourses: SnapshotStateList<JSONObject>,
	showToday: Boolean,
	nextClassIndex: Int = 0,
	onToggle: (Boolean) -> Unit,
	onCourseClick: (JSONObject) -> Unit,
	onCourseLongClick: (JSONObject, String) -> Unit,
	activity: FragmentActivity,
                                                                              ) {
	val context = LocalContext.current
	var courses = if (showToday) todayCourses else tomorrowCourses
	var selectedIndex by remember { mutableIntStateOf(0) }
	LaunchedEffect(selectedIndex) {
		onToggle(selectedIndex == 0)
		courses = if (selectedIndex == 0) todayCourses else tomorrowCourses
	}
	Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
		CardTitle(Icons.Rounded.School, text = stringResource(R.string.course)) {
			context.startActivity(Intent(context, CourseScheduleActivity::class.java), ActivityOptionsCompat.makeSceneTransitionAnimation(activity).toBundle())
		}
		
		SingleChoiceSegmentedButtonRow {
			listOf(R.string.today, R.string.recent).forEachIndexed { index, label ->
				SegmentedButton(
					shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
					onClick = { selectedIndex = index },
					selected = selectedIndex == index,
					icon = {},
				               ) {
					Text(stringResource(label))
				}
			}
		}
	}
	
	ElevatedCard(modifier = Modifier.fillMaxWidth()) {
		if (courses.isEmpty()) Text(text = stringResource(R.string.noClass),
		                            style = MaterialTheme.typography.titleLargeEmphasized,
		                            modifier = Modifier.padding(dimensionResource(R.dimen.horizontal_padding), dimensionResource(R.dimen.vertical_padding)))
		else Row(modifier = Modifier
			.fillMaxWidth()
			.height(IntrinsicSize.Max)
			.horizontalScroll(rememberScrollState(nextClassIndex)), verticalAlignment = Alignment.CenterVertically) {
			courses.forEachIndexed { index, item ->
				if (index > 0) VerticalDivider()
				CourseItem(item = item, onClick = { onCourseClick(item) }, onLongClick = { key -> onCourseLongClick(item, key) })
			}
		}
	}
}

@OptIn(ExperimentalFoundationApi::class) @Composable private fun CourseItem(
	item: JSONObject,
	onClick: () -> Unit,
	onLongClick: (String) -> Unit,
                                                                           ) {
	val status = item.getString("status") ?: "after"
	val isBefore = status == "before"
	val alpha = if (isBefore) 0.64f else 1.0f
	val backgroundColor = when (status) {
		"in" -> MaterialTheme.colorScheme.surfaceDim
		"before" -> Color.Transparent
		else -> MaterialTheme.colorScheme.surface
	}
	val clipboard = LocalClipboard.current
	val coroutineScope = rememberCoroutineScope()
	Card(colors = CardDefaults.cardColors(containerColor = backgroundColor), shape = RoundedCornerShape(0.dp), modifier = Modifier
		.fillMaxHeight()
		.combinedClickable(onClick = onClick, onLongClick = { onLongClick("courseName") })
		.alpha(alpha)) {
		Column(modifier = Modifier.padding(dimensionResource(R.dimen.horizontal_margin), dimensionResource(R.dimen.vertical_margin))) {
			Spacer(modifier = Modifier.height(4.dp))
			Text(text = item.getString("courseName", ""), style = if (isBefore) MaterialTheme.typography.titleMedium
			else MaterialTheme.typography.titleMediumEmphasized, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier
				.fillMaxWidth()
				.align(Alignment.CenterHorizontally))
			listOf("teachingPlace", "time", "teacherName", "course").zip(listOf(Icons.Rounded.LocationOn, Icons.Rounded.Timer, Icons.Rounded.AccountCircle, Icons.Rounded.CalendarMonth)).forEach { (key, icon) ->
				val text = item.getString(key, "")
				GenericButton(icon = icon, text = text) {
					coroutineScope.launch {
						clipboard.setClipEntry(ClipData.newPlainText(key, text).toClipEntry())
					}
				}
			}
		}
	}
}

@Composable private fun ExamSection(
	week18Exams: SnapshotStateList<JSONObject>,
	week19Exams: SnapshotStateList<JSONObject>,
	showWeek18: Boolean,
	todayExamIndex: Int = 0,
	onToggle: (Boolean) -> Unit,
	onExamClick: (JSONObject) -> Unit,
	onExamLongClick: (String) -> Unit,
	activity: FragmentActivity,
	coroutineScope: CoroutineScope,
                                   ) {
	val context = LocalContext.current
	val exams = if (showWeek18) week18Exams else week19Exams
	var selectedIndex by remember { mutableIntStateOf(if (showWeek18) 0 else 1) }
	LaunchedEffect(selectedIndex) {
		onToggle(selectedIndex == 0)
	}
	
	Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
		CardTitle(R.drawable.exam, text = stringResource(R.string.exam)) {
			context.startActivity(Intent(context, ExamActivity::class.java), ActivityOptionsCompat.makeSceneTransitionAnimation(activity).toBundle())
		}
		SingleChoiceSegmentedButtonRow {
			listOf(R.string.week18, R.string.week19).forEachIndexed { index, label ->
				SegmentedButton(
					shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
					onClick = { selectedIndex = index },
					selected = selectedIndex == index,
					icon = {},
				               ) {
					Text(stringResource(label))
				}
			}
		}
	}
	
	ElevatedCard(modifier = Modifier.fillMaxWidth()) {
		if (exams.isEmpty()) Text(text = stringResource(R.string.noExam), style = MaterialTheme.typography.titleLargeEmphasized, modifier = Modifier.padding(dimensionResource(R.dimen.horizontal_padding), dimensionResource(R.dimen.vertical_padding)))
		else {
			Row(modifier = Modifier
				.fillMaxWidth()
				.height(IntrinsicSize.Max)
				.horizontalScroll(rememberScrollState(todayExamIndex)), verticalAlignment = Alignment.CenterVertically) {
				exams.forEachIndexed { index, exam ->
					if (index > 0) VerticalDivider()
					ExamItem(exam = exam, onClick = { onExamClick(exam) }, onLongClick = { text -> onExamLongClick(text) }, coroutineScope = coroutineScope)
				}
			}
		}
	}
}

@OptIn(ExperimentalFoundationApi::class) @Composable private fun ExamItem(
	exam: JSONObject,
	onClick: () -> Unit,
	onLongClick: (String) -> Unit,
	coroutineScope: kotlinx.coroutines.CoroutineScope,
                                                                         ) {
	val status = exam.getString("status") ?: "after"
	val isBefore = status == "before"
	val alpha = if (isBefore) 0.64f else 1.0f
	val backgroundColor = when (status) {
		"in" -> MaterialTheme.colorScheme.surfaceDim
		"before" -> Color.Transparent
		else -> MaterialTheme.colorScheme.surface
	}
	val weeks = stringArrayResource(R.array.weeks)
	val clipboard = LocalClipboard.current
	Card(colors = CardDefaults.cardColors(containerColor = backgroundColor), modifier = Modifier
		.fillMaxHeight()
		.combinedClickable(onClick = onClick, onLongClick = { onLongClick(exam.getString("examSubjectName") ?: "") })
		.alpha(alpha)) {
		Column(modifier = Modifier.padding(dimensionResource(R.dimen.horizontal_margin), dimensionResource(R.dimen.vertical_margin))) {
			Spacer(modifier = Modifier.height(4.dp))
			Text(text = exam.getString("examSubjectName", ""), style = if (isBefore) MaterialTheme.typography.titleMedium
			else MaterialTheme.typography.titleMediumEmphasized, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier
				.fillMaxWidth()
				.align(Alignment.CenterHorizontally))
			val examDate = exam.getString("examDate", "")
			val weekIdx = exam.getInteger("week")?.let { it - 1 }?.coerceIn(0, weeks.size - 1) ?: 0
			listOf(exam.getString("classroomNumber", ""),
			       "$examDate ${weeks[weekIdx]}",
			       "${exam.getString("duration", "")}${stringResource(R.string.minute)}",
			       exam.getString("durationTime", ""),
			       stringResource(R.string.section_range, exam.getIntValue("startClassTimes"), exam.getIntValue("endClassTimes"))).zip(listOf(Icons.Rounded.LocationOn,
			                                                                                                                                  Icons.Rounded.Timer,
			                                                                                                                                  Icons.Rounded.Schedule,
			                                                                                                                                  Icons.Rounded.School,
			                                                                                                                                  Icons.Rounded.CalendarMonth)).forEach { (text, icon) ->
				GenericButton(icon = icon, text = text) {
					coroutineScope.launch {
						clipboard.setClipEntry(ClipData.newPlainText("exam", text).toClipEntry())
					}
				}
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class) @Composable private fun TodoSection(
	todoList: List<TodoEntity>,
	onViewAllClick: () -> Unit,
	todoManager: TodoManager,
                                                                           ) {
	var addTrigger by remember { mutableIntStateOf(0) }
	Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
		CardTitle(R.drawable.todo, text = stringResource(R.string.todo)) {
			onViewAllClick()
		}
		SingleChoiceSegmentedButtonRow {
			SegmentedButton(onClick = { addTrigger++ }, selected = false, shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2), icon = {}, contentPadding = PaddingValues(0.dp)) {
				Icon(painter = painterResource(R.drawable.add), contentDescription = stringResource(R.string.add))
			}
			SegmentedButton(onClick = onViewAllClick, selected = false, shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2), icon = {}, contentPadding = PaddingValues(0.dp)) {
				Icon(painter = painterResource(R.drawable.view), contentDescription = stringResource(R.string.view_detail))
			}
		}
	}
	
	ElevatedCard(modifier = Modifier.fillMaxWidth()) {
		if (todoList.isEmpty()) Text(text = stringResource(R.string.no_todo),
		                             style = MaterialTheme.typography.titleLargeEmphasized,
		                             modifier = Modifier.padding(dimensionResource(R.dimen.horizontal_margin), dimensionResource(R.dimen.vertical_margin)))
		todoManager.TodoListScreen(todoList = todoList, addTrigger = addTrigger)
	}
}

@Composable fun GenericButton(
	icon: ImageVector,
	text: String = "",
	enable: Boolean = true,
	onClick: () -> Unit = {},
                             ) {
	TextButton(onClick = onClick, enabled = enable, shapes = ButtonDefaults.shapes()) {
		Icon(icon, contentDescription = text, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(ButtonDefaults.IconSize))
		Spacer(Modifier.size(ButtonDefaults.IconSpacing))
		Text(text)
	}
}

@Composable fun GenericButton(
	image: Int,
	text: String = "",
	enable: Boolean = true,
	onClick: () -> Unit = {},
                             ) {
	TextButton(onClick = onClick, enabled = enable, shapes = ButtonDefaults.shapes()) {
		Icon(painter = painterResource(image), contentDescription = text, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(ButtonDefaults.IconSize))
		Spacer(Modifier.size(ButtonDefaults.IconSpacing))
		Text(text)
	}
}

@Composable fun RowScope.CardTitle(
	image: Int,
	text: String = "",
	onClick: () -> Unit = {},
                                  ) {
	Row(
		modifier = Modifier
			.weight(1f)
			.clickable(onClick = onClick, indication = null, interactionSource = null),
		verticalAlignment = Alignment.CenterVertically,
	   ) {
		Icon(painter = painterResource(image), contentDescription = text, tint = MaterialTheme.colorScheme.primary)
		Spacer(Modifier.size(ButtonDefaults.IconSpacing))
		Text(text, style = MaterialTheme.typography.titleMediumEmphasized)
	}
}

@Composable fun RowScope.CardTitle(
	image: ImageVector,
	text: String = "",
	onClick: () -> Unit = {},
                                  ) {
	Row(
		modifier = Modifier
			.clickable(onClick = onClick, indication = null, interactionSource = null)
			.weight(1f),
		verticalAlignment = Alignment.CenterVertically,
	   ) {
		Icon(image, contentDescription = text, tint = MaterialTheme.colorScheme.primary)
		Spacer(Modifier.size(ButtonDefaults.IconSpacing))
		Text(text, style = MaterialTheme.typography.titleMediumEmphasized)
	}
}

@Composable fun LongClickButton(
	onClick: () -> Unit,
	onLongClick: () -> Unit,
	modifier: Modifier = Modifier,
	icon: ImageVector? = null,
	label: String? = null,
	enabled: Boolean = true,
	colors: ButtonColors = ButtonDefaults.buttonColors(),
	interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
	content: @Composable RowScope.() -> Unit = {},
                               ) {
	val haptic = LocalHapticFeedback.current
	val isPressed by interactionSource.collectIsPressedAsState()
	val shapes = ButtonDefaults.shapes()
	val shape = when {
		isPressed -> shapes.pressedShape
		else -> shapes.shape
	}
	Surface(
		modifier = modifier
			.minimumInteractiveComponentSize()
			.combinedClickable(interactionSource = interactionSource, enabled = enabled, onClick = onClick, onLongClick = {
				haptic.performHapticFeedback(HapticFeedbackType.LongPress)
				onLongClick()
			}),
		shape = shape,
		color = if (enabled) colors.containerColor else colors.disabledContainerColor,
		contentColor = if (enabled) colors.contentColor else colors.disabledContentColor,
	       ) {
		CompositionLocalProvider(LocalContentColor provides if (enabled) colors.contentColor else colors.disabledContentColor) {
			Row(modifier = Modifier.padding(if (icon != null) ButtonDefaults.ButtonWithIconContentPadding else ButtonDefaults.ContentPadding), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
				icon?.let { Icon(it, contentDescription = label, modifier = Modifier.size(ButtonDefaults.MediumIconSize)) }
				if (icon != null && label != null) Spacer(Modifier.size(ButtonDefaults.IconSpacing))
				label?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
				content()
			}
		}
	}
}