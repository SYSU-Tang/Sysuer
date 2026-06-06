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
import com.sysu.edu.api.TargetUrl
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.ArrayDeque

class XgxtModel(context: Context) {
	val contextUtil: ContextUtil = ContextUtil(context)
	val authorizationManager: AuthorizationManager =
		AuthorizationManager("xgxt.sysu.edu.cn", "xgxt-443.webvpn.sysu.edu.cn")
	private val http = HttpManager(Handler(Looper.getMainLooper())).apply {
		setCookieManager(CookieManager(context))
	}
	private val queue = ArrayDeque<CommonUtil.Tuple2<Request, Int>>()
	val message: MutableLiveData<CommonUtil.Tuple2<Int, JSONObject>> =
		MutableLiveData<CommonUtil.Tuple2<Int, JSONObject>>()
	private val afterLoginRequest = mutableSetOf<CommonUtil.Tuple2<Request, Int>>()
	fun add(request: Request, what: Int) {
		queue.add(CommonUtil.Tuple2<Request, Int>(request, what))
	}
	
	fun add(path: String?, what: Int) {
		add(path, null, null, what)
	}
	
	fun add(path: String?, data: String?, what: Int) {
		add(path, data, null, what)
	}
	
	fun add(path: String?, data: String? = null, type: String? = null, what: Int) {
		queue.add(
			CommonUtil.Tuple2<Request, Int>(
				http.generateRequest(
					"https://${authorizationManager.baseUrl}/$path",
					data,
					type
				).build(), what
			)
		)
	}
	
	fun next() {
		queue.poll()?.let { request(it) }
	}
	
	fun nextAll() {
		while (!queue.isEmpty()) next()
	}
	
	fun addAndNext(path: String?, data: String?, type: String?, code: Int) {
		add(path, data, type, code)
		next()
	}
	
	fun addAndNext(path: String?, data: String?, code: Int) {
		addAndNext(path, data, null, code)
	}
	
	fun addAndNext(path: String?, code: Int) {
		addAndNext(path, null, code)
	}
	
	fun login(request: CommonUtil.Tuple2<Request, Int>) {
		afterLoginRequest.isEmpty().let {
			afterLoginRequest.add(request)
			if (it) contextUtil.login(
				if (authorizationManager.isAccessible()) TargetUrl.XGXT else TargetUrl.XGXT_WEBVPN
			) {
				afterLoginRequest.forEach { request -> retry(request) }
			}
		}
	}
	
	fun request(request: CommonUtil.Tuple2<Request, Int>) {
		http.client.newCall(request.getFirst()).enqueue(object : Callback {
			override fun onFailure(call: Call, e: IOException) {
				http.handler.post { contextUtil.toast(R.string.no_net_connected) }
			}
			
			override fun onResponse(call: Call, response: Response) {
				val content = response.body.string()
				val code = response.code
				when (code) {
					0 -> {
						authorizationManager.setAccessible(false)
						retry(request)
					}
					302 -> login(request)
					200 ->
						response.header("Content-Type")
							?.takeIf { it.contains("application/json") }
							?.let {
								val data = JSONObject.parse(content)
								val meta: JSONObject? =
									if (data.containsKey("meta")) data.getJSONObject(
										"meta") else null
								meta?.let {
									if (meta.containsKey("statusCode") && meta.getInteger(
											"statusCode") == 302)
										login(request)
									else http.handler.post {
										contextUtil.toast(
											CommonUtil.toStringOrDefault(
												meta.getString(
													"message"
												)
											)
										)
									}
								} ?: run {
									if (data.containsKey("code") && data.getInteger(
											"code") != 200)
										http.handler.post {
											contextUtil.toast(CommonUtil.toStringOrDefault(
												data.getString("msg")))
										}
									message.postValue(
										CommonUtil.Tuple2<Int, JSONObject>(
											request.getSecond(),
											data
										)
									)
									afterLoginRequest.remove(request)
								}
							} ?: run {
							if (!authorizationManager.isAuthorized(content)) login(request)
							else if (!authorizationManager.isAccessible(content)) retry(request)
						}
				}
			}
		})
	}
	
	private fun retry(request: CommonUtil.Tuple2<Request, Int>) {
		request.setFirst(updateRequest(request.getFirst()))
		request(request)
	}
	
	fun request(request: Request, code: Int) {
		request(CommonUtil.Tuple2<Request, Int>(request, code))
	}
	
	val host: String?
		get() = authorizationManager.baseUrl
	
	fun updateRequest(request: Request): Request {
		return request.newBuilder()
			.url(request.url.newBuilder().host(authorizationManager.baseUrl).build()).header(
				"Cookie",
				http.cookieManager.toSimpleString(authorizationManager.baseUrl)
			)
			.build()
	}
	
	fun dispose() {
		contextUtil.dispose()
	}
}
