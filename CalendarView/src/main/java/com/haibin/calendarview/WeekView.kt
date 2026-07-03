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
 * 周视图，因为日历UI采用热插拔实现，所以这里必须继承实现，达到UI一致即可
 * Created by huanghaibin on 2017/11/21.
 */
abstract class WeekView(context: Context) : BaseWeekView(context) {
	/**
	 * 绘制日历文本
	 * 
	 * @param canvas canvas
	 */
	override fun onDraw(canvas: Canvas) {
		if (mItems!!.isNotEmpty()) {
			mItemWidth = (width - mDelegate!!.calendarPaddingLeft - mDelegate!!.calendarPaddingRight) / 7
			onPreviewHook()
			
			(mItems as Iterable<Any?>).forEachIndexed { i, calendar ->
				val x = i * mItemWidth + mDelegate!!.calendarPaddingLeft
				onLoopStart()
				val isSelected = i == mCurrentItem
				val hasScheme = (calendar as Calendar).hasScheme()
				if (hasScheme) {
					val isDrawSelected = //是否继续绘制选中的onDrawScheme
						if (isSelected) onDrawSelected(canvas, calendar, x, true) else false
					if (isDrawSelected || !isSelected) { //将画笔设置为标记颜色
						mSchemePaint.setColor(if (calendar.schemeColor != 0) calendar.schemeColor else mDelegate!!.schemeThemeColor)
						onDrawScheme(canvas, calendar, x)
					}
				}
				else if (isSelected) onDrawSelected(canvas, calendar, x, false)
				onDrawText(canvas, calendar, x, hasScheme, isSelected)
			}
		}
	}
	
	override fun onClick(v: View?) {
		if (isClick) {
			val calendar = index ?: return
			if (onCalendarIntercept(calendar)) {
				mDelegate!!.mCalendarInterceptListener.onCalendarInterceptClick(calendar, true)
				return
			}
			if (!isInRange(calendar)) {
				mDelegate!!.mCalendarSelectListener?.onCalendarOutOfRange(calendar)
				return
			}
			mCurrentItem = mItems!!.indexOf(calendar)
			mDelegate!!.mInnerListener?.onWeekDateSelected(calendar, true)
			mParentLayout?.updateSelectWeek(CalendarUtil.getWeekFromDayInMonth(calendar, mDelegate!!.weekStart))
			
			
			mDelegate!!.mCalendarSelectListener?.onCalendarSelect(calendar, true)
			
			invalidate()
		}
	}
	
	override fun onLongClick(v: View?): Boolean {
		if (mDelegate!!.mCalendarLongClickListener == null) return false
		if (!isClick) {
			return false
		}
		val calendar = index ?: return false
		if (onCalendarIntercept(calendar)) {
			mDelegate!!.mCalendarInterceptListener.onCalendarInterceptClick(calendar, true)
			return true
		}
		if (!isInRange(calendar)) {
			mDelegate!!.mCalendarLongClickListener?.onCalendarLongClickOutOfRange(calendar)
			return true
		}
		
		if (mDelegate!!.isPreventLongPressedSelected) { //如果启用拦截长按事件不选择日期
			mDelegate!!.mCalendarLongClickListener?.onCalendarLongClick(calendar)
			return true
		}
		
		mCurrentItem = mItems!!.indexOf(calendar)
		mDelegate!!.mIndexCalendar = mDelegate!!.mSelectedCalendar
		mDelegate!!.mInnerListener?.onWeekDateSelected(calendar, true)
		mParentLayout?.updateSelectWeek(CalendarUtil.getWeekFromDayInMonth(calendar, mDelegate!!.weekStart))
		mDelegate!!.mCalendarSelectListener?.onCalendarSelect(calendar, true)
		mDelegate!!.mCalendarLongClickListener?.onCalendarLongClick(calendar)
		
		invalidate()
		return true
	}
	
	/**
	 * 绘制选中的日期
	 * 
	 * @param canvas    canvas
	 * @param calendar  日历日历calendar
	 * @param x         日历Card x起点坐标
	 * @param hasScheme hasScheme 非标记的日期
	 * @return 是否绘制 onDrawScheme
	 */
	protected abstract fun onDrawSelected(canvas: Canvas,
	                                      calendar: Calendar,
	                                      x: Int,
	                                      hasScheme: Boolean): Boolean
	
	/**
	 * 绘制标记的日期
	 * 
	 * @param canvas   canvas
	 * @param calendar 日历calendar
	 * @param x        日历Card x起点坐标
	 */
	protected abstract fun onDrawScheme(canvas: Canvas, calendar: Calendar, x: Int)
	
	/**
	 * 绘制日历文本
	 * 
	 * @param canvas     canvas
	 * @param calendar   日历calendar
	 * @param x          日历Card x起点坐标
	 * @param hasScheme  是否是标记的日期
	 * @param isSelected 是否选中
	 */
	protected abstract fun onDrawText(canvas: Canvas,
	                                  calendar: Calendar,
	                                  x: Int,
	                                  hasScheme: Boolean,
	                                  isSelected: Boolean)
}
