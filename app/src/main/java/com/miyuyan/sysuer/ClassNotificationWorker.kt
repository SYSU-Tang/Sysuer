package com.miyuyan.sysuer

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.miyuyan.sysuer.ClassIsland.sendCourseNotification

class ClassNotificationWorker(context: Context, workerParams: WorkerParameters) :
	Worker(context, workerParams) {
	override fun doWork(): Result {
		sendCourseNotification(applicationContext, inputData.getString("courseName"), inputData.getString("time"), inputData.getString("teachingPlace"))
		return Result.success()
	}
}
