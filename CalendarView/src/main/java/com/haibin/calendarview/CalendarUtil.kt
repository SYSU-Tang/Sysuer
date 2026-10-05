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
import com.haibin.calendarview.LunarCalendar.setupLunarCalendar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.min

/**
 * 一些日期辅助计算工具
 */
object CalendarUtil {
	private const val ONE_DAY = (1000 * 3600 * 24).toLong()

	@JvmStatic
	fun getDate(formatStr: String?, date: Date): Int {
		return SimpleDateFormat(formatStr, Locale.getDefault()).format(date).toInt()
	}

	/**
	 * 判断一个日期是否是周末，即周六日
	 * 
	 * @param sysuerCalendar calendar
	 * @return 判断一个日期是否是周末，即周六日
	 */
	fun isWeekend(sysuerCalendar: SysuerCalendar): Boolean {
		val week = getWeekFormCalendar(sysuerCalendar)
		return week == 0 || week == 6
	}

	/**
	 * 获取某月的天数
	 * 
	 * @param year  年
	 * @param month 月
	 * @return 某月的天数
	 */
	@JvmStatic
	fun getMonthDaysCount(year: Int, month: Int): Int {
		var count = 0
		//判断大月份
		if (month == 1 || month == 3 || month == 5 || month == 7 || month == 8 || month == 10 || month == 12) {
			count = 31
		}

		//判断小月
		if (month == 4 || month == 6 || month == 9 || month == 11) {
			count = 30
		}

		//判断平年与闰年
		if (month == 2) {
			count = if (isLeapYear(year)) {
				29
			} else {
				28
			}
		}
		return count
	}


	/**
	 * 是否是闰年
	 * 
	 * @param year year
	 * @return 是否是闰年
	 */
	fun isLeapYear(year: Int): Boolean {
		return ((year % 4 == 0) && (year % 100 != 0)) || (year % 400 == 0)
	}


	fun getMonthViewLineCount(year: Int, month: Int, weekStartWith: Int, mode: Int): Int {
		if (mode == CalendarViewDelegate.MODE_ALL_MONTH) {
			return 6
		}
		val nextDiff = getMonthEndDiff(year, month, weekStartWith)
		val preDiff = getMonthViewStartDiff(year, month, weekStartWith)
		val monthDayCount = getMonthDaysCount(year, month)
		return (preDiff + monthDayCount + nextDiff) / 7
	}

	/**
	 * 获取月视图的确切高度
	 * Test pass
	 * 
	 * @param year       年
	 * @param month      月
	 * @param itemHeight 每项的高度
	 * @param weekStartWith 周起始
	 * @return 不需要多余行的高度
	 */
	@JvmStatic
	fun getMonthViewHeight(year: Int, month: Int, itemHeight: Int, weekStartWith: Int): Int {
		val date = Calendar.getInstance()
		date.set(year, month - 1, 1, 12, 0, 0)
		val preDiff = getMonthViewStartDiff(year, month, weekStartWith)
		val monthDaysCount = getMonthDaysCount(year, month)
		val nextDiff = getMonthEndDiff(year, month, monthDaysCount, weekStartWith)
		return (preDiff + monthDaysCount + nextDiff) / 7 * itemHeight
	}

	/**
	 * 获取月视图的确切高度
	 * Test pass
	 * 
	 * @param year       年
	 * @param month      月
	 * @param itemHeight 每项的高度
	 * @param weekStartWith weekStartWith
	 * @param mode  mode
	 * @return 不需要多余行的高度
	 */
	@JvmStatic
	fun getMonthViewHeight(
		year: Int, month: Int, itemHeight: Int, weekStartWith: Int, mode: Int
	): Int {
		if (mode == CalendarViewDelegate.MODE_ALL_MONTH) {
			return itemHeight * 6
		}
		return getMonthViewHeight(year, month, itemHeight, weekStartWith)
	}

