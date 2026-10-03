package com.miyuyan.sysuer.life

import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExpandedFullScreenContainedSearchBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberContainedSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.CommonUtil.trim
import com.miyuyan.sysuer.browser.BrowserActivity
import com.miyuyan.sysuer.nav.navigateBack
import com.miyuyan.sysuer.view.ActivityPager
import com.miyuyan.sysuer.view.MenuItem
import com.miyuyan.sysuer.view.StatePage
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * 资讯门户：单个 ActivityPager 承载资讯、公众号、通知、今日中大四个栏目。顶栏为搜索框，
 * 输入时展示搜索联想词，点击联想词或回车跳转门户搜索；栏目内容为图文卡片流，滚动到底部
 * 自动分页（资讯为推荐流无分页）
 */
@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun NewsRoute(
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val viewModel: NewsViewModel = viewModel()
	val activity = LocalActivity.current
	val context = LocalContext.current
	val news by viewModel.news.collectAsStateWithLifecycle()
	val subscriptions by viewModel.subscriptions.collectAsStateWithLifecycle()
	val notices by viewModel.notices.collectAsStateWithLifecycle()
	val dailyNews by viewModel.dailyNews.collectAsStateWithLifecycle()
	val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
	val newsUiState by viewModel.newsUiState.collectAsStateWithLifecycle()
	val subscriptionUiState by viewModel.subscriptionUiState.collectAsStateWithLifecycle()
	val noticeUiState by viewModel.noticeUiState.collectAsStateWithLifecycle()
	val dailyNewsUiState by viewModel.dailyNewsUiState.collectAsStateWithLifecycle()
	val searchBarState = rememberContainedSearchBarState()
	val textFieldState = rememberTextFieldState()
	val scope = rememberCoroutineScope()

	fun openBrowser(url: String?) {
		val intent = Intent(context, BrowserActivity::class.java).setData(url.orEmpty().toUri())
		activity?.let {
			context.startActivity(
					intent/*, ActivityOptionsCompat.makeSceneTransitionAnimation(
					it, it.window.decorView, "miniapp"
			).toBundle()
			*/
			)
		} ?: context.startActivity(intent)
	}

	LaunchedEffect(textFieldState) {
		snapshotFlow { textFieldState.text.toString() }.debounce(300.milliseconds)
			.collect { keyword ->
				if (keyword.isBlank()) viewModel.clearSuggestions() else viewModel.fetchSuggestions(
						keyword
				)
			}
	}

	LaunchedEffect(Unit) {
		(0..3).forEach { viewModel.fetch(it) }
	}

	ActivityPager(
			title = stringResource(R.string.news),
			tabs = listOf(
					MenuItem(stringResource(R.string.news)),
					MenuItem(stringResource(R.string.official_account)),
					MenuItem(stringResource(R.string.notification)),
					MenuItem(stringResource(R.string.daily_news)),
			),
			isTopBarContentFixed = true,
			onNavigationClick = { backStack.navigateBack(activity) },
			isNestedScrollEnabled = false,
			sharedTransitionScope = sharedTransitionScope,
			animatedVisibilityScope = animatedVisibilityScope,
			sharedKey = "News",
			topBarContent = {
				val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
				val appBarWithSearchColors = SearchBarDefaults.appBarWithSearchColors(
						searchBarColors = SearchBarDefaults.containedColors(state = searchBarState)
				)
				AppBarWithSearch(
						scrollBehavior = scrollBehavior,
						windowInsets = SearchBarDefaults.windowInsets.only(WindowInsetsSides.Horizontal),
						state = searchBarState,
						colors = SearchBarDefaults.appBarWithSearchColors(
								appBarContainerColor = Color.Transparent,
						),
						inputField = {
							SearchBarDefaults.InputField(
									textFieldState = rememberTextFieldState(),
									searchBarState = searchBarState,
									colors = appBarWithSearchColors.searchBarColors.inputFieldColors,
									onSearch = { },
									placeholder = {
										Text(
												modifier = Modifier.clearAndSetSemantics {},
												text = stringResource(R.string.query)
										)
									},
									leadingIcon = {
										Icon(
												Icons.Rounded.Search,
												contentDescription = stringResource(R.string.search)
										)
									},
							)
						},
				)
				ExpandedFullScreenContainedSearchBar(
						state = searchBarState,
						inputField = {
							SearchBarDefaults.InputField(
									textFieldState = textFieldState,
									searchBarState = searchBarState,
									colors = appBarWithSearchColors.searchBarColors.inputFieldColors,
									onSearch = { keyword ->
										if (keyword.isNotBlank()) {
											scope.launch { searchBarState.animateToCollapsed() }
											openBrowser(viewModel.searchUrl(keyword))
										}
									},
									placeholder = {
										Text(
												modifier = Modifier.clearAndSetSemantics {},
												text = stringResource(R.string.query)
										)
									},
									leadingIcon = {
										IconButton(onClick = {
											scope.launch { searchBarState.animateToCollapsed() }
										}) {
											Icon(
													Icons.AutoMirrored.Rounded.ArrowBack,
													contentDescription = stringResource(R.string.back)
											)
										}
									},
							)
						},
				) {
					SuggestionList(suggestions) { suggestion ->
						scope.launch { searchBarState.animateToCollapsed() }
						openBrowser(viewModel.searchUrl(suggestion))
					}
				}
			},
	) { page ->
		val (list, uiState) = when (page) {
			0 -> news to newsUiState
			1 -> subscriptions to subscriptionUiState
			2 -> notices to noticeUiState
			else -> dailyNews to dailyNewsUiState
		}
		StatePage(state = uiState) {
			NewsGrid(
					list = list,
					cookie = viewModel.cookie,
					authorization = viewModel.authorization,
					onLoadMore = { viewModel.loadMore(page) },
			) { item ->
				openBrowser(item.getString("url"))
			}
		}
	}
}

