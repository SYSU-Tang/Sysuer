package com.miyuyan.sysuer.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.Preferences
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.api.DataStoreManager
import com.miyuyan.sysuer.model.JwxtModel
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.ExperimentalCoroutinesApi

class WidgetUpdateWorker(context: Context, workerParams: WorkerParameters) :
	Worker(context, workerParams) {
	val model: JwxtModel = JwxtModel(applicationContext)

	@OptIn(ExperimentalCoroutinesApi::class)
	override fun doWork(): Result {
		try {
			val networkData = data
			val dataStore = DataStoreManager.getInstance(applicationContext)
			dataStore.updateDataAsync { prefsIn: Preferences ->
				val mutablePreferences = prefsIn.toMutablePreferences()
				if (networkData != null) mutablePreferences[DataStoreManager.TODAY_CLASS] =
					networkData.toJSONString()
				Single.just(mutablePreferences)
			}
			val widgetName = inputData.getString("component")
			val widgetNames: Array<String?>? = inputData.getNullableStringArray("components")
			if (widgetName != null) updateWidget(widgetName)
			else widgetNames?.forEach {
				updateWidget(it)
			}
			return Result.success()
		} catch (_: Exception) {
			return Result.failure()
		}
	}

	@Throws(ClassNotFoundException::class)
	private fun updateWidget(name: String?) {
		updateWidget(Class.forName(applicationContext.packageName + ".widget." + name))
	}

	private fun updateWidget(widgetClass: Class<*>) {
		applicationContext.sendBroadcast(
				Intent(applicationContext, widgetClass)
					.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE).putExtra(
							AppWidgetManager.EXTRA_APPWIDGET_IDS,
							AppWidgetManager.getInstance(applicationContext)
								.getAppWidgetIds(ComponentName(applicationContext, widgetClass))
					)
		)
	}

	private val data: JSONArray?
		get() {
			val r1: Pair<Int, JSONObject>? = term()
			val term: String? = r1?.second?.getJSONObject("data")?.getString("acadYearSemester")
			val r2: Pair<Int, JSONObject>? = getWeek(term)
			val r3: Pair<Int, JSONObject>? = getTodayCourses(term)
			return if (r2 != null && r3 != null) JSONArray.of(
					r1?.second, r2.second, r3.second
			) else null
		}

	private fun term() = model.execute("jwxt/base-info/acadyearterm/showNewAcadlist", code = 0)

	private fun getWeek(term: String?) = model.execute(
			"jwxt/timetable-search/classTableInfo/getDateWeekly?academicYear=$term", code = 1
	)

	private fun getTodayCourses(term: String?) = model.execute(
			"jwxt/timetable-search/classTableInfo/queryTodayStudentClassTable?academicYear=$term",
			code = 2
	)
}

