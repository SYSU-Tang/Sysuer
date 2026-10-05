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
 * 默认高仿魅族日历布局
 */
class DefaultMonthView(context: Context) : MonthView(context) {
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
	private val mRadio: Float = CalendarUtil.dipToPx(context, 7f).toFloat()
	private val mPadding: Int = CalendarUtil.dipToPx(context, 4f)
	private val mSchemeBaseLine: Float
	
	init {
		val metrics = mSchemeBasicPaint.getFontMetrics()
		mSchemeBaseLine = mRadio - metrics.descent + (metrics.bottom - metrics.top) / 2 + CalendarUtil.dipToPx(context, 1f)
	}
	
	/**
	 * @param canvas    canvas
	 * @param sysuerCalendar  FullCalendar
	 * @param x         日历Card x起点坐标
	 * @param y         日历Card y起点坐标
	 * @param hasScheme hasScheme 非标记的日期
	 * @return true 则绘制onDrawScheme，因为这里背景色不是是互斥的
	 */
	override fun onDrawSelected(canvas: Canvas,
	                            sysuerCalendar: SysuerCalendar,
	                            x: Int,
	                            y: Int,
	                            hasScheme: Boolean): Boolean {
		mSelectedPaint.style = Paint.Style.FILL
		canvas.drawRect((x + mPadding).toFloat(), (y + mPadding).toFloat(), (x + mItemWidth - mPadding).toFloat(), (y + mItemHeight - mPadding).toFloat(), mSelectedPaint)
		return true
	}
	
	override fun onDrawScheme(canvas: Canvas, sysuerCalendar: SysuerCalendar, x: Int, y: Int) {
		mSchemeBasicPaint.setColor(sysuerCalendar.schemeColor)
		canvas.drawCircle(x + mItemWidth - mPadding - mRadio / 2, y + mPadding + mRadio, mRadio, mSchemeBasicPaint)
		canvas.drawText(sysuerCalendar.scheme, x + mItemWidth - mPadding - mRadio / 2 - getTextWidth(sysuerCalendar.scheme) / 2, y + mPadding + mSchemeBaseLine, mTextPaint)
	}
	
	/**
	 * 获取字体的宽
	 * @param text text
	 * @return return
	 */
	private fun getTextWidth(text: String?): Float = mTextPaint.measureText(text)
	override fun onDrawText(canvas: Canvas,
	                        sysuerCalendar: SysuerCalendar,
	                        x: Int,
	                        y: Int,
	                        hasScheme: Boolean,
	                        isSelected: Boolean) {
		val cx = x + mItemWidth / 2
		val top = y - mItemHeight / 6
		when {
			isSelected -> {
				canvas.drawText(sysuerCalendar.day.toString(), cx.toFloat(), mTextBaseLine + top, mSelectTextPaint)
				canvas.drawText(sysuerCalendar.lunar, cx.toFloat(), mTextBaseLine + y + mItemHeight / 10, mSelectedLunarTextPaint)
			}
			hasScheme -> {
				canvas.drawText(sysuerCalendar.day.toString(), cx.toFloat(), mTextBaseLine + top, if (sysuerCalendar.isCurrentDay) mCurDayTextPaint else if (sysuerCalendar.isCurrentMonth) mSchemeTextPaint else mOtherMonthTextPaint)
				canvas.drawText(sysuerCalendar.lunar, cx.toFloat(), mTextBaseLine + y + mItemHeight / 10, if (sysuerCalendar.isCurrentDay) mCurDayLunarTextPaint else mSchemeLunarTextPaint)
			}
			else -> {
				canvas.drawText(sysuerCalendar.day.toString(), cx.toFloat(), mTextBaseLine + top, if (sysuerCalendar.isCurrentDay) mCurDayTextPaint else if (sysuerCalendar.isCurrentMonth) mCurMonthTextPaint else mOtherMonthTextPaint)
				canvas.drawText(sysuerCalendar.lunar, cx.toFloat(), mTextBaseLine + y + mItemHeight / 10, if (sysuerCalendar.isCurrentDay) mCurDayLunarTextPaint else if (sysuerCalendar.isCurrentMonth) mCurMonthLunarTextPaint else mOtherMonthLunarTextPaint)
			}
		}
	}
	override fun onDestroy() {
	}
}
