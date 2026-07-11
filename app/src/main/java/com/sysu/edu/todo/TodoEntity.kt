package com.sysu.edu.todo

import androidx.room3.ColumnInfo
import androidx.room3.ColumnTypeConverters
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.alibaba.fastjson2.JSONArray
import com.sysu.edu.api.StringJSONArrayConverter

@Entity(tableName = "todos") data class TodoEntity(@field:PrimaryKey(autoGenerate = true)
												   val id: Int = 0,
                                                   var title: String? = null,
                                                   var description: String? = null,
                                                   @field:ColumnInfo(name = "due_date")
												   var dueDate: String? = null,
                                                   @field:ColumnInfo(name = "due_time")
												   var dueTime: String? = null,
                                                   @field:ColumnInfo(name = "done_datetime")
												   var doneDateTime: String? = null,
                                                   @field:ColumnInfo(name = "create_datetime", defaultValue = "CURRENT_TIMESTAMP")
												   var createDateTime: String? = null,
                                                   @field:ColumnInfo(name = "update_datetime", defaultValue = "CURRENT_TIMESTAMP")
												   var updateDateTime: String? = null,
                                                   @field:ColumnInfo(defaultValue = "0")
												   var status: Int = 0,
                                                   @field:ColumnInfo(defaultValue = "0")
												   var priority: Int = 0,
                                                   @field:ColumnInfo(name = "todo_type")
												   var todoType: String? = null,
                                                   @field:ColumnTypeConverters(StringJSONArrayConverter::class)
												   var subtask: JSONArray = JSONArray(),
                                                   @field:ColumnTypeConverters(StringJSONArrayConverter::class)
												   var attachment: JSONArray = JSONArray(),
                                                   @field:ColumnTypeConverters(StringJSONArrayConverter::class)
												   var tag: JSONArray = JSONArray(),
                                                   var subject: String? = null,
                                                   var location: String? = null,
                                                   var color: String? = null,
                                                   var label: String? = null,
                                                   var ddl: String? = null,
                                                   @field:ColumnInfo(name = "ddl_time")
                                                   var ddlTime: String? = null,
                                                   @field:ColumnInfo(name = "ddl_remind_time")
                                                   var ddlRemindTime: String? = null,
                                                   @field:ColumnInfo(name = "remind_time")
												   var remindTime: String? = null)

@Entity(tableName = "types", indices = [Index(value = ["name"], unique = true)]) data class TypeEntity(@field:PrimaryKey(autoGenerate = true)
												   val id: Int = 0,
												   var name: String?,
												   var color: String? = null)

@Entity(tableName = "subjects", indices = [Index(value = ["name"], unique = true)]) data class SubjectEntity(@field:PrimaryKey(autoGenerate = true)
														 val id: Int = 0,
                                                         var name: String?,
                                                         var color: String? = null)

@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)]) data class TagEntity(@field:PrimaryKey(autoGenerate = true)
												 val id: Int = 0,
                                                 var name: String?,
                                                 var color: String? = null)
