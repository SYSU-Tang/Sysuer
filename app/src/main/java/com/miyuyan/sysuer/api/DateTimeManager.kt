package com.miyuyan.sysuer.api

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

object DateTimeManager {
	/**
	 * 获取当前日期
	 *
	 * @return 当前日期
	 */
	val today: LocalDate get() = LocalDate.now()
	val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
	val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

	/**
	 * 获取当前日期的年份
	 *
	 * @return 当前日期的年份
	 */
	val year: Int
		get() = today.year

	/**
	 * 将时间戳转换为日期字符串（格式：yyyy-MM-dd）
	 *
	 * @param millis 时间戳（毫秒）
	 * @return 日期字符串（格式：yyyy-MM-dd）
	 */
	fun toDateString(millis: Long): String? = dateFormatter.format(toDate(millis))

	/**
	 * 将日期转换为日期字符串（格式：yyyy-MM-dd）
	 *
	 * @param date 日期
	 * @return 日期字符串（格式：yyyy-MM-dd）
	 */
	fun toDateString(date: LocalDate?): String? = dateFormatter.format(date)

	/**
	 * 计算未来或过去指定天数后的日期字符串（格式：yyyy-MM-dd）
	 *
	 * @param days 天数
	 * @param days 天数为正数时，返回未来日期；为负数时，返回过去日期
	 * @return 日期字符串（格式：yyyy-MM-dd）
	 */
	fun toDateStringPLus(days: Int): String? = toDateString(today.plusDays(days.toLong()))

	/**
	 * 将日期时间转换为日期时间字符串（格式：yyyy-MM-dd HH:mm:ss）
	 *
	 * @param date 日期时间
	 * @return 日期时间字符串（格式：yyyy-MM-dd HH:mm:ss）
	 */
	fun toDateTimeString(date: LocalDateTime?): String? = dateTimeFormatter.format(date)

	/**
	 * 获取当前月份的第一天
	 *
	 * @return 当前月份的第一天
	 */
	val firstOfMonth: LocalDate?
		get() = today.with(TemporalAdjusters.firstDayOfMonth())

	/**
	 * 获取当前月份的最后一天
	 *
	 * @return 当前月份的最后一天
	 */
	val endOfMonth: LocalDate?
		get() = today.with(TemporalAdjusters.lastDayOfMonth())

	/**
	 * 将时间戳转换为日期（毫秒）
	 *
	 * @param millis 时间戳（毫秒）
	 * @return 日期（毫秒）
	 */
	fun toDate(millis: Long): LocalDate =
		Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

	/**
	 * 将日期字符串（格式：yyyy-MM-dd）转换为日期
	 *
	 * @param date 日期字符串（格式：yyyy-MM-dd）
	 * @return 日期
	 */
	fun toDate(date: String?): LocalDate = LocalDate.parse(date, dateFormatter)

	/**
	 * 将日期转换为时间戳（毫秒）
	 *
	 * @param date 日期
	 * @return 时间戳（毫秒）
	 */
	fun toMillis(date: LocalDate): Long = toMillis(date.atTime(LocalTime.NOON))

	/**
	 * 将日期时间转换为时间戳（毫秒）
	 *
	 * @param date 日期时间
	 * @return 时间戳（毫秒）
	 */
	fun toMillis(date: LocalDateTime): Long =
		date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

	/**
	 * 将日期字符串（格式：yyyy-MM-dd）转换为时间戳（毫秒）
	 *
	 * @param date 日期字符串（格式：yyyy-MM-dd）
	 * @return 时间戳（毫秒）
	 */
	fun toMillis(date: String?): Long = toMillis(LocalDate.parse(date))
}