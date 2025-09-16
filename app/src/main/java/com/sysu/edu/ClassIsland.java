package com.sysu.edu;

import android.app.Notification;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ClassIsland extends Worker {

    Context context;
    public ClassIsland(Context context, WorkerParameters wp){
        super(context,wp);
        this.context = context;
    }
    @NonNull
    @Override
    public Result doWork() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                //binding.time.setText(new SimpleDateFormat("hh:mm:ss", Locale.CHINESE).format(new Date()));
                if(!new SimpleDateFormat("hh:mm",Locale.CHINESE).format(new Date()).equals("13:50")){
                    Notification notice = new Notification.Builder(context).setContentTitle("时间").setSubText("13:50").build();
                   // notice.
                    return;
                }
                handler.postDelayed(this,500);
                System.out.println(new SimpleDateFormat("hh:mm:ss",Locale.CHINESE).format(new Date()));
            }
        });
        return Result.success();
    }
}