	/**
	 * 获取某天在该月的第几周,换言之就是获取这一天在该月视图的第几行,第几周，根据周起始动态获取
	 * Test pass，单元测试通过
	 * 
	 * @param sysuerCalendar  calendar
	 * @param weekStart 其实星期是哪一天？
	 * @return 获取某天在该月的第几周 the week line in MonthView
	 */
	@JvmStatic
	fun getWeekFromDayInMonth(sysuerCalendar: SysuerCalendar, weekStart: Int): Int {
		val date = Calendar.getInstance()
		date.set(sysuerCalendar.year, sysuerCalendar.month - 1, 1, 12, 0, 0)
		//该月第一天为星期几,星期天 == 0
		val diff = getMonthViewStartDiff(sysuerCalendar, weekStart)
		return (sysuerCalendar.day + diff - 1) / 7 + 1
	}

	/**
	 * 获取上一个日子
	 * 
	 * @param sysuerCalendar calendar
	 * @return 获取上一个日子
	 */
	fun getPreCalendar(sysuerCalendar: SysuerCalendar): SysuerCalendar {
		val date = Calendar.getInstance()

		date.set(sysuerCalendar.year, sysuerCalendar.month - 1, sysuerCalendar.day, 12, 0, 0) //

		val timeMills = date.getTimeInMillis() //获得起始时间戳

		date.setTimeInMillis(timeMills - ONE_DAY)

		val preSysuerCalendar = SysuerCalendar()
		preSysuerCalendar.year = date.get(Calendar.YEAR)
		preSysuerCalendar.month = date.get(Calendar.MONTH) + 1
		preSysuerCalendar.day = date.get(Calendar.DAY_OF_MONTH)

		return preSysuerCalendar
	}

	fun getNextCalendar(sysuerCalendar: SysuerCalendar): SysuerCalendar {
		val date = Calendar.getInstance()

		date.set(sysuerCalendar.year, sysuerCalendar.month - 1, sysuerCalendar.day, 12, 0, 0) //

		val timeMills = date.getTimeInMillis() //获得起始时间戳

		date.setTimeInMillis(timeMills + ONE_DAY)

		val nextSysuerCalendar = SysuerCalendar()
		nextSysuerCalendar.year = date.get(Calendar.YEAR)
		nextSysuerCalendar.month = date.get(Calendar.MONTH) + 1
		nextSysuerCalendar.day = date.get(Calendar.DAY_OF_MONTH)

		return nextSysuerCalendar
	}

	/**
	 * DAY_OF_WEEK return  1  2  3 	4  5  6	 7，偏移了一位
	 * 获取日期所在月视图对应的起始偏移量
	 * Test pass
	 * 
	 * @param sysuerCalendar  calendar
	 * @param weekStart weekStart 星期的起始
	 * @return 获取日期所在月视图对应的起始偏移量 the start diff with MonthView
	 */
	@JvmStatic
	fun getMonthViewStartDiff(sysuerCalendar: SysuerCalendar, weekStart: Int): Int {
		val date = Calendar.getInstance()
		date.set(sysuerCalendar.year, sysuerCalendar.month - 1, 1, 12, 0, 0)
		val week = date.get(Calendar.DAY_OF_WEEK)
		if (weekStart == CalendarViewDelegate.WEEK_START_WITH_SUN) {
			return week - 1
		}
		if (weekStart == CalendarViewDelegate.WEEK_START_WITH_MON) {
			return if (week == 1) 6 else week - weekStart
		}
		return if (week == CalendarViewDelegate.WEEK_START_WITH_SAT) 0 else week
	}


	/**
	 * DAY_OF_WEEK return  1  2  3 	4  5  6	 7，偏移了一位
	 * 获取日期所在月视图对应的起始偏移量
	 * Test pass
	 * 
	 * @param year      年
	 * @param month     月
	 * @param weekStart 周起始
	 * @return 获取日期所在月视图对应的起始偏移量 the start diff with MonthView
	 */
	@JvmStatic
	fun getMonthViewStartDiff(year: Int, month: Int, weekStart: Int): Int {
		val date = Calendar.getInstance()
		date.set(year, month - 1, 1, 12, 0, 0)
		val week = date.get(Calendar.DAY_OF_WEEK)
		if (weekStart == CalendarViewDelegate.WEEK_START_WITH_SUN) {
			return week - 1
		}
		if (weekStart == CalendarViewDelegate.WEEK_START_WITH_MON) {
			return if (week == 1) 6 else week - weekStart
		}
		return if (week == CalendarViewDelegate.WEEK_START_WITH_SAT) 0 else week
	}


