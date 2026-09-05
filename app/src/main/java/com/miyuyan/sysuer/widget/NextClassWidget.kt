package com.miyuyan.sysuer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.datastore.preferences.core.Preferences
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager.Companion.getInstance
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.academic.AgendaActivity
import com.miyuyan.sysuer.api.DataStoreManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class NextClassWidget : AppWidgetProvider() {
	var delay: Long = 0
	@OptIn(ExperimentalCoroutinesApi::class) override fun onUpdate(context: Context,
	                                                               appWidgetManager: AppWidgetManager,
	                                                               appWidgetIds: IntArray) {
		val pendingResult = goAsync()
		Executors.newSingleThreadExecutor().let { executor ->
			executor.execute {
				try {
					val cachedData = DataStoreManager.getInstance(context.applicationContext)
						.data()
						.map<JSONArray> { prefs: Preferences? -> JSONArray.parseArray(prefs!![DataStoreManager.TODAY_CLASS]) }
						.firstOrError()
						.blockingGet()
					val remoteViews = RemoteViews(context.packageName, R.layout.widget_next_class)
					cachedData.forEachIndexed { it, v ->
						handlerMessage(it, v as JSONObject, context, remoteViews)
					}
					appWidgetIds.forEach {
						appWidgetManager.updateAppWidget(it, remoteViews)
					}
					getInstance(context).enqueueUniqueWork("NextClassWidget", ExistingWorkPolicy.KEEP, OneTimeWorkRequest.Builder(WidgetUpdateWorker::class.java)
						.setConstraints(Constraints.Builder()
											.setRequiredNetworkType(NetworkType.CONNECTED)
											.build())
						.setInputData(Data.Builder()
										  .putString("component", "NextClassWidget")
										  .build())
						.setInitialDelay(delay, TimeUnit.MILLISECONDS)
						.build())
				} catch (_: Exception) {
				} finally {
					pendingResult.finish()
				}
			}
		}
	}
	
	private fun handlerMessage(what: Int,
	                           response: JSONObject,
	                           context: Context,
	                           remoteViews: RemoteViews) {
		if (response.get("code") == 200) {
			when (what) {
				2 -> {
					val todayCourse = mutableListOf<JSONObject>()
					val tomorrowCourse = mutableListOf<JSONObject>()
					val beforeArray = mutableListOf<JSONObject>()
					val afterArray = mutableListOf<JSONObject>()
					response.getJSONArray("data").forEach { e: Any? ->
						val item = e as JSONObject
						val status = getTimePosition(item.getString("teachingDate") + " " + item.getString("startTime"), item.getString("teachingDate") + " " + item.getString("endTime"))
						item["status"] = status
						item["time"] = item.get("startTime")
							.toString() + "~" + item.get("endTime")
						item["course"] = "第" + item.get("startClassTimes") + "~" + item.get("endClassTimes") + "节课"
						val flag = item.get("useflag") as String?
						if ("TD" == flag) (if (status == "before") beforeArray else afterArray).add(item)
						(if ("TD" == flag) todayCourse else tomorrowCourse).add(item)
					}
					val isAvailable = !afterArray.isEmpty() || !tomorrowCourse.isEmpty()
					var array = JSONObject()
					if (isAvailable) {
						array = (if (afterArray.isEmpty()) tomorrowCourse[0] else todayCourse[beforeArray.size])
						delay = LocalDateTime.parse("${array.getString("teachingDate")} ${array.getString("endTime")}", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
							.atZone(ZoneId.systemDefault())
							.toInstant()
							.toEpochMilli() - System.currentTimeMillis()
					}
					remoteViews.setTextViewText(R.id.content, "${context.getString(R.string.location)}：${if (isAvailable) array.getString("teachingPlace") else context.getString(R.string.none)}\n${context.getString(R.string.time)}：${if (isAvailable) array.getString("teachingDate") else context.getString(R.string.none)} ${if (isAvailable) array.getString("time") else context.getString(R.string.none)}")
					remoteViews.setTextViewText(R.id.title, if (isAvailable) array.getString("courseName") else context.getString(R.string.none))
				}
				0 -> remoteViews.setTextViewText(R.id.day, "${
					response.getJSONObject("data").getString("acadYearSemester")
				}学期\n${
					DateTimeFormatter.ofPattern("MM-dd").format(LocalDate.now())
				}周${
					context.resources.getStringArray(R.array.week_values)[LocalDate.now()
						.getDayOfWeek().value - 1]
				}")
				1 -> remoteViews.setTextViewText(R.id.week, String.format(context.getString(R.string.week_s), response.getJSONArray("data")
					.getJSONObject(0)
					.getString("weekTimes")))
			}
			remoteViews.setOnClickPendingIntent(android.R.id.background, PendingIntent.getActivity(context, 0, Intent(context, AgendaActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
		}
	}
	
	fun getTimePosition(from: String?, to: String?): String {
		val now = LocalDateTime.now()
		val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
		return if (now.isBefore(LocalDateTime.parse(from, formatter))) "after" else if (now.isAfter(LocalDateTime.parse(to, formatter))) "before" else "in"
	}
}
