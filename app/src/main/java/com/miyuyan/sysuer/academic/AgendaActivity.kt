package com.miyuyan.sysuer.academic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson2.JSONObject
import com.haibin.calendarview.Calendar
import com.haibin.calendarview.CalendarView.OnCalendarSelectListener
import com.miyuyan.sysuer.BaseActivity
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.CommonUtil
import com.miyuyan.sysuer.databinding.ActivityAgendaBinding
import com.miyuyan.sysuer.databinding.ItemPreferenceBinding
import com.miyuyan.sysuer.model.PortalModel
import com.miyuyan.sysuer.todo.TitleAdapter
import com.miyuyan.sysuer.view.RecyclerAdapter
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

class AgendaActivity : BaseActivity() {
	lateinit var binding: ActivityAgendaBinding
	lateinit var model: PortalModel
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		model = PortalModel(this)
		val concatAdapter = ConcatAdapter()
		binding = ActivityAgendaBinding.inflate(layoutInflater).apply {
			list.setLayoutManager(LinearLayoutManager(this@AgendaActivity))
			list.setAdapter(concatAdapter)
			toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
			calendarView.setOnCalendarSelectListener(object : OnCalendarSelectListener {
				override fun onCalendarOutOfRange(calendar: Calendar?) {
				}
				
				override fun onCalendarSelect(calendar: Calendar?, isClick: Boolean) {
					agenda
				}
			})
			calendarView.setOnMonthChangeListener { year: Int, month: Int -> binding.toolbar.setSubtitle(String.format(Locale.getDefault(), "%d年%d月", year, month)) }
			toolbar.setSubtitle(String.format(Locale.getDefault(), "%d年%d月", calendarView.curYear, calendarView.curMonth))
			calendarView.setSelectSingleMode()
		}
		setContentView(binding.getRoot())
		model.message.observe(this, Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			val response: JSONObject = message.second
			if (response.getJSONObject("meta").getInteger("statusCode") == 200 && response.get("data") != null) {
				if (message.first == 0) {
					concatAdapter.adapters.forEach { adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder?>? -> concatAdapter.removeAdapter(adapter!!) }
					response.getJSONArray("data").takeIf { it.isNotEmpty() }?.let {
						it.getJSONObject(0).getJSONArray("newUserScheduleDetailList").forEach { i: Any? ->
								concatAdapter.addAdapter(TitleAdapter((i as JSONObject).getString("timeZone")))
								concatAdapter.addAdapter(AgendaAdapter().also { agendaAdapter -> agendaAdapter.add(i) })
							}
					}
				}
			}
		})
		agenda
	}
	
	val agenda: Unit
		get() {
			model.addAndNext("newClient/api/schedule/newSchedule/getScheduleByTimeZone", "$args", 0)
		}
	val args: JSONObject
		get() {
			val day =
				Instant.ofEpochMilli(binding.calendarView.selectedCalendar.timeInMillis).atZone(ZoneId.systemDefault()).toLocalDate()
			return JSONObject.of("startTime", day, "endTime", day, "types", null, "isMine", "1", "teamWorkDeptId", null)
		}
	
	internal class AgendaAdapter : RecyclerAdapter<JSONObject>() {
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
			return object :
				RecyclerView.ViewHolder(ItemPreferenceBinding.inflate(LayoutInflater.from(parent.context), parent, false).getRoot()) {}
		}
		
		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
			val item = get(position)
			val place = item.getString("place")
			ItemPreferenceBinding.bind(holder.itemView).apply {
				itemTitle.text = item.getString("title")
				if (!place.isNullOrEmpty()) itemContent.text = place
				else itemContent.visibility = View.GONE
				itemIcon.setImageResource(R.drawable.text)
				root.updateAppearance(position, itemCount)
				root.setOnClickListener {}
			}
			super.onBindViewHolder(holder, position)
		}
	}
}