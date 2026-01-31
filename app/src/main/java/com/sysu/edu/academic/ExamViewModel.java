package com.sysu.edu.academic;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.alibaba.fastjson2.JSONObject;

import java.util.ArrayList;

public class ExamViewModel extends ViewModel {
    final MutableLiveData<ArrayList<String>> termList = new MutableLiveData<>();
    final MutableLiveData<String> term = new MutableLiveData<>();
    final MutableLiveData<ArrayList<String>> examWeekList = new MutableLiveData<>();
    final MutableLiveData<ArrayList<JSONObject>> examWeekInfo = new MutableLiveData<>();
    final MutableLiveData<String> examWeek = new MutableLiveData<>();
    final MutableLiveData<String> examResult = new MutableLiveData<>();
    final MutableLiveData<String> examWeekId = new MutableLiveData<>();

    public MutableLiveData<ArrayList<String>> getTermList() {
        return termList;
    }

    public void setTermList(ArrayList<String> terms) {
        termList.setValue(terms);
    }
   /* public void addTerms(String term){
        Objects.requireNonNull(getTermList().getValue()).add(term);
    }*/

    public MutableLiveData<String> getTerm() {
        return term;
    }

    public void setTerm(String term) {
        getTerm().setValue(term);
    }

    public MutableLiveData<ArrayList<String>> getExamWeekList() {
        return examWeekList;
    }

    public void setExamWeekList(ArrayList<String> examWeekList) {
        getExamWeekList().setValue(examWeekList);
    }

    public MutableLiveData<String> getExamWeek() {
        return examWeek;
    }

    public void setExamWeek(String examWeek) {
        getExamWeek().setValue(examWeek);
    }

    public MutableLiveData<String> getExamResult() {
        return examResult;
    }

    public void setExamResult(String examResult) {
        getExamResult().setValue(examResult);
    }

    public MutableLiveData<String> getExamWeekId() {
        return examWeekId;
    }

    public void setExamWeekId(String examWeekId) {
        getExamWeekId().setValue(examWeekId);
    }

    public MutableLiveData<ArrayList<JSONObject>> getExamWeekInfo() {
        return examWeekInfo;
    }

    public void setExamWeekInfo(ArrayList<JSONObject> examWeekInfo) {
        getExamWeekInfo().setValue(examWeekInfo);
    }
}
