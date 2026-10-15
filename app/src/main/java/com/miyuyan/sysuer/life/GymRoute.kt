package com.miyuyan.sysuer.life

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.nav.navigateBack
import com.miyuyan.sysuer.view.ActivityPager
import com.miyuyan.sysuer.view.MenuItem

/**
 * 体育场馆预订：场地 / 预约 / 订单 / 我的 四个页签。
 *
 * 原实现由 GymReservationActivity（Toolbar + NavHostFragment + BottomNavigationView）
 * 承载，现改用与 [PayRoute] 一致的 [ActivityPager] 形态。场地的「校区 → 场馆 → 时段」
 * 三级下钻状态提升到本层，使顶栏 topBarContent 能渲染面包屑；预约 / 订单的日期区间
 * 选择器也一并放到顶栏，随页签切换而显示。
 */
@Composable
fun GymRoute(
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val viewModel: GymViewModel = viewModel()
	val activity = LocalActivity.current
	var page by rememberSaveable { mutableIntStateOf(0) }

	// 场地下钻：0 校区 / 1 场馆 / 2 时段详情；名称供面包屑展示
	var level by rememberSaveable { mutableIntStateOf(0) }
	var campusId by rememberSaveable { mutableStateOf("") }
	var campusName by rememberSaveable { mutableStateOf("") }
	var venueId by rememberSaveable { mutableStateOf("") }
	var venueName by rememberSaveable { mutableStateOf("") }

	val reservationFromTo by viewModel.reservationFromTo.collectAsStateWithLifecycle()
	val orderFromTo by viewModel.orderFromTo.collectAsStateWithLifecycle()

	// 场地页处于下钻层时，系统返回键先退回上一层
	BackHandler(enabled = page == 0 && level > 0) { level -= 1 }

	ActivityPager(
			title = stringResource(R.string.gym_reservation),
			navs = listOf(
					MenuItem(stringResource(R.string.field), iconResource = R.drawable.card),
					MenuItem(stringResource(R.string.reservation), iconResource = R.drawable.todo),
					MenuItem(stringResource(R.string.order), iconResource = R.drawable.text),
					MenuItem(stringResource(R.string.account), iconResource = R.drawable.account),
			),
			onPageChange = { page = it },
			onNavigationClick = {
				if (page == 0 && level > 0) level -= 1 else backStack.navigateBack(activity)
			},
			topBarContent = { current ->
				when (current) {
					0 -> if (level == 2) {
					// 详情层同时保留面包屑与提交行，提交行不再顶替面包屑
					Column {
						GymFieldBreadcrumb(
								level = level,
								campusName = campusName,
								venueName = venueName,
								onNavigate = { target -> level = target },
						)
						GymSubmitBar(viewModel)
					}
				} else if (level > 0) {
					GymFieldBreadcrumb(
							level = level,
							campusName = campusName,
							venueName = venueName,
							onNavigate = { target -> level = target },
					)
				}

					1 -> GymDateRange(
							from = reservationFromTo.first, to = reservationFromTo.second
					) { from, to -> viewModel.setReservationFromTo(from, to) }

					2 -> GymDateRange(
							from = orderFromTo.first, to = orderFromTo.second
					) { from, to -> viewModel.setOrderFromTo(from, to) }
				}
			},
			sharedTransitionScope = sharedTransitionScope,
			animatedVisibilityScope = animatedVisibilityScope,
			sharedKey = "GymReservation",
	) { current ->
		when (current) {
			0 -> GymFieldTab(
					viewModel = viewModel,
					level = level,
					campusId = campusId,
					venueId = venueId,
					onCampusClick = { id, name ->
						campusId = id
						campusName = name
						level = 1
					},
					onVenueClick = { id, name ->
						venueId = id
						venueName = name
						level = 2
					},
			)

			1 -> GymReservationTab(viewModel)
			2 -> GymOrderTab(viewModel)
			else -> GymAccountTab(viewModel)
		}
	}
}

/** 时段详情顶部的提交行：左侧为已选摘要，右侧为提交按钮 */
@Composable
private fun GymSubmitBar(viewModel: GymViewModel) {
	val selection = viewModel.selectionSummary()
	val unselected = stringResource(R.string.unselected)
	val submit = stringResource(R.string.submit)
	Row(
			verticalAlignment = Alignment.CenterVertically,
			modifier = Modifier
				.fillMaxWidth()
				.padding(
						horizontal = dimensionResource(R.dimen.horizontal_margin),
						vertical = dimensionResource(R.dimen.vertical_margin),
				),
	) {
		Text(
				text = selection?.first ?: unselected,
				style = MaterialTheme.typography.bodyMedium,
				modifier = Modifier.weight(1f),
		)
		FilledTonalButton(
				onClick = { viewModel.submit() },
				enabled = selection != null,
				shape = ButtonDefaults.shape,
		) { Text(submit) }
	}
}

/** 场地页顶部面包屑：场地 / 校区 / 场馆，点击可回退到对应层级 */
@Composable
private fun GymFieldBreadcrumb(
	level: Int,
	campusName: String,
	venueName: String,
	onNavigate: (Int) -> Unit,
) {
	val root = stringResource(R.string.field)
	Row(
			verticalAlignment = Alignment.CenterVertically,
			modifier = Modifier
				.fillMaxWidth()
				.horizontalScroll(rememberScrollState())
				.padding(
						horizontal = dimensionResource(R.dimen.horizontal_margin),
						vertical = dimensionResource(R.dimen.vertical_margin),
				),
	) {
		GymCrumb(root, selected = level == 0) { onNavigate(0) }
		if (level >= 1) {
			GymCrumbSeparator()
			GymCrumb(campusName, selected = level == 1) { onNavigate(1) }
		}
		if (level >= 2) {
			GymCrumbSeparator()
			GymCrumb(venueName, selected = true) {}
		}
	}
}

@Composable
private fun GymCrumbSeparator() {
	Text(
			text = "/",
			style = MaterialTheme.typography.titleSmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			modifier = Modifier.padding(horizontal = 4.dp),
	)
}

/** 面包屑单级：非当前级可点击回退，当前级高亮且不可点 */
@Composable
private fun GymCrumb(text: String, selected: Boolean, onClick: () -> Unit) {
	Text(
			text = text,
			style = MaterialTheme.typography.titleSmall,
			color = if (selected) MaterialTheme.colorScheme.primary
			else MaterialTheme.colorScheme.onSurfaceVariant,
			modifier = Modifier
				.clip(CircleShape)
				.clickable(enabled = !selected, onClick = onClick)
				.padding(horizontal = 4.dp),
	)
}
