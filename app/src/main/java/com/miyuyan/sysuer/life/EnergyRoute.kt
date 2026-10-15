package com.miyuyan.sysuer.life

import android.content.ClipData
import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ColorScheme
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.haibin.calendarview.compose.CalendarColors
import com.haibin.calendarview.compose.CalendarDay
import com.haibin.calendarview.compose.CalendarDayScheme
import com.haibin.calendarview.compose.CalendarSelectMode
import com.haibin.calendarview.compose.CalendarWeekStart
import com.haibin.calendarview.compose.ComposeCalendar
import com.haibin.calendarview.compose.ComposeCalendarConfig
import com.haibin.calendarview.compose.CalendarMonthShowMode
import com.haibin.calendarview.compose.rememberComposeCalendarState
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

/**
 * 水电费（综合能源平台）：单个 ActivityPager 承载仪表盘 / 水费 / 电费 / 账户四页。
 * 页面内容用 compose-preference 渲染；日历为 ComposeCalendar。
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
			1 -> WaterFeePage(viewModel)
			2 -> ElectricityFeePage(viewModel)
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
	viewModel: EnergyViewModel,
) {
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()
	val schemes by viewModel.waterSchemes.collectAsStateWithLifecycle()
	val roomName by viewModel.waterRoomName.collectAsStateWithLifecycle()
	val monthText by viewModel.waterMonthText.collectAsStateWithLifecycle()
	val detail by viewModel.waterDetail.collectAsStateWithLifecycle()

	StatePage(state = uiState, onRetry = { viewModel.retry() }) {
		FeeCalendar(
				schemes = schemes,
				onMonthChange = { year, month ->
					viewModel.dispatch(EnergyEvent.WaterMonthChange(year, month))
				},
		) {
			LazyColumn(
					modifier = Modifier
						.background(MaterialTheme.colorScheme.surfaceBright)
						.padding(
								horizontal = dimensionResource(R.dimen.horizontal_margin),
								vertical = dimensionResource(R.dimen.vertical_margin)
						)
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
											.fillMaxWidth(),
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
	viewModel: EnergyViewModel,
) {
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()
	val schemes by viewModel.electricitySchemes.collectAsStateWithLifecycle()
	val roomName by viewModel.electricityRoomName.collectAsStateWithLifecycle()
	val monthText by viewModel.electricityMonthText.collectAsStateWithLifecycle()
	val detail by viewModel.electricityDetail.collectAsStateWithLifecycle()

	StatePage(state = uiState, onRetry = { viewModel.retry() }) {
		FeeCalendar(
				schemes = schemes,
				onMonthChange = { year, month ->
					viewModel.dispatch(EnergyEvent.ElectricityMonthChange(year, month))
				},
		) {
			LazyColumn(
					modifier = Modifier
						.background(MaterialTheme.colorScheme.surfaceBright)
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

/**
 * 用量日历：ComposeCalendar 承载每日用量标记，翻月回调交给 ViewModel。
 *
 * 折叠/展开由 [ComposeCalendar] 自带的嵌套滚动处理，无需外部维护折叠状态；
 * [content] 是日历下方的账单列表，上滑收起为周视图、顶部下拉重新展开。
 *
 * @param schemes 日期到用量标记的映射，键为旧版 [SysuerCalendar.toString] 的 yyyyMMdd
 * @param onMonthChange 翻月回调，参数为年、月
 * @param content 日历下方的账单列表
 */
@Composable
private fun FeeCalendar(
	schemes: Map<String, SysuerCalendar>,
	onMonthChange: (Int, Int) -> Unit,
	content: @Composable () -> Unit,
) {
	val colorScheme = MaterialTheme.colorScheme
	val config = remember(colorScheme) {
		ComposeCalendarConfig(
				// 与旧版 view_fee_calendar.xml 的 CalendarView 属性保持一致
				weekStart = CalendarWeekStart.Monday,
				monthShowMode = CalendarMonthShowMode.OnlyCurrentMonth,
				selectMode = CalendarSelectMode.Single,
				minYear = 2008,
				maxYear = 2050,
				itemHeight = 56.dp,
				weekBarHeight = 36.dp,
				calendarPaddingLeft = 4.dp,
				calendarPaddingRight = 4.dp,
				dayTextSize = 16.sp,
				lunarTextSize = 10.sp,
				weekBarTextSize = 12.sp,
				colors = feeCalendarColors(colorScheme),
		)
	}
	val state = rememberComposeCalendarState(config = config, initialDate = CalendarDay.today())
	// 用量标记同步到日历状态；schemeColor 为 0 时回退到旧版 scheme_theme_color
	val schemeDates = remember(schemes) {
		schemes.values.mapNotNull { calendar ->
			calendar.scheme.takeIf { it.isNotBlank() }?.let {
				CalendarDay(calendar.year, calendar.month, calendar.day) to
					CalendarDayScheme(
							color = if (calendar.schemeColor != 0) Color(calendar.schemeColor)
							else Color(0xFF128C4B),
							text = it,
					)
			}
		}.toMap()
	}

	val currentOnMonthChange by rememberUpdatedState(onMonthChange)
	SideEffect {
		state.schemeDates = schemeDates
		state.onMonthChange = { year, month -> currentOnMonthChange(year, month) }
	}

	// 横向手势交给嵌套滚动自行竞争：手势落在日历上时内层月视图分页器优先消费，
	// 落在下方账单列表上时外层 ActivityPager 翻页，无需手动禁用外层滚动
	ComposeCalendar(
			state = state,
			config = config,
			modifier = Modifier.fillMaxWidth(),
			content = content,
	)
}

/** 由 Material3 配色派生用量日历配色，对齐旧版自定义月/周视图的取色 */
private fun feeCalendarColors(colorScheme: ColorScheme): CalendarColors =
	CalendarColors.default().copy(
			background = colorScheme.surfaceBright,
			weekBarBackground = colorScheme.surface,
			weekBarTextColor = colorScheme.onSurface,
			dayTextColor = colorScheme.onSurface,
			otherMonthDayTextColor = colorScheme.onSurface,
			lunarTextColor = colorScheme.onSurface,
			otherMonthLunarTextColor = colorScheme.onSurface,
			currentDayTextColor = Color(0xFFFF0000),
			currentDayLunarTextColor = Color(0xFFFF0000),
			selectedBackgroundColor = colorScheme.primary,
			selectedTextColor = colorScheme.onPrimary,
			selectedLunarTextColor = colorScheme.onPrimary,
			schemeTextColor = colorScheme.onSurface,
			schemeLunarTextColor = colorScheme.onSurface,
			schemeDefaultColor = Color(0xFF128C4B),
	)

