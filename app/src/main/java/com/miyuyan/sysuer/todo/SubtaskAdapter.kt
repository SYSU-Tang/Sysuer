package com.miyuyan.sysuer.todo

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.textfield.TextInputEditText
import com.miyuyan.sysuer.databinding.ItemSubtaskBinding
import com.miyuyan.sysuer.view.RecyclerAdapter

class SubtaskAdapter(private val onDataChanged: (SubtaskAdapter) -> Unit) :
	RecyclerAdapter<JSONObject>() {
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		return object :
			RecyclerView.ViewHolder(ItemSubtaskBinding.inflate(LayoutInflater.from(parent.context), parent, false).root) {}
	}
	
	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		val item = get(position)
		val isDone = item.getIntValue("status", TodoInfo.TODO) == TodoInfo.DONE
		ItemSubtaskBinding.bind(holder.itemView).apply {
			root.updateAppearance(position + 1, itemCount + 1)			// 1. Clear listeners to avoid triggering during state update
			check.setOnCheckedChangeListener(null)			// 2. Set content
			title.setText(item.getString("title") ?: "")
			check.isChecked = isDone
			updateTitleStyle(title, isDone)			// 3. Set fresh listeners
			check.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
				item["status"] = if (isChecked) TodoInfo.DONE else TodoInfo.TODO
				updateTitleStyle(title, isChecked)
				onDataChanged(this@SubtaskAdapter)
			}
			
			delete.setOnClickListener {
				remove(position)
				onDataChanged(this@SubtaskAdapter)
			}
			title.addTextChangedListener {
				item["title"] = "$it"
				onDataChanged(this@SubtaskAdapter)
			}
		}
		
		super.onBindViewHolder(holder, position)
	}
	
	private fun updateTitleStyle(title: TextInputEditText, isDone: Boolean) {
		title.alpha = if (isDone) 0.5f else 1.0f
		title.paintFlags = (if (isDone) title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
		else title.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv())
	}
}
