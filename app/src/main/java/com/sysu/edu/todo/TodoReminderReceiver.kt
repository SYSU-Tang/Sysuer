package com.sysu.edu.todo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.sysu.edu.R

class TodoReminderReceiver : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		val todoId = intent.getIntExtra("todo_id", -1)
		val notificationId = intent.getIntExtra("notification_id", todoId)
		val title = intent.getStringExtra("todo_title") ?: context.getString(R.string.todo)
		val description = intent.getStringExtra("todo_description") ?: ""
		
		showNotification(context, notificationId, title, description)
	}
	
	private fun showNotification(context: Context, id: Int, title: String, description: String) {
		val channelId = "todo_reminder_channel"
		val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		val channel = NotificationChannel(channelId, context.getString(R.string.todo), NotificationManager.IMPORTANCE_HIGH).apply {
			this.description = context.getString(R.string.remind)
		}
		notificationManager.createNotificationChannel(channel)
		val intent = Intent(context, TodoActivity::class.java).apply {
			flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
		}
		val pendingIntent = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
		
		val builder = NotificationCompat.Builder(context, channelId)
			.setSmallIcon(R.drawable.alarm)
			.setContentTitle(title)
			.setContentText(description)
			.setPriority(NotificationCompat.PRIORITY_HIGH)
			.setAutoCancel(true)
			.setContentIntent(pendingIntent)
		
		notificationManager.notify(id, builder.build())
	}
}
