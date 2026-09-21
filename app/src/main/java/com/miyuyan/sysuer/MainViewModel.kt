package com.miyuyan.sysuer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.api.HttpManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {
	val httpManager = HttpManager()
	private val _update = MutableStateFlow<JSONObject?>(null)
	val update = _update.asStateFlow()

	/**
	 * 获取最新版本信息
	 * @return Result<JSONObject> 成功时包含版本信息，失败时包含异常
	 */
	suspend fun getLatestVersion() = withContext(Dispatchers.IO) {
		httpManager.client.newCall(
				httpManager.generateRequest(
						"https://sysu-tang.github.io/latest.json", null, null
				).build()
		).execute()
	}.use { response ->
		if (response.isSuccessful) {
			_update.value = JSONObject.parseObject(response.body.string())
		}
	}
}