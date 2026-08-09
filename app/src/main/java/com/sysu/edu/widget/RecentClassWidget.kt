package com.sysu.edu.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.widget.RemoteViewsCompat
import androidx.core.widget.RemoteViewsCompat.setRemoteAdapter
import androidx.datastore.preferences.core.Preferences
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager.Companion.getInstance
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.R
import com.sysu.edu.academic.AgendaActivity
import com.sysu.edu.api.DataStoreManager
import io.reactivex.rxjava3.functions.Function
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class RecentClassWidget : AppWidgetProvider() {
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
						.map<JSONArray>(Function { prefs: Preferences? -> JSONArray.parseArray(prefs!![DataStoreManager.TODAY_CLASS]) })
						.firstOrError()
						.blockingGet()
					val remoteViews = RemoteViews(context.packageName, R.layout.widget_today_class)
					for (i in cachedData.indices) handlerMessage(i, cachedData.getJSONObject(i), context, remoteViews)
					for (appWidgetId in appWidgetIds) appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
					getInstance(context).enqueueUniqueWork("RecentClassWidget", ExistingWorkPolicy.KEEP, OneTimeWorkRequest.Builder(WidgetUpdateWorker::class.java)
						.setConstraints(Constraints.Builder()
											.setRequiredNetworkType(NetworkType.CONNECTED)
											.build())
						.setInputData(Data.Builder()
										  .putString("component", "RecentClassWidget")
										  .build())
						.setInitialDelay(delay, TimeUnit.MILLISECONDS)
						.build())
				} catch (`_`: Exception) {
				} finally {
					pendingResult.finish()
				}
			}
		}
	}
	
	override fun onDeleted(context: Context, appWidgetIds: IntArray) {
		appWidgetIds.forEach {
			getInstance(context).cancelUniqueWork("$it")
		}
		super.onDeleted(context, appWidgetIds)
	}
	
	fun handlerMessage(what: Int,
	                   response: JSONObject,
	                   context: Context,
	                   remoteViews: RemoteViews) {
		if (response.get("code") == 200) {
			when (what) {
				2 -> {
					val finish = AtomicReference<LocalDateTime?>(null)
					val items = RemoteViewsCompat.RemoteCollectionItems.Builder()
					response.getJSONArray("data").forEach { e: Any? ->
						val item = e as JSONObject
						val status = getTimePosition(item.getString("teachingDate") + " " + item.getString("startTime"), item.getString("teachingDate") + " " + item.getString("endTime"))
						item["status"] = status
						item["time"] = item.get("startTime").toString() + "~" + item.get("endTime")
						val date = LocalDateTime.parse(item.getString("teachingDate") + " " + item.getString("startTime"), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
						if ("RT" == item.get("useflag") && (finish.get() == null || finish.get()!!
								.isBefore(date))) {
							finish.set(date)
							val view = RemoteViews(context.packageName, R.layout.widget_item)
							view.setTextViewText(R.id.content, "${context.getString(R.string.location)}：${item.getString("teachingPlace")}\n${context.getString(R.string.time)}：${item.getString("teachingDate")} ${item.getString("time")}")
							view.setTextViewText(R.id.title, item.getString("courseName"))
							items.addItem(View.generateViewId().toLong(), view)
							delay = finish.get()!!
								.atZone(ZoneId.systemDefault())
								.toInstant()
								.toEpochMilli() - System.currentTimeMillis()
						}
					}
					setRemoteAdapter(context, remoteViews, R.layout.widget_item, R.id.list, items.build())
				}
				0 -> remoteViews.setTextViewText(R.id.day, "${
					response.getJSONObject("data").getString("acadYearSemester")
				} ${
					LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM.dd"))
				}周${
					context.resources.getStringArray(R.array.week_values)[LocalDate.now()
						.getDayOfWeek().value - 1]
				}")
				1 -> remoteViews.setTextViewText(R.id.week, String.format(context.getString(R.string.week_s), response.getJSONArray("data")
					.getJSONObject(0)
					.getString("weekTimes")))
			}
			remoteViews.setOnClickPendingIntent(android.R.id.background, PendingIntent.getActivity(context, 0, Intent(context, AgendaActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
			remoteViews.setTextViewText(R.id.widget_name, context.getString(R.string.recent_class))
		}
	}
	
	fun getTimePosition(from: String?, to: String?): String {
		val now = LocalDateTime.now()
		val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
		return if (now.isBefore(LocalDateTime.parse(from, formatter))) "after" else if (now.isAfter(LocalDateTime.parse(to, formatter))) "before" else "in"
	}
}
