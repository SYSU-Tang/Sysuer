package com.sysu.edu.view

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.sysu.edu.databinding.ItemButtonGroupBinding
import com.sysu.edu.databinding.ItemButtonOutlineBinding

class ButtonAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder?>() {
	private val data = mutableListOf<String?>()
	private var onBindListener: OnBindListener? = null
	
	fun setListener(listener: OnBindListener?) {
		onBindListener = listener
	}
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		val itemView = ItemButtonGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false).root
		data.forEachIndexed { i, d ->
			itemView.addView(ItemButtonOutlineBinding.inflate(LayoutInflater.from(parent.context), parent, false).root.apply {
				text = d
				onBindListener?.onBind(this, i)
			})
		}
		return object : RecyclerView.ViewHolder(itemView) {}
	}
	
	fun add(text: String?) {
		data.add(text)
		notifyItemChanged(0)
	}
	
	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
	}
	
	override fun getItemCount(): Int = 1
}

fun interface OnBindListener {
	fun onBind(button: Button, position: Int)
}