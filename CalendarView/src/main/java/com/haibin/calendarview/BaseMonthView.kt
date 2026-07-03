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

/**
 * 月视图基础控件,可自由继承实现
 * 可通过此扩展各种视图如：MonthView、RangeMonthView、MultiMonthView
 */
abstract class BaseMonthView(context: Context?) : BaseView(context) {
	/**
	 * 当前日历卡年份
	 */
	protected var mYear: Int = 0
	
	/**
	 * 当前日历卡月份
	 */
	protected var mMonth: Int = 0
	
	/**
	 * 日历的行数
	 */
	@JvmField protected var mLineCount: Int = 0
	
	/**
	 * 日历高度
	 */
	protected var mHeight: Int = 0
	
	/**
	 * 下个月偏移的数量
	 */
	@JvmField protected var mNextDiff: Int = 0
	@JvmField var mMonthViewPager: MonthViewPager? = null
	
	/**
	 * 初始化日期
	 * 
	 * @param year  year
	 * @param month month
	 */
	fun initMonthWithDate(year: Int, month: Int) {
		mYear = year
		mMonth = month
		initCalendar()
		mHeight = CalendarUtil.getMonthViewHeight(year, month, mItemHeight, mDelegate!!.weekStart, mDelegate!!.monthViewShowMode)
	}
	
	/**
	 * 初始化日历
	 */
	private fun initCalendar() {
		mNextDiff = CalendarUtil.getMonthEndDiff(mYear, mMonth, mDelegate!!.weekStart)
		val preDiff = CalendarUtil.getMonthViewStartDiff(mYear, mMonth, mDelegate!!.weekStart)
		val monthDayCount = CalendarUtil.getMonthDaysCount(mYear, mMonth)
		mItems = CalendarUtil.initCalendarForMonthView(mYear, mMonth, mDelegate!!.currentDay, mDelegate!!.weekStart)
		mCurrentItem = (if (mItems!!.contains(mDelegate!!.currentDay)) mItems!!.indexOf(mDelegate!!.currentDay)
		else mItems!!.indexOf(mDelegate!!.mSelectedCalendar))
		
		if (mCurrentItem > 0 && mDelegate!!.mCalendarInterceptListener != null && mDelegate!!.mCalendarInterceptListener.onCalendarIntercept(mDelegate!!.mSelectedCalendar)) mCurrentItem = -1
		
		mLineCount = (if (mDelegate!!.monthViewShowMode == CalendarViewDelegate.MODE_ALL_MONTH) 6
		else (preDiff + monthDayCount + mNextDiff) / 7)
		addSchemesFromMap()
		invalidate()
	}
	
	protected val index: Calendar?
		/**
		 * 获取点击选中的日期
		 * 
		 * @return return
		 */
		get() {
			if (mItemWidth != 0 && mItemHeight != 0) {
				if (mX <= mDelegate!!.calendarPaddingLeft || mX >= width - mDelegate!!.calendarPaddingRight) {
					onClickCalendarPadding()
					return null
				}
				val position = mY.toInt() / mItemHeight * 7 + (((mX - mDelegate!!.calendarPaddingLeft).toInt() / mItemWidth).takeIf { it >= 7 }
					?: 6) // 选择项
				return if (position >= 0 && position < mItems!!.size) mItems!![position]
				else null
			}
			return null
		}
	
	private fun onClickCalendarPadding() {
		if (mDelegate!!.mClickCalendarPaddingListener != null) {
			var calendar: Calendar?
			val position = mY.toInt() / mItemHeight * 7 + (((mX - mDelegate!!.calendarPaddingLeft).toInt() / mItemWidth).takeIf { it >= 7 }
				?: 6) // 选择项
			if (position >= 0 && position < mItems!!.size) {
				calendar = mItems!![position]
				mDelegate!!.mClickCalendarPaddingListener.onClickCalendarPadding(mX, mY, true, calendar, getClickCalendarPaddingObject(mX, mY, calendar))
			}
		}
	}
	
	/**
	 * 获取点击事件处的对象
	 * 
	 * @param x                x
	 * @param y                y
	 * @param adjacentCalendar adjacent calendar
	 * @return obj can as null
	 */
	protected fun getClickCalendarPaddingObject(x: Float,
	                                            y: Float,
	                                            adjacentCalendar: Calendar?): Any? = null
	
	/**
	 * 记录已经选择的日期
	 * 
	 * @param calendar calendar
	 */
	fun setSelectedCalendar(calendar: Calendar) {
		mCurrentItem = mItems!!.indexOf(calendar)
	}
	
	/**
	 * 更新显示模式
	 */
	fun updateShowMode() {
		mLineCount = CalendarUtil.getMonthViewLineCount(mYear, mMonth, mDelegate!!.weekStart, mDelegate!!.monthViewShowMode)
		mHeight = CalendarUtil.getMonthViewHeight(mYear, mMonth, mItemHeight, mDelegate!!.weekStart, mDelegate!!.monthViewShowMode)
		invalidate()
	}
	
	/**
	 * 更新周起始
	 */
	fun updateWeekStart() {
		initCalendar()
		mHeight = CalendarUtil.getMonthViewHeight(mYear, mMonth, mItemHeight, mDelegate!!.weekStart, mDelegate!!.monthViewShowMode)
	}
	
	override fun updateItemHeight() {
		super.updateItemHeight()
		mHeight = CalendarUtil.getMonthViewHeight(mYear, mMonth, mItemHeight, mDelegate!!.weekStart, mDelegate!!.monthViewShowMode)
	}
	
	override fun updateCurrentDate() {
		if (mItems != null) {
			if (mItems!!.contains(mDelegate!!.currentDay)) {
				for (it in mItems) {    //添加操作
					it.isCurrentDay = false
				}
				mItems!![mItems!!.indexOf(mDelegate!!.currentDay)].isCurrentDay = true
			}
			invalidate()
		}
	}
	
	/**
	 * 获取选中的下标
	 * 
	 * @param calendar calendar
	 * @return 获取选中的下标
	 */
	fun getSelectedIndex(calendar: Calendar?): Int? = mItems?.indexOf(calendar)
	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		super.onMeasure(widthMeasureSpec, if (mLineCount != 0) MeasureSpec.makeMeasureSpec(mHeight, MeasureSpec.EXACTLY) else heightMeasureSpec)
	}
	
	/**
	 * 循环绘制开始的回调，不需要可忽略
	 * 绘制每个日历项的循环，用来计算baseLine、圆心坐标等都可以在这里实现
	 * 
	 * @param x 日历Card x起点坐标
	 * @param y 日历Card y起点坐标
	 */
	protected fun onLoopStart(x: Int, y: Int) {
	}
	
	override fun onDestroy() {
	}
}