	/**
	 * DAY_OF_WEEK return  1  2  3 	4  5  6	 7，偏移了一位
	 * 获取日期月份对应的结束偏移量,用于计算两个年份之间总共有多少周，不用于MonthView
	 * Test pass
	 * 
	 * @param year      年
	 * @param month     月
	 * @param weekStart 周起始
	 * @return 获取日期月份对应的结束偏移量 the end diff in Month not MonthView
	 */
	@JvmStatic
	fun getMonthEndDiff(year: Int, month: Int, weekStart: Int): Int {
		return getMonthEndDiff(year, month, getMonthDaysCount(year, month), weekStart)
	}


	/**
	 * DAY_OF_WEEK return  1  2  3 	4  5  6	 7，偏移了一位
	 * 获取日期月份对应的结束偏移量,用于计算两个年份之间总共有多少周，不用于MonthView
	 * Test pass
	 * 
	 * @param year      年
	 * @param month     月
	 * @param weekStart 周起始
	 * @return 获取日期月份对应的结束偏移量 the end diff in Month not MonthView
	 */
	private fun getMonthEndDiff(year: Int, month: Int, day: Int, weekStart: Int): Int {
		val date = Calendar.getInstance()
		date.set(year, month - 1, day)
		val week = date.get(Calendar.DAY_OF_WEEK)
		if (weekStart == CalendarViewDelegate.WEEK_START_WITH_SUN) {
			return 7 - week
		}
		if (weekStart == CalendarViewDelegate.WEEK_START_WITH_MON) {
			return if (week == 1) 0 else 7 - week + 1
		}
		return if (week == 7) 6 else 7 - week - 1
	}

	/**
	 * 获取某个日期是星期几
	 * 测试通过
	 * 
	 * @param sysuerCalendar 某个日期
	 * @return 返回某个日期是星期几
	 */
	fun getWeekFormCalendar(sysuerCalendar: SysuerCalendar): Int {
		val date = Calendar.getInstance()
		date.set(sysuerCalendar.year, sysuerCalendar.month - 1, sysuerCalendar.day)
		return date.get(Calendar.DAY_OF_WEEK) - 1
	}


	/**
	 * 获取周视图的切换默认选项位置 WeekView index
	 * 测试通过 test pass
	 * 
	 * @param sysuerCalendar  calendar
	 * @param weekStart weekStart
	 * @return 获取周视图的切换默认选项位置
	 */
	fun getWeekViewIndexFromCalendar(
		sysuerCalendar: SysuerCalendar, weekStart: Int
	): Int {
		return getWeekViewStartDiff(sysuerCalendar.year, sysuerCalendar.month, sysuerCalendar.day, weekStart)
	}

	/**
	 * 是否在日期范围內
	 * 测试通过 test pass
	 * 
	 * @param sysuerCalendar     calendar
	 * @param minYear      minYear
	 * @param minYearDay   最小年份天
	 * @param minYearMonth minYearMonth
	 * @param maxYear      maxYear
	 * @param maxYearMonth maxYearMonth
	 * @param maxYearDay   最大年份天
	 * @return 是否在日期范围內
	 */
	fun isCalendarInRange(
		sysuerCalendar: SysuerCalendar,
		minYear: Int,
		minYearMonth: Int,
		minYearDay: Int,
		maxYear: Int,
		maxYearMonth: Int,
		maxYearDay: Int
	): Boolean {
		val c = Calendar.getInstance()
		c.set(minYear, minYearMonth - 1, minYearDay)
		val minTime = c.getTimeInMillis()
		c.set(maxYear, maxYearMonth - 1, maxYearDay)
		val maxTime = c.getTimeInMillis()
		c.set(sysuerCalendar.year, sysuerCalendar.month - 1, sysuerCalendar.day)
		val curTime = c.getTimeInMillis()
		return curTime in minTime..maxTime
	}

