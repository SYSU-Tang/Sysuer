package com.miyuyan.sysuer.browser.data

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.api.FileManager.readAssets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [UserAgentEntity::class, JavaScriptEntity::class], version = 17, exportSchema = false)
abstract class BrowserDatabase : RoomDatabase() {
	abstract fun browserDao(): BrowserDao
	
	companion object {
		@Volatile private var INSTANCE: BrowserDatabase? = null
		val MIGRATION_12_13: Migration = object : Migration(12, 13) {
			override suspend fun migrate(connection: SQLiteConnection) {				// 迁移 ua 表
				connection.execSQL("ALTER TABLE ua RENAME TO ua_old")
				connection.execSQL("CREATE TABLE IF NOT EXISTS `ua` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `uaId` INTEGER, `position` INTEGER, `title` TEXT, `ua` TEXT, `description` TEXT, `time` TEXT DEFAULT CURRENT_TIMESTAMP)")
				connection.execSQL("INSERT INTO ua (id, uaId, position, title, ua, description, time) SELECT id, uaId, position, title, ua, description, time FROM ua_old")
				connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_ua_uaId` ON `ua` (`uaId`)")
				connection.execSQL("DROP TABLE ua_old")				// 迁移 js 表
				connection.execSQL("ALTER TABLE js RENAME TO js_old")
				connection.execSQL("CREATE TABLE IF NOT EXISTS `js` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `jsId` INTEGER, `position` INTEGER, `title` TEXT, `namespace` TEXT, `version` TEXT, `author` TEXT, `description` TEXT, `homepage` TEXT, `icon` TEXT, `updateURL` TEXT, `downloadURL` TEXT, `supportURL` TEXT, `script` TEXT, `matches` TEXT, `includes` TEXT, `excludes` TEXT, `requires` TEXT, `resources` TEXT, `connects` TEXT, `grants` TEXT, `antifeatures` TEXT, `runAt` TEXT, `noframes` INTEGER, `state` INTEGER NOT NULL, `run` INTEGER NOT NULL, `time` TEXT DEFAULT CURRENT_TIMESTAMP)")
				connection.execSQL("INSERT INTO js (id, jsId, position, title, author, description, script, matches, state, run, time) SELECT id, jsId, position, title, author, description, script, matches, state, run, time FROM js_old")
				connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_js_jsId` ON `js` (`jsId`)")
				connection.execSQL("DROP TABLE js_old")
			}
		}

		val MIGRATION_13_14: Migration = object : Migration(13, 14) {
			override suspend fun migrate(connection: SQLiteConnection) {
				// 处理 ua 表
				connection.execSQL("ALTER TABLE ua RENAME TO ua_old")
				connection.execSQL("CREATE TABLE IF NOT EXISTS `ua` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `uaId` INTEGER, `position` INTEGER, `title` TEXT, `ua` TEXT, `description` TEXT, `time` TEXT DEFAULT CURRENT_TIMESTAMP)")
				connection.execSQL("INSERT INTO ua (id, uaId, position, title, ua, description, time) SELECT id, uaId, position, title, ua, description, time FROM ua_old")
				connection.execSQL("DROP INDEX IF EXISTS `index_ua_uaId`")
				connection.execSQL("CREATE UNIQUE INDEX `index_ua_uaId` ON `ua` (`uaId`)")
				connection.execSQL("DROP TABLE ua_old")

				// 处理 js 表
				connection.execSQL("ALTER TABLE js RENAME TO js_old")
				connection.execSQL("CREATE TABLE IF NOT EXISTS `js` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `position` INTEGER, `title` TEXT, `namespace` TEXT, `jsId` INTEGER, `version` TEXT, `author` TEXT, `description` TEXT, `homepage` TEXT, `icon` TEXT, `updateURL` TEXT, `downloadURL` TEXT, `supportURL` TEXT, `script` TEXT, `matches` TEXT, `includes` TEXT, `excludes` TEXT, `requires` TEXT, `resources` TEXT, `connects` TEXT, `grants` TEXT, `antifeatures` TEXT, `runAt` TEXT, `noframes` INTEGER, `state` INTEGER NOT NULL, `run` INTEGER NOT NULL, `time` TEXT DEFAULT CURRENT_TIMESTAMP)")
				connection.execSQL("INSERT INTO js (id, position, title, namespace, jsId, version, author, description, homepage, icon, updateURL, downloadURL, supportURL, script, matches, includes, excludes, requires, resources, connects, grants, antifeatures, runAt, noframes, state, run, time) SELECT id, position, title, namespace, jsId, version, author, description, homepage, icon, updateURL, downloadURL, supportURL, script, matches, includes, excludes, requires, resources, connects, grants, antifeatures, runAt, noframes, state, run, time FROM js_old")
				connection.execSQL("DROP INDEX IF EXISTS `index_js_jsId`")
				connection.execSQL("CREATE UNIQUE INDEX `index_js_jsId` ON `js` (`jsId`)")
				connection.execSQL("DROP TABLE js_old")
			}
		}

		val MIGRATION_14_15: Migration = object : Migration(14, 15) {
			override suspend fun migrate(connection: SQLiteConnection) {
				// 针对之前可能迁移失败（索引丢失）的情况进行修复
				connection.execSQL("DROP INDEX IF EXISTS `index_ua_uaId`")
				connection.execSQL("CREATE UNIQUE INDEX `index_ua_uaId` ON `ua` (`uaId`)")
				
				connection.execSQL("DROP INDEX IF EXISTS `index_js_jsId`")
				connection.execSQL("CREATE UNIQUE INDEX `index_js_jsId` ON `js` (`jsId`)")
			}
		}

		val MIGRATION_15_16: Migration = object : Migration(15, 16) {
			override suspend fun migrate(connection: SQLiteConnection) {
				// 修复 js 表：Room 要求所有 JSONArray 字段必须为 NOT NULL
				connection.execSQL("ALTER TABLE js RENAME TO js_old")
				connection.execSQL("""
					CREATE TABLE IF NOT EXISTS `js` (
						`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
						`position` INTEGER, 
						`title` TEXT, 
						`namespace` TEXT, 
						`jsId` INTEGER, 
						`version` TEXT, 
						`author` TEXT, 
						`description` TEXT, 
						`homepage` TEXT, 
						`icon` TEXT, 
						`updateURL` TEXT, 
						`downloadURL` TEXT, 
						`supportURL` TEXT, 
						`script` TEXT, 
						`matches` TEXT NOT NULL, 
						`includes` TEXT NOT NULL, 
						`excludes` TEXT NOT NULL, 
						`requires` TEXT NOT NULL, 
						`resources` TEXT NOT NULL, 
						`connects` TEXT NOT NULL, 
						`grants` TEXT NOT NULL, 
						`antifeatures` TEXT NOT NULL, 
						`runAt` TEXT, 
						`noframes` INTEGER, 
						`state` INTEGER NOT NULL, 
						`run` INTEGER NOT NULL, 
						`time` TEXT DEFAULT CURRENT_TIMESTAMP
					)
				""".trimIndent())
				
				connection.execSQL("""
					INSERT INTO js (
						id, position, title, namespace, jsId, version, author, description, homepage, icon, 
						updateURL, downloadURL, supportURL, script, matches, includes, excludes, requires, 
						resources, connects, grants, antifeatures, runAt, noframes, state, run, time
					) SELECT 
						id, position, title, namespace, jsId, version, author, description, homepage, icon, 
						updateURL, downloadURL, supportURL, script, 
						IFNULL(matches, '[]'), IFNULL(includes, '[]'), IFNULL(excludes, '[]'), 
						IFNULL(requires, '[]'), IFNULL(resources, '[]'), IFNULL(connects, '[]'), 
						IFNULL(grants, '[]'), IFNULL(antifeatures, '[]'), 
						runAt, noframes, state, run, time 
					FROM js_old
				""".trimIndent())
				
				connection.execSQL("DROP INDEX IF EXISTS `index_js_jsId`")
				connection.execSQL("CREATE UNIQUE INDEX `index_js_jsId` ON `js` (`jsId`)")
				connection.execSQL("DROP TABLE js_old")
			}
		}

		val MIGRATION_16_17: Migration = object : Migration(16, 17) {
			override suspend fun migrate(connection: SQLiteConnection) {
				connection.execSQL("DELETE FROM js")
			}
		}
		
		fun getDatabase(context: Context, scope: CoroutineScope): BrowserDatabase {
			return INSTANCE ?: synchronized(this) {
				val instance = Room.databaseBuilder(context.applicationContext, BrowserDatabase::class.java, "browser.db")
					.addCallback(BrowserDatabaseCallback(context, scope))
					.addMigrations(MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17)
					.fallbackToDestructiveMigrationOnDowngrade()
					.build()
				INSTANCE = instance
				instance
			}
		}
	}
	
