package com.sysu.edu.life

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sysu.edu.api.AuthorizationManager
import com.sysu.edu.api.CommonUtil
import java.time.LocalDateTime
import java.time.ZoneId

class GymReservationViewModel : ViewModel() {
	@JvmField val ua: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0"
	@JvmField
    val authorizationManager: AuthorizationManager = AuthorizationManager("https://gym.sysu.edu.cn/", "https://gym-443.webvpn.sysu.edu.cn/")
	@JvmField val position: MutableLiveData<Int?> = MutableLiveData<Int?>()
	@JvmField
    val reservationFromTo: MutableLiveData<CommonUtil.Tuple2<Long?, Long?>?> = MutableLiveData<CommonUtil.Tuple2<Long?, Long?>?>(CommonUtil.Tuple2(System.currentTimeMillis(), LocalDateTime.now()
		.plusDays(7)
		.atZone(ZoneId.systemDefault())
		.toInstant()
		.toEpochMilli()))
	@JvmField val selected: MutableLiveData<MutableSet<Int>?> = MutableLiveData()
	@JvmField var from: Long = System.currentTimeMillis()
	@JvmField var to: Long = System.currentTimeMillis()
}