	/**
	 * 获取两个日期之间一共有多少周，
	 * 注意周起始周一、周日、周六
	 * 测试通过 test pass
	 * 
	 * @param minYear      minYear 最小年份
	 * @param minYearMonth maxYear 最小年份月份
	 * @param minYearDay   最小年份天
	 * @param maxYear      maxYear 最大年份
	 * @param maxYearMonth maxYear 最大年份月份
	 * @param maxYearDay   最大年份天
	 * @param weekStart    周起始
	 * @return 周数用于WeekViewPager itemCount
	 */
	fun getWeekCountBetweenBothCalendar(
		minYear: Int,
		minYearMonth: Int,
		minYearDay: Int,
		maxYear: Int,
		maxYearMonth: Int,
		maxYearDay: Int,
		weekStart: Int
	): Int {
		val date = Calendar.getInstance()
		date.set(minYear, minYearMonth - 1, minYearDay)
		val minTimeMills = date.getTimeInMillis() //给定时间戳
		val preDiff = getWeekViewStartDiff(minYear, minYearMonth, minYearDay, weekStart)

		date.set(maxYear, maxYearMonth - 1, maxYearDay)

		val maxTimeMills = date.getTimeInMillis() //给定时间戳

		val nextDiff = getWeekViewEndDiff(maxYear, maxYearMonth, maxYearDay, weekStart)

		var count = preDiff + nextDiff

		val c = ((maxTimeMills - minTimeMills) / ONE_DAY).toInt() + 1
		count += c
		return count / 7
	}


	/**
	 * 根据日期获取距离最小日期在第几周
	 * 用来设置 WeekView currentItem
	 * 测试通过 test pass
	 * 
	 * @param sysuerCalendar     calendar
	 * @param minYear      minYear 最小年份
	 * @param minYearMonth maxYear 最小年份月份
	 * @param minYearDay   最小年份天
	 * @param weekStart    周起始
	 * @return 返回两个年份中第几周 the WeekView currentItem
	 */
	fun getWeekFromCalendarStartWithMinCalendar(
		sysuerCalendar: SysuerCalendar,
		minYear: Int,
		minYearMonth: Int,
		minYearDay: Int,
		weekStart: Int
	): Int {
		val date = Calendar.getInstance()
		date.set(minYear, minYearMonth - 1, minYearDay) //起始日期
		val firstTimeMill = date.getTimeInMillis() //获得范围起始时间戳

		val preDiff = getWeekViewStartDiff(minYear, minYearMonth, minYearDay, weekStart) //范围起始的周偏移量

		val weekStartDiff = getWeekViewStartDiff(
				sysuerCalendar.year, sysuerCalendar.month, sysuerCalendar.day, weekStart
		) //获取点击的日子在周视图的起始，为了兼容全球时区，最大日差为一天，如果周起始偏差weekStartDiff=0，则日期加1

		date.set(
				sysuerCalendar.year,
				sysuerCalendar.month - 1,
				if (weekStartDiff == 0) sysuerCalendar.day + 1 else sysuerCalendar.day
		)

		val curTimeMills = date.getTimeInMillis() //给定时间戳

		val c = ((curTimeMills - firstTimeMill) / ONE_DAY).toInt()

		val count = preDiff + c

		return count / 7 + 1
	}

