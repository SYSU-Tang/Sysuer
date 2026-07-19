package com.sysu.edu.api

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.graphics.Point
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Pair
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.sysu.edu.R
import com.sysu.edu.api.LoginManager.LoginListener
import com.sysu.edu.databinding.DialogAccountBinding
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlin.math.roundToInt

class ContextUtil(val context: Context) {
	private val sharedPreferences: SharedPreferences = context.getSharedPreferences("privacy",
	                                                                                Context.MODE_PRIVATE)
	private val loginManager: LoginManager = LoginManager(context)
	val accountManager: AccountManager = AccountManager.getInstance(context.applicationContext)
	private val handler = Handler(Looper.getMainLooper())
	val disposable: CompositeDisposable = CompositeDisposable()
	private val binding: DialogAccountBinding by lazy {
		DialogAccountBinding.inflate(LayoutInflater.from(context)).apply {
			password.editLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
		}
	}
	private var dialog: AlertDialog? = null
	
	init {
		if (userName.isNotEmpty() && password.isNotEmpty()) disposable.add(accountManager.setAccountAsync(
			TargetHost.SYSU,
			userName,
			password,
			true).subscribe { sharedPreferences.edit { remove("username").remove("password") } })
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
		context.getSystemService(ClipboardManager::class.java)
			.setPrimaryClip(ClipData.newPlainText(tag, text))
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
	fun loginForUrl(service: String?, host: String, afterLogin: Runnable?) {
		disposable.add(accountManager.getActiveAccountAsync(host)
			               .subscribe { activeAccount: Pair<String?, String?> ->
				               if (!activeAccount.first.isNullOrEmpty() && !activeAccount.second.isNullOrEmpty() && !service.isNullOrEmpty()) performLogin(
					               service,
					               host,
					               activeAccount,
					               afterLogin)
				               else changeAccount(service, host, afterLogin)
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
	
	private fun performLogin(service: String?,
	                         host: String,
	                         account: Pair<String?, String?>,
	                         afterLogin: Runnable?) {
		loginManager.loginListener = object : LoginListener {
			override fun onSuccess() {
				afterLogin?.run()
			}
			
			override fun onError(code: String?, message: String?) {
				if ("SSO10002" == code || "30506" == code) changeAccount(service, host, afterLogin)
				else handler.post { toast(message ?: "") }
			}
		}
		loginManager.loginForSysu(account.first ?: "", account.second ?: "", service ?: "")
	}
	
	fun login(url: String?, afterLogin: Runnable?) {
		loginForUrl(url, TargetHost.SYSU, afterLogin)
	}
	
	fun changeAccount(url: String?, host: String, afterLogin: Runnable?) {
		if (context is Activity && !context.isFinishing && !context.isDestroyed) {
			if (dialog == null) dialog = MaterialAlertDialogBuilder(context).setView(binding.root)
				.setTitle(R.string.privacy)
				.setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
					val username = binding.username.edit.getText().toString()
					val password = binding.password.edit.getText().toString()
					if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) toast(R.string.username_password_warning)
					else disposable.add(accountManager.setAccountAsync(host,
					                                                   username,
					                                                   password,
					                                                   true).subscribe {
						performLogin(url, host, Pair(username, password), afterLogin)
					})
				}
				.setNegativeButton(R.string.cancel, null)
				.create()
			disposable.add(accountManager.getActiveAccountAsync(host)
				               .subscribe { account: Pair<String?, String?>? ->
					               context.runOnUiThread {
						               if (!account?.first.isNullOrEmpty() && !account.second.isNullOrEmpty()) {
							               binding.password.edit.setText(account.second)
							               binding.username.edit.setText(account.first)
						               }
						               dialog!!.show()
					               }
				               })
		}
	}
	
	fun dispose() {
		disposable.dispose()
	}
	
	val width: Int?
		/**
		 * 获取屏幕宽度
		 *
		 * @return 屏幕宽度（px）
		 */
		get() = if (SDK_INT >= Build.VERSION_CODES.R) ContextCompat.getSystemService(context,
		                                                                             WindowManager::class.java)?.currentWindowMetrics?.bounds?.width()
		else run {
			val realSize = Point()
			context.getSystemService(WindowManager::class.java)?.defaultDisplay?.getRealSize(
				realSize)
			realSize.x
		}
	val column: Int
		/**
		 * 获取列数，根据屏幕宽度动态调整，手机屏幕为一列，以此类推
		 *
		 * @return 列数（1、2 或 3）
		 */
		get() = width?.let {
			if (it < dpToPx(540)) 1 else if (it < dpToPx(900)) 2 else 3
		} ?: 1
}
