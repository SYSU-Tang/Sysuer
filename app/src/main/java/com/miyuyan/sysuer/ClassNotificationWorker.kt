package com.miyuyan.sysuer

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class ClassNotificationWorker(context: Context, workerParams: WorkerParameters) :
	Worker(context, workerParams) {
	override fun doWork(): Result {
		ClassIsland.triggerAndScheduleTick(applicationContext)
		return Result.success()
	}
}