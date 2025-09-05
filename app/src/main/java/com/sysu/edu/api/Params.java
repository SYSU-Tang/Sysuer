package com.sysu.edu.api;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Toast;

import com.sysu.edu.academic.BrowserActivity;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Params {
    Activity context;

    static Calendar c=Calendar.getInstance();
    public Params(Activity context) {
        this.context = context;
    }

    public static int getYear(){
        return c.get(Calendar.YEAR);
    }

    public static int getMonth(){
        return c.get(Calendar.MONTH);
    }

    public int dpToPx(int dps) {
        return Math.round(context.getResources().getDisplayMetrics().density * dps);
    }

    public static int getDay(){
        return c.get(Calendar.DAY_OF_MONTH);
    }

    public static String toDate(){
        return toDate(Calendar.getInstance());
    }

    public static String getDateTime(){
        return toDate(Calendar.getInstance());
    }

    public static String toDate(Calendar calendar){
        return new SimpleDateFormat("yyyy-MM-dd",Locale.CHINA).format(calendar.getTime());
    }

    public static String toDate(Date date){
        return new SimpleDateFormat("yyyy-MM-dd",Locale.CHINA).format(date);
    }

    public static String getDateTime(Calendar calendar){
        return getDateTime(calendar.getTime());
    }

    public static String getDateTime(Date date){
        return new SimpleDateFormat("yyyy-MM-dd hh:mm:ss",Locale.CHINA).format(date);
    }

    public static Calendar getFirstOfMonth(){
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH,c.getActualMinimum(Calendar.DAY_OF_MONTH));
        return calendar;
    }

    public static Calendar getFirstOfMonth(int month){
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH,month);
        calendar.set(Calendar.DAY_OF_MONTH,c.getActualMinimum(Calendar.DAY_OF_MONTH));
        return calendar;
    }

    public static Calendar getEndOfMonth(){
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH,c.getActualMaximum(Calendar.DAY_OF_MONTH));
        return calendar;
    }

    public static Calendar getEndOfMonth(int month){
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH,month);
        calendar.set(Calendar.DAY_OF_MONTH,c.getActualMaximum(Calendar.DAY_OF_MONTH));
        return calendar;
    }

    public int getWidth() {
        DisplayMetrics dm = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getRealMetrics(dm);
        return dm.widthPixels;
    }
    public int getColumn() {
        return (getWidth() < dpToPx(540)) ? 1 : (getWidth() < dpToPx(900)) ? 2 : 3;
    }

    public String getCookie() {
        return context.getSharedPreferences("privacy", Context.MODE_PRIVATE).getString("Cookie", "");
    }

    public String getToken() {
        return context.getSharedPreferences("privacy", Context.MODE_PRIVATE).getString("token", "");
    }

    public View.OnClickListener browse(String url) {
        return (View v) -> v.getContext().startActivity(new Intent(context, BrowserActivity.class).setData(Uri.parse(url)));
    }

    public void copy(String a,String b) {
        ClipboardManager clip = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        clip.setPrimaryClip(ClipData.newPlainText(a, b));
    }
    public void toast(int resource){
        toast(context.getString(resource));
    }

    public void toast(String toast){
        Toast.makeText(context,toast,Toast.LENGTH_LONG).show();
    }
}
