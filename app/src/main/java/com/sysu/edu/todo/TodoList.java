package com.sysu.edu.todo;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;

import com.sysu.edu.R;
import com.sysu.edu.todo.info.TodoInfo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TodoList extends SQLiteOpenHelper {

    private static ContentValues value = new ContentValues();
    private final Context context;

    public TodoList(Context context, int version) {
        super(context, "todo.db", null, version);
        this.context = context;
    }

    @NonNull
    private static ContentValues setContentValues(TodoInfo todoInfo) {
        value.clear();
        value.put("title", todoInfo.getTitle().getValue());
        value.put("description", todoInfo.getDescription().getValue());
        value.put("due_date", todoInfo.getDueDate().getValue());
        value.put("status", todoInfo.getStatus().getValue());
        value.put("priority", todoInfo.getPriority().getValue());
        value.put("todo_type", todoInfo.getType().getValue());
        value.put("subtask", todoInfo.getSubtask().getValue());
        value.put("attachment", todoInfo.getAttachment().getValue());
        value.put("subject", todoInfo.getSubject().getValue());
        value.put("location", todoInfo.getLocation().getValue());
        value.put("color", todoInfo.getColor().getValue());
        value.put("label", todoInfo.getTag().getValue());
        value.put("due_time", todoInfo.getDueTime().getValue());
        value.put("remind_time", todoInfo.getRemindTime().getValue());
        value.put("done_datetime", todoInfo.getDoneDate().getValue());
        value.put("update_datetime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        return value;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //db = context.openOrCreateDatabase("todo.db", Context.MODE_PRIVATE, null);
        //db.execSQL("Drop table if exists types");
        db.execSQL("CREATE TABLE IF NOT EXISTS todos (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, description TEXT, due_date DATETIME,due_time DATETIME, done_datetime DATETIME, create_datetime DATETIME DEFAULT CURRENT_TIMESTAMP, update_datetime DATETIME DEFAULT CURRENT_TIMESTAMP, status INTEGER DEFAULT 0, priority INTEGER DEFAULT 0, todo_type TEXT,subtask TEXT,attachment TEXT,subject TEXT, location TEXT,color TEXT,label TEXT,ddl DATETIME,remind_time TEXT);");
        db.execSQL("CREATE TABLE IF NOT EXISTS types (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE, color TEXT);");
        db.execSQL("CREATE TABLE IF NOT EXISTS subjects (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE, color TEXT);");
        db.execSQL("CREATE TABLE IF NOT EXISTS tags (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE, color TEXT);");
        db.execSQL("CREATE TABLE IF NOT EXISTS subjects (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE, color TEXT);");
        addType(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        if (i <= 6) {
            addType(db);
        }
    }

    public void addType(SQLiteDatabase db) {
        db.beginTransaction();
        ContentValues value = new ContentValues();
        for (String type : context.getResources().getStringArray(R.array.todo_base_type)) {
            value.put("name", type);
            try {
                db.insertWithOnConflict("types", null, value, SQLiteDatabase.CONFLICT_ABORT);
            } catch (Exception ignored) {
            }
            value.clear();
        }
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public void addType() {
        addType(getWritableDatabase());
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

    public void deleteTodo(String id) {
        getWritableDatabase().delete("todos", "id  = ?", new String[]{id});
        close();
    }

    public void deleteTodo(TodoInfo todoInfo) {
        deleteTodo(String.valueOf(todoInfo.getId().getValue()));
    }

    public void addTodo(TodoInfo todoInfo) {
        value = setContentValues(todoInfo);
        getWritableDatabase().insert("todos", null, value);
        close();
    }

    public void updateTodo(TodoInfo todoInfo) {
        value = setContentValues(todoInfo);
        getWritableDatabase().update("todos", value, "id = ?", new String[]{String.valueOf(todoInfo.getId().getValue())});
        close();
    }
}
