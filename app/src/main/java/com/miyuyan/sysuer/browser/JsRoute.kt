package com.miyuyan.sysuer.browser

import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Input
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.alibaba.fastjson2.JSONArray
import com.miyuyan.preference.EditPreference
import com.miyuyan.preference.MenuPreference
import com.miyuyan.preference.Preference
import com.miyuyan.preference.PreferenceCategory
import com.miyuyan.preference.PreferenceScreen
import com.miyuyan.preference.SwitchPreference
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.ContextUtil
import com.miyuyan.sysuer.browser.data.BrowserRepository
import com.miyuyan.sysuer.browser.data.JavaScriptEntity
import com.miyuyan.sysuer.browser.data.JsModel
import com.miyuyan.sysuer.browser.data.ScriptManager.checkForUpdate
import com.miyuyan.sysuer.browser.data.ScriptParser.parseFromUrl
import com.miyuyan.sysuer.nav.JsDetail
import com.miyuyan.sysuer.nav.JsList
import com.miyuyan.sysuer.nav.navigateBack
import com.miyuyan.sysuer.view.ActivityPager
import com.miyuyan.sysuer.view.MenuItem
import com.miyuyan.sysuer.view.StatePage
import com.miyuyan.sysuer.view.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 匹配/排除规则的编辑状态；index 为 -1 时表示新增 */
private data class PatternEdit(val isMatch: Boolean, val index: Int, val draft: String)

/** 脚本更新检查状态：Idle 无更新链接；Checking 检查中；Failed 检查失败；NoUpdate/HasUpdate 检查完成 */
private enum class UpdateCheckState { Idle, Checking, Failed, NoUpdate, HasUpdate }

@Composable
private fun PreferenceIcon(drawableId: Int) {
	Icon(
			painter = painterResource(drawableId),
			contentDescription = null,
			tint = MaterialTheme.colorScheme.primary
	)
}

/** 列表页与详情页共用同一 ViewModelStoreOwner 下的 JsModel，保证数据同步 */
@Composable
private fun rememberJsModel(): JsModel {
	val context = LocalContext.current
	val lifecycleScope = LocalLifecycleOwner.current.lifecycleScope
	val repository = remember { BrowserRepository(context, lifecycleScope) }
	return viewModel { JsModel(repository) }
}

/**
 * 脚本管理列表页：以偏好项展示脚本，悬浮按钮"新建"、顶栏菜单"导入"添加脚本，
 * 条目菜单支持编辑/禁用/删除；点击条目压入 [JsDetail]。autoAdd 为 true 时进入后
 * 自动新建脚本并跳转详情。
 */
