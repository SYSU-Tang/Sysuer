package com.miyuyan.sysuer.browser.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class JsModelFactory(private val repository: BrowserRepository) : ViewModelProvider.Factory {
	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass.isAssignableFrom(JsModel::class.java)) return JsModel(repository) as T
		throw IllegalArgumentException("Unknown ViewModel class")
	}
}