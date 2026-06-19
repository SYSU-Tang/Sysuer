package com.sysu.edu.academic

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CourseSelectionViewModel : ViewModel() {
	@JvmField val filterName: MutableLiveData<HashMap<String?, String?>?> = MutableLiveData<HashMap<String?, String?>?>(HashMap())
	
	@JvmField
	val filterValue: MutableLiveData<HashMap<String?, String?>?> = MutableLiveData<HashMap<String?, String?>?>(HashMap())
	var returnData: String? = null
		get() {
			return if (field == null) "" else field
		}
	
	fun getFilterName(): HashMap<String?, String?>? {
		return filterName.getValue()
	}
	
	fun setFilterName(filter: HashMap<String?, String?>?) {
		filterName.postValue(filter)
	}
	
	fun getFilterValue(): HashMap<String?, String?>? {
		return filterValue.getValue()
	}
	
	fun setFilterValue(filter: HashMap<String?, String?>?) {
		filterValue.postValue(filter)
	} //	fun getReturnData(): String {	//		return (if (returnData == null) "" else returnData)!!
	//	}
	//
	//	fun setReturnData(data: String?) {
	//		returnData = data
	//	}
}