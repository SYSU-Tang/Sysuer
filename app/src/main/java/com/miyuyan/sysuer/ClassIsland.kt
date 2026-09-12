package com.miyuyan.sysuer

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.academic.CourseScheduleActivity
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

object ClassIsland {
	private const val CHANNEL_ID = "course_schedule_channel"
	private const val CHANNEL_NAME = "课程表通知"
	private const val NOTIFICATION_ID = 1001
	private const val TICK_WORK_NAME = "class_island_tick"

	private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
	private const val LONG_BREAK_THRESHOLD_MIN = 20L

	enum class CourseState {
		NO_CLASS_TODAY, CLASS_FINISHED, BEFORE_CLASS, IN_CLASS, /*SHORT_BREAK, LONG_BREAK,*/ BREAK
	}

	data class IslandCourse(
		val courseName: String,
		val teachingPlace: String,
		val teachingDate: String,
		val startTime: String,
		val endTime: String,
		val startClassTimes: String,
		val endClassTimes: String
	) {
		val startDateTime: LocalDateTime
			get() = LocalDateTime.parse("$teachingDate $startTime", dateTimeFormatter)
		val endDateTime: LocalDateTime
			get() = LocalDateTime.parse("$teachingDate $endTime", dateTimeFormatter)
	}

	data class StateResult(
		val state: CourseState,
		val current: IslandCourse? = null,
		val prev: IslandCourse? = null,
		val next: IslandCourse? = null,
		val totalMinutes: Long = 0L,
		val elapsedMinutes: Long = 0L,
		val remainingMinutes: Long = 0L,
		val progress: Int = if (totalMinutes > 0) {
			((elapsedMinutes * 100) / totalMinutes).toInt().coerceIn(0, 100)
		} else 0
	)

	@Volatile
	private var cachedTodayCourses: List<JSONObject> = emptyList()

	@Volatile
	private var cachedRecentCourses: List<JSONObject> = emptyList()

	private fun JSONObject.toIslandCourse(): IslandCourse = IslandCourse(
		courseName = getString("courseName", ""),
		teachingPlace = getString("teachingPlace", ""),
		teachingDate = getString("teachingDate", ""),
		startTime = getString("startTime", ""),
		endTime = getString("endTime", ""),
		startClassTimes = getString("startClassTimes", ""),
		endClassTimes = getString("endClassTimes", "")
	)

	fun calculateState(
		todayCourses: List<JSONObject>, recentCourses: List<JSONObject>
	): StateResult {
		val now = LocalDateTime.now()
		val today = todayCourses.map { it.toIslandCourse() }.sortedBy { it.startDateTime }

		if (today.isEmpty()) {
			val tomorrowFirst = recentCourses.firstOrNull()?.toIslandCourse()
			return StateResult(CourseState.NO_CLASS_TODAY, next = tomorrowFirst)
		}

		val inClass = today.firstOrNull {
			now.isAfter(it.startDateTime) && now.isBefore(it.endDateTime)
		}
		if (inClass != null) {
			val totalMillis =
				Duration.between(inClass.startDateTime, inClass.endDateTime).toMillis()
			val elapsedMillis = Duration.between(inClass.startDateTime, now).toMillis()
			val progress = if (totalMillis > 0) {
				((elapsedMillis * 100) / totalMillis).toInt().coerceIn(0, 100)
			} else 0
			val remaining = Duration.between(now, inClass.endDateTime).toMinutes()
			return StateResult(
				CourseState.IN_CLASS,
				current = inClass,
				totalMinutes = totalMillis,
				elapsedMinutes = elapsedMillis,
				remainingMinutes = remaining,
				progress = progress,
			)
		}

		val nextCourse = today.firstOrNull { it.startDateTime.isAfter(now) }

		if (nextCourse != null) {
			val remainingToNext = Duration.between(now, nextCourse.startDateTime).toMinutes()
//			val prevCourse = today.lastOrNull { it.endDateTime.isBefore(now) }
//			if (prevCourse != null) {
//				val totalBreak = Duration.between(
//					prevCourse.endDateTime, nextCourse.startDateTime
//				).toMinutes()
//				if (remainingToNext > 15) {
//					val isLong = totalBreak > LONG_BREAK_THRESHOLD_MIN
//					return StateResult(
//						state =
//
//							if (isLong) CourseState.LONG_BREAK else CourseState.SHORT_BREAK,
//						prev = prevCourse,
//						next = nextCourse,
//						totalMinutes = remainingToNext,
//					)
//				}
//			}
			return StateResult(
				if (remainingToNext > 15) CourseState.BEFORE_CLASS else CourseState.BREAK,
				current = nextCourse,
				remainingMinutes = remainingToNext
			)
		}

		val tomorrowFirst = recentCourses.firstOrNull()?.toIslandCourse()
		return StateResult(CourseState.CLASS_FINISHED, next = tomorrowFirst)
	}

