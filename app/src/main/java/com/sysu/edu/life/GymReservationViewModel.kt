package com.sysu.edu.life;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.sysu.edu.api.AuthorizationManager;
import com.sysu.edu.api.CommonUtil;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;

public class GymReservationViewModel extends ViewModel {
    final String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0";
    final AuthorizationManager authorizationManager = new AuthorizationManager("https://gym.sysu.edu.cn/", "https://gym-443.webvpn.sysu.edu.cn/");
    final MutableLiveData<Integer> position = new MutableLiveData<>();
    final MutableLiveData<CommonUtil.Tuple2<Long, Long>> reservationFromTo = new MutableLiveData<>(new CommonUtil.Tuple2<>(System.currentTimeMillis(), LocalDateTime.now().plusDays(7).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
    final MutableLiveData<HashSet<Integer>> selected = new MutableLiveData<>();
    long from = System.currentTimeMillis();
    long to = System.currentTimeMillis();
}
