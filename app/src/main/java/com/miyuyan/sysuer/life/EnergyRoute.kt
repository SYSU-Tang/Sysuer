package com.miyuyan.sysuer.life

import android.content.ClipData
import android.content.Intent
import android.view.LayoutInflater
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.haibin.calendarview.CalendarView
import com.haibin.calendarview.SysuerCalendar
import com.miyuyan.preference.ItemPreference
import com.miyuyan.preference.MenuPreference
import com.miyuyan.preference.PreferenceCategory
import com.miyuyan.preference.PreferenceScreen
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.nav.navigateBack
import com.miyuyan.sysuer.view.ActivityPager
import com.miyuyan.sysuer.view.MenuItem
import com.miyuyan.sysuer.view.StatePage
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

/**
 * 水电费（综合能源平台）：单个 ActivityPager 承载仪表盘 / 水费 / 电费 / 账户四页。
 * 页面内容用 compose-preference 渲染；日历为 CalendarView 的 AndroidView 互操作。
 */
@Composable
fun EnergyRoute(
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val viewModel: EnergyViewModel = viewModel()
	val activity = LocalActivity.current
	val snackbarHostState = remember { SnackbarHostState() }
	val pagerState = rememberPagerState(pageCount = { 4 })
	var pagerUserScrollEnabled by remember { mutableStateOf(true) }

	LaunchedEffect(Unit) {
		viewModel.snackbarMessage.collect {
			snackbarHostState.showSnackbar(it)
		}
	}

	LaunchedEffect(Unit) {
		viewModel.retry()
	}

	ActivityPager(
			title = stringResource(R.string.water_electricity_fee),
			snackbar = snackbarHostState,
			pagerState = pagerState,
			userScrollEnabled = pagerUserScrollEnabled,
			navs = listOf(
					MenuItem(stringResource(R.string.dashboard), iconResource = R.drawable.home),
					MenuItem(stringResource(R.string.water_fee), iconResource = R.drawable.water),
					MenuItem(
							stringResource(R.string.electricity_fee),
							iconResource = R.drawable.electricity
					),
					MenuItem(stringResource(R.string.account), iconResource = R.drawable.account),
			),
			isNestedScrollEnabled = false,
			onNavigationClick = { backStack.navigateBack(activity) },
			sharedTransitionScope = sharedTransitionScope,
			animatedVisibilityScope = animatedVisibilityScope,
			sharedKey = "EnergyFee",
	) { page ->
		when (page) {
			0 -> DashboardPage(viewModel)
			1 -> WaterFeePage(viewModel) { pagerUserScrollEnabled = it }
			2 -> ElectricityFeePage(viewModel) { pagerUserScrollEnabled = it }
			else -> AccountPage(viewModel)
		}
	}
}

