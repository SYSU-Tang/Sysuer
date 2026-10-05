package com.miyuyan.sysuer.model

import android.content.Context
import com.miyuyan.sysuer.api.AuthorizationManager
import com.miyuyan.sysuer.api.HttpManager
import com.miyuyan.sysuer.api.TargetUrl

/**
 * 学习平台（LMS，Moodle）模型：基于 Moodle WebService AJAX 接口，
 * 会话凭据为 URL 中的 sesskey，登录失效时由调用方重新登录后再次请求
 */
open class LmsModel(context: Context) : BaseModel(context) {
	override val http: HttpManager = HttpManager().apply {
		initContext(context)
	}

	override val authorizationManager: AuthorizationManager =
		AuthorizationManager("lms.sysu.edu.cn").also {
			it.setTargetUrl(TargetUrl.LMS)
		}

	/** 获取近期日程（作业与截止事件） */
	fun getUpcomingEvents() {
		enqueueUrl(
				"$HOST/lib/ajax/service.php?sesskey=$token&info=core_calendar_get_calendar_upcoming_view",
				"[{\"index\":0,\"methodname\":\"core_calendar_get_calendar_upcoming_view\",\"args\":{\"courseid\":\"1\",\"categoryid\":\"0\"}}]",
				code = EVENTS_REQUEST
		)
	}

	companion object {
		const val EVENTS_REQUEST = 0
		private const val HOST = "https://lms.sysu.edu.cn"
	}
}
