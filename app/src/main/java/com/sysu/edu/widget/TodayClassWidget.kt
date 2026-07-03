package com.sysu.edu.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.widget.RemoteViewsCompat;
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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TodayClassWidget extends AppWidgetProvider {
    
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final PendingResult pendingResult = goAsync();
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            executor.execute(() -> {
                try {
                    JSONArray cachedData = DataStoreManager.getInstance(context.getApplicationContext()).data()
                            .map(prefs -> JSONArray.parseArray(prefs.get(DataStoreManager.TODAY_CLASS)))
                            .firstOrError() // 只取最新的一条数据
                            .blockingGet();
                    RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_today_class);
                    for (int i = 0; i < cachedData.size(); i++)
                        handlerMessage(i, cachedData.getJSONObject(i), context, remoteViews);
                    for (int appWidgetId : appWidgetIds)
                        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
                    WorkManager.getInstance(context).enqueueUniqueWork(
                            "TodayClassWidget",
                            ExistingWorkPolicy.KEEP,
                            new OneTimeWorkRequest.Builder(WidgetUpdateWorker.class)
                                    .setConstraints(new Constraints.Builder()
                                            .setRequiredNetworkType(NetworkType.CONNECTED)
                                            .build())
                                    .setInputData(new Data.Builder()
                                            .putString("component", "TodayClassWidget")
                                            .build())
                                    .setInitialDelay(24 - LocalTime.now().getHour(), TimeUnit.HOURS)
                                    .build());
                } catch (Exception _) {
                } finally {
                    pendingResult.finish();
                }
            });
        }
    }
    
    String getTimePosition(String from, String to) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return now.isBefore(LocalDateTime.parse(from, formatter)) ? "after" : now.isAfter(LocalDateTime.parse(to, formatter)) ? "before" : "in";
    }
    
    void handlerMessage(Integer what, JSONObject response, Context context, RemoteViews remoteViews) {
        if (response.get("code").equals(200)) {
            switch (what) {
                case 2 -> {
                    ArrayList<JSONObject> beforeArray = new ArrayList<>();
                    RemoteViewsCompat.RemoteCollectionItems.Builder items = new RemoteViewsCompat.RemoteCollectionItems.Builder();
                    response.getJSONArray("data").forEach(e -> {
                        JSONObject item = (JSONObject) e;
                        String status = getTimePosition(item.getString("teachingDate") + " " + item.getString("startTime"), item.getString("teachingDate") + " " + item.getString("endTime"));
                        item.put("status", status);
                        item.put("time", item.get("startTime") + "~" + item.get("endTime"));
                        String flag = (String) item.get("useflag");
                        if ("TD".equals(flag)) {
                            if (Objects.equals(status, "before")) beforeArray.add(item);
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
                    
                }
                case 0 ->
                        remoteViews.setTextViewText(R.id.day, String.format("%s %s周%s", response.getJSONObject("data").getString("acadYearSemester"),
                                LocalDate.now().format(DateTimeFormatter.ofPattern("MM.dd")), context.getResources().getStringArray(R.array.week_values)[LocalDate.now().getDayOfWeek().getValue() - 1]));
                case 1 ->
                        remoteViews.setTextViewText(R.id.week, String.format(context.getString(R.string.week_s), response.getJSONArray("data").getJSONObject(0).getString("weekTimes")));
            }
            remoteViews.setOnClickPendingIntent(android.R.id.background, PendingIntent.getActivity(context, 0, new Intent(context, AgendaActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            remoteViews.setTextViewText(R.id.widget_name, context.getString(R.string.today_class));
        }
    }
}
