package com.sysu.edu.view

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TabIndicatorScope
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.times
import com.sysu.edu.R
import com.sysu.edu.theme.SysuerTheme
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class) @Composable fun ActivityPager(
	title: String = "",
	tabs: List<MenuItem> = emptyList(),
	navs: List<MenuItem> = emptyList(),
	onNavigationClick: () -> Unit = { },
	onPageChange: ((Int) -> Unit)? = null,
	isNestedScrollEnabled: Boolean = true,
	floatingActionButton: @Composable () -> Unit = {},
	actions: @Composable RowScope.() -> Unit = {},
	pageContent: @Composable (page: Int) -> Unit = {},
                                                                                                       ) {
	val pagerState = rememberPagerState(pageCount = {
		if (tabs.isNotEmpty()) tabs.size else if (navs.isNotEmpty()) navs.size else 1
	})
	val coroutineScope = rememberCoroutineScope()
	val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
	
	LaunchedEffect(pagerState.currentPage) {
		onPageChange?.invoke(pagerState.currentPage)
	}
	
	SysuerTheme {
		Scaffold(
			modifier = Modifier
				.fillMaxSize()
				.nestedScroll(scrollBehavior.nestedScrollConnection),
			topBar = {
				val backgroundColor = lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceContainer, scrollBehavior.state.overlappedFraction)
				Surface(color = backgroundColor) {
					Column {
						TopAppBar(
							title = { Text(text = title) },
							navigationIcon = {
								IconButton(onClick = onNavigationClick) {
									Icon(
										imageVector = Icons.AutoMirrored.Filled.ArrowBack,
										contentDescription = stringResource(R.string.back),
									    )
								}
							},
							actions = actions,
							colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
							scrollBehavior = scrollBehavior,
						         )
						if (tabs.isNotEmpty()) {
							val tabContent = @Composable {
								tabs.forEachIndexed { index, tabItem ->
									val selected = pagerState.currentPage == index
									UnboundedTab(selected = selected, icon = tabItem.icon, text = tabItem.title, onClick = {
										coroutineScope.launch {
											pagerState.animateScrollToPage(index)
										}
									})
								}
							}
							if (tabs.size > 4) PrimaryScrollableTabRow(edgePadding = 0.dp,
							                                           selectedTabIndex = pagerState.currentPage,
							                                           modifier = Modifier.fillMaxWidth(),
							                                           containerColor = Color.Transparent,
							                                           divider = {},
							                                           tabs = tabContent,
							                                           indicator = {
								                                           TabRowDefaults.PrimaryIndicator(modifier = Modifier.tabIndicatorOffset(selectedTabIndex = pagerState.currentPage, matchContentSize = true),
								                                                                           width = Dp.Unspecified,
								                                                                           color = MaterialTheme.colorScheme.primary,
								                                                                           shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
							                                           })
							else PrimaryTabRow(selectedTabIndex = pagerState.currentPage, modifier = Modifier.fillMaxWidth(), containerColor = Color.Transparent, divider = {}, tabs = tabContent, indicator = {
								TabRowDefaults.PrimaryIndicator(modifier = Modifier.tabIndicatorOffset(selectedTabIndex = pagerState.currentPage, matchContentSize = true),
								                                width = Dp.Unspecified,
								                                color = MaterialTheme.colorScheme.primary,
								                                shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
							})
						}
					}
				}
			},
			bottomBar = {
				if (navs.isNotEmpty()) {
					NavigationBar(windowInsets = NavigationBarDefaults.windowInsets) {
						navs.forEachIndexed { index, navItem ->
							NavigationBarItem(selected = pagerState.currentPage == index, label = { Text(text = navItem.title ?: "") }, onClick = {
								coroutineScope.launch {
									pagerState.animateScrollToPage(index)
								}
							}, icon = navItem.icon?.let { { Icon(imageVector = it, contentDescription = "") } } ?: {})
						}
					}
				}
			},
			floatingActionButton = floatingActionButton,
		        ) { innerPadding ->
			if (tabs.isNotEmpty() || navs.size > 1) {
				HorizontalPager(
					state = pagerState,
					modifier = Modifier
						.fillMaxSize()
						.padding(innerPadding),
				               ) { page ->
					pageContent(page)
				}
			}
			else {
				if (isNestedScrollEnabled) {
					Box(
						modifier = Modifier
							.fillMaxSize()
							.padding(innerPadding)
							.verticalScroll(rememberScrollState())
							.nestedScroll(rememberNestedScrollInteropConnection()),
					   ) {
						pageContent(0)
					}
				}
				else {
					Box(
						modifier = Modifier
							.fillMaxSize()
							.padding(innerPadding),
					   ) {
						pageContent(0)
					}
				}
			}
		}
	}
}

data class MenuItem(val title: String? = null, val icon: ImageVector? = null, val enabled: Boolean = true, val onClick: () -> Boolean = { false })

@Preview(showBackground = true) @Composable fun ActivityPagerPreview() {
	SysuerTheme {
		ActivityPager(
			title = "标题",
			tabs = listOf(
				MenuItem(title = "教务通知"),
				MenuItem(title = "教务通知"),
			             ),
			pageContent = { page ->
				Box(
					modifier = Modifier.fillMaxSize(),
					contentAlignment = Alignment.Center,
				   ) {
					Text(text = "Page ${page + 1} content")
				}
			},
		             )
	}
}

@Composable fun TabIndicatorScope.LiquidTabIndicator(
	pagerState: PagerState,
	modifier: Modifier = Modifier,
                                                    ) {
	TabRowDefaults.PrimaryIndicator(modifier = modifier.tabIndicatorLayout { measurable, constraints, tabPositions ->
		if (tabPositions.isEmpty()) {
			return@tabIndicatorLayout layout(0, 0) {}
		}
		val pagePosition = pagerState.currentPage + pagerState.currentPageOffsetFraction
		val fromPage = pagePosition.toInt().coerceIn(0, tabPositions.lastIndex)
		val toPage = if (pagePosition >= fromPage) {
			(fromPage + 1).coerceAtMost(tabPositions.lastIndex)
		}
		else {
			(fromPage - 1).coerceAtLeast(0)
		}
		val progress = abs(pagePosition - fromPage).coerceIn(0f, 1f)
		val currentTab = tabPositions[fromPage]
		val targetTab = tabPositions[toPage]                /*
				 * Material You 动画曲线
				 */
		val smoothProgress = FastOutSlowInEasing.transform(progress)                /*
				 * Tab中心移动
				 */
		val currentCenter = currentTab.left + currentTab.width / 2
		val targetCenter = targetTab.left + targetTab.width / 2
		val center = lerp(currentCenter, targetCenter, smoothProgress)                /*
				 * 宽度平滑变化
				 */
		val baseWidth = lerp(currentTab.width, targetTab.width, smoothProgress)                /*
				 * 液态拉伸
				 *
				 * 中间最大
				 */
		val stretch = sin(progress * Math.PI).toFloat() * baseWidth * 0.45f
		val indicatorWidth = baseWidth + stretch
		val indicatorLeft = center - indicatorWidth / 2
		val placeable = measurable.measure(Constraints.fixed(width = indicatorWidth.roundToPx(), height = 2.dp.roundToPx()))
		
		layout(constraints.maxWidth, constraints.maxHeight) {
			placeable.placeRelative(x = indicatorLeft.roundToPx(), y = constraints.maxHeight - placeable.height)
		}
	}, height = 2.dp, shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
}

@Composable fun UnboundedTab(
	selected: Boolean,
	icon: ImageVector?,
	text: String?,
	onClick: () -> Unit,
                            ) {
	Box(modifier = Modifier
		.height(48.dp)
		.selectable(selected = selected, onClick = onClick, role = Role.Tab, interactionSource = remember { MutableInteractionSource() }, indication = ripple(bounded = false))
		.padding(dimensionResource(R.dimen.horizontal_padding), dimensionResource(R.dimen.vertical_padding))        /*.clickable(interactionSource = interactionSource, indication = ripple(bounded = false), onClick = onClick)*/,
	    contentAlignment = Alignment.Center) {
		icon?.let { Icon(imageVector = it, contentDescription = text) }
		text?.let {
			Text(text = it, color = if (selected) MaterialTheme.colorScheme.primary
			else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
		}
	}
}