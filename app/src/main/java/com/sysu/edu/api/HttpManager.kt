package com.sysu.edu.api

import android.os.Bundle
import android.os.Handler
import android.os.Message
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

/**
 * 构造函数
 *
 * @param handler 处理消息的 Handler 对象
 */
class HttpManager(val handler: Handler) {
	/**
	 * 获取 OkHttpClient 客户端
	 * 
	 * @return OkHttpClient 客户端
	 */
	val client: OkHttpClient = OkHttpClient.Builder().build() // 全局 OkHttpClient 实例
	
	/**
	 * 获取 CookieManager 管理器
	 * 
	 * @return CookieManager 管理器
	 */
	var cookieManager: CookieManager? = null // 全局 CookieManager 实例
	@JvmField var referrer: String? = null // Referer 头字段值
	@JvmField var cookie: String? = null // Cookie 头字段值
	@JvmField var authorization: String? = null // Authorization 头字段值
	@JvmField var config: Config? = null // 请求参数对象
	@JvmField var ua: String? = null // User-Agent 头字段值
	@JvmField var target: String? = null // 目标 URL
	@JvmField var isAuthorizationRequired: Boolean = false // 是否需要 Authorization 头字段
	@JvmField var isTokenRequired: Boolean = false // 是否需要 token 头字段
	@JvmField var header: MutableMap<String?, String?>? = null // 自定义请求头字段
	@JvmField
	var authorizationJar: AuthorizationJar? = null // 自定义 Authorization 头字段 //	init {
	//		setHandler(handler)
	//	}
	//	/**
	//	 * 获取处理消息的 Handler 对象
	//	 *
	//	 * @return 处理消息的 Handler 对象
	//	 */
	//	fun getHandler(): Handler {
	//		return handler!!
	//	}
	//	/**
	//	 * 设置处理消息的 Handler 对象
	//	 *
	//	 * @param handler 处理消息的 Handler 对象
	//	 */
	//	fun setHandler(handler: Handler) {
	//		this.handler = handler
	//	}
	/**
	 * 设置请求参数
	 *
	 * @param config 请求参数对象
	 */
	fun setParams(config: Config) {
		this.config = config
		cookieManager = CookieManager(config.context)
		authorizationJar = AuthorizationJar(config.context)
		}
	
	/**
	 * 设置 Referer 头字段
	 *
	 * @param referrer Referer 头字段值
	 */
	fun setReferrer(referrer: String?) {
		this.referrer = referrer
	}
	
	/**
	 * 设置 Cookie 头字段，优先级最高
	 *
	 * @param cookie Cookie 头字段值
	 */
	fun setCookie(cookie: String?) {
		this.cookie = cookie
	}
	
	/**
	 * 设置是否需要 Authorization 头字段
	 *
	 * @param isAuthorizationRequired 是否需要使用 Params 中的 Authorization 头字段
	 */
	fun setAuthorizationRequired(isAuthorizationRequired: Boolean) {
		this.isAuthorizationRequired = isAuthorizationRequired
	}
	
	/**
	 * 设置是否需要 token 头字段
	 *
	 * @param isTokenRequired 是否需要使用 Params 中的 token 头字段
	 */
	fun setTokenRequired(isTokenRequired: Boolean) {
		this.isTokenRequired = isTokenRequired
	}
	
	/**
	 * 设置 User-Agent 头字段
	 *
	 * @param ua User-Agent 头字段值
	 */
	fun setUA(ua: String?) {
		this.ua = ua
	}
	
	/**
	 * 设置请求目标 URL
	 *
	 * @param target 请求目标 URL作为Cookie的提供者
	 */
	fun setTarget(target: String?) {
		this.target = target
	}
	
	/**
	 * 设置请求头字段
	 *
	 * @param header 请求头字段映射
	 */
	fun setHeader(header: MutableMap<String?, String?>?) {
		this.header = header
	}
	
	/**
	 * 设置 AuthorizationJar 对象
	 *
	 * @param authorizationJar AuthorizationJar 对象
	 */
	fun setAuthorizationJar(authorizationJar: AuthorizationJar?) {
		this.authorizationJar = authorizationJar
	}
	