@Composable
fun JsListRoute(
	backStack: MutableList<NavKey>,
	navKey: JsList = backStack.last() as JsList,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val context = LocalContext.current
	val activity = LocalActivity.current
	val contextUtil = remember { ContextUtil.getInstance(context) }
	val jsModel = rememberJsModel()
	var jsList by remember { mutableStateOf<List<JavaScriptEntity>>(emptyList()) }
	fun reload() = jsModel.loadJs { jsList = it }
	val coroutineScope = rememberCoroutineScope()
	var jsMenuFor by remember { mutableStateOf<JavaScriptEntity?>(null) }
	var showImportDialog by remember { mutableStateOf(false) }
	var importLink by rememberSaveable { mutableStateOf("") }
	val newScript = stringResource(R.string.new_script)

	var launchHandled by rememberSaveable { mutableStateOf(false) }
	LaunchedEffect(Unit) {
		reload()
		if (navKey.autoAdd && !launchHandled) {
			launchHandled = true
			jsModel.addJs(
					JavaScriptEntity(
							title = newScript,
							namespace = "Your Namespace",
							version = "1.0.0",
							author = "You",
							description = "Hello world!",
							script = NEW_SCRIPT_TEMPLATE
					)
			) { id ->
				// 重置 autoAdd 标记，避免返回列表时重复新建
				if (backStack.lastOrNull() is JsList) backStack[backStack.size - 1] = JsList()
				backStack.add(JsDetail(id))
			}
		}
	}

	ActivityPager(
			title = stringResource(R.string.js),
			isNestedScrollEnabled = false,
			sharedKey = "JsList",
			sharedTransitionScope = sharedTransitionScope,
			animatedVisibilityScope = animatedVisibilityScope,
			onNavigationClick = { backStack.navigateBack(activity) },
			topBarMenus = {
				listOf(
						MenuItem(
								stringResource(R.string.importing).removeSuffix("…"),
								iconVector = Icons.AutoMirrored.Rounded.Input
						) {
							importLink = ""
							showImportDialog = true
						})
			},
			floatingActionButton = {
				ExtendedFloatingActionButton(
						onClick = {
							jsModel.addJs(
									JavaScriptEntity(
											title = newScript,
											namespace = "Your Namespace",
											version = "1.0.0",
											author = "You",
											description = "Hello world!",
											script = NEW_SCRIPT_TEMPLATE
									)
							) { id -> backStack.add(JsDetail(id)) }
						},
						icon = {
							Icon(
									painter = painterResource(R.drawable.add),
									contentDescription = stringResource(R.string.add)
							)
						},
						text = { Text(stringResource(R.string.add)) },
				)
			},
	) { _ ->
		if (jsList.isEmpty()) {
			StatePage(state = UiState.Empty) {}
		} else {
			PreferenceScreen {
				PreferenceCategory {
					jsList.forEach { js ->
						item {
							Preference(
									modifier = Modifier.then(
											if (sharedTransitionScope != null && animatedVisibilityScope != null) {
										with(sharedTransitionScope) {
											Modifier.sharedBounds(
													sharedContentState = rememberSharedContentState(
															key = js.id.toString()
													),
													animatedVisibilityScope = animatedVisibilityScope
											)
										}
									} else Modifier),
									onClick = { backStack.add(JsDetail(js.id)) },
									title = js.title ?: "",
									summary = when {
										js.state != 1 -> stringResource(R.string.disable) + (js.description?.let { " · $it" }
											?: "")

										else -> js.description
									},
									icon = { PreferenceIcon(R.drawable.js) },
									trailing = {
										Box {
											IconButton(onClick = { jsMenuFor = js }) {
												Icon(
														Icons.Rounded.MoreVert,
														contentDescription = null
												)
											}
											DropdownMenu(
													expanded = jsMenuFor == js,
													onDismissRequest = { jsMenuFor = null },
											) {
												DropdownMenuItem(
														text = { Text(stringResource(R.string.edit)) },
														onClick = {
															jsMenuFor = null
															backStack.add(JsDetail(js.id))
														},
												)
												DropdownMenuItem(
														text = {
															Text(
																	stringResource(
																			if (js.state == 1) R.string.disable else R.string.enable
																	)
															)
														},
														onClick = {
															jsMenuFor = null
															jsModel.updateJs(js.copy(state = 1 - js.state)) { reload() }
														},
												)
												DropdownMenuItem(
														text = { Text(stringResource(R.string.delete)) },
														onClick = {
															jsMenuFor = null
															jsModel.deleteJs(js) { reload() }
														},
												)
											}
										}
									},
							)
						}
					}
				}
			}
		}
	}
	// 导入脚本对话框
	if (showImportDialog) {
		AlertDialog(
				onDismissRequest = { showImportDialog = false },
				title = { Text(stringResource(R.string.link)) },
				text = {
					OutlinedTextField(
							value = importLink,
							onValueChange = { importLink = it },
							placeholder = { Text(stringResource(R.string.link)) },
							singleLine = true,
							modifier = Modifier.fillMaxWidth()
					)
				},
				confirmButton = {
					TextButton(
							onClick = {
								showImportDialog = false
								if (importLink.isNotBlank()) {
									contextUtil.toast(R.string.importing)
									coroutineScope.launch {
										val js = withContext(Dispatchers.IO) {
											parseFromUrl(importLink)
										}
										if (js != null) {
											jsModel.addJs(js) {
												contextUtil.toast(R.string.import_success)
												reload()
											}
										} else contextUtil.toast(R.string.import_fail)
									}
								}
							}, shapes = ButtonDefaults.shapes()
					) { Text(stringResource(R.string.confirm)) }
				},
				dismissButton = {
					TextButton(
							onClick = { showImportDialog = false }, shapes = ButtonDefaults.shapes()
					) { Text(stringResource(R.string.cancel)) }
				},
		)
	}
}

