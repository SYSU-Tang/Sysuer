package com.sysu.edu.academic

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LeaveReturnRegistrationViewModel : ViewModel() {
	val year: MutableLiveData<String?> = MutableLiveData<String?>()
}