// ---------------------------------------------------------------------------------------------
// 仪表盘
// ---------------------------------------------------------------------------------------------
@Composable
private fun DashboardPage(viewModel: EnergyViewModel) {
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()
	val electricityStats by viewModel.electricityStats.collectAsStateWithLifecycle()
	val waterStats by viewModel.waterStats.collectAsStateWithLifecycle()

	StatePage(state = uiState, onRetry = { viewModel.retry() }) {
		Column(
				modifier = Modifier
					.fillMaxSize()
					.verticalScroll(rememberScrollState()),
				verticalArrangement = Arrangement.spacedBy(
						dimensionResource(R.dimen.vertical_margin)
				),
		) {
			ElevatedCard(
					modifier = Modifier
						.fillMaxWidth()
						.padding(horizontal = dimensionResource(R.dimen.horizontal_margin))
			) {
				FlowRow(
						modifier = Modifier
							.fillMaxWidth()
							.padding(
									horizontal = dimensionResource(R.dimen.horizontal_padding),
									vertical = dimensionResource(R.dimen.vertical_padding)
							), horizontalArrangement = Arrangement.SpaceEvenly
				) {
					listOf(
							Triple(R.drawable.flash, "上月用电", electricityStats?.lastMonth),
							Triple(R.drawable.flash, "本月用电", electricityStats?.thisMonth),
							Triple(R.drawable.flash, "用电变化", electricityStats?.delta),
							Triple(R.drawable.water, "上月用水", waterStats?.lastMonth),
							Triple(R.drawable.water, "本月用水", waterStats?.thisMonth),
							Triple(R.drawable.water, "用水变化", waterStats?.delta),
					).forEach { (a, b, c) ->
						c?.let {
							UsageButton(
									a,
									b,
									it,
							)
						}
					}
				}
			}
			ElevatedCard(
					modifier = Modifier
						.fillMaxWidth()
						.padding(
								horizontal = dimensionResource(R.dimen.horizontal_margin),
								vertical = dimensionResource(R.dimen.vertical_margin)
						)
			) {
				Column(
						modifier = Modifier
							.fillMaxWidth()
							.padding(
									vertical = dimensionResource(R.dimen.vertical_padding)
							),
						verticalArrangement = Arrangement.spacedBy(
								dimensionResource(R.dimen.vertical_gap)
						),
				) {
					Text(
							text = stringResource(R.string.payment_history),
							style = MaterialTheme.typography.titleLarge,
							color = MaterialTheme.colorScheme.primary,
							modifier = Modifier.padding(
									horizontal = dimensionResource(R.dimen.horizontal_padding)
							),
					)
					viewModel.records.forEach { record ->
						Text(
								text = record.date,
								style = MaterialTheme.typography.titleMedium,
								color = MaterialTheme.colorScheme.primary,
								modifier = Modifier.padding(
										horizontal = dimensionResource(R.dimen.horizontal_padding)
								),
						)
						record.details.forEach { detail ->
							PreferenceCategory(
									title = detail.title, modifier = Modifier.padding(
									horizontal = dimensionResource(R.dimen.horizontal_padding)
							)
							) {
								detail.rows.forEach { row ->
									item {
										ItemPreference(
												title = row.key,
												icon = row.icon,
												summary = row.value
										)
									}
								}
							}
						}
					}
					if (viewModel.records.isEmpty()) {
						Text(
								text = stringResource(R.string.no_data_available),
								style = MaterialTheme.typography.bodyMedium,
								modifier = Modifier.padding(
										horizontal = dimensionResource(R.dimen.horizontal_padding),
										vertical = dimensionResource(R.dimen.vertical_padding)
								),
						)
					}
				}
			}
		}
	}
}

