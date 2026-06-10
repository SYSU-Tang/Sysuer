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

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.academic.AgendaActivity;
import com.sysu.edu.api.ContextUtil;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.TargetUrl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class TomorrowClassWidget extends AppWidgetProvider {
    
    HttpManager http;
    
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        ContextUtil contextUtil = new ContextUtil(context);
        http = new HttpManager(new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what != -1 && msg.getData().getBoolean("isJSON")) {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_today_class);
                    if (Objects.equals(response.getJSONObject("meta").getInteger("statusCode"), 200)) {
                        JSONObject data = response.getJSONObject("data");
                        if (msg.what == 0) {
                            remoteViews.setTextViewText(R.id.day, data.getString("chooseTime"));
                            RemoteViewsCompat.RemoteCollectionItems.Builder items = new RemoteViewsCompat.RemoteCollectionItems.Builder();
                            if (data.containsKey("list") && data.get("list") != null) {
                                JSONArray list = data.getJSONArray("list");
                                list.forEach(e -> {
                                    JSONObject item = (JSONObject) e;
                                    var view = new RemoteViews(context.getPackageName(), R.layout.widget_item);
                                    view.setTextViewText(R.id.content, String.format("%s：%s\n%s：%s",
                                            context.getString(R.string.location), item.getString("place"),
                                            context.getString(R.string.time), item.getString("timeZone")));
                                    view.setTextViewText(R.id.title, item.getString("title"));
                                    items.addItem(View.generateViewId(), view);
                                });
                                RemoteViewsCompat.setRemoteAdapter(context, remoteViews, R.layout.widget_item, R.id.list, items.build());
                                OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(DailyWidgetWorker.class)
                                        .setConstraints(new Constraints.Builder()
                                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                                .build())
                                        .setInitialDelay(24 - LocalTime.now().getHour(), TimeUnit.HOURS)
                                        .build();
                                WorkManager.getInstance(context).enqueue(workRequest);
                                remoteViews.setTextViewText(R.id.week, String.format(Locale.getDefault(), "共%d节", list.size()));
                            }
                        }
                        remoteViews.setOnClickPendingIntent(android.R.id.background, PendingIntent.getActivity(context, 0, new Intent(context, AgendaActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
                        remoteViews.setTextViewText(R.id.widget_name, context.getString(R.string.tomorrow_class));
                        for (int appWidgetId : appWidgetIds)
                            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
                    }
                } else contextUtil.login(TargetUrl.PORTAL, () -> getTomorrowSchedule());
            }
        });
        getTomorrowSchedule();
    }
    
    void getTomorrowSchedule() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        http.postRequest("https://mportal.sysu.edu.cn/newClient/api/schedule/newSchedule/getNextDaySchedule", String.format("{\"types\":[],\"startTime\":\"%s\",\"endTime\":\"%s\"}", tomorrow, tomorrow.plusDays(1)), 0);
    }
}
