package com.sysu.edu.studentAffair

import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sysu.edu.view.EditTextDialog

class StudentPartTimeViewModel : ViewModel() {
	val year: MutableLiveData<String?> = MutableLiveData<String?>("2026")
	val jobType: MutableLiveData<String?> = MutableLiveData<String?>("")
	val campus: MutableLiveData<String?> = MutableLiveData<String?>("")
	@JvmField val yearName: MutableLiveData<String?> = MutableLiveData<String?>("2026")
	@JvmField val jobTypeName: MutableLiveData<String?> = MutableLiveData<String?>("")
	@JvmField val campusName: MutableLiveData<String?> = MutableLiveData<String?>("")
	@JvmField val jobName: MutableLiveData<String?> = MutableLiveData<String?>("")
	@JvmField val unitName: MutableLiveData<String?> = MutableLiveData<String?>("")
	@JvmField var yearPop: PopupMenu? = null
	@JvmField var campusPop: PopupMenu? = null
	@JvmField var typePop: PopupMenu? = null
	@JvmField var jobNameDialog: EditTextDialog? = null
	@JvmField var unitDialog: EditTextDialog? = null
}