	/**
	 * 根据星期数和最小日期推算出该星期的第一天，
	 * 为了防止夏令时，导致的时间提前和延后1-2小时，导致日期出现误差1天，因此吧hourOfDay = 12
	 * //测试通过 Test pass
	 * 
	 * @param minYear      最小年份如2017
	 * @param minYearMonth maxYear 最小年份月份，like : 2017-07
	 * @param minYearDay   最小年份天
	 * @param week         从最小年份minYear月minYearMonth 日1 开始的第几周 week > 0
	 * @param weekStart 周起始
	 * @return 该星期的第一天日期
	 */
	fun getFirstCalendarStartWithMinCalendar(
		minYear: Int, minYearMonth: Int, minYearDay: Int, week: Int, weekStart: Int
	): SysuerCalendar {
		val date = Calendar.getInstance()

		date.set(minYear, minYearMonth - 1, minYearDay, 12, 0) //

		val firstTimeMills = date.getTimeInMillis() //获得起始时间戳


		val weekTimeMills = (week - 1) * 7 * ONE_DAY

		var timeCountMills = weekTimeMills + firstTimeMills

		date.setTimeInMillis(timeCountMills)

		val startDiff = getWeekViewStartDiff(
				date.get(Calendar.YEAR),
				date.get(Calendar.MONTH) + 1,
				date.get(Calendar.DAY_OF_MONTH),
				weekStart
		)

		timeCountMills -= startDiff * ONE_DAY
		date.setTimeInMillis(timeCountMills)

		val sysuerCalendar = SysuerCalendar()
		sysuerCalendar.year = date.get(Calendar.YEAR)
		sysuerCalendar.month = date.get(Calendar.MONTH) + 1
		sysuerCalendar.day = date.get(Calendar.DAY_OF_MONTH)

		return sysuerCalendar
	}


	/**
	 * 是否在日期范围内
	 * 
	 * @param sysuerCalendar calendar
	 * @param delegate delegate
	 * @return 是否在日期范围内
	 */
	@JvmStatic
	fun isCalendarInRange(
		sysuerCalendar: SysuerCalendar, delegate: CalendarViewDelegate
	): Boolean {
		return isCalendarInRange(
				sysuerCalendar,
				delegate.minYear,
				delegate.minYearMonth,
				delegate.minYearDay,
				delegate.maxYear,
				delegate.maxYearMonth,
				delegate.maxYearDay
		)
	}

	/**
	 * 是否在日期范围內
	 * 
	 * @param year         year
	 * @param month        month
	 * @param minYear      minYear
	 * @param minYearMonth minYearMonth
	 * @param maxYear      maxYear
	 * @param maxYearMonth maxYearMonth
	 * @return 是否在日期范围內
	 */
	fun isMonthInRange(
		year: Int, month: Int, minYear: Int, minYearMonth: Int, maxYear: Int, maxYearMonth: Int
	): Boolean {
		return year in minYear..maxYear && !(year == minYear && month < minYearMonth) && !(year == maxYear && month > maxYearMonth)
	}

	/**
	 * 运算 calendar1 - calendar2
	 * test Pass
	 * 
	 * @param sysuerCalendar1 calendar1
	 * @param sysuerCalendar2 calendar2
	 * @return calendar1 - calendar2
	 */
	fun differ(
		sysuerCalendar1: SysuerCalendar?, sysuerCalendar2: SysuerCalendar?
	): Int {
		if (sysuerCalendar1 == null) {
			return Int.MIN_VALUE
		}
		if (sysuerCalendar2 == null) {
			return Int.MAX_VALUE
		}
		val date = Calendar.getInstance()

		date.set(sysuerCalendar1.year, sysuerCalendar1.month - 1, sysuerCalendar1.day, 12, 0, 0) //

		val startTimeMills = date.getTimeInMillis() //获得起始时间戳

		date.set(sysuerCalendar2.year, sysuerCalendar2.month - 1, sysuerCalendar2.day, 12, 0, 0) //

		val endTimeMills = date.getTimeInMillis() //获得结束时间戳

		return ((startTimeMills - endTimeMills) / ONE_DAY).toInt()
	}

	/**
	 * 比较日期大小
	 * 
	 * @param minYear      minYear
	 * @param minYearMonth minYearMonth
	 * @param minYearDay   minYearDay
	 * @param maxYear      maxYear
	 * @param maxYearMonth maxYearMonth
	 * @param maxYearDay   maxYearDay
	 * @return <0 0 >0
	 */
	@JvmStatic
	fun compareTo(
		minYear: Int,
		minYearMonth: Int,
		minYearDay: Int,
		maxYear: Int,
		maxYearMonth: Int,
		maxYearDay: Int
	): Int {
		val first = SysuerCalendar()
		first.year = minYear
		first.month = minYearMonth
		first.day = minYearDay

		val second = SysuerCalendar()
		second.year = maxYear
		second.month = maxYearMonth
		second.day = maxYearDay
		return first.compareTo(second)
	}

