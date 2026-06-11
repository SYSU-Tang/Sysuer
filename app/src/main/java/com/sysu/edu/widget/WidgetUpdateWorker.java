package com.sysu.edu.widget;

import android.appwidget.AppWidgetManager;
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

import java.util.Objects;

import io.reactivex.rxjava3.core.Single;
import okhttp3.Request;

public class WidgetUpdateWorker extends Worker {
    
    final JwxtWidgetModel model = new JwxtWidgetModel(getApplicationContext());
    
    public WidgetUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        try {
            JSONArray networkData = getData();
            RxDataStore<Preferences> dataStore = DataStoreManager.getInstance(getApplicationContext());
            dataStore.updateDataAsync(prefsIn -> {
                MutablePreferences mutablePreferences = prefsIn.toMutablePreferences();
                if (networkData != null)
                    mutablePreferences.set(DataStoreManager.TODAY_CLASS, networkData.toJSONString());
                return Single.just(mutablePreferences);
            });
            String widgetName = getInputData().getString("component");
            String[] widgetNames = getInputData().getStringArray("components");
            if (widgetName != null)
                updateWidget(widgetName);
            else if (widgetNames != null)
                for (String name : widgetNames)
                    updateWidget(name);
            return Result.success();
        } catch (Exception e) {
            return Result.failure();
        }
    }
    
    private void updateWidget(String name) throws ClassNotFoundException {
        updateWidget(Class.forName(getApplicationContext().getPackageName() + ".widget." + name));
    }
    
    private void updateWidget(Class<?> widgetClass) {
        getApplicationContext().sendBroadcast(new Intent(getApplicationContext(), widgetClass)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, AppWidgetManager.getInstance(getApplicationContext())
                        .getAppWidgetIds(new ComponentName(getApplicationContext(), widgetClass))));
    }
    
    private JSONArray getData() {
        getTerm();
        CommonUtil.Tuple2<Integer, JSONObject> r1 = model.execute(Objects.requireNonNull(model.getNextRequest()));
        String term = null;
        if (r1 != null) {
            term = r1.second.getJSONObject("data").getString("acadYearSemester");
        }
        getWeek(term);
        CommonUtil.Tuple2<Integer, JSONObject> r2 = model.execute(model.getNextRequest());
        getTodayCourses(term);
        CommonUtil.Tuple2<Integer, JSONObject> r3 = model.execute(model.getNextRequest());
        return (r1 != null && r2 != null && r3 != null)
                ? JSONArray.of(r1.second, r2.second, r3.second) : null;
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
        protected void retry(@NonNull CommonUtil.Tuple2<Request, Integer> request) {
            execute(request);
        }
    }
}

