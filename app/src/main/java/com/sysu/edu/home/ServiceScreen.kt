package com.sysu.edu.home

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Shortcut
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.KeyboardVoice
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Output
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ChipColors
import androidx.compose.material3.ChipElevation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.navigation3.runtime.NavKey
import com.alibaba.fastjson2.toJSONString
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.rememberMarkdownState
import com.sysu.edu.MainActivity
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.ContextUtil
import com.sysu.edu.browser.BrowserActivity
import kotlinx.coroutines.launch
import kotlin.reflect.full.primaryConstructor

@OptIn(
	ExperimentalMaterial3Api::class,
	ExperimentalLayoutApi::class,
	ExperimentalFoundationApi::class
)
@Composable
internal fun ServiceScreen(
	homeViewModel: HomeViewModel,
	serviceViewModel: ServiceViewModel,
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val context = LocalContext.current
	val config = remember { ContextUtil(context) }
	var showActionItem by remember { mutableStateOf<ServiceConfig?>(null) }
	var showOrderDialog by rememberSaveable { mutableStateOf(false) }
	val collection = serviceViewModel.collection
	val serviceData = serviceViewModel.serviceData

	LaunchedEffect(Unit) {
		serviceViewModel.loadCollection()
		serviceViewModel.loadServiceData()
	}
	ServiceActionDialog(
		item = showActionItem,
		onDismiss = { showActionItem = null },
		onShowOrder = { _ -> showActionItem = null; showOrderDialog = true },
		serviceViewModel = serviceViewModel,
		homeViewModel = homeViewModel,
		config = config,
	)

	ServiceOrderDialog(
		show = showOrderDialog,
		onDismiss = { showOrderDialog = false },
		serviceViewModel = serviceViewModel,
	)
	val nestedScrollConnection = rememberNestedScrollInteropConnection()
	val verticalMargin = dimensionResource(R.dimen.vertical_margin)

	LazyColumn(
		modifier = Modifier
			.fillMaxSize()
			.nestedScroll(nestedScrollConnection),
		verticalArrangement = Arrangement.spacedBy(verticalMargin),
	) {
		if (collection.isNotEmpty()) {
			item(key = "collection") {
				ServiceBox(
					title = stringResource(R.string.collect),
					items = serviceViewModel.collection,
					onItemClick = { navigateToServiceItem(context, backStack, it, config) },
					onItemLongClick = { showActionItem = it },
					onTitleClick = { showOrderDialog = true },
					sharedTransitionScope = sharedTransitionScope,
					animatedVisibilityScope = animatedVisibilityScope,
				)
			}
		}

		items(serviceData, key = { it.first }) { (name, items) ->
			ServiceBox(
				title = name,
				items = items,
				onItemClick = { navigateToServiceItem(context, backStack, it, config) },
				onItemLongClick = { showActionItem = it },
				sharedTransitionScope = sharedTransitionScope,
				animatedVisibilityScope = animatedVisibilityScope,
			)
		}

		item(key = "bottomSpacer") {
			Spacer(modifier = Modifier.height(verticalMargin))
		}
	}
}

private fun navigateToServiceItem(
	context: Context,
	backStack: MutableList<NavKey>,
	item: ServiceConfig,
	config: ContextUtil
) {
	when {
		!item.route.isNullOrBlank() -> {
			runCatching<NavKey?> {
				val kClass = Class.forName("${context.packageName}.nav.${item.route}").kotlin
				kClass.objectInstance as? NavKey
					?: kClass.primaryConstructor?.callBy(emptyMap()) as? NavKey
			}.getOrNull()?.let { backStack.add(it) }
		}
		else -> getServiceItemIntent(context, item, null)?.let {
			(context as FragmentActivity).startActivity(
				it,
				ActivityOptionsCompat.makeSceneTransitionAnimation(context).toBundle()
			)
		} ?: config.toast(R.string.activity_not_found)
	}
}

