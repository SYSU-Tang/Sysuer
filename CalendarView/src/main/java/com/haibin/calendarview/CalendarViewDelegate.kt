/*
 * Copyright (C) 2016 huanghaibin_dev <huanghaibin_dev@163.com>
 * WebSite https://github.com/MiracleTimes-Dev
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.haibin.calendarview

import android.content.Context
import android.graphics.Color
import android.text.TextUtils
import android.util.AttributeSet
import androidx.core.content.withStyledAttributes
import com.haibin.calendarview.CalendarUtil.dipToPx
import com.haibin.calendarview.CalendarUtil.getMonthDaysCount
import com.haibin.calendarview.CalendarView.OnCalendarInterceptListener
import com.haibin.calendarview.CalendarView.OnCalendarLongClickListener
import com.haibin.calendarview.CalendarView.OnCalendarMultiSelectListener
import com.haibin.calendarview.CalendarView.OnCalendarRangeSelectListener
import com.haibin.calendarview.CalendarView.OnCalendarSelectListener
import com.haibin.calendarview.CalendarView.OnClickCalendarPaddingListener
import com.haibin.calendarview.CalendarView.OnInnerDateSelectedListener
import com.haibin.calendarview.CalendarView.OnMonthChangeListener
import com.haibin.calendarview.CalendarView.OnViewChangeListener
import com.haibin.calendarview.CalendarView.OnWeekChangeListener
import com.haibin.calendarview.CalendarView.OnYearChangeListener
import com.haibin.calendarview.CalendarView.OnYearViewChangeListener
import com.haibin.calendarview.LunarCalendar.init
import com.haibin.calendarview.LunarCalendar.setupLunarCalendar
import java.util.Calendar
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

/**
 * Google规范化的属性委托,
 * 代码量多，但是不影响阅读性
 */
class CalendarViewDelegate internal constructor(context: Context, attrs: AttributeSet?) {
	/**
	 * 年月视图是否打开
	 */

	var isShowYearSelectedLayout: Boolean = false

	/**
	 * 当前月份和周视图的item位置
	 */

	var mCurrentMonthViewItem: Int = 0

	/**
	 * 标记的日期,数量巨大，请使用这个
	 */

	var mSchemeDatesMap = ConcurrentHashMap<String, SysuerCalendar>()

	/**
	 * 点击Padding位置事件
	 */

	var mClickCalendarPaddingListener: OnClickCalendarPaddingListener? = null

	/**
	 * 日期拦截事件
	 */

	var mCalendarInterceptListener: OnCalendarInterceptListener? = null

	/**
	 * 日期选中监听
	 */

	var mCalendarSelectListener: OnCalendarSelectListener? = null

	/**
	 * 范围选择
	 */

	var mCalendarRangeSelectListener: OnCalendarRangeSelectListener? = null

	/**
	 * 多选选择事件
	 */

	var mCalendarMultiSelectListener: OnCalendarMultiSelectListener? = null

	/**
	 * 外部日期长按事件
	 */

	var mCalendarLongClickListener: OnCalendarLongClickListener? = null

	/**
	 * 内部日期切换监听，用于内部更新计算
	 */

	var mInnerListener: OnInnerDateSelectedListener? = null

	/**
	 * 快速年份切换
	 */

	var mYearChangeListener: OnYearChangeListener? = null

	/**
	 * 月份切换事件
	 */

	var mMonthChangeListener: OnMonthChangeListener? = null

	/**
	 * 周视图改变事件
	 */

	var mWeekChangeListener: OnWeekChangeListener? = null

	/**
	 * 视图改变事件
	 */

	var mViewChangeListener: OnViewChangeListener? = null

	/**
	 * 年视图改变事件
	 */

	var mYearViewChangeListener: OnYearViewChangeListener? = null

	/**
	 * 保存选中的日期
	 */

	var mSelectedSysuerCalendar: SysuerCalendar? = null

	/**
	 * 保存标记位置
	 */

	var mIndexSysuerCalendar: SysuerCalendar? = null

	/**
	 * 多选日历
	 */

	val mSelectedCalendars: MutableMap<String?, SysuerCalendar?> = HashMap()

	/**
	 * 选择范围日历
	 */

	var mSelectedStartRangeSysuerCalendar: SysuerCalendar? = null


	var mSelectedEndRangeSysuerCalendar: SysuerCalendar? = null
	var curDayLunarTextColor: Int = 0

