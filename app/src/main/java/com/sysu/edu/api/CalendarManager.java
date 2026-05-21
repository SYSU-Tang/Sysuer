package com.sysu.edu.api;

import android.icu.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CalendarManager {

    final Calendar calendar = Calendar.getInstance();

    final Date now = new Date();

    private final SimpleDateFormat dateString = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault());

    public CalendarManager() {
        calendar.setTime(now);
    }

    public int getYear() {
        return calendar.get(Calendar.YEAR);
    }

    public String toDateString(Date date) {
        return dateString.format(date);
    }

    public String toDateStringAdd(int days) {
        calendar.add(Calendar.DATE, days);
        String dateString = toDateString(calendar.getTime());
        calendar.setTime(now);
        return dateString;
    }

    public String getDateTime(Calendar calendar) {
        return getDateTime(calendar.getTime());
    }

    public String getDateTime(Date date) {
        return dateTimeFormat.format(date);
    }

    public Calendar getFirstOfMonth() {
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
        return calendar;
    }

    public Calendar getEndOfMonth() {
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        return calendar;
    }
}