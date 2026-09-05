package com.miyuyan.sysuer.home.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

@Dao
interface CollectionDao {
	@Query("SELECT * FROM service_collection ORDER BY position ASC")
	suspend fun getCollectedServices(): List<ServiceCollectionEntity>

	@Query("SELECT * FROM dashboard_shortcut_collection ORDER BY position ASC")
	suspend fun getCollectedDashboardShortcuts(): List<DashboardShortcutEntity>

	@Query("SELECT EXISTS(SELECT 1 FROM service_collection WHERE serviceId = :id)")
	suspend fun isServiceCollected(id: Int): Boolean

	@Query("SELECT EXISTS(SELECT 1 FROM dashboard_shortcut_collection WHERE shortcutId = :id)")
	suspend fun isDashboardShortcutCollected(id: Int): Boolean

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun addService(entity: ServiceCollectionEntity)

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun addDashboardShortcut(entity: DashboardShortcutEntity)

	@Query("DELETE FROM service_collection WHERE serviceId = :id")
	suspend fun deleteService(id: Int)

	@Query("DELETE FROM dashboard_shortcut_collection WHERE shortcutId = :id")
	suspend fun deleteDashboardShortcut(id: Int)

	@Query("UPDATE service_collection SET position = :position WHERE serviceId = :id")
	suspend fun updateServicePosition(id: Int, position: Int?)

	@Query("UPDATE dashboard_shortcut_collection SET position = :position WHERE shortcutId = :id")
	suspend fun updateDashboardShortcutPosition(id: Int, position: Int?)
}