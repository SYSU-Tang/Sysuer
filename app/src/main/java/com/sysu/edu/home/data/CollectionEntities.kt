package com.sysu.edu.home.data

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "service_collection", indices = [Index(value = ["serviceId"], unique = true)])
data class ServiceCollectionEntity(
	@PrimaryKey(autoGenerate = true) val id: Long = 0,
	val serviceId: Int?,
	val serviceJson: String?,
	@ColumnInfo(defaultValue = "CURRENT_TIMESTAMP", typeAffinity = ColumnInfo.TEXT) val collectTime: String? = null,
	val position: Int?
)

@Entity(tableName = "dashboard_shortcut_collection", indices = [Index(value = ["shortcutId"], unique = true)])
data class DashboardShortcutEntity(
	@PrimaryKey(autoGenerate = true) val id: Long = 0,
	val shortcutId: Int?,
	val shortcutJson: String?,
	@ColumnInfo(defaultValue = "CURRENT_TIMESTAMP", typeAffinity = ColumnInfo.TEXT) val collectTime: String? = null,
	val position: Int?
)