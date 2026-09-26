package com.miyuyan.sysuer.life

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.BaseFragment
import com.miyuyan.sysuer.R

import com.miyuyan.sysuer.databinding.ItemComplaintSquareBinding
import com.miyuyan.sysuer.databinding.RecyclerViewScrollBinding
import com.miyuyan.sysuer.model.XinfangModel
import com.miyuyan.sysuer.view.RecyclerAdapter
import kotlinx.coroutines.launch

class ComplaintSquareFragment : BaseFragment() {
	lateinit var model: XinfangModel
	lateinit var layoutManager: StaggeredGridLayoutManager
	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		super.onCreateView(inflater, container, savedInstanceState)
		val adapter = SquareAdapter()
		layoutManager =
			StaggeredGridLayoutManager(config.column, StaggeredGridLayoutManager.VERTICAL)
		val binding = RecyclerViewScrollBinding.inflate(inflater, container, false).apply {
			root.adapter = adapter
			root.layoutManager = layoutManager
		}
		model = XinfangModel(requireContext())
		viewLifecycleOwner.lifecycleScope.launch {
			viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
				model.message.collect { (code, response) ->
					if (code == 0) if (response.getBoolean("ok")) response.getJSONArray("data")
						.forEach { adapter.add(it as JSONObject) }
					else config.toast(response.getString("msg"))
				}
			}
		}
		loadSquare()
		return binding.root
	}

	override fun onDestroyView() {
		super.onDestroyView()
		model.dispose()
	}

	private fun loadSquare() {
			model.enqueue("jsp_api/hsgc", "", 0)
		}

	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)
		layoutManager.setSpanCount(
			when {
				newConfig.screenWidthDp < 540 -> 1
				newConfig.screenWidthDp < 900 -> 2
				else -> 3
			}
		)
	}

	internal class SquareAdapter : RecyclerAdapter<JSONObject>() {
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
			object : RecyclerView.ViewHolder(
				LayoutInflater.from(parent.context)
					.inflate(R.layout.item_complaint_square, parent, false)
			) {}

		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
			val item = get(position)
			ItemComplaintSquareBinding.bind(holder.itemView).apply {
				title.text = item.getString("name")
				detail.text =
					"#${item.getString("createDate")}  #${item.getString("questionType", "未分类")}"
				request.text = item.getString("description", "暂无公开答复内容")
				response.setMarkdown(item.getString("dfnr"), true)
			}
		}
	}
}