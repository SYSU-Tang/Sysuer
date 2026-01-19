package com.sysu.edu.api;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;

public class CourseSelectionViewModel extends ViewModel {
    public final MutableLiveData<HashMap<String, String>> filterName = new MutableLiveData<>(new HashMap<>());
    public final MutableLiveData<HashMap<String, String>> filterValue = new MutableLiveData<>(new HashMap<>());
    String returnData;

    public HashMap<String, String> getFilterName() {
        return filterName.getValue();
    }

    public void setFilterName(HashMap<String, String> filter) {
        filterName.postValue(filter);
    }

    public HashMap<String, String> getFilterValue() {
        return filterValue.getValue();
    }

    public void setFilterValue(HashMap<String, String> filter) {
        filterValue.postValue(filter);
    }

    public String getReturnData() {
        return returnData == null ? "" : returnData;
    }

    public void setReturnData(String data) {
        returnData = data;
    }

    //public void clearReturnData() {
//        returnData = null;
//    }

}

