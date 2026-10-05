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
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import com.haibin.calendarview.CalendarUtil.getMonthEndDiff
import com.haibin.calendarview.CalendarUtil.getMonthViewStartDiff
import com.haibin.calendarview.CalendarUtil.initCalendarForMonthView
import kotlin.math.max

/**
 * 年视图
 * Created by huanghaibin on 2018/10/9.
 */
@Suppress("unused")
abstract class YearView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
	View(context, attrs) {
	/**
	 * 当前月份日期的笔
	 */
	protected val mCurMonthTextPaint: Paint = Paint()

	/**
	 * 其它月份日期颜色
	 */
	protected val mOtherMonthTextPaint: Paint = Paint()

	/**
	 * 当前月份农历文本颜色
	 */
	protected val mCurMonthLunarTextPaint: Paint = Paint()

	/**
	 * 当前月份农历文本颜色
	 */
	protected val mSelectedLunarTextPaint: Paint = Paint()

	/**
	 * 其它月份农历文本颜色
	 */
	protected val mOtherMonthLunarTextPaint: Paint = Paint()

	/**
	 * 其它月份农历文本颜色
	 */
	protected val mSchemeLunarTextPaint: Paint = Paint()

	/**
	 * 标记的日期背景颜色画笔
	 */
	protected val mSchemePaint: Paint = Paint()

	/**
	 * 被选择的日期背景色
	 */
	protected val mSelectedPaint: Paint = Paint()

	/**
	 * 标记的文本画笔
	 */
	protected val mSchemeTextPaint: Paint = Paint()

	/**
	 * 选中的文本画笔
	 */
	protected val mSelectTextPaint: Paint = Paint()

	/**
	 * 当前日期文本颜色画笔
	 */
	protected val mCurDayTextPaint: Paint = Paint()

	/**
	 * 当前日期文本颜色画笔
	 */
	protected val mCurDayLunarTextPaint: Paint = Paint()

	/**
	 * 月份画笔
	 */
	protected val mMonthTextPaint: Paint = Paint()

	/**
	 * 周栏画笔
	 */
	protected val mWeekTextPaint: Paint = Paint()

	/**
	 * 每一项的高度
	 */
	protected var mItemHeight: Int = 0

	/**
	 * 每一项的宽度
	 */
	protected var mItemWidth: Int = 0

	/**
	 * Text的基线
	 */
	protected var mTextBaseLine: Float = 0f

	/**
	 * Text的基线
	 */
	protected var mMonthTextBaseLine: Float = 0f

	/**
	 * Text的基线
	 */
	protected var mWeekTextBaseLine: Float = 0f

	/**
	 * 当前日历卡年份
	 */
	protected var mYear: Int = 0

	/**
	 * 当前日历卡月份
	 */
	protected var mMonth: Int = 0

	/**
	 * 下个月偏移的数量
	 */
	protected var mNextDiff: Int = 0

	/**
	 * 周起始
	 */
	protected var mWeekStart: Int = 0

	/**
	 * 日历的行数
	 */
	protected var mLineCount: Int = 0
	var mDelegate: CalendarViewDelegate? = null

	/**
	 * 日历项
	 */
	var mItems: MutableList<SysuerCalendar>? = null

	init {
		initPaint()
	}

	/**
	 * 初始化配置
	 */
	private fun initPaint() {
		mCurMonthTextPaint.isAntiAlias = true
		mCurMonthTextPaint.textAlign = Paint.Align.CENTER
		mCurMonthTextPaint.setColor(-0xeeeeef)
		mCurMonthTextPaint.isFakeBoldText = true

		mOtherMonthTextPaint.isAntiAlias = true
		mOtherMonthTextPaint.textAlign = Paint.Align.CENTER
		mOtherMonthTextPaint.setColor(-0x1e1e1f)
		mOtherMonthTextPaint.isFakeBoldText = true

		mCurMonthLunarTextPaint.isAntiAlias = true
		mCurMonthLunarTextPaint.textAlign = Paint.Align.CENTER

		mSelectedLunarTextPaint.isAntiAlias = true
		mSelectedLunarTextPaint.textAlign = Paint.Align.CENTER

		mOtherMonthLunarTextPaint.isAntiAlias = true
		mOtherMonthLunarTextPaint.textAlign = Paint.Align.CENTER

		mMonthTextPaint.isAntiAlias = true
		mMonthTextPaint.isFakeBoldText = true

		mWeekTextPaint.isAntiAlias = true
		mWeekTextPaint.isFakeBoldText = true
		mWeekTextPaint.textAlign = Paint.Align.CENTER

		mSchemeLunarTextPaint.isAntiAlias = true
		mSchemeLunarTextPaint.textAlign = Paint.Align.CENTER

		mSchemeTextPaint.isAntiAlias = true
		mSchemeTextPaint.style = Paint.Style.FILL
		mSchemeTextPaint.textAlign = Paint.Align.CENTER
		mSchemeTextPaint.setColor(-0x12acad)
		mSchemeTextPaint.isFakeBoldText = true

		mSelectTextPaint.isAntiAlias = true
		mSelectTextPaint.style = Paint.Style.FILL
		mSelectTextPaint.textAlign = Paint.Align.CENTER
		mSelectTextPaint.setColor(-0x12acad)
		mSelectTextPaint.isFakeBoldText = true

		mSchemePaint.isAntiAlias = true
		mSchemePaint.style = Paint.Style.FILL
		mSchemePaint.strokeWidth = 2f
		mSchemePaint.setColor(-0x101011)

		mCurDayTextPaint.isAntiAlias = true
		mCurDayTextPaint.textAlign = Paint.Align.CENTER
		mCurDayTextPaint.setColor(Color.RED)
		mCurDayTextPaint.isFakeBoldText = true

		mCurDayLunarTextPaint.isAntiAlias = true
		mCurDayLunarTextPaint.textAlign = Paint.Align.CENTER
		mCurDayLunarTextPaint.setColor(Color.RED)
		mCurDayLunarTextPaint.isFakeBoldText = true

		mSelectedPaint.isAntiAlias = true
		mSelectedPaint.style = Paint.Style.FILL
		mSelectedPaint.strokeWidth = 2f
	}

	/**
	 * 设置
	 * 
	 * @param delegate delegate
	 */
	fun setup(delegate: CalendarViewDelegate?) {
		mDelegate = delegate
		updateStyle()
	}

	fun updateStyle() {
		if (mDelegate == null) {
			return
		}
		mCurMonthTextPaint.textSize = mDelegate!!.yearViewDayTextSize.toFloat()
		mSchemeTextPaint.textSize = mDelegate!!.yearViewDayTextSize.toFloat()
		mOtherMonthTextPaint.textSize = mDelegate!!.yearViewDayTextSize.toFloat()
		mCurDayTextPaint.textSize = mDelegate!!.yearViewDayTextSize.toFloat()
		mSelectTextPaint.textSize = mDelegate!!.yearViewDayTextSize.toFloat()

		mSchemeTextPaint.setColor(mDelegate!!.yearViewSchemeTextColor)
		mCurMonthTextPaint.setColor(mDelegate!!.yearViewDayTextColor)
		mOtherMonthTextPaint.setColor(mDelegate!!.yearViewDayTextColor)
		mCurDayTextPaint.setColor(mDelegate!!.yearViewCurDayTextColor)
		mSelectTextPaint.setColor(mDelegate!!.yearViewSelectTextColor)
		mMonthTextPaint.textSize = mDelegate!!.yearViewMonthTextSize.toFloat()
		mMonthTextPaint.setColor(mDelegate!!.yearViewMonthTextColor)
		mWeekTextPaint.setColor(mDelegate!!.yearViewWeekTextColor)
		mWeekTextPaint.textSize = mDelegate!!.yearViewWeekTextSize.toFloat()
	}

	/**
	 * 初始化年视图
	 * 
	 * @param year  year
	 * @param month month
	 */
	fun init(year: Int, month: Int) {
		mYear = year
		mMonth = month
		mNextDiff = getMonthEndDiff(mYear, mMonth, mDelegate!!.weekStart)
		val preDiff = getMonthViewStartDiff(mYear, mMonth, mDelegate!!.weekStart)

		mItems =
			initCalendarForMonthView(mYear, mMonth, mDelegate!!.currentDay, mDelegate!!.weekStart)

		mLineCount = 6
		addSchemesFromMap()
	}

	/**
	 * 测量大小
	 * 
	 * @param width  width
	 * @param height height
	 */
	fun measureSize(width: Int, height: Int) {
		val rect = Rect()
		mCurMonthTextPaint.getTextBounds("1", 0, 1, rect)
		val textHeight = rect.height()
		val mMinHeight = 12 * textHeight + this.monthViewTop

		val h = max(height, mMinHeight)

		layoutParams.width = width
		layoutParams.height = h
		mItemHeight = (h - this.monthViewTop) / 6

		val metrics = mCurMonthTextPaint.getFontMetrics()
		mTextBaseLine =
			mItemHeight.toFloat() / 2 - metrics.descent + (metrics.bottom - metrics.top) / 2

		val monthMetrics = mMonthTextPaint.getFontMetrics()
		mMonthTextBaseLine =
			mDelegate!!.yearViewMonthHeight.toFloat() / 2 - monthMetrics.descent + (monthMetrics.bottom - monthMetrics.top) / 2

		val weekMetrics = mWeekTextPaint.getFontMetrics()
		mWeekTextBaseLine =
			mDelegate!!.yearViewWeekHeight.toFloat() / 2 - weekMetrics.descent + (weekMetrics.bottom - weekMetrics.top) / 2

		invalidate()
	}

	/**
	 * 添加事件标记，来自Map
	 */
	private fun addSchemesFromMap() {
		if (mDelegate!!.mSchemeDatesMap.isEmpty()) {
			return
		}
		for (a in mItems!!) {
			if (mDelegate!!.mSchemeDatesMap.containsKey(a.toString())) {
				val d = mDelegate!!.mSchemeDatesMap[a.toString()] ?: continue
				a.scheme =
					(if (android.text.TextUtils.isEmpty(d.scheme)) mDelegate!!.schemeText else d.scheme)!!
				a.schemeColor = d.schemeColor
				a.schemes = d.schemes
			} else {
				a.scheme = ""
				a.schemeColor = 0
				a.schemes.clear()
			}
		}
	}

	override fun onDraw(canvas: Canvas) {
		mItemWidth =
			(width - mDelegate!!.yearViewMonthPaddingLeft - mDelegate!!.yearViewMonthPaddingRight) / 7
		onPreviewHook()
		onDrawMonth(canvas)
		onDrawWeek(canvas)
		onDrawMonthView(canvas)
	}

	/**
	 * 绘制
	 * 
	 * @param canvas canvas
	 */
	private fun onDrawMonth(canvas: Canvas) {
		onDrawMonth(
				canvas,
				mYear,
				mMonth,
				mDelegate!!.yearViewMonthPaddingLeft,
				mDelegate!!.yearViewMonthPaddingTop,
				width - 2 * mDelegate!!.yearViewMonthPaddingRight,
				mDelegate!!.yearViewMonthHeight + mDelegate!!.yearViewMonthPaddingTop
		)
	}

	private val monthViewTop: Int
		get() = mDelegate!!.yearViewMonthPaddingTop + mDelegate!!.yearViewMonthHeight + mDelegate!!.yearViewMonthPaddingBottom + mDelegate!!.yearViewWeekHeight

	/**
	 * 绘制
	 * 
	 * @param canvas canvas
	 */
	private fun onDrawWeek(canvas: Canvas) {
		if (mDelegate!!.yearViewWeekHeight <= 0) {
			return
		}
		var week = mDelegate!!.weekStart
		if (week > 0) {
			week -= 1
		}
		val width =
			(width - mDelegate!!.yearViewMonthPaddingLeft - mDelegate!!.yearViewMonthPaddingRight) / 7
		for (i in 0..6) {
			onDrawWeek(
					canvas,
					week,
					mDelegate!!.yearViewMonthPaddingLeft + i * width,
					mDelegate!!.yearViewMonthHeight + mDelegate!!.yearViewMonthPaddingTop + mDelegate!!.yearViewMonthPaddingBottom,
					width,
					mDelegate!!.yearViewWeekHeight
			)
			week += 1
			if (week >= 7) {
				week = 0
			}
		}
	}

	/**
	 * 绘制月份数据
	 * 
	 * @param canvas canvas
	 */
	private fun onDrawMonthView(canvas: Canvas) {
		val count = mLineCount * 7
		var d = 0
		for (i in 0..<mLineCount) {
			for (j in 0..6) {
				val calendar = mItems!![d]
				if (d > mItems!!.size - mNextDiff) {
					return
				}
				if (!calendar.isCurrentMonth) {
					++d
					continue
				}
				draw(canvas, calendar, i, j, d)
				++d
			}
		}
	}


	/**
	 * 开始绘制
	 * 
	 * @param canvas   canvas
	 * @param sysuerCalendar 对应日历
	 * @param i        i
	 * @param j        j
	 * @param d        d
	 */
	private fun draw(canvas: Canvas, sysuerCalendar: SysuerCalendar, i: Int, j: Int, d: Int) {
		val x = j * mItemWidth + mDelegate!!.yearViewMonthPaddingLeft
		val y = i * mItemHeight + this.monthViewTop

		val isSelected = sysuerCalendar == mDelegate!!.mSelectedSysuerCalendar
		val hasScheme = sysuerCalendar.hasScheme()

		if (hasScheme) {
			//标记的日子
			var isDrawSelected = false //是否继续绘制选中的onDrawScheme
			if (isSelected) {
				isDrawSelected = onDrawSelected(canvas, sysuerCalendar, x, y, true)
			}
			if (isDrawSelected || !isSelected) {
				//将画笔设置为标记颜色
				mSchemePaint.setColor(if (sysuerCalendar.schemeColor != 0) sysuerCalendar.schemeColor else mDelegate!!.schemeThemeColor)
				onDrawScheme(canvas, sysuerCalendar, x, y)
			}
		} else {
			if (isSelected) {
				onDrawSelected(canvas, sysuerCalendar, x, y, false)
			}
		}
		onDrawText(canvas, sysuerCalendar, x, y, hasScheme, isSelected)
	}

	/**
	 * 开始绘制前的钩子，这里做一些初始化的操作，每次绘制只调用一次，性能高效
	 * 没有需要可忽略不实现
	 * 例如：
	 * 1、需要绘制圆形标记事件背景，可以在这里计算半径
	 * 2、绘制矩形选中效果，也可以在这里计算矩形宽和高
	 */
	protected fun onPreviewHook() {
	}


	/**
	 * 绘制月份
	 * 
	 * @param canvas canvas
	 * @param year   year
	 * @param month  month
	 * @param x      x
	 * @param y      y
	 * @param width  width
	 * @param height height
	 */
	protected abstract fun onDrawMonth(
		canvas: Canvas, year: Int, month: Int, x: Int, y: Int, width: Int, height: Int
	)


	/**
	 * 绘制年视图的周栏
	 * 
	 * @param canvas canvas
	 * @param week   week
	 * @param x      x
	 * @param y      y
	 * @param width  width
	 * @param height height
	 */
	protected abstract fun onDrawWeek(
		canvas: Canvas, week: Int, x: Int, y: Int, width: Int, height: Int
	)


	/**
	 * 绘制选中的日期
	 * 
	 * @param canvas    canvas
	 * @param sysuerCalendar  日历日历calendar
	 * @param x         日历Card x起点坐标
	 * @param y         日历Card y起点坐标
	 * @param hasScheme hasScheme 非标记的日期
	 * @return 是否绘制onDrawScheme，true or false
	 */
	protected abstract fun onDrawSelected(
		canvas: Canvas, sysuerCalendar: SysuerCalendar, x: Int, y: Int, hasScheme: Boolean
	): Boolean

	/**
	 * 绘制标记的日期,这里可以是背景色，标记色什么的
	 * 
	 * @param canvas   canvas
	 * @param sysuerCalendar 日历calendar
	 * @param x        日历Card x起点坐标
	 * @param y        日历Card y起点坐标
	 */
	protected abstract fun onDrawScheme(
		canvas: Canvas,
		sysuerCalendar: SysuerCalendar,
		x: Int,
		y: Int
	)


	/**
	 * 绘制日历文本
	 * 
	 * @param canvas     canvas
	 * @param sysuerCalendar   日历calendar
	 * @param x          日历Card x起点坐标
	 * @param y          日历Card y起点坐标
	 * @param hasScheme  是否是标记的日期
	 * @param isSelected 是否选中
	 */
	protected abstract fun onDrawText(
		canvas: Canvas,
		sysuerCalendar: SysuerCalendar,
		x: Int,
		y: Int,
		hasScheme: Boolean,
		isSelected: Boolean
	)
}
