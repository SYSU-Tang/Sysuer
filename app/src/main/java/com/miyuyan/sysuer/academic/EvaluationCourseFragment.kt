package com.miyuyan.sysuer.academic

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.Observer
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.viewbinding.ViewBinding
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.BaseFragment
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.CommonUtil
import com.miyuyan.sysuer.api.CommonUtil.toStringOrDefault
import com.miyuyan.sysuer.databinding.ItemEvaluationBinding
import com.miyuyan.sysuer.databinding.RecyclerViewScrollBinding
import com.miyuyan.sysuer.model.PjxtModel
import com.miyuyan.sysuer.view.AdapterListener
import com.miyuyan.sysuer.view.RecyclerAdapter
import java.util.Locale
import java.util.Map

class EvaluationCourseFragment : BaseFragment() {
	var page: Int = 1
	lateinit var model: PjxtModel
	private lateinit var sgm: StaggeredGridLayoutManager
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		super.onCreateView(inflater, container, savedInstanceState)
		model = PjxtModel(requireContext())
		page = 1
		sgm = StaggeredGridLayoutManager(config.column, 1)
		val binding = RecyclerViewScrollBinding.inflate(inflater, container, false).apply {
			root.layoutManager = sgm
		}
		val type = requireArguments().getString("firstwjid")
		val rwid = requireArguments().getString("rwid")
		val pjrdm = requireArguments().getString("pjrdm")
		val keys: Array<String> = arrayOf("kcmc", "skjsmc", "kcdlmc", "kkyxmc", "bjmc", "kcdm", "xnxqmc", "lsjgzt")
		val values: Array<String> = arrayOf("%s", "教师：%s", "课程类型：%s", "开课院系：%s", "教学班号：%s", "课程代码：%s", "学期：%s", "评价状态：%s")
		val arguments: Array<String> = arrayOf("rwid", "wjid", "sxz", "pjrdm", "bpdm", "kcdm", "rwh", "lsjgzt", "bpmc")
		val adp = CourseEvaluationAdapter().apply {
			listener = object : AdapterListener {
				override fun onBind(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
				                    holder: RecyclerView.ViewHolder,
				                    position: Int) {
					val bind = ItemEvaluationBinding.bind(holder.itemView)
					val args = Bundle()
					val context = holder.itemView.context
					val item = get(position)
					for (arg in arguments) args.putString(arg, item.getString(arg))
					val drawable = AppCompatResources.getDrawable(context, if (item.getString("lsjgzt") == "2") R.drawable.submit else R.drawable.window)
					drawable?.setBounds(0, 0, 72, 72)
					bind.title.setCompoundDrawables(drawable, null, null, null)
					bind.title.setCompoundDrawablePadding(36)
					val action = View.OnClickListener { findNavController(binding.root).navigate(R.id.from_course_to_evaluation, args) }
					bind.open.setOnClickListener(action)
					holder.itemView.setOnClickListener(action)
					bind.title.text = String.format(values[0], toStringOrDefault<String?>(item.getString(keys[0])))
					val stringBuilder = StringBuilder()
					keys.forEachIndexed { i, key ->
						stringBuilder.append(String.format(values[i], if (key == "lsjgzt") Map.of<String?, String?>("0", "待评价", "2", "已评价", "3", "已保存")
							.getOrDefault(item.getString(key), "未知") else toStringOrDefault<String?>(item.getString(key), "")))
							.append("\n")
					}
					bind.startTime.text = "$stringBuilder".trim { it <= ' ' }
				}
				
				override fun onCreate(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
				                      binding: ViewBinding?) {
				}
			}
		}
		binding.root.adapter = adp
		model.message.observe(requireActivity(), Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			val response = message.second
			if (response.get("code") == "200") if (message.first == 1) {
				val result = response.getJSONObject("result")
				result.getJSONArray("list").forEach { e: Any? -> adp.add(e as JSONObject) }
				if (result.getInteger("total") / 20.0 > page) getEvaluation(type, rwid, pjrdm!!)
			}
		})
		if (type != null && rwid != null && pjrdm != null) getEvaluation(type, rwid, pjrdm)
		return binding.root
	}
	
	fun getEvaluation(wjid: String?, rwid: String?, pjrdm: String) {
		model.addAndNext(String.format(Locale.getDefault(), "personnelEvaluation/listEcaluationRalationshipEnriry?pjrdm=%s&wjid=%s&rwid=%s&pageNum=%d&pageSize=20", pjrdm, wjid, rwid, page++), 1)
	}
	
	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)
		sgm.setSpanCount(config.column)
	}
	
	internal class CourseEvaluationAdapter : RecyclerAdapter<JSONObject>() {
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
			return object :
				RecyclerView.ViewHolder(ItemEvaluationBinding.inflate(LayoutInflater.from(parent.context), parent, false).root) {}
		}
	}
}