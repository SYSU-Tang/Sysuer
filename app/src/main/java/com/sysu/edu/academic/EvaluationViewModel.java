package com.sysu.edu.academic;

import androidx.lifecycle.ViewModel;

import com.sysu.edu.api.AuthorizationManager;

public class EvaluationViewModel extends ViewModel {
    public final AuthorizationManager authorizationManager = new AuthorizationManager("https://pjxt.sysu.edu.cn/", "https://pjxt-443.webvpn.sysu.edu.cn/");
}
