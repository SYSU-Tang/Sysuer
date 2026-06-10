package com.sysu.edu;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class ClassNotificationWorker extends Worker {
    public ClassNotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        ClassIsland.sendCourseNotification(getApplicationContext(), getInputData().getString("courseName"), getInputData().getString("time"), getInputData().getString("teachingPlace"));
        return Result.success();
    }
}
