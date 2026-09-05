package com.miyuyan.sysuer.todo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.R
import com.miyuyan.sysuer.databinding.ItemTitleBinding

class TitleAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder?> {
	@JvmField var title: String? = ""
	
	constructor(title: String?) {
		this.title = title
	}
	
	constructor(title: String?, n: Int) {
		setTitle(title, n)
	}
	
	fun setTitle(title: String?) {
		this.title = title
		notifyItemInserted(0)
	}
	
	fun setTitle(title: String?, n: Int) {
		this.title = title
		header = n
	}
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		return object :
			RecyclerView.ViewHolder(ItemTitleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
										.getRoot()) {}
	}
	
	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		ItemTitleBinding.bind(holder.itemView).title.apply {
			text = title
			setTextAppearance(if (0 <= header && header < headers.size) headers[header] else 0)
		}
	}
	
	override fun getItemCount(): Int {
		return 1
	}
	
	var header: Int = 0
		/**
		 * 设置标题样式
		 * 0: TextAppearance_Material3_TitleMedium
		 * 1: TextAppearance_Material3_TitleLarge_Emphasized
		 * 2: TextAppearance_Material3_TitleLarge
		 * 
		 */
		set(header) {
			field = header
			notifyItemChanged(0)
		}
	
	companion object {
		val headers: IntArray = intArrayOf(R.style.TextAppearance_Material3_TitleMedium, R.style.TextAppearance_Material3_TitleLarge_Emphasized, R.style.TextAppearance_Material3_TitleLarge)
	}
}
