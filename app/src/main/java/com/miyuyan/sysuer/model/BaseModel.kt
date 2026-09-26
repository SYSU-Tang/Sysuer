package com.miyuyan.sysuer.model

import android.content.Context
import android.os.Looper
import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.alibaba.fastjson2.JSONValidator
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.AuthorizationManager
import com.miyuyan.sysuer.api.ContextUtil
import com.miyuyan.sysuer.api.CookieManager
import com.miyuyan.sysuer.api.HttpManager
import com.miyuyan.sysuer.view.UiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 基础网络模型抽象类 (BaseModel)
 *
 * 负责统一管理 OkHttp 并发网络请求、网络请求生命周期、登录拦截重载逻辑以及 View 层的响应 UI 状态 (UiState)。
 *
 * 特性：
 * 1. 所有网络请求并行发起，无需排队等待；
 * 2. 遇到身份过期或需登录时，自动拦截任务，登录完成后批量并行重启所有挂起任务；
 * 3. 记录失败请求，提供统一的 [retryAll] / [retryFailed] 函数供 UI 或 ViewModel 重启所有失败请求。
 *
 * @param context Android 上下文对象
 */
abstract class BaseModel(context: Context) {

	/** 上下文工具类，提供 Toast、登录跳转与 Disposable 订阅管理 */
	val contextUtil: ContextUtil = ContextUtil(context)

	/** 抽象鉴权管理器，由具体子类配置相应的 WebVPN / Host 与登录地址 */
	abstract val authorizationManager: AuthorizationManager

	/** HTTP 请求管理器，配置主线程 Handler、Cookie 管理器与缓存 */
	open val http: HttpManager = HttpManager().apply {
		cookieManager = CookieManager(context)
		setCache(context.cacheDir)
	}

	// ============================================================================================
	// 数据结构定义 (Data Classes & Enums)
	// ============================================================================================

	/**
	 * 表示请求任务在并发集合中的生命周期状态
	 */
	enum class JobStatus {
		IDLE,        // 初始状态
		EXECUTING,   // 正在发起网络请求 (并行执行中)
		SUCCESS,     // 请求成功完成
		FAILED,      // 请求失败
		RETRYING     // 正在重新尝试
	}

	/**
	 * 表示响应拦截判定的状态类型
	 */
	enum class ResponseStatus {
		NORMAL,                // 正常响应
		NEEDS_LOGIN,           // 需要登录 / 身份校验失效
		NEEDS_CAMPUS_NETWORK   // 需要校园网 / WebVPN 模式且不可直连访问
	}

	/**
	 * 网络请求任务数据类
	 *
	 * @property request OkHttp 请求对象
	 * @property requestCode 请求识别码，用于标识请求类型并映射 UI 状态与消息通道
	 * @property retryCount 已重试次数
	 * @property createdAt 任务创建时间戳（毫秒）
	 * @property status 当前任务状态
	 * @property isSync 请求发起模式：true 表示同步请求，false 表示异步请求
	 */
	data class RequestJob(
		val request: Request,
		val requestCode: Int,
		var retryCount: Int = 0,
		val createdAt: Long = System.currentTimeMillis(),
		var status: JobStatus = JobStatus.IDLE,
		var isSync: Boolean = false
	) {
//		/** 兼容旧版代码属性 [what] */
//		val what: Int get() = requestCode
//
//		/** 兼容三参数构造函数 (request, what, retryCount) */
//		constructor(request: Request, what: Int, retryCount: Int) : this(
//				request = request,
//				requestCode = what,
//				retryCount = retryCount,
//				createdAt = System.currentTimeMillis(),
//				status = JobStatus.IDLE,
//				isSync = false
//		)
//
//		/** 兼容四参数构造函数 (request, what, retryCount, isSync) */
//		constructor(request: Request, what: Int, retryCount: Int, isSync: Boolean) : this(
//				request = request,
//				requestCode = what,
//				retryCount = retryCount,
//				createdAt = System.currentTimeMillis(),
//				status = JobStatus.IDLE,
//				isSync = isSync
//		)
	}

//	/**
//	 * 网络响应结果包装数据类
//	 *
//	 * @property requestCode 请求识别码
//	 * @property data 响应的 JSON 数据
//	 * @property isSuccess 请求与业务逻辑是否成功
//	 * @property errorMessage 错误信息
//	 */
//	data class ResponseResult(
//		val requestCode: Int,
//		val data: JSONObject?,
//		val isSuccess: Boolean = true,
//		val errorMessage: String? = null,
//	) {
//		/** 转换为旧版 [Pair] 对象 */
//		fun toTuple(): Pair<Int, JSONObject>? {
//			return data?.let { Pair(requestCode, it) }
//		}
//	}

