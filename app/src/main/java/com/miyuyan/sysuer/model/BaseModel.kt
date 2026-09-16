package com.miyuyan.sysuer.model

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.AuthorizationManager
import com.miyuyan.sysuer.api.CommonUtil
import com.miyuyan.sysuer.api.ContextUtil
import com.miyuyan.sysuer.api.CookieManager
import com.miyuyan.sysuer.api.HttpManager
import com.miyuyan.sysuer.view.UiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.Queue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

abstract class BaseModel(context: Context) {
	val contextUtil: ContextUtil = ContextUtil(context)
	abstract val authorizationManager: AuthorizationManager
	open val http: HttpManager = HttpManager(Handler(Looper.getMainLooper())).apply {
		cookieManager = CookieManager(context)
		setCache(context.cacheDir)
	}

	private val _messageChannel = MutableSharedFlow<Pair<Int, JSONObject>>(
		extraBufferCapacity = 256
	)
	val messageChannel = _messageChannel.asSharedFlow()

	private val state = ConcurrentHashMap<Int, MutableStateFlow<UiState>>()
	private val queue: Queue<RequestJob> = ConcurrentLinkedQueue()

	private val isRequesting = AtomicBoolean(false)
	private val isLoggingIn = AtomicBoolean(false)
	private val afterLoginJobs = mutableListOf<RequestJob>()

	protected open val maxRetryCount: Int = 2

	data class RequestJob(
		val request: Request,
		val what: Int,
		var retryCount: Int = 0,
	)

	fun getUiState(code: Int): MutableStateFlow<UiState> =
		state.getOrPut(code) { MutableStateFlow(UiState.Unstarted) }

	fun add(request: Request, what: Int) {
		enqueue(RequestJob(request, what))
	}

	fun add(path: String?, what: Int) {
		add(path, null, null, what)
	}

	fun add(path: String?, data: String?, what: Int) {
		add(path, data, null, what)
	}

	fun add(path: String?, data: String? = null, type: String? = null, what: Int) {
		val url = "https://${authorizationManager.host}/$path"
		enqueue(RequestJob(http.generateRequest(url, data, type).build(), what))
	}

	fun set(path: String?, data: String? = null, type: String? = null, what: Int) {
		enqueue(RequestJob(http.generateRequest("$path", data, type).build(), what))
	}

	fun setAndNext(path: String?, data: String? = null, type: String? = null, what: Int) {
		enqueue(RequestJob(http.generateRequest("$path", data, type).build(), what))
		next()
	}

	fun next() {
		if (!isRequesting.compareAndSet(false, true)) return
		val job = queue.poll() ?: run {
			isRequesting.set(false)
			return
		}
		execute(job)
	}

	val nextRequest: CommonUtil.Tuple2<Request, Int>?
		get() = queue.firstOrNull()?.let { CommonUtil.Tuple2(it.request, it.what) }

