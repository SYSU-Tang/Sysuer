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
		if (mItems.isNotEmpty()) {
			mItemWidth =
				(width - mDelegate.calendarPaddingLeft - mDelegate.calendarPaddingRight) / 7
			onPreviewHook()

			(0..6).forEach {
				onLoopStart()
				val calendar = mItems[it]
				val isSelected = isCalendarSelected(calendar)
				val isPreSelected = isSelectPreCalendar(calendar, it)
				val isNextSelected = isSelectNextCalendar(calendar, it)
				if (calendar.hasScheme()) {
					if ((isSelected && onDrawSelected(
								canvas,
								calendar,
								it * mItemWidth + mDelegate.calendarPaddingLeft,
								true,
								isPreSelected,
								isNextSelected
						)) || !isSelected
					) { //将画笔设置为标记颜色
						mSchemePaint.setColor(if (calendar.schemeColor != 0) calendar.schemeColor else mDelegate.schemeThemeColor)
						onDrawScheme(
								canvas,
								calendar,
								it * mItemWidth + mDelegate.calendarPaddingLeft,
								isSelected
						)
					}
				} else if (isSelected) onDrawSelected(
						canvas,
						calendar,
						it * mItemWidth + mDelegate.calendarPaddingLeft,
						false,
						isPreSelected,
						isNextSelected
				)
				onDrawText(
						canvas,
						calendar,
						it * mItemWidth + mDelegate.calendarPaddingLeft,
						calendar.hasScheme(),
						isSelected
				)
			}
		}
	}

	/**
	 * 日历是否被选中
	 * 
	 * @param sysuerCalendar calendar
	 * @return 日历是否被选中
	 */
	protected fun isCalendarSelected(sysuerCalendar: SysuerCalendar): Boolean =
		!onCalendarIntercept(sysuerCalendar) && mDelegate.mSelectedCalendars.containsKey("$sysuerCalendar")

	override fun onClick(v: View?) {
		if (isClick) {
			val calendar = index ?: return
			if (onCalendarIntercept(calendar)) {
				mDelegate.mCalendarInterceptListener?.onCalendarInterceptClick(calendar, true)
				return
			}
			if (!isInRange(calendar)) {
				mDelegate.mCalendarMultiSelectListener?.onCalendarMultiSelectOutOfRange(calendar)
				return
			}
			val key = "$calendar"

			if (mDelegate.mSelectedCalendars.containsKey(key)) mDelegate.mSelectedCalendars.remove(
					key
			)
			else {
				if (mDelegate.mSelectedCalendars.size >= mDelegate.maxMultiSelectSize) {
					mDelegate.mCalendarMultiSelectListener?.onMultiSelectOutOfSize(
							calendar,
							mDelegate.maxMultiSelectSize
					)

					return
				}
				mDelegate.mSelectedCalendars[key] = calendar
			}
			mCurrentItem = mItems.indexOf(calendar)
			mDelegate.mInnerListener?.onWeekDateSelected(calendar, true)
			mParentLayout?.updateSelectWeek(
					CalendarUtil.getWeekFromDayInMonth(
							calendar,
							mDelegate.weekStart
					)
			)
			mDelegate.mCalendarMultiSelectListener?.onCalendarMultiSelect(
					calendar,
					mDelegate.mSelectedCalendars.size,
					mDelegate.maxMultiSelectSize
			)
			invalidate()
		}
	}

	override fun onLongClick(v: View?): Boolean = false

	/**
	 * 上一个日期是否选中
	 * 
	 * @param sysuerCalendar      当前日期
	 * @param calendarIndex 当前位置
	 * @return 上一个日期是否选中
	 */
	protected fun isSelectPreCalendar(sysuerCalendar: SysuerCalendar, calendarIndex: Int): Boolean {
		val preSysuerCalendar: SysuerCalendar
		if (calendarIndex == 0) {
			preSysuerCalendar = CalendarUtil.getPreCalendar(sysuerCalendar)
			mDelegate.updateCalendarScheme(preSysuerCalendar)
		} else {
			preSysuerCalendar = mItems[calendarIndex - 1]
		}
		return isCalendarSelected(preSysuerCalendar)
	}

	/**
	 * 下一个日期是否选中
	 * 
	 * @param sysuerCalendar      当前日期
	 * @param calendarIndex 当前位置
	 * @return 下一个日期是否选中
	 */
	protected fun isSelectNextCalendar(sysuerCalendar: SysuerCalendar, calendarIndex: Int): Boolean {
		val nextSysuerCalendar: SysuerCalendar
		if (calendarIndex == mItems.size - 1) {
			nextSysuerCalendar = CalendarUtil.getNextCalendar(sysuerCalendar)
			mDelegate.updateCalendarScheme(nextSysuerCalendar)
		} else {
			nextSysuerCalendar = mItems[calendarIndex + 1]
		}
		return isCalendarSelected(nextSysuerCalendar)
	}

	/**
	 * 绘制选中的日期
	 * 
	 * @param canvas         canvas
	 * @param sysuerCalendar       日历日历calendar
	 * @param x              日历Card x起点坐标
	 * @param hasScheme      hasScheme 非标记的日期
	 * @param isSelectedPre  上一个日期是否选中
	 * @param isSelectedNext 下一个日期是否选中
	 * @return 是否绘制 onDrawScheme
	 */
	protected abstract fun onDrawSelected(
		canvas: Canvas?,
		sysuerCalendar: SysuerCalendar?,
		x: Int,
		hasScheme: Boolean,
		isSelectedPre: Boolean,
		isSelectedNext: Boolean
	): Boolean

	/**
	 * 绘制标记的日期
	 * 
	 * @param canvas     canvas
	 * @param sysuerCalendar   日历calendar
	 * @param x          日历Card x起点坐标
	 * @param isSelected 是否选中
	 */
	protected abstract fun onDrawScheme(
		canvas: Canvas?, sysuerCalendar: SysuerCalendar?, x: Int, isSelected: Boolean
	)

	/**
	 * 绘制日历文本
	 * 
	 * @param canvas     canvas
	 * @param sysuerCalendar   日历calendar
	 * @param x          日历Card x起点坐标
	 * @param hasScheme  是否是标记的日期
	 * @param isSelected 是否选中
	 */
	protected abstract fun onDrawText(
		canvas: Canvas?, sysuerCalendar: SysuerCalendar?, x: Int, hasScheme: Boolean, isSelected: Boolean
	)
}