	// ============================================================================================
	// 状态管理与事件通道
	// ============================================================================================

	/** 内部消息通道，用于向 ViewModel / UI 传递 (requestCode to JSON) 数据响应 */
	private val _message = MutableSharedFlow<Pair<Int, JSONObject>>(
			extraBufferCapacity = 256
	)

	/** 供外部 ViewModel 订阅的消息 Flow 通道 */
	val message: SharedFlow<Pair<Int, JSONObject>> = _message.asSharedFlow()

	/** 按 requestCode 存储并维护每个请求所对应的 UI 状态 (UiState) */
	private val stateMap = ConcurrentHashMap<Int, MutableStateFlow<UiState>>()

	/** 记录所有已发起的请求任务映射 (requestCode -> RequestJob) */
	private val allJobs = ConcurrentHashMap<Int, RequestJob>()

	/** 记录请求失败的任务映射 (requestCode -> RequestJob)，便于集中重试 */
	private val failedJobs = ConcurrentHashMap<Int, RequestJob>()

	/** 标识当前是否正在触发登录流程 */
	private val isLoggingIn = AtomicBoolean(false)

	/** 登录期间拦截并暂存的待重试请求列表 (线程安全) */
	private val pendingLoginJobs = ConcurrentLinkedQueue<RequestJob>()

	/** 最大允许重试次数 */
	protected open val maxRetryCount: Int = 2

	// ============================================================================================
	// UI 状态获取与更新
	// ============================================================================================

	/**
	 * 获取指定请求码 [requestCode] 对应的 [UiState] StateFlow。若不存在则自动创建。
	 *
	 * @param requestCode 请求识别码
	 * @return 可观测的 [MutableStateFlow<UiState>]
	 */
	fun getUiState(requestCode: Int): MutableStateFlow<UiState> =
		stateMap.getOrPut(requestCode) { MutableStateFlow(UiState.Unstarted) }

	/**
	 * 更新指定 [requestCode] 的 UI 状态
	 *
	 * @param requestCode 请求识别码
	 * @param newState 新的 [UiState] 状态
	 */
	fun updateUiState(requestCode: Int, newState: UiState) {
		getUiState(requestCode).value = newState
	}

	// ============================================================================================
	// 并发请求发起 (Concurrent Execution)
	// ============================================================================================

//	/** 最新入队的请求任务 */
//	@Volatile
//	private var lastJob: RequestJob? = null

//	/** 获取最新入队的请求任务 (兼容 [nextRequest] 属性) */
//	val nextRequest: Pair<Request, Int>?
//		get() = lastJob?.let { Pair(it.request, it.requestCode) }

	/**
	 * 提交请求任务并立即并发执行（不排队）
	 *
	 * @param job 请求任务对象
	 * @return 提交的 [RequestJob]
	 */
	fun enqueueRequest(job: RequestJob): RequestJob {
//		lastJob = job
		allJobs[job.requestCode] = job
		executeJobAsync(job)
		return job
	}

	/**
	 * 根据 [Request] 与请求码提交并立即并发执行
	 */
	fun enqueueRequest(request: Request, requestCode: Int): RequestJob {
		return enqueueRequest(RequestJob(request, requestCode))
	}

	/**
	 * 构建 URL 请求并立即并发执行
	 *
	 * @param path 相对路径或 API 路径
	 * @param data 请求体数据 (字符串/JSON)
	 * @param type 请求内容类型 (如 application/json)
	 * @param requestCode 请求识别码
	 */
	fun enqueueUrlRequest(
		path: String?, data: String? = null, type: String? = null, requestCode: Int
	): RequestJob {
		val url = "https://${authorizationManager.host}/$path"
		val request = http.generateRequest(url, data, type).build()
		return enqueueRequest(RequestJob(request, requestCode))
	}

	// ============================================================================================
	// 请求执行逻辑 (Async & Sync Execution)
	// ============================================================================================

