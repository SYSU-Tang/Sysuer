package com.sysu.edu.academic

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavDirections
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.FragmentNavigator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.button.MaterialButtonToggleGroup
import com.sysu.edu.BaseFragment
import com.sysu.edu.R
import com.sysu.edu.databinding.FragmentCourseSelectionPreviewBinding
import com.sysu.edu.databinding.ItemActionChipBinding
import com.sysu.edu.databinding.ItemEvaluationBinding
import com.sysu.edu.model.JwxtModel
import com.sysu.edu.view.RecyclerAdapter
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.MarkwonVisitor.BlockHandler
import io.noties.markwon.ext.tables.TablePlugin
import org.commonmark.node.Node

class CourseSelectionPreviewFragment : BaseFragment() {
	val type: MutableLiveData<Int?> = MutableLiveData<Int?>(1)
	lateinit var binding: FragmentCourseSelectionPreviewBinding
	var page: Int = 1
	var total: Int = -1
	val previewAdapter: CourseSelectionPreviewAdapter = CourseSelectionPreviewAdapter()
	lateinit var model: JwxtModel
	var filterName: CourseFilterNameData = CourseFilterNameData()
	var filterValue: CourseFilterValueData = CourseFilterValueData()
	override fun onDestroyView() {
		super.onDestroyView()
		model.dispose()
	}
	
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): ConstraintLayout {
		super.onCreateView(inflater, container, savedInstanceState)
		model = JwxtModel(requireContext())
		binding = FragmentCourseSelectionPreviewBinding.inflate(inflater, container, false).apply {
			list.recyclerView.layoutManager = StaggeredGridLayoutManager(config.column, StaggeredGridLayoutManager.VERTICAL)
			list.recyclerView.adapter = previewAdapter
			list.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
				override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
					if (!recyclerView.canScrollVertically(1) && dy > 0 && total > (page - 1) * 10) list
				}
			})
			type.addOnButtonCheckedListener { _: MaterialButtonToggleGroup?, _: Int, _: Boolean -> this@CourseSelectionPreviewFragment.type.value = if (major.isChecked) 1 else if (collegePublicSelective.isChecked) 4 else 2 }
			addFilter.setOnClickListener {
				val action: NavDirections = CourseSelectionPreviewFragmentDirections.previewToFilter()
					.apply {
						setCourseSelectionNameFilter(filterName)
						setCourseSelectionValueFilter(filterValue)
					}
				findNavController(root).navigate(action.actionId, action.arguments, null, FragmentNavigator.Extras.Builder()
					.addSharedElement(addFilter, "miniapp")
					.build())
			}
		}
		model.message.observe(requireActivity()) { (code, response) ->
			if (response.getInteger("code") == 200) {
				if (code == 0) {
					val data = response.getJSONObject("data")
					total = data.getInteger("total")
					data.getJSONArray("rows")
						.forEach { e: Any? -> previewAdapter.add(e as JSONObject?) }
				}
			}
		}
		type.observe(viewLifecycleOwner) { regetList() }
		return binding.root
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val navController = findNavController(view)
		navController.currentBackStackEntry?.savedStateHandle?.getLiveData<CourseFilterNameData>("filter_name")
			?.observe(viewLifecycleOwner) { result ->
				binding.seniorFilter.removeAllViews()
				listOf(result.courseName, result.studyCampusId, result.week, result.classTimes, result.courseUnitNum, result.teachingTeacherNum, result.teachingLanguageCode, result.specialClassCode).forEach { v ->
					if (!v.isNullOrEmpty()) {
						val item = ItemActionChipBinding.inflate(layoutInflater, binding.seniorFilter, false)
						item.root.text = v
						binding.seniorFilter.addView(item.root)
					}
				}
				filterName = result
				navController.currentBackStackEntry?.savedStateHandle?.remove<CourseFilterNameData>("filter_name")
			}
		navController.currentBackStackEntry?.savedStateHandle?.getLiveData<CourseFilterValueData>("filter_value")
			?.observe(viewLifecycleOwner) { result ->
				filterValue = result
				navController.currentBackStackEntry?.savedStateHandle?.remove<CourseFilterValueData>("filter_value")
			}
	}
	
	private fun regetList() {
		page = 1
		total = -1
		previewAdapter.clear()
		list
	}
	
	val list: Unit
		get() {
			val data = JSONObject.of("pageNo", page++, "pageSize", 10, "param", JSONObject.of("hiddenSelectedStatus", "0", "type", type.value
				?: 1))
			data.getJSONObject("param").putAll(JSONObject.from(filterValue))
			model.addAndNext("jwxt/choose-course-front-server/schoolCourse/pageList", "$data", 0)
		}
	
	class CourseSelectionPreviewAdapter : RecyclerAdapter<JSONObject?>() {
		val key: Array<String> = arrayOf("courseName", "courseCategoryName", "courseUnitName", "scheduleExamTime", "examFormName", "credit", "teachingClassId", "teachingClassNum", "teachingClassName", "courseNum")
		val name: Array<String> = arrayOf("课程名称", "课程类别", "开设学院", "考试时间", "考核方式", "学分", "教学班ID", "教学班号", "教学班名", "课程号")
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
			return object :
				RecyclerView.ViewHolder(ItemEvaluationBinding.inflate(LayoutInflater.from(parent.context), parent, false).root) {}
		}
		
		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
			val binding = ItemEvaluationBinding.bind(holder.itemView)
			val item = get(position)
			val context = binding.root.context
			binding.title.text = item!!.getString("courseName")
			val md = StringBuilder("|老师|时间|地点|\n|:-----:|:----:|:----:|\n|" + item.getString("teachingTimePlace")
				.replace(";", " | ")
				.replace(",", " |\n| ") + "|\n")
			key.forEachIndexed { i, v ->
				md.append("\n${name[i]}：**${
					item.getString(v) ?: context.getString(R.string.none)
				}**\n")
			}
			val action = View.OnClickListener { view: View? ->
				view!!.context.startActivity(Intent(view.context, CourseDetailActivity::class.java).putExtra("id", item.getString("teachingClassId"))
												 .putExtra("code", item.getString("courseNum"))
												 .putExtra("class", item.getString("teachingClassNum")), ActivityOptionsCompat.makeSceneTransitionAnimation((binding.root.context as Activity?)!!, binding.title, "miniapp")
												 .toBundle())
			}
			binding.root.setOnClickListener(action)
			binding.open.setOnClickListener(action)
			Markwon.builder(binding.root.context)
				.usePlugins(listOf(object : AbstractMarkwonPlugin() {
					override fun configureVisitor(builder: MarkwonVisitor.Builder) {
						super.configureVisitor(builder)
						builder.blockHandler(object : BlockHandler {
							override fun blockStart(visitor: MarkwonVisitor, node: Node) {
							}
							
							override fun blockEnd(visitor: MarkwonVisitor, node: Node) {
								if (visitor.hasNext(node)) visitor.ensureNewLine()
							}
						})
					}
				}, TablePlugin.create(binding.root.context)))
				.build()
				.setMarkdown(binding.startTime, "$md")
		}
	}
}
