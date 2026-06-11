package com.sysu.edu.api

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

class CalendarManager {
	val today: LocalDate = LocalDate.now()
	val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
	val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
	val year: Int
		get() = today.year
	
	fun toDateString(millis: Long): String? {
		return dateFormatter.format(toDate(millis))
	}
	
	fun toDateString(date: LocalDate?): String? {
		return dateFormatter.format(date)
	}
	
	fun toDateStringPLus(days: Int): String? {
		return toDateString(today.plusDays(days.toLong()))
	}
	
	fun toDateTimeString(date: LocalDateTime?): String? {
		return dateTimeFormatter.format(date)
	}
	
	val firstOfMonth: LocalDate?
		get() = today.with(TemporalAdjusters.firstDayOfMonth())
	val endOfMonth: LocalDate?
		get() = today.with(TemporalAdjusters.lastDayOfMonth())
	
	fun toDate(millis: Long): LocalDate? {
		return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
	}
	
	fun toMillis(date: LocalDate): Long {
		return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
	}
	
	fun toMillis(date: LocalDateTime): Long {
		return date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
	}
	
	fun toMillis(date: String?): Long {
		return LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
	}
}