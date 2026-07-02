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
import android.graphics.Paint

/**
 * 默认高仿魅族周视图
 */
class DefaultWeekView(context: Context) : WeekView(context) {
	private val mTextPaint = Paint().apply {
		
		textSize = CalendarUtil.dipToPx(context, 8f).toFloat()
		setColor(-0x1)
		isAntiAlias = true
		isFakeBoldText = true
	}
	private val mSchemeBasicPaint = Paint().apply {
		isAntiAlias = true
		style = Paint.Style.FILL
		textAlign = Paint.Align.CENTER
		setColor(-0x12acad)
		isFakeBoldText = true
	}
	private val mRadio: Float = CalendarUtil.dipToPx(getContext(), 7f).toFloat()
	private val mPadding: Int = CalendarUtil.dipToPx(getContext(), 4f)
	private val mSchemeBaseLine: Float
	
	init {
		val metrics = mSchemeBasicPaint.getFontMetrics()
		mSchemeBaseLine = mRadio - metrics.descent + (metrics.bottom - metrics.top) / 2 + CalendarUtil.dipToPx(getContext(), 1f)
	}
	
	/**
	 * 如果需要点击Scheme没有效果，则return true
	 * 
	 * @param canvas    canvas
	 * @param calendar  FullCalendar
	 * @param x         日历Card x起点坐标
	 * @param hasScheme hasScheme 非标记的日期
	 * @return true 则绘制onDrawScheme，因为这里背景色不是是互斥的
	 */
	override fun onDrawSelected(canvas: Canvas,
	                            calendar: Calendar,
	                            x: Int,
	                            hasScheme: Boolean): Boolean {
		mSelectedPaint.style = Paint.Style.FILL
		canvas.drawRect((x + mPadding).toFloat(), mPadding.toFloat(), (x + mItemWidth - mPadding).toFloat(), (mItemHeight - mPadding).toFloat(), mSelectedPaint)
		return true
	}
	
	override fun onDrawScheme(canvas: Canvas, calendar: Calendar, x: Int) {
		mSchemeBasicPaint.setColor(calendar.schemeColor)
		canvas.drawCircle(x + mItemWidth - mPadding - mRadio / 2, mPadding + mRadio, mRadio, mSchemeBasicPaint)
		canvas.drawText(calendar.scheme, x + mItemWidth - mPadding - mRadio / 2 - getTextWidth(calendar.scheme) / 2, mPadding + mSchemeBaseLine, mTextPaint)
	}
	
	/**
	 * 获取字体的宽
	 * @param text text
	 * @return return
	 */
	private fun getTextWidth(text: String?): Float {
		return mTextPaint.measureText(text)
	}
	
	override fun onDrawText(canvas: Canvas,
	                        calendar: Calendar,
	                        x: Int,
	                        hasScheme: Boolean,
	                        isSelected: Boolean) {
		val cx = (x + mItemWidth / 2).toFloat()
		val top = -mItemHeight / 6
		when {
			isSelected -> {
				canvas.drawText(calendar.day.toString(), cx, mTextBaseLine + top, mSelectTextPaint)
				canvas.drawText(calendar.lunar, cx, mTextBaseLine + mItemHeight / 10, mSelectedLunarTextPaint)
			}
			hasScheme -> {
				canvas.drawText(calendar.day.toString(), cx, mTextBaseLine + top, if (calendar.isCurrentDay) mCurDayTextPaint else if (calendar.isCurrentMonth) mSchemeTextPaint else mOtherMonthTextPaint)
				canvas.drawText(calendar.lunar, cx, mTextBaseLine + mItemHeight / 10, if (calendar.isCurrentDay) mCurDayLunarTextPaint else mSchemeLunarTextPaint)
			}
			else -> {
				canvas.drawText(calendar.day.toString(), cx, mTextBaseLine + top, if (calendar.isCurrentDay) mCurDayTextPaint else if (calendar.isCurrentMonth) mCurMonthTextPaint else mOtherMonthTextPaint)
				canvas.drawText(calendar.lunar, cx, mTextBaseLine + mItemHeight / 10, if (calendar.isCurrentDay) mCurDayLunarTextPaint else if (calendar.isCurrentMonth) mCurMonthLunarTextPaint else mOtherMonthLunarTextPaint)
			}
		}
	}
}