private fun getServiceItemIntent(context: Context, item: ServiceConfig, intent: Intent?): Intent? {
	return when {
		!item.activity.isNullOrBlank() -> {
			try {
				Intent(context, Class.forName(context.packageName + item.activity)).takeIf {
					it.resolveActivity(context.packageManager) != null
				} ?: intent
			} catch (_: Exception) {
				intent
			}
		}

		!item.url.isNullOrBlank() -> {
			Intent(context, BrowserActivity::class.java).setData(CommonUtil.trim(item.url).toUri())
		}

		else -> intent
	}
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun ServiceBox(
	title: String,
	items: List<ServiceConfig>,
	onItemClick: (ServiceConfig) -> Unit,
	onItemLongClick: (ServiceConfig) -> Unit,
	onTitleClick: (() -> Unit)? = null,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	Row(
		modifier = Modifier.fillMaxWidth().padding(
			horizontal = dimensionResource(R.dimen.horizontal_margin),
			vertical = dimensionResource(R.dimen.vertical_margin)
		).apply {
			if (onTitleClick != null) combinedClickable(onClick = onTitleClick)
		},
		verticalAlignment = Alignment.CenterVertically,
	) {
		Text(
			text = title,
			style = MaterialTheme.typography.titleMedium,
			color = MaterialTheme.colorScheme.primary,
		)
	}
	Card(
		modifier = Modifier.fillMaxWidth(),
		shape = MaterialTheme.shapes.small,
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
	) {
		FlowRow(
			modifier = Modifier
				.fillMaxWidth()
				.padding(
					dimensionResource(R.dimen.horizontal_padding),
					dimensionResource(R.dimen.vertical_padding)
				),
			horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_gap)),
			verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vertical_margin)),
		) {
			items.forEach { item ->
				LongClickableElevatedAssistChip(
					modifier = Modifier.then(
						if (sharedTransitionScope != null && animatedVisibilityScope != null && item.route != null) {
						with(sharedTransitionScope) {
							Modifier.sharedBounds(
								sharedContentState = rememberSharedContentState(
									key = item.route
								), animatedVisibilityScope = animatedVisibilityScope
							)
						}
					} else Modifier),
					onClick = {
						onItemClick(item)
					},
					onLongClick = { onItemLongClick(item) },
					label = item.name,
					colors = if (item.activity.isNullOrBlank()) AssistChipDefaults.elevatedAssistChipColors(
						containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
					)
					else AssistChipDefaults.elevatedAssistChipColors(),
				)
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceActionDialog(
	item: ServiceConfig?,
	onDismiss: () -> Unit,
	onShowOrder: (ServiceConfig?) -> Unit,
	serviceViewModel: ServiceViewModel,
	homeViewModel: HomeViewModel,
	config: ContextUtil,
) {
	if (item == null) return
	val context = LocalContext.current
	val coroutineScope = rememberCoroutineScope()
	val itemId = item.id
	var isServiceCollected by remember { mutableStateOf(false) }
	var isShortcutCollected by remember { mutableStateOf(false) }
	val name = item.name ?: ""
	val description = item.description
	val url = item.url
	val markdown = StringBuilder("### $name\n$description")
	if (!url.isNullOrBlank()) markdown.append("\n`$url`")
	LaunchedEffect(item) {
		isServiceCollected = serviceViewModel.isServiceCollected(itemId)
		isShortcutCollected = serviceViewModel.isDashboardShortcutCollected(itemId)
	}

	ModalBottomSheet(onDismissRequest = onDismiss) {
		Column(modifier = Modifier.fillMaxWidth()) {
			Card(
				modifier = Modifier
					.fillMaxWidth()
					.padding(
						horizontal = dimensionResource(R.dimen.horizontal_margin),
						vertical = dimensionResource(R.dimen.vertical_margin)
					),
			) {
				Markdown(
					rememberMarkdownState("$markdown"),
					colors = markdownColor(),
					typography = markdownTypography(h3 = MaterialTheme.typography.titleMediumEmphasized),
					modifier = Modifier.padding(dimensionResource(R.dimen.content_padding)),
				)
			}

			FlowRow(
				modifier = Modifier
					.fillMaxWidth()
					.padding(
						horizontal = dimensionResource(R.dimen.horizontal_margin),
						vertical = dimensionResource(R.dimen.vertical_margin)
					),
				horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_gap)),
				verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vertical_gap)),
			) {
				GenericTonalButton(
					image = if (isServiceCollected) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
					text = stringResource(if (isServiceCollected) R.string.cancel_collect else R.string.collect)
				) {
					isServiceCollected = !isServiceCollected
					coroutineScope.launch {
						if (isServiceCollected) {
							serviceViewModel.addService(itemId, item.toJSONString())
							config.toast(R.string.collect_success)
						} else {
							serviceViewModel.deleteService(itemId)
							config.toast(R.string.cancel_collect_success)
						}
						serviceViewModel.loadCollection()
					}
				}

				GenericTonalButton(
					image = if (isShortcutCollected) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.Shortcut,
					text = stringResource(if (isShortcutCollected) R.string.cancel_add_shortcut else R.string.add_to_dashboard)
				) {
					isShortcutCollected = !isShortcutCollected
					coroutineScope.launch {
						if (isShortcutCollected) {
							serviceViewModel.addDashboardShortcut(itemId, item.toJSONString(), null)
							config.toast(R.string.add_shortcut_success)
						} else {
							serviceViewModel.deleteDashboardShortcut(itemId)
							config.toast(R.string.cancel_add_shortcut_success)
						}
						homeViewModel.updateDashboardShortcut.value = true
					}
				}

				GenericTonalButton(
					image = Icons.Rounded.Output,
					text = stringResource(R.string.add_to_launcher)
				) {
					if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
						getServiceItemIntent(
							context,
							item,
							Intent(context, MainActivity::class.java)
						)?.let { intent ->
							ShortcutInfoCompat.Builder(context, "$itemId").setShortLabel(name)
								.setLongLabel(name)
								.setIcon(IconCompat.createWithResource(context, R.mipmap.icon))
								.setIntent(intent.setAction(Intent.ACTION_VIEW)).build()
						}?.let { info ->
							ShortcutManagerCompat.requestPinShortcut(
								context, info,
								PendingIntent.getBroadcast(
									context, 0,
									ShortcutManagerCompat.createShortcutResultIntent(context, info),
									PendingIntent.FLAG_IMMUTABLE,
								).intentSender,
							)
						}
					} else config.toast(R.string.fail_to_add_shortcut)
				}

				GenericTonalButton(
					image = Icons.Rounded.ClearAll,
					text = stringResource(R.string.service_order)
				) {
					onShowOrder(item)
				}

				GenericTonalButton(
					image = Icons.Rounded.KeyboardVoice,
					text = stringResource(R.string.feedback)
				) {
					context.startActivity(
						Intent(Intent.ACTION_VIEW).setData("https://github.com/SYSU-Tang/Sysuer/issues/new?title=反馈：服务->$name&labels=bug,crash-report".toUri())
							.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
					)
				}

				GenericTonalButton(
					image = Icons.Rounded.Link,
					text = stringResource(R.string.open_as_url)
				) {
					val itemUrl = item.url
					if (!itemUrl.isNullOrBlank()) context.startActivity(
						Intent(
							context,
							BrowserActivity::class.java
						).setData(itemUrl.toUri())
					)
				}

				GenericTonalButton(
					image = Icons.Rounded.Book,
					text = stringResource(R.string.guide)
				) {
					if (!item.doc.isNullOrBlank()) context.startActivity(
						Intent(
							context,
							BrowserActivity::class.java
						).setData("https://sysu-tang.github.io/sysuer-website${CommonUtil.trim(item.doc)}".toUri())
					)
					else config.toast(R.string.undeveloped_warning)
				}
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceOrderDialog(
	show: Boolean,
	onDismiss: () -> Unit,
	serviceViewModel: ServiceViewModel,
) {
	if (!show) return
	val orderCollection = serviceViewModel.orderCollection

	LaunchedEffect(Unit) {
		serviceViewModel.loadOrderCollection()
	}

	ModalBottomSheet(onDismissRequest = onDismiss) {
		Column(modifier = Modifier.fillMaxWidth()) {
			Text(
				stringResource(R.string.service_order),
				style = MaterialTheme.typography.titleMedium,
				modifier = Modifier.padding(
					dimensionResource(R.dimen.horizontal_padding),
					dimensionResource(R.dimen.vertical_margin)
				),
			)
			LazyColumn(modifier = Modifier.fillMaxWidth()) {
				itemsIndexed(orderCollection, key = { _, item -> item.id }) { index, item ->
					ListItem(
						overlineContent = {
							item.name?.let {
								Text(
									it,
									maxLines = 1,
									overflow = TextOverflow.Ellipsis,
									style = MaterialTheme.typography.titleMedium,
								)
							}
						},
						leadingContent = {
							Row {
								IconButton(
									onClick = {
										if (index > 0) serviceViewModel.moveOrderCollection(
											index,
											index - 1
										)
									},
									enabled = index > 0,
								) {
									Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = null)
								}
								IconButton(
									onClick = {
										if (index < orderCollection.lastIndex) serviceViewModel.moveOrderCollection(
											index,
											index + 1
										)
									},
									enabled = index < orderCollection.lastIndex,
								) {
									Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
								}
							}
						},
						modifier = Modifier.animateItem(),
					) {}
				}
			}
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(
						dimensionResource(R.dimen.horizontal_margin),
						dimensionResource(R.dimen.vertical_margin)
					),
				horizontalArrangement = Arrangement.End,
			) {
				TextButton(onClick = {
					serviceViewModel.saveOrderCollection()
					onDismiss()
				}) { Text(stringResource(R.string.confirm)) }
			}
		}
	}
}

