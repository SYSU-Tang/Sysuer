package com.miyuyan.sysuer.life

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.miyuyan.sysuer.api.CommonUtil
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDateTime
import java.time.ZoneId

class GymReservationViewModel : ViewModel() {
	@JvmField
	val position: MutableLiveData<Int?> = MutableLiveData<Int?>()

	@JvmField
	val reservationFromTo: MutableLiveData<CommonUtil.Tuple2<Long?, Long?>> =
		MutableLiveData<CommonUtil.Tuple2<Long?, Long?>>(
				CommonUtil.Tuple2(
						System.currentTimeMillis(),
						LocalDateTime.now().plusDays(7).atZone(ZoneId.systemDefault()).toInstant()
							.toEpochMilli()
				)
		)
	@JvmField
	val selected = MutableStateFlow(mutableSetOf<Int>())
	@JvmField
	var from: Long = System.currentTimeMillis()
	@JvmField
	var to: Long = System.currentTimeMillis()
}
