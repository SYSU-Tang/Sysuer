package com.sysu.edu.academic

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.button.MaterialButton
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.Config
import com.sysu.edu.databinding.ActivityCourseSelectedBinding
import com.sysu.edu.databinding.ItemCourseSelectedBinding
import com.sysu.edu.model.JwxtModel
import com.sysu.edu.view.RecyclerAdapter
import java.util.Locale
import java.util.regex.Pattern

class CourseSelectedActivity : BaseActivity() {
	var page: Int = 0
	lateinit var model: JwxtModel
	override fun onDestroy() {
		super.onDestroy()
		model.dispose()
	}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		model = JwxtModel(this)
		val courseAdapter = CourseSelectedAdapter()
		val binding = ActivityCourseSelectedBinding.inflate(layoutInflater).apply {
			toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
			toolbar.menu
				.add(R.string.export)
				.setIcon(R.drawable.export)
				.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM)
				.setOnMenuItemClickListener {
					startActivity(Intent(this@CourseSelectedActivity, MarkdownViewActivity::class.java).putExtra("content", courseAdapter.toMarkdown())
									  .putExtra("title", getString(R.string.course_selected)), ActivityOptionsCompat.makeSceneTransitionAnimation(this@CourseSelectedActivity, toolbar, "miniapp")
									  .toBundle())
					true
				}
			search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
				override fun onQueryTextSubmit(query: String?): Boolean {
					return true
				}
				
				override fun onQueryTextChange(newText: String?): Boolean {
					page = 0
					courseAdapter.clear()
					getSelectedCourses(newText)
					return true
				}
			})
			list.setLayoutManager(StaggeredGridLayoutManager(config.column, StaggeredGridLayoutManager.VERTICAL))
			list.setAdapter(courseAdapter)
		}
		setContentView(binding.root)
		model.message.observe(this, Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			val response = message.second
			if (response.getInteger("code") == 200) {
				val data = response.getJSONObject("data")
				data.getJSONArray("rows")
					.forEach { o: Any? -> courseAdapter.add(o as JSONObject?) }
				if (data.getInteger("total") > page * 10) getSelectedCourses(binding.search.query.toString())
			}
		})
		getSelectedCourses("")
	}
	
	fun getSelectedCourses(courseName: String?) {
		model.addAndNext("jwxt/choose-course-front-server/selectedCourse/list", String.format(Locale.getDefault(), "{\"pageNo\":%d,\"pageSize\":10,\"total\":true,\"param\":{\"courseName\":\"%s\",\"successStatus\":\"1\",\"failureStatus\":\"0\",\"retiredClass\":\"0\",\"waitingScreen\":\"0\"}}", ++page, courseName), 1)
	}
	
	class CourseSelectedAdapter : RecyclerAdapter<JSONObject?>() {
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
			return ViewHolder(ItemCourseSelectedBinding.inflate(LayoutInflater.from(parent.context), parent, false)).apply<ViewHolder> { setInfo(data[viewType]) }
		}
		
		fun toMarkdown(): String {
			val key: Array<String?> = arrayOf("courseName", "courseCategoryName", "courseUnitName", "scheduleExamTime", "examFormName", "credit", "teachingClassId", "teachingClassNum", "teachingClassName", "courseNum")
			val md = StringBuilder().append("| 课程名称 | 课程类别 | 开设学院 | 考试时间 | 考核方式 | 学分 | 班级ID | 班级号 | 班级名 | 课程号 |\n")
			.append("| -------- | -------- | -------- | -------- | -------- | -------- | -------- | -------- | -------- | -------- |\n")
			data.forEach { item: JSONObject? ->
				for (s in key) md.append(if (item!!.getString(s) == null) "无" else item.getString(s))
					.append(" | ")
				md.append("\n")
			}
			return "$md"
		}
		
		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
			(holder as ViewHolder).setInfo(data[position])
			holder.binding.root.setOnClickListener { view: View? ->
				view!!.context.startActivity(Intent(view.context, CourseDetailActivity::class.java).putExtra("id", data[position]!!.getString("teachingClassId"))
												 .putExtra("code", data[position]!!.getString("courseNum"))
												 .putExtra("class", data[position]!!.getString("teachingClassNum")), ActivityOptionsCompat.makeSceneTransitionAnimation((view.context as android.app.Activity?)!!, holder.binding.title, "miniapp")
												 .toBundle())
			}
			super.onBindViewHolder(holder, position)
		}
		
		override fun getItemViewType(position: Int): Int {
			return position
		}
		
		internal class ViewHolder(val binding: ItemCourseSelectedBinding) :
			RecyclerView.ViewHolder(binding.root) {
			val ids: MutableList<Int?> = mutableListOf()
			val info: MutableLiveData<JSONObject?> = MutableLiveData<JSONObject?>()
			
			init {
				info.observe((binding.root.context as androidx.fragment.app.FragmentActivity?)!!, Observer { info: JSONObject? -> this.loadInfo(info!!) })
			}
			
			fun setInfo(info: JSONObject?) {
				this.info.postValue(info)
			}
			
			fun loadInfo(info: JSONObject) {
				val key: Array<String?> = arrayOf("courseName", "courseCategoryName", "courseUnitName", "scheduleExamTime", "examFormName", "credit", "teachingClassId", "teachingClassNum", "teachingClassName", "courseNum")
				val name: Array<String?> = arrayOf("课程名称", "课程类别", "开设学院", "考试时间", "考核方式", "学分", "班级ID", "班级号", "班级名", "课程号")
				ids.forEach { e: Int? -> binding.group.removeView(binding.group.findViewById(e!!)) }
				ids.clear()
				binding.title.text = info.getString("courseName")
				val teachingTimePlace = info.getString("teachingTimePlace")
				if (teachingTimePlace == null || teachingTimePlace.isEmpty()) ids.add(addItem(binding.root.context.getString(R.string.none), "课程安排")) else {
					Pattern.compile(",")
						.splitAsStream(teachingTimePlace)
						.forEach { s: String? -> ids.add(addItem(s!!.replace(";", "/"), "课程安排")) }
				}
				key.indices.forEach {
					ids.add(addItem(if (info.getString(key[it]) == null) binding.root.context.getString(R.string.none) else info.getString(key[it]), name[it]!!))
				}
				binding.courseInfo.setReferencedIds(ids.stream()
														.mapToInt { obj: Int? -> obj!! }
														.toArray())
			}
			
			fun addItem(value: String?, name: String): Int {
				val viewId = View.generateViewId()
				val config = Config((binding.root.context as androidx.fragment.app.FragmentActivity?)!!)
				binding.group.addView(MaterialButton(binding.root.context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
					setTextAppearance(binding.root.context, com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
					layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT)
					setOnClickListener { config.copy(name, value) }
					text = "$name: $value"
					cornerRadius = config.dpToPx(8)
					setPadding(config.dpToPx(8), config.dpToPx(6), config.dpToPx(8), config.dpToPx(6))
					gravity =Gravity.CENTER
					id = viewId
				})
				return viewId
			}
		}
	}
}