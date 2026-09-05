package com.miyuyan.sysuer.academic

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.viewbinding.ViewBinding
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.BaseFragment
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.CommonUtil.trim
import com.miyuyan.sysuer.databinding.ItemEvaluationBinding
import com.miyuyan.sysuer.databinding.RecyclerViewScrollBinding
import com.miyuyan.sysuer.model.PjxtModel
import com.miyuyan.sysuer.view.AdapterListener
import com.miyuyan.sysuer.view.RecyclerAdapter

class EvaluationCategoryFragment : BaseFragment() {
	lateinit var staggeredGridLayoutManager: StaggeredGridLayoutManager
	lateinit var model: PjxtModel
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		super.onCreateView(inflater, container, savedInstanceState)
		model = PjxtModel(requireContext())
		staggeredGridLayoutManager = StaggeredGridLayoutManager(config.column, 1)
		val binding = RecyclerViewScrollBinding.inflate(inflater, container, false).apply {
			root.layoutManager = staggeredGridLayoutManager
		}
		val categoryAdapter = CategoryAdapter()
		val keys: Array<String> = arrayOf("rwmc", "rwkssj", "rwjssj", "pjsl", "ypsl")
		val values: Array<String> = arrayOf("%s", "起始时间：%s", "结束时间：%s", "总评数：%s", "已评数：%s")
		val arguments: Array<String> = arrayOf("rwid", "firstwjid", "pjrdm")
		categoryAdapter.listener = object : AdapterListener {
			override fun onBind(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
			                    holder: RecyclerView.ViewHolder,
			                    position: Int) {
				val binding = ItemEvaluationBinding.bind(holder.itemView)
				val args = Bundle()
				val item = categoryAdapter.get(position)
				for (param in arguments) args.putString(param, item.getString(param))
				val listener = View.OnClickListener { findNavController(binding.root).navigate(R.id.from_category_to_course, args) }
				binding.open.setOnClickListener(listener)
				holder.itemView.setOnClickListener(listener)
				binding.title.setCompoundDrawablesWithIntrinsicBounds(if (item.getString("pjsl")
						.toInt() <= item.getString("ypsl").toInt()) R.drawable.submit
				                                                   else R.drawable.window, 0, 0, 0)
				binding.title.setCompoundDrawablePadding(36)
				binding.title.text = String.format(values[0], trim(item.getString(keys[0])))
				val stringBuilder = StringBuilder()
				keys.forEachIndexed { index, string ->
					stringBuilder.append(String.format(values[index], trim(item.getString(string))))
						.append("\n")
				}
				binding.startTime.text = "$stringBuilder".trim { it <= ' ' }
			}
			
			override fun onCreate(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
			                      binding: ViewBinding?) {
			}
		}
		binding.root.adapter = categoryAdapter
		model.message.observe(requireActivity(), Observer { (code, data) ->
			if (data.get("code") == "200") if (code == 1) data.getJSONObject("result")
				.getJSONArray("list")
				.forEach { categoryAdapter.add(it as JSONObject) }
		})
		evaluation
		return binding.root
	}
	
	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)
		staggeredGridLayoutManager.setSpanCount(config.column)
	}
	
	val evaluation: Unit
		get() {
			model.addAndNext("personnelEvaluation/listObtainPersonnelEvaluationTasks?pageNum=1&pageSize=10", 1)
		}
	
	class CategoryAdapter : RecyclerAdapter<JSONObject>() {
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
			return object :
				RecyclerView.ViewHolder(ItemEvaluationBinding.inflate(LayoutInflater.from(parent.context), parent, false).root) {}
		}
	}
}