package com.miyuyan.sysuer.browser.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope

class BrowserRepository(context: Context, scope: CoroutineScope) {
    private val database = BrowserDatabase.getDatabase(context, scope)
    private val dao = database.browserDao()

    suspend fun getAllUserAgents(): List<UserAgentEntity> = dao.getAllUserAgents()

    suspend fun getAllJavaScript(): List<JavaScriptEntity> = dao.getAllJavaScript()

    suspend fun insertJs(js: JavaScriptEntity): Long? = dao.insertJs(js)
    
    suspend fun deleteJs(js: JavaScriptEntity): Unit = dao.deleteJs(js)
    
    suspend fun deleteJS(jsId: Long): Unit = dao.deleteJS(jsId)
    
    suspend fun updateJs(js: JavaScriptEntity): Unit = dao.updateJs(js)
    
    suspend fun getJs(jsId: Long): JavaScriptEntity? = dao.getJs(jsId)
    
    
    suspend fun insertUa(ua: UserAgentEntity): Unit = dao.insertUa(ua)


    suspend fun updateJsTitle(oldTitlePattern: String, newTitle: String): Unit =
        dao.updateJsTitleByPattern(oldTitlePattern, newTitle)

    /**
     * 从 URL 导入脚本到数据库
     */
    suspend fun importScriptFromUrl(url: String): Boolean {
        val entity = ScriptParser.parseFromUrl(url)
        return if (entity != null) {
            dao.insertJs(entity)
            true
        } else false
    }
}
