package com.miyuyan.sysuer.academic

import android.text.Html
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.nav.Browser
import com.miyuyan.sysuer.nav.navigateBack
import com.miyuyan.sysuer.view.ActivityPager
import com.miyuyan.sysuer.view.MenuItem
import com.miyuyan.sysuer.view.StatePage

/**
 * 学习平台作业：单个 ActivityPager 承载作业列表与设置两页（设置页暂未开发）。
 * 列表为按事件逐条的卡片流，卡片可查看描述、打开作业页面与提交入口。
 */
@Composable
fun HomeworkRoute(
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val viewModel: HomeworkViewModel = viewModel()
	val activity = LocalActivity.current
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()
	val snackbarHostState = remember { SnackbarHostState() }

	LaunchedEffect(Unit) {
		viewModel.snackbarMessage.collect {
			snackbarHostState.showSnackbar(it)
		}
	}

	LaunchedEffect(Unit) {
		viewModel.fetch()
	}

	ActivityPager(
			title = stringResource(R.string.homework),
			snackbar = snackbarHostState,
			navs = listOf(
					MenuItem(stringResource(R.string.homework), iconResource = R.drawable.text),
					MenuItem(stringResource(R.string.setting), iconResource = R.drawable.setting),
			),
			isNestedScrollEnabled = false,
			onNavigationClick = { backStack.navigateBack(activity) },
			sharedTransitionScope = sharedTransitionScope,
			animatedVisibilityScope = animatedVisibilityScope,
			sharedKey = "Homework",
	) { page ->
		when (page) {
			0 -> StatePage(state = uiState, onRetry = { viewModel.fetch() }) {
				HomeworkList(items = viewModel.items) { url ->
					backStack.add(Browser(url))
				}
			}

			else -> {}
		}
	}
}

@Composable
private fun HomeworkList(
	items: List<HomeworkItem>,
	onOpen: (String) -> Unit,
) {
	LazyColumn(
			modifier = Modifier.fillMaxSize(),
			contentPadding = PaddingValues(dimensionResource(R.dimen.content_padding)),
			verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vertical_margin)),
	) {
		items(items) { item ->
			Column {
				Text(
						text = item.course,
						style = MaterialTheme.typography.titleMedium,
						color = MaterialTheme.colorScheme.primary,
						modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.horizontal_padding)),
				)
				HomeworkCard(event = item.event, onOpen = onOpen)
			}
		}
	}
}

@Composable
private fun HomeworkCard(event: JSONObject, onOpen: (String) -> Unit) {
	var showDescription by remember { mutableStateOf(false) }
	ElevatedCard(modifier = Modifier.fillMaxWidth()) {
		Column(
				modifier = Modifier
					.fillMaxWidth()
					.padding(dimensionResource(R.dimen.horizontal_padding)),
				verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vertical_margin)),
		) {
			Row(verticalAlignment = Alignment.CenterVertically) {
				Text(
						text = event.getString("name", ""),
						style = MaterialTheme.typography.titleMedium,
						modifier = Modifier.weight(1f),
				)
				IconButton(onClick = { showDescription = true }) {
					Icon(
							painter = painterResource(R.drawable.view),
							contentDescription = stringResource(R.string.view_detail),
							tint = MaterialTheme.colorScheme.primary,
					)
				}
			}
			Text(
					text = "${stringResource(R.string.type)} ${
						event.getString("normalisedeventtypetext", "")
					}\n${stringResource(R.string.link)} ${event.getString("viewurl", "")}",
					style = MaterialTheme.typography.labelMedium,
			)
			Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_margin))) {
				FilledTonalButton(
						onClick = { onOpen(event.getString("url") ?: "") },
						shapes = ButtonDefaults.shapes(),
						modifier = Modifier.weight(1f),
				) {
					Icon(
							painter = painterResource(R.drawable.view),
							contentDescription = null,
							modifier = Modifier.size(ButtonDefaults.IconSize),
					)
					Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
					Text(stringResource(R.string.view_detail))
				}
				FilledTonalButton(
						onClick = { onOpen(event.getString("viewurl") ?: "") },
						shapes = ButtonDefaults.shapes(),
						modifier = Modifier.weight(1f),
				) {
					Icon(
							painter = painterResource(R.drawable.up),
							contentDescription = null,
							modifier = Modifier.size(ButtonDefaults.IconSize),
					)
					Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
					Text(stringResource(R.string.submit))
				}
			}
		}
	}
	if (showDescription) {
		AlertDialog(
				onDismissRequest = { showDescription = false },
				text = {
					Text(
							text = Html.fromHtml(
									event.getString("description", ""), Html.FROM_HTML_MODE_COMPACT
							).toString()
					)
				},
				confirmButton = {
					TextButton(
							onClick = { showDescription = false }, shapes = ButtonDefaults.shapes()
					) { Text(stringResource(R.string.confirm)) }
				},
		)
	}
}
