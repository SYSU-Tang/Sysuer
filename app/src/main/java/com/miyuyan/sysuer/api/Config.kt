package com.miyuyan.sysuer.api

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.miyuyan.sysuer.browser.BrowserActivity

class Config {
	var activity: FragmentActivity? = null // 关联的 FragmentActivity 对象
	var fragment: Fragment? = null // 关联的 Fragment 对象
	var afterLogin: Runnable? = null // 登录成功后的回调 Runnable 对象
	var contextUtil: ContextUtil
	
	/**
	 * 构造函数，用于初始化 Params 对象
	 * 
	 * @param activity 关联的 FragmentActivity 对象
	 */
	constructor(activity: FragmentActivity) {
		this.activity = activity
		contextUtil = ContextUtil(activity)
	}
	
	/**
	 * 构造函数，用于初始化 Params 对象
	 * 
	 * @param fragment 关联的 Fragment 对象
	 */
	constructor(fragment: Fragment) {
		this.fragment = fragment
		activity = fragment.requireActivity()
		contextUtil = ContextUtil(fragment.requireContext())
	}
	
	/**
	 * 设置登录回调
	 * 
	 * @param afterLogin 登录成功后的回调 Runnable 对象
	 */
	fun setCallback(afterLogin: Runnable?) {
		this.afterLogin = afterLogin
	}
	
	/**
	 * 将 dp 值转换为 px 值
	 * 
	 * @param dps dp 值
	 * @return 对应的 px 值
	 */
	fun dpToPx(dps: Int): Int = contextUtil.dpToPx(dps)
	val width: Int?
		/**
		 * 获取屏幕宽度
		 * 
		 * @return 屏幕宽度（px）
		 */
		get() = contextUtil.width
	val column: Int
		/**
		 * 获取列数，根据屏幕宽度动态调整，手机屏幕为一列，以此类推
		 * 
		 * @return 列数（1、2 或 3）
		 */
		get() = contextUtil.column
	val context: Context
		get() = contextUtil.context
	
	/**
	 * 打开浏览器
	 * 
	 * @param url 要打开的 URL
	 * @return 点击事件监听器
	 */
	fun browse(url: String?): View.OnClickListener =
		View.OnClickListener { context.startActivity(Intent(context, BrowserActivity::class.java).setData(Uri.parse(url))) }
	
	/**
	 * 复制文本到剪贴板
	 * 
	 * @param tag  剪贴板标签
	 * @param text 要复制的文本
	 */
	fun copy(tag: String?, text: String?) {
		contextUtil.copy(tag, text)
	}
	
	/**
	 * 显示 Toast 消息
	 * 
	 * @param resource 字符串资源 ID
	 */
	fun toast(resource: Int) {
		contextUtil.toast(resource)
	}
	
	/**
	 * 显示 Toast 消息
	 * 
	 * @param toast 要显示的文本
	 */
	fun toast(toast: String?) {
		contextUtil.toast(toast)
	}
	
	/**
	 * 跳转登录页面
	 * 
	 * @param url 登录 URL，建议使用 TargeterURL 中的默认登录 URL
	 */
	fun gotoLogin(url: String?) {
		contextUtil.login(url, afterLogin)
	}
}
