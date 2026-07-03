package com.sysu.edu.browser

import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import java.util.regex.Pattern

class JavaScript {
	private var jsList = JSONArray()
	
	constructor()
	
	fun add(title: String, description: String?, matches: Array<String?>?, script: String?) {
		jsList.add(JSONObject.parse("{\"title\": \"$title\",\"description\": \"$description\",\"matches\": ${matches.contentToString()},\"script\": \"$script\"}"))
	}
	
	fun add(item: JSONObject?) {
		jsList.add(item)
	}
	
	fun searchJS(key: String): MutableList<JSONObject?> {
		val list = mutableListOf<JSONObject?>()
		jsList.forEach { a: Any? ->
			val item = a as JSONObject
			if (item.containsKey("state") && item.getInteger("state") == 1) {
				for (e in item.getJSONArray("matches")) {
					if (Pattern.compile(e as String).matcher(key).find()) {
						list.add(item)
						break
					}
				}
			}
		}
		return list
	}
	
	fun searchJS(key: String, isActive: Boolean): ArrayList<JSONObject?> {
		val list = ArrayList<JSONObject?>()
		jsList.forEach { a: Any? ->
			val item = a as JSONObject
			if (item.containsKey("state") && item.getInteger("state") == 1 && isActive && item.containsKey("run") && item.getInteger("run") == 1) {
				for (e in item.getJSONArray("matches")) {
					if (Pattern.compile(e as String).matcher(key).find()) {
						list.add(item)
						break
					}
				}
			}
		}
		return list
	}
	
	fun clear() {
		jsList.clear()
	}
}