	/**
	 * 为月视图初始化日历
	 * 
	 * @param year        year
	 * @param month       month
	 * @param currentDate currentDate
	 * @param weekStar    weekStar
	 * @return 为月视图初始化日历项
	 */
	@JvmStatic
	fun initCalendarForMonthView(
		year: Int, month: Int, currentDate: SysuerCalendar?, weekStar: Int
	): MutableList<SysuerCalendar> {
		val date = Calendar.getInstance()

		date.set(year, month - 1, 1)

		val mPreDiff = getMonthViewStartDiff(year, month, weekStar) //获取月视图其实偏移量

		val monthDayCount = getMonthDaysCount(year, month) //获取月份真实天数

		val preYear: Int
		val preMonth: Int
		val nextYear: Int
		val nextMonth: Int

		val size = 42

		val mItems = mutableListOf<SysuerCalendar>()

		val preMonthDaysCount: Int
		when (month) {
			1 -> { //如果是1月
				preYear = year - 1
				preMonth = 12
				nextYear = year
				nextMonth = month + 1
				preMonthDaysCount = if (mPreDiff == 0) 0 else getMonthDaysCount(preYear, preMonth)
			}

			12 -> { //如果是12月
				preYear = year
				preMonth = month - 1
				nextYear = year + 1
				nextMonth = 1
				preMonthDaysCount = if (mPreDiff == 0) 0 else getMonthDaysCount(preYear, preMonth)
			}

			else -> { //平常
				preYear = year
				preMonth = month - 1
				nextYear = year
				nextMonth = month + 1
				preMonthDaysCount = if (mPreDiff == 0) 0 else getMonthDaysCount(preYear, preMonth)
			}
		}
		var nextDay = 1
		for (i in 0..<size) {
			val sysuerCalendarDate = SysuerCalendar()
			if (i < mPreDiff) {
				sysuerCalendarDate.year = preYear
				sysuerCalendarDate.month = preMonth
				sysuerCalendarDate.day = preMonthDaysCount - mPreDiff + i + 1
			} else if (i >= monthDayCount + mPreDiff) {
				sysuerCalendarDate.year = nextYear
				sysuerCalendarDate.month = nextMonth
				sysuerCalendarDate.day = nextDay
				++nextDay
			} else {
				sysuerCalendarDate.year = year
				sysuerCalendarDate.month = month
				sysuerCalendarDate.isCurrentMonth = true
				sysuerCalendarDate.day = i - mPreDiff + 1
			}
			if (sysuerCalendarDate == currentDate) {
				sysuerCalendarDate.isCurrentDay = true
			}
			setupLunarCalendar(sysuerCalendarDate)
			mItems.add(sysuerCalendarDate)
		}
		return mItems
	}

	fun getWeekCalendars(
		sysuerCalendar: SysuerCalendar, mDelegate: CalendarViewDelegate
	): MutableList<SysuerCalendar> {
		var curTime = sysuerCalendar.timeInMillis

		val date = Calendar.getInstance()
		date.set(
				sysuerCalendar.year, sysuerCalendar.month - 1, sysuerCalendar.day, 12, 0
		) //
		val week = date.get(Calendar.DAY_OF_WEEK)
		val startDiff: Int = if (mDelegate.weekStart == 1) {
			week - 1
		} else if (mDelegate.weekStart == 2) {
			if (week == 1) 6 else week - mDelegate.weekStart
		} else {
			if (week == 7) 0 else week
		}

		curTime -= startDiff * ONE_DAY
		val minCalendar = Calendar.getInstance()
		minCalendar.setTimeInMillis(curTime)
		val startSysuerCalendar = SysuerCalendar()
		startSysuerCalendar.year = minCalendar.get(Calendar.YEAR)
		startSysuerCalendar.month = minCalendar.get(Calendar.MONTH) + 1
		startSysuerCalendar.day = minCalendar.get(Calendar.DAY_OF_MONTH)
		return initCalendarForWeekView(startSysuerCalendar, mDelegate, mDelegate.weekStart)
	}

