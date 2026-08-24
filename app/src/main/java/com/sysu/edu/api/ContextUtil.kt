package com.sysu.edu.api

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.util.component1
import androidx.core.util.component2
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.sysu.edu.Application
import com.sysu.edu.R
import com.sysu.edu.api.LoginManager.LoginListener
import com.sysu.edu.databinding.DialogAccountBinding
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.io.IOException
import java.util.Base64
import java.util.UUID
import java.util.concurrent.CompletableFuture
import kotlin.concurrent.Volatile
import kotlin.math.roundToInt

class ContextUtil(val context: Context) {
	fun getAvailableActivity(): FragmentActivity? = if (context is FragmentActivity && !context.isFinishing && !context.isDestroyed) context
	else (context.applicationContext as? Application)?.currentActivity?.let {
		if (!it.isFinishing && !it.isDestroyed) it else null
	}
	
	private val sharedPreferences: SharedPreferences = context.getSharedPreferences("privacy", Context.MODE_PRIVATE)
	private val loginManager: LoginManager = LoginManager(context)
	val accountManager: AccountManager = AccountManager.getInstance(context.applicationContext)
	private val handler = Handler(Looper.getMainLooper())
	val disposable: CompositeDisposable = CompositeDisposable()
	private var binding: DialogAccountBinding? = null
	private var dialog: AlertDialog? = null
	
	init {
		if (userName.isNotEmpty() && password.isNotEmpty()) disposable.add(accountManager.setAccountAsync(TargetHost.SYSU, userName, password, true).subscribe { sharedPreferences.edit { remove("username").remove("password") } })
	}
	
	fun getColorFromAttr(attr: Int): Int {
		val typedValue = TypedValue()
		context.theme.resolveAttribute(attr, typedValue, true)
		return typedValue.data
	}
	
	/**
	 * 将 dp 值转换为 px 值
	 * 
	 * @param dps dp 值
	 * @return 对应的 px 值
	 */
	fun dpToPx(dps: Int): Int = (context.resources.displayMetrics.density * dps).roundToInt()
	val userName: String
		/**
		 * 获取用户名
		 * 
		 * @return 用户名
		 */
		get() = sharedPreferences.getString("username", "") ?: ""
	val password: String
		/**
		 * 获取密码
		 * 
		 * @return 密码
		 */
		get() = sharedPreferences.getString("password", "") ?: ""
	
