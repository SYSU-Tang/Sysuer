package com.sysu.edu.academic

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sysu.edu.BaseFragment
import com.sysu.edu.R
import com.sysu.edu.api.AuthorizationJar
import com.sysu.edu.api.HttpManager
import com.sysu.edu.api.TargetUrl
import com.sysu.edu.browser.BrowserActivity
import com.sysu.edu.databinding.FragmentHomeworkMainBinding
import com.sysu.edu.databinding.ItemHomeworkBinding
import com.sysu.edu.todo.TitleAdapter
import com.sysu.edu.view.AdapterListener
import com.sysu.edu.view.RecyclerAdapter

class HomeworkMainFragment : BaseFragment() {
	lateinit var http: HttpManager
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		super.onCreateView(inflater, container, savedInstanceState)
		val adapter = ConcatAdapter()
		val binding = FragmentHomeworkMainBinding.inflate(getLayoutInflater()).apply {
			list.layoutManager = LinearLayoutManager(requireContext())
			list.adapter = adapter
		}
		val authorizationJar = AuthorizationJar(requireContext())
		val detailDialog = MaterialAlertDialogBuilder(requireContext()).create()
		http = HttpManager(object : Handler(Looper.getMainLooper()) {
			override fun handleMessage(msg: Message) {
				if (msg.what == -1) config.toast(R.string.no_net_connected)
				else if (msg.getData().getBoolean("isJSON")) {
					var error = false
					(JSONArray.parseArray(msg.obj as String?)).forEach { i ->
						val item = i as JSONObject
						error = item.getBoolean("error")
						if (error) {
							config.toast(item.getJSONObject("exception").getString("message") ?: "")
							return@forEach
						} else {
							item.getJSONObject("data")
								.getJSONArray("events")
								.forEach { event: Any? ->
									val eventItem = event as JSONObject
									adapter.addAdapter(TitleAdapter(eventItem.getString("popupname")))
									adapter.addAdapter(HomeworkAdapter().apply {
										add(eventItem)
										setListener(object : AdapterListener {
											override fun onBind(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
											                    holder: RecyclerView.ViewHolder,
											                    position: Int) {
												ItemHomeworkBinding.bind(holder.itemView).apply {
													fold.setOnClickListener {
														detailDialog.setMessage(Html.fromHtml(get(position).getString("description"), Html.FROM_HTML_MODE_COMPACT))
														detailDialog.show()
													}
													this.view.setOnClickListener {
														startActivity(Intent(requireContext(), BrowserActivity::class.java).setData(Uri.parse(get(position).getString("url"))))
													}
													upload.setOnClickListener {
														startActivity(Intent(requireContext(), BrowserActivity::class.java).setData(Uri.parse(get(position).getString("viewurl"))))
													}
												}
											}
											
											override fun onCreate(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
											                      binding: ViewBinding?) {
											}
										})
									})
								}
						}
					}
					if (error) config.contextUtil.login(TargetUrl.LMS) {
						getLmsTask(authorizationJar.getToken("lms.sysu.edu.cn")) //						http.postRequest("https://lms.sysu.edu.cn/lib/ajax/service.php?sesskey=${authorizationJar.getToken("lms.sysu.edu.cn")}&info=core_calendar_get_calendar_upcoming_view", "[{\"index\":0,\"methodname\":\"core_calendar_get_calendar_upcoming_view\",\"args\":{\"courseid\":\"1\",\"categoryid\":\"0\"}}]", 0)
					}
				}
			}
		})
		http.setParams(config)
		getLmsTask(authorizationJar.getToken("lms.sysu.edu.cn"))
		return binding.root
	}
	
	fun getLmsTask(key: String) {
		http.postRequest("https://lms.sysu.edu.cn/lib/ajax/service.php?sesskey=$key&info=core_calendar_get_calendar_upcoming_view", "[{\"index\":0,\"methodname\":\"core_calendar_get_calendar_upcoming_view\",\"args\":{\"courseid\":\"1\",\"categoryid\":\"0\"}}]", 0)
	}
	
	internal class HomeworkAdapter : RecyclerAdapter<JSONObject>() {
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
			return object :
				RecyclerView.ViewHolder(ItemHomeworkBinding.inflate(LayoutInflater.from(parent.context), parent, false).root) {}
		}
		
		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
			val item = get(position)
			ItemHomeworkBinding.bind(holder.itemView).apply {
				val context = root.context
				title.text = item.getString("name")
				detail.text = "${context.getString(R.string.type)} ${item.getString("normalisedeventtypetext")}\n${context.getString(R.string.link)} ${item.getString("viewurl")}"
			}
			super.onBindViewHolder(holder, position)
		}
	}
}