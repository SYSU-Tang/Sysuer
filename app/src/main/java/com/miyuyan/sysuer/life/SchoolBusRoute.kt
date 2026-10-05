package com.miyuyan.sysuer.life

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.nav.navigateBack
import com.miyuyan.sysuer.view.ActivityPager
import com.miyuyan.sysuer.view.MenuItem
import com.miyuyan.sysuer.view.StaggerScreen
import com.miyuyan.sysuer.view.StatePage
import com.miyuyan.sysuer.view.exportMarkdownMenuItem
import kotlinx.coroutines.launch

/**
 * 校车班车:顶栏为工作日/假日切换、公告与线路跳转,按行驶方向分页展示各班次卡片
 */
@Composable
fun SchoolBusRoute(
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val viewModel: SchoolBusViewModel = viewModel()
	val activity = LocalActivity.current
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()
	val day by viewModel.day.collectAsStateWithLifecycle()
	val notice by viewModel.notice.collectAsStateWithLifecycle()
	val pagerState = rememberPagerState { viewModel.routes.size }
	val scope = rememberCoroutineScope()
	var noticeVisible by remember { mutableStateOf(false) }
	var optionExpanded by remember { mutableStateOf(false) }

	LaunchedEffect(Unit) {
		viewModel.load()
	}

	ActivityPager(
			title = stringResource(R.string.school_bus),
			onNavigationClick = { backStack.navigateBack(activity) },
			tabs = viewModel.routes.map { MenuItem(it) },
			pagerState = pagerState,
			isNestedScrollEnabled = false,
			isTopBarContentFixed = true,
			sharedTransitionScope = sharedTransitionScope,
			animatedVisibilityScope = animatedVisibilityScope,
			sharedKey = "SchoolBus",
			topBarMenus = { page ->
				listOf(
						MenuItem(
								iconResource = R.drawable.notice,
								title = stringResource(R.string.notice)
						) { noticeVisible = true },

						exportMarkdownMenuItem(
								backStack,
								viewModel.routeSections.getOrElse(page) { remember { mutableStateListOf() } },
								viewModel.routes.getOrElse(page) { "" },
								stringResource(R.string.school_bus)
						)
				)
			},
			topBarContent = {
				SchoolBusHeader(
						day = day,
						onDayChange = { viewModel.setDay(it) },
						options = viewModel.routes,
						selected = pagerState.currentPage,
						expanded = optionExpanded,
						onExpandedChange = { optionExpanded = it },
						onSelect = { index ->
							scope.launch { pagerState.scrollToPage(index) }
						},
				)
			},
	) { page ->
		StatePage(state = uiState, onRetry = { viewModel.retry() }) {
			viewModel.routeSections.getOrNull(page)?.let {
				StaggerScreen(
						sections = it
				)
			}
		}
	}
	if (noticeVisible) AlertDialog(
			onDismissRequest = { noticeVisible = false },
			title = { Text(stringResource(R.string.notice)) },
			text = {
				SelectionContainer {
					Text(notice, modifier = Modifier.verticalScroll(rememberScrollState()))
				}
			},
			confirmButton = {
				TextButton(onClick = { noticeVisible = false }) {
					Text(stringResource(R.string.confirm))
				}
			},
	)
}

@Composable
private fun SchoolBusHeader(
	day: Boolean,
	onDayChange: (Boolean) -> Unit,
	options: List<String>,
	selected: Int,
	expanded: Boolean,
	onExpandedChange: (Boolean) -> Unit,
	onSelect: (Int) -> Unit,
) {
	Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(
						dimensionResource(R.dimen.horizontal_padding),
						dimensionResource(R.dimen.vertical_padding)
				),
			horizontalArrangement = Arrangement.SpaceEvenly,
			verticalAlignment = Alignment.CenterVertically
	) {
		SingleChoiceSegmentedButtonRow {
			SegmentedButton(
					selected = day,
					onClick = { onDayChange(true) },
					shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
			) {
				Text(stringResource(R.string.workday))
			}
			SegmentedButton(
					selected = !day,
					onClick = { onDayChange(false) },
					shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
			) {
				Text(stringResource(R.string.holiday))
			}
		}
		ExposedDropdownMenuBox(
				expanded = expanded,
				onExpandedChange = onExpandedChange,
		) {
			OutlinedTextField(
					modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
					value = options.getOrElse(selected) { "" },
					onValueChange = {},
					readOnly = true,
					singleLine = true,
					label = { Text(stringResource(R.string.route)) },
					trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
					colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
			)
			ExposedDropdownMenu(
					expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
				options.forEachIndexed { index, option ->
					DropdownMenuItem(
							modifier = Modifier.background(if (index == selected) Color.LightGray else Color.Transparent),
							text = { Text(option) },
							onClick = {
								onSelect(index)
								onExpandedChange(false)
							},
							contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
					)
				}
			}
		}
	}
}
