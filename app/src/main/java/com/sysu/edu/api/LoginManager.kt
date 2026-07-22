package com.sysu.edu.api

import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.widget.ImageView
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.R
import com.sysu.edu.rainClass.QrCode
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.spec.X509EncodedKeySpec
import java.util.ArrayDeque
import java.util.Base64
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class LoginManager(private val context: Context) {
	val authorizationJar: AuthorizationJar = AuthorizationJar(context)
	private val timestamps = ArrayDeque<Long?>()
	val cookieManager: CookieManager = CookieManager(context)
	val cookieJar: CookieStore = CookieStore(cookieManager)
	val client: OkHttpClient = OkHttpClient.Builder()
		.connectTimeout(TIMEOUT, TimeUnit.SECONDS)
		.readTimeout(TIMEOUT, TimeUnit.SECONDS)
		.writeTimeout(TIMEOUT, TimeUnit.SECONDS)
		.followRedirects(true)
		.cookieJar(cookieJar)
		.build()
	val directClient: OkHttpClient = OkHttpClient.Builder()
		.followRedirects(false)
		.cookieJar(cookieJar)
		.build()
	private val casAuthorizationManager = AuthorizationManager("https://cas.sysu.edu.cn",
	                                                           "https://cas.sysu.edu.cn")
	var loginListener: LoginListener? = null
	var isLoginSuccess: Boolean = true
	private val publicKey: String
		get() {
			return try {
				client.newCall(Request.Builder()
					               .url("${casAuthorizationManager.host}/esc-sso/api/v3/auth/policy")
					               .build()).execute().body.string()
			} catch (_: IOException) {
				""
			}
		}
	
	private fun doLogin(username: String?, password: String?, publicKeyId: String?, captcha: String? = null): String {
		try {
			return client.newCall(Request.Builder()
				                      .post(("{\"authType\":\"webLocalAuth\",\"dataField\":{\"username\":\"$username\",\"password\":\"$password\",\"publicKeyId\":\"$publicKeyId\"${if (captcha.isNullOrEmpty()) "" else ",\"vcode\":\"$captcha\""}}}").toRequestBody(
					                      "application/json".toMediaTypeOrNull()))
				                      .url("https://cas.sysu.edu.cn/esc-sso/api/v3/auth/doLogin")
				                      .build()).execute().body.string()
		} catch (_: IOException) {
			onError("404", "登录失败")
		}
		return ""
	}
	
	private fun request(path: String, isRedirect: Boolean = false) {
		try {
			val response = client.newCall(Request.Builder()
				                              .url((if (isRedirect) "${casAuthorizationManager.host}/esc-sso/login?service=$path" else path))
				                              .build()).execute()
			val body = response.body.string()
			if (response.header("Content-Type", "")?.contains("application/json") == true) {
				val json = JSONObject.parse(body)
				when (val code = json.getString("code")) {
					"0" -> request(if (json.containsKey("data")) json.getJSONObject("data")
						.getString("redirect")
					               else json.getString("redirect"))
					"401" -> { //                    List<Cookie> list = getWebvpnKey(path);
						//                    if (!list.isEmpty())
						//                        cookieJar.saveFromResponse(HttpUrl.get("https://mportal.sysu.edu.cn"), list);
						//                    request(path, isRedirect);
					}
					else -> onError(code, body)
				}
			}
		} catch (_: IOException) {
		}
	}
	
	@Throws(IOException::class) private fun loginForGym(path: String): String {
		val url = if (path.startsWith("http")) path else casAuthorizationManager.host + path
		val response = client.newCall(Request.Builder()
			                              .header("Accept", "application/json, text/plain, */*")
			                              .header("User-Agent",
			                                      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36")
			                              .url(url)
			                              .build()).execute()
		val content = response.body.string()
		return if (response.header("Content-Type", "")?.contains("application/json") == true) {
			redirect(content)?.let { loginForGym(it) } ?: content
		}
		else content
	}
	
	/**
	 * 解析重定向 URL
	 * 
	 * @param response 响应 JSON 字符串
	 * @return 重定向 URL
	 */
	private fun redirect(response: String?): String? {
		val json = JSONObject.parse(response)
		return when {
			"0" == json.getString("code") -> if (json.containsKey("data")) json.getJSONObject("data")
				.getString("redirect")
			else json.getString("redirect")
			else -> {
				onError(json.getString("code"), response)
				null
			}
		}
	}
	
	@Throws(Exception::class) fun loginForKTP(username: String, password: String): Boolean {
		val password = AESCBCEncrypter.encryptByCBC(password,
		                                            "ktp4567890123456",
		                                            "ktp4567890123456")!!
		val data = "{\"email\":\"$username\",\"password\":\"$password\",\"remember\":\"1\",\"code\":\"\",\"mobile\":\"\",\"type\":\"login\",\"encryption\":1}"
		val response = client.newCall(Request.Builder()
			                              .post(data.toRequestBody("application/json".toMediaTypeOrNull()))
			                              .url("https://openapiv5.ketangpai.com//UserApi/login")
			                              .build()).execute()
		val type = response.header("Content-Type", "")
		if (type?.contains("application/json") == true) {
			val result = JSONObject.parse(response.body.string())
			val code = result.getString("code")
			val status = result.getInteger("status")
			return if ("1000" == code || status == 1) {
				authorizationJar.setToken("www.ketangpai.com",
				                          result.getJSONObject("data").getString("token"))
				true
			}
			else {
				onError(code, "登录失败：" + result.getString("msg"))
				false
			}
		}
		return false
	}
	
	fun loginForYuketang(imageView: ImageView?) {
		CompletableFuture.supplyAsync {
			QrCode(imageView).run()
			try {
				val response = JSONObject.parse(client.newCall(Request.Builder()
					                                               .post("".toRequestBody("application/json".toMediaTypeOrNull()))
					                                               .url("https://www.yuketang.cn/pc/web_login")
					                                               .build())
					                                .execute().body.string())
				if (response.containsKey("success") && response.getBoolean("success")) return@supplyAsync true
			} catch (e: IOException) {
				Log.e("LoginManager", e.message, e)
			}
			false
		}.thenAccept { b: Boolean ->
			if (b) onSuccess()
		}
	}
	
	/**
	 * 登录，使用指定的用户名和密码登录
	 * 
	 * @param username 用户名
	 * @param password 密码
	 * @param service  登录服务
	 */
	fun login(username: String, password: String, service: String, host: String?, captcha: String?) {
		val now = System.currentTimeMillis()
		if (!timestamps.isEmpty()) {
			val top = timestamps.getLast()
			if (top != null && now - top > 4000) timestamps.clear()
		}
		if (timestamps.size >= 5) {
			onError("503", context.getString(R.string.login_too_frequently))
			return
		}
		timestamps.add(now)
		CompletableFuture.supplyAsync {
			return@supplyAsync when (host) {
				TargetHost.SYSU -> loginSysu(username, password, service, captcha)
				TargetHost.KE_TANG_PIE -> loginForKTP(username, password)
				else -> false
			}
		}.thenAccept { b: Boolean? ->
			println("Login result: $b")
			if (b == true) onSuccess()
		}
	}
	
	//fun loginForSysu(username: String, password: String, service: String) {
	//	login(username, password, service, TargetHost.SYSU)
	//}
	
	fun loginForSysu(username: String, password: String, service: String, captcha: String? = null) {
		login(username, password, service, TargetHost.SYSU, captcha)
	}
	
	private fun loginSysu(username: String?,
	                      password: String,
	                      service: String,
	                      captcha: String?): Boolean {
		try {
			val host = service.toHttpUrl().host
			val targetBaseUrl = "${service.toHttpUrl().scheme}://$host/"
			cookieJar.add("https://cas.sysu.edu.cn",
			              Cookie.Builder()
				              .name("device_trust_Cookie")
				              .value("true")
				              .domain("cas.sysu.edu.cn")
				              .build())
			if (service.contains("webvpn")) {
				casAuthorizationManager.isAccessible = false
				val publicKey = JSONObject.parse(this.publicKey)
					.getJSONObject("data")
					.getJSONObject("param")
				val redirect = redirect(doLogin(username,
				                                encrypt(publicKey.getString("publicKey"), password),
				                                publicKey.getString("publicKeyId")))
				if (redirect == null) return false
				request("https://webvpn.sysu.edu.cn/users/auth/cas/callback?url", true)
				getWebvpnKey(service)
				when (service) {
					TargetUrl.NEWS_WEBVPN -> setAuthorization(host, getNewsAuthorization(service))
					TargetUrl.GYM_WEBVPN -> {
						getGymToken(targetBaseUrl)
						cookieJar.copy(targetBaseUrl, "https://gym.webvpn.sysu.edu.cn")
						setAuthorization(host, getGymAuthorization(targetBaseUrl))
					}
					TargetUrl.XGXT_WEBVPN -> {
						request(service, true)
						getXGXTToken(service, targetBaseUrl)
					}
					TargetUrl.XINFANG_WEBVPN -> {
					}
					else -> request(service, true)
				}
			}
			else {
				val publicKey = JSONObject.parse(this.publicKey)
					.getJSONObject("data")
					.getJSONObject("param")
				val redirect = redirect(doLogin(username,
				                                encrypt(publicKey.getString("publicKey"), password),
				                                publicKey.getString("publicKeyId"), captcha))
				if (redirect == null) return false
				request(service, true)
				when (service) {
					TargetUrl.PORTAL -> {
						loginForPortal()
						cookieJar.copy("https://portal.sysu.edu.cn", "https://mportal.sysu.edu.cn")
					}
					TargetUrl.GYM -> {
						getGymToken(targetBaseUrl) //                            cookieJar.copy(targetBaseUrl, "https://gym.webvpn.sysu.edu.cn");
						setAuthorization(host, getGymAuthorization(targetBaseUrl))
					}
					TargetUrl.PAY -> {
						val token = getPayToken(service)
						setToken(host, token)
						cookieJar.saveFromResponse(service.toHttpUrl(),
						                           listOf(Cookie.Builder()
							                                  .name("ibps-1.0.1-token")
							                                  .value(token)
							                                  .domain("pay.sysu.edu.cn")
							                                  .build()))
					}
					TargetUrl.ZHNY -> authorizationJar.setAuthorization(host,
					                                                    getZHNYAuthoritarian(service))
					TargetUrl.XGXT -> getXGXTToken(service, targetBaseUrl)
					TargetUrl.NEWS -> setAuthorization(host, getNewsAuthorization(service))
					TargetUrl.LMS -> setToken(host, this.lmsToken)
				}
			}
		} catch (e: Exception) {
			Log.e("LoginManager", e.message, e)
		}
		return isLoginSuccess
	}
	
	private fun getWebvpnKey(service: String): List<Cookie> {
		request("https://webvpn.sysu.edu.cn/vpn_key/update")
		val webvpnKey = cookieJar.loadForRequest("https://webvpn.sysu.edu.cn/vpn_key/update".toHttpUrl())
			.filter { e: Cookie -> "_webvpn_key" == e.name }
		if (!webvpnKey.isEmpty()) {
			cookieJar.saveFromResponse(service.toHttpUrl(), webvpnKey)
			cookieJar.saveFromResponse(casAuthorizationManager.host.toHttpUrl(), webvpnKey)
		}
		return webvpnKey
	}
	
	fun onError(code: String?, message: String?) {
		isLoginSuccess = false
		loginListener?.onError(code, message)
	}
	
	fun onSuccess() {
		isLoginSuccess = true
		loginListener?.onSuccess()
	}
	
	@Throws(IOException::class) private fun loginForPortal() {
		val location = directClient.newCall(Request.Builder()
			                                    .header("Accept",
			                                            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
			                                    .url("https://portal.sysu.edu.cn/newClient/auth?service=https%3A%2F%2Fportal.sysu.edu.cn%2FnewClient%2F%23%2FnewPortal%2Findex")
			                                    .build()).execute().headers["Location"]
		if (location?.startsWith("https://webvpn.sysu.edu.cn") == true) {
			val webvpnKey: List<Cookie> = getWebvpnKey(TargetUrl.PORTAL)
			if (!webvpnKey.isEmpty()) cookieJar.saveFromResponse("https://mportal.sysu.edu.cn".toHttpUrl(),
			                                                     webvpnKey.toList())
		}
		client.newCall(Request.Builder()
			               .header("Accept",
			                       "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
			               .url("https://portal.sysu.edu.cn/newClient/auth?service=https%3A%2F%2Fportal.sysu.edu.cn%2FnewClient%2F%23%2FnewPortal%2Findex")
			               .build()).execute()
	}
	
	private fun setToken(host: String?, token: String?) {
		authorizationJar.setToken(host, token)
	}
	
	/*
     * 设置认证
     * @param host 主机
     * @param auth 认证
     * */
	private fun setAuthorization(host: String?, auth: String?) {
		authorizationJar.setAuthorization(host, "Bearer $auth")
	}
	
	@Throws(IOException::class) private fun getXGXTToken(service: String?, targetBaseUrl: String?) {
		client.newCall(Request.Builder()
			               .url("${targetBaseUrl}sso/login?realm=sysuRealm&ticket=${
				               getTicket(service)
			               }&service=$service")
			               .post("".toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull()))
			               .build()).execute()
	}
	
	@Throws(IOException::class) private fun getPayToken(service: String?): String {
		return JSONObject.parse(client.newCall(Request.Builder()
			                                       .url("https://pay.sysu.edu.cn/client/api/client/auth/netId/login")
			                                       .header("Referer", "https://pay.sysu.edu.cn/")
			                                       .post(("{\"key\":\"https://cas.sysu.edu.cn/cas/serviceValidate?service=https://pay.sysu.edu.cn/sso&ticket=${
				                                       getTicket(service)
			                                       }\"}").toRequestBody("application/json".toMediaTypeOrNull()))
			                                       .build()).execute().body.string())
			.getString("data")
	}
	
	@get:Throws(IOException::class) private val lmsToken: String?
		get() {
			val response = client.newCall(Request.Builder()
				                              .url("https://lms.sysu.edu.cn/my/")
				                              .build()).execute().body.string()
			val matcher = Pattern.compile("\"sesskey\":\"(.+?)\"").matcher(response)
			if (matcher.find()) return matcher.group(1)
			else onError("403", "获取 LMS 会话密钥失败")
			return ""
		}
	
	@Throws(IOException::class) private fun getZHNYAuthoritarian(service: String?): String =
		JSONObject.parse(client.newCall(Request.Builder()
			                                .url("https://zhny.sysu.edu.cn/kbp/auth/third/h5/casLogin/${
				                                getTicket(service)
			                                }")
			                                .build()).execute().body.string()).getString("data")
	
	@Throws(Exception::class) private fun encrypt(publicKeyBase64: String?,
	                                              plainText: String): String? {
		val keyFactory = KeyFactory.getInstance("RSA")
		val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
		cipher.init(Cipher.ENCRYPT_MODE,
		            keyFactory.generatePublic(X509EncodedKeySpec(Base64.getDecoder()
			                                                         .decode(publicKeyBase64))))
		return Base64.getEncoder()
			.encodeToString(cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8)))
	}
	
	@Throws(IOException::class) fun getTicket(service: String?): String =
		directClient.newCall(Request.Builder()
			                     .url("${casAuthorizationManager.host}/esc-sso/login?service=$service")
			                     .build()).execute().headers["Location"]?.toHttpUrl()
			?.queryParameter("ticket") ?: ""
	
	@Throws(IOException::class) fun getGymToken(targetBaseUrl: String) {
		val re = Pattern.compile("prefix = '(.+?)'").matcher(loginForGym(targetBaseUrl))
		var prefix: String? = ""
		val filterChallenge: List<Cookie?> = cookieJar.loadForRequest(targetBaseUrl.toHttpUrl())
			.filter { e: Cookie? -> "safeline_bot_challenge" == e!!.name }
		if (re.find()) prefix = re.group(1)
		if (!filterChallenge.isEmpty() && !TextUtils.isEmpty(prefix)) cookieJar.saveFromResponse(
			targetBaseUrl.toHttpUrl(),
			listOf(Cookie.Builder()
				       .domain(targetBaseUrl.toHttpUrl().host)
				       .name("safeline_bot_challenge_ans")
				       .value(Answer.encode(prefix, filterChallenge[0]!!.value))
				       .build()))
	}
	
	fun getNewsAuthorization(url: String?): String? {
		return getAuthorization(Request.Builder()
			                        .url("${casAuthorizationManager.host}/esc-sso/login?service=$url")
			                        .build())
	}
	
	fun getGymAuthorization(targetBaseUrl: String?): String? {
		return getAuthorization(Request.Builder()
			                        .url("${targetBaseUrl}authsport/Account/Auth?response_type=token&client_id=sysu_2021&redirect_uri=https%3A%2F%2gym.sysu.edu.cn%2F%23&client_id=unnc&scope=PE")
			                        .header("Accept", "application/json, text/plain, */*")
			                        .header("User-Agent",
			                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36")
			                        .build())
	}
	
	fun getAuthorization(request: Request): String? {
		try {
			val response = client.newCall(request).execute()
			var location: String? = null
			if ((response.priorResponse?.header("location")
					.also { location = it })?.contains("access_token") == true) {
				val matcher = Pattern.compile("access_token=(.*?)&").matcher(location!!)
				if (matcher.find()) return matcher.group(1)
			}
		} catch (_: IOException) {
		}
		return ""
	}
	
	interface LoginListener {
		fun onSuccess()
		fun onError(code: String?, message: String?)
	}
	
	class CookieStore(val cookieManager: CookieManager) : AndroidCookieJar() {
		private val _cookieStore = mutableMapOf<String?, MutableList<Cookie>?>()
		override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
			val host = url.host
			val currentCookies = _cookieStore[host]
			val responseCookies = cookies.toMutableList()
			val keys: List<String> = responseCookies.map { it.name }
			if (currentCookies != null && !responseCookies.isEmpty() && !currentCookies.isEmpty()) currentCookies.filter { currentCookie: Cookie ->
				!responseCookies.contains(currentCookie) && (!currentCookie.value.isEmpty()) && (!keys.contains(
					currentCookie.name))
			}.forEach { e: Cookie -> responseCookies.add(e) }
			_cookieStore[host] = responseCookies
			cookieManager.set(host, responseCookies.map { "$it" }.toMutableSet())
			super.saveFromResponse(url, cookies)
		}
		
		override fun loadForRequest(url: HttpUrl): List<Cookie> {
			super.loadForRequest(url)
			return _cookieStore[url.host]?.filter { it.value.isNotEmpty() } ?: emptyList()
		}
		
		fun copy(from: String, to: String) {
			saveFromResponse(to.toHttpUrl(), loadForRequest(from.toHttpUrl()))
		}
		
		fun add(baseUrl: String, cookie: Cookie) {
			saveFromResponse(baseUrl.toHttpUrl(), listOf(cookie))
		}
	}
	
	internal object Answer {
		/**
		 * 计算字符串的SHA1哈希值，返回十六进制字符串
		 */
		@Throws(NoSuchAlgorithmException::class) fun hexSha1(input: String): String =
			bytesToHex(MessageDigest.getInstance("SHA-1").digest(input.toByteArray()))
		
		/**
		 * 将字节数组转换为十六进制字符串
		 */
		private fun bytesToHex(bytes: ByteArray): String {
			val hexString = StringBuilder()
			bytes.forEach { b ->
				val hex = Integer.toHexString(0xff and b.toInt())
				if (hex.length == 1) hexString.append('0')
				hexString.append(hex)
			}
			return "$hexString"
		}
		
		/**
		 * 将十六进制字符串转换为二进制字符串 每个十六进制字符转换为4位二进制
		 */
		fun hexToBinary(hexStr: String): String {
			val binaryStr = StringBuilder()
			hexStr.forEach { element ->
				binaryStr.append(String.format("%4s",
				                               Integer.toBinaryString(element.digitToIntOrNull(16)
					                                                      ?: -1)).replace(' ', '0'))
			}
			return "$binaryStr"
		}
		
		/**
		 * 模拟JS中的bin_sha1函数
		 */
		@Throws(NoSuchAlgorithmException::class) fun binSha1(input: String): String =
			hexToBinary(hexSha1(input))
		
		/**
		 * 找到满足条件的suffix
		 */
		@Throws(NoSuchAlgorithmException::class) fun findSuffix(prefix: String?,
		                                                        leadingZeroBit: Int): String {
			var cnt = 0
			while (true) {
				val suffix = Integer.toHexString(cnt)
				val hashBinary: String = binSha1(prefix + suffix)
				if (hashBinary.substring(0,
				                         leadingZeroBit) == "0".repeat(leadingZeroBit)) return suffix
				cnt++
			}
		}
		
		/**
		 * 计算最终的safeline_bot_challenge_ans cookie值
		 */
		@Throws(NoSuchAlgorithmException::class) fun getFinalCookie(safelineBotChallenge: String?,
		                                                            prefix: String?,
		                                                            leadingZeroBit: Int): String =
			safelineBotChallenge + findSuffix(prefix, leadingZeroBit)
		
		fun encode(prefix: String?, safelineBotChallenge: String?): String {
			try {
				return getFinalCookie(safelineBotChallenge, prefix, 9)
			} catch (e: NoSuchAlgorithmException) {
				System.err.println("SHA-1 算法不可用: " + e.message)
			} catch (e: Exception) {
				System.err.println("发生错误: " + e.message)
			}
			return ""
		}
	}
	
	internal object AESCBCEncrypter {
		/**
		 * AES-CBC 加密，PKCS7 填充，输出 Base64 字符串
		 * 
		 * @param plaintext 明文字符串
		 * @param key       密钥字符串（UTF-8 编码后长度必须为 16、24 或 32 字节）
		 * @param iv        初始向量字符串（UTF-8 编码后长度必须为 16 字节）
		 * @return Base64 编码的密文
		 * @throws Exception 加解密异常
		 */
		@Throws(Exception::class) fun encryptByCBC(plaintext: String,
		                                           key: String,
		                                           iv: String): String? {
			val keyBytes = key.toByteArray(StandardCharsets.UTF_8)
			val ivBytes = iv.toByteArray(StandardCharsets.UTF_8)
			require(mutableListOf<Int?>(16,
			                            24,
			                            32).contains(keyBytes.size)) { "密钥长度必须为 16、24 或 32 字节" }
			require(ivBytes.size == 16) { "IV 长度必须为 16 字节" }
			val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // PKCS5Padding 在 AES 下等价于 PKCS7
			cipher.init(Cipher.ENCRYPT_MODE,
			            SecretKeySpec(keyBytes, "AES"),
			            IvParameterSpec(ivBytes))
			return Base64.getEncoder()
				.encodeToString(cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8)))
		}
	}
	
	companion object {
		private const val TIMEOUT = 15L
	}
}