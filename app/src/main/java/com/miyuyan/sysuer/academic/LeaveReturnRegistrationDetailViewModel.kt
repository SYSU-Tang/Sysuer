package com.miyuyan.sysuer.academic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.model.XgxtModel
import com.miyuyan.sysuer.view.UiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DropdownOption(
	val label: String,
	val value: String,
)

class LeaveReturnRegistrationDetailViewModel(application: Application) :
	AndroidViewModel(application) {
	private val model: XgxtModel = XgxtModel(application)

	private val _detailInfo = MutableStateFlow<JSONObject?>(null)
	val detailInfo: StateFlow<JSONObject?> = _detailInfo.asStateFlow()

	private val _uiState = MutableStateFlow(UiState.Loading)
	val uiState: StateFlow<UiState> = _uiState.asStateFlow()

	// Form State
	private val _isStay = MutableStateFlow("0") // "0" = 离校, "1" = 留校
	val isStay: StateFlow<String> = _isStay.asStateFlow()

	private val _leaveDate = MutableStateFlow("")
	val leaveDate: StateFlow<String> = _leaveDate.asStateFlow()

	private val _returnDate = MutableStateFlow("")
	val returnDate: StateFlow<String> = _returnDate.asStateFlow()

	private val _destinationType = MutableStateFlow("")
	val destinationType: StateFlow<String> = _destinationType.asStateFlow()

	private val _transportation = MutableStateFlow("")
	val transportation: StateFlow<String> = _transportation.asStateFlow()

	private val _country = MutableStateFlow("")
	val country: StateFlow<String> = _country.asStateFlow()

	private val _province = MutableStateFlow("")
	val province: StateFlow<String> = _province.asStateFlow()

	private val _city = MutableStateFlow("")
	val city: StateFlow<String> = _city.asStateFlow()

	private val _stayReason = MutableStateFlow("")
	val stayReason: StateFlow<String> = _stayReason.asStateFlow()
	private val _outDate = MutableStateFlow(false)
	val outDate: StateFlow<Boolean> = _outDate.asStateFlow()

	// Dropdown Options
	private val _transportOptions = MutableStateFlow<List<DropdownOption>>(emptyList())
	val transportOptions: StateFlow<List<DropdownOption>> = _transportOptions.asStateFlow()

	private val _destinationOptions = MutableStateFlow<List<DropdownOption>>(emptyList())
	val destinationOptions: StateFlow<List<DropdownOption>> = _destinationOptions.asStateFlow()

	private val _countryOptions = MutableStateFlow<List<DropdownOption>>(emptyList())
	val countryOptions: StateFlow<List<DropdownOption>> = _countryOptions.asStateFlow()

	private val _provinceOptions = MutableStateFlow<List<DropdownOption>>(emptyList())
	val provinceOptions: StateFlow<List<DropdownOption>> = _provinceOptions.asStateFlow()

	private val _cityOptions = MutableStateFlow<List<DropdownOption>>(emptyList())
	val cityOptions: StateFlow<List<DropdownOption>> = _cityOptions.asStateFlow()

	private val _toastEvent = MutableSharedFlow<String>()
	val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

	private var currentId: String? = null

	init {
		viewModelScope.launch {
			model.message.collect { (code, response) ->
				if (response.getInteger("code") == 200) {
					when (code) {
						0 -> {
							_detailInfo.value = response.getJSONObject("data")?.also { data ->
								val sflx = data.getString("sflx", "0")
								_isStay.value = sflx
								_leaveDate.value = data.getString("yjlxsj", "")
								_returnDate.value = data.getString("yjfxsj", "")
								_destinationType.value = data.getString("qxlx", "")
								_transportation.value = data.getString("jtgj", "")
								val country = data.getString("wcdgj", "")
								val province = data.getString("wcdsf", "")
								val city = data.getString("wcdcs", "")
								_country.value = country
								_province.value = province
								_city.value = city
								_stayReason.value = data.getString("lxyy", "")
								_outDate.value = data.getString("zt", "") == "registered"
								if ("中国" == country) {
									getProvince()
									if (province.isNotEmpty()) {
										getCity(province)
									}
								}
							}
							_uiState.value = UiState.Content
						}

						1 -> {
							_transportOptions.value = parseDropdownOptions(response)
						}

						2 -> {
							_destinationOptions.value = parseDropdownOptions(response)
						}

						3 -> {
							_countryOptions.value = parseDropdownOptions(response)
						}

						4 -> {
							_provinceOptions.value = parseDropdownOptions(response)
						}

						5 -> {
							_cityOptions.value = parseDropdownOptions(response)
						}

						6 -> {
							_toastEvent.tryEmit(response.getString("message", "操作成功"))
							currentId?.let { id -> loadDetail(id) }
						}
					}
				} else {
					_toastEvent.emit(response.getString("message", "出错了"))
				}
			}
		}
	}

	private fun parseDropdownOptions(response: JSONObject): List<DropdownOption> {
		val array = response.getJSONArray("data") ?: return emptyList()
		return array.filterIsInstance<JSONObject>().map { obj ->
			val label = obj.getString("label", "")
			val value = obj.getString("value", "")
			DropdownOption(label, value)
		}
	}

	fun loadDetail(id: String) {
		currentId = id
		_uiState.value = UiState.Loading
		model.enqueue("jjrlfx/api/sm-jjrlfx/student/$id/info", 0)
		model.enqueue("jjrlfx/api/sm-jjrlfx/student/transport", 1)
		model.enqueue("jjrlfx/api/sm-jjrlfx/student/destination-type", 2)
		model.enqueue("jjrlfx/api/sm-jjrlfx/student/country/drop", 3)
	}

	fun getProvince() {
		model.enqueue("jjrlfx/api/sm-jjrlfx/student/province/drop?0=%E4%B8%AD&1=%E5%9B%BD", 4)
	}

	fun getCity(province: String) {
		model.enqueue("jjrlfx/api/sm-jjrlfx/student/city/drop?fdm=$province", 5)
	}

	fun setIsStay(value: String) {
		_isStay.value = value
	}

	fun setLeaveDate(date: String) {
		_leaveDate.value = date
	}

	fun setReturnDate(date: String) {
		_returnDate.value = date
	}

	fun setDestinationType(type: String) {
		_destinationType.value = type
	}

	fun setTransportation(trans: String) {
		_transportation.value = trans
	}

	fun setCountry(c: String) {
		_country.value = c
		if ("中国" == c) {
			getProvince()
		} else {
			_province.value = ""
			_city.value = ""
		}
	}

	fun setProvince(p: String) {
		_province.value = p
		if (p.isNotEmpty()) {
			getCity(p)
		} else {
			_city.value = ""
		}
	}

	fun setCity(c: String) {
		_city.value = c
	}

	fun setStayReason(reason: String) {
		_stayReason.value = reason
	}

	fun save(id: String) {
		viewModelScope.launch {
			if (_isStay.value == "0") {
				val json = JSONObject.of(
						"cjlfxgzId",
						id,
						"sflx",
						"0",
						"yjlxsj",
						_leaveDate.value,
						"yjfxsj",
						_returnDate.value,
						"qxlx",
						_destinationType.value,
						"jtgj",
						_transportation.value,
						"wcd",
						JSONObject.of(
								"gj", _country.value, "sf", _province.value, "cs", _city.value
						),
						"wcdgj",
						_country.value,
						"wcdsf",
						_province.value,
						"wcdcs",
						_city.value
				)
				model.enqueue("jjrlfx/api/sm-jjrlfx/student/register", json.toJSONString(), 6)
			} else {
				val json = JSONObject.of(
						"cjlfxgzId", id, "sflx", "1", "lxyy", _stayReason.value
				)
				model.enqueue("jjrlfx/api/sm-jjrlfx/student/register", json.toJSONString(), 6)
			}
		}
	}

	override fun onCleared() {
		model.dispose()
	}
}
