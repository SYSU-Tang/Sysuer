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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar

/**
 * 年份布局选择View
 */
class YearRecyclerView(context: Context, attrs: AttributeSet? = null) :
	RecyclerView(context, attrs) {
	private val mAdapter: YearViewAdapter = YearViewAdapter(context).apply {
		setOnItemClickListener(object : BaseRecyclerAdapter.OnItemClickListener {
			override fun onItemClick(position: Int, itemId: Long) {
				getItem(position)?.run {
					if (CalendarUtil.isMonthInRange(year, month, mDelegate!!.minYear, mDelegate!!.minYearMonth, mDelegate!!.maxYear, mDelegate!!.maxYearMonth)) {
						mListener?.onMonthSelected(year, month)
						mDelegate!!.mYearViewChangeListener?.onYearViewChange(true)
					}
				}
			}
		})
	}
	private var mDelegate: CalendarViewDelegate? = null
	private var mListener: OnMonthSelectedListener? = null
	
	init {
		setLayoutManager(GridLayoutManager(context, 3))
		setAdapter(mAdapter)
	}
	
	/**
	 * 设置
	 * 
	 * @param delegate delegate
	 */
	fun setup(delegate: CalendarViewDelegate) {
		mDelegate = delegate
		mAdapter.setup(delegate)
	}
	
	/**
	 * 初始化年视图
	 * 
	 * @param year year
	 */
	fun init(year: Int) {
		val date = Calendar.getInstance()
		(1..12).forEach { i ->
			date.set(year, i - 1, 1)
			mAdapter.addItem(Month().apply {
				diff = CalendarUtil.getMonthViewStartDiff(year, i, mDelegate!!.weekStart)
				count = CalendarUtil.getMonthDaysCount(year, i)
				this.month = i
				this.year = year
			})
		}
	}
	
	/**
	 * 更新周起始
	 */
	fun updateWeekStart() {
		mAdapter.items.forEach { month ->
			month?.let {
				it.diff = CalendarUtil.getMonthViewStartDiff(it.year, it.month, mDelegate?.weekStart
					?: 0)
			}
		}
	}
	
	/**
	 * 更新字体颜色大小
	 */
	fun updateStyle() {
		(0..<childCount).forEach {
			(getChildAt(it) as YearView).apply {
				updateStyle()
				invalidate()
			}
		}
	}
	
	/**
	 * 月份选择事件
	 * 
	 * @param listener listener
	 */
	fun setOnMonthSelectedListener(listener: OnMonthSelectedListener?) {
		mListener = listener
	}
	
	fun notifyAdapterDataSetChanged() {
		adapter?.notifyDataSetChanged()
	}
	
	override fun onMeasure(widthSpec: Int, heightSpec: Int) {
		super.onMeasure(widthSpec, heightSpec)
		mAdapter.setYearViewSize(MeasureSpec.getSize(widthSpec) / 3, MeasureSpec.getSize(heightSpec) / 4)
	}
	
	interface OnMonthSelectedListener {
		fun onMonthSelected(year: Int, month: Int)
	}
}
