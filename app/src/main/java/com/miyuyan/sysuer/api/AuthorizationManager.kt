package com.miyuyan.sysuer.api

import java.util.regex.Pattern

/**
 * 认证管理器
 * 用于处理认证相关的操作，包括判断是否认证和是否可访问
 */
/**
 * 构造函数
 *
 * @param originHost     原始 URL
 * @param substituteHost 替代 URL
 */
class AuthorizationManager(
	private val originHost: String, private var substituteHost: String
) {
	var isAuthorized: Boolean = true // 是否认证
	var isAccessible: Boolean = true // 是否可访问
	var originTargetUrl: String? = null // 原始 URL 目标 URL
	var substituteTargetUrl: String? = null // 替代 URL 目标 URL
	val host: String
		/**
		 * 获取根 URL
		 * 
		 * @return 根 URL，根据是否可访问返回原始 URL 或替代 URL
		 */
		get() = if (isAccessible) originHost else substituteHost
	val targetUrl: String
		/**
		 * 获取目标 URL
		 *
		 * @return 目标 URL，根据是否可访问返回原始 URL 或替代 URL
		 */
		get() = (if (isAccessible) originTargetUrl else substituteTargetUrl)!!

	/**
	 * 判断内容是否可访问
	 * 
	 * @param content 要判断的内容
	 * @return 如果内容中不包含"Access Forbidden"，则返回true；否则返回false
	 */
	fun isAccessible(content: String): Boolean {
		val isInaccessible = Pattern.compile("Access Forbidden").matcher(content).find()
		if (isInaccessible) isAccessible = false
		return !isInaccessible
	}
	/**
	 * 判断内容是否认证
	 * 
	 * @param content 要判断的内容
	 * @return 如果内容中不包含"中山大学统一身份认证"，则返回true；否则返回false
	 */
	fun isAuthorized(content: String): Boolean {
		val isContentUnauthorized = Pattern.compile("中山大学统一身份认证").matcher(content).find()
		if (isContentUnauthorized) isAuthorized = false
		return !isContentUnauthorized
	}

	/**
	 * 设置目标 URL
	 * @param origin 原始 URL
	 * @param substitute 替代 URL
	 * */
	fun setTargetUrl(origin: String, substitute: String) {
		originTargetUrl = origin
		substituteTargetUrl = substitute
	}
}
