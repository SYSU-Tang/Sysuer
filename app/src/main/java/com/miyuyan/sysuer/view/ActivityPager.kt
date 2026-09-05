package com.miyuyan.sysuer.view

import android.content.Intent
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.rounded.Output
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FlexibleBottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.DataStoreManager
import com.miyuyan.sysuer.api.SettingManager
import com.miyuyan.sysuer.browser.RichTextActivity
import com.miyuyan.sysuer.nav.RichText
import com.miyuyan.sysuer.theme.SysuerTheme
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ActivityPager(
	modifier: Modifier = Modifier,
	title: String = "",
	tabs: List<MenuItem> = emptyList(),
	navs: List<MenuItem> = emptyList(),
	snackbar: SnackbarHostState = remember { SnackbarHostState() },
	topBarContent: @Composable (Int) -> Unit = {},
	onNavigationClick: (() -> Unit)? = null,
	onPageChange: ((Int) -> Unit)? = null,
	isNestedScrollEnabled: Boolean = true,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
	sharedKey: Any = "toolbar",
	floatingActionButton: @Composable (Int) -> Unit = {},
	actions: @Composable (RowScope.() -> Unit)? = null,
	topBarMenus: @Composable ((Int) -> List<MenuItem>)? = null,
	pageContent: @Composable (page: Int) -> Unit = {},
) {
	val pagerState = rememberPagerState(pageCount = {
		if (tabs.isNotEmpty()) tabs.size else if (navs.isNotEmpty()) navs.size else 1
	})
	val coroutineScope = rememberCoroutineScope()
	val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
	val settingManager = SettingManager(LocalContext.current)
	val blurEnabled =
		Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && settingManager.isBlurNavigationBar
	var isNavBarVisible by remember { mutableStateOf(true) }
	val floatingNavBarScroll = remember {
		object : NestedScrollConnection {
			override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
				if (available.y < -1) isNavBarVisible = false
				else if (available.y > 1) isNavBarVisible = true
				return Offset.Zero
			}
		}
	}
	LaunchedEffect(pagerState.currentPage) {
		onPageChange?.invoke(pagerState.currentPage)
	}

	SysuerTheme(settingManager = settingManager) {
		val surface = MaterialTheme.colorScheme.surface
		val backdrop = rememberLayerBackdrop {
			drawRect(surface)
			drawContent()
		}
		val behavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()

		Scaffold(
			modifier = modifier
				.fillMaxSize()
				.nestedScroll(scrollBehavior.nestedScrollConnection)
				.nestedScroll(floatingNavBarScroll)
				.nestedScroll(behavior.nestedScrollConnection),
			snackbarHost = { SnackbarHost(snackbar) },
			topBar = {
				val backgroundColor = lerp(
					MaterialTheme.colorScheme.surface,
					MaterialTheme.colorScheme.surfaceContainer,
					scrollBehavior.state.overlappedFraction
				)
//				val topBackdrop = rememberLayerBackdrop {
//					drawRect(backgroundColor)
//					drawContent()
//				}
				val modifier = Modifier.fillMaxWidth()
//					.textureBlur(
//						backdrop = backdrop,
//						shape = RoundedCornerShape(4.dp),
//						blurRadius = 36f,
//						highlight = if (settingManager.isDarkTheme) Highlight.GlassStrokeMiddleDark else Highlight.GlassStrokeMiddleLight,
//					            )
				Surface(color = backgroundColor) {
					Column {
						TopAppBar(
							modifier = Modifier.then(
								if (sharedTransitionScope != null && animatedVisibilityScope != null) {
								with(sharedTransitionScope) {
									Modifier.sharedBounds(
										sharedContentState = rememberSharedContentState(key = sharedKey),
										animatedVisibilityScope = animatedVisibilityScope
									)
								}
							} else Modifier),
							title = {
								Text(
									text = title,
									color = MaterialTheme.colorScheme.primary
								)
							},
							navigationIcon = {
								if (onNavigationClick != null) IconButton(onClick = onNavigationClick) {
									Icon(
										imageVector = Icons.AutoMirrored.Filled.ArrowBack,
										contentDescription = stringResource(R.string.back),
										tint = MaterialTheme.colorScheme.primary,
									)
								}
							},
							actions = actions ?: topBarMenus?.run {
								{
									invoke(pagerState.currentPage).forEach { menu ->
										menu.icon?.let {
											IconButton(onClick = { menu.onClick() }) {
												Icon(
													imageVector = it,
													contentDescription = menu.title,
													tint = MaterialTheme.colorScheme.primary
												)
											}
										} ?: run {
											menu.title?.let {
												TextButton(onClick = { menu.onClick() }) {
													Text(
														text = it,
														color = MaterialTheme.colorScheme.primary
													)
												}
											}
										}
										menu.content()
									}
								}
							} ?: {},
							colors = TopAppBarDefaults.topAppBarColors(
								containerColor = Color.Transparent,
								scrolledContainerColor = Color.Transparent
							),
							scrollBehavior = scrollBehavior,
						)
						if (tabs.isNotEmpty()) {
							val tabContent = @Composable {
								tabs.forEachIndexed { index, tabItem ->
									val selected = pagerState.currentPage == index
									UnboundedTab(
										selected = selected,
										icon = tabItem.icon,
										text = tabItem.title,
										onClick = {
											coroutineScope.launch {
												pagerState.animateScrollToPage(index)
											}
										})
								}
							}

							if (tabs.size > 4) PrimaryScrollableTabRow(
								edgePadding = 0.dp,
								selectedTabIndex = pagerState.currentPage,
								modifier = modifier,
								containerColor = Color.Transparent,
								divider = {},
								tabs = tabContent,
								indicator = {
									TabRowDefaults.PrimaryIndicator(
										modifier = Modifier.tabIndicatorOffset(
											selectedTabIndex = pagerState.currentPage,
											matchContentSize = true
										),
										width = Dp.Unspecified,
										color = MaterialTheme.colorScheme.primary,
										shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
									)
								})
							else PrimaryTabRow(
								selectedTabIndex = pagerState.currentPage,
								modifier = modifier,
								containerColor = Color.Transparent,
								divider = {},
								tabs = tabContent,
								indicator = {
									TabRowDefaults.PrimaryIndicator(
										modifier = Modifier.tabIndicatorOffset(
											selectedTabIndex = pagerState.currentPage,
											matchContentSize = true
										),
										width = Dp.Unspecified,
										color = MaterialTheme.colorScheme.primary,
										shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
									)
								})
						}
						AnimatedContent(
							targetState = pagerState.currentPage,
							transitionSpec = { expandVertically() togetherWith shrinkVertically() },
							label = "topBarExpand"
						) { page ->
							topBarContent(page)
						}
					}
				}
			},
			bottomBar = {
				if (navs.isNotEmpty() && !blurEnabled) {
					FlexibleBottomAppBar(scrollBehavior = behavior) {
						navs.forEachIndexed { index, navItem ->
							NavigationBarItem(
								selected = pagerState.currentPage == index,
								label = { Text(text = navItem.title ?: "") },
								onClick = {
									coroutineScope.launch {
										pagerState.animateScrollToPage(index)
									}
								},
								icon = navItem.icon?.let {
									{
										Icon(
											imageVector = it,
											contentDescription = ""
										)
									}
								} ?: {})
						}
					}
				}
			},
			floatingActionButton = { floatingActionButton(pagerState.currentPage) },
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
					if (blurEnabled && navs.isNotEmpty()) {
						AnimatedVisibility(
							visible = isNavBarVisible,
							enter = slideInVertically(initialOffsetY = { it }),
							exit = slideOutVertically(targetOffsetY = { it }),
							modifier = Modifier.align(Alignment.BottomCenter)
						) {
							LiquidGlassNavBar(
								pagerState = pagerState,
								items = navs,
								backdrop = backdrop,
								onItemClick = { index ->
									coroutineScope.launch {
										pagerState.animateScrollToPage(index)
									}
								},
								isDark = settingManager.isDarkTheme
							)
						}
					}
				}
			} else {
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

data class MenuItem(
	val title: String? = null,
	val icon: ImageVector? = null, /*val painter: Painter? = null,*/
	val enabled: Boolean = true,
	val content: @Composable () -> Unit = {},
	val onClick: () -> Boolean = { false }
)

@Composable
fun UnboundedTab(
	selected: Boolean,
	icon: ImageVector?,
	text: String?,
	onClick: () -> Unit,
) {
	Box(
		modifier = Modifier
			.height(48.dp)
			.selectable(
				selected = selected,
				onClick = onClick,
				role = Role.Tab,
				interactionSource = remember { MutableInteractionSource() },
				indication = ripple(bounded = false)
			)
			.padding(
				dimensionResource(R.dimen.horizontal_padding),
				dimensionResource(R.dimen.vertical_padding)
			)        /*.clickable(interactionSource = interactionSource, indication = ripple(bounded = false), onClick = onClick)*/,
		contentAlignment = Alignment.Center
	) {
		icon?.let { Icon(imageVector = it, contentDescription = text) }
		text?.let {
			Text(
				text = it,
				color = if (selected) MaterialTheme.colorScheme.primary
				else MaterialTheme.colorScheme.onSurfaceVariant,
				style = MaterialTheme.typography.bodyMedium,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)
		}
	}
}


@Composable
fun exportMarkdownMenuItem(
	sectionData: List<SnapshotStateList<SectionData>>,
	tabs: List<MenuItem>,
	name: String
): MenuItem {
	val context = LocalContext.current
	return MenuItem(title = stringResource(R.string.export), icon = Icons.Rounded.Output) {
		val markdown = StringBuilder()
		sectionData.zip(tabs).forEachIndexed { index, (section, tab) ->
			markdown.append("###### ${tab.title}").append("\n\n").append(section.toMarkdown())
			if (index < sectionData.size - 1) markdown.append("\n\n---\n\n")
		}
		DataStoreManager.saveContent(context, name, "$markdown") {
			context.startActivity(
				Intent(context, RichTextActivity::class.java).putExtra(
						"type",
						DataStoreManager.ContentType.MARKDOWN.name
					).putExtra("title", name)
			)
		}
		true
	}
}

@Composable
fun exportMarkdownMenuItem(
	sectionData: SnapshotStateList<SectionData>,
	tab: String,
	name: String
): MenuItem {
	val context = LocalContext.current
	return MenuItem(title = stringResource(R.string.export), icon = Icons.Rounded.Output) {
		val markdown =
			StringBuilder().append("###### $tab").append("\n\n").append(sectionData.toMarkdown())
		DataStoreManager.saveContent(context, name, "$markdown") {
			context.startActivity(
				Intent(context, RichTextActivity::class.java).putExtra(
						"type",
						DataStoreManager.ContentType.MARKDOWN.name
					).putExtra("title", name)
			)
		}
		true
	}
}

@Composable
fun exportMarkdownMenuItem(
	backStack: MutableList<NavKey>,
	sectionData: SnapshotStateList<SectionData>,
	tab: String,
	name: String
): MenuItem = MenuItem(title = stringResource(R.string.export), icon = Icons.Rounded.Output) {
	val markdown =
		StringBuilder().append("###### $tab").append("\n\n").append(sectionData.toMarkdown())
	backStack.add(
		RichText(
			title = name,
			content = "$markdown",
			contentType = DataStoreManager.ContentType.MARKDOWN.name
		)
	)
	true
}

@Composable
fun exportMarkdownMenuItem(
	backStack: MutableList<NavKey>,
	sectionData: List<SnapshotStateList<SectionData>>,
	tabs: List<MenuItem>,
	name: String
): MenuItem = MenuItem(title = stringResource(R.string.export), icon = Icons.Rounded.Output) {
	val markdown = StringBuilder()
	sectionData.zip(tabs).forEachIndexed { index, (section, tab) ->
		markdown.append("###### ${tab.title}").append("\n\n").append(section.toMarkdown())
		if (index < sectionData.size - 1) markdown.append("\n\n---\n\n")
	}
	backStack.add(
		RichText(
			title = name,
			content = "$markdown",
			contentType = DataStoreManager.ContentType.MARKDOWN.name
		)
	)
	true
}