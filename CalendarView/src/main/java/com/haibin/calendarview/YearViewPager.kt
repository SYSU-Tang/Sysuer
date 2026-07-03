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
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.haibin.calendarview.YearRecyclerView.OnMonthSelectedListener

/**
 * 年份+月份选择布局
 * ViewPager + RecyclerView
 */
class YearViewPager(context: Context, attrs: AttributeSet? = null) : ViewPager(context, attrs) {
	private var mYearCount = 0
	private var isUpdateYearView = false
	var mDelegate: CalendarViewDelegate? = null
	private var mListener: OnMonthSelectedListener? = null
	fun setup(delegate: CalendarViewDelegate) {
		mDelegate = delegate
		mYearCount = mDelegate!!.maxYear - mDelegate!!.minYear + 1
		setAdapter(object : PagerAdapter() {
			override fun getCount(): Int {
				return mYearCount
			}
			
			override fun getItemPosition(`object`: Any): Int {
				return if (isUpdateYearView) POSITION_NONE else super.getItemPosition(`object`)
			}
			
			override fun isViewFromObject(view: View, o: Any): Boolean {
				return view === o
			}
			
			override fun instantiateItem(container: ViewGroup, position: Int): Any {
				val view = YearRecyclerView(context).apply {
					mDelegate = delegate
					setOnMonthSelectedListener(mListener)
					init(position + mDelegate!!.minYear)
				}
				container.addView(view)
				return view
			}
			
			override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
				container.removeView(`object` as View)
			}
		})
		currentItem = mDelegate!!.currentDay.year - mDelegate!!.minYear
	}
	
	override fun setCurrentItem(item: Int) {
		setCurrentItem(item, false)
	}
	
	override fun setCurrentItem(item: Int, smoothScroll: Boolean) {
		super.setCurrentItem(item, false)
	}
	
	/**
	 * 通知刷新
	 */
	fun notifyDataSetChanged() {
		mYearCount = mDelegate!!.maxYear - mDelegate!!.minYear + 1
		adapter?.notifyDataSetChanged()
	}
	
	/**
	 * 滚动到某年
	 * 
	 * @param year         year
	 * @param smoothScroll smoothScroll
	 */
	fun scrollToYear(year: Int, smoothScroll: Boolean) {
		setCurrentItem(year - mDelegate!!.minYear, smoothScroll)
	}
	
	/**
	 * 更新日期范围
	 */
	fun updateRange() {
		isUpdateYearView = true
		notifyDataSetChanged()
		isUpdateYearView = false
	}
	
	/**
	 * 更新界面
	 */
	fun update() {
		(0..<childCount).forEach {
			(getChildAt(it) as YearRecyclerView).notifyAdapterDataSetChanged()
		}
	}
	
	/**
	 * 更新周起始
	 */
	fun updateWeekStart() {
		(0..<childCount).forEach { i ->
			(getChildAt(i) as YearRecyclerView).apply {
				updateWeekStart()
				notifyAdapterDataSetChanged()
			}
		}
	}
	
	/**
	 * 更新字体颜色大小
	 */
	fun updateStyle() {
		(0..<childCount).forEach {
			(getChildAt(it) as YearRecyclerView).updateStyle()
		}
	}
	
	fun setOnMonthSelectedListener(listener: OnMonthSelectedListener?) {
		mListener = listener
	}
	
	override fun onMeasure(widthMeasureSpec: Int,
	                       heightMeasureSpec: Int) { //heightMeasureSpec = MeasureSpec.makeMeasureSpec(getHeight(getContext(), this), MeasureSpec.EXACTLY);
		super.onMeasure(widthMeasureSpec, heightMeasureSpec)
	}
	
	@SuppressLint("ClickableViewAccessibility")
	override fun onTouchEvent(ev: MotionEvent?): Boolean {
		return mDelegate!!.isYearViewScrollable && super.onTouchEvent(ev)
	}
	
	override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
		return mDelegate!!.isYearViewScrollable && super.onInterceptTouchEvent(ev)
	}
	
	companion object {
		/**
		 * 计算相对高度
		 * 
		 * @param context context
		 * @param view    view
		 * @return 年月视图选择器最适合的高度
		 */
		private fun getHeight(context: Context, view: View): Int {
			val dm = DisplayMetrics()
			ContextCompat.getSystemService(context, WindowManager::class.java)?.defaultDisplay?.getMetrics(dm)
			val h = dm.heightPixels
			val location = IntArray(2)
			view.getLocationInWindow(location)
			view.getLocationOnScreen(location)
			return h - location[1]
		}
	}
}