	/**
	 * 生成周视图的7个item
	 * 
	 * @param sysuerCalendar  周视图的第一个日子calendar，所以往后推迟6天，生成周视图
	 * @param mDelegate mDelegate
	 * @param weekStart weekStart
	 * @return 生成周视图的7个item
	 */
	fun initCalendarForWeekView(
		sysuerCalendar: SysuerCalendar, mDelegate: CalendarViewDelegate, weekStart: Int
	): MutableList<SysuerCalendar> {
		val date = Calendar.getInstance() //当天时间
		date.set(sysuerCalendar.year, sysuerCalendar.month - 1, sysuerCalendar.day, 12, 0)
		val curDateMills = date.getTimeInMillis() //生成选择的日期时间戳

		//int weekEndDiff = getWeekViewEndDiff(calendar.getYear(), calendar.getMonth(), calendar.getDay(), weekStart);
		//weekEndDiff 例如周起始为周日1，当前为2020-04-01，周三，则weekEndDiff为本周结束相差今天三天，weekEndDiff=3
		val weekEndDiff = 6
		val mItems = mutableListOf<SysuerCalendar>()

		date.setTimeInMillis(curDateMills)
		val selectSysuerCalendar = SysuerCalendar()
		selectSysuerCalendar.year = sysuerCalendar.year
		selectSysuerCalendar.month = sysuerCalendar.month
		selectSysuerCalendar.day = sysuerCalendar.day
		if (selectSysuerCalendar == mDelegate.currentDay) {
			selectSysuerCalendar.isCurrentDay = true
		}
		setupLunarCalendar(selectSysuerCalendar)
		selectSysuerCalendar.isCurrentMonth = true
		mItems.add(selectSysuerCalendar)


		for (i in 1..weekEndDiff) {
			date.setTimeInMillis(curDateMills + i * ONE_DAY)
			val sysuerCalendarDate = SysuerCalendar()
			sysuerCalendarDate.year = date.get(Calendar.YEAR)
			sysuerCalendarDate.month = date.get(Calendar.MONTH) + 1
			sysuerCalendarDate.day = date.get(Calendar.DAY_OF_MONTH)
			if (sysuerCalendarDate == mDelegate.currentDay) {
				sysuerCalendarDate.isCurrentDay = true
			}
			setupLunarCalendar(sysuerCalendarDate)
			sysuerCalendarDate.isCurrentMonth = true
			mItems.add(sysuerCalendarDate)
		}
		return mItems
	}

	/**
	 * 单元测试通过
	 * 从选定的日期，获取周视图起始偏移量，用来生成周视图布局
	 * 
	 * @param year      year
	 * @param month     month
	 * @param day       day
	 * @param weekStart 周起始，1，2，7 日 一 六
	 * @return 获取周视图起始偏移量，用来生成周视图布局
	 */
	private fun getWeekViewStartDiff(year: Int, month: Int, day: Int, weekStart: Int): Int {
		val date = Calendar.getInstance()
		date.set(year, month - 1, day, 12, 0) //
		val week = date.get(Calendar.DAY_OF_WEEK)
		if (weekStart == 1) {
			return week - 1
		}
		if (weekStart == 2) {
			return if (week == 1) 6 else week - weekStart
		}
		return if (week == 7) 0 else week
	}


	/**
	 * 单元测试通过
	 * 从选定的日期，获取周视图结束偏移量，用来生成周视图布局
	 * 为了兼容DST，DST时区可能出现时间偏移1-2小时，从而导致凌晨时候实际获得的日期往前或者往后推移了一天，
	 * 日历没有时和分的概念，因此把日期的时间强制在12:00，可以避免DST兼容问题
	 * 
	 * @param year      year
	 * @param month     month
	 * @param day       day
	 * @param weekStart 周起始，1，2，7 日 一 六
	 * @return 获取周视图结束偏移量，用来生成周视图布局
	 */
	fun getWeekViewEndDiff(year: Int, month: Int, day: Int, weekStart: Int): Int {
		val date = Calendar.getInstance()
		date.set(year, month - 1, day, 12, 0)
		val week = date.get(Calendar.DAY_OF_WEEK)
		if (weekStart == 1) {
			return 7 - week
		}
		if (weekStart == 2) {
			return if (week == 1) 0 else 7 - week + 1
		}
		return if (week == 7) 6 else 7 - week - 1
	}


