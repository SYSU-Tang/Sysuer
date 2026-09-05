package com.miyuyan.sysuer.academic

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize data class CourseFilterValueData(
	var courseName: String? = "",
	var studyCampusId: String? = "",
	var week: String? = "",
	var classTimes: String? = "",
	var courseUnitNum: String? = "",
	var teachingTeacherNum: String? = "",
	var teachingLanguageCode: String? = "",
	var specialClassCode: String? = "",
                                           ) : Parcelable {
	fun set(key: String, value: String?) {
		when (key) {
			"campus" -> studyCampusId = value
			"day" -> week = value
			"section" -> classTimes = value
			"language" -> teachingLanguageCode = value
			"special" -> specialClassCode = value
		}
	}
}

@Parcelize data class CourseFilterNameData(
	var courseName: String? = "",
	var studyCampusId: String? = "",
	var week: String? = "",
	var classTimes: String? = "",
	var courseUnitNum: String? = "",
	var teachingTeacherNum: String? = "",
	var teachingLanguageCode: String? = "",
	var specialClassCode: String? = "",
                                          ) : Parcelable {
	fun set(key: String, value: String?) {
		when (key) {
			"campus" -> studyCampusId = value
			"day" -> week = value
			"section" -> classTimes = value
			"language" -> teachingLanguageCode = value
			"special" -> specialClassCode = value
		}
	}
}
