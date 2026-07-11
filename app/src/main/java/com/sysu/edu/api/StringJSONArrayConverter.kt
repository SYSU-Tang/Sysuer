package com.sysu.edu.api

import androidx.room3.ColumnTypeConverter
import com.alibaba.fastjson2.JSONArray

class StringJSONArrayConverter {
	@ColumnTypeConverter fun string2json(s: String?): JSONArray = if (s.isNullOrEmpty()) JSONArray() else JSONArray.parse(s) ?: JSONArray()
	@ColumnTypeConverter fun json2string(s: JSONArray?): String = if (s == null) "[]" else "$s"
}