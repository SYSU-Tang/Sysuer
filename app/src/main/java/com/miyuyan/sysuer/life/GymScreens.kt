package com.miyuyan.sysuer.life

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.Icon
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.alibaba.fastjson2.JSONObject
import com.haibin.calendarview.compose.CalendarColors
import com.haibin.calendarview.compose.CalendarDay
import com.haibin.calendarview.compose.CalendarDayState
import com.haibin.calendarview.compose.CalendarSelectMode
import com.haibin.calendarview.compose.CalendarViewMode
import com.haibin.calendarview.compose.ComposeCalendarConfig
import com.haibin.calendarview.compose.ComposeCalendarWeekBar
import com.haibin.calendarview.compose.ComposeCalendarWeekPager
import com.haibin.calendarview.compose.rememberComposeCalendarState
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.CommonUtil.extractValue
import com.miyuyan.sysuer.api.DateTimeManager
import com.miyuyan.sysuer.view.MenuItem
import com.miyuyan.sysuer.view.RowData
import com.miyuyan.sysuer.view.SectionData
import com.miyuyan.sysuer.view.StaggerScreen
import com.miyuyan.sysuer.view.StatePage
import com.miyuyan.sysuer.view.UiState
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// ================================================================================================
// 场地：校区 → 场馆 → 时段详情（下钻，不下沉为导航节点）
// ================================================================================================

@Composable
fun GymFieldTab(
	viewModel: GymViewModel,
	level: Int,
	campusId: String,
	venueId: String,
	onCampusClick: (String, String) -> Unit,
	onVenueClick: (String, String) -> Unit,
) {
	val fieldState by viewModel.fieldState.collectAsStateWithLifecycle()

	LaunchedEffect(level) {
		when (level) {
			0 -> viewModel.loadCampus()
			1 -> viewModel.loadVenues()
		}
	}

	if (level == 2) {
		GymDetailContent(viewModel = viewModel, venueTypeId = venueId)
		return
	}

	// 场馆列表按所选校区过滤，校区列表原样展示
	val items = remember(level, campusId, viewModel.fields.size) {
		if (level == 0) viewModel.fields.toList()
		else viewModel.fields.filter { it.getString("Campus") == campusId }
	}

	StatePage(
			state = if (level == 1 && items.isEmpty() && fieldState == UiState.Content) {
				UiState.Empty
			} else {
				fieldState
			},
			onRetry = { if (level == 0) viewModel.loadCampus() else viewModel.loadVenues() },
	) {
		LazyVerticalStaggeredGrid(
				columns = StaggeredGridCells.Adaptive(240.dp),
				modifier = Modifier.fillMaxSize(),
				contentPadding = PaddingValues(vertical = dimensionResource(R.dimen.vertical_margin), horizontal = dimensionResource(R.dimen.horizontal_margin)),
				verticalItemSpacing = dimensionResource(R.dimen.vertical_margin),
				horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_gap)),
		) {
			items(items) { item ->
				GymFieldCard(
						item = item,
						cookie = viewModel.cookie,
						authorization = viewModel.authorization,
				) {
					val id = item.getString("Identity") ?: ""
					val name = item.getString("Name") ?: ""
					if (level == 0) onCampusClick(id, name) else onVenueClick(id, name)
				}
			}
		}
	}
}