	/**
	 * 复制文本到剪贴板
	 * 
	 * @param tag  剪贴板标签
	 * @param text 要复制的文本
	 */
	fun copy(tag: String?, text: String?) {
		context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(tag, text))
	}
	
	/**
	 * 显示 Toast 消息
	 * 
	 * @param resource 字符串资源 ID
	 */
	fun toast(resource: Int) {
		Toast.makeText(context, resource, Toast.LENGTH_LONG).show()
	}
	
	/**
	 * 显示 Toast 消息
	 * 
	 * @param toast 要显示的文本
	 */
	fun toast(toast: String?) {
		Toast.makeText(context, toast, Toast.LENGTH_LONG).show()
	}
	
	/**
	 * 登录
	 * 
	 * @param service    登录 URL,建议使用 TargeterURL 中的默认登录 URL
	 * @param afterLogin 登录成功后的回调 Runnable 对象
	 */
	fun loginForUrl(service: String?, host: String, captcha: String?, afterLogin: Runnable?) {
		disposable.add(accountManager.getActiveAccountAsync(host).subscribe { (username, password) ->
			if (!username.isNullOrEmpty() && !password.isNullOrEmpty() && !service.isNullOrEmpty()) performLogin(service, host, username, password, captcha, afterLogin)
			else changeAccount(service, host, captcha, afterLogin)
		})
	}
	
	fun loginByQrCode(host: String, imageView: ImageView, afterLogin: Runnable?) {
		when (host) {
			TargetHost.YU_KE_TANG -> loginManager.loginForYuketang(imageView)
		}
		loginManager.loginListener = object : LoginListener {
			override fun onSuccess() {
				afterLogin?.run()
			}
			
			override fun onError(code: String?, message: String?) {
				handler.post { toast(message ?: "") }
			}
		}
	}
	
	private fun performLogin(
		service: String?,
		host: String,
		username: String,
		password: String,
		captcha: String?,
		afterLogin: Runnable?,
	                        ) {
		loginManager.loginListener = object : LoginListener {
			override fun onSuccess() {
				afterLogin?.run()
			}
			
			override fun onError(code: String?, message: String?) {
				println("Login error: $code, $message")
				when (code) {
					"SSO10002", "30506" -> {
						changeAccount(service, host, null, afterLogin)
						handler.post { toast(message) }
					}
					"SSO10093" -> {
						changeAccount(service, host, captcha ?: "", afterLogin)
					}
					"SSO10023" -> {
						changeAccount(service, host, "", afterLogin)
						handler.post { toast(message) }
					}
					else -> handler.post { toast(message ?: "") }
				}
			}
		}
		loginManager.loginForSysu(username, password, service ?: "", captcha)
	}
	
	fun login(service: String?, afterLogin: Runnable?) {
		loginForUrl(service, TargetHost.SYSU, null, afterLogin)
	}
	
	fun changeAccount(
		service: String?,
		host: String,
		captcha: String? = null,
		afterLogin: Runnable? = null,
	                 ) {
		val act = getAvailableActivity()
		if (act == null) {
			handler.post { changeAccount(service, host, captcha, afterLogin) }
			return
		}
		if (binding == null) binding = DialogAccountBinding.inflate(LayoutInflater.from(act)).apply {
			password.editLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
		}
		act.runOnUiThread {
			if (captcha != null) {
				binding!!.captchaGroup.isVisible = true
				binding!!.captchaText.editText?.setText(captcha)
				loginManager.cookieJar.saveFromResponse("https://cas.sysu.edu.cn/esc-sso/api/v1/image/getRandcode".toHttpUrl(),
				                                        listOf(Cookie.Builder().name("SESSION").value(Base64.getEncoder().encodeToString(UUID.randomUUID().toString().toByteArray())).domain("cas.sysu.edu.cn").build()))
				fun loadCaptcha() {
					CompletableFuture.supplyAsync {
						try {
							loginManager.client.newCall(Request.Builder().url("https://cas.sysu.edu.cn/esc-sso/api/v1/image/getRandcode").build()).execute().use { response ->
								if (response.isSuccessful) response.body.bytes()
								else null
							}
						} catch (_: IOException) {
							null
						}
					}.thenAccept { bytes ->
						if (bytes != null) handler.post {
							Glide.with(act).load(bytes).override(dpToPx(160), dpToPx(40)).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).into(binding!!.captchaImage)
						}
					}
				}
				binding!!.captchaImage.setOnClickListener {
					loadCaptcha()
				}
				loadCaptcha()
			}
			if (dialog == null) dialog = MaterialAlertDialogBuilder(act).setView(binding!!.root).setTitle(R.string.privacy).setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
				val username = binding!!.username.edit.text.toString()
				val password = binding!!.password.edit.text.toString()
				val captcha = binding!!.captchaText.editText?.text.toString()
				if (username.isEmpty() || password.isEmpty()) toast(R.string.username_password_warning)
				else disposable.add(accountManager.setAccountAsync(host, username, password, true).subscribe {
					performLogin(service, host, username, password, captcha, afterLogin)
				})
			}.setNegativeButton(R.string.cancel, null).create()
		}
		disposable.add(accountManager.getActiveAccountAsync(host).observeOn(AndroidSchedulers.mainThread()).subscribe({ (username, password) ->
			                                                                                                              if (!username.isNullOrEmpty() && !password.isNullOrEmpty()) {
				                                                                                                              binding?.password?.edit?.setText(password)
				                                                                                                              binding?.username?.edit?.setText(username)
			                                                                                                              }
			                                                                                                              dialog?.show()
		                                                                                                              }, {}))
	}
	
	fun dispose() {
		disposable.dispose()
	}
	
	val width: Int?
		get() = if (SDK_INT >= Build.VERSION_CODES.R) ContextCompat.getSystemService(context, WindowManager::class.java)?.currentWindowMetrics?.bounds?.width()
		else context.resources.displayMetrics.widthPixels
	val column: Int
		/**
		 * 获取列数，根据屏幕宽度动态调整，手机屏幕为一列，以此类推
		 *
		 * @return 列数（1、2 或 3）
		 */
		get() = width?.let {
			if (it < dpToPx(540)) 1 else if (it < dpToPx(900)) 2 else 3
		} ?: 1
	
	companion object {
		@SuppressLint("StaticFieldLeak") @Volatile private var INSTANCE: ContextUtil? = null
		fun getInstance(context: Context): ContextUtil = INSTANCE ?: synchronized(ContextUtil::class.java) {
			INSTANCE ?: ContextUtil(context.applicationContext).also { INSTANCE = it }
		}
	}
}