package com.miyuyan.sysuer.studentAffair

import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.miyuyan.sysuer.view.EditTextDialog

class StudentPartTimeViewModel : ViewModel() {
	val year: MutableLiveData<String?> = MutableLiveData("2026")
	val jobType: MutableLiveData<String?> = MutableLiveData("")
	val campus: MutableLiveData<String?> = MutableLiveData("")
	@JvmField val yearName: MutableLiveData<String?> = MutableLiveData("2026")
	@JvmField val jobTypeName: MutableLiveData<String?> = MutableLiveData("")
	@JvmField val campusName: MutableLiveData<String?> = MutableLiveData("")
	@JvmField val jobName: MutableLiveData<String?> = MutableLiveData("")
	@JvmField val unitName: MutableLiveData<String?> = MutableLiveData("")
	@JvmField var yearPop: PopupMenu? = null
	@JvmField var campusPop: PopupMenu? = null
	@JvmField var typePop: PopupMenu? = null
	@JvmField var jobNameDialog: EditTextDialog? = null
	@JvmField var unitDialog: EditTextDialog? = null
}
