package com.sysu.edu.todo

import androidx.room3.RoomRawQuery

class TodoRepository(private val todoDao: TodoDao) {
	suspend fun addTodo(todoInfo: TodoEntity): Long {
		return todoDao.insertTodo(todoInfo)
	}
	
	suspend fun updateTodo(todoInfo: TodoEntity) {
		todoDao.updateTodo(todoInfo)
	}
	
	suspend fun getTodo(): List<TodoEntity> {
		return todoDao.queryTodo()
	}
	
	suspend fun getTodo(where: String,
	                    args: Array<String>): List<TodoEntity> {		// 构造原始查询。注意：$where 直接嵌入 SQL，args 通过 bindText 安全绑定
		val query = RoomRawQuery(sql = "SELECT * FROM todos WHERE $where", onBindStatement = { stmt ->
			args.forEachIndexed { index, arg ->                    // Room 的参数索引从 1 开始
				stmt.bindText(index + 1, arg)
			}
		})
		return todoDao.queryTodo(query)
	}
	
	suspend fun deleteTodo(id: Int) {
		todoDao.deleteTodoById(id)
	}
	
	suspend fun deleteTodo(todoInfo: TodoEntity) {
		todoDao.deleteTodoById(todoInfo.id)
	}
	
	suspend fun addType(type: String) {
		todoDao.insertType(name = type)
	}
	
	suspend fun addTag(tag: String) {
		todoDao.insertTag(name = tag)
	}
	
	suspend fun addSubject(subject: String) {
		todoDao.insertSubject(name = subject)
	}
	
	suspend fun deleteType(table: String) {
		todoDao.deleteType(table)
	}
	
	suspend fun deleteTag(table: String) {
		todoDao.deleteTag(table)
	}
	
	suspend fun deleteSubject(table: String) {
		todoDao.deleteSubject(table)
	}
	
	suspend fun getTypes(): List<TypeEntity> {
		return todoDao.queryTypes()
	}
	
	suspend fun getSubjects(): List<SubjectEntity> {
		return todoDao.querySubjects()
	}
	
	suspend fun getTags(): List<TagEntity> {
		return todoDao.queryTags()
	}
}
