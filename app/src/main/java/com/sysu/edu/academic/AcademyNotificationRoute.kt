package com.sysu.edu.academic

import android.app.Activity
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.R
import com.sysu.edu.browser.BrowserActivity
import com.sysu.edu.nav.navigateBack
import com.sysu.edu.view.ActivityPager
import com.sysu.edu.view.MenuItem
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import top.yukonga.miuix.kmp.basic.SearchBar
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AcademyNotificationRoute(
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope,
	animatedVisibilityScope: AnimatedVisibilityScope,
) {
	val viewModel: AcademyNotificationViewModel = viewModel()
	val academicNotices by viewModel.academicNotices.observeAsState(emptyList())
	val schoolNotices by viewModel.schoolNotices.observeAsState(emptyList())
	val noticeContent by viewModel.noticeContent.observeAsState()
	var sharedElementBounds by remember { mutableStateOf<Rect?>(null) }
	val context = LocalContext.current
	val activity = context as? Activity
	var searchQuery by rememberSaveable { mutableStateOf("") }
	var searchQuery2 by rememberSaveable { mutableStateOf("") }

	LaunchedEffect(Unit) {
		viewModel.fetchNotices()
	}

	@OptIn(FlowPreview::class) LaunchedEffect(Unit) {
		snapshotFlow { searchQuery }.debounce(300L.milliseconds).distinctUntilChanged()
			.collect { keyword ->
				viewModel.fetchAcademicNotice(keyword)
			}
	}
	@OptIn(FlowPreview::class) LaunchedEffect(Unit) {
		snapshotFlow { searchQuery2 }.debounce(300L.milliseconds).distinctUntilChanged()
			.collect { keyword ->
				viewModel.fetchSchoolNotice(keyword)
			}
	}
	LaunchedEffect(noticeContent) {
		noticeContent?.let { content ->
			val intent = Intent(context, BrowserActivity::class.java).putExtra(
				"data", ("""<!DOCTYPE html><html><head>
																			  <style>
                                            body{
                                            padding: 24px !important;
                                            }
                                            a,body,p,span{
                                            font-size: 2.5rem !important;
                                            line-height: 2.0 !important;
                                             }
                                             table{
                                            table-layout: auto !important;
                                            width: 100% !important;
                                             }
                                             table,th, td
                                                    {
                                            font-size: 1.0rem !important;
                                            line-height: 1.0 !important;
                                                    border-collapse: collapse !important;
                                                    border: 2px solid windowtext !important;
                                                    }
                                            </style></head><body>""".trimIndent() + content + "</body></html>").trim()
			)
			val options = sharedElementBounds?.let { bounds ->
				activity?.let { act ->
					val view = View(act).apply {
						x = bounds.left
						y = bounds.top
						layoutParams = ViewGroup.LayoutParams(
							bounds.width.toInt(), bounds.height.toInt()
						)
						transitionName = "miniapp"
					}
					(act.window.decorView as ViewGroup).addView(view)
					val opt =
						ActivityOptionsCompat.makeSceneTransitionAnimation(act, view, "miniapp")
							.toBundle()
					act.window.decorView.postDelayed({
						(act.window.decorView as ViewGroup).removeView(view)
					}, 1000)
					opt
				}
			} ?: activity?.let {
				ActivityOptionsCompat.makeSceneTransitionAnimation(
					it, it.window.decorView, "miniapp"
				).toBundle()
			}

			context.startActivity(intent, options)
			viewModel.clearNoticeContent()
		}
	}

	ActivityPager(
		title = stringResource(id = R.string.academic_affair_notice),
		tabs = mutableListOf(
			MenuItem(stringResource(id = R.string.academic_affair_notice)),
			MenuItem(stringResource(id = R.string.school_affair_notice))
		),
		sharedKey = "AcademyNotification",
		sharedTransitionScope = sharedTransitionScope,
		animatedVisibilityScope = animatedVisibilityScope,
		onNavigationClick = { backStack.navigateBack(activity) },
		topBarContent = {
			fun setQuery(str: String) {
				when (it) {
					0 -> searchQuery = str
					else -> searchQuery2 = str
				}
			}
			fun getQuery(): String = when (it) {
				0 -> searchQuery
				else -> searchQuery2
			}
			SearchBar({
				OutlinedTextField(
					value = getQuery(),
					onValueChange = { str -> setQuery(str) },
					trailingIcon = {
						if (getQuery().isNotEmpty()) IconButton(onClick = {
							setQuery("")
						}) {
							Icon(
								Icons.Rounded.Close,
								contentDescription = stringResource(R.string.clear)
							)
						}
					},
					label = { Text(stringResource(R.string.search_course)) },
					singleLine = true,
					modifier = Modifier
						.fillMaxWidth()

				)
			}, {}) {}

		},
		pageContent = { page ->
			AnimatedContent(targetState = page, label = "page_transition") { targetPage ->
				NewsList(
					newsList = if (targetPage == 0) academicNotices else schoolNotices,
					sharedTransitionScope = sharedTransitionScope,
					animatedVisibilityScope = animatedVisibilityScope
				) { notice, bounds ->
					sharedElementBounds = bounds
					viewModel.fetchContent(notice.getString("id"))
				}
			}
		})
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NewsList(
	newsList: List<JSONObject>,
	sharedTransitionScope: SharedTransitionScope,
	animatedVisibilityScope: AnimatedVisibilityScope,
	onItemClick: (JSONObject, Rect) -> Unit,
) {
	LazyVerticalStaggeredGrid(
		columns = StaggeredGridCells.Adaptive(240.dp),
		modifier = Modifier.fillMaxSize(),
		contentPadding = PaddingValues(dimensionResource(R.dimen.content_padding)),
		horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_padding)),
		verticalItemSpacing = dimensionResource(R.dimen.vertical_padding)
	) {
		items(newsList, key = { it.getString("id") }) { item ->
			NewsItem(
				item = item,
				sharedTransitionScope = sharedTransitionScope,
				animatedVisibilityScope = animatedVisibilityScope
			) { bounds ->
				onItemClick(item, bounds)
			}
		}
	}
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NewsItem(
	item: JSONObject,
	sharedTransitionScope: SharedTransitionScope,
	animatedVisibilityScope: AnimatedVisibilityScope,
	onClick: (Rect) -> Unit,
) {
	var currentBounds by remember { mutableStateOf(Rect.Zero) }
	with(sharedTransitionScope) {
		Card(
			onClick = { onClick(currentBounds) },
			modifier = Modifier
				.fillMaxWidth()
				.onGloballyPositioned { currentBounds = it.boundsInWindow() }
				.sharedBounds(
					rememberSharedContentState(key = "news_${item.getString("id")}"),
					animatedVisibilityScope = animatedVisibilityScope
				)) {
			Column(
				modifier = Modifier
					.padding(
						dimensionResource(R.dimen.horizontal_padding),
						dimensionResource(R.dimen.vertical_padding)
					)
					.fillMaxWidth(),
				verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vertical_padding))
			) {
				Text(
					text = item.getString("title", ""), style = MaterialTheme.typography.titleMedium
				)
				Text(
					text = item.getString("deliveryDate", ""),
					style = MaterialTheme.typography.bodySmall,
				)
			}
		}
	}
}
