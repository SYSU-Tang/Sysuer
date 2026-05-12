package com.sysu.edu.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class NextClassWidget extends AppWidgetProvider {

    final ArrayList<JSONObject> todayCourse = new ArrayList<>();
    final ArrayList<JSONObject> tomorrowCourse = new ArrayList<>();
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
                RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_next_class);
                if (msg.what == -1) {
                    new ContextUtil(context).login(TargetUrl.JWXT, () -> getTerm());
                } else {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    if (response.get("code").equals(200)) {
                        switch (msg.what) {
                            case 1:
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
                                    //                                        Date target = new SimpleDateFormat("yyyy-MM-dd hh:mm", Locale.getDefault()).parse(String.format("%s %s",
//                                                array.getString("teachingDate"), array.getString("endTime")));
                                    LocalDateTime target = LocalDateTime.parse(String.format("%s %s",
                                            array.getString("teachingDate"), array.getString("endTime")), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                                    if (target != null)
                                        WorkManager.getInstance(context.getApplicationContext())
                                                .enqueueUniqueWork("next_class_widget_update",
                                                        ExistingWorkPolicy.KEEP, new OneTimeWorkRequest.Builder(NextClassWidgetWorker.class)
                                                                .setConstraints(new Constraints.Builder()
                                                                        .setRequiredNetworkType(NetworkType.CONNECTED)
                                                                        .build())
                                                                .setInitialDelay(target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - System.currentTimeMillis(), TimeUnit.MILLISECONDS).build());
                                }

                                remoteViews.setTextViewText(R.id.content, String.format("%s：%s\n%s：%s %s",
                                        context.getString(R.string.location), isAvailable ? array.getString("teachingPlace") : context.getString(R.string.none),
                                        context.getString(R.string.time), isAvailable ? array.getString("teachingDate") : context.getString(R.string.none),
                                        isAvailable ? array.getString("time") : context.getString(R.string.none)));
                                remoteViews.setTextViewText(R.id.title, isAvailable ? array.getString("courseName") : context.getString(R.string.none));
                                break;
                            /*case 2:
                                JSONArray dataArray = response.getJSONArray("data");
                                if (!dataArray.isEmpty()) {
                                    for (int i = 0; i < dataArray.size(); i++) {
                                        LinkedList<JSONObject> exams = List.of(thisWeekExams, nextWeekExams).get(i);
                                        TreeMap<Integer, JSONArray> sortedTimetable = new TreeMap<>();
                                        dataArray.getJSONObject(i).getJSONObject("timetable").forEach((s, t) -> {
                                            if (t != null)
                                                sortedTimetable.put(Integer.parseInt(s), (JSONArray) t);
                                        });
                                        sortedTimetable.forEach((key, value) -> {
                                            if (key.equals(sortedTimetable.firstKey()))
                                                value.forEach(c -> exams.addFirst((JSONObject) c));
                                            else
                                                value.forEach(c -> exams.addLast((JSONObject) c));
                                        });
                                    }
                                    binding.toggle2.clearChecked();
                                    binding.toggle2.check(R.id.week_18);
                                }
                                break;*/
                            case 3:
                                String term = response.getJSONObject("data").getString("acadYearSemester");
                                getTodayCourses(term);
//                                getExams(term);
                                getWeek(term);
                                remoteViews.setTextViewText(R.id.day, String.format("%s学期\n%s周%s", term, DateTimeFormatter.ofPattern("MM-dd").format(LocalDate.now()), new String[]{"日", "一", "二", "三", "四", "五", "六"}[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1]));
                                break;
                            case 4:
                                remoteViews.setTextViewText(R.id.week, String.format(context.getString(R.string.week_s), response.getJSONArray("data").getJSONObject(0).getString("weekTimes")));
                                break;
                        }

                        remoteViews.setOnClickPendingIntent(android.R.id.background, PendingIntent.getActivity(context, 0, new Intent(context, AgendaActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
                        update(appWidgetManager, appWidgetIds, remoteViews);

                    }
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

    /*void getExams(String term) {
        http.setReferrer("https://jwxt.sysu.edu.cn/jwxt/mk/");
        http.postRequest("https://jwxt.sysu.edu.cn/jwxt/examination-manage/classroomResource/queryStuEaxmInfo?code=jwxsd_ksxxck" + term, String.format("{\"acadYear\":\"%s\",\"examWeekId\":\"1928284621349085186\",\"examWeekName\":\"18-19周期末考\",\"examDate\":\"\"}", term), 2);
    }*/

}
