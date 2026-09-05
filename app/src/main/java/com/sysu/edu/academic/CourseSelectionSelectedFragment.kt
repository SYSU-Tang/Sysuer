package com.sysu.edu.academic

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import com.sysu.edu.BaseFragment
import com.sysu.edu.R
import com.sysu.edu.academic.CourseSelectionMainFragment.SpacesItemDecoration
import com.sysu.edu.api.CommonUtil.trim
import com.sysu.edu.api.Config
import com.sysu.edu.databinding.FragmentCourseSelectionSelectedBinding
import com.sysu.edu.databinding.ItemActionChipBinding
import com.sysu.edu.databinding.ItemCourseSelectionBinding
import com.sysu.edu.model.JwxtModel
import com.sysu.edu.view.RecyclerAdapter

class CourseSelectionSelectedFragment : BaseFragment() {
	var courseSelectedAdapter: CourseSelectedAdapter? = null
	var page: Int = 1
	var success: Int = 1
	var failure: Int = 1
	var retired: Int = 1
	var waiting: Int = 1
	var total: Int = -1
	var category: String? = null
	var layoutManager: StaggeredGridLayoutManager? = null
	lateinit var model: JwxtModel
	override fun onDestroyView() {
		super.onDestroyView()
		model.dispose()
	}

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		super.onCreateView(inflater, container, savedInstanceState)
		config = Config(this)
		model = JwxtModel(requireContext())
		courseSelectedAdapter = CourseSelectedAdapter().apply {
			setParams(config)

			likeAction = { type: String?, id: String? -> setPNP(type, id!!) }
		}
		layoutManager = StaggeredGridLayoutManager(
			config.column, StaggeredGridLayoutManager.VERTICAL
		)
		val binding =
			FragmentCourseSelectionSelectedBinding.inflate(inflater, container, false).apply {
				list.root.layoutManager = layoutManager
				list.root.adapter = courseSelectedAdapter?.apply {
					selectAction = { position: Int? ->
						val status = get(position!!).getInteger("status")
						val isSelected = status == 3 || status == 4
						Snackbar.make(
							root,
							if (isSelected) R.string.drop_course else R.string.select_course,
							Snackbar.LENGTH_LONG
						).setAction(R.string.confirm) {
							if (isSelected) unselect(
								convert(position, "courseId"),
								convert(position, "teachingClassId"),
								get(position).getString("selectedType")
							)
							else select(
								convert(position, "teachingClassId"),
								convert(position, "selectedType"),
								get(position).getString("courseCateCode")
							)
						}.show()
					}
				}
				list.root.addOnScrollListener(object : RecyclerView.OnScrollListener() {
					override fun onScrolled(v: RecyclerView, dx: Int, dy: Int) {
						if (!v.canScrollVertically(1) && total > (page - 1) * 10 && dy > 0) selectedCourses
						head.elevation =
							(if (v.canScrollVertically(-1)) config.dpToPx(2) else 0).toFloat()
					}
				})
				list.root.addItemDecoration(SpacesItemDecoration(config.dpToPx(8)))
				filter.setOnCheckedStateChangeListener { _: ChipGroup?, checkedId: MutableList<Int>? ->
					this@CourseSelectionSelectedFragment.success =
						if (checkedId!!.contains(R.id.success)) 1 else 0
					this@CourseSelectionSelectedFragment.failure =
						if (checkedId.contains(R.id.failure)) 1 else 0
					this@CourseSelectionSelectedFragment.retired =
						if (checkedId.contains(R.id.retired)) 1 else 0
					this@CourseSelectionSelectedFragment.waiting =
						if (checkedId.contains(R.id.to_filter)) 1 else 0
					regetSelectedCourses()
				}
				category.setOnCheckedStateChangeListener { _: ChipGroup?, checkedId: MutableList<Int>? ->
					val i = checkedId!![0]
					this@CourseSelectionSelectedFragment.category = when (i) {
						R.id.all -> ""
						R.id.public_compulsory -> "10"
						R.id.public_selective -> "30"
						R.id.major_compulsory -> "11"
						R.id.major_selective -> "21"
						R.id.cross_major -> "kzy"
						R.id.honor -> "31"
						else -> ""
					}
				}
				regetSelectedCourses()
			}
		model.message.observe(requireActivity()) { (code, response) ->
			if (response.getIntValue("code") == 200) {
				when (code) {
					0 -> {
						total = response.getJSONObject("data").getInteger("total")
						response.getJSONObject("data").getJSONArray("rows")
							.forEach { o: Any? -> courseSelectedAdapter!!.add(o as JSONObject) }
					}

					1 -> {
						if (response.containsKey("data") && response.getString("data") != null) config.toast(
							response.getString("data")
						)
						regetSelectedCourses()
					}
				}
			}
		}
		selectedCourses
		return binding.root
	}

	fun unselect(classId: String, code: String?, type: String?) {
		model.addAndNext(
			"jwxt/choose-course-front-server/classCourseInfo/course/back",
			"{\"courseId\":\"$classId\",\"clazzId\":\"$code\",\"selectedType\":\"$type\"}",
			1
		)
	}

	fun select(code: String, type: String?, category: String?) {
		model.addAndNext(
			"jwxt/choose-course-front-server/classCourseInfo/course/choose",
			"{\"clazzId\":\"$code\",\"selectedType\":\"$type\",\"selectedCate\":\"$category\",\"check\":true}",
			1
		)
	}

	val selectedCourses: Unit
		get() {
			val args = JSONObject.of(
				"successStatus",
				"$success",
				"failureStatus",
				"$failure",
				"retiredClass",
				"$retired",
				"waitingScreen",
				"$waiting"
			)
			if (!category.isNullOrEmpty()) args["courseCateCode"] = category
			model.addAndNext(
				"jwxt/choose-course-front-server/selectedCourse/list",
				"{\"pageNo\":${page++},\"pageSize\":10,\"total\":true,\"param\":${args.toJSONString()}}",
				0
			)
		}

	fun regetSelectedCourses() {
		page = 1
		total = -1
		courseSelectedAdapter!!.clear()
		selectedCourses
	}

	fun setPNP(type: String?, id: String) {
		model.addAndNext(
			"jwxt/choose-course-front-server/selectedCourse/setTwoTier?type=$type",
			"{\"clazzId\":\"$id\"}",
			1
		)
	}

	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)
		layoutManager!!.setSpanCount(config.column)
	}

	class CourseSelectedAdapter : RecyclerAdapter<JSONObject>() {
		var selectAction: ((Int?) -> Unit)? = null
		var likeAction: ((String?, String?) -> Unit)? = null
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
			val context = parent.context
			val binding = ItemCourseSelectionBinding.inflate(
				LayoutInflater.from(context), parent, false
			)
			(0..3).forEach { _ ->
				val chip = ItemActionChipBinding.inflate(
					LayoutInflater.from(context), binding.courseInfo, false
				).root.apply {
					setOnLongClickListener {
						config?.copy("", "")
						false
					}
					setOnClickListener {
						Snackbar.make(context, this, text, Snackbar.LENGTH_LONG).show()
					}
				}

				binding.courseInfo.addView(chip)
			}
			return object : RecyclerView.ViewHolder(binding.root) {}
		}

		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
			val binding = ItemCourseSelectionBinding.bind(holder.itemView).apply { }
			val context = binding.root.context
			binding.courseName.text = "${convert(position, "courseNum")}-${
				convert(position, "courseName")
			}"
			val item = get(position)
			val status = item.getInteger("status")
			val isSelected = status == 3 || status == 4
			val canPNP = status == 4 && item.getString("isInTwoTierSet") == "1" && listOf<String?>(
				*item.getString(
					"courseCateList"
				).split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
			).contains(
				item.getString("courseCateCode")
			)
			binding.select.setSelected(isSelected)
			val selectBg = if (isSelected) MaterialColors.getColor(
				binding.select, com.google.android.material.R.attr.colorPrimaryContainer
			) else Color.TRANSPARENT
			val selectFg = MaterialColors.getColor(
				binding.select,
				if (isSelected) com.google.android.material.R.attr.colorOnPrimaryContainer else com.google.android.material.R.attr.colorOnSurface
			)
			binding.select.backgroundTintList = ColorStateList.valueOf(selectBg)
			binding.select.setTextColor(ColorStateList.valueOf(selectFg))
			binding.select.setIconTint(ColorStateList.valueOf(selectFg))
			binding.select.text =
				if (binding.select.isSelected) context.getString(R.string.drop_course)
				else context.getString(R.string.select_course)
			binding.select.setOnClickListener {
				if (selectAction != null) selectAction?.invoke(position)
			}
			binding.filtering.text = "${context.getString(R.string.status)}：${
				"\n${context.getString(if (status == 4) R.string.status_selected else if (status == 3) R.string.filtering else if (status == 1) R.string.retired else R.string.unselected)}"
			}"
			binding.open.setOnClickListener { v: View? ->
				context.startActivity(
					Intent(
						context, CourseDetailActivity::class.java
					).putExtra(
						"code", convert(
							position, "courseNum"
						)
					).putExtra("id", convert(position, "courseId"))
						.putExtra("class", convert(position, "clazzNum")),
					ActivityOptionsCompat.makeSceneTransitionAnimation(
						context as Activity, v!!, "miniapp"
					).toBundle()
				)
			}
			binding.head.text =
				convert(position, "teachingTimePlace").replace(";", " | ").replace(",", "\n")
			binding.like.visibility = if (canPNP) View.VISIBLE else View.GONE
			if (canPNP) {
				val isPNP =
					item.getString("isTwoTier") == null || "0" == item.getString("isTwoTier")
				val pnpBg = if (isPNP) Color.TRANSPARENT else MaterialColors.getColor(
					binding.like, com.google.android.material.R.attr.colorPrimaryContainer
				)
				val pnpFg = MaterialColors.getColor(
					binding.like,
					if (isPNP) com.google.android.material.R.attr.colorOnSurface else com.google.android.material.R.attr.colorOnPrimaryContainer
				)
				binding.like.setText(if (isPNP) R.string.set_pnp else R.string.cancel_pnp)
				binding.like.setOnClickListener {
					likeAction?.invoke(if (isPNP) "1" else "0", item.getString("teachingClassId"))
				}
				binding.like.backgroundTintList = ColorStateList.valueOf(pnpBg)
				binding.like.setTextColor(ColorStateList.valueOf(pnpFg))
				binding.like.setIconTint(ColorStateList.valueOf(pnpFg))
			}
			val courseInfoLabels = context.resources.getStringArray(R.array.course_info_labels)
			val infoList =
				context.resources.getStringArray(R.array.seat_info_labels).drop(1).toTypedArray()
			arrayOf(
				"credit", "teachingClassNum", "scheduleExamTime", "examFormName"
			).forEachIndexed { index, value ->
				(binding.courseInfo.getChildAt(index) as Chip).text = "${courseInfoLabels[index]}：${
					convert(position, value)
				}"
			}
			arrayOf("baseReceiveNum", "selectCount").forEachIndexed { index, value ->
				(arrayOf(binding.left, binding.selected)[index]).text = "${infoList[index]}\n${
					convert(position, value)
				}"
			}
		}

		fun convert(position: Int, key: String?): String {
			return trim(data[position].getString(key)).replace("\n\n", "\n")
		}
	}
}
