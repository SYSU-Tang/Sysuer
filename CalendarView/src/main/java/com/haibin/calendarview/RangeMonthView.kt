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
import android.graphics.Canvas
import android.view.View

/**
 * 范围选择月视图
 * Created by huanghaibin on 2018/9/11.
 */
abstract class RangeMonthView(context: Context?) : BaseMonthView(context) {
	override fun onDraw(canvas: Canvas) {
		if (mLineCount != 0) {
			mItemWidth = (width - mDelegate.calendarPaddingLeft - mDelegate.calendarPaddingRight) / 7
			onPreviewHook()
			val count = mLineCount * 7
			var d = 0
			(0..<mLineCount).forEach { i ->
				(0..6).forEach { j ->
					val calendar = mItems[d]
					if (mDelegate.monthViewShowMode == CalendarViewDelegate.MODE_ONLY_CURRENT_MONTH) {
						if (d > mItems.size - mNextDiff) return
						if (!calendar.isCurrentMonth) {
							++d
							return@forEach
						}
					}
					else if (mDelegate.monthViewShowMode == CalendarViewDelegate.MODE_FIT_MONTH && d >= count) return
					draw(canvas, calendar, d, i, j)
					++d
				}
			}
		}
	}
	
	/**
	 * 开始绘制
	 * 
	 * @param canvas   canvas
	 * @param calendar 对应日历
	 * @param i        i
	 * @param j        j
	 */
	private fun draw(canvas: Canvas?, calendar: Calendar, calendarIndex: Int, i: Int, j: Int) {
		val x = j * mItemWidth + mDelegate.calendarPaddingLeft
		val y = i * mItemHeight
		onLoopStart(x, y)
		val isSelected = isCalendarSelected(calendar)
		val isPreSelected = isSelectPreCalendar(calendar, calendarIndex)
		val isNextSelected = isSelectNextCalendar(calendar, calendarIndex)
		if (calendar.hasScheme()) { //标记的日子
			val isDrawSelected = if (isSelected) onDrawSelected(canvas, calendar, x, y, true, isPreSelected, isNextSelected) else false            // 是否继续绘制选中的onDrawScheme
			if (isDrawSelected || !isSelected) { //将画笔设置为标记颜色
				mSchemePaint.setColor(if (calendar.schemeColor != 0) calendar.schemeColor else mDelegate.schemeThemeColor)
				onDrawScheme(canvas, calendar, x, y, true)
			}
		}
		else if (isSelected) onDrawSelected(canvas, calendar, x, y, false, isPreSelected, isNextSelected)
		onDrawText(canvas, calendar, x, y, calendar.hasScheme(), isSelected)
	}
	
	/**
	 * 日历是否被选中
	 * 
	 * @param calendar calendar
	 * @return 日历是否被选中
	 */
	protected fun isCalendarSelected(calendar: Calendar): Boolean {
		return when {
			mDelegate.mSelectedStartRangeCalendar == null || onCalendarIntercept(calendar) -> false
			mDelegate.mSelectedEndRangeCalendar == null -> calendar.compareTo(mDelegate.mSelectedStartRangeCalendar) == 0
			else -> calendar >= mDelegate.mSelectedStartRangeCalendar && calendar <= mDelegate.mSelectedEndRangeCalendar
		}
	}
	
