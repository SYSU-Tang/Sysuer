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
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import java.lang.reflect.Constructor

/**
 * 周视图滑动ViewPager，需要动态固定高度
 * 周视图是连续不断的视图，因此不能简单的得出每年都有52+1周，这样会计算重叠的部分
 * WeekViewPager需要和CalendarView关联:
 */
class WeekViewPager @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
	ViewPager(context, attrs) {
	/**
	 * 日历布局，需要在日历下方放自己的布局
	 */
	@JvmField var mParentLayout: CalendarLayout? = null
	private var isUpdateWeekView = false
	private var mWeekCount = 0
	private var mDelegate: CalendarViewDelegate? = null
	
	/**
	 * 是否使用滚动到某一天
	 */
	private var isUsingScrollToCalendar = false
	fun setup(delegate: CalendarViewDelegate) {
		mDelegate = delegate
		init()
	}
	
	private fun init() {
		mWeekCount = CalendarUtil.getWeekCountBetweenBothCalendar(mDelegate!!.minYear, mDelegate!!.minYearMonth, mDelegate!!.minYearDay, mDelegate!!.maxYear, mDelegate!!.maxYearMonth, mDelegate!!.maxYearDay, mDelegate!!.weekStart)
		setAdapter(WeekViewPagerAdapter())
		addOnPageChangeListener(object : OnPageChangeListener {
			override fun onPageScrolled(position: Int,
			                            positionOffset: Float,
			                            positionOffsetPixels: Int) {
			}
			
			override fun onPageSelected(position: Int) { //默认的显示星期四，周视图切换就显示星期4
				if (visibility != VISIBLE || isUsingScrollToCalendar) {
					isUsingScrollToCalendar = false
					return
				}
				findViewWithTag<BaseWeekView?>(position)?.run {
					performClickCalendar(if (mDelegate.selectMode != CalendarViewDelegate.SELECT_MODE_DEFAULT) mDelegate.mIndexCalendar else mDelegate.mSelectedCalendar, !isUsingScrollToCalendar)
					mDelegate.mWeekChangeListener?.onWeekChange(currentWeekCalendars)
				}
				isUsingScrollToCalendar = false
			}
			
			override fun onPageScrollStateChanged(state: Int) {
			}
		})
	}
	
	val currentWeekCalendars: MutableList<Calendar?>
		/**
		 * 获取当前周数据
		 * 
		 * @return 获取当前周数据
		 */
		get() {
			val calendars = CalendarUtil.getWeekCalendars(mDelegate!!.mIndexCalendar, mDelegate)
			mDelegate!!.addSchemesFromMap(calendars)
			return calendars
		}
	
	/**
	 * 更新周视图
	 */
	fun notifyDataSetChanged() {
		mWeekCount = CalendarUtil.getWeekCountBetweenBothCalendar(mDelegate!!.minYear, mDelegate!!.minYearMonth, mDelegate!!.minYearDay, mDelegate!!.maxYear, mDelegate!!.maxYearMonth, mDelegate!!.maxYearDay, mDelegate!!.weekStart)
		notifyAdapterDataSetChanged()
	}
	
	/**
	 * 更新周视图布局
	 */
	fun updateWeekViewClass() {
		isUpdateWeekView = true
		notifyAdapterDataSetChanged()
		isUpdateWeekView = false
	}
	
	/**
	 * 更新日期范围
	 */
	fun updateRange() {
		isUpdateWeekView = true
		notifyDataSetChanged()
		isUpdateWeekView = false
		if (visibility == VISIBLE) {
			isUsingScrollToCalendar = true
			val calendar = mDelegate!!.mSelectedCalendar
			updateSelected(calendar, false)
			mDelegate!!.mInnerListener?.onWeekDateSelected(calendar, false)
			mDelegate!!.mCalendarSelectListener?.onCalendarSelect(calendar, false)
			mParentLayout!!.updateSelectWeek(CalendarUtil.getWeekFromDayInMonth(calendar, mDelegate!!.weekStart))
		}
	}
	
	/**
	 * 滚动到指定日期
	 * 
	 * @param year  年
	 * @param month 月
	 * @param day   日
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
			this.isCurrentDay = this == mDelegate!!.currentDay
		}
		LunarCalendar.setupLunarCalendar(calendar)
		mDelegate!!.mIndexCalendar = calendar
		mDelegate!!.mSelectedCalendar = calendar
		mDelegate!!.updateSelectCalendarScheme()
		updateSelected(calendar, smoothScroll)
		mDelegate!!.mInnerListener?.onWeekDateSelected(calendar, false)
		
		if (invokeListener) mDelegate!!.mCalendarSelectListener?.onCalendarSelect(calendar, false)
		val i = CalendarUtil.getWeekFromDayInMonth(calendar, mDelegate!!.weekStart)
		mParentLayout!!.updateSelectWeek(i)
	}
	
	/**
	 * 滚动到当前
	 */
	fun scrollToCurrent(smoothScroll: Boolean) {
		isUsingScrollToCalendar = true
		val position = CalendarUtil.getWeekFromCalendarStartWithMinCalendar(mDelegate!!.currentDay, mDelegate!!.minYear, mDelegate!!.minYearMonth, mDelegate!!.minYearDay, mDelegate!!.weekStart) - 1
		if (currentItem == position) {
			isUsingScrollToCalendar = false
		}
		setCurrentItem(position, smoothScroll)
		findViewWithTag<BaseWeekView?>(position)?.run {
			performClickCalendar(mDelegate.currentDay, false)
			setSelectedCalendar(mDelegate.currentDay)
			invalidate()
		}
		
		if (visibility == VISIBLE) {
			mDelegate!!.mCalendarSelectListener?.onCalendarSelect(mDelegate!!.mSelectedCalendar, false)
			mDelegate!!.mInnerListener?.onWeekDateSelected(mDelegate!!.currentDay, false)
		}
		mParentLayout!!.updateSelectWeek(CalendarUtil.getWeekFromDayInMonth(mDelegate!!.currentDay, mDelegate!!.weekStart))
	}
	
	/**
	 * 更新任意一个选择的日期
	 */
	fun updateSelected(calendar: Calendar, smoothScroll: Boolean) {
		val position = CalendarUtil.getWeekFromCalendarStartWithMinCalendar(calendar, mDelegate!!.minYear, mDelegate!!.minYearMonth, mDelegate!!.minYearDay, mDelegate!!.weekStart) - 1
		isUsingScrollToCalendar = currentItem != position
		setCurrentItem(position, smoothScroll)
		findViewWithTag<BaseWeekView?>(position)?.run {
			setSelectedCalendar(calendar)
			invalidate()
		}
	}
	
	/**
	 * 更新单选模式
	 */
	fun updateSingleSelect() {
		if (mDelegate!!.selectMode != CalendarViewDelegate.SELECT_MODE_DEFAULT) children.forEach {
			(it as BaseWeekView).updateSingleSelect()
		}
	}
	
	/**
	 * 更新为默认选择模式
	 */
	fun updateDefaultSelect() {
		findViewWithTag<BaseWeekView?>(this.currentItem)?.run {
			setSelectedCalendar(mDelegate.mSelectedCalendar)
			invalidate()
		}
	}
	
	/**
	 * 更新选择效果
	 */
	fun updateSelected() {
		children.forEach {
			(it as BaseWeekView).apply {
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
			(it as BaseWeekView).apply {
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
			(it as BaseWeekView).apply {
				update()
			}
		}
	}
	
	/**
	 * 更新当前日期，夜间过度的时候调用这个函数，一般不需要调用
	 */
	fun updateCurrentDate() {
		children.forEach {
			(it as BaseWeekView).apply {
				updateCurrentDate()
			}
		}
	}
	
	/**
	 * 更新显示模式
	 */
	fun updateShowMode() {
		children.forEach {
			(it as BaseWeekView).apply {
				updateShowMode()
			}
		}
	}
	
	/**
	 * 更新周起始
	 */
	fun updateWeekStart() {
		if (adapter == null) {
			return
		}
		val count = adapter!!.count
		mWeekCount = CalendarUtil.getWeekCountBetweenBothCalendar(mDelegate!!.minYear, mDelegate!!.minYearMonth, mDelegate!!.minYearDay, mDelegate!!.maxYear, mDelegate!!.maxYearMonth, mDelegate!!.maxYearDay, mDelegate!!.weekStart)/*
         * 如果count发生变化，意味着数据源变化，则必须先调用notifyDataSetChanged()，
         * 否则会抛出异常
         */
		if (count != mWeekCount) {
			isUpdateWeekView = true
			adapter!!.notifyDataSetChanged()
		}
		children.forEach {
			(it as BaseWeekView).apply {
				updateWeekStart()
			}
		}
		isUpdateWeekView = false
		updateSelected(mDelegate!!.mSelectedCalendar, false)
	}
	
	/**
	 * 更新高度
	 */
	fun updateItemHeight() {
		children.forEach {
			(it as BaseWeekView).apply {
				updateItemHeight()
				requestLayout()
			}
		}
	}
	
	/**
	 * 清除选择范围
	 */
	fun clearSelectRange() {
		children.forEach {
			(it as BaseWeekView).apply {
				invalidate()
			}
		}
	}
	
	fun clearSingleSelect() {
		children.forEach {
			(it as BaseWeekView).apply {
				mCurrentItem = -1
				invalidate()
			}
		}
	}
	
	fun clearMultiSelect() {
		children.forEach {
			(it as BaseWeekView).apply {
				mCurrentItem = -1
				invalidate()
			}
		}
	}
	
	private fun notifyAdapterDataSetChanged() {
		adapter?.notifyDataSetChanged()
	}
	
	override fun onTouchEvent(ev: MotionEvent?): Boolean {
		return mDelegate!!.isWeekViewScrollable && super.onTouchEvent(ev)
	}
	
	override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
		return mDelegate!!.isWeekViewScrollable && super.onInterceptTouchEvent(ev)
	}
	
	/**
	 * 周视图的高度应该与日历项的高度一致
	 */
	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(mDelegate!!.calendarItemHeight, MeasureSpec.EXACTLY))
	}
	
	/**
	 * 周视图切换
	 */
	private inner class WeekViewPagerAdapter : PagerAdapter() {
		override fun getCount(): Int = mWeekCount
		override fun getItemPosition(o: Any): Int {
			return if (isUpdateWeekView) POSITION_NONE else super.getItemPosition(o)
		}
		
		override fun isViewFromObject(view: View, o: Any): Boolean {
			return view == o
		}
		
		override fun instantiateItem(container: ViewGroup, position: Int): Any {
			val calendar = CalendarUtil.getFirstCalendarStartWithMinCalendar(mDelegate!!.minYear, mDelegate!!.minYearMonth, mDelegate!!.minYearDay, position + 1, mDelegate!!.weekStart)
			val view: BaseWeekView
			try {
				val constructor: Constructor<*> = mDelegate!!.weekViewClass.getConstructor(Context::class.java)
				view = constructor.newInstance(context) as BaseWeekView
			} catch (_: Exception) {
				return DefaultWeekView(context)
			}
			view.mParentLayout = mParentLayout
			view.setup(mDelegate)
			view.setup(calendar)
			view.tag = position
			view.setSelectedCalendar(mDelegate!!.mSelectedCalendar)
			container.addView(view)
			return view
		}
		
		override fun destroyItem(container: ViewGroup, position: Int, o: Any) {
			val view = o as BaseWeekView
			view.onDestroy()
			container.removeView(view)
		}
	}
}
