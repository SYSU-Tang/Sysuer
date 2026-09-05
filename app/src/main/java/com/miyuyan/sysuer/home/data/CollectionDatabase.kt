package com.miyuyan.sysuer.home.data

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Database(entities = [ServiceCollectionEntity::class, DashboardShortcutEntity::class], version = 6, exportSchema = true)
abstract class CollectionDatabase : RoomDatabase() {
	abstract fun collectionDao(): CollectionDao

	companion object {
		@Volatile
		private var INSTANCE: CollectionDatabase? = null

		private val MIGRATION_5_6 = object : Migration(5, 6) {
			override suspend fun migrate(connection: SQLiteConnection) {
				connection.execSQL("ALTER TABLE service_collection RENAME TO service_collection_old")
				connection.execSQL("CREATE TABLE IF NOT EXISTS `service_collection` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `serviceId` INTEGER, `serviceJson` TEXT, `collectTime` TEXT DEFAULT CURRENT_TIMESTAMP, `position` INTEGER)")
				connection.execSQL("INSERT INTO service_collection (id, serviceId, serviceJson, collectTime, position) SELECT id, serviceId, serviceJson, collectTime, position FROM service_collection_old")
				connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_service_collection_serviceId` ON `service_collection` (`serviceId`)")
				connection.execSQL("DROP TABLE service_collection_old")

				connection.execSQL("ALTER TABLE dashboard_shortcut_collection RENAME TO dashboard_shortcut_collection_old")
				connection.execSQL("CREATE TABLE IF NOT EXISTS `dashboard_shortcut_collection` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `shortcutId` INTEGER, `shortcutJson` TEXT, `collectTime` TEXT DEFAULT CURRENT_TIMESTAMP, `position` INTEGER)")
				connection.execSQL("INSERT INTO dashboard_shortcut_collection (id, shortcutId, shortcutJson, collectTime, position) SELECT id, shortcutId, shortcutJson, collectTime, position FROM dashboard_shortcut_collection_old")
				connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_dashboard_shortcut_collection_shortcutId` ON `dashboard_shortcut_collection` (`shortcutId`)")
				connection.execSQL("DROP TABLE dashboard_shortcut_collection_old")
			}
		}

		fun getDatabase(context: Context): CollectionDatabase {
			return INSTANCE ?: synchronized(this) {
				val instance = Room.databaseBuilder(
					context.applicationContext,
					CollectionDatabase::class.java,
					"service_collection.db"
				)
					.addMigrations(MIGRATION_5_6)
					.build()
				INSTANCE = instance
				instance
			}
		}
	}
}