	@JvmStatic
	fun sendIslandNotification(context: Context, result: StateResult) {
		createNotificationChannel(context)
		val timeString = context.getString(R.string.time)
		val locationString = context.getString(R.string.location)
		val nextClassString = context.getString(R.string.next_class)
		val noClassTodayString = context.getString(R.string.no_class_today)
		val minuteString = context.getString(R.string.minute)
		val hourString = context.getString(R.string.hour)

		val contentIntent = PendingIntent.getActivity(
			context,
			0,
			Intent(
				context, CourseScheduleActivity::class.java
			).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
			PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		)

		val builder = NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(
			IconCompat.createWithResource(context, R.drawable.book).setTint(
				context.getColor(R.color.md_theme_onPrimaryContainer)
			)
		).setOngoing(true).setRequestPromotedOngoing(true)
			.setPriority(NotificationCompat.PRIORITY_HIGH).setOnlyAlertOnce(true)
			.setContentIntent(contentIntent).setAutoCancel(false)

		when (result.state) {
			CourseState.NO_CLASS_TODAY -> {
				builder.setContentTitle(noClassTodayString)/*.setContentText("好好享受一天吧")*/
					.setStyle(
						NotificationCompat.BigTextStyle().bigText(
							buildString {
								result.next?.let {
									append("$nextClassString：《${it.courseName}》\n")
									append("$timeString：${it.startTime}~${it.endTime}\n")
									append("$locationString：${it.teachingPlace}\n")
								}
							})
					).setProgress(0, 0, false)
			}

			CourseState.CLASS_FINISHED -> {
				builder.setContentTitle(context.getString(R.string.all_course_finished))
					.setContentText(result.next?.let { "$nextClassString：《${it.courseName}》" }
						?: context.getString(R.string.no_next_class)).setStyle(
						NotificationCompat.BigTextStyle().bigText(
							buildString {
								append(context.getString(R.string.all_course_finished))
								append("\n")
								result.next?.let {
									append("$nextClassString：《${it.courseName}》\n")
									append("$timeString：${it.startTime}~${it.endTime}\n")
									append("$locationString：${it.teachingPlace}\n")
								} ?: append(context.getString(R.string.no_next_class))
							})
					).setProgress(0, 0, false)
			}

			CourseState.BEFORE_CLASS -> {
				val course = result.current ?: return
				val mins = result.remainingMinutes
				val minsText = when {
					mins <= 0L -> context.getString(R.string.immediate_class_warning)
					mins <= 15L -> "${mins}$minuteString"
					else -> "${mins / 60}$hourString${mins % 60}$minuteString"
				}
				builder.setLargeIcon(
					Icon.createWithResource(context, R.drawable.warning).setTint(
						context.getColor(R.color.md_theme_secondary)
					)
				).setContentTitle(context.getString(R.string.immediate_class))
					.setShortCriticalText(course.courseName.take(6))
					.setContentText("${course.courseName} · $minsText").setStyle(
						NotificationCompat.BigTextStyle().bigText(
							buildString {
								append(
									context.getString(
										R.string.next_class_time, course.courseName, mins
									)
								)
								append("$timeString：${course.startTime}~${course.endTime}\n")
								append("$locationString：${course.teachingPlace}\n")
							})
					).setProgress(0, 0, false)
			}

			CourseState.IN_CLASS -> {
				val course = result.current ?: return
				val remaining = result.remainingMinutes
				val progress = result.progress.coerceIn(0, 100)
				builder.setContentTitle("📚 ${course.courseName}")
					.setShortCriticalText("${remaining}$minuteString").setContentText(
						context.getString(
							R.string.remaining_class_time, course.courseName, remaining
						)
					).setStyle(
						NotificationCompat.ProgressStyle().setStyledByProgress(true)
							.setProgress(progress).addProgressSegment(
								NotificationCompat.ProgressStyle.Segment(progress).setColor(
									context.getColor(R.color.md_theme_primary)
								)
							).addProgressSegment(
								NotificationCompat.ProgressStyle.Segment(
									(100 - progress).coerceAtLeast(
										1
									)
								).setColor(
									context.getColor(R.color.md_theme_secondary)
								)
							)
					)
			}

			CourseState.BREAK -> {
				val next = result.next!!
				builder.setContentTitle(context.getString(R.string.no_class))
					.setShortCriticalText(next.courseName.take(6)).setContentText(
						context.getString(
							R.string.next_class_time, next.courseName, result.remainingMinutes
						)
					).setStyle(
						NotificationCompat.BigTextStyle().bigText(
							buildString {
								append(
									context.getString(
										R.string.next_class_time,
										next.courseName,
										result.remainingMinutes
									)
								)
								append("$timeString：${next.startTime}~${next.endTime}\n")
								append("$locationString：${next.teachingPlace}\n")
							})
					).setProgress(0, 0, false)
			}
		}

		if (ActivityCompat.checkSelfPermission(
				context, Manifest.permission.POST_NOTIFICATIONS
			) == PackageManager.PERMISSION_GRANTED
		) {
			NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
		}
	}

