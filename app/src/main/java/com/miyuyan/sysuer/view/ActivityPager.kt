package com.miyuyan.sysuer.view

import android.content.Intent
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.rounded.Output
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FlexibleBottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.TabIndicatorScope
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import top.yukonga.miuix.kmp.squircle.squircleBorder

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ActivityPager(
	modifier: Modifier = Modifier,
	title: String = "",
	expandable: Boolean = false,
	tabs: List<MenuItem> = emptyList(),
	navs: List<MenuItem> = emptyList(),
	snackbar: SnackbarHostState = remember { SnackbarHostState() },
	topBarContent: @Composable (Int) -> Unit = {},
	isTopBarContentFixed: Boolean = false,
	onNavigationClick: (() -> Unit)? = null,
	onPageChange: ((Int) -> Unit)? = null,
	isNestedScrollEnabled: Boolean = true,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
	sharedKey: Any = "toolbar",
	floatingActionButton: @Composable (Int) -> Unit = {},
	actions: @Composable (RowScope.() -> Unit)? = null,
	topBarMenus: @Composable ((Int) -> List<MenuItem>)? = null,
	pagerState: PagerState = rememberPagerState(pageCount = {
		if (tabs.isNotEmpty()) tabs.size else if (navs.isNotEmpty()) navs.size else 1
	}),
	pageContent: @Composable (page: Int) -> Unit = {}
) {
	val coroutineScope = rememberCoroutineScope()
	val scrollBehavior = if (expandable) TopAppBarDefaults.enterAlwaysScrollBehavior()
	else TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
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
					Surface(color = backgroundColor) {
						Column {
							val modifier =
								if (sharedTransitionScope != null && animatedVisibilityScope != null) {
									with(sharedTransitionScope) {
										Modifier.sharedBounds(
												sharedContentState = rememberSharedContentState(key = sharedKey),
												animatedVisibilityScope = animatedVisibilityScope
										)
									}
								} else Modifier
							val title = @Composable {
								Text(
										text = title, color = MaterialTheme.colorScheme.primary
								)
							}
							val navigationIcon = @Composable {
								if (onNavigationClick != null) IconButton(onClick = onNavigationClick) {
									Icon(
											imageVector = Icons.AutoMirrored.Filled.ArrowBack,
											contentDescription = stringResource(R.string.back),
											tint = MaterialTheme.colorScheme.primary,
									)
								}
							}
							val actionsContent = actions ?: topBarMenus?.run {
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
							} ?: {}
							val colors = TopAppBarDefaults.topAppBarColors(
									containerColor = Color.Transparent,
									scrolledContainerColor = Color.Transparent
							)
							if (expandable) MediumTopAppBar(
									modifier = modifier,
									title = title,
									navigationIcon = navigationIcon,
									actions = actionsContent,
									colors = colors,
									scrollBehavior = scrollBehavior,
							) else TopAppBar(
									modifier = modifier,
									title = title,
									navigationIcon = navigationIcon,
									actions = actionsContent,
									colors = colors,
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
													shape = RoundedCornerShape(
															topStart = 2.dp, topEnd = 2.dp
													)
											)
										})
								else PrimaryTabRow(
										selectedTabIndex = pagerState.currentPage,
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
													shape = RoundedCornerShape(
															topStart = 2.dp, topEnd = 2.dp
													)
											)
										})
							}
							if (isTopBarContentFixed) topBarContent(pagerState.currentPage) else AnimatedContent(
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
														contentDescription = navItem.title
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
	val key: String? = null,
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
	sectionData: List<SnapshotStateList<SectionData>>, tabs: List<MenuItem>, name: String
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
							"type", DataStoreManager.ContentType.MARKDOWN.name
					).putExtra("title", name)
			)
		}
		true
	}
}

