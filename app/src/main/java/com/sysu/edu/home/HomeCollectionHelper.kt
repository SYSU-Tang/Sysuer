package com.sysu.edu.home

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class HomeCollectionHelper(context: Context?) :
	SQLiteOpenHelper(context, "service_collection.db", null, 5) {
	override fun onCreate(db: SQLiteDatabase) {
		db.execSQL("CREATE TABLE IF NOT EXISTS service_collection (id INTEGER PRIMARY KEY AUTOINCREMENT, serviceId INTEGER, serviceJson TEXT, collectTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP, position INTEGER)")
		db.execSQL("CREATE TABLE IF NOT EXISTS dashboard_shortcut_collection (id INTEGER PRIMARY KEY AUTOINCREMENT, shortcutId INTEGER, shortcutJson TEXT, collectTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP, position INTEGER)")
	}
	
	override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
		if (newVersion <= 2) db.execSQL("ALTER TABLE service_collection ADD COLUMN serviceId INTEGER")
		if (newVersion <= 3) db.execSQL("ALTER TABLE service_collection ADD COLUMN position INTEGER")
		if (newVersion <= 5) db.execSQL("CREATE TABLE IF NOT EXISTS dashboard_shortcut_collection (id INTEGER PRIMARY KEY AUTOINCREMENT, shortcutId INTEGER, shortcutJson TEXT, collectTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP, position INTEGER)")
	}
	
	fun addService(id: Int, serviceJson: String?, position: Int?) {
		if (!isServiceCollected(id)) writableDatabase.insertOrThrow("service_collection", null, ContentValues().apply {
			put("serviceId", id)
			put("serviceJson", serviceJson)
			put("position", position)
		})
	}
	
	fun addDashboardShortcut(id: Int, shortcutJson: String?, position: Int?) {
		if (!isDashboardShortcutCollected(id)) writableDatabase.insertOrThrow("dashboard_shortcut_collection", null, ContentValues().apply {
			put("shortcutId", id)
			put("shortcutJson", shortcutJson)
			if (position != null) put("position", position)
		})
	}
	
	fun updateServicePosition(id: Int, position: Int?) {
		if (isServiceCollected(id)) {
			writableDatabase.update("service_collection", ContentValues().apply {
				if (position != null) put("position", position)
			}, "serviceId = ?", arrayOf("$id"))
		}
	}
	
	fun updateDashboardShortcutPosition(id: Int, position: Int?) {
		if (isCollected("dashboard_shortcut_collection", "shortcutId", id)) writableDatabase.update("dashboard_shortcut_collection", ContentValues().apply {
			if (position != null) put("position", position)
		}, "shortcutId = ?", arrayOf("$id"))
	}
	
	fun isServiceCollected(id: Int): Boolean {
		return isCollected("service_collection", "serviceId", id)
	}
	
	fun isDashboardShortcutCollected(id: Int): Boolean {
		return isCollected("dashboard_shortcut_collection", "shortcutId", id)
	}
	
	fun isCollected(table: String, column: String?, id: Int): Boolean {
		val cursor = readableDatabase.query(table, null, "$column = ?", arrayOf("$id"), null, null, null)
		val count = cursor.count
		cursor.close()
		return count > 0
	}
	
	fun deleteDashboardShortcut(id: Int) {
		writableDatabase.delete("dashboard_shortcut_collection", "shortcutId = ?", arrayOf("$id"))
	}
	
	fun deleteService(id: Int) {
		writableDatabase.delete("service_collection", "serviceId = ?", arrayOf("$id"))
	}
}
