package com.sysu.edu.life;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class GymReservationViewModel extends ViewModel {
//    String authorization = "";
    final String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0";
    MutableLiveData<String> authorization = new MutableLiveData<>("");
    String token = "";
}
