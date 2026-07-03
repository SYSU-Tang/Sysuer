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
 * 月视图基础控件,可自由继承实现
 */
abstract class MonthView(context: Context?) : BaseMonthView(context) {
	override fun onDraw(canvas: Canvas) {
		if (mLineCount != 0) {
			mItemWidth = (width - mDelegate.calendarPaddingLeft - mDelegate.calendarPaddingRight) / 7
			onPreviewHook()
			val count = mLineCount * 7
			var d = 0
			(0..<mLineCount).forEach { i ->
				(0..6).forEach { j ->
					val calendar = mItems!![d]
					if (mDelegate.monthViewShowMode == CalendarViewDelegate.MODE_ONLY_CURRENT_MONTH) {
						if (d > mItems!!.size - mNextDiff) {
							return
						}
						if (!calendar.isCurrentMonth) {
							++d
							return@forEach
						}
					}
					else if (mDelegate.monthViewShowMode == CalendarViewDelegate.MODE_FIT_MONTH) {
						if (d >= count) {
							return
						}
					}
					draw(canvas, calendar, i, j, d)
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
	 * @param d        d
	 */
	private fun draw(canvas: Canvas, calendar: Calendar, i: Int, j: Int, d: Int) {
		val x = j * mItemWidth + mDelegate.calendarPaddingLeft
		val y = i * mItemHeight
		onLoopStart(x, y)
		val isSelected = d == mCurrentItem
		if (calendar.hasScheme()) { //标记的日子
			var isDrawSelected = false //是否继续绘制选中的 onDrawScheme
			if (isSelected) isDrawSelected = onDrawSelected(canvas, calendar, x, y, true)
			if (isDrawSelected || !isSelected) { //将画笔设置为标记颜色
				mSchemePaint.setColor(if (calendar.schemeColor != 0) calendar.schemeColor else mDelegate.schemeThemeColor)
				onDrawScheme(canvas, calendar, x, y)
			}
		}
		else if (isSelected) onDrawSelected(canvas, calendar, x, y, false)
		onDrawText(canvas, calendar, x, y, calendar.hasScheme(), isSelected)
	}
	
	override fun onClick(v: View?) {
		if (!isClick) return
		val calendar = index ?: return
		if (mDelegate.monthViewShowMode == CalendarViewDelegate.MODE_ONLY_CURRENT_MONTH && !calendar.isCurrentMonth) return
		
		if (onCalendarIntercept(calendar)) {
			mDelegate.mCalendarInterceptListener.onCalendarInterceptClick(calendar, true)
			return
		}
		
		
		if (!isInRange(calendar)) {
			mDelegate.mCalendarSelectListener?.onCalendarOutOfRange(calendar)
			return
		}
		
		mCurrentItem = mItems!!.indexOf(calendar)
		
		if (!calendar.isCurrentMonth && mMonthViewPager != null) {
			val cur = mMonthViewPager!!.currentItem
			val position = if (mCurrentItem < 7) cur - 1 else cur + 1
			mMonthViewPager!!.currentItem = position
		}
		mDelegate.mInnerListener?.onMonthDateSelected(calendar, true)
		if (mParentLayout != null) {
			if (calendar.isCurrentMonth) {
				mParentLayout!!.updateSelectPosition(mItems!!.indexOf(calendar))
			}
			else {
				mParentLayout!!.updateSelectWeek(CalendarUtil.getWeekFromDayInMonth(calendar, mDelegate.weekStart))
			}
		}
		
		mDelegate.mCalendarSelectListener?.onCalendarSelect(calendar, true)
	}
	
	override fun onLongClick(v: View?): Boolean {
		if (mDelegate.mCalendarLongClickListener == null) return false
		if (!isClick) {
			return false
		}
		val calendar = index ?: return false
		if (mDelegate.monthViewShowMode == CalendarViewDelegate.MODE_ONLY_CURRENT_MONTH && !calendar.isCurrentMonth) {
			return false
		}
		if (onCalendarIntercept(calendar)) {
			mDelegate.mCalendarInterceptListener.onCalendarInterceptClick(calendar, true)
			return false
		}
		val isCalendarInRange = isInRange(calendar)
		if (!isCalendarInRange) {
			mDelegate.mCalendarLongClickListener?.onCalendarLongClickOutOfRange(calendar)
			return true
		}
		
		if (mDelegate.isPreventLongPressedSelected) {
			mDelegate.mCalendarLongClickListener?.onCalendarLongClick(calendar)
			return true
		}
		mCurrentItem = mItems!!.indexOf(calendar)
		if (!calendar.isCurrentMonth && mMonthViewPager != null) {
			val cur = mMonthViewPager!!.currentItem
			val position = if (mCurrentItem < 7) cur - 1 else cur + 1
			mMonthViewPager!!.currentItem = position
		}
		mDelegate.mInnerListener?.onMonthDateSelected(calendar, true)
		if (mParentLayout != null) {
			if (calendar.isCurrentMonth) mParentLayout!!.updateSelectPosition(mItems!!.indexOf(calendar))
			else mParentLayout!!.updateSelectWeek(CalendarUtil.getWeekFromDayInMonth(calendar, mDelegate.weekStart))
		}
		mDelegate.mCalendarSelectListener?.onCalendarSelect(calendar, true)
		mDelegate.mCalendarLongClickListener?.onCalendarLongClick(calendar)
		
		invalidate()
		return true
	}
	
	/**
	 * 绘制选中的日期
	 * 
	 * @param canvas    canvas
	 * @param calendar  日历日历calendar
	 * @param x         日历Card x起点坐标
	 * @param y         日历Card y起点坐标
	 * @param hasScheme hasScheme 非标记的日期
	 * @return 是否绘制onDrawScheme，true or false
	 */
	protected abstract fun onDrawSelected(canvas: Canvas,
	                                      calendar: Calendar,
	                                      x: Int,
	                                      y: Int,
	                                      hasScheme: Boolean): Boolean
	
	/**
	 * 绘制标记的日期,这里可以是背景色，标记色什么的
	 * 
	 * @param canvas   canvas
	 * @param calendar 日历calendar
	 * @param x        日历Card x起点坐标
	 * @param y        日历Card y起点坐标
	 */
	protected abstract fun onDrawScheme(canvas: Canvas, calendar: Calendar, x: Int, y: Int)
	
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
	protected abstract fun onDrawText(canvas: Canvas,
	                                  calendar: Calendar,
	                                  x: Int,
	                                  y: Int,
	                                  hasScheme: Boolean,
	                                  isSelected: Boolean)
}