	//    private int mCurMonthLunarTextColor;
	var weekTextColor: Int = 0

	/**
	 * 年视图一些padding
	 */
	var yearViewPadding: Int = 0
		private set

	/**
	 * 年视图一些padding
	 */
	var yearViewMonthPaddingLeft: Int = 0
	var yearViewMonthPaddingRight: Int = 0
	var yearViewMonthPaddingTop: Int = 0
	var yearViewMonthPaddingBottom: Int = 0

	/**
	 * 年视图字体大小
	 */
	var yearViewMonthTextSize: Int = 0
	var yearViewDayTextSize: Int = 0
	var yearViewWeekTextSize: Int = 0

	/**
	 * 年视图月份高度和周的高度
	 */
	var yearViewMonthHeight: Int = 0
	var yearViewWeekHeight: Int = 0
	var yearViewSelectTextColor: Int = 0
	var yearViewCurDayTextColor: Int = 0
	var yearViewWeekTextColor: Int = 0

	/**
	 * 星期栏的背景、线的背景、年份背景
	 */
	var weekLineBackground: Int = 0
	var yearViewBackground: Int = 0
	var weekBackground: Int = 0

	/**
	 * 星期栏Line margin
	 */
	var weekLineMargin: Int = 0

	/**
	 * 星期栏字体大小
	 */
	var weekTextSize: Int = 0

	/**
	 * 自定义的日历路径
	 */
	private var mMonthViewClassPath: String? = null

	/**
	 * 自定义周视图路径
	 */
	private var mWeekViewClassPath: String? = null

	/**
	 * 自定义年视图路径
	 */
	var yearViewClassPath: String? = null

	/**
	 * 自定义周栏路径
	 */
	private var mWeekBarClassPath: String? = null

	/**
	 * 日期和农历文本大小
	 */
	var dayTextSize: Int = 0
	var lunarTextSize: Int = 0

	/**
	 * 是否是全屏日历
	 */

	var isFullScreenCalendar: Boolean = false

	/**
	 * 星期栏的高度
	 */
	var weekBarHeight: Int = 0
	var defaultCalendarSelectDay: Int = 0

	/**
	 * 周起始
	 */
	var weekStart: Int = 0

	/**
	 * 月份显示模式
	 */
	var monthViewShowMode: Int = 0

	/**
	 * 选择模式
	 */
	var selectMode: Int = 0

	/**
	 * 各种字体颜色，看名字知道对应的地方
	 */
	var curDayTextColor: Int = 0
		private set
	var schemeTextColor: Int = 0
		private set
	var schemeLunarTextColor: Int = 0
		private set
	var otherMonthTextColor: Int = 0
		private set
	var currentMonthTextColor: Int = 0
		private set
	var selectedTextColor: Int = 0
		private set
	var selectedLunarTextColor: Int = 0
		private set
	var currentMonthLunarTextColor: Int = 0
		private set
	var otherMonthLunarTextColor: Int = 0
		private set
	var isPreventLongPressedSelected: Boolean = false
	var yearViewPaddingLeft: Int = 0
		private set
	var yearViewPaddingRight: Int = 0
		private set

	/**
	 * 日历内部左右padding
	 */
	private var mCalendarPadding = 0

	/**
	 * 日历内部左padding
	 */
	var calendarPaddingLeft: Int = 0

	/**
	 * 日历内部右padding
	 */
	var calendarPaddingRight: Int = 0

	/**
	 * 年视图字体和标记颜色
	 */
	var yearViewMonthTextColor: Int = 0
		private set
	var yearViewDayTextColor: Int = 0
		private set
	var yearViewSchemeTextColor: Int = 0
		private set

	/**
	 * 标记的主题色和选中的主题色
	 */
	var schemeThemeColor: Int = 0
		private set
	var selectedThemeColor: Int = 0
		private set

	/**
	 * 月视图类
	 */
	lateinit var monthViewClass: Class<*>

	/**
	 * 周视图类
	 */
	lateinit var weekViewClass: Class<*>

	/**
	 * 周视图类
	 */
	lateinit var yearViewClass: Class<*>

	/**
	 * 自定义周栏
	 */
	lateinit var weekBarClass: Class<*>

	/**
	 * 标记文本
	 */
	var schemeText: String? = null
		private set

	/**
	 * 最小年份和最大年份
	 */
	var minYear: Int = 0
		private set
	var maxYear: Int = 0
		private set

