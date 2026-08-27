package com.sysu.edu.nav

import android.app.Activity
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

fun MutableList<NavKey>.navigateBack(onFinish: (() -> Unit)? = null) {
	if (size > 1) removeLastOrNull()
	else onFinish?.invoke()
}

fun MutableList<NavKey>.navigateBack(activity: Activity? = null) {
	navigateBack {
		activity?.finishAfterTransition()
	}
}

fun navKeyOf(name: String): NavKey? = when (name) {
	"Home" -> Home
	"CourseSelected" -> CourseSelected
	"SchoolEnrollment" -> SchoolEnrollment
	"CET" -> CET
	"Registration" -> Registration
	"SchoolWorkWarning" -> SchoolWorkWarning
	"CourseCompletion" -> CourseCompletion
	"LeaveReturnRegistration" -> LeaveReturnRegistration
	"PhysicalFitnessTestResult" -> PhysicalFitnessTestResult
	"Dorm" -> Dorm
	"PersonalInformation" -> PersonalInformation
	"StudentPartTime" -> StudentPartTime
	"Todo" -> Todo
	"Agenda" -> Agenda
	"Homework" -> Homework
	"News" -> News
	"AcademyNotification" -> AcademyNotification
	"Evaluation" -> Evaluation
	"CourseSelection" -> CourseSelection
	"CourseSchedule" -> CourseSchedule
	"Exam" -> Exam
	"Calendar" -> Calendar
	"ClassroomQuery" -> ClassroomQuery
	"Grade" -> Grade
	"CourseQuery" -> CourseQuery
	"PersonalTrainingProgram" -> PersonalTrainingProgram
	"TrainingProgram" -> TrainingProgram
	"MajorInfo" -> MajorInfo
	"AssistantInfo" -> AssistantInfo
	"GradeForLevel" -> GradeForLevel
	"RoomQuery" -> RoomQuery
	"AssistantEvaluation" -> AssistantEvaluation
	"LeaveSlip" -> LeaveSlip
	"RainClassMain" -> RainClassMain
	"SchoolBus" -> SchoolBus
	"EnergyFee" -> EnergyFee
	"Pay" -> Pay
	"GymReservation" -> GymReservation
	"NetPay" -> NetPay
	"Complaint" -> Complaint
	else -> null
}

@Serializable data object Home : NavKey
@Serializable data object CourseSelected : NavKey
@Serializable data class CourseDetail(val courseId: String = "", val courseNum: String = "") : NavKey
@Serializable data class WebPage(val url: String, val title: String = "") : NavKey
@Serializable data class RichText(val title: String = "预览", val content: String? = "", val contentType: String? = "") : NavKey
@Serializable data object SchoolEnrollment : NavKey
@Serializable data object CET : NavKey
@Serializable data object Registration : NavKey
@Serializable data object SchoolWorkWarning : NavKey
@Serializable data object CourseCompletion : NavKey
@Serializable data object LeaveReturnRegistration : NavKey
@Serializable data object PhysicalFitnessTestResult : NavKey
@Serializable data object Dorm : NavKey
@Serializable data object PersonalInformation : NavKey
@Serializable data object StudentPartTime : NavKey
@Serializable data object Todo : NavKey
@Serializable data object Agenda : NavKey
@Serializable data object Homework : NavKey
@Serializable data object News : NavKey
@Serializable data object AcademyNotification : NavKey
@Serializable data object Evaluation : NavKey
@Serializable data object CourseSelection : NavKey
@Serializable data object CourseSchedule : NavKey
@Serializable data object Exam : NavKey
@Serializable data object Calendar : NavKey
@Serializable data object ClassroomQuery : NavKey
@Serializable data object Grade : NavKey
@Serializable data object CourseQuery : NavKey
@Serializable data object PersonalTrainingProgram : NavKey
@Serializable data object TrainingProgram : NavKey
@Serializable data object MajorInfo : NavKey
@Serializable data object AssistantInfo : NavKey
@Serializable data object GradeForLevel : NavKey
@Serializable data object RoomQuery : NavKey
@Serializable data object AssistantEvaluation : NavKey
@Serializable data object LeaveSlip : NavKey
@Serializable data object RainClassMain : NavKey
@Serializable data object SchoolBus : NavKey
@Serializable data object EnergyFee : NavKey
@Serializable data object Pay : NavKey
@Serializable data object GymReservation : NavKey
@Serializable data object NetPay : NavKey
@Serializable data object Complaint : NavKey