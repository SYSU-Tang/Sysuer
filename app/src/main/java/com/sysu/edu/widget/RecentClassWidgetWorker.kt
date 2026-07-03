package com.sysu.edu.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters

class RecentClassWidgetWorker(context: Context, workerParams: WorkerParameters) :
	Worker(context, workerParams) {
	override fun doWork(): Result {
		applicationContext.startService(Intent(applicationContext, RecentClassWidget::class.java).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
												 .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, AppWidgetManager.getInstance(applicationContext)
													 .getAppWidgetIds(ComponentName(applicationContext, RecentClassWidget::class.java))))
		return Result.success()
	}
}