	/**
	 * 异步并发执行单个 [RequestJob] 任务
	 *
	 * @param job 待执行的任务
	 */
	protected open fun executeJobAsync(job: RequestJob) {
		job.status = JobStatus.EXECUTING
		updateUiState(job.requestCode, UiState.Loading)

		http.client.newCall(job.request).enqueue(object : Callback {
			override fun onFailure(call: Call, e: IOException) {
				job.status = JobStatus.FAILED
				failedJobs[job.requestCode] = job
				updateUiState(job.requestCode, UiState.Error)
				handleFailure(job, e)
			}

			@Throws(IOException::class)
			override fun onResponse(call: Call, response: Response) {
				response.use { res ->
					handleResponse(job, res)
				}
			}
		})
	}

	/**
	 * 同步执行 HTTP 请求，并直接返回解析结果
	 *
	 * @param request OkHttp 请求对象
	 * @param requestCode 请求识别码
	 * @return 响应 Tuple2 或 null
	 */
	fun executeSync(request: Request, requestCode: Int): Pair<Int, JSONObject>? {
		val job = RequestJob(
				request = request, requestCode = requestCode, isSync = true
		)
		allJobs[requestCode] = job
		return executeSyncInternal(job)
	}

	/**
	 * 同步执行单个 [RequestJob] 任务
	 */
	protected open fun executeSyncInternal(job: RequestJob): Pair<Int, JSONObject>? {
		job.status = JobStatus.EXECUTING
		job.isSync = true
		updateUiState(job.requestCode, UiState.Loading)

		return try {
			val response = http.client.newCall(job.request).execute()
			response.use { res ->
				handleResponse(job, res)
			}
		} catch (e: IOException) {
			job.status = JobStatus.FAILED
			failedJobs[job.requestCode] = job
			updateUiState(job.requestCode, UiState.Error)
			handleFailure(job, e)
			null
		}
	}

	/**
	 * 直接发起原生 OkHttp 异步回调请求
	 */
	fun executeAsync(request: Request, callback: Callback) {
		http.client.newCall(request).enqueue(callback)
	}

	/**
	 * 根据相对路径发起原生 OkHttp 异步回调请求
	 */
	fun executeAsync(path: String, data: String? = null, type: String? = null, callback: Callback) {
		executeAsync(
				http.generateRequest("https://${authorizationManager.host}/$path", data, type)
					.build(), callback
		)
	}

	// ============================================================================================
	// 响应与异常处理 (Response & Failure Handling)
	// ============================================================================================

	/**
	 * 结合 HTTP 状态码 [code]、响应体文本 [content] 以及解析后的 [json] 对象综合判断响应状态。
	 *
	 * 基类仅进行基础 HTTP 状态码与非 JSON 文本的鉴权校验。若响应为 JSON，非通用字段的校验在各自 Model 中进行重写。
	 *
	 * @param code HTTP 响应状态码 (response.code)
	 * @param content 响应体原始文本
	 * @param json 尝试解析出的 JSON 对象（若非 JSON 响应则为 null）
	 * @return [ResponseStatus] 枚举：NORMAL (正常)、NEEDS_LOGIN (需登录)、NEEDS_CAMPUS_NETWORK (需校园网)
	 */
	protected open fun checkResponseStatus(
		code: Int, content: String, json: JSONObject?
	): ResponseStatus {
		// 1. 通过 HTTP 状态码进行初步判断
		when (code) {
			302, 401 -> return ResponseStatus.NEEDS_LOGIN
			403, 0 -> return ResponseStatus.NEEDS_CAMPUS_NETWORK
		}

		// 2. 非 JSON 类型 (HTML/纯文本) 才进行 AuthorizationManager 鉴权/网络可达性判断
		if (json == null) {
			if (!authorizationManager.isAuthorized(content)) {
				return ResponseStatus.NEEDS_LOGIN
			}
			if (!authorizationManager.isAccessible(content)) {
				return ResponseStatus.NEEDS_CAMPUS_NETWORK
			}
		}

		return ResponseStatus.NORMAL
	}

//	/**
//	 * 兼容旧版 [isLoginRequired] 函数，转接至 [checkResponseStatus]
//	 */
//	protected open fun isLoginRequired(response: Response, content: String): Boolean {
//		return checkResponseStatus(response.code, content, null) == ResponseStatus.NEEDS_LOGIN
//	}

	/**
	 * 处理 OkHttp 请求响应（接受 [RequestJob] 参数）
	 * 默认转交给带 Tuple2 参数的 [handleResponse] 方法，方便子类重写
	 */
	protected open fun handleResponse(
		job: RequestJob,
		response: Response,
	): Pair<Int, JSONObject>? {
		return handleResponse(Pair(job.request, job.requestCode), response)
	}

