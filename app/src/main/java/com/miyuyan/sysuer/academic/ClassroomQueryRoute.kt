package com.miyuyan.sysuer.academic

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.preference.ChoicePreference
import com.miyuyan.preference.FilterPreference
import com.miyuyan.preference.ItemPreference
import com.miyuyan.preference.PreferenceCategory
import com.miyuyan.preference.PreferenceScreen
import com.miyuyan.preference.RangeSliderPreference
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.DateTimeManager
import com.miyuyan.sysuer.nav.navigateBack
import com.miyuyan.sysuer.view.ActivityPager
import com.miyuyan.sysuer.view.MenuItem
import com.miyuyan.sysuer.view.StatePage
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * 自习室/研讨室查询：单个 ActivityPager 承载查询与结果两页。查询页用 compose-preference
 * 的筛选样式（校区-教学楼联动、教室类型、日期与节次范围），悬浮按钮"查询"触发检索并
 * 翻到结果页；结果页为状态页 + 瀑布流卡片（含经 WebVPN 鉴权加载的教室照片），滚动到底部
 * 自动分页。
 */
@Composable
fun ClassroomQueryRoute(
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val viewModel: ClassroomQueryViewModel = viewModel()
	val activity = LocalActivity.current
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()
	val pagerState = rememberPagerState(pageCount = { 2 })
	val coroutineScope = rememberCoroutineScope()
	val title = stringResource(R.string.classroom_query)
	val snackbarHostState = remember { SnackbarHostState() }

	LaunchedEffect(Unit) {
		viewModel.snackbarMessage.collect {
			snackbarHostState.showSnackbar(it)
		}
	}

	LaunchedEffect(Unit) {
		viewModel.fetchCampuses()
	}
	ActivityPager(
			title = title,
			snackbar = snackbarHostState,
			tabs = listOf(
					MenuItem(stringResource(R.string.query)),
					MenuItem(stringResource(R.string.result)),
			),
			pagerState = pagerState,
			isNestedScrollEnabled = false,
			onNavigationClick = { backStack.navigateBack(activity) },
			sharedTransitionScope = sharedTransitionScope,
			animatedVisibilityScope = animatedVisibilityScope,
			sharedKey = "ClassroomQuery",
			floatingActionButton = { page ->
				if (page == 0) ExtendedFloatingActionButton(
						onClick = {
							viewModel.query()
							coroutineScope.launch { pagerState.animateScrollToPage(1) }
						},
						icon = {
							Icon(
									painter = painterResource(R.drawable.search),
									contentDescription = stringResource(R.string.query)
							)
						},
						text = { Text(stringResource(R.string.query)) },
				)
			},
	) { page ->
		when (page) {
			0 -> QueryPage(viewModel)
			else -> StatePage(state = uiState) {
				ClassroomResult(viewModel)
			}
		}
	}
}

@Composable
private fun QueryPage(viewModel: ClassroomQueryViewModel) {
	val campusNames by viewModel.campusNames.collectAsStateWithLifecycle()
	val campusValues by viewModel.campusValues.collectAsStateWithLifecycle()
	val buildingNames by viewModel.buildingNames.collectAsStateWithLifecycle()
	val buildingValues by viewModel.buildingValues.collectAsStateWithLifecycle()
	val dateMillis by viewModel.dateMillis.collectAsStateWithLifecycle()
	var showDatePicker by rememberSaveable { mutableStateOf(false) }

	PreferenceScreen(modifier = Modifier.fillMaxSize()) {
		PreferenceCategory(title = stringResource(R.string.campus)) {
			item {
				FilterPreference(
						title = stringResource(R.string.campus),
						entries = campusNames,
						entryValues = campusValues,
						onChange = { _, _, values ->
							viewModel.campusSelection = values
							viewModel.fetchBuildings()
						},
				)
			}
			item {
				FilterPreference(
						title = stringResource(R.string.office),
						entries = buildingNames,
						entryValues = buildingValues,
						onChange = { _, _, values -> viewModel.buildingSelection = values },
				)
			}
		}
		PreferenceCategory(title = stringResource(R.string.classroom_type)) {
			item {
				ChoicePreference(
						title = stringResource(R.string.classroom_type),
						entries = listOf(
								stringResource(R.string.self_study_room),
								stringResource(R.string.seminar_room),
						),
						entryValues = listOf(
								ClassroomQueryViewModel.SELF_STUDY_ROOM,
								ClassroomQueryViewModel.SEMINAR_ROOM,
						),
						initialSelections = setOf(0, 1),
						requireSelection = true,
						onChange = { _, _, values -> viewModel.typeSelection = values },
				)
			}
		}
		PreferenceCategory(title = stringResource(R.string.time)) {
			item {
				ItemPreference(
						title = stringResource(R.string.date),
						icon = R.drawable.calendar,
						summary = DateTimeManager.toDateString(
								dateMillis, DateTimeFormatter.ofPattern("yyyy年MM月dd日")
						),
						onClick = { showDatePicker = true },
				)
			}
			item {
				RangeSliderPreference(
						title = stringResource(R.string.class_range),
						valueRange = 1f..11f,
						steps = 9,
						initialValues = viewModel.sections,
						valuesText = {
							stringResource(
									R.string.from_to_section,
									it.start.roundToInt(),
									it.endInclusive.roundToInt()
							)
						},
						onValueChange = { viewModel.sections = it },
				)
			}
		}
	}
	if (showDatePicker) {
		val state = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
		DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = {
			TextButton(
					onClick = {
						state.selectedDateMillis?.let { viewModel.setDate(it) }
						showDatePicker = false
					}, shapes = ButtonDefaults.shapes()
			) { Text(stringResource(R.string.confirm)) }
		}, dismissButton = {
			TextButton(
					onClick = { showDatePicker = false }, shapes = ButtonDefaults.shapes()
			) { Text(stringResource(R.string.cancel)) }
		}) {
			DatePicker(state = state)
		}
	}
}

