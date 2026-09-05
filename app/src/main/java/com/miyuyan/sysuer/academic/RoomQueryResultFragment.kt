package com.miyuyan.sysuer.academic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.CommonUtil
import com.miyuyan.sysuer.api.CommonUtil.extractValue
import com.miyuyan.sysuer.databinding.FragmentCourseQueryResultBinding
import com.miyuyan.sysuer.model.JwxtModel
import com.miyuyan.sysuer.view.StaggerFragment

class RoomQueryResultFragment : StaggerFragment() {
	lateinit var model: JwxtModel
	override fun onDestroyView() {
		super.onDestroyView()
		model.dispose()
	}
	
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	                         ): View {
		val courseQueryResultBinding = FragmentCourseQueryResultBinding.inflate(inflater, container, false).apply {
				root.addView(super.onCreateView(inflater, root, savedInstanceState), -1, -1)
				fab.setOnClickListener {
					export(fab, getString(R.string.course))
				}
			}
		model = JwxtModel(requireContext())
		model.message.observe(requireActivity()) { (_, response) ->
			if (response.getInteger("code") == 200) response.getJSONObject("data").getJSONArray("data").forEach { item: Any? ->
				val values: ArrayList<String?> = extractValue(item as JSONObject, arrayOf("yearTerm", "date", "week", "dayWeek", "campus", "teachingBuild", "teachingBuildNum", "classroomNum", "floor", "classroomID", "seatCount"))
				arrayOf("oneSection",
				        "twoSection",
				        "threeSection",
				        "fourSection",
				        "fiveSection",
				        "sixSection",
				        "sevenSection",
				        "eightSection",
				        "nineSection",
				        "tenSection",
				        "elevenSection"/*, "twelveSection", "thirteenSection", "fourteenSection", "fifteenSection", "sixteenSection"*/).forEach { i ->
					item.getJSONObject(i)?.let {
						values.add(it.getString("occupyReason", "") + "-" + it.getString("occupyUseDepartment", ""))
					}
				}
				addSection("${item.getString("classroomNum")}/${item.getString("date")}",
				           CommonUtil.getString(requireContext(),
				                                intArrayOf(R.string.year_term,
				                                           R.string.date,
				                                           R.string.week_range,
				                                           R.string.week,
				                                           R.string.campus,
				                                           R.string.office,
				                                           R.string.teaching_building_number,
				                                           R.string.classroom_number,
				                                           R.string.floor,
				                                           R.string.classroom_id,
				                                           R.string.seat_count,
				                                           R.string.first_section,
				                                           R.string.second_section,
				                                           R.string.third_section,
				                                           R.string.fourth_section,
				                                           R.string.fifth_section,
				                                           R.string.sixth_section,
				                                           R.string.seventh_section,
				                                           R.string.eighth_section,
				                                           R.string.ninth_section,
				                                           R.string.tenth_section,
				                                           R.string.eleventh_section)/* getString(R.string.twelfth_section), getString(R.string.thirteenth_section), getString(R.string.fourteenth_section), getString(R.string.fifteenth_section), getString(R.string.sixteenth_section)*/),
				           values)
			}
		}
		rooms
		return courseQueryResultBinding.root
	}
	
	val rooms: Unit
		get() {
			model.addAndNext("jwxt/schedule/agg/classroomOccupy/pageCheckList", "{\"pageNo\":1,\"pageSize\":10,\"total\":true,\"param\":${requireArguments().getString("params")}}", 0)
		}
}
