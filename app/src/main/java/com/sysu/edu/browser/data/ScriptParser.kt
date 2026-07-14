package com.sysu.edu.browser.data

import com.alibaba.fastjson2.JSONArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

object ScriptParser {
    private val client = OkHttpClient()

    /**
     * 解析油猴脚本内容（Metadata Block）
     */
    fun parseFromScript(content: String): JavaScriptEntity {
        val metadataStart = content.indexOf("// ==UserScript==")
        val metadataEnd = content.indexOf("// ==/UserScript==")
        
        val title: String?
        val namespace: String?
        val version: String?
        val author: String?
        val description: String?
        val homepage: String?
        val icon: String?
        val updateURL: String?
        val downloadURL: String?
        val supportURL: String?
        val matches = JSONArray()
        val includes = JSONArray()
        val excludes = JSONArray()
        val requires = JSONArray()
        val resources = JSONArray()
        val connects = JSONArray()
        val grants = JSONArray()
        val antifeatures = JSONArray()
        var runAt: String? = "document-idle"
        var noframes = false

        if (metadataStart != -1 && metadataEnd != -1) {
            val metadata = content.substring(metadataStart, metadataEnd)
            val regex = Regex("//\\s*@(\\S+)\\s+(.*)")
            
            var tName: String? = null
            var tNamespace: String? = null
            var tVersion: String? = null
            var tAuthor: String? = null
            var tDescription: String? = null
            var tHomepage: String? = null
            var tIcon: String? = null
            var tUpdateURL: String? = null
            var tDownloadURL: String? = null
            var tSupportURL: String? = null

            metadata.lines().forEach { line ->
                regex.find(line)?.let { match ->
                    val key = match.groupValues[1].lowercase()
                    val value = match.groupValues[2].trim()
                    
                    when (key) {
                        "name" -> tName = value
                        "namespace" -> tNamespace = value
                        "version" -> tVersion = value
                        "author" -> tAuthor = value
                        "description" -> tDescription = value
                        "homepage", "homepageurl" -> tHomepage = value
                        "icon", "iconurl", "defaulticon" -> tIcon = value
                        "updateurl" -> tUpdateURL = value
                        "downloadurl", "installurl" -> tDownloadURL = value
                        "supporturl" -> tSupportURL = value
                        "match" -> matches.add(value)
                        "include" -> includes.add(value)
                        "exclude" -> excludes.add(value)
                        "require" -> requires.add(value)
                        "resource" -> resources.add(value)
                        "connect" -> connects.add(value)
                        "grant" -> grants.add(value)
                        "antifeature" -> antifeatures.add(value)
                        "run-at" -> runAt = value
                        "noframes" -> noframes = true
                    }
                }
            }
            title = tName
            namespace = tNamespace
            version = tVersion
            author = tAuthor
            description = tDescription
            homepage = tHomepage
            icon = tIcon
            updateURL = tUpdateURL
            downloadURL = tDownloadURL
            supportURL = tSupportURL
        } else {
            title = "Unknown Script"
            namespace = null
            version = null
            author = null
            description = null
            homepage = null
            icon = null
            updateURL = null
            downloadURL = null
            supportURL = null
        }

        return JavaScriptEntity(
            jsId = (title + namespace).hashCode(),
            position = 0,
            title = title ?: "Untitled",
            namespace = namespace,
            version = version,
            author = author,
            description = description,
            homepage = homepage,
            icon = icon,
            updateURL = updateURL,
            downloadURL = downloadURL,
            supportURL = supportURL,
            script = content,
            matches = matches,
            includes = includes,
            excludes = excludes,
            requires = requires,
            resources = resources,
            connects = connects,
            grants = grants,
            antifeatures = antifeatures,
            runAt = runAt,
            noframes = noframes
        )
    }

    /**
     * 从 URL 下载并解析脚本
     */
    suspend fun parseFromUrl(url: String): JavaScriptEntity? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
				response.body.string().let { content ->
					val entity = parseFromScript(content)					// 如果脚本没有指定下载链接，使用当前的 URL
					return@withContext if (entity.downloadURL == null) {
						entity.copy(downloadURL = url)
					} else entity
				}
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        null
    }
}