	override fun onClick(v: View?) {
		if (isClick) {
			val calendar = index ?: return
			if (mDelegate.monthViewShowMode != CalendarViewDelegate.MODE_ONLY_CURRENT_MONTH || calendar.isCurrentMonth) {
				if (!onCalendarIntercept(calendar)) {
					if (!isInRange(calendar)) {
						mDelegate.mCalendarRangeSelectListener?.onCalendarSelectOutOfRange(calendar)
						
						return
					} //优先判断各种直接return的情况，减少代码深度
					if (mDelegate.mSelectedStartRangeCalendar != null && mDelegate.mSelectedEndRangeCalendar == null) {
						val minDiffer = CalendarUtil.differ(calendar, mDelegate.mSelectedStartRangeCalendar)
						if (minDiffer >= 0 && mDelegate.minSelectRange != -1 && mDelegate.minSelectRange > minDiffer + 1) {
							mDelegate.mCalendarRangeSelectListener?.onSelectOutOfRange(calendar, true)
							return
						}
						else if (mDelegate.maxSelectRange != -1 && mDelegate.maxSelectRange < CalendarUtil.differ(calendar, mDelegate.mSelectedStartRangeCalendar) + 1) {
							mDelegate.mCalendarRangeSelectListener?.onSelectOutOfRange(calendar, false)
							return
						}
					}
					
					if (mDelegate.mSelectedStartRangeCalendar == null || mDelegate.mSelectedEndRangeCalendar != null) {
						mDelegate.mSelectedStartRangeCalendar = calendar
						mDelegate.mSelectedEndRangeCalendar = null
					}
					else {
						val compare = calendar.compareTo(mDelegate.mSelectedStartRangeCalendar)
						if (mDelegate.minSelectRange == -1 && compare <= 0) {
							mDelegate.mSelectedStartRangeCalendar = calendar
							mDelegate.mSelectedEndRangeCalendar = null
						}
						else if (compare < 0) {
							mDelegate.mSelectedStartRangeCalendar = calendar
							mDelegate.mSelectedEndRangeCalendar = null
						}
						else if (compare == 0 && mDelegate.minSelectRange == 1) mDelegate.mSelectedEndRangeCalendar = calendar
						else mDelegate.mSelectedEndRangeCalendar = calendar
					}
					
					mCurrentItem = mItems.indexOf(calendar)
					
					if (!calendar.isCurrentMonth && mMonthViewPager != null) {
						val cur = mMonthViewPager!!.currentItem
						val position = if (mCurrentItem < 7) cur - 1 else cur + 1
						mMonthViewPager!!.currentItem = position
					}
					
					mDelegate.mInnerListener?.onMonthDateSelected(calendar, true)
					
					
					if (mParentLayout != null) {
						if (calendar.isCurrentMonth) mParentLayout.updateSelectPosition(mItems.indexOf(calendar))
						else mParentLayout.updateSelectWeek(CalendarUtil.getWeekFromDayInMonth(calendar, mDelegate.weekStart))
					}
					mDelegate.mCalendarRangeSelectListener?.onCalendarRangeSelect(calendar, mDelegate.mSelectedEndRangeCalendar != null)
					return
				}
				mDelegate.mCalendarInterceptListener.onCalendarInterceptClick(calendar, true)
			}
		}
	}
	
	override fun onLongClick(v: View?): Boolean = false
	
	/**
	 * 上一个日期是否选中
	 * 
	 * @param calendar 当前日期
	 * @param calendarIndex 当前位置
	 * @return 上一个日期是否选中
	 */
	protected fun isSelectPreCalendar(calendar: Calendar, calendarIndex: Int): Boolean {
		val preCalendar: Calendar
		if (calendarIndex == 0) {
			preCalendar = CalendarUtil.getPreCalendar(calendar)
			mDelegate.updateCalendarScheme(preCalendar)
		}
		else preCalendar = mItems[calendarIndex - 1]
		
		return mDelegate.mSelectedStartRangeCalendar != null && isCalendarSelected(preCalendar)
	}
	
	/**
	 * 下一个日期是否选中
	 * 
	 * @param calendar 当前日期
	 * @param calendarIndex 当前位置
	 * @return 下一个日期是否选中
	 */
	protected fun isSelectNextCalendar(calendar: Calendar, calendarIndex: Int): Boolean {
		val nextCalendar: Calendar
		if (calendarIndex == mItems.size - 1) {
			nextCalendar = CalendarUtil.getNextCalendar(calendar)
			mDelegate.updateCalendarScheme(nextCalendar)
		}
		else nextCalendar = mItems[calendarIndex + 1]
		
		return mDelegate.mSelectedStartRangeCalendar != null && isCalendarSelected(nextCalendar)
	}
	
	/**
	 * 绘制选中的日期
	 * 
	 * @param canvas         canvas
	 * @param calendar       日历日历calendar
	 * @param x              日历Card x起点坐标
	 * @param y              日历Card y起点坐标
	 * @param hasScheme      hasScheme 非标记的日期
	 * @param isSelectedPre  上一个日期是否选中
	 * @param isSelectedNext 下一个日期是否选中
	 * @return 是否继续绘制onDrawScheme，true or false
	 */
	protected abstract fun onDrawSelected(canvas: Canvas?,
	                                      calendar: Calendar?,
	                                      x: Int,
	                                      y: Int,
	                                      hasScheme: Boolean,
	                                      isSelectedPre: Boolean,
	                                      isSelectedNext: Boolean): Boolean
	
	/**
	 * 绘制标记的日期,这里可以是背景色，标记色什么的
	 * 
	 * @param canvas     canvas
	 * @param calendar   日历calendar
	 * @param x          日历Card x起点坐标
	 * @param y          日历Card y起点坐标
	 * @param isSelected 是否选中
	 */
	protected abstract fun onDrawScheme(canvas: Canvas?,
	                                    calendar: Calendar?,
	                                    x: Int,
	                                    y: Int,
	                                    isSelected: Boolean)
	
	/**
	 * 绘制日历文本
	 * 
	 * @param canvas     canvas
	 * @param calendar   日历calendar
	 * @param x          日历Card x起点坐标
	 * @param y          日历Card y起点坐标
	 * @param hasScheme  是否是标记的日期
	 * @param isSelected 是否选中
	 */
	protected abstract fun onDrawText(canvas: Canvas?,
	                                  calendar: Calendar?,
	                                  x: Int,
	                                  y: Int,
	                                  hasScheme: Boolean,
	                                  isSelected: Boolean)
}
