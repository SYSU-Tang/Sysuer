package com.sysu.edu.todo

import androidx.lifecycle.MutableLiveData

class TodoInfo {
	@JvmField val title: MutableLiveData<String?> = MutableLiveData<String?>()
	@JvmField val description: MutableLiveData<String?> = MutableLiveData<String?>()
	@JvmField val dueDate: MutableLiveData<String?> = MutableLiveData<String?>()
	@JvmField val ddlDate: MutableLiveData<String?> = MutableLiveData<String?>()
	@JvmField val dueTime: MutableLiveData<String?> = MutableLiveData<String?>()
	@JvmField val remindTime: MutableLiveData<String?> = MutableLiveData<String?>()
	@JvmField val type: MutableLiveData<String?> = MutableLiveData<String?>()
	@JvmField val location: MutableLiveData<String?> = MutableLiveData<String?>()
	@JvmField val subject: MutableLiveData<String?> = MutableLiveData<String?>()
	
	/**
	 * 获取优先级
	 * 0: 无优先级
	 * 1: 不重要且不紧急
	 * 2: 不重要且紧急
	 * 3: 重要且不紧急
	 * 4: 重要且紧急
	 * 
	 * @return 优先级
	 */
	@JvmField val priority: MutableLiveData<Int?> = MutableLiveData<Int?>()
	val subtask: MutableLiveData<String?> = MutableLiveData<String?>()
	val attachment: MutableLiveData<String?> = MutableLiveData<String?>()
	val doneDate: MutableLiveData<String?> = MutableLiveData<String?>()
	@JvmField val status: MutableLiveData<Int?> = MutableLiveData<Int?>(0)
	val color: MutableLiveData<String?> = MutableLiveData<String?>()
	@JvmField val tag: MutableLiveData<String?> = MutableLiveData<String?>()
	@JvmField val id: MutableLiveData<Int?> = MutableLiveData<Int?>(0)
	@JvmField var function: Int = ADD
	
	init {
		reset()
	}
	
	fun setDdlDate(ddlDate: String?) {
		this.ddlDate.value = ddlDate
	}
	
	fun setDueTime(dueTime: String?) {
		this.dueTime.value = dueTime
	}
	
	fun setRemindTime(remindTime: String?) {
		this.remindTime.value = remindTime
	}
	
	fun setType(type: String?) {
		this.type.value = type
	}
	
	fun setLocation(location: String?) {
		this.location.value = location
	}
	
	fun setTitle(title: String?) {
		this.title.value = title
	}
	
	fun setDescription(description: String?) {
		this.description.value = description
	}
	
	fun setDueDate(dueDate: String?) {
		this.dueDate.value = dueDate
	}
	
	/**
	 * 设置优先级
	 * 0: 无优先级
	 * 1: 不重要且不紧急
	 * 2: 不重要且紧急
	 * 3: 重要且不紧急
	 * 4: 重要且紧急
	 * 
	 * @param priority 优先级
	 */
	fun setPriority(priority: Int?) {
		this.priority.value = priority
	}
	
	fun setSubject(subject: String?) {
		this.subject.value = subject
	}
	
	fun setSubtask(subtask: String?) {
		this.subtask.value = subtask
	}
	
	fun setAttachment(attachment: String?) {
		this.attachment.value = attachment
	}
	
	fun setStatus(status: Int?) {
		this.status.value = status
	}
	
	fun setColor(color: String?) {
		this.color.value = color
	}
	
	fun setTag(tag: String?) {
		this.tag.value = tag
	}
	
	fun setDoneDate(doneDate: String?) {
		this.doneDate.value = doneDate
	}
	
	fun setId(id: Int?) {
		this.id.value = id
	}
	
	fun reset() {
		setTitle("")
		setDescription("")
		setDueDate("")
		setDdlDate("")
		setDueTime("")
		setPriority(null)
		setRemindTime("")
		setType("")
		setLocation("")
		setSubject("")
		setSubtask("")
		setAttachment("")
		setDoneDate("")
		setStatus(0)
		setColor("")
		setTag("")
		function = ADD
	}
	
	fun copyFrom(todoInfo: TodoInfo) {
		title.value = todoInfo.title.value
		description.value = todoInfo.description.value
		dueDate.value = todoInfo.dueDate.value
		ddlDate.value = todoInfo.ddlDate.value
		dueTime.value = todoInfo.dueTime.value
		priority.value = todoInfo.priority.value
		remindTime.value = todoInfo.remindTime.value
		type.value = todoInfo.type.value
		location.value = todoInfo.location.value
		subject.value = todoInfo.subject.value
		subtask.value = todoInfo.subtask.value
		attachment.value = todoInfo.attachment.value
		doneDate.value = todoInfo.doneDate.value
		status.value = todoInfo.status.value
		color.value = todoInfo.color.value
		tag.value = todoInfo.tag.value
		id.value = todoInfo.id.value
		function = todoInfo.function
	}
	
	companion object {
		const val ADD: Int = 0
		const val VIEW: Int = 1
		const val TODO: Int = 0
		const val DONE: Int = 1
		const val DELETE: Int = 2
	}
}