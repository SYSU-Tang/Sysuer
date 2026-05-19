package com.sysu.edu.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.datastore.preferences.core.MutablePreferences;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.rxjava3.RxDataStore;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.api.CommonUtil;
import com.sysu.edu.model.JwxtModel;

import io.reactivex.rxjava3.core.Single;
import okhttp3.Request;

public class WidgetUpdateWorker extends Worker {
    
    JwxtWidgetModel model = new JwxtWidgetModel(getApplicationContext());
    
    public WidgetUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        try {
            JSONArray networkData = getData();
            RxDataStore<Preferences> dataStore = DataStoreManager.getInstance(getApplicationContext());
            Preferences p = dataStore.updateDataAsync(prefsIn -> {
                MutablePreferences mutablePreferences = prefsIn.toMutablePreferences();
                mutablePreferences.set(DataStoreManager.TODAY_CLASS, networkData.toJSONString());
                return Single.just(mutablePreferences);
            }).blockingGet();
            int widgetId = getInputData().getInt("widget_id", -1);
            if (widgetId != -1) {
                AppWidgetProviderInfo info = AppWidgetManager.getInstance(getApplicationContext()).getAppWidgetInfo(widgetId);
                if (info != null) updateWidget(info.provider.getClass());
            }
            return Result.success();
        } catch (Exception e) {
            return Result.failure();
        }
    }
    
    private void updateWidget(Class<?> widgetClass) {
        getApplicationContext().sendBroadcast(new Intent(getApplicationContext(), widgetClass)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, AppWidgetManager.getInstance(getApplicationContext())
                        .getAppWidgetIds(new ComponentName(getApplicationContext(), widgetClass))));
    }
    
    private JSONArray getData() {
        getTerm();
        CommonUtil.Tuple2<Integer, JSONObject> r1 = model.execute(model.getNextRequest());
        String term = r1.getSecond().getJSONObject("data").getString("acadYearSemester");
        getWeek(term);
        CommonUtil.Tuple2<Integer, JSONObject> r2 = model.execute(model.getNextRequest());
        getTodayCourses(term);
        CommonUtil.Tuple2<Integer, JSONObject> r3 = model.execute(model.getNextRequest());
        return JSONArray.of(r1.getSecond(), r2.getSecond(), r3.getSecond());
    }
    
    void getTerm() {
        model.add("jwxt/base-info/acadyearterm/showNewAcadlist", 0);
    }
    
    void getWeek(String term) {
        model.add("jwxt/timetable-search/classTableInfo/getDateWeekly?academicYear=" + term, 1);
    }
    
    void getTodayCourses(String term) {
        model.add("jwxt/timetable-search/classTableInfo/queryTodayStudentClassTable?academicYear=" + term, 2);
    }
    
    static class JwxtWidgetModel extends JwxtModel {
        public JwxtWidgetModel(Context context) {
            super(context);
        }
        
        @Override
        protected void retry(CommonUtil.Tuple2<Request, Integer> request) {
            execute(request);
        }
    }
}

