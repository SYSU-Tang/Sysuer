package com.sysu.edu.home;

import android.view.View;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;
import java.util.Map;

public class HomeViewModel extends ViewModel {

    public final MutableLiveData<Boolean> updateDashboardShortcut = new MutableLiveData<>();

    public final Map<Integer, View.OnClickListener> actionMap = new HashMap<>();

}