/** 场地卡片：图片 + 标题，对应 item_field.xml */
@Composable
private fun GymFieldCard(
	item: JSONObject,
	cookie: String,
	authorization: String,
	onClick: () -> Unit,
) {
	val context = LocalContext.current
	ElevatedCard(
			onClick = onClick,
			modifier = Modifier.fillMaxWidth(),
			/*
							.padding(dimensionResource(R.dimen.vertical_padding))*/
	) {
		// 图片接口需要登录态，故带上 Cookie / Authorization
		item.getString("ImageUrl")?.takeIf { it.isNotEmpty() }?.let { image ->
			AsyncImage(
					model = ImageRequest.Builder(context).data(image).addHeader("Cookie", cookie)
						.addHeader("Authorization", authorization).addHeader(
								"User-Agent",
								"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " + "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
						).addHeader(
								"Accept",
								"text/html,application/xhtml+xml,application/xml;q=0.9,image/avif," + "image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"
						).crossfade(true).build(),
					contentDescription = item.getString("Name"),
					contentScale = ContentScale.FillWidth,
					modifier = Modifier.fillMaxWidth(),
					/*.aspectRatio(1.6f)*/
			)
		}
		Text(
				text = item.getString("Name") ?: "",
				style = MaterialTheme.typography.titleMedium,
				modifier = Modifier.padding(
						vertical = dimensionResource(R.dimen.vertical_margin),
						horizontal = dimensionResource(R.dimen.horizontal_margin)
				),
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
		)
	}
}

// ================================================================================================
// 时段详情：周视图日历 + 时段网格 + 下单
// ================================================================================================

@Composable
private fun GymDetailContent(viewModel: GymViewModel, venueTypeId: String) {
	val position by viewModel.position.collectAsStateWithLifecycle()
	val detailState by viewModel.detailState.collectAsStateWithLifecycle()
	val columnCount by viewModel.columnCount.collectAsStateWithLifecycle()

	LaunchedEffect(venueTypeId) { viewModel.loadSlots(venueTypeId, 0) }

	// 周视图日历：横向翻周选日期，替代原「LazyRow 日期卡」方案；
	// minDate 限定为今天，过去的日期自然不可选
	val calendarState = rememberComposeCalendarState(
			config = ComposeCalendarConfig(
					selectMode = CalendarSelectMode.Single,
					showLunar = false,
					showSchemeText = false,
					itemHeight = 56.dp,
					defaultExpanded = false,
					minDate = CalendarDay.today(),
					colors = CalendarColors.default().copy(
							background = Color.Transparent,
							weekBarBackground = Color.Transparent,
					),
			),
			initialMode = CalendarViewMode.WEEK,
	)
	with(calendarState) {
		onDaySelected = { day, _ ->
			// 日历日期 -> 相对今天的天数下标（原 DateAdapter 的 position 语义）
			val offset =
				ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.of(day.year, day.month, day.day))
			viewModel.loadSlots(venueTypeId, offset.toInt())
		}
	}

	Column(modifier = Modifier.fillMaxSize()) {
		ComposeCalendarWeekBar(
				state = calendarState,
				modifier = Modifier.fillMaxWidth(),
		)
		ComposeCalendarWeekPager(
				state = calendarState,
				// HorizontalPager 默认占满剩余高度，需显式限定为单行日期的高度
				modifier = Modifier
					.fillMaxWidth()
					.height(calendarState.config.itemHeight),
				config = calendarState.config.copy(dayContent = { day, dayState ->
					GymCalendarDayCell(day, dayState, viewModel.availableCapacity)
				}),
		)

		// 网格 + 提交行整体可纵向滚动；overlay 模式让状态视图叠加在内容上，
		// 不会造成布局坍塌，加载态下格子仍可测量
		StatePage(
				state = detailState,
				onRetry = { viewModel.loadSlots(venueTypeId, position) },
				overlay = true,
				modifier = Modifier.weight(1f),
		) {
			Column(
					modifier = Modifier
						.fillMaxSize()
						.verticalScroll(rememberScrollState()),
			) {
				// 按行渲染：首行为时段表头，其余行首格为场地名；
				// 行内格子等宽，避免按列堆叠时因换行高度不一导致错位
				val rows = remember(columnCount, detailState, viewModel.slots.size) {
					if (columnCount <= 0) emptyList() else viewModel.slots.chunked(columnCount)
				}
				if (rows.isNotEmpty()) {
					Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
						rows.forEachIndexed { rowIndex, row ->
							Row {
								row.forEachIndexed { columnIndex, cell ->
									val index = rowIndex * columnCount + columnIndex
									GymSlotCell(
											cell = cell,
											selected = viewModel.selected.contains(index),
									) { viewModel.toggleSelection(index) }
								}
							}
						}
					}
				}
			}
		}
	}
}

