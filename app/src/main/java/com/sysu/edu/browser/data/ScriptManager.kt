package com.sysu.edu.browser.data

import android.util.Log
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
		// SECURITY: scripts that opt into universal URL coverage (`<all_urls>`)
		// must also have `run == 1` (the explicit "trust this script" flag
		// stored on the entity). Without that gate, a malicious or careless
		// script using `<all_urls>` would inject userscript code into every
		// page the user visits — including banking, email, and the credential
		// forms that this very app relies on.
		val supportsAllUrls = url.startsWith("http://") || url.startsWith("https://") ||
			url.startsWith("file://") || url.startsWith("about:")
		return allScripts.filter { script -> // 1. 检查状态是否启用 (state = 1 表示启用)
			if (script.state != 1) return@filter false // 2. 检查黑名单 (excludes)
			val allowAllUrls = script.run == 1
			val isExcluded = script.excludes.any { pattern ->
				matchUrlWithAllUrls("$pattern", url, supportsAllUrls && allowAllUrls)
			}
			if (isExcluded) return@filter false // 3. 检查白名单 (matches & includes)
			val isMatched = script.matches.any { pattern ->
				matchUrlWithAllUrls("$pattern", url, supportsAllUrls && allowAllUrls)
			}
			val isIncluded = script.includes.any { pattern ->
				matchUrlWithAllUrls("$pattern", url, supportsAllUrls && allowAllUrls)
			}

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
	
	/**
	 * 执行匹配的脚本
	 * @param webView 目标 WebView
	 * @param scripts 过滤出的匹配脚本列表
	 * @param runAt 运行阶段列表 (document-start, document-end, document-idle)
	 */
	fun executeScripts(webView: WebView, scripts: List<JavaScriptEntity>, runAt: List<String>) {
		webView.post {
			scripts.filter { script -> runAt.contains(script.runAt) }.forEach { script ->
				executeScript(script, webView)
			}
		}
	}
	fun executeScript(entity: JavaScriptEntity, webView: WebView) {
		val scriptContent = entity.script ?: return
		val scriptName = entity.title ?: "Untitled"
		val scriptId = "${scriptName}_${entity.namespace ?: ""}"

		// Sanitize user-controlled values used inside the JS string literal
		val safeName = scriptName.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
		val safeNamespace = (entity.namespace ?: "").replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
		val safeVersion = (entity.version ?: "").replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
		val safeScriptId = scriptId.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")

		// Sandboxed GM polyfill:
		//  - All GM_* APIs are declared with `const` inside an IIFE so they live in
		//    the script's lexical scope and are NOT attached to `window`.
		//  - Only the API object (via the `unsafeWindow`-like `__GM_API__` reference
		//    passed to the user script) is reachable from the script body itself.
		//  - Web page JS cannot directly reach `AndroidGM` via the GM_ functions,
		//    because the polyfill is no longer hoisted onto `window`.
		//
		// Note: `AndroidGM` (the JavascriptInterface) is still attached to the
		// WebView globally, but any non-user-script page code calling it can only
		// invoke operations that are routed through `gmBridge` which has its own
		// permission checks (see GMBridge). Critical values like `username` /
		// `password` are NO LONGER exposed via `GM_getValue` keys.
		val gmPolyfill = """
                    (function() {
                        const __SCRIPT_ID__ = "${safeScriptId}";
                        const __SCRIPT_NAME__ = "${safeName}";
                        const GM_info = Object.freeze({
                            script: Object.freeze({
                                name: __SCRIPT_NAME__,
                                namespace: "${safeNamespace}",
                                version: "${safeVersion}"
                            })
                        });
                        const GM_setValue = (key, value) => AndroidGM.setValue(__SCRIPT_ID__, key, JSON.stringify(value));
                        const GM_getValue = (key, defaultValue) => {
                            const val = AndroidGM.getValue(__SCRIPT_ID__, key, null);
                            return val ? JSON.parse(val) : defaultValue;
                        };
                        const GM_deleteValue = (key) => AndroidGM.deleteValue(__SCRIPT_ID__, key);
                        const GM_listValues = () => JSON.parse(AndroidGM.listValues(__SCRIPT_ID__));
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
                        const GM_log = (msg) => AndroidGM.log(__SCRIPT_NAME__ + ": " + msg);
                        const GM_registerMenuCommand = (name, fn) => {
                            AndroidGM.registerMenuCommand(__SCRIPT_ID__, name);
                            const commands = (typeof unsafeWindow !== 'undefined' ? unsafeWindow : window).gm_commands || {};
                            commands[__SCRIPT_ID__ + "_" + name] = fn;
                            if (typeof unsafeWindow !== 'undefined') unsafeWindow.gm_commands = commands;
                            else window.gm_commands = commands;
                        };
                        const GM_xmlhttpRequest = (details) => {
                            console.warn('GM_xmlhttpRequest is not fully implemented yet');
                        };

                        // Expose the API surface to the user script body via an
                        // IIFE-bound `__GM_API__` reference. The user script is
                        // wrapped below to receive it as a parameter so that the
                        // GM_* identifiers are reachable without ever touching
                        // `window.GM_*`.
                        const __GM_API__ = {
                            GM_info,
                            GM_setValue,
                            GM_getValue,
                            GM_deleteValue,
                            GM_listValues,
                            GM_addStyle,
                            GM_log,
                            GM_registerMenuCommand,
                            GM_xmlhttpRequest
                        };

                        // Run the user script with destructured GM_* bindings.
                        try {
                            (function({
                                GM_info,
                                GM_setValue,
                                GM_getValue,
                                GM_deleteValue,
                                GM_listValues,
                                GM_addStyle,
                                GM_log,
                                GM_registerMenuCommand,
                                GM_xmlhttpRequest
                            }) {
                                $scriptContent
                            })(__GM_API__);
                        } catch (e) {
                            console.error('Script [$safeName] Error:', e);
                        }
                    })();
                """.trimIndent()
		val wrappedScript = gmPolyfill
		webView.evaluateJavascript(wrappedScript, null)
	}
	
	/**
	 * 检测脚本更新
	 * @return 如果有新版本，返回解析后的新实体，否则返回 null
	 */
	suspend fun checkForUpdate(entity: JavaScriptEntity): JavaScriptEntity? {
		val updateUrl = entity.updateURL ?: entity.downloadURL ?: return null
		// SECURITY: refuse to fetch userscript updates over plain HTTP — that
		// would let any on-path attacker transparently swap the script body
		// for a malicious payload while the user thinks they're getting a
		// legitimate upgrade.
		if (!updateUrl.startsWith("https://")) {
			Log.w(
				"GM_Script",
				"Refusing non-HTTPS userscript update URL: $updateUrl"
			)
			return null
		}
		val remoteEntity = ScriptParser.parseFromUrl(updateUrl) ?: return null
		val localVersion = entity.version ?: "0"
		val remoteVersion = remoteEntity.version ?: "0"
		println("localVersion: $localVersion, remoteVersion: $remoteVersion")
		if (compareVersion(remoteVersion, localVersion) > 0) {
			return remoteEntity/*.apply {
				id = entity.id
				position = entity.position
				state = entity.state
			}*/
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
		(0 until length).forEach { i ->
			val p1 = parts1.getOrNull(i)
			val p2 = parts2.getOrNull(i)
			
			if (p1 == p2) return@forEach
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
	 * SECURITY: `<all_urls>` is a Userscript metadata convention meaning "match
	 * every HTTP/HTTPS URL". In a desktop userscript engine this is a normal
	 * setting; in our embedded WebView it is potentially dangerous because the
	 * same engine also runs site JS for arbitrary origins (banking, email, etc).
	 * To reduce the abuse surface we still honour `<all_urls>` but require an
	 * explicit opt-in flag on the entity (`entity.run == 1`). Userscripts that
	 * rely on `<all_urls>` should set that flag manually; default-installed
	 * scripts from our assets do so.
	 *
	 * The pure pattern matcher is exposed for callers that need to inspect a
	 * specific pattern independently from the entity flag.
	 * */
	fun matchUrl(pattern: String, url: String): Boolean {
		return try { // 将通配符模式转换为正则表达式
			val regex = pattern.replace(".", "\\.").replace("*", ".*").replace("?", "\\?")

			Regex("^$regex$").containsMatchIn(url)
		} catch (_: Exception) {
			false
		}
	}

	/**
	 * Match with `<all_urls>` semantics gated by an explicit opt-in flag.
	 * Callers should use this instead of `matchUrl` directly so that
	 * untrusted user scripts cannot silently gain universal URL coverage.
	 */
	fun matchUrlWithAllUrls(pattern: String, url: String, allowAllUrls: Boolean): Boolean {
		if (pattern == "<all_urls>") return allowAllUrls
		return matchUrl(pattern, url)
	}
}
