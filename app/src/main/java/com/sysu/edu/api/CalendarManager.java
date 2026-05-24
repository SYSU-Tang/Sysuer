package com.sysu.edu.api;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

public class CalendarManager {
    
    final LocalDate today = LocalDate.now();
    final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public int getYear() {
        return today.getYear();
    }
    
    public String toDateString(LocalDate date) {
        return dateFormatter.format(date);
    }
    
    public String toDateStringPLus(int days) {
        return toDateString(today.plusDays(days));
    }
    
    public String toDateTimeString(LocalDateTime date) {
        return dateTimeFormatter.format(date);
    }
    
    
    public LocalDate getFirstOfMonth() {
        return today.with(TemporalAdjusters.firstDayOfMonth());
    }
    
    public LocalDate getEndOfMonth() {
        return today.with(TemporalAdjusters.lastDayOfMonth());
    }
    
    public LocalDate toDate(long millis) {
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate();
    }
    
    public long toMillis(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}