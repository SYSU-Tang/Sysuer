package com.sysu.edu.todo

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.sqlite.transaction
import com.sysu.edu.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TodoHelper(private val context: Context, version: Int) :
	SQLiteOpenHelper(context, "todo.db", null, version) {
	override fun onCreate(db: SQLiteDatabase) {
		db.execSQL("CREATE TABLE IF NOT EXISTS todos (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT, description TEXT, due_date TEXT, due_time TEXT, done_datetime TEXT, create_datetime TEXT DEFAULT CURRENT_TIMESTAMP, update_datetime TEXT DEFAULT CURRENT_TIMESTAMP, status INTEGER NOT NULL DEFAULT 0, priority INTEGER NOT NULL DEFAULT 0, todo_type TEXT, subtask TEXT NOT NULL, attachment TEXT NOT NULL, tag TEXT NOT NULL, subject TEXT, location TEXT, color TEXT, label TEXT, ddl TEXT, remind_time TEXT);")
		db.execSQL("CREATE TABLE IF NOT EXISTS types (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT, color TEXT);")
		db.execSQL("CREATE TABLE IF NOT EXISTS subjects (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT, color TEXT);")
		db.execSQL("CREATE TABLE IF NOT EXISTS tags (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT, color TEXT);")
		addType(db)
	}
	
	override fun onUpgrade(db: SQLiteDatabase, i: Int, i1: Int) {
		if (i <= 6) addType(db)
	}
	
	@JvmOverloads fun addType(db: SQLiteDatabase = writableDatabase) {
		db.transaction {
			val value = ContentValues()
			context.resources.getStringArray(R.array.todo_base_type).forEach {
				value.put("name", it)
				try {
					insertWithOnConflict("types", null, value, SQLiteDatabase.CONFLICT_ABORT)
				} catch (`_`: Exception) {
				}
				value.clear()
			}
		}
	}
	
	fun deleteTodo(id: String?) {
		writableDatabase.delete("todos", "id  = ?", arrayOf(id))
		close()
	}
	
	fun deleteTodo(todoInfo: TodoInfo) {
		deleteTodo(todoInfo.id.value.toString())
	}
	
	fun addTodo(todoInfo: TodoInfo) {
		value = setContentValues(todoInfo)
		writableDatabase.insert("todos", null, value)
		close()
	}
	
	fun updateTodo(todoInfo: TodoInfo) {
		value = setContentValues(todoInfo)
		writableDatabase.update("todos", value, "id = ?", arrayOf(todoInfo.id.value.toString()))
		close()
	}
	
	companion object {
		private var value = ContentValues()
		private fun setContentValues(todoInfo: TodoInfo): ContentValues {
			value.apply {
				clear()
				put("title", todoInfo.title.value)
				put("description", todoInfo.description.value)
				put("due_date", todoInfo.dueDate.value)
				put("status", todoInfo.status.value)
				put("priority", todoInfo.priority.value ?: 0)
				put("todo_type", todoInfo.type.value)
				put("subtask", todoInfo.subtask.value)
				put("attachment", todoInfo.attachment.value)
				put("subject", todoInfo.subject.value)
				put("location", todoInfo.location.value)
				put("color", todoInfo.color.value)
				put("label", todoInfo.tag.value)
				put("due_time", todoInfo.dueTime.value)
				put("remind_time", todoInfo.remindTime.value)
				put("done_datetime", todoInfo.doneDate.value)
				put("update_datetime", LocalDateTime.now()
					.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
				put("ddl", todoInfo.ddlDate.value)
			}
			
			return value
		}
	}
}
