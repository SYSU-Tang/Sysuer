package com.sysu.edu.api

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

object DateTimeManager {
	val today: LocalDate = LocalDate.now()
	val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
	val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
	val year: Int
		get() = today.year
	
	fun toDateString(millis: Long): String? = dateFormatter.format(toDate(millis))
	fun toDateString(date: LocalDate?): String? = dateFormatter.format(date)
	fun toDateStringPLus(days: Int): String? = toDateString(today.plusDays(days.toLong()))
	fun toDateTimeString(date: LocalDateTime?): String? = dateTimeFormatter.format(date)
	val firstOfMonth: LocalDate?
		get() = today.with(TemporalAdjusters.firstDayOfMonth())
	val endOfMonth: LocalDate?
		get() = today.with(TemporalAdjusters.lastDayOfMonth())
	
	fun toDate(millis: Long): LocalDate? =
		Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
	
	fun toMillis(date: LocalDate): Long = toMillis(date.atTime(LocalTime.NOON))
	fun toMillis(date: LocalDateTime): Long =
		date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
	
	fun toMillis(date: String?): Long =
		LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
	
}