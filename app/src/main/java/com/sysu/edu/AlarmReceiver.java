package com.sysu.edu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        System.out.println("AlarmReceiver onReceive");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "NEXT COURSE")
                .setSmallIcon(R.drawable.logo)
                .setContentTitle("定时通知")
                .setContentText("这是定时发送的通知")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(0, builder.build());
    }
}