/**
 * 脚本详情页：用偏好设置编辑元信息、匹配与排除规则，支持检查更新与重装，
 * 编辑代码跳转 JSEditorActivity。
 */
@Composable
fun JsDetailRoute(
	backStack: MutableList<NavKey>,
	navKey: JsDetail = backStack.last() as JsDetail,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val context = LocalContext.current
	val activity = LocalActivity.current
	val contextUtil = remember { ContextUtil.getInstance(context) }
	val jsModel = rememberJsModel()
	var detail by remember { mutableStateOf<JavaScriptEntity?>(null) }
	fun reload() = jsModel.getJs(navKey.id) { detail = it }
	val coroutineScope = rememberCoroutineScope()
	var newVersion by remember { mutableStateOf<JavaScriptEntity?>(null) }
	var updateCheckState by remember { mutableStateOf(UpdateCheckState.Idle) }
	var checkRetry by remember { mutableIntStateOf(0) }
	var patternEdit by remember { mutableStateOf<PatternEdit?>(null) }

	fun update(entity: JavaScriptEntity) {
		// 先本地生效保证界面立即刷新，再写库并回读同步
		detail = entity
		jsModel.updateJs(entity) { reload() }
	}

	LaunchedEffect(Unit) {
		reload()
	}

	// 没有更新链接的脚本无需检查更新；key 只取 id 与链接（外加手动重试计数），
	// 编辑其他字段触发的回读不会重启检查（旧实现以实体实例为 key，
	// 每次回读都重置并重查，网络慢时更新项会长时间消失）
	LaunchedEffect(detail?.id, detail?.updateURL, detail?.downloadURL, checkRetry) {
		val entity = detail
		if (entity == null || (entity.updateURL.isNullOrBlank() && entity.downloadURL.isNullOrBlank())) {
			newVersion = null
			updateCheckState = UpdateCheckState.Idle
			return@LaunchedEffect
		}
		newVersion = null
		updateCheckState = UpdateCheckState.Checking
		updateCheckState = try {
			val remote = checkForUpdate(entity)
			newVersion = remote
			if (remote != null) UpdateCheckState.HasUpdate else UpdateCheckState.NoUpdate
		} catch (e: CancellationException) {
			throw e
		} catch (_: Exception) {
			UpdateCheckState.Failed
		}
	}

	ActivityPager(
			title = detail?.title ?: stringResource(R.string.js),
			isNestedScrollEnabled = false,
			sharedKey = navKey.id.toString(),
			sharedTransitionScope = sharedTransitionScope,
			animatedVisibilityScope = animatedVisibilityScope,
			onNavigationClick = { backStack.navigateBack(activity) },
			floatingActionButton = {
				detail?.let {
					ExtendedFloatingActionButton(
							onClick = { update(it) },
							icon = {
								Icon(
										painter = painterResource(R.drawable.save),
										contentDescription = stringResource(R.string.save)
								)
							},
							text = { Text(stringResource(R.string.save)) },
					)
				}
			},
	) { _ ->
		StatePage(state = if (detail == null) UiState.Empty else UiState.Content) {
			val detail = detail ?: return@StatePage
			val runNames = stringArrayResource(R.array.run).toList()
			val runValues = stringArrayResource(R.array.run_at).toList()
			PreferenceScreen {
				PreferenceCategory {
					item {
						EditPreference(
								title = stringResource(R.string.title),
								value = detail.title ?: "",
								onChange = { _, _, value -> update(detail.copy(title = value)) },
								icon = { PreferenceIcon(R.drawable.text) },
						)
					}
					item {
						EditPreference(
								title = stringResource(R.string.description),
								value = detail.description ?: "",
								onChange = { _, _, value ->
									update(detail.copy(description = value))
								},
								icon = { PreferenceIcon(R.drawable.text) },
						)
					}
					item {
						MenuPreference(
								title = stringResource(R.string.run),
								icon = { PreferenceIcon(R.drawable.time) },
								summary = runNames.getOrElse(
										runValues.indexOf(detail.runAt)
								) { "" },
								entries = runNames,
								entryValues = runValues,
								selectedIndex = runValues.indexOf(detail.runAt).takeIf { it >= 0 },
								onChange = { _, _, value ->
									value?.let { update(detail.copy(runAt = it)) }
								},
						)
					}
				}
				PreferenceCategory(title = stringResource(R.string.action)) {
					item {
						SwitchPreference(
								title = stringResource(R.string.status),
								checked = detail.state == 1,
								onCheckedChange = { update(detail.copy(state = if (it) 1 else 0)) },
								summary = stringResource(if (detail.state == 1) R.string.enable else R.string.disable),
								icon = { PreferenceIcon(R.drawable.warning) },
						)
					}
					// 有更新链接即显示，检查完成后 newVersion 决定显示"更新"还是"重装"
					if (!detail.updateURL.isNullOrBlank() || !detail.downloadURL.isNullOrBlank()) {
						item {
							val checkState = updateCheckState
							Preference(
									onClick = {
										when (checkState) {
											UpdateCheckState.HasUpdate, UpdateCheckState.NoUpdate -> {
												contextUtil.toast(R.string.updating)
												val target =
													newVersion?.downloadURL ?: detail.downloadURL
													?: run {
														contextUtil.toast(R.string.install_failed)
														null
													}
												target?.let { url ->
													coroutineScope.launch {
														val js = withContext(Dispatchers.IO) {
															parseFromUrl(url)
														}
														if (js != null) {
															update(
																	js.copy(
																			id = detail.id,
																			position = detail.position,
																			state = detail.state,
																			run = detail.run,
																			time = detail.time,
																	)
															)
															contextUtil.toast(R.string.install_success)
														} else contextUtil.toast(R.string.install_failed)
													}
												}
											}

											UpdateCheckState.Failed -> checkRetry++
											else -> {}
										}
									},
									enabled = checkState != UpdateCheckState.Checking,
									title = stringResource(
											when (checkState) {
												UpdateCheckState.HasUpdate -> R.string.update
												UpdateCheckState.Idle, UpdateCheckState.NoUpdate -> R.string.reinstall
												else -> R.string.check_update
											}
									),
									summary = when (checkState) {
										UpdateCheckState.HasUpdate -> "${detail.version} -> ${newVersion?.version}"
										UpdateCheckState.Failed -> stringResource(R.string.check_failed)
										UpdateCheckState.Checking -> stringResource(R.string.check_update) + "…"
										else -> "${detail.version} -> ${detail.version}"
									},
									icon = { PreferenceIcon(R.drawable.down) },
							)
						}
					}
					item {
						Preference(
								onClick = {
									context.startActivity(
											Intent(
													context, JSEditorActivity::class.java
											).putExtra("id", detail.id)
									)
								},
								title = stringResource(R.string.edit),
								icon = { PreferenceIcon(R.drawable.edit) },
						)
					}
					item {
						Preference(
								onClick = { update(detail) },
								title = stringResource(R.string.save),
								icon = { PreferenceIcon(R.drawable.save) },
						)
					}
					item {
						Preference(
								onClick = {
									jsModel.deleteJs(detail) { backStack.navigateBack(activity) }
								},
								title = stringResource(R.string.delete),
								icon = { PreferenceIcon(R.drawable.delete) },
						)
					}
				}
				PreferenceCategory(title = stringResource(R.string.matches)) {
					detail.matches.forEachIndexed { index, match ->
						item {
							Preference(
									onClick = {
										patternEdit = PatternEdit(true, index, "$match")
									},
									title = stringResource(R.string.matches),
									summary = "$match",
									trailing = {
										IconButton(onClick = {
											val patterns =
												detail.matches.map { "$it" }.toMutableList()
											patterns.removeAt(index)
											update(detail.copy(matches = JSONArray(patterns)))
										}) {
											Icon(Icons.Rounded.Close, contentDescription = null)
										}
									},
							)
						}
					}
					item {
						Preference(
								onClick = { patternEdit = PatternEdit(true, -1, "") },
								title = stringResource(R.string.add),
								icon = { PreferenceIcon(R.drawable.add) },
						)
					}
				}
				PreferenceCategory(title = stringResource(R.string.exclude)) {
					detail.excludes.forEachIndexed { index, exclude ->
						item {
							Preference(
									onClick = {
										patternEdit = PatternEdit(false, index, "$exclude")
									},
									title = stringResource(R.string.exclude),
									summary = "$exclude",
									trailing = {
										IconButton(onClick = {
											val patterns =
												detail.excludes.map { "$it" }.toMutableList()
											patterns.removeAt(index)
											update(detail.copy(excludes = JSONArray(patterns)))
										}) {
											Icon(Icons.Rounded.Close, contentDescription = null)
										}
									},
							)
						}
					}
					item {
						Preference(
								onClick = { patternEdit = PatternEdit(false, -1, "") },
								title = stringResource(R.string.add),
								icon = { PreferenceIcon(R.drawable.add) },
						)
					}
				}
				PreferenceCategory(title = stringResource(R.string.info)) {
					item {
						EditPreference(
								title = stringResource(R.string.author),
								value = detail.author ?: "",
								onChange = { _, _, value -> update(detail.copy(author = value)) },
								icon = { PreferenceIcon(R.drawable.account) },
						)
					}
					item {
						EditPreference(
								title = stringResource(R.string.namespace),
								value = detail.namespace ?: "",
								onChange = { _, _, value ->
									update(detail.copy(namespace = value))
								},
								icon = { PreferenceIcon(R.drawable.account) },
						)
					}
					item {
						EditPreference(
								title = stringResource(R.string.version),
								value = detail.version ?: "",
								onChange = { _, _, value -> update(detail.copy(version = value)) },
								icon = { PreferenceIcon(R.drawable.version) },
						)
					}
					item {
						EditPreference(
								title = stringResource(R.string.script_link),
								value = detail.downloadURL ?: "",
								onChange = { _, _, value ->
									update(detail.copy(downloadURL = value))
								},
								icon = { PreferenceIcon(R.drawable.link) },
						)
					}
					item {
						EditPreference(
								title = stringResource(R.string.update_link),
								value = detail.updateURL ?: "",
								onChange = { _, _, value ->
									update(detail.copy(updateURL = value))
								},
								icon = { PreferenceIcon(R.drawable.link) },
						)
					}
					item {
						EditPreference(
								title = stringResource(R.string.homepage),
								value = detail.homepage ?: "",
								onChange = { _, _, value ->
									update(detail.copy(homepage = value))
								},
								icon = { PreferenceIcon(R.drawable.home) },
						)
					}
					item {
						EditPreference(
								title = stringResource(R.string.support_link),
								value = detail.supportURL ?: "",
								onChange = { _, _, value ->
									update(detail.copy(supportURL = value))
								},
								icon = { PreferenceIcon(R.drawable.money) },
						)
					}
				}
			}
		}
	}
	// 匹配/排除规则编辑对话框
	patternEdit?.let { edit ->
		var draft by remember(edit) { mutableStateOf(edit.draft) }
		AlertDialog(
				onDismissRequest = { patternEdit = null },
				title = {
					Text(
							stringResource(
									if (edit.isMatch) R.string.matches else R.string.exclude
							)
					)
				},
				text = {
					OutlinedTextField(
							value = draft,
							onValueChange = { draft = it },
							singleLine = true,
							modifier = Modifier.fillMaxWidth()
					)
				},
				confirmButton = {
					TextButton(
							onClick = {
								if (draft.isNotBlank()) {
									val current = (if (edit.isMatch) detail?.matches
									else detail?.excludes)?.map { "$it" }?.toMutableList()
										?: mutableListOf()
									if (edit.index >= 0 && edit.index < current.size) {
										current[edit.index] = draft
									} else current.add(draft)
									detail?.let {
										update(
												if (edit.isMatch) it.copy(
														matches = JSONArray(
																current
														)
												)
												else it.copy(excludes = JSONArray(current))
										)
									}
								}
								patternEdit = null
							}, shapes = ButtonDefaults.shapes()
					) { Text(stringResource(R.string.confirm)) }
				},
				dismissButton = {
					TextButton(
							onClick = { patternEdit = null }, shapes = ButtonDefaults.shapes()
					) { Text(stringResource(R.string.cancel)) }
				},
		)
	}
}

private const val NEW_SCRIPT_TEMPLATE = """
// ==UserScript==
// @name         New Userscript
// @namespace    Your Namespace
// @version      1.0.0
// @description  try to take over the world!
// @author       You
// @grant        none
// ==/UserScript==

(function() {
    'use strict';

    // Your code here...
})();
"""