/** 一行三列的用量瓦片（图标在上，对应原 Flow 布局的 6 个 TextButton） */
@Composable
private fun UsageButton(icon: Int, title: String, value: Double) {
	TextButton(
			onClick = {}, shapes = ButtonDefaults.shapes()
	) {
		Column(
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.spacedBy(
						dimensionResource(R.dimen.vertical_gap)
				),
		) {
			Icon(
					painter = painterResource(icon),
					contentDescription = null,
					tint = MaterialTheme.colorScheme.primary,
			)
			Text(
					text = title,
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
			Text(
					text = String.format(
							LocalLocale.current.platformLocale, "%.2f", value
					),
					style = MaterialTheme.typography.titleMedium,
					color = MaterialTheme.colorScheme.primary,
			)
		}
	}
}


// ---------------------------------------------------------------------------------------------
// 水费
// ---------------------------------------------------------------------------------------------
@Composable
private fun WaterFeePage(
	viewModel: EnergyViewModel, onCalendarDrag: (Boolean) -> Unit,
) {
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()
	val schemes by viewModel.waterSchemes.collectAsStateWithLifecycle()
	val roomName by viewModel.waterRoomName.collectAsStateWithLifecycle()
	val monthText by viewModel.waterMonthText.collectAsStateWithLifecycle()
	val detail by viewModel.waterDetail.collectAsStateWithLifecycle()

	StatePage(state = uiState, onRetry = { viewModel.retry() }) {
		val collapse = rememberCollapseState()
		Column {
			FeeCalendar(
					schemes = schemes,
					onMonthChange = { year, month ->
						viewModel.dispatch(EnergyEvent.WaterMonthChange(year, month))
					},
					onDragChange = onCalendarDrag,
					collapsed = collapse.collapsed,
					collapseOffset = collapse.offset,
					onHeightChanged = { if (!collapse.collapsed) collapse.calendarHeight = it },
			)
			LazyColumn(
					modifier = Modifier
						.background(MaterialTheme.colorScheme.surfaceBright)
						.padding(
								horizontal = dimensionResource(R.dimen.horizontal_margin),
								vertical = dimensionResource(R.dimen.vertical_margin)
						)
						.nestedScroll(collapse.connection)
			) {
				item {
					PreferenceCategory(title = monthText) {
						item {
							RoomMenuPreference(
									title = stringResource(R.string.dorm),
									rooms = viewModel.rooms,
									selectedName = roomName,
							) { code ->
								viewModel.dispatch(EnergyEvent.SelectWaterRoom(code))
							}
						}
					}
				}
				items(viewModel.waterBills) { bill ->
					PreferenceCategory(title = bill.duration) {
						bill.rows.forEach { row ->
							item {
								ItemPreference(
										title = row.key, icon = row.icon, summary = row.value
								)
							}
						}
						item {
							ItemPreference(
									title = stringResource(R.string.view_detail),
									icon = R.drawable.view,
									onClick = {
										viewModel.dispatch(EnergyEvent.WaterBillDetail(bill.id))
									},
							)
							if (bill.canPay) {
								FilledTonalButton(
										onClick = {
											viewModel.dispatch(EnergyEvent.PayWaterBill(bill))
										},
										shapes = ButtonDefaults.shapes(),
										modifier = Modifier
											.fillMaxWidth()
											.padding(
													horizontal = dimensionResource(R.dimen.horizontal_padding),
													vertical = dimensionResource(R.dimen.vertical_padding)
											),
								) { Text(stringResource(R.string.pay_fee)) }
							}
						}
					}
				}
			}
		}
	}
	detail?.let { rows ->
		InfoBottomSheet(rows = rows, onDismiss = {
			viewModel.dispatch(EnergyEvent.DismissWaterDetail)
		})
	}
}

// ---------------------------------------------------------------------------------------------
// 电费
// ---------------------------------------------------------------------------------------------
@Composable
private fun ElectricityFeePage(
	viewModel: EnergyViewModel, onCalendarDrag: (Boolean) -> Unit,
) {
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()
	val schemes by viewModel.electricitySchemes.collectAsStateWithLifecycle()
	val roomName by viewModel.electricityRoomName.collectAsStateWithLifecycle()
	val monthText by viewModel.electricityMonthText.collectAsStateWithLifecycle()
	val detail by viewModel.electricityDetail.collectAsStateWithLifecycle()

	StatePage(state = uiState, onRetry = { viewModel.retry() }) {
		val collapse = rememberCollapseState()
		Column {
			FeeCalendar(
					schemes = schemes,
					onMonthChange = { year, month ->
						viewModel.dispatch(EnergyEvent.ElectricityMonthChange(year, month))
					},
					onDragChange = onCalendarDrag,
					collapsed = collapse.collapsed,
					collapseOffset = collapse.offset,
					onHeightChanged = { if (!collapse.collapsed) collapse.calendarHeight = it },
			)
			LazyColumn(
					modifier = Modifier
						.background(MaterialTheme.colorScheme.surfaceBright)
						.nestedScroll(collapse.connection)
						.padding(
								horizontal = dimensionResource(R.dimen.horizontal_padding),
								vertical = dimensionResource(R.dimen.vertical_padding)
						),
			) {
				item {
					PreferenceCategory(title = monthText) {
						item {
							RoomMenuPreference(
									title = stringResource(R.string.dorm),
									rooms = viewModel.rooms,
									selectedName = roomName,
							) { code ->
								viewModel.dispatch(EnergyEvent.SelectElectricityRoom(code))
							}
						}
					}
				}
				items(viewModel.electricityBills) { bill ->
					PreferenceCategory(title = bill.title) {
						bill.rows.forEach { row ->
							item {
								ItemPreference(
										title = row.key, icon = row.icon, summary = row.value
								)
							}
						}
						item {
							ItemPreference(
									title = stringResource(R.string.view_detail),
									icon = R.drawable.view,
									onClick = {
										viewModel.dispatch(
												EnergyEvent.ElectricityBillDetail(bill.id)
										)
									},
							)
							if (bill.canPay) {
								FilledTonalButton(
										onClick = {
											viewModel.dispatch(EnergyEvent.PayElectricityBill(bill))
										},
										shapes = ButtonDefaults.shapes(),
										modifier = Modifier
											.fillMaxWidth()
											.padding(
													vertical = dimensionResource(R.dimen.vertical_padding)
											),
								) { Text(stringResource(R.string.pay_fee)) }
							}
						}

					}
				}
			}
		}
	}
	detail?.let { rows ->
		InfoBottomSheet(rows = rows, onDismiss = {
			viewModel.dispatch(EnergyEvent.DismissElectricityDetail)
		})
	}
}

// ---------------------------------------------------------------------------------------------
// 账户
// ---------------------------------------------------------------------------------------------
@Composable
private fun AccountPage(viewModel: EnergyViewModel) {
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()
	val userRows by viewModel.userRows.collectAsStateWithLifecycle()
	val dormRows by viewModel.dormRows.collectAsStateWithLifecycle()
	val balance by viewModel.balance.collectAsStateWithLifecycle()
	val showRecharge by viewModel.showRecharge.collectAsStateWithLifecycle()
	val context = LocalContext.current
	val clipboard = LocalClipboard.current

	val share = stringResource(R.string.share)
	val recharge = stringResource(R.string.recharge)
	LaunchedEffect(Unit) {
		viewModel.wechatLink.collect { location ->
			clipboard.setClipEntry(ClipData.newPlainText("recharge", location).toClipEntry())
			context.startActivity(
					Intent.createChooser(
							Intent(Intent.ACTION_SEND).setType("text/plain")
								.putExtra(Intent.EXTRA_TEXT, location).putExtra(
										Intent.EXTRA_SUBJECT, recharge
								).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), share
					)
			)
		}
	}

	StatePage(state = uiState, onRetry = { viewModel.retry() }) {
		PreferenceScreen {
			PreferenceCategory(title = stringResource(R.string.account)) {
				items(userRows) { row ->
					ItemPreference(title = row.key, icon = row.icon, summary = row.value)
				}
			}
			if (dormRows.isNotEmpty()) {
				PreferenceCategory(title = stringResource(R.string.dorm)) {
					items(dormRows) { row ->
						ItemPreference(title = row.key, icon = row.icon, summary = row.value)
					}
				}
			}
			PreferenceCategory(title = stringResource(R.string.balance)) {
				balance?.let {
					item {
						ItemPreference(
								title = stringResource(R.string.balance),
								icon = R.drawable.money,
								summary = it,
						)
						FilledTonalButton(
								onClick = { viewModel.showRecharge() },
								shapes = ButtonDefaults.shapes(),
								modifier = Modifier
									.fillMaxWidth()
									.padding(
											vertical = dimensionResource(R.dimen.vertical_padding)
									),
						) { Text(stringResource(R.string.pay_fee)) }
					}
				}
			}
		}
	}
	if (showRecharge) {
		RechargeBottomSheet(
				onDismiss = { viewModel.dismissRecharge() },
				onSubmit = { amountFen, remark ->
					viewModel.dispatch(EnergyEvent.Recharge(amountFen, remark))
					viewModel.dismissRecharge()
				},
		)
	}
}

