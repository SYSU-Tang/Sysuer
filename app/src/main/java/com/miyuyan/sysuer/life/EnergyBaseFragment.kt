package com.miyuyan.sysuer.life

import android.util.ArraySet
import androidx.recyclerview.widget.ConcatAdapter
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.BaseFragment
import com.miyuyan.sysuer.api.CommonUtil
import com.miyuyan.sysuer.model.ZhnyModel

abstract class EnergyBaseFragment : BaseFragment() {
	val rooms: ArraySet<CommonUtil.Tuple2<String?, String?>?> =
		ArraySet<CommonUtil.Tuple2<String?, String?>?>()

	abstract val model: ZhnyModel

	fun loadUserInfo() {
		model.addAndNext("kbp/auth/userInfo", 0)
	}

	fun getRoom(username: String?) {
		model.addAndNext(
			"kbp/admin/sys/personRoom/list",
			JSONObject.of("username", username).toJSONString(),
			1
		)
	}

	fun resetAdapter(adapter: ConcatAdapter) {
		adapter.adapters.forEach { adapter.removeAdapter(it) }
	}
}
