package com.miyuyan.sysuer.browser.data

import android.util.Log
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
		}
		else {
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
		
		return JavaScriptEntity(jsId = (title + namespace).hashCode(),
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
		                        noframes = noframes)
	}
	
	/**
	 * 从 URL 下载并解析脚本
	 */
	suspend fun parseFromUrl(url: String): JavaScriptEntity? = withContext(Dispatchers.IO) {
		// SECURITY: refuse to import userscripts from non-HTTPS sources. Plain
		// HTTP exposes users to trivial MITM tampering with arbitrary JS that
		// will then run with the privileges of a userscript.
		if (!url.startsWith("https://")) {
			Log.w("GM_Script", "Refusing non-HTTPS userscript import: $url")
			return@withContext null
		}
		try {
			val response = client.newCall(Request.Builder().url(url).build()).execute()
			if (response.isSuccessful) {
				val entity = parseFromScript(response.body.string())
				if (entity.downloadURL == null) {
					entity.downloadURL = url
				}
				return@withContext entity
			}
		} catch (e: IOException) {
			e.printStackTrace()
		}
		null
	}

	/**
	 * 将 JavaScriptEntity 转换为包含完整元数据块的脚本字符串
	 */
	fun updateScriptByEntity(entity: JavaScriptEntity): String {
		val sb = StringBuilder()
		sb.append("// ==UserScript==\n")
		sb.append("// @name         ${entity.title}\n")
		entity.namespace?.let { sb.append("// @namespace    $it\n") }
		entity.version?.let { sb.append("// @version      $it\n") }
		entity.author?.let { sb.append("// @author       $it\n") }
		entity.description?.let { sb.append("// @description  $it\n") }
		entity.homepage?.let { sb.append("// @homepage     $it\n") }
		entity.icon?.let { sb.append("// @icon         $it\n") }
		entity.updateURL?.let { sb.append("// @updateURL    $it\n") }
		entity.downloadURL?.let { sb.append("// @downloadURL  $it\n") }
		entity.supportURL?.let { sb.append("// @supportURL   $it\n") }

		entity.matches.forEach { sb.append("// @match        $it\n") }
		entity.includes.forEach { sb.append("// @include      $it\n") }
		entity.excludes.forEach { sb.append("// @exclude      $it\n") }
		entity.requires.forEach { sb.append("// @require      $it\n") }
		entity.resources.forEach { sb.append("// @resource     $it\n") }
		entity.connects.forEach { sb.append("// @connect      $it\n") }
		entity.grants.forEach { sb.append("// @grant        $it\n") }
		entity.antifeatures.forEach { sb.append("// @antifeature  $it\n") }

		if (entity.runAt != "document-idle") sb.append("// @run-at       ${entity.runAt}\n")
		if (entity.noframes == true) sb.append("// @noframes\n")

		sb.append("// ==/UserScript==\n\n")

		// 剥离原脚本中的旧元数据块
		val cleanScript = stripMetadata(entity.script ?: "")
		sb.append(cleanScript)
		entity.script = "$sb"
		return "$sb"
	}

	/**
	 * 根据新的脚本内容更新实体的元数据，保留本地数据库状态
	 */
	fun updateEntityByScript(entity: JavaScriptEntity, newContent: String): JavaScriptEntity {
		return parseFromScript(newContent).apply {
			id = entity.id
			position = entity.position
			state = entity.state
			run = entity.run
			time = entity.time
		}
	}

	private fun stripMetadata(script: String): String {
		val startTag = "// ==UserScript=="
		val endTag = "// ==/UserScript=="
		val startIndex = script.indexOf(startTag)
		val endIndex = script.indexOf(endTag)

		if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
			val before = script.substring(0, startIndex)
			val after = script.substring(endIndex + endTag.length)
			return (before.trim() + "\n" + after.trim()).trim()
		}
		return script.trim()
	}
}