	/**
	 * 最小年份和最大年份对应最小月份和最大月份
	 * when you want set 2015-07 to 2017-08
	 */
	var minYearMonth: Int = 0
		private set
	var maxYearMonth: Int = 0
		private set

	/**
	 * 最小年份和最大年份对应最小天和最大天数
	 * when you want set like 2015-07-08 to 2017-08-30
	 */
	var minYearDay: Int = 0
		private set
	var maxYearDay: Int = 0
		private set

	/**
	 * 日历卡的项高度
	 */
	var calendarItemHeight: Int = 0

	/**
	 * 今天的日子
	 */
	private lateinit var mCurrentDate: SysuerCalendar
	var isMonthViewScrollable: Boolean = false
	var isWeekViewScrollable: Boolean = false
	var isYearViewScrollable: Boolean = false
	var maxMultiSelectSize: Int = 0
	var minSelectRange: Int = 0
		private set
	var maxSelectRange: Int = 0
		private set

	init {
		try {
			context.withStyledAttributes(attrs, R.styleable.CalendarView) {
				init(context)
				mCalendarPadding =
					getDimension(R.styleable.CalendarView_calendar_padding, 0f).toInt()
				calendarPaddingLeft =
					getDimension(R.styleable.CalendarView_calendar_padding_left, 0f).toInt()
				calendarPaddingRight =
					getDimension(R.styleable.CalendarView_calendar_padding_right, 0f).toInt()

				if (mCalendarPadding != 0) {
					calendarPaddingLeft = mCalendarPadding
					calendarPaddingRight = mCalendarPadding
				}

				schemeTextColor = getColor(R.styleable.CalendarView_scheme_text_color, -0x1)
				schemeLunarTextColor =
					getColor(R.styleable.CalendarView_scheme_lunar_text_color, -0x1e1e1f)
				schemeThemeColor = getColor(R.styleable.CalendarView_scheme_theme_color, 0x50CFCFCF)
				mMonthViewClassPath = getString(R.styleable.CalendarView_month_view)
				yearViewClassPath = getString(R.styleable.CalendarView_year_view)
				mWeekViewClassPath = getString(R.styleable.CalendarView_week_view)
				mWeekBarClassPath = getString(R.styleable.CalendarView_week_bar_view)
				weekTextSize = getDimensionPixelSize(
						R.styleable.CalendarView_week_text_size, dipToPx(context, 12f)
				)
				weekBarHeight = getDimension(
						R.styleable.CalendarView_week_bar_height, dipToPx(context, 40f).toFloat()
				).toInt()
				weekLineMargin = getDimension(
						R.styleable.CalendarView_week_line_margin, dipToPx(context, 0f).toFloat()
				).toInt()

				schemeText = getString(R.styleable.CalendarView_scheme_text)
				if (TextUtils.isEmpty(schemeText)) {
					schemeText = "记"
				}

				isMonthViewScrollable =
					getBoolean(R.styleable.CalendarView_month_view_scrollable, true)
				isWeekViewScrollable =
					getBoolean(R.styleable.CalendarView_week_view_scrollable, true)
				isYearViewScrollable =
					getBoolean(R.styleable.CalendarView_year_view_scrollable, true)

				defaultCalendarSelectDay = getInt(
						R.styleable.CalendarView_month_view_auto_select_day, FIRST_DAY_OF_MONTH
				)

				monthViewShowMode =
					getInt(R.styleable.CalendarView_month_view_show_mode, MODE_ALL_MONTH)
				weekStart = getInt(R.styleable.CalendarView_week_start_with, WEEK_START_WITH_SUN)
				selectMode = getInt(R.styleable.CalendarView_select_mode, SELECT_MODE_DEFAULT)
				maxMultiSelectSize =
					getInt(R.styleable.CalendarView_max_multi_select_size, Int.MAX_VALUE)
				minSelectRange = getInt(R.styleable.CalendarView_min_select_range, -1)
				maxSelectRange = getInt(R.styleable.CalendarView_max_select_range, -1)
				setSelectRange(minSelectRange, maxSelectRange)

				weekBackground = getColor(R.styleable.CalendarView_week_background, Color.WHITE)
				weekLineBackground =
					getColor(R.styleable.CalendarView_week_line_background, Color.TRANSPARENT)
				yearViewBackground =
					getColor(R.styleable.CalendarView_year_view_background, Color.WHITE)
				weekTextColor = getColor(R.styleable.CalendarView_week_text_color, -0xcccccd)

				curDayTextColor =
					getColor(R.styleable.CalendarView_current_day_text_color, Color.RED)
				curDayLunarTextColor =
					getColor(R.styleable.CalendarView_current_day_lunar_text_color, Color.RED)

				selectedThemeColor =
					getColor(R.styleable.CalendarView_selected_theme_color, 0x50CFCFCF)
				selectedTextColor =
					getColor(R.styleable.CalendarView_selected_text_color, -0xeeeeef)

				selectedLunarTextColor =
					getColor(R.styleable.CalendarView_selected_lunar_text_color, -0xeeeeef)
				currentMonthTextColor =
					getColor(R.styleable.CalendarView_current_month_text_color, -0xeeeeef)
				otherMonthTextColor =
					getColor(R.styleable.CalendarView_other_month_text_color, -0x1e1e1f)

				currentMonthLunarTextColor = getColor(
						R.styleable.CalendarView_current_month_lunar_text_color, -0x1e1e1f
				)
				otherMonthLunarTextColor =
					getColor(R.styleable.CalendarView_other_month_lunar_text_color, -0x1e1e1f)
				minYear = getInt(R.styleable.CalendarView_min_year, 1971).coerceAtLeast(MIN_YEAR)
				maxYear = getInt(R.styleable.CalendarView_max_year, 2055).coerceAtMost(MAX_YEAR)
				minYearMonth = getInt(R.styleable.CalendarView_min_year_month, 1)
				maxYearMonth = getInt(R.styleable.CalendarView_max_year_month, 12)
				minYearDay = getInt(R.styleable.CalendarView_min_year_day, 1)
				maxYearDay = getInt(R.styleable.CalendarView_max_year_day, -1)

				dayTextSize = getDimensionPixelSize(
						R.styleable.CalendarView_day_text_size, dipToPx(context, 16f)
				)
				lunarTextSize = getDimensionPixelSize(
						R.styleable.CalendarView_lunar_text_size, dipToPx(context, 10f)
				)
				calendarItemHeight = getDimension(
						R.styleable.CalendarView_calendar_height, dipToPx(context, 56f).toFloat()
				).toInt()
				isFullScreenCalendar =
					getBoolean(R.styleable.CalendarView_calendar_match_parent, false)

				//年视图相关
				yearViewMonthTextSize = getDimensionPixelSize(
						R.styleable.CalendarView_year_view_month_text_size, dipToPx(context, 18f)
				)
				yearViewDayTextSize = getDimensionPixelSize(
						R.styleable.CalendarView_year_view_day_text_size, dipToPx(context, 7f)
				)
				yearViewMonthTextColor =
					getColor(R.styleable.CalendarView_year_view_month_text_color, -0xeeeeef)
				yearViewDayTextColor =
					getColor(R.styleable.CalendarView_year_view_day_text_color, -0xeeeeef)
				yearViewSchemeTextColor = getColor(
						R.styleable.CalendarView_year_view_scheme_color, schemeThemeColor
				)
				yearViewWeekTextColor =
					getColor(R.styleable.CalendarView_year_view_week_text_color, -0xcccccd)
				yearViewCurDayTextColor = getColor(
						R.styleable.CalendarView_year_view_current_day_text_color, curDayTextColor
				)
				yearViewSelectTextColor =
					getColor(R.styleable.CalendarView_year_view_select_text_color, -0xcccccd)
				yearViewWeekTextSize = getDimensionPixelSize(
						R.styleable.CalendarView_year_view_week_text_size, dipToPx(context, 8f)
				)
				yearViewMonthHeight = getDimensionPixelSize(
						R.styleable.CalendarView_year_view_month_height, dipToPx(context, 32f)
				)
				yearViewWeekHeight = getDimensionPixelSize(
						R.styleable.CalendarView_year_view_week_height, dipToPx(context, 0f)
				)

				yearViewPadding = getDimension(
						R.styleable.CalendarView_year_view_padding, dipToPx(context, 12f).toFloat()
				).toInt()
				yearViewPaddingLeft = getDimension(
						R.styleable.CalendarView_year_view_padding_left,
						dipToPx(context, 12f).toFloat()
				).toInt()
				yearViewPaddingRight = getDimension(
						R.styleable.CalendarView_year_view_padding_right,
						dipToPx(context, 12f).toFloat()
				).toInt()

				if (yearViewPadding != 0) {
					yearViewPaddingLeft = yearViewPadding
					yearViewPaddingRight = yearViewPadding
				}

				yearViewMonthPaddingTop = getDimension(
						R.styleable.CalendarView_year_view_month_padding_top,
						dipToPx(context, 4f).toFloat()
				).toInt()
				yearViewMonthPaddingBottom = getDimension(
						R.styleable.CalendarView_year_view_month_padding_bottom,
						dipToPx(context, 4f).toFloat()
				).toInt()

				yearViewMonthPaddingLeft = getDimension(
						R.styleable.CalendarView_year_view_month_padding_left,
						dipToPx(context, 4f).toFloat()
				).toInt()
				yearViewMonthPaddingRight = getDimension(
						R.styleable.CalendarView_year_view_month_padding_right,
						dipToPx(context, 4f).toFloat()
				).toInt()

			}
		} catch (_: Exception) {
		}
		init()
	}

