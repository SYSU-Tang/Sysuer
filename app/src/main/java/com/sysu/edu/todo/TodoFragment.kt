package com.sysu.edu.todo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.haibin.calendarview.Calendar
import com.haibin.calendarview.CalendarView
import com.haibin.calendarview.CalendarView.OnCalendarSelectListener
import com.sysu.edu.BaseFragment
import com.sysu.edu.R
import com.sysu.edu.api.CalendarManager
import com.sysu.edu.api.CommonUtil.isEmpty
import com.sysu.edu.databinding.FragmentTodoBinding

class TodoFragment : BaseFragment() {
	val todoInfo: TodoInfo = TodoInfo().apply {
		setStatus(null)
	}
	lateinit var calendarView: CalendarView
	var due: Boolean = true
	var ddl: Boolean = false
	var todo: Boolean = true
	var done: Boolean = true
	lateinit var todoManager: TodoManager
	val calendarManager: CalendarManager = CalendarManager()
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		val concatAdapter = ConcatAdapter(ConcatAdapter.Config.Builder()
											  .setIsolateViewTypes(true)
											  .build())
		val toolbar = requireActivity().findViewById<Toolbar>(R.id.toolbar)
		val binding = FragmentTodoBinding.inflate(inflater, container, false).apply {
			recyclerView.adapter = concatAdapter
			recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false) //		val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
			calendarView.setOnMonthChangeListener { year: Int, month: Int -> toolbar.setSubtitle("${year}年${month}月") }
			calendarView.setSelectSingleMode()
			toolbar.setSubtitle("${calendarView.curYear}年${calendarView.curMonth}月")
			calendarView.setOnCalendarSelectListener(object : OnCalendarSelectListener {
				override fun onCalendarOutOfRange(calendar: Calendar?) {
				}
				
				override fun onCalendarSelect(calendar: Calendar, isClick: Boolean) {
					println(calendar)
					todoManager.performRefresh()
				}
			})
		}.also {
			calendarView = it.calendarView
		}
		todoManager = TodoManager(requireActivity(), concatAdapter)
		todoManager.setOnRefreshListener { refresh() }
		requireActivity().findViewById<FloatingActionButton>(R.id.add).setOnClickListener {
			todoManager.showTodoAddDialog()
			todoManager.getTodoInfo().setDueDate(date)
		}
		(requireActivity().findViewById<View?>(R.id.todo_date) as MaterialButtonToggleGroup).addOnButtonCheckedListener { _: MaterialButtonToggleGroup?, checkedId: Int, isChecked: Boolean ->
			if (checkedId == R.id.due_todo) due = isChecked
			else if (checkedId == R.id.ddl_todo) ddl = isChecked
			refresh()
		}
		requireActivity().findViewById<MaterialButtonToggleGroup>(R.id.todo_status).apply {
			check(R.id.todo_todo)
			check(R.id.done_todo)
			addOnButtonCheckedListener { _: MaterialButtonToggleGroup?, checkedId: Int, isChecked: Boolean ->
				if (checkedId == R.id.todo_todo) todo = isChecked
				else if (checkedId == R.id.done_todo) done = isChecked
				refresh()
			}
		}
		refresh()
		return binding.root
	}
	
	val date: String?
		get() {
			return calendarManager.toDateString(calendarView.selectedCalendar.getTimeInMillis())
		}
	
	fun refresh() {
		val a = mutableListOf<String?>()
		val b = mutableListOf<String?>()
		val map = mutableMapOf<String, MutableLiveData<*>>()        //        map.put("due_date", todoInfo.getDueDate());		//        map.put("ddl", todoInfo.getDdlDate());
		map["status"] = todoInfo.getStatus()
		map["title"] = todoInfo.getTitle()
		map["description"] = todoInfo.getDescription()
		map["priority"] = todoInfo.getPriority()
		map["todo_type"] = todoInfo.getType()
		map["subtask"] = todoInfo.getSubtask()
		map["attachment"] = todoInfo.getAttachment()
		map["subject"] = todoInfo.getSubject()
		map["location"] = todoInfo.getLocation()
		map["color"] = todoInfo.getColor()
		map["label"] = todoInfo.getTag()
		map["due_time"] = todoInfo.getDueTime()
		map["remind_time"] = todoInfo.getRemindTime()
		map["done_datetime"] = todoInfo.getDoneDate()
		map.forEach { (key: String, value: MutableLiveData<*>) ->
			if (!isEmpty(value.value)) {
				a.add("$key = ?")
				b.add(value.value.toString())
			}
		}
		if (due && ddl) {
			a.add("(due_date= ? OR ddl = ?)")
			b.add(date)
			b.add(date)
		}
		else if (due) {
			a.add("due_date= ?")
			b.add(date)
		}
		else if (ddl) {
			a.add("ddl= ?")
			b.add(date)
		}
		if (todo && done) {
			a.add("(status = ? OR status = ?)")
			b.add("0")
			b.add("1")
		}
		else if (todo) {
			a.add("status = ?")
			b.add("0")
		}
		else if (done) {
			a.add("status = ?")
			b.add("1")
		}
		todoManager.refresh(a.joinToString(" AND "), b.toTypedArray<String?>())
	}
}