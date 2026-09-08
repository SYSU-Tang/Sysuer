package com.miyuyan.sysuer.extra

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.api.HttpManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException

class UpdateModel(application: Application) : AndroidViewModel(application) {
	val httpManager = HttpManager()
	private val _update = MutableStateFlow<JSONObject?>(null)
	val update = _update.asStateFlow()

	suspend fun getLatestVersion() = withContext(Dispatchers.IO) {
		runCatching {
			httpManager.client.newCall(
				httpManager.generateRequest(
					"https://sysu-tang.github.io/latest.json", null, null
				).build()
			).execute().use { response ->
				if (!response.isSuccessful) throw IOException("Unexpected code $response")
				_update.value = JSONObject.parseObject(response.body.string())
			}
		}
	}
}