	private fun init() {
		val c = Calendar.getInstance()
		c.time = Date()
		mCurrentDate = SysuerCalendar().apply {
			year = c.get(Calendar.YEAR)
			month = c.get(Calendar.MONTH) + 1
			day = c.get(Calendar.DAY_OF_MONTH)
			isCurrentDay = true
		}
		setupLunarCalendar(mCurrentDate)
		setRange(minYear, minYearMonth, maxYear, maxYearMonth)

		try {
			weekBarClass = mWeekBarClassPath?.takeIf { it.isNotEmpty() }?.let {
				Class.forName(it)
			} ?: WeekBar::class.java
		} catch (_: Exception) {
		}

		try {
			yearViewClass = yearViewClassPath?.takeIf { it.isNotEmpty() }?.let {
				Class.forName(it)
			} ?: DefaultYearView::class.java
		} catch (_: Exception) {
		}

		try {
			monthViewClass = mMonthViewClassPath?.takeIf { it.isNotEmpty() }?.let {
				Class.forName(it)
			} ?: DefaultMonthView::class.java
		} catch (_: Exception) {
		}

		try {
			this.weekViewClass = mWeekViewClassPath?.takeIf { it.isNotEmpty() }?.let {
				Class.forName(it)
			} ?: DefaultWeekView::class.java
		} catch (_: Exception) {
		}
	}


