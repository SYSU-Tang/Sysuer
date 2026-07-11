package com.sysu.edu.todo

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import com.sysu.edu.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [TodoEntity::class, TypeEntity::class, SubjectEntity::class, TagEntity::class], version = 10, exportSchema = false)
abstract class TodoDatabase : RoomDatabase() {
	abstract fun todoDao(): TodoDao
	
	companion object {
		@Volatile private var INSTANCE: TodoDatabase? = null
		private val MIGRATION_FIX_ALL = object : Migration(1, 10) {
			override suspend fun migrate(connection: SQLiteConnection) {
				fixSchema(connection)
			}
		}
		private val MIGRATION_9_10 = object : Migration(9, 10) {
			override suspend fun migrate(connection: SQLiteConnection) {
				connection.prepare("ALTER TABLE `todos` ADD COLUMN `ddl_time` TEXT").use { it.step() }
				connection.prepare("ALTER TABLE `todos` ADD COLUMN `ddl_remind_time` TEXT").use { it.step() }
			}
		}
		private val MIGRATION_8_10 = object : Migration(8, 10) {
			override suspend fun migrate(connection: SQLiteConnection) {
				fixSchema(connection)
			}
		}
		private val MIGRATION_7_10 = object : Migration(7, 10) {
			override suspend fun migrate(connection: SQLiteConnection) {
				fixSchema(connection)
			}
		}
		private val MIGRATION_6_10 = object : Migration(6, 10) {
			override suspend fun migrate(connection: SQLiteConnection) {
				fixSchema(connection)
			}
		}
		
		private fun fixSchema(connection: SQLiteConnection) {			// 1. 修复 todos 表
			connection.prepare("""
                CREATE TABLE IF NOT EXISTS `todos_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `title` TEXT, 
                    `description` TEXT, 
                    `due_date` TEXT, 
                    `due_time` TEXT, 
                    `done_datetime` TEXT, 
                    `create_datetime` TEXT DEFAULT CURRENT_TIMESTAMP, 
                    `update_datetime` TEXT DEFAULT CURRENT_TIMESTAMP, 
                    `status` INTEGER NOT NULL DEFAULT 0, 
                    `priority` INTEGER NOT NULL DEFAULT 0, 
                    `todo_type` TEXT, 
                    `subtask` TEXT NOT NULL, 
                    `attachment` TEXT NOT NULL, 
                    `tag` TEXT NOT NULL, 
                    `subject` TEXT, 
                    `location` TEXT, 
                    `color` TEXT, 
                    `label` TEXT, 
                    `ddl` TEXT, 
                    `ddl_time` TEXT,
                    `ddl_remind_time` TEXT,
                    `remind_time` TEXT
                )
            """.trimIndent()).use { it.step() }
			
			connection.prepare("""
                INSERT INTO `todos_new` (
                    id, title, description, due_date, due_time, done_datetime, 
                    create_datetime, update_datetime, status, priority, todo_type, 
                    subtask, attachment, tag, subject, location, color, label, ddl, ddl_time, ddl_remind_time, remind_time
                )
                SELECT 
                    id, title, description, CAST(due_date AS TEXT), CAST(due_time AS TEXT), CAST(done_datetime AS TEXT),
                    CAST(create_datetime AS TEXT), CAST(update_datetime AS TEXT), 
                    IFNULL(status, 0), IFNULL(priority, 0), todo_type, 
                    IFNULL(subtask, '[]'), IFNULL(attachment, '[]'), '[]', 
                    subject, location, color, label, CAST(ddl AS TEXT), NULL, NULL, remind_time
                FROM `todos`
            """.trimIndent()).use { it.step() }
			
			connection.prepare("DROP TABLE IF EXISTS `todos`").use { it.step() }
			connection.prepare("ALTER TABLE `todos_new` RENAME TO `todos`")
				.use { it.step() }			// 2. 修复 types 表 (主要是 id 字段的 NOT NULL 约束)
			connection.prepare("""
				CREATE TABLE IF NOT EXISTS `types_new` (
					`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
					`name` TEXT, 
					`color` TEXT
				)
			""".trimIndent()).use { it.step() }
			connection.prepare("INSERT INTO `types_new` (id, name, color) SELECT id, name, color FROM `types`")
				.use { it.step() }
			connection.prepare("DROP TABLE IF EXISTS `types`").use { it.step() }
			connection.prepare("ALTER TABLE `types_new` RENAME TO `types`")
				.use { it.step() }			// 3. 修复 subjects 表
			connection.prepare("""
				CREATE TABLE IF NOT EXISTS `subjects_new` (
					`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
					`name` TEXT, 
					`color` TEXT
				)
			""".trimIndent()).use { it.step() }
			connection.prepare("INSERT INTO `subjects_new` (id, name, color) SELECT id, name, color FROM `subjects`")
				.use { it.step() }
			connection.prepare("DROP TABLE IF EXISTS `subjects`").use { it.step() }
			connection.prepare("ALTER TABLE `subjects_new` RENAME TO `subjects`")
				.use { it.step() }			// 4. 修复 tags 表
			connection.prepare("""
				CREATE TABLE IF NOT EXISTS `tags_new` (
					`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
					`name` TEXT, 
					`color` TEXT
				)
			""".trimIndent()).use { it.step() }
			connection.prepare("INSERT INTO `tags_new` (id, name, color) SELECT id, name, color FROM `tags`")
				.use { it.step() }
			connection.prepare("DROP TABLE IF EXISTS `tags`").use { it.step() }
			connection.prepare("ALTER TABLE `tags_new` RENAME TO `tags`").use { it.step() }
			
			// 5. 清理重复数据并添加唯一索引
			val tables = listOf("types", "subjects", "tags")
			tables.forEach { table ->
				connection.prepare("DELETE FROM `$table` WHERE id NOT IN (SELECT MIN(id) FROM `$table` GROUP BY name)")
					.use { it.step() }
				connection.prepare("CREATE UNIQUE INDEX IF NOT EXISTS `index_${table}_name` ON `$table` (`name`)")
					.use { it.step() }
			}
		}
		
		fun getDatabase(context: Context, scope: CoroutineScope): TodoDatabase {
			return INSTANCE ?: synchronized(this) {
				val instance = Room.databaseBuilder(context.applicationContext, TodoDatabase::class.java, "todo.db")
					.addMigrations(MIGRATION_FIX_ALL, MIGRATION_6_10, MIGRATION_7_10, MIGRATION_8_10, MIGRATION_9_10)
					.addCallback(TodoDatabaseCallback(context, scope))
					.build()
				INSTANCE = instance
				instance
			}
		}
	}
	
	private class TodoDatabaseCallback(private val context: Context,
	                                   private val scope: CoroutineScope) : Callback() {
		override suspend fun onCreate(connection: SQLiteConnection) {
			super.onCreate(connection)
			INSTANCE?.let { database ->
				scope.launch(Dispatchers.IO) {
					try {
						database.todoDao()
							.insertTypes(context.resources.getStringArray(R.array.todo_base_type)
											 .map<String, TypeEntity> { TypeEntity(name = it, color = null) })
					} catch (_: Exception) {
					}
				}
			}
		}
	}
}