	private class BrowserDatabaseCallback(private val context: Context,
	                                      private val scope: CoroutineScope) : Callback() {
		override suspend fun onCreate(connection: SQLiteConnection) {
			super.onCreate(connection)
			INSTANCE?.let { database ->
				scope.launch(Dispatchers.IO) {
					populateDatabase(database.browserDao())
				}
			}
		}

		override suspend fun onOpen(connection: SQLiteConnection) {
			super.onOpen(connection)
			INSTANCE?.let { database ->
				scope.launch(Dispatchers.IO) {
					val dao = database.browserDao()
					if (dao.getAllJavaScript().isEmpty()) {
						populateDatabase(dao)
					}
				}
			}
		}
		
		private suspend fun populateDatabase(dao: BrowserDao) {
			try {
				// 1. 加载 assets 下所有 .user.js 文件
				context.assets.list("")?.filter { it.endsWith(".user.js") }?.forEach { fileName ->
					val content = readAssets(context, fileName)
					if (content.isNotEmpty()) {
						try {
							dao.insertJs(ScriptParser.parseFromScript(content))
						} catch (e: Exception) {
							e.printStackTrace()
						}
					}
				}

				// 2. 加载 ua.json
				val uaJson = readAssets(context, "ua.json")
				if (uaJson.isNotEmpty()) {
					JSONArray.parse(uaJson).forEach { item ->
						dao.insertUa(UserAgentEntity(uaId = (item as JSONObject).getInteger("uaId"), position = item.getInteger("position"), title = item.getString("title"), ua = item.getString("ua"), description = item.getString("description")))
					}
				}
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
	}
}
