package com.sysu.edu.view

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sysu.edu.R
import com.sysu.edu.api.SettingManager
import com.sysu.edu.theme.SysuerTheme
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class) @Composable fun ActivityPager(
	title: String = "",
	tabs: List<MenuItem> = emptyList(),
	navs: List<MenuItem> = emptyList(),
	topBarContent: @Composable (Int) -> Unit = {},
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
	val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
	val settingManager = SettingManager(LocalContext.current)
	LaunchedEffect(pagerState.currentPage) {
		onPageChange?.invoke(pagerState.currentPage)
	}
	
	SysuerTheme {
		val surface = MaterialTheme.colorScheme.surface
		val backdrop = rememberLayerBackdrop {
			drawRect(surface)
			drawContent()
		}
		Scaffold(
			modifier = Modifier
				.fillMaxSize()
				.nestedScroll(scrollBehavior.nestedScrollConnection),
			topBar = {
				val backgroundColor = lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceContainer, scrollBehavior.state.overlappedFraction)
//				val topBackdrop = rememberLayerBackdrop {
//					drawRect(backgroundColor)
//					drawContent()
//				}
				val modifier = Modifier
					.fillMaxWidth()
//					.textureBlur(
//						backdrop = backdrop,
//						shape = RoundedCornerShape(4.dp),
//						blurRadius = 36f,
//						highlight = if (settingManager.isDarkTheme) Highlight.GlassStrokeMiddleDark else Highlight.GlassStrokeMiddleLight,
//					            )
				Surface(color = backgroundColor) {
					Column{
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
							
							if (tabs.size > 4) PrimaryScrollableTabRow(edgePadding = 0.dp, selectedTabIndex = pagerState.currentPage, modifier = modifier, containerColor = Color.Transparent, divider = {}, tabs = tabContent, indicator = {
								TabRowDefaults.PrimaryIndicator(modifier = Modifier.tabIndicatorOffset(selectedTabIndex = pagerState.currentPage, matchContentSize = true),
								                                width = Dp.Unspecified,
								                                color = MaterialTheme.colorScheme.primary,
								                                shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
							})
							else PrimaryTabRow(selectedTabIndex = pagerState.currentPage, modifier = modifier, containerColor = Color.Transparent, divider = {}, tabs = tabContent, indicator = {
								TabRowDefaults.PrimaryIndicator(modifier = Modifier.tabIndicatorOffset(selectedTabIndex = pagerState.currentPage, matchContentSize = true),
								                                width = Dp.Unspecified,
								                                color = MaterialTheme.colorScheme.primary,
								                                shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
							})
						}
						AnimatedContent(targetState = pagerState.currentPage, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "topBarFade") { page ->
							topBarContent(page)
						}
					}
				}
			},
			bottomBar = {
				if (navs.isNotEmpty()) {
					if (!supportsBlur) {
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
				}
			},
			floatingActionButton = floatingActionButton,
		        ) { innerPadding ->
			if (tabs.isNotEmpty() || navs.isNotEmpty()) {
				Box(modifier = Modifier.fillMaxSize()) {
					HorizontalPager(
						state = pagerState,
						modifier = Modifier
							.fillMaxSize()
							.padding(innerPadding)
							.layerBackdrop(backdrop),
					               ) { page ->
						pageContent(page)
					}
					if (supportsBlur && navs.isNotEmpty()) {
						LiquidGlassNavBar(pagerState = pagerState, items = navs, backdrop = backdrop, onItemClick = { index ->
							coroutineScope.launch {
								pagerState.animateScrollToPage(index)
							}
						}, isDark = settingManager.isDarkTheme, modifier = Modifier.align(Alignment.BottomCenter))
					}
				}
			}
			else {
				if (isNestedScrollEnabled) Box(
					modifier = Modifier
						.fillMaxSize()
						.padding(innerPadding)
						.verticalScroll(rememberScrollState())
						.nestedScroll(rememberNestedScrollInteropConnection()),
				                              ) {
					pageContent(0)
				}
				else Box(
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

data class MenuItem(val title: String? = null, val icon: ImageVector? = null, val enabled: Boolean = true, val onClick: () -> Boolean = { false })

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