package com.sysu.edu;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;

public class ClassIsland {

    private static final String CHANNEL_ID = "course_schedule_channel";
    private static final String CHANNEL_NAME = "课程表通知";
    private static final int NOTIFICATION_ID = 1001;

    /**
     * 发送课程表超级岛通知
     */
    public static void sendCourseFocusNotification(Context context,
                                                   String className,
                                                   String timeRemaining,
                                                   String classroom) {

        // 1. 检查设备是否支持超级岛（关键步骤）
        if (!checkFocusNotificationSupport(context)) {
//            System.out.println("设备不支持课程表超级岛通知");
            sendFallbackNotification(context, className, timeRemaining, classroom);
            return;
        }

        createNotificationChannel(context);

        // 3. 构建超级岛核心参数
        Bundle focusParam = buildFocusParam(className, timeRemaining, classroom);

        // 4. 构建图片资源Bundle
        Bundle picsBundle = buildPicsBundle(context);

        // 5. 创建通知Builder并设置参数
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context);
        }

        // 设置基础通知信息
        builder.setContentTitle("课程提醒")
                .setContentText(className + " " + timeRemaining + "后开始")
                .setSmallIcon(R.drawable.book) // 小图标
                .setAutoCancel(true);

        // 6. 设置超级岛参数（关键！）
        Bundle extras = new Bundle();
        extras.putBundle("miui.focus.param", focusParam);
        extras.putBundle("miui.focus.pics", picsBundle);
        builder.setExtras(extras);

        // 7. 发送通知
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    /**
     * 构建超级岛核心参数Bundle
     */
    private static Bundle buildFocusParam(String className, String timeRemaining, String classroom) {
        Bundle focusBundle = new Bundle();

        try {
            // 构建baseinfo部分（使用文本组件2）
            JSONObject baseInfo = new JSONObject();
            baseInfo.put("type", 2); // 文本组件2
            baseInfo.put("title", className); // 主要文本：课程名称
            baseInfo.put("content", "下一节"); // 次要文本1
            baseInfo.put("subContent", classroom); // 次要文本2：教室
            baseInfo.put("colorTitle", "#FF000000"); // 黑色
            baseInfo.put("colorContent", "#80000000"); // 半透明黑色
            baseInfo.put("showDivider", true);

            // 构建picinfo部分（识别图形组件1）
            JSONObject picInfo = new JSONObject();
            picInfo.put("type", 1); // 识别图形组件1
            picInfo.put("pic", "miui.focus.pic_course_icon"); // 自定义图标key

            // 构建hintinfo部分（按钮组件3，用于显示倒计时）
            JSONObject hintInfo = new JSONObject();
            hintInfo.put("type", 1); // 按钮组件3
            hintInfo.put("title", timeRemaining + "后开始"); // 主要文本

            // 如果有倒计时需求，添加timerinfo
            JSONObject timerInfo = new JSONObject();
            long currentTime = System.currentTimeMillis();
            timerInfo.put("timerType", -1); // -1:倒计时开始
            timerInfo.put("timerWhen", currentTime);
            timerInfo.put("timerTotal", 600000); // 10分钟倒计时，单位毫秒
            hintInfo.put("timerinfo", timerInfo);

            // 添加操作按钮
            JSONObject actionInfo = new JSONObject();
            actionInfo.put("actionTitle", "查看详情");
            // 注意：actionIntent需要是合法的Intent URI
            actionInfo.put("actionIntent",
                    "intent:#Intent;action=VIEW;component=com.sysu.edu/.AgendaActivity;end");
            actionInfo.put("actionIntentType", 1); // 1:跳转到Activity
            hintInfo.put("actioninfo", actionInfo);

            // 将各部分放入focusBundle
            // 注意：这里将JSON字符串放入Bundle，实际开发中可能需要转换为Bundle嵌套结构
            focusBundle.putString("baseinfo", baseInfo.toString());
            focusBundle.putString("picinfo", picInfo.toString());
            focusBundle.putString("hintinfo", hintInfo.toString());

        } catch (JSONException e) {

        }

        return focusBundle;
    }

    /**
     * 构建图片资源Bundle
     */
    private static Bundle buildPicsBundle(Context context) {
        Bundle picsBundle = new Bundle();

        // 加载课程图标（确保图片尺寸符合要求）
        Bitmap courseIcon = BitmapFactory.decodeResource(
                context.getResources(), R.drawable.book);

        // 将图片放入Bundle，key要与picinfo中引用的名称一致
        // 注意：图片可能需要压缩到100KB以内
        picsBundle.putParcelable("miui.focus.pic_course_icon", courseIcon);

        return picsBundle;
    }

    /**
     * 检查设备是否支持超级岛
     * 注：这是一个简化示例，实际需要调用小米提供的API
     */
    private static boolean checkFocusNotificationSupport(Context context) {
        // 实际开发中应使用小米提供的API：
        // MiuiFocusNotificationHelper.isSupportFocusNotification(context);
        return Build.MANUFACTURER.equalsIgnoreCase("Xiaomi") &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
    }

    /**
     * 降级方案：发送普通通知
     */
    private static void sendFallbackNotification(Context context,
                                                 String className,
                                                 String timeRemaining,
                                                 String classroom) {
        // 创建普通通知的代码...
    }

    /**
     * 创建通知渠道（Android 8.0+必需）
     */
    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH // 需要高重要性才能显示为焦点通知
            );
            channel.setDescription("课程表提醒通知");
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * 更新课程进度（用于上课中场景）
     */
    public static void updateCourseProgress(Context context,
                                            int progressPercent,
                                            String statusText) {
        // 更新现有的超级岛通知，显示进度条
        Bundle focusBundle = new Bundle();

        try {
            // 构建带进度条的信息（使用进度组件2）
            JSONObject progressInfo = new JSONObject();
            progressInfo.put("progress", progressPercent); // 进度百分比
            progressInfo.put("colorProgress", "#FF4CAF50"); // 进度条颜色

            focusBundle.putString("progressinfo", progressInfo.toString());

        } catch (JSONException e) {
        }

        // 更新现有通知（使用相同的NOTIFICATION_ID）
        // ... 更新通知的代码
    }
}