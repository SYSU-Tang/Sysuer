package com.sysu.edu;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class ClassIsland {
    
    private static final String CHANNEL_ID = "course_schedule_channel";
    private static final String CHANNEL_NAME = "课程表通知";
    private static final int NOTIFICATION_ID = 1001;
    
    /**
     * 发送课程表灵动岛通知
     **/
    public static void sendCourseNotification(Context context,
                                              String className,
                                              String timeRemaining,
                                              String classroom) {
        
        createNotificationChannel(context);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID);
        builder.setSmallIcon(R.drawable.book)
                .setContentTitle(className)
                .setContentText(timeRemaining + "，" + classroom)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(timeRemaining + "，" + classroom))
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
    }
    
    /**
     * 创建通知渠道
     */
    private static void createNotificationChannel(Context context) {
        NotificationChannelCompat channel = new NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setDescription("课程表提醒通知")
                .setName(CHANNEL_NAME).build();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.createNotificationChannel(channel);
    }
}