/** 房间选择：compose-preference 的菜单偏好项 */
@Composable
private fun RoomMenuPreference(
	title: String,
	rooms: List<Pair<String, String?>>,
	selectedName: String?,
	onSelect: (String?) -> Unit,
) {
	MenuPreference(
			title = title,
			required = true,
			entries = rooms.map { it.first },
			entryValues = rooms.map { it.second },
			initialIndex = rooms.indexOfFirst { it.first == selectedName }.takeIf { it >= 0 },
			onChange = { _, _, value -> onSelect(value) },
	)
}

/** 账单详情底部弹层 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun InfoBottomSheet(rows: List<InfoRow>, onDismiss: () -> Unit) {
	ModalBottomSheet(onDismissRequest = onDismiss) {
		PreferenceScreen {
			PreferenceCategory {
				items(rows) { row ->
					ItemPreference(title = row.key, icon = row.icon, summary = row.value)
				}
			}
		}
	}
}

/** 房间钱包充值弹层 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RechargeBottomSheet(onDismiss: () -> Unit, onSubmit: (Int, String) -> Unit) {
	var amount by rememberSaveable(stateSaver = TextFieldValue.Saver) {
		mutableStateOf(TextFieldValue(""))
	}
	var remark by rememberSaveable { mutableStateOf("") }
	ModalBottomSheet(onDismissRequest = onDismiss) {
		Column(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = dimensionResource(R.dimen.horizontal_padding)),
				verticalArrangement = Arrangement.spacedBy(
						dimensionResource(R.dimen.vertical_margin)
				),
		) {
			OutlinedTextField(
					value = amount,
					onValueChange = { amount = it },
					label = { Text(stringResource(R.string.fee)) },
					singleLine = true,
					keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
					modifier = Modifier.fillMaxWidth(),
			)
			OutlinedTextField(
					value = remark,
					onValueChange = { remark = it },
					label = { Text(stringResource(R.string.remark)) },
					singleLine = true,
					modifier = Modifier.fillMaxWidth(),
			)
			FilledTonalButton(
					onClick = {
						val amountFen = amount.text.toDoubleOrNull()?.times(100)?.toInt() ?: 0
						onSubmit(amountFen, remark)
					},
					enabled = (amount.text.toDoubleOrNull() ?: 0.0) > 0,
					shapes = ButtonDefaults.shapes(),
					modifier = Modifier
						.fillMaxWidth()
						.padding(bottom = dimensionResource(R.dimen.content_padding)),
			) { Text(stringResource(R.string.submit)) }
		}
	}
}

/** CalendarView 的 AndroidView 互操作：显示每日用量标记，翻月回调由 VM 处理 */
@Composable
private fun FeeCalendar(
	schemes: Map<String, SysuerCalendar>,
	onMonthChange: (Int, Int) -> Unit,
	onDragChange: (Boolean) -> Unit = {},
	collapsed: Boolean = false,
	collapseOffset: Float = 0f,
	onHeightChanged: (Int) -> Unit = {},
) {
	val currentOnMonthChange by rememberUpdatedState(onMonthChange)
	var calendarView by remember { mutableStateOf<CalendarView?>(null) }
	// 手指按在日历上时禁用 ActivityPager 的横向翻页，抬起后恢复，
	// 解决嵌套横向滚动时外层 HorizontalPager 抢走月份滑动手势的问题
	Box(
			modifier = Modifier
				.fillMaxWidth()
				// 折叠：布局高度随位移递减并裁剪底部（interop 的 AndroidView
				// 永远绘制在兄弟 Compose 内容之上，不能用列表覆盖的方式）
				.layout { measurable, constraints ->
					val placeable = measurable.measure(constraints)
					val visible = (placeable.height - collapseOffset.roundToInt()).coerceAtLeast(0)
					layout(placeable.width, visible) { placeable.place(0, 0) }
				}
				.clipToBounds()
				.pointerInput(Unit) {
					awaitEachGesture {
						awaitFirstDown(requireUnconsumed = false)
						onDragChange(false)
						waitForUpOrCancellation()
						onDragChange(true)
					}
				},
	) {
		AndroidView(
				modifier = Modifier
					.fillMaxWidth()
					.onSizeChanged { onHeightChanged(it.height) },
				factory = { context ->
					(LayoutInflater.from(context)
						.inflate(R.layout.view_fee_calendar, null) as CalendarView).apply {
						// 监听器只设置一次；翻月回调经 rememberUpdatedState 取最新值
						setOnMonthChangeListener { year, month ->
							currentOnMonthChange(year, month)
						}
					}.also { calendarView = it }
				},
		)
		// 仅在标记数据变化时同步，避免重组期间重置 ViewPager 打断滑动手势
		LaunchedEffect(calendarView, schemes) {
			calendarView?.setSchemeDate(ConcurrentHashMap(schemes))
		}
		// 列表滚动驱动：折叠为周视图 / 展开为月视图
		LaunchedEffect(calendarView, collapsed) {
			if (collapsed) calendarView?.showWeekView() else calendarView?.showMonthView()
		}
	}
}

