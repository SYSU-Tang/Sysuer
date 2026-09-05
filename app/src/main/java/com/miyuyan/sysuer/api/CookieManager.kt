package com.miyuyan.sysuer.api

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import okhttp3.Cookie
import java.net.HttpCookie
import java.util.stream.Collectors

class CookieManager(context: Context) {
	private val cookiePreference: SharedPreferences = context.getSharedPreferences("cookie", Context.MODE_PRIVATE)
	fun get(host: String?): MutableSet<String?> = cookiePreference.getStringSet(host, HashSet<String?>())!!
	fun toString(host: String?): String {
		val strings = get(host)
		return if (strings.isEmpty()) "" else strings.joinToString(separator = ";")
	}
	
	fun toSimpleString(host: String?): String = get(host).stream().map { c: String? -> c!!.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0] }.collect(Collectors.joining(";"))
	fun set(host: String?, cookieSet: MutableSet<String?>) {
		cookiePreference.edit { putStringSet(host, cookieSet.stream().filter { c: String? -> !c!!.startsWith("rememberMe=") }.collect(Collectors.toSet())) }
	}
	
	fun clear(host: String?) {
		cookiePreference.edit { remove(host) }
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
		val parts: Array<String?> = cookie.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		if ("rememberMe" != parts[0]) add(host, Cookie.Builder().domain(host).name(parts[0]!!).value(parts[1]!!).build())
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
		@Volatile private var INSTANCE: CookieManager? = null
		fun getInstance(context: Context): CookieManager = INSTANCE ?: synchronized(CookieManager::class.java) {
			INSTANCE ?: CookieManager(context.applicationContext).also { INSTANCE = it }
		}
	}
}
