package com.miyuyan.sysuer.life

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.BaseFragment
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.CommonUtil
import com.miyuyan.sysuer.databinding.ItemComplaintSquareBinding
import com.miyuyan.sysuer.databinding.RecyclerViewScrollBinding
import com.miyuyan.sysuer.model.XinfangModel
import com.miyuyan.sysuer.view.RecyclerAdapter
import io.noties.markwon.Markwon
import io.noties.markwon.html.HtmlPlugin

class ComplaintSquareFragment : BaseFragment() {
	lateinit var model: XinfangModel
	lateinit var layoutManager: StaggeredGridLayoutManager
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		super.onCreateView(inflater, container, savedInstanceState)
		val adapter = SquareAdapter()
		val layoutManager = StaggeredGridLayoutManager(config.column, StaggeredGridLayoutManager.VERTICAL)
		val binding = RecyclerViewScrollBinding.inflate(inflater, container, false).apply {
			root.adapter = adapter
			root.layoutManager = layoutManager
		}
		model = XinfangModel(requireContext())
		model.message.observe(requireActivity(), Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			val response = message.second
			if (message.first == 0) if (response.getBoolean("ok")) response.getJSONArray("data")
				.forEach { adapter.add(it as JSONObject) }
			else config.toast(response.getString("msg"))
		})
		square
		return binding.root
	}
	
	val square: Unit
		get() {
			model.addAndNext("jsp_api/hsgc", "", 0)
		}
	
	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)
		layoutManager.spanCount = config.column
	}
	
	internal class SquareAdapter : RecyclerAdapter<JSONObject>() {
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
			return object : RecyclerView.ViewHolder(LayoutInflater.from(parent.context)
														.inflate(R.layout.item_complaint_square, parent, false)) {}
		}
		
		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
			val item = get(position)
			ItemComplaintSquareBinding.bind(holder.itemView).apply {
				title.text = item.getString("name")
				detail.text = "#${item.getString("createDate")}  #${item.getString("questionType", "未分类")}"
				request.text = item.getString("description", "暂无公开答复内容")
				Markwon.builder(holder.itemView.context)
					.usePlugin(HtmlPlugin.create())
					.build()
					.setMarkdown(response, item.getString("dfnr"))
			}
		}
	}
}