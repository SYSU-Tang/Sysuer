package com.sysu.edu

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object ClassIsland {
	private const val CHANNEL_ID = "course_schedule_channel"
	private const val CHANNEL_NAME = "课程表通知"
	private const val NOTIFICATION_ID = 1001
	
	/**
	 * 发送课程表灵动岛通知
	 */
	@JvmStatic fun sendCourseNotification(context: Context,
	                                      className: String?,
	                                      timeRemaining: String?,
	                                      classroom: String?) {
		createNotificationChannel(context)
		val builder = NotificationCompat.Builder(context, CHANNEL_ID)
		builder.setSmallIcon(R.drawable.book)
			.setContentTitle(className)
			.setContentText("$timeRemaining，$classroom")
			.setStyle(NotificationCompat.BigTextStyle().bigText("$timeRemaining，$classroom"))
			.setOngoing(true)
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
		if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) NotificationManagerCompat.from(context)
			.notify(NOTIFICATION_ID, builder.build())
	}
	
	/**
	 * 创建通知渠道
	 */
	private fun createNotificationChannel(context: Context) {
		val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
			.setDescription("课程表提醒通知")
			.setName(CHANNEL_NAME)
			.build()
		val notificationManager = NotificationManagerCompat.from(context)
		notificationManager.createNotificationChannel(channel)
	}
}