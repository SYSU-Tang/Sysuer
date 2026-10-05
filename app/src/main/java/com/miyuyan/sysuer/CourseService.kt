package com.miyuyan.sysuer

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Message
import android.os.Process
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat

class CourseService : Service() {
	var serviceHandler: Handler? = null

	override fun onBind(intent: Intent?): IBinder {
		return MyBinder()
	}

	override fun onCreate() {
		println("CourseService onCreate")
		val manager = NotificationManagerCompat.from(this)
		val notificationIntent = Intent(this, MainActivity::class.java)
		val pendingIntent = PendingIntent.getActivity(
				this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
		)
		val serviceChannel = "service_channel"
		val notificationChannel = NotificationChannelCompat.Builder(
				serviceChannel, NotificationManagerCompat.IMPORTANCE_HIGH
		) //                .setDescription("计时")
			.setName("前台服务").build()
		manager.createNotificationChannel(notificationChannel)
		val notification = NotificationCompat.Builder(this, serviceChannel)
			.setContentTitle(getString(R.string.app_name)).setContentText("服务中")
			.setSmallIcon(R.drawable.ic_launcher_foreground).setContentIntent(pendingIntent)
			.setTicker(getString(R.string.app_name)).build()
		var type = 0
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
			type = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
		}
		ServiceCompat.startForeground(this, 1, notification, type)


		val thread = HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_FOREGROUND)
		thread.start()
		serviceHandler = object : Handler(thread.getLooper()) {
			override fun handleMessage(msg: Message) {
				super.handleMessage(msg)
				try {
					println(
							"Service" + "当前进程编号" + Thread.currentThread()
								.getName() + " ·····正在处理任务"
					)
					Thread.sleep(5000)
				} catch (_: InterruptedException) {
					Thread.currentThread().interrupt()
				}
			}
		}
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		println("CourseService onStartCommand")
		val msg = serviceHandler!!.obtainMessage()
		msg.arg1 = startId
		serviceHandler!!.sendMessage(msg)
		return START_STICKY
	}

	override fun onDestroy() {
		ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
		super.onDestroy()
	}

	internal inner class MyBinder : Binder() {
		val service: CourseService
			/**
			 * 获取Service的方法
			 * 
			 * @return 返回PlayerService
			 */
			get() = this@CourseService
	}
}
