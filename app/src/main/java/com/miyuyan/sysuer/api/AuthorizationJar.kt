package com.miyuyan.sysuer.api

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class AuthorizationJar(val context: Context) {
	private val authPreferences: SharedPreferences =
		context.getSharedPreferences("authorization", Context.MODE_PRIVATE)
	private val tokenPreferences: SharedPreferences =
		context.getSharedPreferences("token", Context.MODE_PRIVATE)

	/**
	 * 获取Authorization
	 * @param host 主机
	 * @return Authorization
	 * */
	fun getAuthorization(host: String?): String = authPreferences.getString(host, "") ?: ""

	/**
	 * 设置Authorization
	 * @param host 主机
	 * @param authorization Authorization
	 * */
	fun setAuthorization(host: String?, authorization: String?) {
		authPreferences.edit { putString(host, authorization) }
	}

	/**
	 * 获取Token
	 * @param host 主机
	 * @return Token
	 * */
	fun getToken(host: String?): String = tokenPreferences.getString(host, "") ?: ""

	/**
	 * 设置Token
	 * @param host 主机
	 * @param token Token
	 * */
	fun setToken(host: String?, token: String?) {
		tokenPreferences.edit { putString(host, token) }
	}
}