	private fun setRange(
		minYear: Int, minYearMonth: Int, maxYear: Int, maxYearMonth: Int
	) {
		this.minYear = minYear
		this.minYearMonth = minYearMonth
		this.maxYear = maxYear.coerceAtLeast(currentDay.year)
		this.maxYearMonth = maxYearMonth.takeUnless { it == -1 } ?: getMonthDaysCount(
				this.maxYear,
				this.maxYearMonth
		)
		val y = currentDay.year - this.minYear
		mCurrentMonthViewItem = 12 * y + currentDay.month - this.minYearMonth
	}

	fun setRange(
		minYear: Int,
		minYearMonth: Int,
		minYearDay: Int,
		maxYear: Int,
		maxYearMonth: Int,
		maxYearDay: Int
	) {
		this.minYear = minYear
		this.minYearMonth = minYearMonth
		this.minYearDay = minYearDay
		this.maxYear = maxYear
		this.maxYearMonth = maxYearMonth
		this.maxYearDay = maxYearDay
		if (this.maxYearDay == -1) this.maxYearDay = getMonthDaysCount(
				this.maxYear, this.maxYearMonth
		)
		val y = currentDay.year - this.minYear
		mCurrentMonthViewItem = 12 * y + currentDay.month - this.minYearMonth
	}

	fun setTextColor(
		curDayTextColor: Int,
		curMonthTextColor: Int,
		otherMonthTextColor: Int,
		curMonthLunarTextColor: Int,
		otherMonthLunarTextColor: Int
	) {
		this.curDayTextColor = curDayTextColor
		this.otherMonthTextColor = otherMonthTextColor
		this.currentMonthTextColor = curMonthTextColor
		this.currentMonthLunarTextColor = curMonthLunarTextColor
		this.otherMonthLunarTextColor = otherMonthLunarTextColor
	}

	fun setSchemeColor(schemeColor: Int, schemeTextColor: Int, schemeLunarTextColor: Int) {
		this.schemeThemeColor = schemeColor
		this.schemeTextColor = schemeTextColor
		this.schemeLunarTextColor = schemeLunarTextColor
	}

