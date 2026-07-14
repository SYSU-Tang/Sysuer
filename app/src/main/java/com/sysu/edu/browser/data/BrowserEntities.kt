package com.sysu.edu.browser.data

import androidx.room3.ColumnInfo
import androidx.room3.ColumnTypeConverters
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.alibaba.fastjson2.JSONArray
import com.sysu.edu.api.StringJSONArrayConverter

@Entity(tableName = "ua", indices = [Index(value = ["uaId"], unique = true)])
data class UserAgentEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0,
                           val uaId: Int?,
                           val position: Int?,
                           val title: String?,
                           val ua: String?,
                           val description: String?,
                           @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP", typeAffinity = ColumnInfo.TEXT)
						   val time: String? = null)

@Entity(tableName = "js", indices = [Index(value = ["jsId"], unique = true)])
data class JavaScriptEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0,
                            val position: Int? = 0,
                            var title: String?,
                            val namespace: String? = null,
                            val jsId: Int? = (title + namespace).hashCode(),
                            val version: String? = null,
                            var author: String? = null,
                            var description: String? = null,
                            val homepage: String? = null,
                            val icon: String? = null,
                            val updateURL: String? = null,
                            val downloadURL: String? = null,
                            val supportURL: String? = null,
                            var script: String?,
                            @field:ColumnTypeConverters(StringJSONArrayConverter::class)
                            var matches: JSONArray = JSONArray(),
                            @field:ColumnTypeConverters(StringJSONArrayConverter::class)
							val includes: JSONArray = JSONArray(),
                            @field:ColumnTypeConverters(StringJSONArrayConverter::class)
                            var excludes: JSONArray = JSONArray(),
                            @field:ColumnTypeConverters(StringJSONArrayConverter::class)
							val requires: JSONArray = JSONArray(),
                            @field:ColumnTypeConverters(StringJSONArrayConverter::class)
							val resources: JSONArray = JSONArray(),
                            @field:ColumnTypeConverters(StringJSONArrayConverter::class)
							val connects: JSONArray = JSONArray(),
                            @field:ColumnTypeConverters(StringJSONArrayConverter::class)
							val grants: JSONArray = JSONArray(),
                            @field:ColumnTypeConverters(StringJSONArrayConverter::class)
							val antifeatures: JSONArray = JSONArray(),
                            var runAt: String? = "document-idle",
                            val noframes: Boolean? = false,
                            var state: Int = 1,
                            val run: Int = 0,
                            @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP", typeAffinity = ColumnInfo.TEXT)
							val time: String? = null)
