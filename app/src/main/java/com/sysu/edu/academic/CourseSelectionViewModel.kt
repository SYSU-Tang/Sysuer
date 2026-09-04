package com.sysu.edu.academic

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CourseSelectionViewModel : ViewModel() {
    //@JvmField val filterName: MutableLiveData<MutableMap<String?, String?>?> = MutableLiveData<MutableMap<String?, String?>?>(HashMap())

    @JvmField
    val filterValue: MutableLiveData<MutableMap<String?, String?>?> =
        MutableLiveData<MutableMap<String?, String?>?>(HashMap())
    var returnData: String? = null
        get() {
            return if (field == null) "" else field
        }

    //fun getFilterName(): MutableMap<String?, String?>? {
    //	return filterName.getValue()
    //}
    //
    //fun setFilterName(filter: MutableMap<String?, String?>) {
    //	filterName.postValue(filter)
    //}
    //
    //fun getFilterValue(): MutableMap<String?, String?>? {
    //	return filterValue.getValue()
    //}
    //
    //fun setFilterValue(filter: MutableMap<String?, String?>) {
    //	filterValue.postValue(filter)
    //}
//	fun getReturnData(): String {	//		return (if (returnData == null) "" else returnData)!!
    //	}
    //
    //	fun setReturnData(data: String?) {
    //		returnData = data
    //	}
}