/**
 * 日历日期格：日期数字下方叠加当日可约数量（取自 [GymViewModel.availableCapacity]），
 * 供 ComposeCalendarWeekPager 的 dayContent 插槽使用
 */
@Composable
private fun GymCalendarDayCell(
	day: CalendarDay,
	state: CalendarDayState,
	availableCapacity: Map<Int, Int>,
) {
	val date = LocalDate.of(day.year, day.month, day.day)
	val index = ChronoUnit.DAYS.between(LocalDate.now(), date).toInt()
	val capacity = availableCapacity[index]
	Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
		// 选中背景：淡色圆底，替代默认实心圆，避免遮住容量文字
		if (state.isSelected) {
			Box(
					modifier = Modifier
						.size(44.dp)
						.clip(CircleShape)
						.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
			)
		}
		Column(horizontalAlignment = Alignment.CenterHorizontally) {
			Text(
					text = day.day.toString(),
					style = MaterialTheme.typography.titleMedium,
					color = when {
						state.isSelected || state.isCurrentDay -> MaterialTheme.colorScheme.primary
						state.isDisabled -> MaterialTheme.colorScheme.outline
						else -> MaterialTheme.colorScheme.onSurface
					},
			)
			Text(
					text = if (capacity != null && capacity >= 0) capacity.toString() else "",
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
		}
	}
}

/** 时段单元格：按 Type 区分表头 / 场地名 / 可预约格 / 占位，对应 item_field_detail.xml */
@Composable
private fun GymSlotCell(cell: JSONObject, selected: Boolean, onClick: () -> Unit) {
	val type = cell.getInteger("Type")
	val capacity = cell.getInteger("AvailableCapacity")
	val selectable = type == 1 && capacity != null && capacity > 0
	val text = when (type) {
		0 -> cell.getString("VenueName") ?: ""
		1 -> if (selectable) "${stringResource(R.string.reservable)}/$capacity"
		else stringResource(R.string.reserved)

		2 -> cell.getString("Name") ?: ""
		else -> ""
	}
	// 全部格子统一宽度，行式渲染下各行、各列才能严格对齐
	val modifier = Modifier
		.width(104.dp)
		.heightIn(min = 56.dp)
		.padding(4.dp)
		// 已约满的格子压暗，与原实现的 alpha = 0.5f 一致
		.alpha(if (type == 1 && !selectable) 0.5f else 1f)
	val shape = RoundedCornerShape(dimensionResource(R.dimen.content_padding))
	val contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
	val label: @Composable () -> Unit = {
		Column(horizontalAlignment = Alignment.CenterHorizontally) {
			// 选中打勾：图标叠在文字上方，格子尺寸不随选中变化
			if (selected && type == 1) {
				Icon(
						Icons.Rounded.Check,
						contentDescription = null,
						modifier = Modifier
							.size(16.dp)
							.padding(bottom = 2.dp),
				)
			}
			Text(
					text = text,
					textAlign = TextAlign.Center,
					style = MaterialTheme.typography.bodyMedium,
					maxLines = 3,
					overflow = TextOverflow.Ellipsis,
			)
		}
	}

	if (type == 1 && selectable) {
		// 可预约格：未选中为描边，选中变为填充并打勾，即 Outline -> Fill 的 check 效果
		if (selected) {
			FilledTonalButton(
					onClick = onClick,
					shape = shape,
					contentPadding = contentPadding,
					colors = ButtonDefaults.filledTonalButtonColors(
							containerColor = MaterialTheme.colorScheme.secondaryContainer,
							contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
					),
					modifier = modifier,
			) { label() }
		} else {
			OutlinedButton(
					onClick = onClick,
					shape = shape,
					contentPadding = contentPadding,
					modifier = modifier,
			) { label() }
		}
	} else {
		FilledTonalButton(
				onClick = onClick,
				enabled = false,
				shape = shape,
				contentPadding = contentPadding,
				modifier = modifier,
		) { label() }
	}
}

// ================================================================================================
// 我的预约
// ================================================================================================

