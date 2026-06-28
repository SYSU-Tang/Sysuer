package com.sysu.edu.academic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.sysu.edu.BaseFragment
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil.toStringOrDefault
import com.sysu.edu.api.CommonUtil.trim
import com.sysu.edu.databinding.FragmentCourseDetailBinding
import com.sysu.edu.databinding.ItemActionChipBinding
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers

class CourseDetailFragment : BaseFragment() {
	lateinit var binding: FragmentCourseDetailBinding
	var data: JSONObject? = null
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		super.onCreateView(inflater, container, savedInstanceState)
		binding = FragmentCourseDetailBinding.inflate(inflater)
		return binding.getRoot()
	}
	
	override fun setArguments(args: Bundle?) {
		if (args != null) {
			config.contextUtil.disposable.add(Observable.just(args.getString("data") as Any).map {
				JSONObject.parse(it as String?)
			}.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe {
				if (it != null) {
					when (args.getInt("what")) {
						1 -> data = it
						2 -> {
							binding.intro.text = trim(data!!.getString("courseContentInChinese"))
							binding.goal.text = trim(data!!.getString("courseObjectiveAndRequirement"))
							binding.method.text = trim(data!!.getString("teachMethod"))
							binding.evaluationMethod.text = trim(data!!.getString("evaluationMethod"))
							binding.reference.text = trim(data!!.getString("referenceBook"))
							binding.resource.text = trim(data!!.getString("courseResource"))
							val info: Array<String?> = arrayOf("courseName", "faceProfessionName", "courseTypeName", "courseNum", "courseId", "subCourseTypeName", "subTypeModuleName", "courseTextBook", "credit", "totalHours", "lecturesCreHours", "labCreHours", "weekHours", "totalHoursComment", "languageName", "establishUnitNumberName", "planClassSize", "teacherName", "intendedAcadYear", "intendedCampusName")
							for (i in info.indices) {
								val content = toStringOrDefault<String?>((if ((i == 9) or (i == 10)) it else data)!!.getString(info[i]))
								binding.detail.addView(ItemActionChipBinding.inflate(getLayoutInflater())
														   .getRoot()
														   .apply {
															   this.text = "${resources.getStringArray(R.array.course_outline)[i]}：$content"
															   setOnLongClickListener {
																   config.copy("courseId", content)
																   config.toast(R.string.copy_successfully)
																   false
															   }
															   setOnClickListener { a: View? ->
																   Snackbar.make(requireContext(), this, (a as Chip).getText(), Snackbar.LENGTH_LONG)
																	   .show()
															   }
														   })
							}
						}
					}
				}
			})
		}
		super.setArguments(args)
	}
}