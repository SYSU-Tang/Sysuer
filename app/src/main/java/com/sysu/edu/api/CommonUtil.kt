package com.sysu.edu.api

import android.content.Context
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * 通用工具类
 */
object CommonUtil {
	/**
	 * 从 JSONObject 中提取指定键的值
	 * 
	 * @param data JSONObject 数据
	 * @param keys 要提取的键数组
	 * @return 包含提取值的 ArrayList
	 */
	@JvmStatic fun extractValue(data: JSONObject, keys: Array<String?>): ArrayList<String?> {
		val values = ArrayList<String?>()
		for (i in keys) values.add(data.getString(i))
		return values
	}
	
	/**
	 * 从 JSONObject 中提取指定键的值
	 * 
	 * @param data JSONObject 数据
	 * @param keys 要提取的键列表
	 * @return 包含提取值的 ArrayList
	 */
	@JvmStatic fun extractValue(data: JSONObject, keys: MutableList<String?>): ArrayList<String?> {
		val values = ArrayList<String?>()
		for (i in keys) values.add(data.getString(i))
		return values
	}
	
	/**
	 * 将boolean值转换为字符串"1"或"0"
	 * 
	 * @param b 要转换的 boolean 值
	 * @return 转换后的字符串"1"或"0"
	 */
	@JvmStatic fun bool2str(b: Boolean): String {
		return if (b) "1" else "0"
	}
	
	/**
	 * 检查字符串是否为空或仅包含空格
	 * 
	 * @param str 要检查的字符串
	 * @return 如果字符串为空或仅包含空格，则返回true；否则返回false
	 */
	@JvmStatic fun isEmpty(str: String?): Boolean {
		return str == null || str.trim { it <= ' ' }.isEmpty()
	}
	
	/**
	 * 检查对象是否为空或仅包含空格
	 * 
	 * @param str 要检查的对象
	 * @return 如果对象为空或仅包含空格，则返回true；否则返回false
	 */
	@JvmStatic fun <T> isEmpty(str: T?): Boolean {
		return str == null || "$str".trim { it <= ' ' }.isEmpty()
	}
	
	/**
	 * 对字符串进行trim操作，若字符串为空则返回空字符串
	 * 
	 * @param str 要进行修剪操作的字符串
	 * @return trim后的字符串，若原字符串为空则返回空字符串
	 */
	@JvmStatic fun trim(str: String?): String {
		return str?.trim { it <= ' ' } ?: ""
	}
	
	/**
	 * 从资源 ID 列表中获取对应的字符串数组
	 * 
	 * @param context  上下文对象
	 * @param resource 资源 ID 数组
	 * @return 包含对应字符串的数组
	 */
	@JvmStatic fun getString(context: Context, resource: IntArray): List<String?> {
		return resource.map { resId: Int -> context.getString(resId) }.toList()
	}
	
	/**
	 * 从资源 ID 列表中获取对应的字符串数组
	 * 
	 * @param context  上下文对象
	 * @param resource 资源 ID 列表
	 * @return 包含对应字符串的数组
	 */
	@JvmStatic fun getString(context: Context, resource: MutableList<Int>): List<String?> {
		return resource.map { resId: Int -> context.getString(resId) }.toList()
	}
	
	/**
	 * 从 JSONArray 中提取指定键的值
	 * 
	 * @param array    JSONArray 数据
	 * @param nameKey  要提取的键名
	 * @param valueKey 要提取的值键名
	 * @return 包含提取值的 Tuple2 对象，其中第一个元素为名称数组，第二个元素为值数组
	 */
	@JvmStatic fun extractValue(array: JSONArray,
	                            nameKey: String?,
	                            valueKey: String?): Tuple2<ArrayList<String?>?, ArrayList<String?>?> {
		val names = ArrayList<String?>()
		val values = ArrayList<String?>()
		array.forEach { i: Any? ->
			names.add((i as JSONObject).getString(nameKey))
			values.add(i.getString(valueKey))
		}
		return Tuple2(names, values)
	}
	
	/**
	 * 从 JSONArray 中提取指定键的值
	 * 
	 * @param array   JSONArray
	 * @param nameKey 要提取的键名
	 * @return 包含提取值的 ArrayList
	 */
	@JvmStatic fun extractValue(array: JSONArray, nameKey: String?): ArrayList<String?> {
		val names = ArrayList<String?>()
		array.forEach { i: Any? -> names.add((i as JSONObject).getString(nameKey)) }
		return names
	}
	
	/**
	 * 将boolean值转换为整数1或0
	 * 
	 * @param bool 要转换的 boolean 值
	 * @return 转换后的整数1或0
	 */
	@JvmStatic fun bool2int(bool: Boolean): Int {
		return if (bool) 1 else 0
	}
	
	/**
	 * 将对象转换为字符串，若对象为空则返回空字符串
	 * 
	 * @param t 要转换的对象
	 * @return 转换后的字符串，若对象为空则返回空字符串
	 */
	@JvmStatic fun <T> toStringOrDefault(t: T?): String {
		return toStringOrDefault<T?>(t, "")
	}
	
	/**
	 * 将对象转换为字符串，若对象为空则返回默认值
	 * 
	 * @param t            要转换的对象
	 * @param defaultValue 默认值，若对象为空则返回该值
	 * @return 转换后的字符串，若对象为空则返回默认值
	 */
	@JvmStatic fun <T> toStringOrDefault(t: T?, defaultValue: String): String {
		return t?.toString() ?: defaultValue
	}
	
	@JvmStatic fun toIntegerOrDefault(t: Int?, defaultValue: Int?): Int? {
		return t ?: defaultValue
	}
	
	@JvmStatic fun toIntegerOrDefault(t: String?, defaultValue: Int): Int {
		return if (CommonUtil.isEmpty(t)) defaultValue else t!!.toInt()
	}
	
	/**
	 * 从 URL 中提取主机名
	 * 
	 * @param url 要提取主机名的 URL
	 * @return 提取的主机名
	 */
	@JvmStatic fun getHost(url: String): String {
		return url.toHttpUrl().host
	}
	
	/**
	 * 简单的元组类，用于存储两个值
	 * 
	 * @param <T>  第一个值的类型
	 * @param <T1> 第二个值的类型
	 */
	data class Tuple2<T, T1>(@JvmField var first: T, @JvmField var second: T1) {
		fun set(f: T, s: T1) {
			first = f
			second = s
		}
		
		//		fun setFirst(first: T?) {
		//			this.first = first
		//		}
		//
		//		fun setSecond(second: T1?) {
		//			this.second = second
		//		}
		//
		fun getFirst(): T? {
			return first
		}
		
		fun getSecond(): T1? {
			return second
		} //		override fun toString(): String {
		//			return "($first, $second)"
		//		}
		//
		//		override fun equals(other: Any?): Boolean {
		//			return other is Tuple2<*, *> && first == other.first && second == other.second
		//		}
		//
		//		override fun hashCode(): Int {
		//			return Objects.hash(first, second)
		//		}
	}
}