@Composable
fun exportMarkdownMenuItem(
	sectionData: SnapshotStateList<SectionData>, tab: String, name: String
): MenuItem {
	val context = LocalContext.current
	return MenuItem(title = stringResource(R.string.export), icon = Icons.Rounded.Output) {
		val markdown =
			StringBuilder().append("###### $tab").append("\n\n").append(sectionData.toMarkdown())
		DataStoreManager.saveContent(context, name, "$markdown") {
			context.startActivity(
					Intent(context, RichTextActivity::class.java).putExtra(
							"type", DataStoreManager.ContentType.MARKDOWN.name
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

@Composable
fun TabIndicatorScope.SquircleAnimatedIndicator(
	index: Int,
	modifier: Modifier = Modifier,
	horizontalPadding: Dp = 16.dp,
	height: Dp = 36.dp,
	isTabScrollable: Boolean = true,
) {
	val colors = listOf(
			MaterialTheme.colorScheme.primary,
			MaterialTheme.colorScheme.secondary,
			MaterialTheme.colorScheme.tertiary,
	)

	val indicatorColor by animateColorAsState(
			targetValue = colors[index % colors.size], label = "indicatorColor"
	)

	/*
	 * 记录当前 Indicator 的左右边界。
	 *
	 * 不直接动画 width + offset，
	 * 而是分别控制 left / right，
	 * 这样才能做出“拉伸”效果。
	 */
	var startAnimatable by remember {
		mutableStateOf<Animatable<Dp, AnimationVector1D>?>(null)
	}

	var endAnimatable by remember {
		mutableStateOf<Animatable<Dp, AnimationVector1D>?>(null)
	}

	/*
	 * 保存目标值。
	 *
	 * tabIndicatorLayout 会不断重新测量，
	 * 所以不要直接在里面 launch。
	 */
	var targetStart by remember {
		mutableStateOf<Dp?>(null)
	}

	var targetEnd by remember {
		mutableStateOf<Dp?>(null)
	}

	var animationDirection by remember {
		mutableIntStateOf(0)
	}

	Box(modifier
		.tabIndicatorLayout { measurable, constraints, tabPositions ->

			val position = tabPositions.getOrNull(index) ?: return@tabIndicatorLayout layout(
					constraints.maxWidth, constraints.maxHeight
			) {}

//			val center = (position.left + position.right) / 2

			val indicatorWidth = position.contentWidth + horizontalPadding * 2

//			val contentLeft = (position.right - position.left - position.contentWidth) / 2
//			println("left ${position.left} right ${position.right}")

			val newStart =
				if (isTabScrollable) position.left - horizontalPadding//center - indicatorWidth / 2
				else (position.left + position.right) / 2 - indicatorWidth / 2
			val newEnd = newStart + indicatorWidth//position.right//center + indicatorWidth / 2

			val oldStart = targetStart
			val oldEnd = targetEnd

			if (oldStart != null && oldEnd != null) {
				animationDirection = when {
					newStart > oldStart -> 1
					newStart < oldStart -> -1
					else -> animationDirection
				}
			}

			targetStart = newStart
			targetEnd = newEnd

			val startAnim = startAnimatable ?: Animatable(
					newStart, Dp.VectorConverter
			).also {
				startAnimatable = it
			}

			val endAnim = endAnimatable ?: Animatable(
					newEnd, Dp.VectorConverter
			).also {
				endAnimatable = it
			}

			val indicatorHeight = height.roundToPx().coerceAtMost(constraints.maxHeight)

			/*
			 * Indicator 的实际宽度。
			 */
			val start = startAnim.value.roundToPx()
			val end = endAnim.value.roundToPx()

			val width = (end - start).coerceAtLeast(1)

			val placeable = measurable.measure(
					constraints.copy(
							minWidth = width,
							maxWidth = width,
							minHeight = indicatorHeight,
							maxHeight = indicatorHeight
					)
			)

			val y = (constraints.maxHeight - indicatorHeight) / 2
			layout(
					constraints.maxWidth, constraints.maxHeight
			) {
				placeable.place(
						x = start, y = y
				)
			}
		}
		.squircleBorder(
				width = 2.dp,
				color = indicatorColor,
				cornerRadius = height / 2f,
		)            /*.drawBehind {

				val stroke = 2.dp.toPx()

				*//*
				 * 由于 Indicator 高度已经独立出来，
				 * 这里的 size.height 就是 indicator 本身的高度。
				 *//*
				val radius = size.height / 2f

				drawRoundRect(
						color = indicatorColor, topLeft = Offset(
						stroke / 2, stroke / 2
				), size = Size(
						width = size.width - stroke, height = size.height - stroke
				), cornerRadius = CornerRadius(
						x = radius, y = radius
				), style = Stroke(
						width = stroke
				)
				)
			}*/)
	LaunchedEffect(targetStart, targetEnd) {

		val start = startAnimatable ?: return@LaunchedEffect

		val end = endAnimatable ?: return@LaunchedEffect

		val newStart = targetStart ?: return@LaunchedEffect

		val newEnd = targetEnd ?: return@LaunchedEffect

		if (animationDirection > 0) {

			launch {
				start.animateTo(
						newStart, spring(
						dampingRatio = 1f, stiffness = 250f
				)
				)
			}

			launch {
				end.animateTo(
						newEnd, spring(
						dampingRatio = 1f, stiffness = 1200f
				)
				)
			}

		} else {

			launch {
				start.animateTo(
						newStart, spring(
						dampingRatio = 1f, stiffness = 1200f
				)
				)
			}

			launch {
				end.animateTo(
						newEnd, spring(
						dampingRatio = 1f, stiffness = 250f
				)
				)
			}
		}
	}
}