/** 折叠状态：账单列表上滑时逐步覆盖月历，覆盖完成后切换为周视图，顶部下拉反向展开 */
private class CollapseState(private val density: Density) {
	val scrollState = ScrollState(0)

	var collapsed by mutableStateOf(false)
	var offset by androidx.compose.runtime.mutableFloatStateOf(0f)
	var calendarHeight by mutableIntStateOf(0)

	/** 可折叠位移 = 月历高度 - 周视图高度（周视图 = 星期栏 + 一行日期） */
	private val maxOffset: Float
		get() = with(density) {
			((calendarHeight - 36.dp.toPx()).coerceAtLeast(0f)) * 5f / 6f
		}

	val connection = object : NestedScrollConnection {
		override fun onPreScroll(
			available: Offset, source: NestedScrollSource
		): Offset {
			if (collapsed || available.y >= 0f) return Offset.Zero
			if (offset >= maxOffset) {
				collapsed = true
				return Offset.Zero
			}
			val consume = (-available.y).coerceAtMost(maxOffset - offset)
			offset += consume
			if (offset >= maxOffset) collapsed = true
			return Offset(0f, -consume)
		}

		override fun onPostScroll(
			consumed: Offset, available: Offset, source: NestedScrollSource
		): Offset {
			if (available.y <= 0f) return Offset.Zero
			// 顶部下拉展开：先切回月视图并用位移抵消布局跳变，再随下拉逐步收回覆盖
			if (collapsed) {
				collapsed = false
				offset = maxOffset
			}
			if (offset > 0f) {
				val back = available.y.coerceAtMost(offset)
				offset -= back
				return Offset(0f, back)
			}
			return Offset.Zero
		}
	}
}

@Composable
private fun rememberCollapseState(density: Density = LocalDensity.current): CollapseState =
	remember { CollapseState(density) }
