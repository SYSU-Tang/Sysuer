package com.sysu.edu.academic

import android.animation.ValueAnimator
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.FragmentNavigator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialContainerTransform
import com.sysu.edu.BaseFragment
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.CommonUtil.bool2int
import com.sysu.edu.api.CommonUtil.toIntegerOrDefault
import com.sysu.edu.api.CommonUtil.toStringOrDefault
import com.sysu.edu.api.CommonUtil.trim
import com.sysu.edu.databinding.FragmentCourseSelectionBinding
import com.sysu.edu.databinding.ItemActionChipBinding
import com.sysu.edu.databinding.ItemCourseSelectionBinding
import com.sysu.edu.model.JwxtModel
import com.sysu.edu.view.RecyclerAdapter
import java.util.Locale
import java.util.function.Consumer

class CourseSelectionMainFragment : BaseFragment() {
	val filter: MutableLiveData<String?> = MutableLiveData<String?>()
	val type: MutableLiveData<Int?> = MutableLiveData<Int?>(1)
	val category: MutableLiveData<Int?> = MutableLiveData<Int?>(11)
	val typeCate: MediatorLiveData<CommonUtil.Tuple2<Int?, Int?>?> = MediatorLiveData<CommonUtil.Tuple2<Int?, Int?>?>()
	lateinit var binding: FragmentCourseSelectionBinding
	var tmp: Int = 0
	var page: Int = 1
	var adp: CourseAdapter? = null
	var total: Int? = null
	var term: String? = null
	lateinit var vm: CourseSelectionViewModel
	var gm: GridLayoutManager? = null
	lateinit var model: JwxtModel
	override fun onDestroyView() {
		super.onDestroyView()
		model.dispose()
	}
	
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		super.onCreateView(inflater, container, savedInstanceState)
		binding = FragmentCourseSelectionBinding.inflate(inflater, container, false)
		model = JwxtModel(requireContext())
		vm = ViewModelProvider(requireActivity())[CourseSelectionViewModel::class.java]
		vm.filterValue.observe(requireActivity(), Observer { _: MutableMap<String?, String?>? ->
			filter.value = vm.returnData
			binding.head.seniorFilter.removeAllViews()
			vm.getFilterName()?.forEach { (_: String?, v: String?) ->
				if (!v.isNullOrEmpty()) {
					val item = ItemActionChipBinding.inflate(inflater, binding.head.filter, false)
					item.root.text = v
					binding.head.seniorFilter.addView(item.root)
				}
			}
			regetCourseList()
		})
		binding.head.type.setOnCheckedStateChangeListener { chipGroup: ChipGroup?, _: MutableList<Int?>? ->
			val cid = chipGroup!!.checkedChipId
			if (cid == R.id.my_major) selectCategory()
			else type.value = if (cid == R.id.college_public_selective) 4 else 2
			if (cid != R.id.my_major && binding.head.category.height != 0) tmp = binding.head.category.height
			val animator = ValueAnimator.ofInt(*if (chipGroup.checkedChipId == R.id.my_major) intArrayOf(0, tmp) else intArrayOf(if (binding.head.category.height == 0) 0 else tmp, 0))
			animator.addUpdateListener { valueAnimator: ValueAnimator? ->
				binding.head.category.layoutParams = (binding.head.category.layoutParams as LinearLayout.LayoutParams).apply { height = valueAnimator!!.getAnimatedValue() as Int }
			}
			animator.start()
		}
		binding.zoom.setOnClickListener { _: View? ->
			binding.head.root.visibility = if (binding.head.root.isVisible) View.GONE else View.VISIBLE
		}
		typeCate.addSource<Int?>(type, Observer { s: Int? -> typeCate.value = CommonUtil.Tuple2(toIntegerOrDefault(type.getValue(), 1), s) })
		typeCate.addSource<Int?>(category, Observer { s: Int? -> typeCate.value = CommonUtil.Tuple2(toIntegerOrDefault(category.getValue(), 11), s) })
		typeCate.observe(requireActivity(), Observer { _: CommonUtil.Tuple2<Int?, Int?>? -> regetCourseList() })
		binding.head.category.setOnCheckedStateChangeListener { _: ChipGroup?, _: MutableList<Int?>? -> selectCategory() }
		binding.course.layoutManager = GridLayoutManager(requireContext(), config.column)
		binding.course.addItemDecoration(SpacesItemDecoration(config.dpToPx(8)))
		binding.course.adapter = CourseAdapter().also { adp = it }
		binding.head.filter.setOnCheckedStateChangeListener { _: ChipGroup?, _: MutableList<Int?>? -> regetCourseList() }
		adp!!.selectAction = { position: Int? ->
			if (adp!!.get(position!!).getInteger("selectedStatus") == 3 || adp!!.get(position)
					.getInteger("selectedStatus") == 4) unselect(adp!!.convert(position, "courseId"), adp!!.convert(position, "teachingClassId"))
			else select(adp!!.convert(position, "teachingClassId"))
		}
		adp!!.likeAction =  { code: String? -> like(code!!) }
		binding.course.addOnScrollListener(object : RecyclerView.OnScrollListener() {
			override fun onScrolled(v: RecyclerView, dx: Int, dy: Int) {
				if (!v.canScrollVertically(1) && total!! / 10 + 1 > page && dy > 0) courseList
				binding.head.root.elevation = (if (v.canScrollVertically(-1)) config.dpToPx(2) else 0).toFloat()
			}
		})
		model.message.observe(requireActivity(), Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			val response = message.second
			val code = response.getInteger("code")
			if (code == 200) {
				when (message.first) {
					0 -> {
						term = response.getJSONObject("data").getString("semesterYear")
						this.courseList
					}
					1 -> {
						val data = response.getJSONObject("data")
						if (data != null) {
							total = data.getInteger("total")
							data.getJSONArray("rows")
								.forEach { e: Any? -> adp!!.add(e as JSONObject?) }
						}
					}
					3 -> {
						config.toast(response.getString("data"))
						regetCourseList()
					}
				}
				model.nextAll()
			}
		})
		info
		return binding.root
	}
	
	private fun regetCourseList() {
		clear()
		courseList
	}
	
	private fun selectCategory() {
		when (binding.head.category.checkedChipId) {
			R.id.major_compulsory -> typeCate.value = CommonUtil.Tuple2(1, 11)
			R.id.major_selective -> typeCate.value = CommonUtil.Tuple2(1, 21)
			R.id.school_public_selective -> typeCate.value = CommonUtil.Tuple2(1, 30)
			R.id.pe -> typeCate.value = CommonUtil.Tuple2(3, 10)
			R.id.en -> typeCate.value = CommonUtil.Tuple2(5, 1)
			R.id.public_compulsory -> typeCate.value = CommonUtil.Tuple2(1, 10)
			R.id.honor -> typeCate.value = CommonUtil.Tuple2(1, 31)
		}
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		if (savedInstanceState == null) {
			binding.head.addFilter.setOnClickListener { v: View? ->
				findNavController(view).navigate(R.id.selection_to_filter1, null, NavOptions.Builder() //                                            .setExitAnim(androidx.navigation.ui.R.anim.nav_default_pop_enter_anim)
					//                            .setEnterAnim()
					//                            .setExitAnim(android.R.animator.fade_out)
					.build(), FragmentNavigator.Extras.Builder()
													 .addSharedElement(v!!, "miniapp")
													 .build())
			}
		}
		val transition = MaterialContainerTransform().apply {
			scrimColor = Color.TRANSPARENT
			setAllContainerColors(requireContext().getColor(com.google.android.material.R.color.design_default_color_surface))
		}
		sharedElementEnterTransition = transition
		sharedElementReturnTransition = transition
		super.onViewCreated(view, savedInstanceState)
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
			if (type.getValue() != null && category.getValue() != null && term != null) getCourseList(getType(), getCategory(), term, toStringOrDefault<String?>(filter.getValue()))
		}
	
	fun getCourseList(selectedType: Int, selectedCate: Int, term: String?, filterText: String) {
		model.addAndNext("jwxt/choose-course-front-server/classCourseInfo/course/list", String.format(Locale.getDefault(), "{\"pageNo\":%d,\"pageSize\":10,\"param\":{\"semesterYear\":\"%s\",\"selectedType\":\"%d\",\"selectedCate\":\"%d\",\"hiddenConflictStatus\":\"0\",\"hiddenSelectedStatus\":\"%d\",\"hiddenEmptyStatus\":\"%d\",\"vacancySortStatus\":\"%d\",\"collectionStatus\":\"%d\"%s}", ++page, term, selectedType, selectedCate, bool2int(binding.head.hideSelected.isChecked), bool2int(binding.head.hideVacancy.isChecked), bool2int(binding.head.vacancy.isChecked), bool2int(binding.head.onlyCollection.isChecked), filterText.substring(1)), 1)
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
	
	fun getType(): Int {
		return typeCate.getValue()?.first ?: 1
	}
	
	fun getCategory(): Int {
		return typeCate.getValue()?.second ?: 11
	}
	
	fun unselect(classId: String, code: String?) {
		model.addAndNext("jwxt/choose-course-front-server/classCourseInfo/course/back", String.format(Locale.getDefault(), "{\"courseId\":\"%s\",\"clazzId\":\"%s\",\"selectedType\":\"%d\"}", classId, code, getType()), 3)
	}
	
	internal class SpacesItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {
		override fun getItemOffsets(outRect: Rect,
		                            view: View,
		                            parent: RecyclerView,
		                            state: RecyclerView.State) {
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
					setOnLongClickListener { a: View? ->
						(context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("", (a as Chip).getText()))
						false
					}
					setOnClickListener { a: View? ->
						Snackbar.make(context, a!!, (a as Chip).getText(), Snackbar.LENGTH_LONG)
							.show()
					}
				})
			}
			return object : RecyclerView.ViewHolder(binding.root) {}
		}
		
		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
			val binding = ItemCourseSelectionBinding.bind(holder.itemView)
			val context = binding.root.context
			val item = data[position]
			binding.courseName.text = "${convert(position, "courseNum")}-${convert(position, "courseName")}"
			val selectedStatus = item.getInteger("selectedStatus")
			item.fluentPut("statusName", context.getString(if (selectedStatus == 4) R.string.status_selected else if (selectedStatus == 3) R.string.filtering else if (selectedStatus == 1) R.string.retired else R.string.unselected))
			binding.like.setSelected(item.containsKey("collectionStatus") && item.getInteger("collectionStatus") == 1)
			binding.select.setSelected(item.containsKey("selectedStatus") && (selectedStatus == 3 || selectedStatus == 4))
			binding.select.text = if (binding.select.isSelected) context.getString(R.string.drop_course) else context.getString(R.string.select_course)
			binding.like.text = if (binding.like.isSelected) context.getString(R.string.unlike) else context.getString(R.string.like)
			binding.select.setOnClickListener { _: View? ->
				if (selectAction != null) selectAction!!.accept(position)
			}
			binding.like.setOnClickListener { v: View? ->
				Snackbar.make(v!!, context.getString(R.string.already) + (v as MaterialButton).getText(), Snackbar.LENGTH_LONG)
					.show()
				v.text = if (v.isSelected) context.getString(R.string.unlike) else context.getString(R.string.like)
				if (likeAction != null) likeAction!!.accept(convert(position, "teachingClassId"))
				v.setSelected(!v.isSelected)
			}
			binding.open.setOnClickListener { v: View? ->
				context.startActivity(Intent(context, CourseDetailActivity::class.java).putExtra("code", convert(position, "courseNum"))
										  .putExtra("id", convert(position, "courseId"))
										  .putExtra("class", convert(position, "clazzNum")), ActivityOptionsCompat.makeSceneTransitionAnimation(context as Activity, v!!, "miniapp")
										  .toBundle())
			}
			binding.head.text = convert(position, "teachingTimePlace").replace(";", " | ")
				.replace(",", "\n")
			val courseInfoLabels = context.resources.getStringArray(R.array.course_info_labels)
			val seatInfoLabels = context.resources.getStringArray(R.array.seat_info_labels)
			info.forEachIndexed { i, s ->
				(binding.courseInfo.getChildAt(i) as Chip).text = "${courseInfoLabels[i]}：${convert(position, s)}"
			}
			arrayOf("baseReceiveNum", "filterSelectedNum", "courseSelectedNum").forEachIndexed { i, s ->
				(arrayOf(binding.left, binding.filtering, binding.selected)[i]).text = "${seatInfoLabels[i]}\n${convert(position, s)}"
			}
		}
		
		fun convert(position: Int, key: String?): String {
			return trim(get(position).getString(key)).replace("\n\n", "\n")
		}
	}
}

