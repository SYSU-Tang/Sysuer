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
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import java.lang.reflect.Constructor

internal class YearViewAdapter(context: Context) : BaseRecyclerAdapter<Month?>(context) {
	private var mDelegate: CalendarViewDelegate? = null
	private var mItemWidth = 0
	private var mItemHeight = 0
	fun setup(delegate: CalendarViewDelegate) {
		mDelegate = delegate
	}
	
	fun setYearViewSize(width: Int, height: Int) {
		mItemWidth = width
		mItemHeight = height
	}
	
	override fun onCreateDefaultViewHolder(parent: ViewGroup?, type: Int): RecyclerView.ViewHolder {
		var yearView: YearView?
		if (TextUtils.isEmpty(mDelegate!!.yearViewClassPath)) yearView = DefaultYearView(mContext)
		else try {
			val constructor: Constructor<*> = mDelegate!!.yearViewClass
				.getConstructor(Context::class.java)
			yearView = constructor.newInstance(mContext) as YearView
		} catch (_: Exception) {
			yearView = DefaultYearView(mContext)
		}
		yearView.setLayoutParams(RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.MATCH_PARENT))
		return YearViewHolder(yearView, mDelegate)
	}
	
	override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, item: Month?, position: Int) {
		val view = (holder as YearViewHolder).mYearView
		view.init(item?.year ?: 0, item?.month ?: 0)
		view.measureSize(mItemWidth, mItemHeight)
	}
	
	private class YearViewHolder(itemView: View, delegate: CalendarViewDelegate?) :
		RecyclerView.ViewHolder(itemView) {
		val mYearView: YearView = itemView as YearView
		
		init {
			mYearView.setup(delegate)
		}
	}
}