@Composable
fun GymReservationTab(viewModel: GymViewModel) {
	val context = LocalContext.current
	val state by viewModel.reservationState.collectAsStateWithLifecycle()
	val fromTo by viewModel.reservationFromTo.collectAsStateWithLifecycle()

	LaunchedEffect(fromTo) {
		viewModel.fetchReservations(fromTo.first, fromTo.second)
	}

	val cancelLabel = stringResource(R.string.cancel_reservation)
	val sections = remember(state, cancelLabel) {
		mutableStateListOf<SectionData>().apply {
			viewModel.reservations.forEachIndexed { index, item ->
				add(
						SectionData(
								title = item.getString("Description"),
						rows = extractValue(
								context, item, intArrayOf(
								R.string.venue,
								R.string.start_time,
								R.string.end_time,
								R.string.money,
								R.string.order_time,
						), arrayOf(
								"VenueName",
								"StartDateTime",
								"EndDateTime",
								"Charge",
								"CreatedAt"
						)
						),
						footerMenus = mutableStateListOf(
								MenuItem(cancelLabel) {
									viewModel.deleteReservation(item.getString("Identity"))
								}),
						key = item.getString("Identity") ?: index.toString(),
				))
			}
		}
	}

	StatePage(
			state = state,
			onRetry = { viewModel.fetchReservations(fromTo.first, fromTo.second) },
	) {
		StaggerScreen(sections = sections)
	}
}

// ================================================================================================
// 消费流水
// ================================================================================================

@Composable
fun GymOrderTab(viewModel: GymViewModel) {
	val context = LocalContext.current
	val state by viewModel.orderState.collectAsStateWithLifecycle()
	val fromTo by viewModel.orderFromTo.collectAsStateWithLifecycle()

	LaunchedEffect(Unit) {
		viewModel.fetchOrders(fromTo.first, fromTo.second)
	}

	val account = stringResource(R.string.account)
	val sections = remember(state, account) {
		mutableStateListOf<SectionData>().apply {
			viewModel.orders.forEachIndexed { index, item ->
				add(
						SectionData(
								title = account,
								rows = extractValue(
										context, item, intArrayOf(
										R.string.date,
										R.string.type,
										R.string.money,
										R.string.balance,
								), arrayOf("Date", "TransactionType", "Amount", "Balance")
								),
								key = "${item.getString("Date")}-$index",
						)
				)
			}
		}
	}

	StatePage(
			state = state,
			onRetry = { viewModel.fetchOrders(fromTo.first, fromTo.second) },
	) {
		StaggerScreen(
				sections = sections,
				onScrollBottom = {
					if (viewModel.hasMoreOrder) {
						viewModel.fetchOrders(fromTo.first, fromTo.second)
					}
				},
		)
	}
}

// ================================================================================================
// 我的账户
// ================================================================================================