@Composable
private fun ClassroomResult(viewModel: ClassroomQueryViewModel) {
	val state = rememberLazyStaggeredGridState()
	val reachBottom by remember {
		derivedStateOf {
			val info = state.layoutInfo
			info.totalItemsCount > 0 && info.visibleItemsInfo.lastOrNull()?.index == info.totalItemsCount - 1
		}
	}
	LaunchedEffect(reachBottom) {
		if (reachBottom) viewModel.fetchResult()
	}
	LazyVerticalStaggeredGrid(
			columns = StaggeredGridCells.Adaptive(320.dp),
			state = state,
			modifier = Modifier.fillMaxSize(),
			contentPadding = PaddingValues(dimensionResource(R.dimen.content_padding)),
			horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_padding)),
			verticalItemSpacing = dimensionResource(R.dimen.vertical_padding)
	) {
		items(viewModel.rooms) { room ->
			ClassroomCard(room = room, host = viewModel.host, cookie = viewModel.cookie)
		}
	}
}

@Composable
private fun ClassroomCard(room: JSONObject, host: String, cookie: String) {
	val context = LocalContext.current
	ElevatedCard(modifier = Modifier.fillMaxWidth()) {
		Row(
				modifier = Modifier.padding(dimensionResource(R.dimen.horizontal_margin)),
				horizontalArrangement = Arrangement.spacedBy(
						dimensionResource(R.dimen.horizontal_margin)
				),
		) {
			AsyncImage(
					model = ImageRequest.Builder(context).data(
								"https://$host/jwxt/base-info/classroom/classRoomView?fileName=jspic.png&filePath=${
									room.get("photoPath")
								}"
						).addHeader("Cookie", cookie)
						.addHeader("Referer", "https://jwxt.sysu.edu.cn/").crossfade(true).build(),
					contentDescription = room.getString("classRoomNum"),
					contentScale = ContentScale.Crop,
					placeholder = painterResource(R.drawable.logo),
					error = painterResource(R.drawable.logo),
					modifier = Modifier
						.width(145.dp)
						.height(132.dp)
						.clip(RoundedCornerShape(16.dp)),
			)
			Column(
					modifier = Modifier.weight(1f),
					verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vertical_gap)),
			) {
				Row(
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_gap)),
				) {
					Text(
							text = room.getString("classRoomTag", ""),
							style = MaterialTheme.typography.labelMedium,
							color = MaterialTheme.colorScheme.onSecondaryContainer,
							modifier = Modifier
								.clip(RoundedCornerShape(8.dp))
								.background(MaterialTheme.colorScheme.secondaryContainer)
								.padding(horizontal = 8.dp, vertical = 2.dp),
					)
					Text(
							text = room.getString("classRoomNum", ""),
							style = MaterialTheme.typography.titleMedium,
					)
				}
				FlowRow(
						verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vertical_gap)),
						horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_gap)),
				) {
					ClassroomInfo(
							R.drawable.location, room.getString("teachingBuildingName", "")
					)
					ClassroomInfo(R.drawable.time, room.getString("classTimes", ""))
					ClassroomInfo(R.drawable.floor, room.getString("floor", ""))
					ClassroomInfo(R.drawable.seat, room.getString("seats", ""))
				}
			}
		}
	}
}

@Composable
private fun ClassroomInfo(icon: Int, text: String) {
	Row(verticalAlignment = Alignment.CenterVertically) {
		Icon(
				painter = painterResource(icon),
				contentDescription = text,
				tint = MaterialTheme.colorScheme.primary,
				modifier = Modifier.size(ButtonDefaults.IconSize),
		)
		Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
		Text(text = text, style = MaterialTheme.typography.bodySmall)
	}
}
