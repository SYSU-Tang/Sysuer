package com.sysu.edu.academic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.api.CommonUtil.toStringOrDefault
import com.sysu.edu.databinding.FragmentCourseQueryResultBinding
import com.sysu.edu.model.JwxtModel
import com.sysu.edu.view.StaggeredFragment

class RoomQueryResultFragment : StaggeredFragment() {
	lateinit var model: JwxtModel
	override fun onDestroyView() {
		super.onDestroyView()
		model.dispose()
	}
	
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		val courseQueryResultBinding = FragmentCourseQueryResultBinding.inflate(inflater, container, false)
			.apply {
				root.addView(super.onCreateView(inflater, root, savedInstanceState), -1, -1)
				fab.setOnClickListener { export(fab, getString(R.string.course)) }
			}
		model = JwxtModel(requireContext())
		model.message.observe(requireActivity(), Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			val response = message.second
			if (response.getInteger("code") == 200) response.getJSONObject("data")
				.getJSONArray("data")
				.forEach { e: Any? ->
					val item = e as JSONObject
					val values: ArrayList<String?> = extractValue(item, arrayOf("yearTerm", "date", "week", "dayWeek", "date", "campus", "teachingBuild", "teachingBuildNum", "classroomNum", "floor", "classroomID", "seatCount"))
					for (i in arrayOf("oneSection", "twoSection", "threeSection", "fourSection", "fiveSection", "sixSection", "sevenSection", "eightSection", "nineSection", "tenSection", "elevenSection", "twelveSection", "thirteenSection", "fourteenSection", "fifteenSection", "sixteenSection")) {
						val section = item.getJSONObject(i)
						values.add(toStringOrDefault<String?>(section.getString("occupyReason"), "") + "-" + toStringOrDefault<String?>(section.getString("occupyUseDepartment"), ""))
					}
					add(item.getString("classroomNum"), listOf(getString(R.string.year_term), getString(R.string.date), getString(R.string.week_range), getString(R.string.week), getString(R.string.date), getString(R.string.campus), getString(R.string.office), getString(R.string.teaching_building_number), getString(R.string.classroom_number), getString(R.string.floor), getString(R.string.classroom_id), getString(R.string.seat_count), getString(R.string.first_section), getString(R.string.second_section), getString(R.string.third_section), getString(R.string.fourth_section), getString(R.string.fifth_section), getString(R.string.sixth_section), getString(R.string.seventh_section), getString(R.string.eighth_section), getString(R.string.ninth_section), getString(R.string.tenth_section), getString(R.string.eleventh_section), getString(R.string.twelfth_section), getString(R.string.thirteenth_section), getString(R.string.fourteenth_section), getString(R.string.fifteenth_section), getString(R.string.sixteenth_section)), values)
				}
		})
		rooms
		return courseQueryResultBinding.root
	}
	
	val rooms: Unit
		get() {
			model.addAndNext("jwxt/schedule/agg/classroomOccupy/pageCheckList", "{\"pageNo\":1,\"pageSize\":10,\"total\":true,\"param\":${requireArguments().getString("params")}}", 0)
		}
}