	/**
	 * 发送网络请求
	 * 
	 * @param request 请求对象
	 * @param what    消息标识
	 */
	fun sendRequest(request: Request, what: Int) {
		client.newCall(request).enqueue(object : Callback {
			override fun onFailure(call: Call,
			                       e: IOException) { //                System.out.println(request.url());
				sendFailure()
			}
			
			@Throws(IOException::class) override fun onResponse(call: Call, response: Response) {
				val msg = Message()
				msg.what = what
				msg.obj = response.body.string()
				val bundle = Bundle()
				bundle.putInt("code", response.code)
				val type = response.header("Content-Type")
				bundle.putBoolean("isJSON", type != null && type.contains("application/json"))
				bundle.putString("data", msg.obj as String?)
				msg.data = bundle
				handler.sendMessage(msg)
			}
		})
	}
	
	/**
	 * 发送失败消息
	 */
	fun sendFailure() {
		handler.sendEmptyMessage(-1)
	}
	
	/**
	 * 获取请求构建器
	 * 
	 * @param url  请求 URL
	 * @param data 请求数据
	 * @param type 请求数据类型
	 * @return 请求构建器
	 */
	fun generateRequest(url: String, data: String?, type: String?): Request.Builder {
		val request = Request.Builder().url(url)
		val host = url.toHttpUrl().host
		if (cookieManager != null) request.header("Cookie", cookieManager!!.toSimpleString(host))
		if (cookie != null) request.header("Cookie", cookie!!)
		if (isAuthorizationRequired && authorizationJar != null) request.header("Authorization", authorizationJar!!.getAuthorization(host))
		if (authorization != null) request.header("Authorization", authorization!!)
		if (referrer != null) request.header("Referer", referrer!!)
		if (ua != null) request.header("User-Agent", ua!!)
		if (data != null) request.post(data.toRequestBody((type
			?: "application/json").toMediaType()))
		if (isTokenRequired && authorizationJar != null) request.header("token", authorizationJar!!.getToken(host))
		header?.forEach { (name: String?, value: String?) -> request.header(name!!, value!!) }
		return request
	}
	
	/**
	 * 生成 GET 请求构建器
	 * 
	 * @param url 请求 URL
	 * @return 请求构建器
	 */
	fun generateGetRequest(url: String): Request.Builder {
		return generateRequest(url, null, null)
	}
	
	/**
	 * 生成 POST 请求构建器
	 * 
	 * @param url  请求 URL
	 * @param data 请求数据
	 * @param type 请求数据类型
	 * @return 请求构建器
	 */
	fun generatePostRequest(url: String, data: String?, type: String?): Request.Builder {
		return generateRequest(url, data, type)
	}
	
	/**
	 * 生成 POST JSON 请求构建器
	 * 
	 * @param url  请求 URL
	 * @param data 请求 JSON 数据
	 * @return 请求构建器
	 */
	fun generatePostRequest(url: String, data: String?): Request.Builder {
		return generateRequest(url, data, "application/json")
	}
	
	/**
	 * 发送 POST 请求
	 * 
	 * @param url  请求 URL
	 * @param data 请求 JSON 数据
	 * @param what 消息标识
	 */
	fun postRequest(url: String, data: String?, what: Int) {
		sendRequest(generatePostRequest(url, data).build(), what)
	}
	
	/**
	 * 发送 POST 请求
	 * 
	 * @param url  请求 URL
	 * @param data 请求数据
	 * @param type 请求数据类型
	 * @param what 消息标识
	 */
	fun postRequest(url: String, data: String?, type: String?, what: Int) {
		sendRequest(generatePostRequest(url, data, type).build(), what)
	}
	
	/**
	 * 发送 GET 请求
	 * 
	 * @param url  请求 URL
	 * @param what 消息标识
	 */
	fun getRequest(url: String, what: Int) {
		sendRequest(generateGetRequest(url).build(), what)
	}
	
	/**
	 * 发送 DELETE 请求
	 * 
	 * @param url  请求 URL
	 * @param what 消息标识
	 */
	fun deleteRequest(url: String, what: Int) {
		sendRequest(generateGetRequest(url).delete().build(), what)
	}
}