@Composable
private fun SuggestionList(suggestions: List<String>, onSuggestionClick: (String) -> Unit) {
	LazyColumn(modifier = Modifier.fillMaxSize()) {
		items(suggestions) { suggestion ->
			ListItem(
					onClick = { onSuggestionClick(suggestion) },
					supportingContent = {
						Text(suggestion, maxLines = 1, overflow = TextOverflow.Ellipsis)
					},
					modifier = Modifier.fillMaxWidth(),
			) {
				Text(trim(suggestion), maxLines = 1, overflow = TextOverflow.Ellipsis)
			}
		}
	}
}

@Composable
private fun NewsGrid(
	list: List<JSONObject>,
	cookie: String,
	authorization: String,
	onLoadMore: () -> Unit,
	onItemClick: (JSONObject) -> Unit,
) {
	val state = rememberLazyStaggeredGridState()
	val reachBottom by remember {
		derivedStateOf {
			val info = state.layoutInfo
			info.totalItemsCount > 0 && info.visibleItemsInfo.lastOrNull()?.index == info.totalItemsCount - 1
		}
	}
	LaunchedEffect(reachBottom) {
		if (reachBottom) onLoadMore()
	}
	LazyVerticalStaggeredGrid(
			columns = StaggeredGridCells.Adaptive(320.dp),
			state = state,
			modifier = Modifier.fillMaxSize(),
			contentPadding = PaddingValues(dimensionResource(R.dimen.content_padding)),
			horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_padding)),
			verticalItemSpacing = dimensionResource(R.dimen.vertical_padding)
	) {
		items(list) { item ->
			NewsItem(
					item = item,
					cookie = cookie,
					authorization = authorization,
					onClick = { onItemClick(item) })
		}
	}
}

@Composable
private fun NewsItem(item: JSONObject, cookie: String, authorization: String, onClick: () -> Unit) {
	val image = item.getJSONArray("coversPicList")?.let {
		if (it.isNotEmpty() && !it.getJSONObject(0).isNullOrEmpty()) {
			it.getJSONObject(0).getString("outLink")
		} else ""
	} ?: ""
	val context = LocalContext.current
	Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
		Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(dimensionResource(R.dimen.horizontal_padding)),
				horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_padding)),
				verticalAlignment = Alignment.CenterVertically
		) {
			if (image.isNotEmpty()) {
				AsyncImage(
						model = ImageRequest.Builder(context).data(image)
							.addHeader("Cookie", cookie).addHeader("Authorization", authorization)
							.crossfade(true).build(),
						contentDescription = null,
						contentScale = ContentScale.Crop,
						modifier = Modifier
							.height(90.dp)
							.fillMaxWidth(0.3f)
							.clip(RoundedCornerShape(16.dp)),
				)
			}
			Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
				Text(
						text = item.getString("title", ""),
						style = MaterialTheme.typography.titleMedium,
						maxLines = 2,
						overflow = TextOverflow.Ellipsis
				)
				Text(
						text = "#${
							item.getJSONObject("source")?.getString("seedName").orEmpty()
						} #${item.getString("createTime", "")}",
						style = MaterialTheme.typography.bodySmall,
				)
			}
		}
	}
}
