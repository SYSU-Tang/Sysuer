package com.sysu.edu.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.core.widget.RemoteViewsCompat;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.academic.AgendaActivity;
import com.sysu.edu.api.ContextUtil;
import com.sysu.edu.api.CookieManager;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.TargetUrl;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class TodayClassWidget extends AppWidgetProvider {

    HttpManager http;

    private static void update(AppWidgetManager appWidgetManager, int[] appWidgetIds, RemoteViews remoteViews) {
        for (int appWidgetId : appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        http = new HttpManager(new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_today_class);
                if (msg.what != -1 && msg.getData().getBoolean("isJSON")) {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    if (response.get("code").equals(200)) {
                        switch (msg.what) {
                            case 1:
//                                ArrayList<JSONObject> todayCourse = new ArrayList<>();
//                                ArrayList<JSONObject> tomorrowCourse = new ArrayList<>();
                                ArrayList<JSONObject> beforeArray = new ArrayList<>();
//                                ArrayList<JSONObject> afterArray = new ArrayList<>();
                                RemoteViewsCompat.RemoteCollectionItems.Builder items = new RemoteViewsCompat.RemoteCollectionItems.Builder();
                                response.getJSONArray("data").forEach(e -> {
                                    JSONObject item = (JSONObject) e;
                                    String status = getTimePosition(item.getString("teachingDate") + " " + item.getString("startTime"), item.getString("teachingDate") + " " + item.getString("endTime"));
                                    item.put("status", status);
                                    item.put("time", item.get("startTime") + "~" + item.get("endTime"));
//                                    item.put("course", "第" + item.get("startClassTimes") + "~" + item.get("endClassTimes") + "节课");
                                    String flag = (String) item.get("useflag");
                                    if ("TD".equals(flag)) {
                                        if (Objects.equals(status, "before")) beforeArray.add(item);
//                                        (Objects.equals(status, "before") ? beforeArray : afterArray).add(item);
                                        var view = new RemoteViews(context.getPackageName(), R.layout.widget_item);
                                        view.setTextViewText(R.id.content, String.format("%s：%s\n%s：%s %s",
                                                context.getString(R.string.location), item.getString("teachingPlace"),
                                                context.getString(R.string.time), item.getString("teachingDate"),
                                                item.getString("time")));
                                        view.setTextViewText(R.id.title, item.getString("courseName"));
                                        items.addItem(View.generateViewId(), view);
                                    }
                                });
                                RemoteViewsCompat.setRemoteAdapter(context, remoteViews, R.layout.widget_item, R.id.list, items.build());
                                remoteViews.setScrollPosition(R.id.list, beforeArray.size());
                                OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(DailyWidgetWorker.class)
                                        .setConstraints(new Constraints.Builder()
                                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                                .build())
                                        .setInitialDelay(24 - LocalTime.now().getHour(), TimeUnit.HOURS)
                                        .build();
                                WorkManager.getInstance(context).enqueue(workRequest);
                                break;
                            case 3:
                                String term = response.getJSONObject("data").getString("acadYearSemester");
                                getTodayCourses(term);
                                getWeek(term);
                                remoteViews.setTextViewText(R.id.day, String.format("%s %s周%s", term,
                                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("M.dd")), new String[]{"日", "一", "二", "三", "四", "五", "六"}[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1]));
                                break;
                            case 4:
                                remoteViews.setTextViewText(R.id.week, String.format(context.getString(R.string.week_d), response.getJSONArray("data").getJSONObject(0).getString("weekTimes")));
                                break;
                        }

                        remoteViews.setOnClickPendingIntent(android.R.id.background, PendingIntent.getActivity(context, 0, new Intent(context, AgendaActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
                        remoteViews.setTextViewText(R.id.widget_name, context.getString(R.string.today_class));
                        update(appWidgetManager, appWidgetIds, remoteViews);

                    }
                } else {
                    new ContextUtil(context).login(TargetUrl.JWXT, () -> getTerm());
                }
            }
        });
        http.setCookieManager(new CookieManager(context));
        getTerm();
    }

    String getTimePosition(String from, String to) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return now.isBefore(LocalDateTime.parse(from, formatter)) ? "after" : now.isAfter(LocalDateTime.parse(to, formatter)) ? "before" : "in";
    }

    void getTerm() {
        http.setReferrer("https://jwxt.sysu.edu.cn/jwxt//yd/classSchedule/");
        http.getRequest("https://jwxt.sysu.edu.cn/jwxt/base-info/acadyearterm/showNewAcadlist", 3);
    }

    void getWeek(String term) {
        http.setReferrer("https://jwxt.sysu.edu.cn/jwxt/yd/index/");
        http.getRequest("https://jwxt.sysu.edu.cn/jwxt/timetable-search/classTableInfo/getDateWeekly?academicYear=" + term, 4);
    }

    void getTodayCourses(String term) {
        http.setReferrer(null);
        http.getRequest("https://jwxt.sysu.edu.cn/jwxt/timetable-search/classTableInfo/queryTodayStudentClassTable?academicYear=" + term, 1);
    }
}
