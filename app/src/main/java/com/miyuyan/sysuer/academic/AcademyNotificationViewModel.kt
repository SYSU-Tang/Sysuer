package com.miyuyan.sysuer.academic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.model.JwxtModel

class AcademyNotificationViewModel(application: Application) : AndroidViewModel(application) {
	private val model: JwxtModel = JwxtModel(application)

	private val _academicNotices = MutableLiveData<List<JSONObject>>()
	val academicNotices: LiveData<List<JSONObject>> = _academicNotices

	private val _schoolNotices = MutableLiveData<List<JSONObject>>()
	val schoolNotices: LiveData<List<JSONObject>> = _schoolNotices

	private val _noticeContent = MutableLiveData<String?>()
	val noticeContent: LiveData<String?> = _noticeContent

	init {
		model.message.observeForever { (code, response) ->
			if (response.getInteger("code") == 200) {
				when (code) {
					0 -> {
						val list = response.getJSONObject("data").getJSONArray("list")
						_academicNotices.postValue(list.filterIsInstance<JSONObject>())
					}

					1 -> {
						val list = response.getJSONObject("data").getJSONArray("list")
						_schoolNotices.postValue(list.filterIsInstance<JSONObject>())
					}

					2 -> {
						val data = response.getString("data")
						_noticeContent.postValue(data)
					}
				}
			}
		}
	}

	fun fetchNotices() {
		fetchAcademicNotice()
		fetchSchoolNotice()
	}

	fun fetchAcademicNotice(keyword: String? = null) {
		model.addAndNext(
			"jwxt/system-manage/info-delivery?column=01&deliveryObject=02&status=1&resourceCode=jwgld&title=$keyword",
			0
		)
	}

	fun fetchSchoolNotice(keyword: String? = null) {
		model.addAndNext(
			"jwxt/system-manage/info-delivery?column=02&deliveryObject=02&status=1&resourceCode=jwgld&title=$keyword",
			1
		)
	}

	fun fetchContent(id: String) {
		model.addAndNext("jwxt/system-manage/info-delivery/noticeId?id=$id", 2)
	}

	fun clearNoticeContent() {
		_noticeContent.value = null
	}

	override fun onCleared() {
		model.dispose()
	}
}