	/**
	 * 处理 OkHttp 请求响应（默认通用实现）
	 * 子类如需定制解析（例如 GymModel, JwxtModel 等）可重写此方法或重写 [checkResponseStatus]
	 */
	protected open fun handleResponse(
		request: Pair<Request, Int>,
		response: Response,
	): Pair<Int, JSONObject>? {
		val content = response.body.string()
		val isJsonContentType =
			response.header("Content-Type")?.contains("application/json") == true
		val json = if (isJsonContentType || JSONValidator.from(content).validate()) {
			try {
				val parsed = JSON.parse(content)
				if (parsed is JSONArray) JSONObject.of("data", parsed) else parsed as? JSONObject
			} catch (_: Exception) {
				null
			}
		} else null

		return when (checkResponseStatus(response.code, content, json)) {
			ResponseStatus.NEEDS_LOGIN -> {
				login(request)
				null
			}

			ResponseStatus.NEEDS_CAMPUS_NETWORK -> {
				authorizationManager.isAccessible = false
				retry(request)
				null
			}

			ResponseStatus.NORMAL -> {
				val result = json?.let { contentJSON ->
//					if (contentJSON.getInteger("code") != 200) {
//						toast(
//								contentJSON.getString("message") ?: contentJSON.getString(
//										"msg",
//										content
//								)
//
//						)
//					}
					Pair(request.second, contentJSON)
				} ?: Pair(request.second, JSONObject.of("data", content))
				_message.tryEmit(result)
				failedJobs.remove(request.second)
				pendingLoginJobs.removeIf { it.requestCode == request.second }
				allJobs[request.second]?.status = JobStatus.SUCCESS
				updateUiState(request.second, UiState.Content)
				Pair(result.first, result.second)
			}
		}
	}

	/**
	 * 处理网络请求失败异常
	 */
	protected open fun handleFailure(job: RequestJob, e: IOException) {
		handleFailure(Pair(job.request, job.requestCode), e)
	}

	/**
	 * 处理网络请求失败异常（接受 Tuple2 参数，可被子类重写）
	 */
	protected open fun handleFailure(
		request: Pair<Request, Int>,
		e: IOException,
	) {
		e.printStackTrace()
		toast(R.string.no_net_connected)
	}

	/**
	 * 触发登录流程并在登录完成后重启挂起的请求任务
	 *
	 * @param job 因身份过期的请求任务
	 */
	fun triggerLoginAndPendingRetry(job: RequestJob) {
		if (pendingLoginJobs.none { it.requestCode == job.requestCode }) {
			pendingLoginJobs.add(job)
		}
		if (!isLoggingIn.compareAndSet(false, true)) return

		login {
			val pendingList = mutableListOf<RequestJob>()
			while (pendingLoginJobs.isNotEmpty()) {
				pendingLoginJobs.poll()?.let { pendingList.add(it) }
			}
			isLoggingIn.set(false)

			pendingList.forEach { pendingJob ->
				val updatedJob = RequestJob(
						request = updateRequest(pendingJob.request),
						requestCode = pendingJob.requestCode,
						retryCount = pendingJob.retryCount,
						status = JobStatus.RETRYING,
						isSync = pendingJob.isSync
				)
				allJobs[pendingJob.requestCode] = updatedJob
				if (updatedJob.isSync) {
					if (Looper.myLooper() == Looper.getMainLooper()) {
						Thread { executeSyncInternal(updatedJob) }.start()
					} else {
						executeSyncInternal(updatedJob)
					}
				} else {
					executeJobAsync(updatedJob)
				}
			}
		}
	}

	/**
	 * 触发登录（带回调）
	 */
	fun login(afterLogin: () -> Unit) {
		contextUtil.login(authorizationManager.targetUrl, afterLogin)
	}

