package com.sysu.edu.model

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.R
import com.sysu.edu.api.AuthorizationManager
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.ContextUtil
import com.sysu.edu.api.CookieManager
import com.sysu.edu.api.HttpManager
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.ArrayDeque

abstract class BaseModel(context: Context) {
	val contextUtil: ContextUtil = ContextUtil(context)
	abstract val authorizationManager: AuthorizationManager
	open val http: HttpManager = HttpManager(Handler(Looper.getMainLooper())).apply {
		cookieManager = CookieManager(context)
		setCache(context.cacheDir)
	}
	private val queue = ArrayDeque<CommonUtil.Tuple2<Request, Int>>()
	val message: MutableLiveData<CommonUtil.Tuple2<Int, JSONObject>> = MutableLiveData<CommonUtil.Tuple2<Int, JSONObject>>()
	val afterLoginRequest: MutableSet<CommonUtil.Tuple2<Request, Int>?> = mutableSetOf()
	fun add(request: Request, what: Int) {
		queue.add(CommonUtil.Tuple2(request, what))
	}
	
	fun add(path: String?, what: Int) {
		add(path, null, null, what)
	}
	
	fun add(path: String?, data: String?, what: Int) {
		add(path, data, null, what)
	}
	
	fun add(path: String?, data: String? = null, type: String? = null, what: Int) {
		queue.add(CommonUtil.Tuple2(http.generateRequest("https://${authorizationManager.host}/$path",
		                                                 data,
		                                                 type).build(), what))
	}
	
	fun next() {
		val request: CommonUtil.Tuple2<Request, Int>? = nextRequest
		request?.let { request(it) }
	}
	
	val nextRequest: CommonUtil.Tuple2<Request, Int>?
		get() = queue.poll()
	
	fun nextAll() {
		while (!queue.isEmpty()) next()
	}
	
	fun addAndNext(path: String?, data: String? = null, type: String? = null, code: Int) {
		add(path, data, type, code)
		next()
	}
	
	fun addAndNext(path: String?, data: String?, code: Int) {
		addAndNext(path, data, null, code)
	}
	
	fun addAndNext(path: String?, code: Int) {
		addAndNext(path, null, code)
	}
	
	fun login(request: CommonUtil.Tuple2<Request, Int>?) {
		val empty = afterLoginRequest.isEmpty()
		afterLoginRequest.add(request)
		if (empty) login {
			afterLoginRequest.forEach { request: CommonUtil.Tuple2<Request, Int>? -> retry(request!!) }
		}
	}
	
	fun login(afterLogin: () -> Unit) {
		contextUtil.login(authorizationManager.targetUrl, afterLogin)
	}
	
	fun request(request: CommonUtil.Tuple2<Request, Int>) {
		http.client.newCall(request.first).enqueue(object : Callback {
			override fun onFailure(call: Call, e: IOException) {
				handleFailure(request, e)
			}
			
			@Throws(IOException::class) override fun onResponse(call: Call, response: Response) {
				handleResponse(request, response)
			}
		})
	}
	
	protected open fun handleFailure(request: CommonUtil.Tuple2<Request, Int>, e: IOException) {
		e.printStackTrace()
		http.handler.post { contextUtil.toast(R.string.no_net_connected) }
	}
	
	fun execute(request: CommonUtil.Tuple2<Request, Int>): CommonUtil.Tuple2<Int, JSONObject>? {
		val call = http.client.newCall(request.first)
		try {
			return handleResponse(request, call.execute())
		} catch (e: IOException) {
			handleFailure(request, e)
		}
		return null
	}
	
	fun execute(request: Request, code: Int): CommonUtil.Tuple2<Int, JSONObject>? {
		try {
			return handleResponse(CommonUtil.Tuple2(request, code),
			                      http.client.newCall(request).execute())
		} catch (e: IOException) {
			handleFailure(CommonUtil.Tuple2(request, code), e)
			return null
		}
	}
	
	fun run(path: String, data: String? = null, type: String? = null, callback: Callback) {
		run(http.generateRequest("https://${authorizationManager.host}/$path", data, type).build(),
		    callback)
	}
	
	fun run(request: Request, callback: Callback) {
		http.client.newCall(request).enqueue(callback)
	}
	
	protected open fun handleResponse(request: CommonUtil.Tuple2<Request, Int>,
	                                  response: Response): CommonUtil.Tuple2<Int, JSONObject>? {
		val content = response.body.string()
		var result: CommonUtil.Tuple2<Int, JSONObject>? = null
		response.header("Content-Type")?.takeIf { it.contains("application/json") }?.let {
			val contentJSON = JSONObject.parse(content)
			val code = contentJSON.getInteger("code")
			if (code == 53000007) login(request)
			else {
				if (code != 200) http.handler.post {
					contextUtil.toast(CommonUtil.toStringOrDefault(contentJSON.getString("message")))
				}
				result = CommonUtil.Tuple2(request.second, contentJSON)
				message.postValue(result)
				afterLoginRequest.remove(request)
			}
		} ?: run {
			if (!authorizationManager.isAuthorized(content)) login(request)
			else if (!authorizationManager.isAccessible(content)) retry(request)
		}
		return result
	}
	
	protected open fun retry(request: CommonUtil.Tuple2<Request, Int>) {
		request.first = updateRequest(request.first)
		request(request)
	}
	
	fun request(request: Request, code: Int) {
		request(CommonUtil.Tuple2(request, code))
	}
	
	val host: String
		get() = authorizationManager.host
	val cookieManager: CookieManager?
		get() = http.cookieManager
	
	open fun updateRequest(request: Request): Request {
		val newRequest = request.newBuilder()
			.url(request.url.newBuilder().host(host).build())
			.header("Cookie", cookie)
		if (http.isTokenRequired) newRequest.header("token", token)
		if (http.isAuthorizationRequired) newRequest.header("Authorization", authorization)
		return newRequest.build()
	}
	
	fun dispose() {
		contextUtil.dispose()
	}
	
	val cookie: String
		get() = cookieManager?.toSimpleString(host) ?: ""
	val authorization: String
		get() = http.authorizationJar?.getAuthorization(host) ?: ""
	val token: String
		get() = http.authorizationJar?.getToken(host) ?: ""
}


