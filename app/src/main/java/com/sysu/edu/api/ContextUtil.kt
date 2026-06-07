package com.sysu.edu.api

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Pair
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.sysu.edu.R
import com.sysu.edu.api.LoginManager.LoginListener
import com.sysu.edu.databinding.DialogAccountBinding
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.functions.Consumer
import kotlin.math.roundToInt

class ContextUtil(val context: Context) {
	private val sharedPreferences: SharedPreferences =
		context.getSharedPreferences("privacy", Context.MODE_PRIVATE)
	private val loginManager: LoginManager = LoginManager(context)
	val accountManager: AccountManager = AccountManager.getInstance(context.applicationContext)
	private val handler = Handler(Looper.getMainLooper())
	private val disposable = CompositeDisposable()
	private var binding: DialogAccountBinding? = null
	private var dialog: AlertDialog? = null
	
	init {
		if (!TextUtils.isEmpty(this.userName) && !TextUtils.isEmpty(this.password)) disposable.add(accountManager.setAccountAsync(TargetHost.SYSU, this.userName, this.password, true).subscribe { sharedPreferences.edit { remove("username").remove("password") } })
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
	fun dpToPx(dps: Int): Int {
		return (context.resources.displayMetrics.density * dps).roundToInt()
	}
	
	val userName: String
		/**
		 * 获取用户名
		 * 
		 * @return 用户名
		 */
		get() = sharedPreferences.getString("username", "")!!
	val password: String
		/**
		 * 获取密码
		 * 
		 * @return 密码
		 */
		get() = sharedPreferences.getString("password", "")!!
	var isDeveloper: Boolean
		/**
		 * 获取是否为开发者
		 * 
		 * @return 是否为开发者
		 */
		get() = sharedPreferences.getBoolean("developer", false)
		/**
		 * 设置是否为开发者
		 * 
		 */
		set(developer) {
			sharedPreferences.edit { putBoolean("developer", developer) }
		}
	
	/**
	 * 复制文本到剪贴板
	 * 
	 * @param tag  剪贴板标签
	 * @param text 要复制的文本
	 */
	fun copy(tag: String?, text: String?) {
		val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
		clip.setPrimaryClip(ClipData.newPlainText(tag, text))
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
	fun loginForUrl(service: String?, host: String?, afterLogin: Runnable?) {
		disposable.add(accountManager.getActiveAccountAsync(host).subscribe { activeAccount: Pair<String?, String?> ->
			if (!TextUtils.isEmpty(activeAccount.first) && !TextUtils.isEmpty(activeAccount.second) && !TextUtils.isEmpty(service)) {
				loginManager.setOnLoginListener(object : LoginListener {
					override fun onSuccess() {
						afterLogin?.run()
					}
					
					override fun onError(code: String?, message: String?) {
						if ("SSO10002" == code || "30506" == code) changeAccount(service, host, afterLogin)
						else handler.post { toast(CommonUtil.toStringOrDefault<String?>(message)) }
					}
				})
				loginManager.login(activeAccount.first, activeAccount.second, service)
			} else changeAccount(service, host, afterLogin)
		})
	}
	
	fun login(url: String?, afterLogin: Runnable?) {
		loginForUrl(url, TargetHost.SYSU, afterLogin)
	}
	
	fun changeAccount(url: String?, host: String?, afterLogin: Runnable?) {
		if (context is Activity && !context.isFinishing && !context.isDestroyed) {
			if (binding == null) {
				binding = DialogAccountBinding.inflate(LayoutInflater.from(context))
				binding!!.password.editLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
			}
			if (dialog == null) dialog = MaterialAlertDialogBuilder(context)
				.setView(binding!!.root)
				.setTitle(R.string.privacy)
				.setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
					val username = binding!!.username.edit.getText()
					val password = binding!!.password.edit.getText()
					if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) toast(R.string.username_password_warning)
					else disposable.add(accountManager.setAccountAsync(host, username.toString(), password.toString(), true)
											.subscribe { loginForUrl(url, host, afterLogin) })
				}
				.setNegativeButton(android.R.string.cancel, null)
				.create()
			disposable.add(accountManager.getActiveAccountAsync(host).subscribe(Consumer { account: Pair<String?, String?>? ->
				context.runOnUiThread {
					if (!TextUtils.isEmpty(account!!.first) && !TextUtils.isEmpty(account.second)) {
						binding!!.password.edit.setText(account.second)
						binding!!.username.edit.setText(account.first)
					}
					dialog!!.show()
				}
			}))
		}
	}
	
	fun dispose() {
		disposable.dispose()
	}
}
