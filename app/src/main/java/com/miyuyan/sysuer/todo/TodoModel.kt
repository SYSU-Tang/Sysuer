package com.miyuyan.sysuer.todo

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class TodoModel(private val repository: TodoRepository) : ViewModel() {
	val types: MutableLiveData<List<TypeEntity>> = MutableLiveData()
	val subjects: MutableLiveData<List<SubjectEntity>> = MutableLiveData()
	val tags: MutableLiveData<List<TagEntity>> = MutableLiveData()
	val todoList: MutableLiveData<List<TodoEntity>> = MutableLiveData()
	fun loadTodos(where: String, args: Array<String>): Job = viewModelScope.launch {
		todoList.value = repository.getTodo(where, args)
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
