package com.sysu.edu.academic

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
import com.sysu.edu.BaseFragment
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.CommonUtil.trim
import com.sysu.edu.api.Config
import com.sysu.edu.databinding.ItemEvaluationBinding
import com.sysu.edu.databinding.RecyclerViewScrollBinding
import com.sysu.edu.model.PjxtModel
import com.sysu.edu.view.AdapterListener
import com.sysu.edu.view.RecyclerAdapter

class EvaluationCategoryFragment : BaseFragment() {
	lateinit var staggeredGridLayoutManager: StaggeredGridLayoutManager
	lateinit var model: PjxtModel
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		super.onCreateView(inflater, container, savedInstanceState)
		config = Config(this)
		model = PjxtModel(requireContext())
		staggeredGridLayoutManager = StaggeredGridLayoutManager(config.column, 1)
		val binding = RecyclerViewScrollBinding.inflate(inflater, container, false).apply {
			root.layoutManager=staggeredGridLayoutManager
		}
		val categoryAdapter = CategoryAdapter()
		val keys: Array<String> = arrayOf("rwmc", "rwkssj", "rwjssj", "pjsl", "ypsl")
		val values: Array<String> = arrayOf("%s", "起始时间：%s", "结束时间：%s", "总评数：%s", "已评数：%s")
		val arguments: Array<String> = arrayOf("rwid", "firstwjid", "pjrdm")
		categoryAdapter.setListener(object : AdapterListener {
			override fun onBind(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>?,
			                    holder: RecyclerView.ViewHolder,
			                    position: Int) {
				val bind = ItemEvaluationBinding.bind(holder.itemView)
				val args = Bundle()
				val item = categoryAdapter.get(position)
				for (param in arguments) args.putString(param, item.getString(param))
				val listener = View.OnClickListener {  findNavController(binding.root).navigate(R.id.from_category_to_course, args) }
				bind.open.setOnClickListener(listener)
				holder.itemView.setOnClickListener(listener)
				bind.title.setCompoundDrawablesWithIntrinsicBounds(if (item.getString("pjsl")
						.toInt() <= item.getString("ypsl")
						.toInt()) R.drawable.submit else R.drawable.window, 0, 0, 0)
				bind.title.setCompoundDrawablePadding(36)
				bind.title.text = String.format(values[0], trim(item.getString(keys[0])))
				val stringBuilder = StringBuilder()
				keys.forEachIndexed { index, string ->
					stringBuilder.append(String.format(values[index], trim(item.getString(string))))
						.append("\n")
				}
				bind.startTime.text = "$stringBuilder".trim { it <= ' ' }
			}
			
			override fun onCreate(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>?,
			                      binding: ViewBinding?) {
			}
		})
		binding.root.setAdapter(categoryAdapter)
		model.message.observe(requireActivity(), Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			val data = message.second
			if (data.get("code") == "200") if (message.first == 1) data.getJSONObject("result")
				.getJSONArray("list")
				.forEach { e: Any? -> categoryAdapter.add(e as JSONObject?) }
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
				RecyclerView.ViewHolder(ItemEvaluationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
											.root) {}
		}
	}
}