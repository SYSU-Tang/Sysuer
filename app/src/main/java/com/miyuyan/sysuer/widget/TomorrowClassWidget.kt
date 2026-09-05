package com.miyuyan.sysuer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.widget.RemoteViews
import androidx.core.widget.RemoteViewsCompat
import androidx.core.widget.RemoteViewsCompat.setRemoteAdapter
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager.Companion.getInstance
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.academic.AgendaActivity
import com.miyuyan.sysuer.api.ContextUtil
import com.miyuyan.sysuer.api.HttpManager
import com.miyuyan.sysuer.api.TargetUrl
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import java.util.concurrent.TimeUnit

class TomorrowClassWidget : AppWidgetProvider() {
	lateinit var http: HttpManager
	override fun onUpdate(context: Context,
	                      appWidgetManager: AppWidgetManager,
	                      appWidgetIds: IntArray) {
		super.onUpdate(context, appWidgetManager, appWidgetIds)
		val contextUtil = ContextUtil(context)
		http = HttpManager(object : Handler(Looper.getMainLooper()) {
			override fun handleMessage(msg: Message) {
				if (msg.what != -1 && msg.getData().getBoolean("isJSON")) {
					val response = JSONObject.parseObject(msg.obj as String?)
					val remoteViews = RemoteViews(context.packageName, R.layout.widget_today_class)
					if (response.getJSONObject("meta").getInteger("statusCode") == 200) {
						val data = response.getJSONObject("data")
						if (msg.what == 0) {
							remoteViews.setTextViewText(R.id.day, data.getString("chooseTime"))
							val items = RemoteViewsCompat.RemoteCollectionItems.Builder()
							if (data.containsKey("list") && data.get("list") != null) {
								data.getJSONArray("list").forEach { e: Any? ->
									val item = e as JSONObject
									val view = RemoteViews(context.packageName, R.layout.widget_item)
									view.setTextViewText(R.id.content, "${context.getString(R.string.location)}：${item.getString("place")}\n${context.getString(R.string.time)}：${item.getString("timeZone")}")
									view.setTextViewText(R.id.title, item.getString("title"))
									items.addItem(View.generateViewId().toLong(), view)
								}
								setRemoteAdapter(context, remoteViews, R.layout.widget_item, R.id.list, items.build())
								val workRequest = OneTimeWorkRequest.Builder(DailyWidgetWorker::class.java)
									.setConstraints(Constraints.Builder()
														.setRequiredNetworkType(NetworkType.CONNECTED)
														.build())
									.setInitialDelay((24 - LocalTime.now()
										.hour).toLong(), TimeUnit.HOURS)
									.build()
								getInstance(context).enqueue(workRequest)
								remoteViews.setTextViewText(R.id.week, String.format(Locale.getDefault(), "共%d节", data.getJSONArray("list").size))
							}
						}
						remoteViews.setOnClickPendingIntent(android.R.id.background, PendingIntent.getActivity(context, 0, Intent(context, AgendaActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
						remoteViews.setTextViewText(R.id.widget_name, context.getString(R.string.tomorrow_class))
						appWidgetIds.forEach {
							appWidgetManager.updateAppWidget(it, remoteViews)
						}
					}
				}
				else contextUtil.login(TargetUrl.PORTAL) { tomorrowSchedule }
			}
		})
		this.tomorrowSchedule
	}
	
	val tomorrowSchedule: Unit
		get() {
			val tomorrow = LocalDate.now().plusDays(1)
			http.postRequest("https://mportal.sysu.edu.cn/newClient/api/schedule/newSchedule/getNextDaySchedule", "{\"types\":[],\"startTime\":\"$tomorrow\",\"endTime\":\"${tomorrow.plusDays(1)}\"}", 0)
		}
}
