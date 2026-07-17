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

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.haibin.calendarview.LunarCalendar.setupLunarCalendar
import java.lang.reflect.Constructor
import kotlin.math.abs

/**
 * 月份切换ViewPager，自定义适应高度
 */
class MonthViewPager @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
	ViewPager(context, attrs) {
	@JvmField var mParentLayout: CalendarLayout? = null
	@JvmField var mWeekPager: WeekViewPager? = null
	@JvmField var mWeekBar: WeekBar? = null
	private var isUpdateMonthView = false
	private var mMonthCount = 0
	private var mDelegate: CalendarViewDelegate? = null
	private var mNextViewHeight = 0
	private var mPreViewHeight = 0
	private var mCurrentViewHeight = 0
	
	/**
	 * 是否使用滚动到某一天
	 */
	private var isUsingScrollToCalendar = false
	
	/**
	 * 初始化
	 * 
	 * @param delegate delegate
	 */
	fun setup(delegate: CalendarViewDelegate) {
		mDelegate = delegate
		updateMonthViewHeight(mDelegate!!.currentDay.year, mDelegate!!.currentDay.month)
		val params = layoutParams
		params.height = mCurrentViewHeight
		setLayoutParams(params)
		init()
	}
	
	/**
	 * 初始化
	 */
	private fun init() {
		mMonthCount = (12 * (mDelegate!!.maxYear - mDelegate!!.minYear) - mDelegate!!.minYearMonth) + 1 + mDelegate!!.maxYearMonth
		setAdapter(MonthViewPagerAdapter())
		addOnPageChangeListener(object : OnPageChangeListener {
			override fun onPageScrolled(position: Int,
			                            positionOffset: Float,
			                            positionOffsetPixels: Int) {
				if (mDelegate!!.monthViewShowMode == CalendarViewDelegate.MODE_ALL_MONTH) {
					return
				}
				val height: Int = if (position < currentItem) //右滑-1
					((mPreViewHeight) * (1 - positionOffset) + mCurrentViewHeight * positionOffset).toInt()
				else //左滑+！
					((mCurrentViewHeight) * (1 - positionOffset) + (mNextViewHeight) * positionOffset).toInt()
				val params = layoutParams
				params.height = height
				setLayoutParams(params)
			}
			
			override fun onPageSelected(position: Int) {
				val calendar = CalendarUtil.getFirstCalendarFromMonthViewPager(position, mDelegate)
				if (visibility == VISIBLE) {
					if (!mDelegate!!.isShowYearSelectedLayout && mDelegate!!.mIndexCalendar != null && calendar.year != mDelegate!!.mIndexCalendar.year) {
						mDelegate!!.mYearChangeListener?.onYearChange(calendar.year)
					}
					mDelegate!!.mIndexCalendar = calendar
				} //月份改变事件
				mDelegate!!.mMonthChangeListener?.onMonthChange(calendar.year, calendar.month)                //周视图显示的时候就需要动态改变月视图高度
				if (mWeekPager!!.visibility == VISIBLE) {
					updateMonthViewHeight(calendar.year, calendar.month)
					return
				}
				
				
				if (mDelegate!!.selectMode == CalendarViewDelegate.SELECT_MODE_DEFAULT) {
					mDelegate!!.mSelectedCalendar = (if (!calendar.isCurrentMonth) calendar
					else CalendarUtil.getRangeEdgeCalendar(calendar, mDelegate))
					mDelegate!!.mIndexCalendar = mDelegate!!.mSelectedCalendar
				}
				else {
					if (mDelegate!!.mSelectedStartRangeCalendar != null && mDelegate!!.mSelectedStartRangeCalendar.isSameMonth(mDelegate!!.mIndexCalendar)) {
						mDelegate!!.mIndexCalendar = mDelegate!!.mSelectedStartRangeCalendar
					}
					else {
						if (calendar.isSameMonth(mDelegate!!.mSelectedCalendar)) {
							mDelegate!!.mIndexCalendar = mDelegate!!.mSelectedCalendar
						}
					}
				}
				
				mDelegate!!.updateSelectCalendarScheme()
				if (!isUsingScrollToCalendar && mDelegate!!.selectMode == CalendarViewDelegate.SELECT_MODE_DEFAULT) {
					mWeekBar!!.onDateSelected(mDelegate!!.mSelectedCalendar, mDelegate!!.weekStart, false)
					mDelegate!!.mCalendarSelectListener?.onCalendarSelect(mDelegate!!.mSelectedCalendar, false)
				}
				val view = findViewWithTag<BaseMonthView?>(position)
				if (view != null) {
					val index: Int = view.getSelectedIndex(mDelegate!!.mIndexCalendar)!!
					if (mDelegate!!.selectMode == CalendarViewDelegate.SELECT_MODE_DEFAULT) {
						view.mCurrentItem = index
					}
					if (index >= 0) {
						mParentLayout?.updateSelectPosition(index)
					}
					view.invalidate()
				}
				mWeekPager!!.updateSelected(mDelegate!!.mIndexCalendar, false)
				updateMonthViewHeight(calendar.year, calendar.month)
				isUsingScrollToCalendar = false
			}
			
			override fun onPageScrollStateChanged(state: Int) {
			}
		})
	}
	
	/**
	 * 更新月视图的高度
	 * 
	 * @param year  year
	 * @param month month
	 */
	private fun updateMonthViewHeight(year: Int, month: Int) {
		if (mDelegate!!.monthViewShowMode == CalendarViewDelegate.MODE_ALL_MONTH) { //非动态高度就不需要了
			mCurrentViewHeight = 6 * mDelegate!!.calendarItemHeight
			val params = layoutParams
			params.height = mCurrentViewHeight
			return
		}
		
		if (mParentLayout != null) {
			if (visibility != VISIBLE) { //如果已经显示周视图，则需要动态改变月视图高度，否则显示就有bug
				val params = layoutParams
				params.height = CalendarUtil.getMonthViewHeight(year, month, mDelegate!!.calendarItemHeight, mDelegate!!.weekStart, mDelegate!!.monthViewShowMode)
				setLayoutParams(params)
			}
			mParentLayout?.updateContentViewTranslateY()
		}
		mCurrentViewHeight = CalendarUtil.getMonthViewHeight(year, month, mDelegate!!.calendarItemHeight, mDelegate!!.weekStart, mDelegate!!.monthViewShowMode)
		if (month == 1) {
			mPreViewHeight = CalendarUtil.getMonthViewHeight(year - 1, 12, mDelegate!!.calendarItemHeight, mDelegate!!.weekStart, mDelegate!!.monthViewShowMode)
			mNextViewHeight = CalendarUtil.getMonthViewHeight(year, 2, mDelegate!!.calendarItemHeight, mDelegate!!.weekStart, mDelegate!!.monthViewShowMode)
		}
		else {
			mPreViewHeight = CalendarUtil.getMonthViewHeight(year, month - 1, mDelegate!!.calendarItemHeight, mDelegate!!.weekStart, mDelegate!!.monthViewShowMode)
			mNextViewHeight = if (month == 12) {
				CalendarUtil.getMonthViewHeight(year + 1, 1, mDelegate!!.calendarItemHeight, mDelegate!!.weekStart, mDelegate!!.monthViewShowMode)
			}
			else {
				CalendarUtil.getMonthViewHeight(year, month + 1, mDelegate!!.calendarItemHeight, mDelegate!!.weekStart, mDelegate!!.monthViewShowMode)
			}
		}
	}
	
	/**
	 * 刷新
	 */
	fun notifyDataSetChanged() {
		mMonthCount = (12 * (mDelegate!!.maxYear - mDelegate!!.minYear) - mDelegate!!.minYearMonth) + 1 + mDelegate!!.maxYearMonth
		notifyAdapterDataSetChanged()
	}
	
	/**
	 * 更新月视图Class
	 */
	fun updateMonthViewClass() {
		isUpdateMonthView = true
		notifyAdapterDataSetChanged()
		isUpdateMonthView = false
	}
	
	/**
	 * 更新日期范围
	 */
	fun updateRange() {
		isUpdateMonthView = true
		notifyDataSetChanged()
		isUpdateMonthView = false
		if (visibility == VISIBLE) {
			isUsingScrollToCalendar = false
			val calendar = mDelegate!!.mSelectedCalendar
			val y = calendar.year - mDelegate!!.minYear
			val position = 12 * y + calendar.month - mDelegate!!.minYearMonth
			setCurrentItem(position, false)
			findViewWithTag<BaseMonthView?>(position)?.run {
				setSelectedCalendar(mDelegate.mIndexCalendar)
				invalidate()
				mParentLayout?.updateSelectPosition(getSelectedIndex(mDelegate.mIndexCalendar)!!)
			}
			if (mParentLayout != null) {
				val week = CalendarUtil.getWeekFromDayInMonth(calendar, mDelegate!!.weekStart)
				mParentLayout!!.updateSelectWeek(week)
			}
			mDelegate!!.mInnerListener?.onMonthDateSelected(calendar, false)
			mDelegate!!.mCalendarSelectListener?.onCalendarSelect(calendar, false)
			
			updateSelected()
		}
	}
	
	/**
	 * 滚动到指定日期
	 * 
	 * @param year           年
	 * @param month          月
	 * @param day            日
	 * @param invokeListener 调用日期事件
	 */
	fun scrollToCalendar(year: Int,
	                     month: Int,
	                     day: Int,
	                     smoothScroll: Boolean,
	                     invokeListener: Boolean) {
		isUsingScrollToCalendar = true
		val calendar = Calendar().apply {
			this.year = year
			this.month = month
			this.day = day
			isCurrentDay = this == mDelegate!!.currentDay
		}
		setupLunarCalendar(calendar)
		mDelegate!!.mIndexCalendar = calendar
		mDelegate!!.mSelectedCalendar = calendar
		mDelegate!!.updateSelectCalendarScheme()
		val y = calendar.year - mDelegate!!.minYear
		val position = 12 * y + calendar.month - mDelegate!!.minYearMonth
		val curItem = currentItem
		if (curItem == position) {
			isUsingScrollToCalendar = false
		}
		setCurrentItem(position, smoothScroll)
		findViewWithTag<BaseMonthView?>(position)?.run {
			setSelectedCalendar(mDelegate.mIndexCalendar)
			invalidate()
			mParentLayout?.updateSelectPosition(getSelectedIndex(mDelegate.mIndexCalendar)!!)
		}
		if (mParentLayout != null) {
			val week = CalendarUtil.getWeekFromDayInMonth(calendar, mDelegate!!.weekStart)
			mParentLayout!!.updateSelectWeek(week)
		}
		
		if (invokeListener) {
			mDelegate!!.mCalendarSelectListener?.onCalendarSelect(calendar, false)
		}
		mDelegate!!.mInnerListener?.onMonthDateSelected(calendar, false)
		
		
		updateSelected()
	}
	
	/**
	 * 滚动到当前日期
	 */
	fun scrollToCurrent(smoothScroll: Boolean) {
		isUsingScrollToCalendar = true
		val position = 12 * (mDelegate!!.currentDay.year - mDelegate!!.minYear) + mDelegate!!.currentDay.month - mDelegate!!.minYearMonth
		if (currentItem == position) {
			isUsingScrollToCalendar = false
		}
		
		setCurrentItem(position, smoothScroll)
		findViewWithTag<BaseMonthView?>(position)?.run {
			setSelectedCalendar(mDelegate.currentDay)
			invalidate()
			mParentLayout?.updateSelectPosition(getSelectedIndex(mDelegate.currentDay)!!)
		}
		
		if (visibility == VISIBLE) {
			mDelegate!!.mCalendarSelectListener?.onCalendarSelect(mDelegate!!.mSelectedCalendar, false)
		}
	}
	
	val currentMonthCalendars: MutableList<Calendar>?
		/**
		 * 获取当前月份数据
		 * 
		 * @return 获取当前月份数据
		 */
		get() {
			val view = findViewWithTag<BaseMonthView?>(currentItem) ?: return null
			return view.mItems
		}
	
	/**
	 * 更新为默认选择模式
	 */
	fun updateDefaultSelect() {
		findViewWithTag<BaseMonthView?>(currentItem)?.run {
			val index: Int = getSelectedIndex(mDelegate.mSelectedCalendar)!!
			mCurrentItem = index
			if (index >= 0) {
				mParentLayout?.updateSelectPosition(index)
			}
			invalidate()
		}
	}
	
	/**
	 * 更新选择效果
	 */
	fun updateSelected() {
		children.forEach {
			(it as BaseMonthView).apply {
				setSelectedCalendar(mDelegate.mSelectedCalendar)
				invalidate()
			}
		}
	}
	
	/**
	 * 更新字体颜色大小
	 */
	fun updateStyle() {
		children.forEach {
			(it as BaseMonthView).apply {
				updateStyle()
				invalidate()
			}
		}
	}
	
	/**
	 * 更新标记日期
	 */
	fun updateScheme() {
		children.forEach {
			(it as BaseMonthView).apply {
				update()
			}
		}
	}
	
	/**
	 * 更新当前日期，夜间过度的时候调用这个函数，一般不需要调用
	 */
	fun updateCurrentDate() {
		children.forEach {
			(it as BaseMonthView).apply {
				updateCurrentDate()
			}
		}
	}
	
	/**
	 * 更新显示模式
	 */
	fun updateShowMode() {
		children.forEach {
			(it as BaseMonthView).apply {
				updateShowMode()
				requestLayout()
			}
		}
		if (mDelegate!!.monthViewShowMode == CalendarViewDelegate.MODE_ALL_MONTH) {
			mCurrentViewHeight = 6 * mDelegate!!.calendarItemHeight
			mNextViewHeight = mCurrentViewHeight
			mPreViewHeight = mCurrentViewHeight
		}
		else {
			updateMonthViewHeight(mDelegate!!.mSelectedCalendar.year, mDelegate!!.mSelectedCalendar.month)
		}
		val params = layoutParams
		params.height = mCurrentViewHeight
		setLayoutParams(params)
		if (mParentLayout != null) {
			mParentLayout!!.updateContentViewTranslateY()
		}
	}
	
	/**
	 * 更新周起始
	 */
	fun updateWeekStart() {
		children.forEach {
			(it as BaseMonthView).apply {
				updateWeekStart()
				requestLayout()
			}
		}
		
		updateMonthViewHeight(mDelegate!!.mSelectedCalendar.year, mDelegate!!.mSelectedCalendar.month)
		val params = layoutParams
		params.height = mCurrentViewHeight
		setLayoutParams(params)
		if (mParentLayout != null) {
			val i = CalendarUtil.getWeekFromDayInMonth(mDelegate!!.mSelectedCalendar, mDelegate!!.weekStart)
			mParentLayout!!.updateSelectWeek(i)
		}
		updateSelected()
	}
	
	/**
	 * 更新高度
	 */
	fun updateItemHeight() {
		children.forEach {
			(it as BaseMonthView).apply {
				updateItemHeight()
				requestLayout()
			}
		}
		val year = mDelegate!!.mIndexCalendar.year
		val month = mDelegate!!.mIndexCalendar.month
		mCurrentViewHeight = CalendarUtil.getMonthViewHeight(year, month, mDelegate!!.calendarItemHeight, mDelegate!!.weekStart, mDelegate!!.monthViewShowMode)
		if (month == 1) {
			mPreViewHeight = CalendarUtil.getMonthViewHeight(year - 1, 12, mDelegate!!.calendarItemHeight, mDelegate!!.weekStart, mDelegate!!.monthViewShowMode)
			mNextViewHeight = CalendarUtil.getMonthViewHeight(year, 2, mDelegate!!.calendarItemHeight, mDelegate!!.weekStart, mDelegate!!.monthViewShowMode)
		}
		else {
			mPreViewHeight = CalendarUtil.getMonthViewHeight(year, month - 1, mDelegate!!.calendarItemHeight, mDelegate!!.weekStart, mDelegate!!.monthViewShowMode)
			mNextViewHeight = (if (month == 12) CalendarUtil.getMonthViewHeight(year + 1, 1, mDelegate!!.calendarItemHeight, mDelegate!!.weekStart, mDelegate!!.monthViewShowMode)
			else CalendarUtil.getMonthViewHeight(year, month + 1, mDelegate!!.calendarItemHeight, mDelegate!!.weekStart, mDelegate!!.monthViewShowMode))
		}
		val params = layoutParams
		params.height = mCurrentViewHeight
		setLayoutParams(params)
	}
	
	/**
	 * 清除选择范围
	 */
	fun clearSelectRange() {
		children.forEach {
			(it as BaseMonthView).apply {
				invalidate()
			}
		}
	}
	
	/**
	 * 清除单选选择
	 */
	fun clearSingleSelect() {
		children.forEach {
			(it as BaseMonthView).apply {
				mCurrentItem = -1
				invalidate()
			}
		}
	}
	
	/**
	 * 清除单选选择
	 */
	fun clearMultiSelect() {
		children.forEach {
			(it as BaseMonthView).apply {
				mCurrentItem = -1
				invalidate()
			}
		}
	}
	
	private fun notifyAdapterDataSetChanged() {
		adapter?.notifyDataSetChanged()
	}
	
	@SuppressLint("ClickableViewAccessibility")
	override fun onTouchEvent(ev: MotionEvent?): Boolean =
		mDelegate!!.isMonthViewScrollable && super.onTouchEvent(ev)
	
	override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean =
		mDelegate!!.isMonthViewScrollable && super.onInterceptTouchEvent(ev)
	
	override fun setCurrentItem(item: Int) {
		setCurrentItem(item, true)
	}
	
	override fun setCurrentItem(item: Int, smoothScroll: Boolean) {
		if (abs(currentItem - item) > 1) super.setCurrentItem(item, false)
		else super.setCurrentItem(item, smoothScroll)
	}
	
	/**
	 * 日历卡月份Adapter
	 */
	private inner class MonthViewPagerAdapter : PagerAdapter() {
		override fun getCount(): Int = mMonthCount
		override fun getItemPosition(o: Any): Int =
			if (isUpdateMonthView) POSITION_NONE else super.getItemPosition(o)
		
		override fun isViewFromObject(view: View, o: Any): Boolean = view == o
		override fun instantiateItem(container: ViewGroup, position: Int): Any {
			val year = (position + mDelegate!!.minYearMonth - 1) / 12 + mDelegate!!.minYear
			val month = (position + mDelegate!!.minYearMonth - 1) % 12 + 1
			val view: BaseMonthView
			try {
				val constructor: Constructor<*> = mDelegate!!.monthViewClass.getConstructor(Context::class.java)
				view = constructor.newInstance(context) as BaseMonthView
			} catch (_: Exception) {
				return DefaultMonthView(context)
			}
			view.mMonthViewPager = this@MonthViewPager
			view.mParentLayout = mParentLayout
			view.setup(mDelegate)
			view.tag = position
			view.initMonthWithDate(year, month)
			view.setSelectedCalendar(mDelegate!!.mSelectedCalendar)
			container.addView(view)
			return view
		}
		
		override fun destroyItem(container: ViewGroup, position: Int, o: Any) {
			val view = o as BaseView
			view.onDestroy()
			container.removeView(view)
		}
	}
}
