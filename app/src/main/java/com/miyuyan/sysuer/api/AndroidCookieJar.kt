package com.miyuyan.sysuer.api

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

open class AndroidCookieJar : CookieJar {
	private val webCookieManager = android.webkit.CookieManager.getInstance().apply {
		setAcceptCookie(true)
	}
	
	override fun loadForRequest(url: HttpUrl): List<Cookie> {
		val cookieString = webCookieManager.getCookie("$url")
		return if (cookieString.isNullOrEmpty()) emptyList()
		else cookieString.split(";").map { it.trim() }.mapNotNull { Cookie.parse(url, it) }
	}
	
	override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
		cookies.forEach { cookie ->
			webCookieManager.setCookie(url.host, "$cookie", null)
		}
		webCookieManager.flush()
	}
}