	/**
	 * 从月视图切换获得第一天的日期
	 * Test Pass 它是100%正确的
	 * 
	 * @param position position
	 * @param delegate position
	 * @return 从月视图切换获得第一天的日期
	 */
	fun getFirstCalendarFromMonthViewPager(
		position: Int, delegate: CalendarViewDelegate
	): SysuerCalendar {
		var sysuerCalendar = SysuerCalendar()
		sysuerCalendar.year = (position + delegate.minYearMonth - 1) / 12 + delegate.minYear
		sysuerCalendar.month = (position + delegate.minYearMonth - 1) % 12 + 1
		if (delegate.defaultCalendarSelectDay != CalendarViewDelegate.FIRST_DAY_OF_MONTH) {
			val monthDays = getMonthDaysCount(sysuerCalendar.year, sysuerCalendar.month)
			val indexCalendar = delegate.mIndexSysuerCalendar
			sysuerCalendar.day = if (indexCalendar == null || indexCalendar.day == 0) 1 else min(
					monthDays, indexCalendar.day
			)
		} else {
			sysuerCalendar.day = 1
		}
		if (!isCalendarInRange(sysuerCalendar, delegate)) {
			sysuerCalendar = if (isMinRangeEdge(sysuerCalendar, delegate)) {
				delegate.minRangeSysuerCalendar
			} else {
				delegate.maxRangeSysuerCalendar
			}
		}
		sysuerCalendar.isCurrentMonth =
			sysuerCalendar.year == delegate.currentDay.year && sysuerCalendar.month == delegate.currentDay.month
		sysuerCalendar.isCurrentDay = sysuerCalendar == delegate.currentDay
		setupLunarCalendar(sysuerCalendar)
		return sysuerCalendar
	}


	/**
	 * 根据传入的日期获取边界访问日期，要么最大，要么最小
	 * 
	 * @param sysuerCalendar calendar
	 * @param delegate delegate
	 * @return 获取边界访问日期
	 */
	fun getRangeEdgeCalendar(
		sysuerCalendar: SysuerCalendar, delegate: CalendarViewDelegate
	): SysuerCalendar {
		if (isCalendarInRange(
					delegate.currentDay, delegate
			) && delegate.defaultCalendarSelectDay != CalendarViewDelegate.LAST_MONTH_VIEW_SELECT_DAY_IGNORE_CURRENT
		) {
			return delegate.createCurrentDate()
		}
		if (isCalendarInRange(sysuerCalendar, delegate)) {
			return sysuerCalendar
		}
		val minRangeCalendar = delegate.minRangeSysuerCalendar
		if (minRangeCalendar.isSameMonth(sysuerCalendar)) {
			return delegate.minRangeSysuerCalendar
		}
		return delegate.maxRangeSysuerCalendar
	}

	/**
	 * 是否是最小访问边界了
	 * 
	 * @param sysuerCalendar calendar
	 * @return 是否是最小访问边界了
	 */
	private fun isMinRangeEdge(
		sysuerCalendar: SysuerCalendar, delegate: CalendarViewDelegate
	): Boolean {
		val c = Calendar.getInstance()
		c.set(
				delegate.minYear, delegate.minYearMonth - 1, delegate.minYearDay, 12, 0
		)
		val minTime = c.getTimeInMillis()
		c.set(sysuerCalendar.year, sysuerCalendar.month - 1, sysuerCalendar.day, 12, 0)
		val curTime = c.getTimeInMillis()
		return curTime < minTime
	}

	/**
	 * dp转px
	 * 
	 * @param context context
	 * @param dpValue dp
	 * @return px
	 */
	@JvmStatic
	fun dipToPx(context: Context, dpValue: Float): Int {
		return (dpValue * context.resources.displayMetrics.density + 0.5f).toInt()
	}
}
