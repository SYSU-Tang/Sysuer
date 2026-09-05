package com.miyuyan.sysuer.browser.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class JsModel(private val repository: BrowserRepository) : ViewModel() {
	val js: MutableLiveData<List<JavaScriptEntity>> = MutableLiveData()
	fun loadJs() {
		viewModelScope.launch {
			js.value = repository.getAllJavaScript()
		}
	}
	
	fun loadJs(onResult: (List<JavaScriptEntity>) -> Unit) {
		viewModelScope.launch {
			onResult(repository.getAllJavaScript())
		}
	}
	
	fun addJs(js: JavaScriptEntity, onResult: (Long) -> Unit = {}) {
		viewModelScope.launch {
			val id = repository.insertJs(js) ?: -1L
			println("addJs: $id")
			if (id != -1L) {
				loadJs()
				onResult(id)
			}
		}
	}
	
	fun deleteJs(js: JavaScriptEntity, onResult: () -> Unit = {}) {
		viewModelScope.launch {
			repository.deleteJs(js)
			loadJs()
			onResult()
		}
	}
	
	fun updateJs(js: JavaScriptEntity, onResult: () -> Unit = {}) {
		viewModelScope.launch {
			repository.updateJs(js)
			loadJs()
			onResult()
		}
	}
	
	fun deleteJs(jsId: Long, onResult: () -> Unit = {}) {
		viewModelScope.launch {
			repository.deleteJS(jsId)
			loadJs()
			onResult()
		}
	}
	
	fun getJs(jsId: Long, onResult: (JavaScriptEntity?) -> Unit) {
		viewModelScope.launch {
			onResult(repository.getJs(jsId))
		}
	}
}