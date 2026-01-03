package com.sysu.edu;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;

public class ClassIsland{

    final Context context;
    public ClassIsland(Context context){
        this.context = context;
    }

    public void doWork() {// 构建岛通知参数
        String islandParams = "";

// 创建通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "1");
        builder.setContentTitle("testTitle")
                .setContentText("testText")
                .setSmallIcon(R.id.campuses);

        Bundle bundle = new Bundle();
// 添加 Action 数据
        Bundle actions = new Bundle();
        //Intent intent = new Intent(Intent.ACTION_FOCUS_NOTIFICATION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Action action = new Notification.Action
                .Builder(Icon.createWithResource(context, R.drawable.exam), "Title", pendingIntent)
                .build();
        actions.putParcelable("miui.focus.action_test", action);

        bundle.putBundle("miui.focus.actions", actions);

// 添加图片数据
        Bundle pics = new Bundle();
        pics.putParcelable("miui.focus.pic_imageText", Icon.createWithResource(context, R.drawable.home));
        pics.putParcelable("miui.focus.pic_highlight", Icon.createWithResource(context, R.drawable.check));
        bundle.putBundle("miui.focus.pics", pics);

        builder.addExtras(bundle);
        Notification notification = builder.build();
// 添加岛参数
        notification.extras.putString("miui.focus.param", islandParams);
// 发送通知
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(1, notification);

//        Handler handler = new Handler(Looper.getMainLooper());
//        handler.post(new Runnable() {
//            @Override
//            public void run() {
//                //binding.time.setText(new SimpleDateFormat("hh:mm:ss", Locale.CHINESE).format(new Date()));
//                if(!new SimpleDateFormat("hh:mm",Locale.CHINESE).format(new Date()).equals("13:50")){
//
//                    return;
//                }
//                handler.postDelayed(this,500);
//                //System.out.println(new SimpleDateFormat("hh:mm:ss",Locale.CHINESE).format(new Date()));
//            }
//        });
    }
}