	@JvmStatic
	fun cancelNotification(context: Context) {
		NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
		WorkManager.getInstance(context).cancelUniqueWork(TICK_WORK_NAME)
	}

	@JvmStatic
	fun updateCourseData(
		todayCourses: List<JSONObject>, tomorrowCourses: List<JSONObject>
	) {
		cachedTodayCourses = todayCourses
		cachedRecentCourses = tomorrowCourses
	}

	@JvmStatic
	fun getCachedTodayCourses(): List<JSONObject> = cachedTodayCourses

	@JvmStatic
	fun getCachedTomorrowCourses(): List<JSONObject> = cachedRecentCourses

	@JvmStatic
	fun triggerAndScheduleTick(context: Context) {
		val result = calculateState(cachedTodayCourses, cachedRecentCourses)
		sendIslandNotification(context, result)

		val needsContinue = when (result.state) {
			CourseState.IN_CLASS, CourseState.BREAK, /*CourseState.SHORT_BREAK, CourseState.LONG_BREAK,*/ CourseState.BEFORE_CLASS -> true
			else -> false
		}

		if (needsContinue) {
			val request = OneTimeWorkRequestBuilder<ClassNotificationWorker>().setInitialDelay(
				1, TimeUnit.MINUTES
			).build()
			WorkManager.getInstance(context).enqueueUniqueWork(
				TICK_WORK_NAME, ExistingWorkPolicy.REPLACE, request
			)
		} else {
			WorkManager.getInstance(context).cancelUniqueWork(TICK_WORK_NAME)
		}
	}

	private fun createNotificationChannel(context: Context) {
		NotificationManagerCompat.from(context).createNotificationChannel(
			NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
				.setDescription("课程表灵动岛提醒").setName(CHANNEL_NAME).build()
		)
	}
}