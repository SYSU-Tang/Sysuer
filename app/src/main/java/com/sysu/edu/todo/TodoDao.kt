package com.sysu.edu.todo

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.RawQuery
import androidx.room3.RoomRawQuery
import androidx.room3.Update

@Dao interface TodoDao {
	@Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertTodo(todo: TodoEntity): Long
	@Update suspend fun updateTodo(todo: TodoEntity)
	@Query("DELETE FROM todos WHERE id = :id") suspend fun deleteTodoById(id: Int)
	@Query("SELECT * FROM todos") suspend fun queryTodo(): List<TodoEntity>
	
	@RawQuery suspend fun queryTodo(query: RoomRawQuery): List<TodoEntity>
	@Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertType(type: TypeEntity)
	@Query("INSERT OR IGNORE INTO types (name, color) VALUES (:name, :color)")
	suspend fun insertType(name: String?, color: String? = null)
	@Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertTypes(types: List<TypeEntity>)
	@Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertTags(tags: List<TagEntity>)
	@Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertTag(tag: TagEntity)
	@Query("INSERT OR IGNORE INTO tags (name, color) VALUES (:name, :color)")
	suspend fun insertTag(name: String?, color: String? = null)
	
	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun insertSubjects(subjects: List<SubjectEntity>)
	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun insertSubject(subject: SubjectEntity)
	@Query("INSERT OR IGNORE INTO subjects (name, color) VALUES (:name, :color)")
	suspend fun insertSubject(name: String?, color: String? = null)
	@Query("DELETE FROM types WHERE name = :name") suspend fun deleteType(name: String)
	@Query("DELETE FROM tags WHERE name = :name") suspend fun deleteTag(name: String)
	@Query("DELETE FROM subjects WHERE name = :name") suspend fun deleteSubject(name: String)
	@Query("SELECT * FROM types") suspend fun queryTypes(): List<TypeEntity>
	@Query("SELECT * FROM tags") suspend fun queryTags(): List<TagEntity>
	@Query("SELECT * FROM subjects") suspend fun querySubjects(): List<SubjectEntity>
}