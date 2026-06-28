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
		//db = context.openOrCreateDatabase("todo.db", Context.MODE_PRIVATE, null);
		//db.execSQL("Drop table if exists types");
		db.execSQL("CREATE TABLE IF NOT EXISTS todos (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, description TEXT, due_date DATETIME,due_time DATETIME, done_datetime DATETIME, create_datetime DATETIME DEFAULT CURRENT_TIMESTAMP, update_datetime DATETIME DEFAULT CURRENT_TIMESTAMP, status INTEGER DEFAULT 0, priority INTEGER DEFAULT 0, todo_type TEXT,subtask TEXT,attachment TEXT,subject TEXT, location TEXT,color TEXT,label TEXT,ddl DATETIME,remind_time TEXT);")
		db.execSQL("CREATE TABLE IF NOT EXISTS types (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE, color TEXT);")
		db.execSQL("CREATE TABLE IF NOT EXISTS subjects (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE, color TEXT);")
		db.execSQL("CREATE TABLE IF NOT EXISTS tags (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE, color TEXT);")
		db.execSQL("CREATE TABLE IF NOT EXISTS subjects (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE, color TEXT);")
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
	
	/*public void add() {
        ContentValues value = new ContentValues();
        value.put("title", "标题");
        value.put("description", "描述");
        value.put("due_date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        value.put("status", 1);
        value.put("priority", 0);
        value.put("todo_type", 1);
        value.put("subtask", "['子任务1','子任务2']");
        value.put("attachment", "0");
        value.put("subject", "大学英语");
        value.put("location", "教学楼A座");
        value.put("color", "red");
        value.put("label", "#标签");
        db.insert("todos", null, value);
    }*/
	fun deleteTodo(id: String?) {
		writableDatabase.delete("todos", "id  = ?", arrayOf(id))
		close()
	}
	
	fun deleteTodo(todoInfo: TodoInfo) {
		deleteTodo(todoInfo.getId().value.toString())
	}
	
	fun addTodo(todoInfo: TodoInfo) {
		value = setContentValues(todoInfo)
		writableDatabase.insert("todos", null, value)
		close()
	}
	
	fun updateTodo(todoInfo: TodoInfo) {
		value = setContentValues(todoInfo)
		writableDatabase.update("todos", value, "id = ?", arrayOf(todoInfo.getId().value.toString()))
		close()
	}
	
	companion object {
		private var value = ContentValues()
		private fun setContentValues(todoInfo: TodoInfo): ContentValues {
			value.apply {
				clear()
				put("title", todoInfo.getTitle().value)
				put("description", todoInfo.getDescription().value)
				put("due_date", todoInfo.getDueDate().value)
				put("status", todoInfo.getStatus().value)
				put("priority", todoInfo.getPriority().value ?: 0)
				put("todo_type", todoInfo.getType().value)
				put("subtask", todoInfo.getSubtask().value)
				put("attachment", todoInfo.getAttachment().value)
				put("subject", todoInfo.getSubject().value)
				put("location", todoInfo.getLocation().value)
				put("color", todoInfo.getColor().value)
				put("label", todoInfo.getTag().value)
				put("due_time", todoInfo.getDueTime().value)
				put("remind_time", todoInfo.getRemindTime().value)
				put("done_datetime", todoInfo.getDoneDate().value)
				put("update_datetime", LocalDateTime.now()
					.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
				put("ddl", todoInfo.getDdlDate().value)
			}
			
			
			return value
		}
	}
}
