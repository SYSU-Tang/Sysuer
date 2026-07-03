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
 * 多选周视图
 * Created by huanghaibin on 2018/9/11.
 */
abstract class MultiWeekView(context: Context) : BaseWeekView(context) {
	/**
	 * 绘制日历文本
	 * 
	 * @param canvas canvas
	 */
	override fun onDraw(canvas: Canvas) {
		if (mItems!!.isNotEmpty()) {
			mItemWidth = (width - mDelegate.calendarPaddingLeft - mDelegate.calendarPaddingRight) / 7
			onPreviewHook()
			
			(0..6).forEach {
				onLoopStart()
				val calendar = mItems!![it]
				val isSelected = isCalendarSelected(calendar)
				val isPreSelected = isSelectPreCalendar(calendar, it)
				val isNextSelected = isSelectNextCalendar(calendar, it)
				if (calendar.hasScheme()) {
					if ((if (isSelected) onDrawSelected(canvas, calendar, it * mItemWidth + mDelegate.calendarPaddingLeft, true, isPreSelected, isNextSelected) else false) || !isSelected) { //将画笔设置为标记颜色
						mSchemePaint.setColor(if (calendar.schemeColor != 0) calendar.schemeColor else mDelegate.schemeThemeColor)
						onDrawScheme(canvas, calendar, it * mItemWidth + mDelegate.calendarPaddingLeft, isSelected)
					}
				}
				else if (isSelected) onDrawSelected(canvas, calendar, it * mItemWidth + mDelegate.calendarPaddingLeft, false, isPreSelected, isNextSelected)
				onDrawText(canvas, calendar, it * mItemWidth + mDelegate.calendarPaddingLeft, calendar.hasScheme(), isSelected)
			}
		}
	}
	
	/**
	 * 日历是否被选中
	 * 
	 * @param calendar calendar
	 * @return 日历是否被选中
	 */
	protected fun isCalendarSelected(calendar: Calendar): Boolean =
		!onCalendarIntercept(calendar) && mDelegate.mSelectedCalendars.containsKey("$calendar")
	
	override fun onClick(v: View?) {
		if (isClick) {
			val calendar = index ?: return
			if (onCalendarIntercept(calendar)) {
				mDelegate.mCalendarInterceptListener.onCalendarInterceptClick(calendar, true)
				return
			}
			if (!isInRange(calendar)) {
				mDelegate.mCalendarMultiSelectListener?.onCalendarMultiSelectOutOfRange(calendar)
				return
			}
			val key = "$calendar"
			
			if (mDelegate.mSelectedCalendars.containsKey(key)) mDelegate.mSelectedCalendars.remove(key)
			else {
				if (mDelegate.mSelectedCalendars.size >= mDelegate.maxMultiSelectSize) {
					if (mDelegate.mCalendarMultiSelectListener != null) {
						mDelegate.mCalendarMultiSelectListener.onMultiSelectOutOfSize(calendar, mDelegate.maxMultiSelectSize)
					}
					return
				}
				mDelegate.mSelectedCalendars[key] = calendar
			}
			mCurrentItem = mItems!!.indexOf(calendar)
			mDelegate.mInnerListener?.onWeekDateSelected(calendar, true)
			mParentLayout?.updateSelectWeek(CalendarUtil.getWeekFromDayInMonth(calendar, mDelegate.weekStart))
			mDelegate.mCalendarMultiSelectListener?.onCalendarMultiSelect(calendar, mDelegate.mSelectedCalendars.size, mDelegate.maxMultiSelectSize)
			invalidate()
		}
	}
	
	override fun onLongClick(v: View?): Boolean = false
	
	/**
	 * 上一个日期是否选中
	 * 
	 * @param calendar      当前日期
	 * @param calendarIndex 当前位置
	 * @return 上一个日期是否选中
	 */
	protected fun isSelectPreCalendar(calendar: Calendar, calendarIndex: Int): Boolean {
		val preCalendar: Calendar
		if (calendarIndex == 0) {
			preCalendar = CalendarUtil.getPreCalendar(calendar)
			mDelegate.updateCalendarScheme(preCalendar)
		}
		else {
			preCalendar = mItems!![calendarIndex - 1]
		}
		return isCalendarSelected(preCalendar)
	}
	
	/**
	 * 下一个日期是否选中
	 * 
	 * @param calendar      当前日期
	 * @param calendarIndex 当前位置
	 * @return 下一个日期是否选中
	 */
	protected fun isSelectNextCalendar(calendar: Calendar, calendarIndex: Int): Boolean {
		val nextCalendar: Calendar
		if (calendarIndex == mItems!!.size - 1) {
			nextCalendar = CalendarUtil.getNextCalendar(calendar)
			mDelegate.updateCalendarScheme(nextCalendar)
		}
		else {
			nextCalendar = mItems!![calendarIndex + 1]
		}
		return isCalendarSelected(nextCalendar)
	}
	
	/**
	 * 绘制选中的日期
	 * 
	 * @param canvas         canvas
	 * @param calendar       日历日历calendar
	 * @param x              日历Card x起点坐标
	 * @param hasScheme      hasScheme 非标记的日期
	 * @param isSelectedPre  上一个日期是否选中
	 * @param isSelectedNext 下一个日期是否选中
	 * @return 是否绘制 onDrawScheme
	 */
	protected abstract fun onDrawSelected(canvas: Canvas?,
	                                      calendar: Calendar?,
	                                      x: Int,
	                                      hasScheme: Boolean,
	                                      isSelectedPre: Boolean,
	                                      isSelectedNext: Boolean): Boolean
	
	/**
	 * 绘制标记的日期
	 * 
	 * @param canvas     canvas
	 * @param calendar   日历calendar
	 * @param x          日历Card x起点坐标
	 * @param isSelected 是否选中
	 */
	protected abstract fun onDrawScheme(canvas: Canvas?,
	                                    calendar: Calendar?,
	                                    x: Int,
	                                    isSelected: Boolean)
	
	/**
	 * 绘制日历文本
	 * 
	 * @param canvas     canvas
	 * @param calendar   日历calendar
	 * @param x          日历Card x起点坐标
	 * @param hasScheme  是否是标记的日期
	 * @param isSelected 是否选中
	 */
	protected abstract fun onDrawText(canvas: Canvas?,
	                                  calendar: Calendar?,
	                                  x: Int,
	                                  hasScheme: Boolean,
	                                  isSelected: Boolean)
}
