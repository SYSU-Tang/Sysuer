package com.miyuyan.sysuer.api

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

object CalendarManager {
    val today: LocalDate get() = DateTimeManager.today
    val dateFormatter: DateTimeFormatter get() = DateTimeManager.dateFormatter
    val dateTimeFormatter: DateTimeFormatter get() = DateTimeManager.dateTimeFormatter
    val year: Int get() = DateTimeManager.year

    fun toDateString(millis: Long): String? = DateTimeManager.toDateString(millis)
    fun toDateString(date: LocalDate?): String? = DateTimeManager.toDateString(date)
    fun toDateStringPLus(days: Int): String? = DateTimeManager.toDateStringPLus(days)
    fun toDateTimeString(date: LocalDateTime?): String? = DateTimeManager.toDateTimeString(date)
    val firstOfMonth: LocalDate? get() = DateTimeManager.firstOfMonth
    val endOfMonth: LocalDate? get() = DateTimeManager.endOfMonth

    fun toDate(millis: Long): LocalDate? = DateTimeManager.toDate(millis)
    fun toMillis(date: LocalDate): Long = DateTimeManager.toMillis(date)
    fun toMillis(date: LocalDateTime): Long = DateTimeManager.toMillis(date)
    fun toMillis(date: String?): Long = DateTimeManager.toMillis(date)
}
