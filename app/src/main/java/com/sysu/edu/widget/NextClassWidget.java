package com.sysu.edu.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.academic.AgendaActivity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NextClassWidget extends AppWidgetProvider {
    
    long delay = 0;
    
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final PendingResult pendingResult = goAsync();
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            executor.execute(() -> {
                try {
                    JSONArray cachedData = DataStoreManager.getInstance(context.getApplicationContext()).data()
                            .map(prefs -> JSONArray.parseArray(prefs.get(DataStoreManager.TODAY_CLASS)))
                            .firstOrError()
                            .blockingGet();
                    RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_next_class);
                    for (int i = 0; i < cachedData.size(); i++)
                        handlerMessage(i, cachedData.getJSONObject(i), context, remoteViews);
                    for (int appWidgetId : appWidgetIds) {
                        WorkManager.getInstance(context).enqueueUniqueWork("widget_work_" + appWidgetId,
                                ExistingWorkPolicy.KEEP, new OneTimeWorkRequest.Builder(RecentClassWidgetWorker.class)
                                        .setConstraints(new Constraints.Builder()
                                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                                .build())
                                        .setInputData(new Data.Builder()
                                                .putInt("widget_id", appWidgetId)
                                                .build())
                                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                                        .build());
                        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
                    }
                } catch (Exception _) {
                } finally {
                    pendingResult.finish();
                }
            });
        }
    }
    
    private void handlerMessage(int what, JSONObject response, Context context, RemoteViews remoteViews) {
        if (response.get("code").equals(200)) {
            switch (what) {
                case 2 -> {
                    
                    final ArrayList<JSONObject> todayCourse = new ArrayList<>();
                    final ArrayList<JSONObject> tomorrowCourse = new ArrayList<>();
                    ArrayList<JSONObject> beforeArray = new ArrayList<>();
                    ArrayList<JSONObject> afterArray = new ArrayList<>();
                    response.getJSONArray("data").forEach(e -> {
                        JSONObject item = (JSONObject) e;
                        String status = getTimePosition(item.getString("teachingDate") + " " + item.getString("startTime"), item.getString("teachingDate") + " " + item.getString("endTime"));
                        item.put("status", status);
                        item.put("time", item.get("startTime") + "~" + item.get("endTime"));
                        item.put("course", "第" + item.get("startClassTimes") + "~" + item.get("endClassTimes") + "节课");
                        String flag = (String) item.get("useflag");
                        if ("TD".equals(flag))
                            (Objects.equals(status, "before") ? beforeArray : afterArray).add(item);
                        ("TD".equals(flag) ? todayCourse : tomorrowCourse).add(item);
                    });
                    boolean isAvailable = !afterArray.isEmpty() || !tomorrowCourse.isEmpty();
                    JSONObject array = new JSONObject();
                    if (isAvailable) {
                        array = afterArray.isEmpty() ? tomorrowCourse.get(0) : todayCourse.get(beforeArray.size());
                        delay = LocalDateTime.parse(String.format("%s %s",
                                array.getString("teachingDate"), array.getString("endTime")), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - System.currentTimeMillis();
                    }
                    remoteViews.setTextViewText(R.id.content, String.format("%s：%s\n%s：%s %s",
                            context.getString(R.string.location), isAvailable ? array.getString("teachingPlace") : context.getString(R.string.none),
                            context.getString(R.string.time), isAvailable ? array.getString("teachingDate") : context.getString(R.string.none),
                            isAvailable ? array.getString("time") : context.getString(R.string.none)));
                    remoteViews.setTextViewText(R.id.title, isAvailable ? array.getString("courseName") : context.getString(R.string.none));
                }
                case 0 ->
                        remoteViews.setTextViewText(R.id.day, String.format("%s学期\n%s周%s", response.getJSONObject("data").getString("acadYearSemester"), DateTimeFormatter.ofPattern("MM-dd").format(LocalDate.now()),
                                context.getResources().getStringArray(R.array.week_values)[LocalDate.now().getDayOfWeek().getValue() - 1]));
                case 1 ->
                        remoteViews.setTextViewText(R.id.week, String.format(context.getString(R.string.week_s), response.getJSONArray("data").getJSONObject(0).getString("weekTimes")));
            }
            remoteViews.setOnClickPendingIntent(android.R.id.background, PendingIntent.getActivity(context, 0, new Intent(context, AgendaActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        }
    }
    
    String getTimePosition(String from, String to) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return now.isBefore(LocalDateTime.parse(from, formatter)) ? "after" : now.isAfter(LocalDateTime.parse(to, formatter)) ? "before" : "in";
    }
}
