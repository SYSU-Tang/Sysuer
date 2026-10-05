package com.miyuyan.sysuer.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.alibaba.fastjson2.JSONArray
import com.miyuyan.sysuer.api.DataStoreManager
import com.miyuyan.sysuer.model.JwxtModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds

/**
 * 桌面小组件数据更新：经 WebVPN 拉取学期、当前周次与今日课表写入 DataStore 后广播刷新。
 * 请求串行依赖（周次与课表依赖学期），使用 [JwxtModel.executeAndWait] 等待含登录重试在内的
 * 最终结果；瞬时失败交由 WorkManager 退避重试。
 */
class WidgetUpdateWorker(context: Context, workerParams: WorkerParameters) :
	CoroutineWorker(context, workerParams) {

	override suspend fun doWork(): Result {
		val model = JwxtModel(applicationContext)
		return try {
			val networkData = withTimeout(WORK_TIMEOUT_MILLIS.milliseconds) { fetch(model) }
				?: return Result.retry()
			DataStoreManager.getInstance(applicationContext).edit { prefs ->
				prefs[DataStoreManager.TODAY_CLASS] = networkData.toJSONString()
			}
			val widgetName = inputData.getString("component")
			val widgetNames: Array<String?>? = inputData.getNullableStringArray("components")
			if (widgetName != null) updateWidget(widgetName)
			else widgetNames?.forEach {
				updateWidget(it)
			}
			Result.success()
		} catch (_: Exception) {
			Result.retry()
		} finally {
			model.dispose()
		}
	}

	private suspend fun fetch(model: JwxtModel): JSONArray? = coroutineScope {
		val termResponse =
			model.executeAndWait("jwxt/base-info/acadyearterm/showNewAcadlist", code = 0)
				?: return@coroutineScope null
		val term = termResponse.getJSONObject("data")?.getString("acadYearSemester")
		val week = async {
			model.executeAndWait(
					"jwxt/timetable-search/classTableInfo/getDateWeekly?academicYear=$term",
					code = 1
			)
		}
		val courses = async {
			model.executeAndWait(
					"jwxt/timetable-search/classTableInfo/queryTodayStudentClassTable?academicYear=$term",
					code = 2
			)
		}
		val weekResponse = week.await() ?: return@coroutineScope null
		val coursesResponse = courses.await() ?: return@coroutineScope null
		JSONArray.of(termResponse, weekResponse, coursesResponse)
	}

	@Throws(ClassNotFoundException::class)
	private fun updateWidget(name: String?) {
		updateWidget(Class.forName("${applicationContext.packageName}.widget.$name"))
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

	companion object {
		/** 整体超时上限：含登录重试的等待，超时按失败退避重试 */
		private const val WORK_TIMEOUT_MILLIS = 60_000L
	}
}
