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
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView

/**
 * 星期栏，如果你要使用星期栏自定义，切记XML使用 merge，不要使用LinearLayout
 * Created by huanghaibin on 2017/11/30.
 */
open class WeekBar(context: Context?) : LinearLayout(context) {
	private var mDelegate: CalendarViewDelegate? = null
	
	init {
		if ("com.haibin.calendarview.WeekBar" == javaClass.getName()) {
			LayoutInflater.from(context).inflate(R.layout.cv_week_bar, this, true)
		}
	}
	
	/**
	 * 传递属性
	 * 
	 * @param delegate delegate
	 */
	fun setup(delegate: CalendarViewDelegate) {
		mDelegate = delegate
		if ("com.haibin.calendarview.WeekBar".equals(javaClass.getName(), ignoreCase = true)) {
			setTextSize(mDelegate!!.weekTextSize)
			setTextColor(delegate.weekTextColor)
			setBackgroundColor(delegate.weekBackground)
			setPadding(delegate.calendarPaddingLeft, 0, delegate.calendarPaddingRight, 0)
		}
	}
	
	/**
	 * 设置文本颜色，使用自定义布局需要重写这个方法，避免出问题
	 * 如果这里报错了，请确定你自定义XML文件跟布局是不是使用merge，而不是LinearLayout
	 * 
	 * @param color color
	 */
	fun setTextColor(color: Int) {
		(0..<childCount).forEach {
			(getChildAt(it) as TextView).setTextColor(color)
		}
	}
	
	/**
	 * 设置文本大小
	 * 
	 * @param size size
	 */
	protected fun setTextSize(size: Int) {
		(0..<childCount).forEach {
			(getChildAt(it) as TextView).setTextSize(TypedValue.COMPLEX_UNIT_PX, size.toFloat())
		}
	}
	
	/**
	 * 日期选择事件，这里提供这个回调，可以方便定制WeekBar需要
	 * 
	 * @param calendar  calendar 选择的日期
	 * @param weekStart 周起始
	 * @param isClick   isClick 点击
	 */
	fun onDateSelected(calendar: Calendar?, weekStart: Int, isClick: Boolean) {
	}
	
	/**
	 * 当周起始发生变化，使用自定义布局需要重写这个方法，避免出问题
	 * 
	 * @param weekStart 周起始
	 */
	fun onWeekStartChange(weekStart: Int) {
		if ("com.haibin.calendarview.WeekBar".equals(javaClass.getName(), ignoreCase = true)) (0..<childCount).forEach {
			(getChildAt(it) as TextView).text = getWeekString(it, weekStart)
		}
	}
	
	/**
	 * 通过View的位置和周起始获取星期的对应坐标
	 * 
	 * @param calendar  calendar
	 * @param weekStart weekStart
	 * @return 通过View的位置和周起始获取星期的对应坐标
	 */
	protected fun getViewIndexByCalendar(calendar: Calendar, weekStart: Int): Int {
		val week = calendar.week + 1
		return when (weekStart) {
			CalendarViewDelegate.WEEK_START_WITH_SUN -> {
				week - 1
			}
			CalendarViewDelegate.WEEK_START_WITH_MON -> {
				if (week == CalendarViewDelegate.WEEK_START_WITH_SUN) 6 else week - 2
			}
			else -> if (week == CalendarViewDelegate.WEEK_START_WITH_SAT) 0 else week
		}
	}
	
	/**
	 * 或者周文本，这个方法仅供父类使用
	 * 
	 * @param index     index
	 * @param weekStart weekStart
	 * @return 或者周文本
	 */
	private fun getWeekString(index: Int, weekStart: Int): String? {
		val weeks = context.resources.getStringArray(R.array.week_string_array)
		return when (weekStart) {
			CalendarViewDelegate.WEEK_START_WITH_SUN -> {
				weeks[index]
			}
			CalendarViewDelegate.WEEK_START_WITH_MON -> {
				weeks[if (index == 6) 0 else index + 1]
			}
			else -> weeks[if (index == 0) 6 else index - 1]
		}
	}
	
	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val heightMeasureSpec: Int = if (mDelegate != null) MeasureSpec.makeMeasureSpec(mDelegate!!.weekBarHeight, MeasureSpec.EXACTLY)
		else MeasureSpec.makeMeasureSpec(CalendarUtil.dipToPx(context, 40f), MeasureSpec.EXACTLY)
		super.onMeasure(widthMeasureSpec, heightMeasureSpec)
	}
}