	fun setYearViewTextColor(
		yearViewMonthTextColor: Int, yearViewDayTextColor: Int, yarViewSchemeTextColor: Int
	) {
		this.yearViewMonthTextColor = yearViewMonthTextColor
		this.yearViewDayTextColor = yearViewDayTextColor
		this.yearViewSchemeTextColor = yarViewSchemeTextColor
	}

	fun setSelectColor(selectedColor: Int, selectedTextColor: Int, selectedLunarTextColor: Int) {
		this.selectedThemeColor = selectedColor
		this.selectedTextColor = selectedTextColor
		this.selectedLunarTextColor = selectedLunarTextColor
	}

	fun setThemeColor(selectedThemeColor: Int, schemeColor: Int) {
		this.selectedThemeColor = selectedThemeColor
		this.schemeThemeColor = schemeColor
	}

	fun setSelectRange(minRange: Int, maxRange: Int) {
		if (maxRange in 1..<minRange) {
			maxSelectRange = minRange
			minSelectRange = minRange
		} else {
			minSelectRange = minRange.coerceAtLeast(-1)
			maxSelectRange = maxRange.coerceAtLeast(-1)
		}
	}

	val currentDay: SysuerCalendar
		get() = mCurrentDate

	fun updateCurrentDay() {
		val c = Calendar.getInstance()
		c.time = Date()
		currentDay.year = c.get(Calendar.YEAR)
		currentDay.month = c.get(Calendar.MONTH) + 1
		currentDay.day = c.get(Calendar.DAY_OF_MONTH)
		setupLunarCalendar(currentDay)
	}

	var calendarPadding: Int
		get() = mCalendarPadding
		set(mCalendarPadding) {
			this.mCalendarPadding = mCalendarPadding
			this.calendarPaddingLeft = mCalendarPadding
			this.calendarPaddingRight = mCalendarPadding
		}

	fun clearSelectedScheme() {
		mSelectedSysuerCalendar?.clearScheme()
	}

	fun updateSelectCalendarScheme() {
		if (!mSchemeDatesMap.isEmpty()) {
			val key = mSelectedSysuerCalendar.toString()
			if (mSchemeDatesMap.containsKey(key)) {
				val d = mSchemeDatesMap[key]
				mSelectedSysuerCalendar?.mergeScheme(d, this.schemeText!!)
			}
		} else {
			clearSelectedScheme()
		}
	}

	fun updateCalendarScheme(targetSysuerCalendar: SysuerCalendar?) {
		if (targetSysuerCalendar != null && !mSchemeDatesMap.isEmpty()) {
			mSchemeDatesMap[targetSysuerCalendar.toString()]?.let {
				targetSysuerCalendar.mergeScheme(it, schemeText ?: "")
			}
		}
	}

	fun createCurrentDate(): SysuerCalendar = SysuerCalendar().apply {
		year = currentDay.year
		week = currentDay.week
		month = currentDay.month
		day = currentDay.day
		isCurrentDay = true
	}.also {
		setupLunarCalendar(it)
	}

	val minRangeSysuerCalendar: SysuerCalendar
		get() = SysuerCalendar().apply {
			year = minYear
			month = minYearMonth
			day = minYearDay
			isCurrentDay = this == mCurrentDate
		}.also {
			setupLunarCalendar(it)
		}

	val maxRangeSysuerCalendar: SysuerCalendar
		get() = SysuerCalendar().apply {
			year = maxYear
			month = maxYearMonth
			day = maxYearDay
			isCurrentDay = this == mCurrentDate
		}.also {
			setupLunarCalendar(it)
		}

	/**
	 * 添加事件标记，来自Map
	 */
	fun addSchemesFromMap(mItems: MutableList<SysuerCalendar>) {
		mItems.forEach {
			if (mSchemeDatesMap.containsKey(it.toString())) {
				val d = mSchemeDatesMap[it.toString()] ?: return
				it.scheme = d.scheme.ifEmpty { schemeText ?: "" }
				it.schemeColor = d.schemeColor
				it.schemes = d.schemes
			} else {
				it.scheme = ""
				it.schemeColor = 0
				it.schemes.clear()
			}
		}
//		for (a in mItems) {
//			if (mSchemeDatesMap.containsKey(a.toString())) {
//				val d = mSchemeDatesMap[a.toString()] ?: continue
//				a.scheme = (if (TextUtils.isEmpty(d.scheme)) this.schemeText else d.scheme)!!
//				a.schemeColor = d.schemeColor
//				a.schemes = d.schemes
//			} else {
//				a.scheme = ""
//				a.schemeColor = 0
//				a.schemes.clear()
//			}
//		}
	}


