package com.sysu.edu.todo

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.sysu.edu.R
import com.sysu.edu.databinding.ItemTodoBinding
import com.sysu.edu.view.RecyclerAdapter

class TodoAdapter : RecyclerAdapter<TodoEntity>() {
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
		object :
			RecyclerView.ViewHolder(ItemTodoBinding.inflate(LayoutInflater.from(parent.context), parent, false).root) {}
	
	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		val binding = ItemTodoBinding.bind(holder.itemView)
		val context = binding.root.context
		val resource = context.resources
		val item = get(position)
		
		binding.title.text = item.title
		val itemColor = item.color?.takeIf { it.isNotEmpty() }?.toColorInt()
		if (itemColor != null) {
			binding.title.setTextColor(itemColor)
			binding.type.background?.mutate()?.setTint(itemColor)
		}
		else {
			binding.title.setTextColor(MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, Color.BLACK))
			binding.type.background?.mutate()?.setTintList(null)
		}
		val description = item.description
		if (description.isNullOrEmpty()) {
			binding.description.visibility = View.GONE
		}
		else {
			binding.description.visibility = View.VISIBLE
			binding.description.text = description
		}
		val type = item.todoType
		if (type.isNullOrEmpty()) {
			binding.type.visibility = View.GONE
		}
		else {
			binding.type.visibility = View.VISIBLE
			binding.type.text = type
		}
		val content = StringBuilder()
		mapOf(R.string.location to item.location, R.string.subject to item.subject, R.string.ddl to item.ddl, R.string.remind to item.remindTime).forEach { (key: Int, value: String?) ->
			if (!value.isNullOrEmpty()) content.append("${resource.getString(key)}:$value|")
		}
		val priority = item.priority
		if (priority > 0) content.append(resource.getStringArray(R.array.priority)[priority])
		
		if (content.isNotEmpty()) {
			binding.detailContent.visibility = View.VISIBLE
			binding.detailContent.text = "$content".substring(0, content.length - 1)
		}
		else {
			binding.detailContent.visibility = View.GONE
		}
		val isDone = TodoInfo.DONE == item.status
		binding.title.alpha = if (isDone) 0.5f else 1.0f
		binding.description.alpha = if (isDone) 0.5f else 1.0f
		binding.check.isChecked = isDone
		binding.title.paintFlags = if (isDone) binding.title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG else binding.title.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
		binding.description.paintFlags = if (isDone) binding.description.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG else binding.description.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
		
		super.onBindViewHolder(holder, position)
	}
}
