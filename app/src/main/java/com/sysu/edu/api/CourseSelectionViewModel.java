package com.sysu.edu.api;

import androidx.lifecycle.ViewModel;

import java.util.HashMap;

public class CourseSelectionViewModel extends ViewModel {
    private String returnData;
    private HashMap<String,String> filter = new HashMap<>();

    public HashMap<String, String> getFilter() {
        return filter;
    }

    public void setFilter(HashMap<String, String> filter) {
        this.filter = filter;
    }

    public String getReturnData() {
        return returnData;
    }

    public void setReturnData(String data) {
        returnData = data;
    }

    public void clearReturnData() {
        returnData = null;
    }

}

