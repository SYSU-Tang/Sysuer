package com.haibin.calendarview

import android.content.Context

/**
 * 干支纪年算法
 * Created by huanghaibin on 2019/2/12.
 */
@Suppress("unused") object TrunkBranchAnnals {
	/**
	 * 天干字符串
	 */
	private var TRUNK_STR: Array<String?>? = null
	
	/**
	 * 地支字符串
	 */
	private var BRANCH_STR: Array<String?>? = null
	
	/**
	 * 单独使用请先调用这个方法
	 * @param context context
	 */
	@JvmStatic fun init(context: Context) {
		if (TRUNK_STR == null) {
			TRUNK_STR = context.resources.getStringArray(R.array.trunk_string_array)
			BRANCH_STR = context.resources.getStringArray(R.array.branch_string_array)
		}
	}
	
	/**
	 * 获取某一年对应天干文字
	 * 
	 * @param year 年份
	 * @return 天干由甲到癸，每10一轮回
	 */
	fun getTrunkString(year: Int): String? {
		return TRUNK_STR!![getTrunkInt(year)]
	}
	
	/**
	 * 获取某一年对应天干，
	 * 
	 * @param year 年份
	 * @return 4 5 6 7 8 9 10 1 2 3
	 */
	fun getTrunkInt(year: Int): Int {
		val trunk = year % 10
		return if (trunk == 0) 9 else trunk - 1
	}
	
	/**
	 * 获取某一年对应地支文字
	 * 
	 * @param year 年份
	 * @return 地支由子到亥，每12一轮回
	 */
	fun getBranchString(year: Int): String? {
		return BRANCH_STR!![getBranchInt(year)]
	}
	
	/**
	 * 获取某一年对应地支
	 * 
	 * @param year 年份
	 * @return 4 5 6 7 8 9 10 11 12 1 2 3
	 */
	fun getBranchInt(year: Int): Int {
		val branch = year % 12
		return if (branch == 0) 11 else branch - 1
	}
	
	/**
	 * 获取干支纪年
	 * 
	 * @param year 年份
	 * @return 干支纪年
	 */
	fun getTrunkBranchYear(year: Int): String {
		return "${getTrunkString(year)}${getBranchString(year)}"
	}
}
