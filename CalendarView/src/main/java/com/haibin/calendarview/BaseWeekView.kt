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

/**
 * 最基础周视图，因为日历UI采用热插拔实现，所以这里必须继承实现，达到UI一致即可
 * 可通过此扩展各种视图如：WeekView、RangeWeekView
 */
abstract class BaseWeekView(context: Context) : BaseView(context) {
	/**
	 * 初始化周视图控件
	 * 
	 * @param calendar calendar
	 */
	fun setup(calendar: Calendar) {
		mItems = CalendarUtil.initCalendarForWeekView(calendar, mDelegate, mDelegate.weekStart)
		addSchemesFromMap()
		invalidate()
	}
	
	/**
	 * 记录已经选择的日期
	 * 
	 * @param calendar calendar
	 */
	fun setSelectedCalendar(calendar: Calendar) {
		if (mDelegate.selectMode != CalendarViewDelegate.SELECT_MODE_SINGLE || calendar == mDelegate.mSelectedCalendar) mCurrentItem = mItems!!.indexOf(calendar)
	}
	
	/**
	 * 周视图切换点击默认位置
	 * 
	 * @param calendar calendar
	 * @param isNotice isNotice
	 */
	fun performClickCalendar(calendar: Calendar, isNotice: Boolean) {
		if (mParentLayout != null && mDelegate.mInnerListener != null && mItems != null && !mItems!!.isEmpty()) {
			val week = if (mItems!!.contains(mDelegate.currentDay)) CalendarUtil.getWeekViewIndexFromCalendar(mDelegate.currentDay, mDelegate.weekStart) else CalendarUtil.getWeekViewIndexFromCalendar(calendar, mDelegate.weekStart)
			var curIndex = week
			var currentCalendar = mItems!![week]
			if (mDelegate.selectMode != CalendarViewDelegate.SELECT_MODE_DEFAULT) {
				if (mItems!!.contains(mDelegate.mSelectedCalendar)) currentCalendar = mDelegate.mSelectedCalendar
				else mCurrentItem = -1
			}
			
			if (!isInRange(currentCalendar)) {
				curIndex = getEdgeIndex(isMinRangeEdge(currentCalendar))
				currentCalendar = mItems!![curIndex]
			}
			
			currentCalendar.isCurrentDay = currentCalendar == mDelegate.currentDay
			mDelegate.mInnerListener.onWeekDateSelected(currentCalendar, false)
			mParentLayout!!.updateSelectWeek(CalendarUtil.getWeekFromDayInMonth(currentCalendar, mDelegate.weekStart))
			
			if (mDelegate.mCalendarSelectListener != null && isNotice && mDelegate.selectMode == CalendarViewDelegate.SELECT_MODE_DEFAULT) mDelegate.mCalendarSelectListener.onCalendarSelect(currentCalendar, false)
			
			mParentLayout!!.updateContentViewTranslateY()
			if (mDelegate.selectMode == CalendarViewDelegate.SELECT_MODE_DEFAULT) mCurrentItem = curIndex
			
			if (!mDelegate.isShowYearSelectedLayout && mDelegate.mIndexCalendar != null && calendar.year != mDelegate.mIndexCalendar.year && mDelegate.mYearChangeListener != null) mDelegate.mYearChangeListener.onYearChange(mDelegate.mIndexCalendar.year)
			
			mDelegate.mIndexCalendar = currentCalendar
			invalidate()
		}
	}
	
	/**
	 * 是否是最小访问边界了
	 * 
	 * @param calendar calendar
	 * @return 是否是最小访问边界了
	 */
	fun isMinRangeEdge(calendar: Calendar): Boolean = with(java.util.Calendar.getInstance()) {
		set(mDelegate.minYear, mDelegate.minYearMonth - 1, mDelegate.minYearDay)
		val minTime = getTimeInMillis()
		set(calendar.year, calendar.month - 1, calendar.day)
		val curTime = getTimeInMillis()
		curTime < minTime
	}
	
	/**
	 * 获得边界范围内下标
	 * 
	 * @param isMinEdge isMinEdge
	 * @return 获得边界范围内下标
	 */
	fun getEdgeIndex(isMinEdge: Boolean): Int {
		mItems?.forEachIndexed { i, v ->
			val isInRange = isInRange(v)
			when {
				isMinEdge && isInRange -> return i
				!isMinEdge && !isInRange -> return i - 1
			}
		}
		return if (isMinEdge) 6 else 0
	}
	
	protected val index: Calendar?
		/**
		 * 获取点击的日历
		 * 
		 * @return 获取点击的日历
		 */
		get() {
			if (mX <= mDelegate.calendarPaddingLeft || mX >= width - mDelegate.calendarPaddingRight) {
				onClickCalendarPadding()
				return null
			}
			val position = mY.toInt() / mItemHeight * 7 + (((mX - mDelegate.calendarPaddingLeft).toInt() / mItemWidth).takeIf { it < 7 }
				?: 6) // 选择项
			return if (position >= 0 && position < mItems!!.size) mItems!![position]
			else null
		}
	
	private fun onClickCalendarPadding() {
		if (mDelegate.mClickCalendarPaddingListener != null) {
			val position = mY.toInt() / mItemHeight * 7 + (((mX - mDelegate.calendarPaddingLeft).toInt() / mItemWidth).takeIf { it < 7 }
				?: 6) // 选择项
			if (position >= 0 && position < mItems!!.size) {
				val calendar = mItems!![position]
				mDelegate.mClickCalendarPaddingListener.onClickCalendarPadding(mX, mY, false, calendar, getClickCalendarPaddingObject(mX, mY, calendar))
			}
		}
	}
	
	/**
	 * / **
	 * 获取点击事件处的对象
	 * 
	 * @param x                x
	 * @param y                y
	 * @param adjacentCalendar adjacent calendar
	 * @return obj can as null
	 */
	protected fun getClickCalendarPaddingObject(x: Float,
	                                            y: Float,
	                                            adjacentCalendar: Calendar?): Any? {
		return null
	}
	
	/**
	 * 更新显示模式
	 */
	fun updateShowMode() {
		invalidate()
	}
	
	/**
	 * 更新周起始
	 */
	fun updateWeekStart() {
		val calendar = CalendarUtil.getFirstCalendarStartWithMinCalendar(mDelegate.minYear, mDelegate.minYearMonth, mDelegate.minYearDay, tag as Int + 1, mDelegate.weekStart)
		setSelectedCalendar(mDelegate.mSelectedCalendar)
		setup(calendar)
	}
	
	/**
	 * 更新当选模式
	 */
	fun updateSingleSelect() {
		if (!mItems!!.contains(mDelegate.mSelectedCalendar)) {
			mCurrentItem = -1
			invalidate()
		}
	}
	
	override fun updateCurrentDate() {
		if (mItems == null) return
		if (mItems!!.contains(mDelegate.currentDay)) {
			for (a in mItems) { //添加操作
				a.isCurrentDay = false
			}
			val index = mItems!!.indexOf(mDelegate.currentDay)
			mItems!![index].isCurrentDay = true
		}
		invalidate()
	}
	
	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(mItemHeight, MeasureSpec.EXACTLY))
	}
	
	protected fun onLoopStart() {
	}
	
	override fun onDestroy() {
	}
}