	fun nextAll() {
		while (queue.isNotEmpty()) next()
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

	private fun enqueue(job: RequestJob) {
		queue.add(job)
		if (!isRequesting.get()) next()
	}

	private fun execute(job: RequestJob) {
		getUiState(job.what).value = UiState.Loading
		val call = http.client.newCall(job.request)
		call.enqueue(object : Callback {
			override fun onFailure(call: Call, e: IOException) {
				isRequesting.set(false)
				getUiState(job.what).value = UiState.Error
				handleFailure(job, e)
				next()
			}

			@Throws(IOException::class)
			override fun onResponse(call: Call, response: Response) {
				isRequesting.set(false)
				response.use { response ->
					handleResponse(job, response)
				}
				next()
			}
		})
	}

	fun request(request: CommonUtil.Tuple2<Request, Int>) {
		enqueue(RequestJob(request.first, request.second))
	}

	fun request(request: Request, code: Int) {
		enqueue(RequestJob(request, code))
	}

	fun login(job: RequestJob) {
		synchronized(afterLoginJobs) {
			afterLoginJobs.add(job)
			if (!isLoggingIn.compareAndSet(false, true)) return
		}
		login {
			val pending: List<RequestJob>
			synchronized(afterLoginJobs) {
				pending = afterLoginJobs.toList()
				afterLoginJobs.clear()
				isLoggingIn.set(false)
			}
			pending.forEach { retry(it) }
		}
	}

	fun login(request: CommonUtil.Tuple2<Request, Int>) {
		login(RequestJob(request.first, request.second))
	}

	fun login(afterLogin: () -> Unit) {
		contextUtil.login(authorizationManager.targetUrl, afterLogin)
	}

	protected open fun handleFailure(job: RequestJob, e: IOException) {
		e.printStackTrace()
		http.handler.post { contextUtil.toast(R.string.no_net_connected) }
	}

	protected open fun handleFailure(
		request: CommonUtil.Tuple2<Request, Int>,
		e: IOException,
	) {
		handleFailure(RequestJob(request.first, request.second), e)
	}

	fun execute(request: CommonUtil.Tuple2<Request, Int>): CommonUtil.Tuple2<Int, JSONObject>? {
		val job = RequestJob(request.first, request.second)
		val call = http.client.newCall(job.request)
		return try {
			handleResponse(job, call.execute())
		} catch (e: IOException) {
			getUiState(job.what).value = UiState.Error
			handleFailure(job, e)
			null
		}
	}

	fun execute(request: Request, code: Int): CommonUtil.Tuple2<Int, JSONObject>? {
		val job = RequestJob(request, code)
		return try {
			handleResponse(job, http.client.newCall(request).execute())
		} catch (e: IOException) {
			getUiState(job.what).value = UiState.Error
			handleFailure(job, e)
			null
		}
	}

	fun run(path: String, data: String? = null, type: String? = null, callback: Callback) {
		run(
			http.generateRequest("https://${authorizationManager.host}/$path", data, type)
				.build(), callback
		)
	}

	fun run(request: Request, callback: Callback) {
		http.client.newCall(request).enqueue(callback)
	}

	protected open fun handleResponse(
		job: RequestJob,
		response: Response,
	): CommonUtil.Tuple2<Int, JSONObject>? {
		return handleResponse(CommonUtil.Tuple2(job.request, job.what), response)
	}

	protected open fun handleResponse(
		request: CommonUtil.Tuple2<Request, Int>,
		response: Response,
	): CommonUtil.Tuple2<Int, JSONObject>? {
		val content = response.body.string()
		var result: CommonUtil.Tuple2<Int, JSONObject>? = null
		response.header("Content-Type")?.takeIf { it.contains("application/json") }?.let {
			val contentJSON = JSONObject.parse(content)
			val code = contentJSON.getInteger("code")
			if (code == 53000007) login(request)
			else {
				if (code != 200) http.handler.post {
					contextUtil.toast(contentJSON.getString("message", ""))
				}
				result = CommonUtil.Tuple2(request.second, contentJSON)
				_messageChannel.tryEmit(result.first to result.second)
				synchronized(afterLoginJobs) { afterLoginJobs.removeAll { it.what == request.second } }
				getUiState(request.second).value = UiState.Content
			}
		} ?: run {
			if (!authorizationManager.isAuthorized(content)) login(request)
			else if (!authorizationManager.isAccessible(content)) retry(request)
		}
		return result
	}

	protected open fun retry(job: RequestJob) {
		if (job.retryCount >= maxRetryCount) {
			getUiState(job.what).value = UiState.Error
			synchronized(afterLoginJobs) { afterLoginJobs.removeAll { it.what == job.what } }
			return
		}
		job.retryCount++
		val updated = RequestJob(updateRequest(job.request), job.what, job.retryCount)
		execute(updated)
	}

	protected open fun retry(request: CommonUtil.Tuple2<Request, Int>) {
		retry(RequestJob(request.first, request.second))
	}

	protected fun sendMessage(what: Int, data: JSONObject) {
		_messageChannel.tryEmit(what to data)
	}

	protected fun sendMessage(result: CommonUtil.Tuple2<Int, JSONObject>) {
		_messageChannel.tryEmit(result.first to result.second)
	}

	fun updateRequest(request: Request): Request {
		val newRequest = request.newBuilder()
			.url(request.url.newBuilder().host(host).build())
			.header("Cookie", cookie)
		if (http.isTokenRequired) newRequest.header("token", token)
		if (http.isAuthorizationRequired) newRequest.header("Authorization", authorization)
		return newRequest.build()
	}

	fun dispose() {
		queue.clear()
		synchronized(afterLoginJobs) { afterLoginJobs.clear() }
		contextUtil.dispose()
	}

	val host: String
		get() = authorizationManager.host
	val cookieManager: CookieManager?
		get() = http.cookieManager

	val cookie: String
		get() = cookieManager?.toSimpleString(host) ?: ""
	val authorization: String
		get() = http.authorizationJar?.getAuthorization(host) ?: ""
	val token: String
		get() = http.authorizationJar?.getToken(host) ?: ""
}