package com.miyuyan.sysuer.browser.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class JsModel(private val repository: BrowserRepository) : ViewModel() {
	fun loadJs(onResult: ((List<JavaScriptEntity>) -> Unit)? = null) {
		viewModelScope.launch {
			onResult?.let { it(repository.getAllJavaScript()) }
		}
	}

	fun addJs(js: JavaScriptEntity, onResult: (Long) -> Unit = {}) {
		viewModelScope.launch {
			val id = repository.insertJs(js) ?: -1L
			if (id != -1L) onResult(id)
		}
	}

	fun deleteJs(js: JavaScriptEntity, onResult: (() -> Unit)? = null) {
		viewModelScope.launch {
			repository.deleteJs(js)
			onResult?.let { it() }
		}
	}

	fun updateJs(js: JavaScriptEntity, onResult: (() -> Unit)? = null) {
		viewModelScope.launch {
			repository.updateJs(js)
			onResult?.let { it() }
		}
	}

	fun deleteJs(jsId: Long, onResult: (() -> Unit)? = null) {
		viewModelScope.launch {
			repository.deleteJS(jsId)
			onResult?.let { it() }
		}
	}

	fun getJs(jsId: Long, onResult: ((JavaScriptEntity?) -> Unit)? = null) {
		viewModelScope.launch {
			onResult?.let { it(repository.getJs(jsId)) }
		}
	}
}
