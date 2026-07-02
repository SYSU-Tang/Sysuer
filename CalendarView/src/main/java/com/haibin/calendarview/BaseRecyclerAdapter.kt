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
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * 基本的适配器
 */
internal abstract class BaseRecyclerAdapter<T>(val mContext: Context) :
	RecyclerView.Adapter<RecyclerView.ViewHolder?>() {
	val items = mutableListOf<T?>()
	private var onItemClickListener: OnItemClickListener? = null
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		return onCreateDefaultViewHolder(parent, viewType)?.apply {
			itemView.tag = this
			itemView.setOnClickListener(object : OnClickListener() {
				override fun onClick(position: Int, itemId: Long) {
					if (onItemClickListener != null) onItemClickListener!!.onItemClick(position, itemId)
				}
			})
		}!!
	}
	
	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		onBindViewHolder(holder, items[position], position)
	}
	
	abstract fun onCreateDefaultViewHolder(parent: ViewGroup?, type: Int): RecyclerView.ViewHolder?
	abstract fun onBindViewHolder(holder: RecyclerView.ViewHolder?, item: T?, position: Int)
	override fun getItemCount(): Int {
		return items.size
	}
	
	fun setOnItemClickListener(onItemClickListener: OnItemClickListener?) {
		this.onItemClickListener = onItemClickListener
	}
	
	@Suppress("unused") fun addAll(items: MutableList<out T?>?) {
		if (!items.isNullOrEmpty()) {
			this.items.addAll(items)
			notifyItemRangeInserted(items.size, items.size)
		}
	}
	
	fun addItem(item: T?) {
		if (item != null) {
			items.add(item)
			notifyItemChanged(items.size)
		}
	}
	
	fun getItem(position: Int): T? {
		return if (position < 0 || position >= items.size) null else items[position]
	}
	
	internal interface OnItemClickListener {
		fun onItemClick(position: Int, itemId: Long)
	}
	
	internal abstract class OnClickListener : View.OnClickListener {
		override fun onClick(v: View) {
			val holder = v.tag as RecyclerView.ViewHolder
			onClick(holder.getBindingAdapterPosition(), holder.itemId)
		}
		
		abstract fun onClick(position: Int, itemId: Long)
	}
}