@Composable
fun LongClickableElevatedAssistChip(
	modifier: Modifier = Modifier,
	label: String? = null,
	onClick: () -> Unit = {},
	onLongClick: () -> Unit = {},
	leadingIcon: (@Composable () -> Unit)? = null,
	colors: ChipColors = AssistChipDefaults.elevatedAssistChipColors(),
	elevation: ChipElevation = AssistChipDefaults.elevatedAssistChipElevation(),
	enabled: Boolean = true,
	content: @Composable RowScope.() -> Unit = {},
) {
	Surface(
		shape = AssistChipDefaults.shape,
		modifier = modifier.height(AssistChipDefaults.Height),
		color = if (enabled) colors.containerColor else colors.disabledContainerColor,
		contentColor = if (enabled) colors.labelColor else colors.disabledLabelColor,
		shadowElevation = if (enabled) elevation.elevation else elevation.disabledElevation,
		tonalElevation = if (enabled) elevation.elevation else elevation.disabledElevation,
	) {
		Row(
			modifier = Modifier
				.combinedClickable(
					interactionSource = remember { MutableInteractionSource() },
					role = Role.Button,
					enabled = enabled,
					onClick = onClick,
					onLongClick = onLongClick,
				)
				.padding(AssistChipDefaults.ContentPadding),
			verticalAlignment = Alignment.CenterVertically
		) {
			if (leadingIcon != null) {
				leadingIcon()
			}
			if (label != null && leadingIcon != null) {
				Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
			}
			label?.let {
				Text(text = it, style = MaterialTheme.typography.labelLarge)
			}
			content()
		}
	}
}

@Composable
fun GenericTonalButton(
	image: Int,
	text: String = "",
	enable: Boolean = true,
	onClick: () -> Unit = {},
) {
	FilledTonalButton(onClick = onClick, enabled = enable, shapes = ButtonDefaults.shapes()) {
		Icon(
			painter = painterResource(image),
			contentDescription = text,
			tint = MaterialTheme.colorScheme.primary,
			modifier = Modifier.size(ButtonDefaults.IconSize)
		)
		Spacer(Modifier.size(ButtonDefaults.IconSpacing))
		Text(text)
	}
}

@Composable
fun GenericTonalButton(
	image: ImageVector,
	text: String = "",
	enable: Boolean = true,
	onClick: () -> Unit = {},
) {
	FilledTonalButton(onClick = onClick, enabled = enable, shapes = ButtonDefaults.shapes()) {
		Icon(image, contentDescription = text, modifier = Modifier.size(ButtonDefaults.IconSize))
		Spacer(Modifier.size(ButtonDefaults.IconSpacing))
		Text(text)
	}
}