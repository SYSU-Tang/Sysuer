package com.miyuyan.sysuer.academic

import android.os.Parcelable
import kotlinx.serialization.Serializable
import kotlinx.parcelize.Parcelize

@Parcelize
@Serializable
data class CourseFilterValueData(
	@JvmField var courseName: String? = "",
	@JvmField var studyCampusId: String? = "",
	@JvmField var week: String? = "",
	@JvmField var classTimes: String? = "",
	@JvmField var courseUnitNum: String? = "",
	@JvmField var teachingTeacherNum: String? = "",
	@JvmField var teachingLanguageCode: String? = "",
	@JvmField var specialClassCode: String? = "",
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
	@JvmField var courseName: String? = "",
	@JvmField var studyCampusId: String? = "",
	@JvmField var week: String? = "",
	@JvmField var classTimes: String? = "",
	@JvmField var courseUnitNum: String? = "",
	@JvmField var teachingTeacherNum: String? = "",
	@JvmField var teachingLanguageCode: String? = "",
	@JvmField var specialClassCode: String? = "",
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
