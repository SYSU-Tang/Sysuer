package com.miyuyan.sysuer.todo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class TodoModel(application: Application) : AndroidViewModel(application) {
	val repository: TodoRepository = TodoRepository(
			TodoDatabase.getDatabase(application, viewModelScope).todoDao()
	)
	val types: MutableLiveData<List<TypeEntity>> = MutableLiveData()
	val subjects: MutableLiveData<List<SubjectEntity>> = MutableLiveData()
	val tags: MutableLiveData<List<TagEntity>> = MutableLiveData()
	val todoList: MutableLiveData<List<TodoEntity>> = MutableLiveData()

	/**
	 * 日历圆点标记：所有待办（不受筛选影响）的 dueDate / ddl 并集。
	 *
	 * 单独一份全量查询，否则一筛选日历上的点就跟着没了。
	 * 存 `yyyy-MM-dd` 字符串而不是 CalendarDay，让 ViewModel 不依赖日历库。
	 */
	val calendarMarks: MutableLiveData<Set<String>> = MutableLiveData()

	fun loadTodos(where: String, args: Array<String>): Job = viewModelScope.launch {
		todoList.value = repository.getTodo(where, args)
	}

	fun loadTodos(where: String, args: List<String>): Job =
		loadTodos(where, args.toTypedArray())

	/** 重跑全量日期标记，只在增删改后调用即可 */
	fun loadCalendarMarks(): Job = viewModelScope.launch {
		val all: List<TodoEntity> = repository.getTodo()
		val dates = all.flatMap { todo ->
			listOfNotNull(
					todo.dueDate?.takeIf { it.isNotEmpty() },
					todo.ddl?.takeIf { it.isNotEmpty() }
			)
		}
		calendarMarks.value = dates.toSet()
	}

	fun addTodo(todoInfo: TodoEntity): Deferred<Long> = viewModelScope.async {
		repository.addTodo(todoInfo)
	}

	fun updateTodo(todoInfo: TodoEntity): Job = viewModelScope.launch {
		repository.updateTodo(todoInfo)
	}

	fun deleteTodo(id: Int): Job = viewModelScope.launch {
		repository.deleteTodo(id)
	}

	fun addType(type: String): Job = viewModelScope.launch {
		repository.addType(type)
		loadTypes()
	}

	fun addTag(tag: String): Job = viewModelScope.launch {
		repository.addTag(tag)
		loadTags()
	}

	fun addSubject(subject: String): Job = viewModelScope.launch {
		repository.addSubject(subject)
		loadSubjects()
	}

	fun deleteType(type: String): Job = viewModelScope.launch {
		repository.deleteType(type)
		loadTypes()
	}

	fun deleteTag(tag: String): Job = viewModelScope.launch {
		repository.deleteTag(tag)
		loadTags()
	}

	fun deleteSubject(subject: String): Job = viewModelScope.launch {
		repository.deleteSubject(subject)
		loadSubjects()
	}

	fun loadTypes(): Job = viewModelScope.launch {
		types.value = repository.getTypes()
	}

	fun loadSubjects(): Job = viewModelScope.launch {
		subjects.value = repository.getSubjects()
	}

	fun loadTags(): Job = viewModelScope.launch {
		tags.value = repository.getTags()
	}
}
