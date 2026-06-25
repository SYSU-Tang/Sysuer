package com.sysu.edu.studentAffair;

import androidx.appcompat.widget.PopupMenu;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.sysu.edu.view.EditTextDialog;

public class StudentPartTimeViewModel extends ViewModel {
    public final MutableLiveData<String> year = new MutableLiveData<>("2026");
    public final MutableLiveData<String> jobType = new MutableLiveData<>("");
    public final MutableLiveData<String> campus = new MutableLiveData<>("");
    public final MutableLiveData<String> yearName = new MutableLiveData<>("2026");
    public final MutableLiveData<String> jobTypeName = new MutableLiveData<>("");
    public final MutableLiveData<String> campusName = new MutableLiveData<>("");
    public final MutableLiveData<String> jobName = new MutableLiveData<>("");
    public final MutableLiveData<String> unitName = new MutableLiveData<>("");
    public PopupMenu yearPop;
    public PopupMenu campusPop;
    public PopupMenu typePop;
    public EditTextDialog jobNameDialog;
    public EditTextDialog unitDialog;
}
