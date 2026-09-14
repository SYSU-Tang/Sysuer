package com.miyuyan.sysuer.academic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson2.JSONObject
import com.haibin.calendarview.Calendar
import com.haibin.calendarview.CalendarView.OnCalendarSelectListener
import com.miyuyan.sysuer.BaseActivity
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.DateTimeManager
import com.miyuyan.sysuer.databinding.ActivityAgendaBinding
import com.miyuyan.sysuer.databinding.ItemPreferenceBinding
import com.miyuyan.sysuer.todo.TitleAdapter
import com.miyuyan.sysuer.view.RecyclerAdapter

class AgendaActivity : BaseActivity() {
	lateinit var binding: ActivityAgendaBinding
	private val viewModel: AgendaViewModel by viewModels()
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val concatAdapter = ConcatAdapter()
		binding = ActivityAgendaBinding.inflate(layoutInflater).apply {
			content.recyclerView.layoutManager = LinearLayoutManager(this@AgendaActivity)
			content.recyclerView.adapter = concatAdapter
			toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
			calendarView.setOnCalendarSelectListener(object : OnCalendarSelectListener {
				override fun onCalendarOutOfRange(calendar: Calendar?) {
				}

				override fun onCalendarSelect(calendar: Calendar?, isClick: Boolean) {
					loadAgenda(calendar?.timeInMillis ?: 0)
				}
			})
			calendarView.setOnMonthChangeListener { year: Int, month: Int ->
				binding.toolbar.setSubtitle(
					getString(R.string.year_month, year, month)
				)
			}
			toolbar.setSubtitle(
				getString(R.string.year_month, calendarView.curYear, calendarView.curMonth)
			)
			calendarView.setSelectSingleMode()
			content.viewModel = viewModel
			content.lifecycleOwner = this@AgendaActivity
			content.root.setBackgroundResource(R.color.md_theme_surface)
			content.root.elevation = config.dpToPx(2).toFloat()
		}
		setContentView(binding.root)
		viewModel.scheduleList.observe(this) { list ->
			concatAdapter.adapters.forEach { adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder?>? ->
				concatAdapter.removeAdapter(adapter!!)
			}
			list?.forEach { i: Any? ->
				concatAdapter.addAdapter(TitleAdapter((i as JSONObject).getString("timeZone")))
				concatAdapter.addAdapter(AgendaAdapter().also { agendaAdapter ->
					agendaAdapter.add(i)
				})
			}
		}
		loadAgenda(binding.calendarView.selectedCalendar.timeInMillis)
	}

	private fun loadAgenda(date: Long) {
		viewModel.loadSchedule(
			DateTimeManager.toDate(date)
		)
	}

	internal class AgendaAdapter : RecyclerAdapter<JSONObject>() {
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
			object : RecyclerView.ViewHolder(
				ItemPreferenceBinding.inflate(
					LayoutInflater.from(parent.context), parent, false
				).root
			) {}


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