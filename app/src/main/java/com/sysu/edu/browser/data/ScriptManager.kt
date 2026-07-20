package com.sysu.edu.browser.data

import android.webkit.WebView

/**
 * 脚本执行器：用于查找匹配当前 URL 的脚本并注入执行
 */
object ScriptManager {
	/**
	 * 查找匹配当前 URL 的脚本列表
	 * @param url 当前页面的 URL
	 * @param allScripts 数据库中所有的脚本实体
	 */
	fun getMatchingScripts(url: String,
	                       allScripts: List<JavaScriptEntity>): List<JavaScriptEntity> {
		return allScripts.filter { script -> // 1. 检查状态是否启用 (state = 1 表示启用)
			if (script.state != 1) return@filter false // 2. 检查黑名单 (excludes)
			val isExcluded = script.excludes.any { pattern -> matchUrl("$pattern", url) }
			if (isExcluded) return@filter false // 3. 检查白名单 (matches & includes)
			val isMatched = script.matches.any { pattern -> matchUrl("$pattern", url) }
			val isIncluded = script.includes.any { pattern -> matchUrl("$pattern", url) }
			
			isMatched || isIncluded
		}
	}
	
	/**
	 * 执行匹配的脚本
	 * @param webView 目标 WebView
	 * @param scripts 过滤出的匹配脚本列表
	 * @param runAt 运行阶段 (document-start, document-end, document-idle)
	 */
	fun executeScripts(webView: WebView, scripts: List<JavaScriptEntity>, runAt: String) {
		webView.post {
			scripts.filter { it.runAt == runAt }.forEach { entity ->
				executeScript(entity, webView)
			}
		}
	}
	
	fun executeScript(entity: JavaScriptEntity, webView: WebView) {
		val scriptContent = entity.script ?: return
		val scriptName = entity.title ?: "Untitled"
		val scriptId = "${scriptName}_${entity.namespace ?: ""}"
		val gmPolyfill = """
                    const GM_info = {
                        script: {
                            name: "${scriptName.replace("\"", "\\\"")}",
                            namespace: "${(entity.namespace ?: "").replace("\"", "\\\"")}",
                            version: "${(entity.version ?: "").replace("\"", "\\\"")}"
                        }
                    };
                    const GM_setValue = (key, value) => AndroidGM.setValue("${
			scriptId.replace("\"", "\\\"")
		}", key, JSON.stringify(value));
                    const GM_getValue = (key, defaultValue) => {
                        const val = AndroidGM.getValue("${
			scriptId.replace("\"", "\\\"")
		}", key, null);
                        return val ? JSON.parse(val) : defaultValue;
                    };
                    const GM_deleteValue = (key) => AndroidGM.deleteValue("${
			scriptId.replace("\"", "\\\"")
		}", key);
                    const GM_listValues = () => JSON.parse(AndroidGM.listValues("${
			scriptId.replace("\"", "\\\"")
		}"));
                    const GM_addStyle = (css) => {
                        const style = document.createElement('style');
                        style.textContent = css;
                        if (document.head) {
                            document.head.appendChild(style);
                        } else {
                            const observer = new MutationObserver(() => {
                                if (document.head) {
                                    document.head.appendChild(style);
                                    observer.disconnect();
                                }
                            });
                            observer.observe(document.documentElement, { childList: true });
                        }
                    };
                    const GM_log = (msg) => AndroidGM.log("${
			scriptName.replace("\"", "\\\"")
		}: " + msg);
                    const GM_registerMenuCommand = (name, fn) => {
                        AndroidGM.registerMenuCommand("${scriptId.replace("\"", "\\\"")}", name);
                        if (!window.gm_commands) window.gm_commands = {};
                        window.gm_commands["${scriptId.replace("\"", "\\\"")}_" + name] = fn;
                    };
                    const GM_xmlhttpRequest = (details) => {
                        console.warn('GM_xmlhttpRequest is not fully implemented yet');
                    };
                """.trimIndent()
		val wrappedScript = """(function() {
$gmPolyfill
    try {
        $scriptContent
    } catch (e) {
        console.error('Script [$scriptName] Error:', e);
    }
})();""".trimIndent()
		webView.evaluateJavascript(wrappedScript, null)
	}
	
	/**
	 * 检测脚本更新
	 * @return 如果有新版本，返回解析后的新实体，否则返回 null
	 */
	suspend fun checkForUpdate(entity: JavaScriptEntity): JavaScriptEntity? {
		val updateUrl = entity.updateURL ?: entity.downloadURL ?: return null
		val remoteEntity = ScriptParser.parseFromUrl(updateUrl) ?: return null
		val localVersion = entity.version ?: "0"
		val remoteVersion = remoteEntity.version ?: "0"
		
		if (compareVersion(remoteVersion, localVersion) > 0) {
			return remoteEntity.apply {
				id = entity.id
				position = entity.position
				state = entity.state
			}
		}
		return null
	}
	
	/**
	 * 版本号对比
	 * @return 1 if v1 > v2, -1 if v1 < v2, 0 if equal
	 */
	private fun compareVersion(v1: String, v2: String): Int {
		val parts1 = v1.split(".", "-").filter { it.isNotEmpty() }
		val parts2 = v2.split(".", "-").filter { it.isNotEmpty() }
		val length = maxOf(parts1.size, parts2.size)
		for (i in 0 until length) {
			val p1 = parts1.getOrNull(i)
			val p2 = parts2.getOrNull(i)
			
			if (p1 == p2) continue
			if (p1 == null) return -1
			if (p2 == null) return 1
			val n1 = p1.toIntOrNull()
			val n2 = p2.toIntOrNull()
			
			if (n1 != null && n2 != null) {
				if (n1 != n2) return n1.compareTo(n2)
			}
			else {
				val res = p1.compareTo(p2, ignoreCase = true)
				if (res != 0) return res
			}
		}
		return 0
	}
	
	/**
	 * URL 匹配算法：支持通配符 *
	 *
	 * */
	fun matchUrl(pattern: String, url: String): Boolean {
		if (pattern == "<all_urls>") return true
		
		return try { // 将通配符模式转换为正则表达式
			val regex = pattern.replace(".", "\\.").replace("*", ".*").replace("?", "\\?")
			
			Regex("^$regex$").containsMatchIn(url)
		} catch (_: Exception) {
			false
		}
	}
}
