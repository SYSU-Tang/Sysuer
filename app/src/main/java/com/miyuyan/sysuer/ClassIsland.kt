package com.miyuyan.sysuer

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.PendingIntent.getActivity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.drawable.Icon
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_HIGH
import androidx.core.app.NotificationCompat.ProgressStyle
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationManagerCompat.from
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.graphics.drawable.IconCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R.color.md_theme_onPrimaryContainer
import com.miyuyan.sysuer.R.color.md_theme_primary
import com.miyuyan.sysuer.R.color.md_theme_secondary
import com.miyuyan.sysuer.R.drawable.book
import com.miyuyan.sysuer.R.drawable.warning
import com.miyuyan.sysuer.R.string.hour
import com.miyuyan.sysuer.R.string.immediate_class
import com.miyuyan.sysuer.R.string.location
import com.miyuyan.sysuer.R.string.minute
import com.miyuyan.sysuer.R.string.next_class
import com.miyuyan.sysuer.R.string.next_class_time
import com.miyuyan.sysuer.R.string.no_class
import com.miyuyan.sysuer.R.string.no_class_today
import com.miyuyan.sysuer.R.string.remaining_class_time
import com.miyuyan.sysuer.R.string.second
import com.miyuyan.sysuer.R.string.time
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

	enum class CourseState {
		NO_CLASS_TODAY, BEFORE_CLASS, IN_CLASS, BREAK
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
		val remainingSeconds: Long = 0L
	)

	@Volatile
	private var cachedTodayCourses: List<JSONObject> = emptyList()

	@Volatile
	private var cachedRecentCourses: List<JSONObject> = emptyList()

	/**
	 * 将JSONObject转换为IslandCourse
	 * @return IslandCourse
	 * */
	private fun JSONObject.toIslandCourse(): IslandCourse = IslandCourse(
			courseName = getString("courseName", ""),
			teachingPlace = getString("teachingPlace", ""),
			teachingDate = getString("teachingDate", ""),
			startTime = getString("startTime", ""),
			endTime = getString("endTime", ""),
			startClassTimes = getString("startClassTimes", ""),
			endClassTimes = getString("endClassTimes", "")
	)

	/**
	 * 根据今日课表和近日课表计算课程状态
	 * @param todayCourses 今日课程
	 * @param recentCourses 近日课程
	 * */
	fun calculateState(
		todayCourses: List<JSONObject>, recentCourses: List<JSONObject>
	): StateResult {
		val now = LocalDateTime.now()
		if (todayCourses.isEmpty()) {
			val tomorrowFirst = recentCourses.firstOrNull()?.toIslandCourse()
			return StateResult(CourseState.NO_CLASS_TODAY, next = tomorrowFirst)
		}
		val today = todayCourses.map { it.toIslandCourse() }.sortedBy { it.startDateTime }

		val inClass = today.firstOrNull {
			now.isAfter(it.startDateTime) && now.isBefore(it.endDateTime)
		}
		if (inClass != null) {
			val totalMillis =
				Duration.between(inClass.startDateTime, inClass.endDateTime).toMillis()
			val elapsedMillis = Duration.between(inClass.startDateTime, now).toMillis()
//			val progress = if (totalMillis > 0) {
//				((elapsedMillis * 100) / totalMillis).toInt().coerceIn(0, 100)
//			} else 0
			val remaining = Duration.between(now, inClass.endDateTime).toMinutes()
			val remainingSecs = Duration.between(now, inClass.endDateTime).seconds
			return StateResult(
					CourseState.IN_CLASS,
					current = inClass,
					totalMinutes = totalMillis,
					elapsedMinutes = elapsedMillis,
					remainingMinutes = remaining,
					remainingSeconds = remainingSecs
			)
		}

		val nextCourse = today.firstOrNull { it.startDateTime.isAfter(now) }

		if (nextCourse != null) {
			val remainingToNext = Duration.between(now, nextCourse.startDateTime).toMinutes()
			val remainingSecs = Duration.between(now, nextCourse.startDateTime).seconds
			return StateResult(
					if (remainingToNext <= 30) CourseState.BEFORE_CLASS else CourseState.BREAK,
					next = nextCourse,
					remainingMinutes = remainingToNext,
					remainingSeconds = remainingSecs
			)
		}

		val recent = recentCourses.firstOrNull()?.toIslandCourse()
		return StateResult(CourseState.NO_CLASS_TODAY, next = recent)
	}

	/**
	 * 取消通知
	 * @param context 上下文
	 * */
	@JvmStatic
	fun cancelNotification(context: Context) {
		from(context).cancel(NOTIFICATION_ID)
		WorkManager.getInstance(context).cancelUniqueWork(TICK_WORK_NAME)
	}

	/**
	 * 更新课程数据
	 *
	 * @param todayCourses 今日课程
	 * @param recentCourses 近日课程
	 * */
	@JvmStatic
	fun updateCourseData(
		todayCourses: List<JSONObject>, recentCourses: List<JSONObject>
	) {
		cachedTodayCourses = todayCourses
		cachedRecentCourses = recentCourses
	}

	/**
	 * 触发并计划通知
	 *
	 * @param context 上下文
	 * */
	@JvmStatic
	fun triggerAndScheduleTick(context: Context) {
		createNotificationChannel(context)
		val result = calculateState(cachedTodayCourses, cachedRecentCourses)
		val timeString = context.getString(time)
		val locationString = context.getString(location)
		val nextClassString = context.getString(next_class)
		val noClassTodayString = context.getString(no_class_today)
		val minuteString = context.getString(minute)
		val hourString = context.getString(hour)
		val secondString = context.getString(second)
		var unit = TimeUnit.MINUTES
		val contentIntent = getActivity(
				context,
				0,
				Intent(
						context, CourseScheduleActivity::class.java
				).setFlags(FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP),
				FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE
		)
		val builder = NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(
				IconCompat.createWithResource(context, book).setTint(
						context.getColor(md_theme_onPrimaryContainer)
				)
		).setOngoing(true).setRequestPromotedOngoing(true).setPriority(PRIORITY_HIGH)
			.setOnlyAlertOnce(true).setContentIntent(contentIntent).setAutoCancel(false)
		when (result.state) {
			CourseState.NO_CLASS_TODAY -> {
				val next = result.next ?: return
				val content = buildString {
					append("$nextClassString：《${next.courseName}》\n")
					append("$timeString：${next.startTime}~${next.endTime}\n")
					append("$locationString：${next.teachingPlace}\n")
				}
				builder.setContentTitle(noClassTodayString).setContentText(content).setStyle(
						NotificationCompat.BigTextStyle().bigText(
								content
						)
				)
			}

			CourseState.BEFORE_CLASS -> {
				val course = result.next ?: return
				val mins = result.remainingMinutes
				val secs = result.remainingSeconds
				val minsText = when {
					secs in 1..60L -> "$secs$secondString"
					mins <= 30L -> "$mins$minuteString"
					else -> "${mins / 60}$hourString${mins % 60}$minuteString"
				}
				val isDownCount = mins <= 1L && secs >= 0L
				if (isDownCount) unit = TimeUnit.SECONDS
				val content = buildString {
					append(
							if (isDownCount) {
								context.getString(
										next_class_time, course.courseName, secs, secondString
								)
							} else {
								context.getString(
										next_class_time, course.courseName, mins, minuteString
								)
							}
					)
					append("$timeString：${course.startTime}~${course.endTime}\n")
					append("$locationString：${course.teachingPlace}\n")
				}
				builder.setLargeIcon(
						Icon.createWithResource(context, warning).setTint(
								context.getColor(md_theme_secondary)
						)
				).setContentTitle("${course.courseName} · $minsText")
					.setShortCriticalText(context.getString(immediate_class))
					.setContentText(content).setStyle(
							NotificationCompat.BigTextStyle().bigText(
									content
							)
					)
			}

			CourseState.IN_CLASS -> {
				val course = result.current ?: return
				val remainingMins = result.remainingMinutes.toInt()
				val remainingSecs = result.remainingSeconds.toInt()
				val elapsed = result.elapsedMinutes.toInt()
				val isDownCount = remainingMins == 1 && remainingSecs >= 0
				if (isDownCount) unit = TimeUnit.SECONDS
				val remainingDisplay =
					if (isDownCount) "${remainingSecs}$secondString" else "${remainingMins}$minuteString"
				builder.setContentTitle(course.courseName).setShortCriticalText(remainingDisplay)
					.setContentText(
							if (isDownCount) {
								context.getString(
										remaining_class_time,
										course.courseName,
										remainingSecs,
										secondString
								)
							} else {
								context.getString(
										remaining_class_time,
										course.courseName,
										remainingMins,
										minuteString
								)
							}
					).setStyle(
							ProgressStyle().setStyledByProgress(true).setProgress(elapsed)
								.addProgressSegment(
										ProgressStyle.Segment(elapsed).setColor(
												context.getColor(md_theme_primary)
										)
								).addProgressSegment(
										ProgressStyle.Segment(
												remainingMins
										).setColor(
												context.getColor(md_theme_secondary)
										)
								)
					)
			}

			CourseState.BREAK -> {
				val next = result.next ?: return
				val content = buildString {
					append(
							context.getString(
									next_class_time,
									next.courseName,
									result.remainingMinutes,
									minuteString
							)
					)
					append("\n")
					append("$timeString：${next.startTime}~${next.endTime}\n")
					append("$locationString：${next.teachingPlace}\n")
				}
				builder.setContentTitle("${nextClassString}：${next.courseName}")
					.setShortCriticalText(context.getString(no_class)).setContentText(
							content
					).setStyle(
							NotificationCompat.BigTextStyle().bigText(
									content
							)
					)
			}
		}
		if (checkSelfPermission(
					context, POST_NOTIFICATIONS
			) == PERMISSION_GRANTED
		) {
			from(context).notify(NOTIFICATION_ID, builder.build())
		}

		if (result.state != CourseState.NO_CLASS_TODAY) {
			val request = OneTimeWorkRequestBuilder<ClassNotificationWorker>().setInitialDelay(
					1, unit
			).build()
			WorkManager.getInstance(context).enqueueUniqueWork(
					TICK_WORK_NAME, ExistingWorkPolicy.REPLACE, request
			)
		} else {
			cancelNotification(context)
//			WorkManager.getInstance(context).cancelUniqueWork(TICK_WORK_NAME)
		}
	}

	/**
	 * 创建通知渠道
	 *
	 * @param context 上下文
	 *
	 * */
	private fun createNotificationChannel(context: Context) {
		from(context).createNotificationChannel(
				NotificationChannelCompat
					.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
					.setDescription("课程表通知提醒").setName(CHANNEL_NAME).build()
		)
	}
}