	/**
	 * 添加数据
	 * 
	 * @param mSchemeDates mSchemeDates
	 */
	fun addSchemes(mSchemeDates: MutableMap<String, SysuerCalendar?>) {
		mSchemeDates.forEach { (string, calendar) ->
			if (calendar == null) mSchemeDatesMap.remove(string) else mSchemeDatesMap[string] =
				calendar
		}
	}

	/**
	 * 清楚选择
	 */
	fun clearSelectRange() {
		mSelectedStartRangeSysuerCalendar = null
		mSelectedEndRangeSysuerCalendar = null
	}

	val selectSysuerCalendarRange: MutableList<SysuerCalendar>?
		/**
		 * 获得选中范围
		 * 
		 * @return 选中范围
		 */
		get() {
			if (selectMode != SELECT_MODE_RANGE) {
				return null
			}
			val sysuerCalendars: MutableList<SysuerCalendar> = mutableListOf()
			val startCalendar = mSelectedStartRangeSysuerCalendar ?: return sysuerCalendars
			val endCalendar = mSelectedEndRangeSysuerCalendar ?: return sysuerCalendars
			val day = (1000 * 3600 * 24).toLong()
			val date = Calendar.getInstance()

			date.set(
					startCalendar.year, startCalendar.month - 1, startCalendar.day
			)
			val startTimeMills = date.getTimeInMillis() //获得起始时间戳
			date.set(
					endCalendar.year, endCalendar.month - 1, endCalendar.day
			)
			val endTimeMills = date.getTimeInMillis()
			var start = startTimeMills
			while (start <= endTimeMills) {
				date.setTimeInMillis(start)
				val sysuerCalendar = SysuerCalendar().apply {
					year = date.get(Calendar.YEAR)
					month = date.get(Calendar.MONTH) + 1
					this.day = date.get(Calendar.DAY_OF_MONTH)
				}
				setupLunarCalendar(sysuerCalendar)
				updateCalendarScheme(sysuerCalendar)
				if (mCalendarInterceptListener?.onCalendarIntercept(
							sysuerCalendar
					) ?: false
				) {
					start += day
					continue
				}

				sysuerCalendars.add(sysuerCalendar)
				start += day
			}
			addSchemesFromMap(sysuerCalendars)
			return sysuerCalendars
		}

	companion object {
		/**
		 * 周起始：周日
		 */
		const val WEEK_START_WITH_SUN: Int = 1

		/**
		 * 周起始：周一
		 */
		const val WEEK_START_WITH_MON: Int = 2

		/**
		 * 周起始：周六
		 */
		const val WEEK_START_WITH_SAT: Int = 7

		/**
		 * 默认选择日期1号first_day_of_month
		 */
		const val FIRST_DAY_OF_MONTH: Int = 0

		/**
		 * 跟随上个月last_select_day
		 */
		const val LAST_MONTH_VIEW_SELECT_DAY: Int = 1

		/**
		 * 跟随上个月last_select_day_ignore_current忽视今天
		 */
		const val LAST_MONTH_VIEW_SELECT_DAY_IGNORE_CURRENT: Int = 2

		/**
		 * 全部显示
		 */
		const val MODE_ALL_MONTH: Int = 0

		/**
		 * 仅显示当前月份
		 */
		const val MODE_ONLY_CURRENT_MONTH: Int = 1

		/**
		 * 自适应显示，不会多出一行，但是会自动填充
		 */
		const val MODE_FIT_MONTH: Int = 2

		/**
		 * 默认选择模式
		 */
		const val SELECT_MODE_DEFAULT: Int = 0

		/**
		 * 单选模式
		 */
		const val SELECT_MODE_SINGLE: Int = 1

		/**
		 * 范围选择模式
		 */
		const val SELECT_MODE_RANGE: Int = 2

		/**
		 * 多选模式
		 */
		const val SELECT_MODE_MULTI: Int = 3

		/**
		 * 支持转换的最小农历年份
		 */
		const val MIN_YEAR: Int = 1900

		/**
		 * 支持转换的最大农历年份
		 */
		private const val MAX_YEAR = 2099
	}
}
