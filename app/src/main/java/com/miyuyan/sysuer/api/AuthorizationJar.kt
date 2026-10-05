package com.miyuyan.sysuer.api

import android.content.Context
import com.miyuyan.sysuer.preference.AuthorizationPreference

class AuthorizationJar(val context: Context) {
	private val authorizationPreference = AuthorizationPreference(context)

	/**
	 * 获取Authorization
	 * @param host 主机
	 * @return Authorization
	 * */
	fun getAuthorization(host: String?): String = authorizationPreference.getAuthorization(host)

	/**
	 * 设置Authorization
	 * @param host 主机
	 * @param authorization Authorization
	 * */
	fun setAuthorization(host: String?, authorization: String?) {
		authorizationPreference.setAuthorization(host, authorization)
	}

	/**
	 * 获取Token
	 * @param host 主机
	 * @return Token
	 * */
	fun getToken(host: String?): String = authorizationPreference.getToken(host)

	/**
	 * 设置Token
	 * @param host 主机
	 * @param token Token
	 * */
	fun setToken(host: String?, token: String?) {
		authorizationPreference.setToken(host, token)
	}
}