@Composable
fun GymAccountTab(viewModel: GymViewModel) {
	val context = LocalContext.current
	val state by viewModel.accountState.collectAsStateWithLifecycle()
	val account by viewModel.account.collectAsStateWithLifecycle()
	val certificates = viewModel.certificates

	LaunchedEffect(Unit) { viewModel.loadAccount() }

	val accountTitle = stringResource(R.string.account)
	val walletTitle = stringResource(R.string.wallet)
	val healthTitle = stringResource(R.string.health_proof)
	val swimmerLabel = stringResource(R.string.is_swimmer_valid)
	val adminLabel = stringResource(R.string.admin)
	val yes = stringResource(R.string.yes)
	val no = stringResource(R.string.no)
	val approved = stringResource(R.string.approved)
	val disapproved = stringResource(R.string.disapproved)
	val statusLabel = stringResource(R.string.status)
	val validDateLabel = stringResource(R.string.valid_date)
	val examDateLabel = stringResource(R.string.physical_exam_date)

	val sections = remember(
			state,
			account,
			certificates.size,
			accountTitle,
			walletTitle,
			healthTitle,
			statusLabel,
			validDateLabel,
			examDateLabel,
			approved,
			disapproved
	) {
		mutableStateListOf<SectionData>().apply {
			account?.let { data ->
				add(
						SectionData(
								title = accountTitle,
								rows = extractValue(
										context, data, intArrayOf(
										R.string.type,
										R.string.name,
										R.string.student_id,
										R.string.net_id,
								), arrayOf("Type", "Name", "HostKey", "UserId")
								),
								key = "account",
						)
				)
				add(
						SectionData(
								title = walletTitle,
								rows = extractValue(
										context, data,
										intArrayOf(R.string.sport_credit, R.string.wallet),
										arrayOf("Credits", "CashWallet"),
								),
								key = "wallet",
						)
				)
				add(
						SectionData(
								title = accountTitle,
								rows = mutableStateListOf(
										RowData(
												swimmerLabel,
												if (data.getBooleanValue("validSwimmer")) yes else no
										),
										RowData(
												adminLabel,
												if (data.getBooleanValue("IsAdmin")) yes else no
										),
								),
								key = "other",
						)
				)
			}
			certificates.forEachIndexed { index, item ->
				// 接口返回 approved/disapproved，展示为本地化文案
				val status = item.getString("Status")
				val rows = mutableStateListOf(
						RowData(statusLabel, if (status == "approved") approved else disapproved),
						RowData(validDateLabel, item.getString("ValidUntil")),
						RowData(examDateLabel, item.getString("PhysicalExamDate")),
				)
				add(SectionData(title = healthTitle, rows = rows, key = "cert-$index"))
			}
		}
	}

	StatePage(state = state, onRetry = { viewModel.loadAccount() }) {
		StaggerScreen(sections = sections)
	}
}

// ================================================================================================
// 公共：顶部区间选择
// ================================================================================================

/** 起止日期选择条，对应 fragment_gym_order.xml 里的 from / ~ / to */
@Composable
fun GymDateRange(from: Long, to: Long, onChange: (Long, Long) -> Unit) {
	var pickingFrom by remember { mutableStateOf(false) }
	var pickingTo by remember { mutableStateOf(false) }
	val between = stringResource(R.string.between)
	val confirm = stringResource(R.string.confirm)
	val cancel = stringResource(R.string.cancel)

	Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.Center,
			modifier = Modifier
				.fillMaxWidth()
				.padding(
						dimensionResource(R.dimen.horizontal_margin),
						dimensionResource(R.dimen.vertical_margin),
				),
	) {
		OutlinedButton(onClick = { pickingFrom = true }, shape = ButtonDefaults.shape) {
			Text(DateTimeManager.toDateString(from))
		}
		Text(
				text = between,
				modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.horizontal_margin)),
		)
		OutlinedButton(onClick = { pickingTo = true }, shape = ButtonDefaults.shape) {
			Text(DateTimeManager.toDateString(to))
		}
	}

	if (pickingFrom) {
		val picker = rememberDatePickerState(initialSelectedDateMillis = from)
		DatePickerDialog(
				onDismissRequest = { pickingFrom = false },
				confirmButton = {
					TextButton(onClick = {
						picker.selectedDateMillis?.let { onChange(it, to) }
						pickingFrom = false
					}, shape = ButtonDefaults.shape) { Text(confirm) }
				},
				dismissButton = {
					TextButton(onClick = { pickingFrom = false }, shape = ButtonDefaults.shape) {
						Text(cancel)
					}
				},
		) { DatePicker(state = picker) }
	}

	if (pickingTo) {
		val picker = rememberDatePickerState(initialSelectedDateMillis = to)
		DatePickerDialog(
				onDismissRequest = { pickingTo = false },
				confirmButton = {
					TextButton(onClick = {
						picker.selectedDateMillis?.let { onChange(from, it) }
						pickingTo = false
					}, shape = ButtonDefaults.shape) { Text(confirm) }
				},
				dismissButton = {
					TextButton(onClick = { pickingTo = false }, shape = ButtonDefaults.shape) {
						Text(cancel)
					}
				},
		) { DatePicker(state = picker) }
	}
}
