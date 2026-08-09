package com.sysu.edu.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.Preferences
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.DataStoreManager
import com.sysu.edu.model.JwxtModel
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.Request

class WidgetUpdateWorker(context: Context, workerParams: WorkerParameters) :
	Worker(context, workerParams) {
	val model: JwxtWidgetModel = JwxtWidgetModel(applicationContext)
	@OptIn(ExperimentalCoroutinesApi::class) override fun doWork(): Result {
		try {
			val networkData = data
			val dataStore = DataStoreManager.getInstance(applicationContext)
			dataStore.updateDataAsync { prefsIn: Preferences? ->
				val mutablePreferences = prefsIn!!.toMutablePreferences()
				if (networkData != null) mutablePreferences[DataStoreManager.TODAY_CLASS] = networkData.toJSONString()
				Single.just(mutablePreferences)
			}
			val widgetName = inputData.getString("component")
			val widgetNames: Array<String>? = inputData.getStringArray("components")
			if (widgetName != null) updateWidget(widgetName)
			else widgetNames?.forEach {
				updateWidget(it)
			}
			return Result.success()
		} catch (_: Exception) {
			return Result.failure()
		}
	}
	
	@Throws(ClassNotFoundException::class) private fun updateWidget(name: String?) {
		updateWidget(Class.forName(applicationContext.packageName + ".widget." + name))
	}
	
	private fun updateWidget(widgetClass: Class<*>) {
		applicationContext.sendBroadcast(Intent(applicationContext, widgetClass).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
											 .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, AppWidgetManager.getInstance(applicationContext)
												 .getAppWidgetIds(ComponentName(applicationContext, widgetClass))))
	}
	
	private val data: JSONArray?
		get() {
			term
			val r1: CommonUtil.Tuple2<Int, JSONObject>? = model.execute(model.nextRequest!!)
			val term: String? = r1?.second?.getJSONObject("data")?.getString("acadYearSemester")
			getWeek(term)
			val r2: CommonUtil.Tuple2<Int, JSONObject>? = model.execute(model.nextRequest!!)
			getTodayCourses(term)
			val r3: CommonUtil.Tuple2<Int, JSONObject>? = model.execute(model.nextRequest!!)
			return if (r2 != null && r3 != null) JSONArray.of(r1?.second, r2.second, r3.second) else null
		}
	val term: Unit
		get() {
			model.add("jwxt/base-info/acadyearterm/showNewAcadlist", 0)
		}
	
	fun getWeek(term: String?) {
		model.add("jwxt/timetable-search/classTableInfo/getDateWeekly?academicYear=$term", 1)
	}
	
	fun getTodayCourses(term: String?) {
		model.add("jwxt/timetable-search/classTableInfo/queryTodayStudentClassTable?academicYear=$term", 2)
	}
	
	class JwxtWidgetModel(context: Context) : JwxtModel(context) {
		override fun retry(request: CommonUtil.Tuple2<Request, Int>) {
			execute(request)
		}
	}
}

