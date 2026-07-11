package com.sysu.edu.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class TodoModelFactory(private val repository: TodoRepository) : ViewModelProvider.Factory {
	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass.isAssignableFrom(TodoModel::class.java)) return TodoModel(repository) as T
		throw IllegalArgumentException("Unknown ViewModel class")
	}
}