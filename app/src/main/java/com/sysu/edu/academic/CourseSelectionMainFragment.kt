package com.sysu.edu.academic

import android.animation.ValueAnimator
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.NavDirections
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialContainerTransform
import com.sysu.edu.BaseFragment
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.CommonUtil.trim
import com.sysu.edu.databinding.DialogServiceOrderBinding
import com.sysu.edu.databinding.FragmentCourseSelectionBinding
import com.sysu.edu.databinding.ItemActionChipBinding
import com.sysu.edu.databinding.ItemCourseSelectionBinding
import com.sysu.edu.model.JwxtModel
import com.sysu.edu.view.PreferenceAdapter
import com.sysu.edu.view.RecyclerAdapter
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import okio.IOException
import java.util.Locale
import java.util.function.Consumer

class CourseSelectionMainFragment : BaseFragment() {
	val type: MutableLiveData<Int?> = MutableLiveData<Int?>()
	val category: MutableLiveData<Int?> = MutableLiveData<Int?>()
	val typeCate: MediatorLiveData<CommonUtil.Tuple2<Int?, Int?>?> = MediatorLiveData<CommonUtil.Tuple2<Int?, Int?>?>(CommonUtil.Tuple2(1, 11)).apply {
		addSource<Int?>(type, Observer {
			typeCate.value = CommonUtil.Tuple2(type.value ?: 1, getCategory())
		})
		addSource<Int?>(category, Observer {
			typeCate.value = CommonUtil.Tuple2(getType(), category.value ?: 11)
		})
	}
	lateinit var binding: FragmentCourseSelectionBinding
	var tmp: Int = 0
	var page: Int = 0
	var adp: CourseAdapter? = null
	var total: Int? = null
	var term: String? = null
	var gm: StaggeredGridLayoutManager? = null
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
		super.onCreateView(inflater, container, savedInstanceState)
		model = JwxtModel(requireContext())
		gm = StaggeredGridLayoutManager(config.column, StaggeredGridLayoutManager.VERTICAL)
		val peDialog = BottomSheetDialog(requireContext())
		val peAdapter = PreferenceAdapter()
		val pe = DialogServiceOrderBinding.inflate(inflater, container, false).apply {
			recyclerView.layoutManager = LinearLayoutManager(requireContext())
			recyclerView.adapter = peAdapter
			ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
				override fun onMove(
					r: RecyclerView,
					s: RecyclerView.ViewHolder,
					t: RecyclerView.ViewHolder,
				                   ): Boolean {
					peAdapter.swap(s.bindingAdapterPosition, t.bindingAdapterPosition)
					return true
				}
				
				override fun onSwiped(vh: RecyclerView.ViewHolder, d: Int) {}
			}).attachToRecyclerView(recyclerView)
			confirm.setOnClickListener {
				val data = JSONArray()
				peAdapter.data.forEachIndexed { i, v ->
					data.add(JSONObject.of("studentFilterID", v.getString("studentFilterID"), "volunteerNum", i + 1))
				}
				sortPE("$data")
			}
		}
		peDialog.setContentView(pe.root)
		val toolbar = requireActivity().findViewById<MaterialToolbar>(R.id.toolbar)
		binding = FragmentCourseSelectionBinding.inflate(inflater, container, false).apply {
			head.type.setOnCheckedStateChangeListener { _: ChipGroup?, _: MutableList<Int?>? ->
				val cid = head.type.checkedChipId
				if (cid == R.id.my_major) selectCategory()
				else {
					type.value = if (cid == R.id.college_public_selective) 4 else 2
					head.peSort.isVisible = false
				}
				if (cid != R.id.my_major && head.category.height != 0) tmp = head.category.height
				val animator = ValueAnimator.ofInt(*if (cid == R.id.my_major) intArrayOf(0, tmp)
				else intArrayOf(if (head.category.height == 0) 0 else tmp, 0))
				animator.addUpdateListener { valueAnimator: ValueAnimator? ->
					head.category.layoutParams = (head.category.layoutParams as LinearLayout.LayoutParams).apply { height = valueAnimator!!.getAnimatedValue() as Int }
				}
				animator.start()
			}
			zoom.setOnClickListener {
				head.root.visibility = if (head.root.isVisible) View.GONE else View.VISIBLE
			}
			head.category.setOnCheckedStateChangeListener { _: ChipGroup?, _: MutableList<Int?>? -> selectCategory() }
			course.layoutManager = gm
			course.addItemDecoration(SpacesItemDecoration(config.dpToPx(8)))
			course.adapter = CourseAdapter().apply {
				selectAction = { position: Int? ->
					if (get(position!!).getInteger("selectedStatus") == 3 || get(position).getInteger("selectedStatus") == 4) unselect(convert(position, "courseId"), convert(position, "teachingClassId"))
					else select(convert(position, "teachingClassId"))
				}
				likeAction = { code: String? -> like(code!!) }
			}.also { adp = it }
			head.filter.setOnCheckedStateChangeListener { _: ChipGroup?, _: MutableList<Int?>? -> regetCourseList() }
			course.addOnScrollListener(object : RecyclerView.OnScrollListener() {
				override fun onScrolled(v: RecyclerView, dx: Int, dy: Int) {
					if (!v.canScrollVertically(1) && total!! > page * 10 && dy > 0) courseList
					head.root.elevation = (if (v.canScrollVertically(-1)) config.dpToPx(2) else 0).toFloat()
				}
			})
			head.peSort.setOnClickListener {
				peDialog.show()
			}
		}
		
		typeCate.observe(viewLifecycleOwner) {
			if (term == null) info
			else regetCourseList()
		}
		model.message.observe(viewLifecycleOwner) { (code, response) ->
			if (response.getInteger("code") == 200) {
				when (code) {
					0 -> {
						val data = response.getJSONObject("data")
						term = data.getString("semesterYear")
						findNavController().currentDestination?.label = data.getString("electiveCourseStageName")
						toolbar.title = data.getString("electiveCourseStageName")
						toolbar.subtitle = "${data.getString("startTime")}~${data.getString("endTime")}"
						courseList
					}
					1 -> response.getJSONObject("data")?.run {
						total = getInteger("total")
						getJSONArray("rows").forEach { e: Any? -> adp!!.add(e as JSONObject) }
					}
					3 -> {
						config.toast(response.getString("data",""))
						regetCourseList()
					}
					4 -> {
						peAdapter.clear()
						if (response.getJSONArray("data").isEmpty()) binding.head.peSort.isVisible = false
						else {
							response.getJSONArray("data").sortedBy { (it as JSONObject).getInteger("volunteerNum") }.forEach { e: Any? ->
								peAdapter.add(JSONObject.of("title", "${(e as JSONObject).getString("courseNum")}-${
									e.getString("courseName")
								}", "content", e.getString("teachingTimePlace"), "icon", R.drawable.menu, "studentFilterID", e.getString("studentFilterID")))
							}
							binding.head.peSort.isVisible = true
						}
					}
				}
			}
		}
		return binding.root
	}
	
	private fun regetCourseList() {
		clear()
		courseList
	}
	
	private fun selectCategory() {
		if (binding.head.category.checkedChipId != R.id.pe) binding.head.peSort.isVisible = false
		when (binding.head.category.checkedChipId) {
			R.id.major_compulsory -> typeCate.value = CommonUtil.Tuple2(1, 11)
			R.id.major_selective -> typeCate.value = CommonUtil.Tuple2(1, 21)
			R.id.school_public_selective -> typeCate.value = CommonUtil.Tuple2(1, 30)
			R.id.pe -> {
				typeCate.value = CommonUtil.Tuple2(3, 10)
				getPE()
			}
			R.id.en -> typeCate.value = CommonUtil.Tuple2(5, 1)
			R.id.public_compulsory -> typeCate.value = CommonUtil.Tuple2(1, 10)
			R.id.honor -> typeCate.value = CommonUtil.Tuple2(1, 31)
		}
	}
	
	var filterName: CourseFilterNameData = CourseFilterNameData()
	var filterValue: CourseFilterValueData = CourseFilterValueData()
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		val navController = findNavController(view)
		navController.currentBackStackEntry?.savedStateHandle?.getLiveData<CourseFilterNameData>("filter_name")?.observe(viewLifecycleOwner) { result ->
			loadFilter(result)
			filterName = result
			navController.currentBackStackEntry?.savedStateHandle?.remove<CourseFilterNameData>("filter_name")
		}
		navController.currentBackStackEntry?.savedStateHandle?.getLiveData<CourseFilterValueData>("filter_value")?.observe(viewLifecycleOwner) { result ->
			filterValue = result
			navController.currentBackStackEntry?.savedStateHandle?.remove<CourseFilterValueData>("filter_value")
		}
		binding.head.addFilter.setOnClickListener { v: View? ->
			val action: NavDirections = CourseSelectionMainFragmentDirections.selectionToFilter().apply {
				setCourseSelectionNameFilter(filterName)
				setCourseSelectionValueFilter(filterValue)
			}
			findNavController(view).navigate(action.actionId, action.arguments, null, FragmentNavigator.Extras.Builder().addSharedElement(v!!, "miniapp").build())
		}
		val transition = MaterialContainerTransform().apply {
			scrimColor = Color.TRANSPARENT
			setAllContainerColors(config.contextUtil.getColorFromAttr(com.google.android.material.R.attr.colorSurface))
		}
		sharedElementEnterTransition = transition
		sharedElementReturnTransition = transition
		super.onViewCreated(view, savedInstanceState)
	}
	
	fun loadFilter(filter: CourseFilterNameData) {
		binding.head.seniorFilter.removeAllViews()
		listOf(filter.courseName, filter.studyCampusId, filter.week, filter.classTimes, filter.courseUnitNum, filter.teachingTeacherNum, filter.teachingLanguageCode, filter.specialClassCode).forEach { v ->
			if (!v.isNullOrEmpty()) {
				val item = ItemActionChipBinding.inflate(layoutInflater, binding.head.filter, false)
				item.root.text = v
				binding.head.seniorFilter.addView(item.root)
			}
		}
	}
	
	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)
		gm?.setSpanCount(config.column)
	}
	
	fun clear() {
		adp?.clear()
		page = 0
		total = -1
	}
	
	val courseList: Unit
		get() {
			if (term != null) getCourseList(getType(), getCategory(), term)
		}
	
	fun getPE() {
		model.addAndNext("jwxt/choose-course-front-server/selectedCourse/sportsSelectedlist", 4)
	}
	
	fun getCourseList(selectedType: Int, selectedCate: Int, term: String?) {
		val data = JSONObject.of("pageNo",
		                         ++page,
		                         "pageSize",
		                         10,
		                         "param",
		                         JSONObject.of("semesterYear",
		                                       term,
		                                       "selectedType",
		                                       selectedType,
		                                       "selectedCate",
		                                       selectedCate,
		                                       "hiddenConflictStatus",
		                                       "0",
		                                       "hiddenSelectedStatus",
		                                       if (binding.head.hideSelected.isChecked) "1" else "0",
		                                       "hiddenEmptyStatus",
		                                       if (binding.head.hideVacancy.isChecked) "1" else "0",
		                                       "vacancySortStatus",
		                                       if (binding.head.vacancy.isChecked) "1" else "0",
		                                       "collectionStatus",
		                                       if (binding.head.onlyCollection.isChecked) "1" else "0"))
		data.getJSONObject("param").putAll(JSONObject.from(filterValue))
		model.addAndNext("jwxt/choose-course-front-server/classCourseInfo/course/list", "$data", 1)
	}
	
	fun like(code: String) {
		model.addAndNext("jwxt/choose-course-front-server/stuCollectedCourse/create", "{\"classesID\":\"$code\",\"selectedType\":\"1\"}", 3)
	}
	
	val info: Unit
		get() {
			model.addAndNext("jwxt/choose-course-front-server/classCourseInfo/selectCourseInfo", 0)
		}
	
	fun select(code: String) {
		model.addAndNext("jwxt/choose-course-front-server/classCourseInfo/course/choose", String.format(Locale.getDefault(), "{\"clazzId\":\"%s\",\"selectedType\":\"%d\",\"selectedCate\":\"%d\",\"check\":true}", code, getType(), getCategory()), 3)
	}
	
	fun getType(): Int = typeCate.value?.first ?: 1
	fun getCategory(): Int = typeCate.value?.second ?: 11
	fun unselect(classId: String, code: String?) {
		model.addAndNext("jwxt/choose-course-front-server/classCourseInfo/course/back", String.format(Locale.getDefault(), "{\"courseId\":\"%s\",\"clazzId\":\"%s\",\"selectedType\":\"%d\"}", classId, code, getType()), 3)
	}
	
	fun sortPE(data: String) {
		model.run("jwxt/choose-course-front-server/selectedCourse/updateSportsSelectedlist", data, null, object : Callback {
			override fun onFailure(call: Call, e: IOException) {
				model.http.handler.post { config.toast(R.string.save_fail) }
			}
			
			override fun onResponse(call: Call, response: Response) {
				if (response.isSuccessful && response.code == 200) model.http.handler.post {
					config.toast(R.string.save_successful)
				}
				else model.login {
					sortPE(data)
				}
			}
		})
	}
	
	internal class SpacesItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {
		override fun getItemOffsets(
			outRect: Rect,
			view: View,
			parent: RecyclerView,
			state: RecyclerView.State,
		                           ) {
			outRect.top = space / 2
			outRect.right = space
			outRect.left = space
			outRect.bottom = space / 2
		}
	}
	
	class CourseAdapter : RecyclerAdapter<JSONObject>() {
		val info: Array<String?> = arrayOf("credit", "clazzNum", "scheduleExamTime", "examFormName", "statusName")
		var selectAction: Consumer<in Int?>? = null
		var likeAction: Consumer<in String?>? = null
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
			val context = parent.context
			val binding = ItemCourseSelectionBinding.inflate(LayoutInflater.from(context), parent, false)
			info.indices.forEach { _ ->
				binding.courseInfo.addView(ItemActionChipBinding.inflate(LayoutInflater.from(context), binding.courseInfo, false).root.apply {
					setOnLongClickListener {
						(context.getSystemService(ClipboardManager::class.java)).setPrimaryClip(ClipData.newPlainText("", text))
						false
					}
					setOnClickListener {
						Snackbar.make(context, this, text, Snackbar.LENGTH_LONG).show()
					}
				})
			}
			
			return object : RecyclerView.ViewHolder(binding.root) {}
		}
		
		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
			val binding = ItemCourseSelectionBinding.bind(holder.itemView)
			val context = binding.root.context
			val item = data[position]
			binding.courseName.text = "${convert(position, "courseNum")}-${
				convert(position, "courseName")
			}"
			val selectedStatus = item.getInteger("selectedStatus")
			item.fluentPut("statusName", context.getString(if (selectedStatus == 4) R.string.status_selected else if (selectedStatus == 3) R.string.filtering else if (selectedStatus == 1) R.string.retired else R.string.unselected))
			val isLike = item.containsKey("collectionStatus") && item.getInteger("collectionStatus") == 1
			binding.like.setSelected(isLike)
			val isSelected = item.containsKey("selectedStatus") && (selectedStatus == 3 || selectedStatus == 4)
			binding.select.setSelected(isSelected)
			binding.select.text = context.getString(if (isSelected) R.string.drop_course else R.string.select_course)
			val selectBg = if (isSelected) MaterialColors.getColor(binding.select, com.google.android.material.R.attr.colorPrimaryContainer) else Color.TRANSPARENT
			val selectFg = MaterialColors.getColor(binding.select, com.google.android.material.R.attr.colorOnPrimaryContainer)
			val likeBg = if (isLike) MaterialColors.getColor(binding.select, com.google.android.material.R.attr.colorPrimaryContainer) else Color.TRANSPARENT
			val likeFg = MaterialColors.getColor(binding.select, com.google.android.material.R.attr.colorOnPrimaryContainer)
			binding.select.backgroundTintList = ColorStateList.valueOf(selectBg)
			binding.select.setTextColor(ColorStateList.valueOf(selectFg))
			binding.select.setIconTint(ColorStateList.valueOf(selectFg))
			binding.like.backgroundTintList = ColorStateList.valueOf(likeBg)
			binding.like.setTextColor(ColorStateList.valueOf(likeFg))
			binding.like.setIconTint(ColorStateList.valueOf(likeFg))
			binding.like.text = context.getString(if (binding.like.isSelected) R.string.unlike else R.string.like)
			binding.select.setOnClickListener {
				Snackbar.make(it, "${binding.select.text}? ${convert(position, "courseName")}", Snackbar.LENGTH_LONG).setAction(context.getString(R.string.confirm)) { _ ->
					selectAction?.accept(position)
				}.show()
			}
			binding.like.setOnClickListener { v: View? ->
				Snackbar.make(v!!, context.getString(R.string.already) + (v as MaterialButton).getText(), Snackbar.LENGTH_LONG).show()
				v.text = context.getString(if (v.isSelected) R.string.unlike else R.string.like)
				likeAction?.accept(convert(position, "teachingClassId"))
				v.setSelected(!v.isSelected)
			}
			binding.open.setOnClickListener { v: View? ->
				context.startActivity(Intent(context, CourseDetailActivity::class.java).putExtra("code", convert(position, "courseNum")).putExtra("id", convert(position, "courseId")).putExtra("class", convert(position, "clazzNum")),
				                      ActivityOptionsCompat.makeSceneTransitionAnimation(context as Activity, v!!, "miniapp").toBundle())
			}
			binding.head.text = convert(position, "teachingTimePlace").replace(";", " | ").replace(",", "\n")
			val courseInfoLabels = context.resources.getStringArray(R.array.course_info_labels)
			val seatInfoLabels = context.resources.getStringArray(R.array.seat_info_labels)
			info.forEachIndexed { i, s ->
				(binding.courseInfo[i] as Chip).text = "${courseInfoLabels[i]}：${
					convert(position, s)
				}"
			}
			arrayOf("baseReceiveNum", "filterSelectedNum", "courseSelectedNum").forEachIndexed { i, s ->
				val button = arrayOf(binding.left, binding.filtering, binding.selected)[i]
				val newText = "${seatInfoLabels[i]}\n${convert(position, s)}"
				if (button.text != newText) {
					button.text = newText
				}
			}
		}
		
		fun convert(position: Int, key: String?): String = trim(get(position).getString(key)).replace("\n\n", "\n")
	}
}