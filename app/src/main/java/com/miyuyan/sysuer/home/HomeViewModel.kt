package com.miyuyan.sysuer.home

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {
	val updateDashboardShortcut: MutableLiveData<Boolean?> = MutableLiveData<Boolean?>()
	val actionMap: MutableMap<Int?, View.OnClickListener> = mutableMapOf()
}
