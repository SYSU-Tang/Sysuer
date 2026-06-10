package com.sysu.edu;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.ServiceCompat;

public class CourseService extends Service {
    
    Handler serviceHandler;
    
    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }
    
    @Override
    public void onCreate() {
        System.out.println("CourseService onCreate");
        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);
        String serviceChannel = "service_channel";
        NotificationChannelCompat notificationChannel = new NotificationChannelCompat.Builder(serviceChannel, NotificationManagerCompat.IMPORTANCE_HIGH)
//                .setDescription("计时")
                .setName("前台服务").build();
        manager.createNotificationChannel(notificationChannel);
        Notification notification = new NotificationCompat.Builder(this, serviceChannel)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("服务中")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setTicker(getString(R.string.app_name))
                .build();
        int type = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            type = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;
        }
        ServiceCompat.startForeground(this, 1, notification, type);
        
        
        HandlerThread thread = new HandlerThread("ServiceStartArguments", android.os.Process.THREAD_PRIORITY_FOREGROUND);
        thread.start();
        serviceHandler = new Handler(thread.getLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                try {
                    System.out.println("Service" + "当前进程编号" + Thread.currentThread().getName() + " ·····正在处理任务");
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("CourseService onStartCommand");
        
        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        serviceHandler.sendMessage(msg);
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }
    
    class MyBinder extends Binder {
        /**
         * 获取Service的方法
         *
         * @return 返回PlayerService
         */
        public CourseService getService() {
            return CourseService.this;
        }
    }
}
