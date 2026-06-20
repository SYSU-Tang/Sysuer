package com.sysu.edu.api

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class AuthorizationJar(val context: Context) {
	private val authPreferences: SharedPreferences = context.getSharedPreferences("authorization", Context.MODE_PRIVATE)
	private val tokenPreferences: SharedPreferences = context.getSharedPreferences("token", Context.MODE_PRIVATE)
	private val cookieManager: CookieManager = CookieManager(context)
	fun getAuthorization(host: String?): String {
		return authPreferences.getString(host, "") ?: ""
	}
	
	fun setAuthorization(host: String?, authorization: String?) {
		authPreferences.edit { putString(host, authorization) }
	}
	
	fun getToken(host: String?): String {
		return tokenPreferences.getString(host, "") ?: ""
	}
	
	fun setToken(host: String?, token: String?) {
		tokenPreferences.edit { putString(host, token) }
	}
	
	fun getCookie(host: String?): String {
		return cookieManager.toSimpleString(host)
	}
}
