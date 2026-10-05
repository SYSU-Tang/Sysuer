package com.miyuyan.sysuer.academic

import android.content.ClipData
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import coil.compose.AsyncImage
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.nav.Browser
import com.miyuyan.sysuer.nav.navigateBack
import com.miyuyan.sysuer.view.ActivityPager
import com.miyuyan.sysuer.view.MenuItem
import com.miyuyan.sysuer.view.StatePage
import kotlinx.coroutines.launch

/**
 * 校历：从教务部官网抓取校历图片并按学年学期分页展示，点击图片在浏览器打开，
 * 长按弹出保存到相册、复制链接与分享菜单。
 */
@Composable
fun SchoolCalendarRoute(
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val viewModel: SchoolCalendarViewModel = viewModel()
	val activity = LocalActivity.current
	val context = LocalContext.current
	val sections by viewModel.sections.collectAsStateWithLifecycle()
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()
	val snackbarHostState = remember { SnackbarHostState() }
	val clipboard = LocalClipboard.current
	val scope = rememberCoroutineScope()

	LaunchedEffect(Unit) {
		viewModel.snackbarMessage.collect {
			snackbarHostState.showSnackbar(it)
		}
	}

	ActivityPager(
			title = stringResource(R.string.calendar),
			snackbar = snackbarHostState,
			tabs = sections.map { MenuItem(it.title) },
			isNestedScrollEnabled = false,
			onNavigationClick = { backStack.navigateBack(activity) },
			sharedTransitionScope = sharedTransitionScope,
			animatedVisibilityScope = animatedVisibilityScope,
			sharedKey = "SchoolCalendar",
	) { page ->
		StatePage(state = uiState) {
			val section = sections.getOrNull(page) ?: return@StatePage
			LazyColumn(
					modifier = Modifier.fillMaxSize(),
					contentPadding = PaddingValues(dimensionResource(R.dimen.content_padding)),
					verticalArrangement = Arrangement.spacedBy(
							dimensionResource(R.dimen.vertical_padding)
					),
			) {
				items(section.images) { url ->
					CalendarImage(
							url = url,
							onClick = { backStack.add(Browser(url)) },
							onSave = { viewModel.saveImage(url) },
							onShare = { viewModel.shareImage(url) },
							onCopy = {
								scope.launch {
									clipboard.setClipEntry(
											ClipData.newPlainText("link", url).toClipEntry()
									)
									snackbarHostState.showSnackbar(
											context.getString(R.string.copy_successfully)
									)
								}
							},
					)
				}
			}
		}
	}
}

@Composable
private fun CalendarImage(
	url: String,
	onClick: () -> Unit,
	onSave: () -> Unit,
	onCopy: () -> Unit,
	onShare: () -> Unit,
) {
	var menuExpanded by remember { mutableStateOf(false) }
	Box {
		AsyncImage(
				model = url,
				contentDescription = null,
				contentScale = ContentScale.FillWidth,
				modifier = Modifier
					.fillMaxWidth()
					.combinedClickable(
							onClick = onClick,
							onLongClick = { menuExpanded = true },
					),
		)
		DropdownMenu(
				expanded = menuExpanded,
				onDismissRequest = { menuExpanded = false },
		) {
			DropdownMenuItem(
					text = { Text(stringResource(R.string.save)) },
					onClick = {
						menuExpanded = false
						onSave()
					},
			)
			DropdownMenuItem(
					text = { Text(stringResource(R.string.copy_link)) },
					onClick = {
						menuExpanded = false
						onCopy()
					},
			)
			DropdownMenuItem(
					text = { Text(stringResource(R.string.share)) },
					onClick = {
						menuExpanded = false
						onShare()
					},
			)
		}
	}
}
