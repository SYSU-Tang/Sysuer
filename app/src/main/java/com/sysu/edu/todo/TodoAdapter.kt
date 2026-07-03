package com.sysu.edu.todo

import android.graphics.Paint
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.databinding.ItemTodoBinding
import com.sysu.edu.view.RecyclerAdapter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TodoAdapter(private val todoManager: TodoManager) : RecyclerAdapter<TodoInfo>() {
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		return object :
			RecyclerView.ViewHolder(ItemTodoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
										.root) {}
	}
	
	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		val binding = ItemTodoBinding.bind(holder.itemView)
		val context = binding.root.context
		val resource = context.resources
		val item = get(position)
		binding.title.text = item.title.value
		val description = item.description.value
		if (TextUtils.isEmpty(description)) binding.description.visibility = View.GONE
		else binding.description.text = description
		val type = item.type.value
		if (TextUtils.isEmpty(type)) binding.type.visibility = View.GONE
		else binding.type.text = type
		val content = StringBuilder()
		mapOf(R.string.location to item.location.value, R.string.subject to item.subject.value, R.string.ddl to item.ddlDate.value, R.string.remind to item.remindTime.value).forEach { (key: Int, value: String?) ->
				if (!CommonUtil.isEmpty(value)) content.append("${resource.getString(key)}:$value|")
			}
		val priority = item.priority.value
		if (priority != null && priority > 0) content.append(resource.getStringArray(R.array.priority)[priority])
		if (!TextUtils.isEmpty(content)) binding.detailContent.text = "$content".substring(0, content.length - 1)
		else binding.detailContent.visibility = View.GONE
		binding.check.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean -> item.setStatus(if (isChecked) TodoInfo.DONE else TodoInfo.TODO) }
		binding.title.alpha=if (TodoInfo.DONE == item.status.value) 0.5f else 1.0f
		binding.description.alpha=if (TodoInfo.DONE == item.status.value) 0.5f else 1.0f
		item.status.observe(context as FragmentActivity, Observer { status: Int? ->
			val isCheck = TodoInfo.DONE == status
			binding.title.alpha=if (isCheck) 0.5f else 1.0f
			binding.description.alpha=if (isCheck) 0.5f else 1.0f
			binding.check.isChecked = isCheck
			binding.title.paintFlags = if (isCheck) binding.title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG else binding.title.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
			binding.description.paintFlags = if (isCheck) binding.description.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG else binding.description.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv() //            binding.menu.setEnabled(!isCheck);
			//binding.check.setChecked(isChecked);
			//            binding.title.setPaintFlags(isChecked ? binding.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG : binding.title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
			//            binding.description.setPaintFlags(isChecked ? binding.description.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG : binding.description.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
			//            binding.menu.setEnabled(!isChecked);
			item.setDoneDate(if (isCheck) LocalDateTime.now()
				.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
							 else null)
			todoManager.updateTodo(item)
		})
		super.onBindViewHolder(holder, position) //binding.dueDate.setText(item.get("due_date"));
	}
}
