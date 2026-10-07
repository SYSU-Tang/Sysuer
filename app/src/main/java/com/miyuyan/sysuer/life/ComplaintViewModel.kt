package com.miyuyan.sysuer.life

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.FileManager.FileRequestBody
import com.miyuyan.sysuer.model.XinfangModel
import com.miyuyan.sysuer.view.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

class ComplaintViewModel(application: Application) : AndroidViewModel(application) {
	private val model: XinfangModel = XinfangModel(application)
	private val complaint = ComplaintModel(model)

	val host: String
		get() = model.host

	private val _attachments = MutableStateFlow<List<JSONObject>>(emptyList())
	val attachments: StateFlow<List<JSONObject>> = _attachments.asStateFlow()

	private val _squares = MutableStateFlow<List<JSONObject>>(emptyList())
	val squares: StateFlow<List<JSONObject>> = _squares.asStateFlow()

	private val _responses = MutableStateFlow<List<JSONObject>>(emptyList())
	val responses: StateFlow<List<JSONObject>> = _responses.asStateFlow()

	val squareState = model.getUiState(REQUEST_SQUARE)
	val responseState = model.getUiState(REQUEST_RESPONSE)

	init {
		viewModelScope.launch {
			model.message.collect { (code, response) ->
				when (code) {
					REQUEST_UPLOAD -> if (response.getBoolean("ok")) {
						_attachments.value += response.getJSONArray("data").orEmpty()
							.filterIsInstance<JSONObject>()
					}

					REQUEST_SQUARE -> {
						if (response.getBoolean("ok")) {
							_squares.value =
								response.getJSONArray("data").orEmpty().filterIsInstance<JSONObject>()
							squareState.value =
								if (_squares.value.isEmpty()) UiState.Empty else UiState.Content
						} else {
							squareState.value = UiState.Error
							model.contextUtil.toast(response.getString("msg"))
						}
					}

					REQUEST_SUBMIT -> if (response.getBoolean("ok")) {
						model.contextUtil.toast(R.string.submit_successful)
					} else model.contextUtil.toast(
						response.getString("msg") ?: getApplication<Application>().getString(R.string.submit_fail)
					)

					REQUEST_RESPONSE -> {
						if (response.getBoolean("ok")) {
							_responses.value =
								response.getJSONArray("data").orEmpty().filterIsInstance<JSONObject>()
							responseState.value =
								if (_responses.value.isEmpty()) UiState.Empty else UiState.Content
						} else {
							responseState.value = UiState.Error
							model.contextUtil.toast(response.getString("msg"))
						}
					}
				}
			}
		}
		loadSquare()
	}

	fun removeAttachment(position: Int) {
		_attachments.value = _attachments.value.filterIndexed { index, _ -> index != position }
	}

	fun uploadAttachment(file: FileRequestBody) {
		model.enqueue(
			model.http.generateRequest("https://$host/jsp_api/upload", null, null)
				.post(
					MultipartBody.Builder()
						.setType(MultipartBody.FORM)
						.addFormDataPart("file", file.fileName, file.file)
						.build()
				).build(), REQUEST_UPLOAD
		)
	}

	fun submit(formFields: Map<String, String>) {
		complaint.submitForm(
			java.util.HashMap<String?, String?>(formFields),
			attachments = JSONArray(_attachments.value)
		)
	}

	fun sendMobileCode(phone: String?) {
		complaint.sendMobileCode(phone)
	}

	fun loadSquare() {
		squareState.value = UiState.Loading
		model.enqueue("jsp_api/hsgc", "", REQUEST_SQUARE)
	}

	fun queryResponse(phone: String?) {
		responseState.value = UiState.Loading
		model.enqueue("jsp_api/jsjb_list", JSONObject.of("mobile", phone).toJSONString(), REQUEST_RESPONSE)
	}

	override fun onCleared() {
		model.dispose()
	}

	companion object {
		private const val REQUEST_UPLOAD = 0
		private const val REQUEST_SQUARE = 1
		private const val REQUEST_SUBMIT = 2
		private const val REQUEST_RESPONSE = 3
	}
}
