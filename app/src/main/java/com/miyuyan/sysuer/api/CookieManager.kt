package com.miyuyan.sysuer.api

import android.content.Context
import com.miyuyan.sysuer.preference.CookiePreference
import okhttp3.Cookie
import java.net.HttpCookie
import java.util.stream.Collectors

class CookieManager(context: Context) {
	private val cookiePreference = CookiePreference(context)

	fun get(host: String?): MutableSet<String> = cookiePreference.get(host)

	fun get(host: String?, cookieName: String?) =
		cookiePreference.get(host).firstOrNull { it.startsWith("$cookieName=") }?.let {
			HttpCookie.parse(it)[0].value
		}

	fun toString(host: String?): String = get(host).joinToString(separator = ";")

	fun toSimpleString(host: String?): String = get(host).stream().map { c: String ->
		c.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
	}.collect(Collectors.joining(";"))

	fun set(host: String?, cookieSet: Set<String?>) {
		cookiePreference.set(
			host,
			cookieSet.filterNotNull().filter { !it.startsWith("rememberMe=") }.toSet()
		)
	}

	fun clear(host: String?) {
		cookiePreference.remove(host)
	}

	fun add(host: String?, cookie: Cookie) {
		if ("rememberMe" != cookie.name) {
			get(host).let {
				it.forEach { o ->
					val c = HttpCookie.parse(o)[0]
					if (c.name == cookie.name) it.remove(o)
				}
				it.add("$cookie")
				set(host, it)
			}
		}
	}

	fun add(host: String, cookie: String) {
		val parts: Array<String?> = cookie.split("=").dropLastWhile { it.isEmpty() }.toTypedArray()
		if ("rememberMe" != parts[0]) add(
				host, Cookie.Builder().domain(host).name(parts[0]!!).value(parts[1]!!).build()
		)
	}

	fun remove(host: String?, cookieName: String?) {
		val cookieSet = get(host)
		for (o in cookieSet) {
			val c = HttpCookie.parse(o)[0]
			if (c.name == cookieName) cookieSet.remove(o)
		}
		set(host, cookieSet)
	}

	companion object {
		@Volatile
		private var INSTANCE: CookieManager? = null
		fun getInstance(context: Context): CookieManager =
			INSTANCE ?: synchronized(CookieManager::class.java) {
				INSTANCE ?: CookieManager(context.applicationContext).also { INSTANCE = it }
			}
	}
}
