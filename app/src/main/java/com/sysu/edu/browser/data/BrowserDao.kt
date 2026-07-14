package com.sysu.edu.browser.data

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update

@Dao interface BrowserDao {
	@Query("SELECT * FROM ua ORDER BY position ASC")
	suspend fun getAllUserAgents(): List<UserAgentEntity>
	
	@Query("SELECT * FROM js ORDER BY position ASC")
	suspend fun getAllJavaScript(): List<JavaScriptEntity>
	
	@Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertJs(js: JavaScriptEntity): Long?
	
	@Delete suspend fun deleteJs(js: JavaScriptEntity)
	
	@Query("DELETE FROM js WHERE jsId = :jsId") suspend fun deleteJS(jsId: Long)
	
	@Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertUa(ua: UserAgentEntity)
	
	@Update suspend fun updateJs(js: JavaScriptEntity)
	
	@Query("SELECT * FROM js WHERE id = :id") suspend fun getJs(id: Long): JavaScriptEntity?
	
	@Query("UPDATE js SET title = :newTitle WHERE title LIKE :oldTitlePattern")
	suspend fun updateJsTitleByPattern(oldTitlePattern: String, newTitle: String)
	
	@Query("UPDATE js SET script = :script WHERE matches LIKE :matchPattern")
	suspend fun updateJsScriptByMatch(matchPattern: String, script: String)
}