	/**
	 * 重启/重试单个请求任务 (依据原始 [RequestJob.isSync] 方式自动采用同步或异步重启)
	 *
	 * @param job 待重试的任务
	 */
	open fun retry(job: RequestJob) {
		if (job.retryCount >= maxRetryCount) {
			job.status = JobStatus.FAILED
			failedJobs[job.requestCode] = job
			updateUiState(job.requestCode, UiState.Error)
			return
		}
		job.retryCount++
		job.status = JobStatus.RETRYING
		val updatedJob = RequestJob(
				request = updateRequest(job.request),
				requestCode = job.requestCode,
				retryCount = job.retryCount,
				createdAt = job.createdAt,
				status = JobStatus.RETRYING,
				isSync = job.isSync
		)
		allJobs[job.requestCode] = updatedJob
		failedJobs.remove(job.requestCode)

		if (updatedJob.isSync) {
			if (Looper.myLooper() == Looper.getMainLooper()) {
				Thread { executeSyncInternal(updatedJob) }.start()
			} else {
				executeSyncInternal(updatedJob)
			}
		} else {
			executeJobAsync(updatedJob)
		}
	}

	/**
	 * 重试 Tuple2 格式的请求
	 */
	open fun retry(request: Pair<Request, Int>) {
		retry(RequestJob(request.first, request.second))
	}

	/**
	 * 重试指定 [requestCode] 的请求任务
	 */
	open fun retry(requestCode: Int) {
		allJobs[requestCode]?.let { retry(it) } ?: failedJobs[requestCode]?.let { retry(it) }
	}

	/**
	 * **重启所有失败的网络请求（并发执行）**
	 *
	 * 遍历所有处于 [JobStatus.FAILED] 状态或记录在 [failedJobs] 中的请求，更新授权/Cookie 信息后同时重新发起请求。
	 */
	open fun retryAll() {
		val jobsToRetry = failedJobs.values.toList().ifEmpty {
			allJobs.values.filter { it.status == JobStatus.FAILED }
		}
		failedJobs.clear()
		jobsToRetry.forEach { job ->
			retry(job)
		}
	}

	/**
	 * 别名函数：重启所有失败的网络请求
	 */
	fun retryFailed() = retryAll()

	/**
	 * 重新构建 Request，更新 Host、Cookie 与 Token 授权头信息
	 */
	fun updateRequest(request: Request): Request {
		val newRequest = request.newBuilder().url(request.url.newBuilder().host(host).build())
			.header("Cookie", cookie)
		if (http.isTokenRequired) newRequest.header("token", token)
		if (http.isAuthorizationRequired) newRequest.header("Authorization", authorization)
		return newRequest.build()
	}

//	/**
//	 * 发送响应数据消息到 [messageChannel]
//	 */
//	protected fun sendMessage(requestCode: Int, data: JSONObject) {
//		_messageChannel.tryEmit(requestCode to data)
//	}

//	/**
//	 * 发送 [Pair] 响应数据消息到 [messageChannel]
//	 */
//	protected fun sendMessage(result: Pair<Int, JSONObject>) {
//		_messageChannel.tryEmit(result.first to result.second)
//	}

	/**
	 * 释放并清理所有任务与状态资源
	 */
	fun dispose() {
		allJobs.clear()
		failedJobs.clear()
		pendingLoginJobs.clear()
		contextUtil.dispose()
	}

	// ============================================================================================
	// 快捷属性 getter
	// ============================================================================================

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

	fun enqueueUrl(url: String, data: String? = null, type: String? = null, code: Int) {
		enqueueRequest(http.generateRequest(url, data, type).build(), code)
	}

	fun enqueue(path: String, data: String? = null, type: String? = null, code: Int) =
		enqueueUrlRequest(path, data, type, code)

	fun enqueue(path: String, data: String?, code: Int) = enqueueUrlRequest(path, data, null, code)

	fun enqueue(path: String, code: Int) = enqueueUrlRequest(path, null, null, code)

	fun enqueue(request: Request, code: Int) = enqueueRequest(request, code)

	fun login(job: RequestJob) = triggerLoginAndPendingRetry(job)
	fun login(request: Pair<Request, Int>) = login(RequestJob(request.first, request.second))

	fun execute(request: Pair<Request, Int>) = executeSync(request.first, request.second)

	fun execute(request: Request, code: Int) = executeSync(request, code)

	fun execute(path: String, data: String? = null, type: String? = null, code: Int) =
		executeSync(http.generateRequest(path, data, type).build(), code)


	fun call(path: String, data: String? = null, type: String? = null, callback: Callback) =
		executeAsync(path, data, type, callback)

	fun call(request: Request, callback: Callback) = executeAsync(request, callback)

	fun toast(message: String) = contextUtil.toast(message)

	fun toast(resId: Int) = contextUtil.toast(resId)
}