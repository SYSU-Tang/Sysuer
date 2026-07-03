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
import android.graphics.Color
import android.graphics.Paint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.View.OnLongClickListener
import kotlin.math.abs

/**
 * 基本的日历View，派生出MonthView 和 WeekView
 * Created by huanghaibin on 2018/1/23.
 */
abstract class BaseView(context: Context?, attrs: AttributeSet? = null) :
	View(context, attrs), View.OnClickListener, OnLongClickListener {
	/**
	 * 当前月份日期的笔
	 */
	protected val mCurMonthTextPaint: Paint = Paint().apply {
		isAntiAlias = true
		textAlign = Paint.Align.CENTER
		setColor(-0xeeeeef)
		isFakeBoldText = true
		textSize = CalendarUtil.dipToPx(context, TEXT_SIZE.toFloat()).toFloat()
	}
	
	/**
	 * 其它月份日期颜色
	 */
	protected val mOtherMonthTextPaint: Paint = Paint().apply {
		isAntiAlias = true
		textAlign = Paint.Align.CENTER
		setColor(-0x1e1e1f)
		isFakeBoldText = true
		textSize = CalendarUtil.dipToPx(context, TEXT_SIZE.toFloat()).toFloat()
	}
	
	/**
	 * 当前月份农历文本颜色
	 */
	protected val mCurMonthLunarTextPaint: Paint = Paint().apply {
		isAntiAlias = true
		textAlign = Paint.Align.CENTER
	}
	
	/**
	 * 当前月份农历文本颜色
	 */
	protected val mSelectedLunarTextPaint: Paint = Paint().apply {
		isAntiAlias = true
		textAlign = Paint.Align.CENTER
	}
	
	/**
	 * 其它月份农历文本颜色
	 */
	protected val mOtherMonthLunarTextPaint: Paint = Paint().apply {
		isAntiAlias = true
		textAlign = Paint.Align.CENTER
	}
	
	/**
	 * 其它月份农历文本颜色
	 */
	protected val mSchemeLunarTextPaint: Paint = Paint().apply {
		isAntiAlias = true
		textAlign = Paint.Align.CENTER
	}
	
	/**
	 * 标记的日期背景颜色画笔
	 */
	@JvmField protected val mSchemePaint: Paint = Paint().apply {
		isAntiAlias = true
		style = Paint.Style.FILL
		strokeWidth = 2f
		setColor(-0x101011)
	}
	
	/**
	 * 被选择的日期背景色
	 */
	protected val mSelectedPaint: Paint = Paint().apply {
		isAntiAlias = true
		style = Paint.Style.FILL
		strokeWidth = 2f
	}
	
	/**
	 * 标记的文本画笔
	 */
	protected val mSchemeTextPaint: Paint = Paint().apply {
		isAntiAlias = true
		style = Paint.Style.FILL
		textAlign = Paint.Align.CENTER
		setColor(-0x12acad)
		isFakeBoldText = true
		textSize = CalendarUtil.dipToPx(context, TEXT_SIZE.toFloat()).toFloat()
	}
	
	/**
	 * 选中的文本画笔
	 */
	protected val mSelectTextPaint: Paint = Paint().apply {
		isAntiAlias = true
		style = Paint.Style.FILL
		textAlign = Paint.Align.CENTER
		setColor(-0x12acad)
		isFakeBoldText = true
		textSize = CalendarUtil.dipToPx(context, TEXT_SIZE.toFloat()).toFloat()
	}
	
	/**
	 * 当前日期文本颜色画笔
	 */
	protected val mCurDayTextPaint: Paint = Paint().apply {
		isAntiAlias = true
		textAlign = Paint.Align.CENTER
		setColor(Color.RED)
		isFakeBoldText = true
		textSize = CalendarUtil.dipToPx(context, TEXT_SIZE.toFloat()).toFloat()
	}
	
	/**
	 * 当前日期文本颜色画笔
	 */
	protected val mCurDayLunarTextPaint: Paint = Paint().apply {
		isAntiAlias = true
		textAlign = Paint.Align.CENTER
		setColor(Color.RED)
		isFakeBoldText = true
		textSize = CalendarUtil.dipToPx(context, TEXT_SIZE.toFloat()).toFloat()
	}
	
	/**
	 * 日历项
	 */
	@JvmField var mItems: MutableList<Calendar>? = null
	
	/**
	 * 每一项的高度
	 */
	@JvmField protected var mItemHeight: Int = 0
	
	/**
	 * 每一项的宽度
	 */
	@JvmField protected var mItemWidth: Int = 0
	
	/**
	 * Text的基线
	 */
	protected var mTextBaseLine: Float = 0f
	
	/**
	 * 点击的x、y坐标
	 */
	@JvmField protected var mX: Float = 0f
	@JvmField protected var mY: Float = 0f
	@JvmField var mDelegate: CalendarViewDelegate? = null
	
	/**
	 * 日历布局，需要在日历下方放自己的布局
	 */
	@JvmField var mParentLayout: CalendarLayout? = null
	
	/**
	 * 是否点击
	 */
	@JvmField var isClick: Boolean = true
	
	/**
	 * 当前点击项
	 */
	@JvmField var mCurrentItem: Int = -1
	
	/**
	 * 周起始
	 */
	var mWeekStartWidth: Int = 0
	
	init {
		setOnClickListener(this)
		setOnLongClickListener(this)
	}
	
	/**
	 * 初始化所有UI配置
	 * 
	 * @param delegate delegate
	 */
	fun setup(delegate: CalendarViewDelegate?) {
		mDelegate = delegate
		mWeekStartWidth = mDelegate!!.weekStart
		updateStyle()
		updateItemHeight()
		initPaint()
	}
	
	fun updateStyle() {
		if (mDelegate != null) {
			mCurDayTextPaint.setColor(mDelegate!!.curDayTextColor)
			mCurDayLunarTextPaint.setColor(mDelegate!!.curDayLunarTextColor)
			mCurMonthTextPaint.setColor(mDelegate!!.currentMonthTextColor)
			mOtherMonthTextPaint.setColor(mDelegate!!.otherMonthTextColor)
			mCurMonthLunarTextPaint.setColor(mDelegate!!.currentMonthLunarTextColor)
			mSelectedLunarTextPaint.setColor(mDelegate!!.selectedLunarTextColor)
			mSelectTextPaint.setColor(mDelegate!!.selectedTextColor)
			mOtherMonthLunarTextPaint.setColor(mDelegate!!.otherMonthLunarTextColor)
			mSchemeLunarTextPaint.setColor(mDelegate!!.schemeLunarTextColor)
			mSchemePaint.setColor(mDelegate!!.schemeThemeColor)
			mSchemeTextPaint.setColor(mDelegate!!.schemeTextColor)
			mCurMonthTextPaint.textSize = mDelegate!!.dayTextSize.toFloat()
			mOtherMonthTextPaint.textSize = mDelegate!!.dayTextSize.toFloat()
			mCurDayTextPaint.textSize = mDelegate!!.dayTextSize.toFloat()
			mSchemeTextPaint.textSize = mDelegate!!.dayTextSize.toFloat()
			mSelectTextPaint.textSize = mDelegate!!.dayTextSize.toFloat()
			
			mCurMonthLunarTextPaint.textSize = mDelegate!!.lunarTextSize.toFloat()
			mSelectedLunarTextPaint.textSize = mDelegate!!.lunarTextSize.toFloat()
			mCurDayLunarTextPaint.textSize = mDelegate!!.lunarTextSize.toFloat()
			mOtherMonthLunarTextPaint.textSize = mDelegate!!.lunarTextSize.toFloat()
			mSchemeLunarTextPaint.textSize = mDelegate!!.lunarTextSize.toFloat()
			
			mSelectedPaint.style = Paint.Style.FILL
			mSelectedPaint.setColor(mDelegate!!.selectedThemeColor)
		}
	}
	
	open fun updateItemHeight() {
		mItemHeight = mDelegate!!.calendarItemHeight
		val metrics = mCurMonthTextPaint.getFontMetrics()
		mTextBaseLine = mItemHeight / 2 - metrics.descent + (metrics.bottom - metrics.top) / 2
	}
	
	/**
	 * 移除事件
	 */
	fun removeSchemes() {
		mItems!!.forEach {
			it.scheme = ""
			it.schemeColor = 0
			it.schemes = null
		}
	}
	
	/**
	 * 添加事件标记，来自Map
	 */
	fun addSchemesFromMap() {
		if (mDelegate!!.mSchemeDatesMap != null && !mDelegate!!.mSchemeDatesMap.isEmpty()) {
			mItems!!.forEach {
				if (mDelegate!!.mSchemeDatesMap.containsKey("$it")) {
					val d = mDelegate!!.mSchemeDatesMap["$it"] ?: return@forEach
					it.scheme = if (TextUtils.isEmpty(d.scheme)) mDelegate!!.schemeText else d.scheme
					it.schemeColor = d.schemeColor
					it.schemes = d.schemes
				}
				else {
					it.scheme = ""
					it.schemeColor = 0
					it.schemes = null
				}
			}
		}
	}
	
	override fun onTouchEvent(event: MotionEvent): Boolean {
		if (event.pointerCount > 1) return false
		when (event.action) {
			MotionEvent.ACTION_DOWN -> {
				mX = event.x
				mY = event.y
				isClick = true
			}
			MotionEvent.ACTION_MOVE -> {
				val mDY: Float
				if (isClick) {
					mDY = event.y - mY
					isClick = abs(mDY) <= 50
				}
			}
			MotionEvent.ACTION_UP -> {
				mX = event.x
				mY = event.y
			}
		}
		return super.onTouchEvent(event)
	}
	
	/**
	 * 开始绘制前的钩子，这里做一些初始化的操作，每次绘制只调用一次，性能高效
	 * 没有需要可忽略不实现
	 * 例如：
	 * 1、需要绘制圆形标记事件背景，可以在这里计算半径
	 * 2、绘制矩形选中效果，也可以在这里计算矩形宽和高
	 */
	protected open fun onPreviewHook() {
	}
	
	/**
	 * 是否是选中的
	 *
	 * @param calendar calendar
	 * @return true or false
	 */
	protected fun isSelected(calendar: Calendar?): Boolean =
		mItems != null && mItems!!.indexOf(calendar!!) == mCurrentItem
	
	/**
	 * 更新事件
	 */
	fun update() {
		if (mDelegate!!.mSchemeDatesMap == null || mDelegate!!.mSchemeDatesMap.isEmpty()) { //清空操作
			removeSchemes()
			invalidate()
			return
		}
		addSchemesFromMap()
		invalidate()
	}
	
	/**
	 * 是否拦截日期，此设置续设置mCalendarInterceptListener
	 *
	 * @param calendar calendar
	 * @return 是否拦截日期
	 */
	protected fun onCalendarIntercept(calendar: Calendar?): Boolean =
		mDelegate!!.mCalendarInterceptListener != null && mDelegate!!.mCalendarInterceptListener.onCalendarIntercept(calendar)
	
	/**
	 * 是否在日期范围内
	 *
	 * @param calendar calendar
	 * @return 是否在日期范围内
	 */
	protected fun isInRange(calendar: Calendar?): Boolean =
		mDelegate != null && CalendarUtil.isCalendarInRange(calendar, mDelegate)
	
	/**
	 * 跟新当前日期
	 */
	abstract fun updateCurrentDate()
	
	/**
	 * 销毁
	 */
	abstract fun onDestroy()
	protected val weekStartWith: Int
		get() = if (mDelegate != null) mDelegate!!.weekStart else CalendarViewDelegate.WEEK_START_WITH_SUN
	protected val calendarPaddingLeft: Int
		get() = if (mDelegate != null) mDelegate!!.calendarPaddingLeft else 0
	protected val calendarPaddingRight: Int
		get() = if (mDelegate != null) mDelegate!!.calendarPaddingRight else 0
	
	/**
	 * 初始化画笔相关
	 */
	protected fun initPaint() {
	}
	
	companion object {
		/**
		 * 字体大小
		 */
		const val TEXT_SIZE: Int = 14
	}
}
