package com.miyuyan.sysuer.academic

import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.slider.LabelFormatter
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.miyuyan.sysuer.BaseFragment
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.CommonUtil
import com.miyuyan.sysuer.databinding.DialogEditTextBinding
import com.miyuyan.sysuer.databinding.FragmentQuestionnaireBinding
import com.miyuyan.sysuer.databinding.ItemOptionBinding
import com.miyuyan.sysuer.model.PjxtModel
import com.miyuyan.sysuer.todo.TitleAdapter
import com.miyuyan.sysuer.view.RecyclerViewHolder

class EvaluationQuestionnaireFragment : BaseFragment() {
	val answers: JSONObject = JSONObject.parseObject("{\"pjidlist\":[],\"pjjglist\":[],\"pjzt\": \"2\"}")
	lateinit var model: PjxtModel
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	                         ): View {
		super.onCreateView(inflater, container, savedInstanceState)
		model = PjxtModel(requireContext())
		val layoutManager = LinearLayoutManager(requireContext())
		val adp = ConcatAdapter(ConcatAdapter.Config.Builder().setIsolateViewTypes(true).build())
		val binding = FragmentQuestionnaireBinding.inflate(inflater, container, false).apply {
			recyclerView.layoutManager = layoutManager
			recyclerView.adapter = adp
			recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
				override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
					if (layoutManager.findFirstVisibleItemPosition() <= 0) title.visibility = View.GONE
					else {
						title.visibility = View.VISIBLE
						(layoutManager.findFirstVisibleItemPosition() - 1..0).forEach { pos ->
							val adapterPair = adp.getWrappedAdapterAndPosition(pos).first
							if (adapterPair is TitleAdapter && adapterPair.header == 1) {
								title.text = adapterPair.title
								return@forEach
							}
						}
					}
				}
			})
		}
		requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
		model.message.observe(requireActivity(), Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			val data = message.second
//			println(data)
			if (data.get("code") == "200") {
				when (message.first) {
					1 -> data.getJSONObject("result")
						.getJSONArray("assessedObjList")
						.forEach { l: Any? ->
							(l as JSONObject).getJSONArray("bpdxList").forEach { list: Any? ->
								val pjjglist = (list as JSONObject).clone()
								pjjglist.remove("dtjgList")
								pjjglist["pjxxlist"] = JSONArray()
								answers.getJSONArray("pjjglist").add(pjjglist)
								val bprmc = list.getString("bprmc") // 被评人名称
								if (!TextUtils.isEmpty(bprmc)) adp.addAdapter(TitleAdapter(bprmc, 1))
								list.getJSONArray("dtjgList").forEach { question: Any? ->
									val pjxxlist = JSONObject.parse("{\"sjly\": \"1\",\"stlx\": \"${(question as JSONObject).getString("tmlx", "1")}\",\"wjid\": \"${list.getString("wjid")}\",\"wjssrwid\": \"${list.getString("wjssrwid")}\",\"wjstctid\": \"\",\"wjstid\": \"${question.getString("tmid")}\",\"xxdalist\": []}")
									val da = question.getJSONArray("tmxxda")
									pjxxlist["xxdalist"] = da
									pjjglist.getJSONArray("pjxxlist").add(pjxxlist)
									adp.addAdapter(TitleAdapter(question.getString("tgmc"))) // 题目标题
									when (question.getString("tmlx")) {
										"1" -> {
											val optionAdapter = OptionAdapter()
											question.getJSONArray("tmxxlist")
												.forEach { o: Any? -> o?.let { optionAdapter.add(it as JSONObject) } }
											optionAdapter.answer = da
											adp.addAdapter(optionAdapter)
										}
										"6" -> question.getJSONArray("tmxxlist")
											.forEach { o: Any? ->
												val blanketAdapter = BlanketAdapter()
												val tmxxlist = o as JSONObject
												pjxxlist["wjstctid"] = tmxxlist.getString("tmxxid")
												pjxxlist["wjstid"] = tmxxlist.getString("tmid")
												val xxda = tmxxlist.getJSONArray("xxda")
												pjxxlist["xxdalist"] = xxda
												blanketAdapter.answer = xxda
												adp.addAdapter(blanketAdapter)
											}
										"5" -> {
											val rankAdapter = RankAdapter()
											rankAdapter.answer = da
											adp.addAdapter(rankAdapter)
										}
									}
								}
							}
						}
					2 -> model.contextUtil.toast(R.string.save_successful)
					3 -> model.contextUtil.toast(R.string.submit_successful)
				}
			}
		})
		getEvaluation(requireArguments().getString("rwid")!!, requireArguments().getString("wjid"), requireArguments().getString("sxz"), requireArguments().getString("pjrdm"), requireArguments().getString("bpdm"), requireArguments().getString("kcdm"), requireArguments().getString("rwh"), if (requireArguments().getString("lsjgzt") == "2") "1" else "", requireArguments().getString("bpmc"))
		binding.save.setOnClickListener { saveEvaluation() }
		binding.submit.setOnClickListener {
			Snackbar.make(binding.floatingToolbar, R.string.unchangeable_after_submission, Snackbar.LENGTH_LONG)
				.setAction(R.string.confirm) { submitEvaluation() }
				.show()
		}
		binding.reset.setOnClickListener {
			Snackbar.make(binding.floatingToolbar, R.string.reset, Snackbar.LENGTH_SHORT)
				.setAction(R.string.confirm) {
					adp.adapters.forEach { adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder?>? ->
						when (adapter) {
							is OptionAdapter -> adapter.clearAnswer()
							is RankAdapter -> adapter.clearAnswer()
							is BlanketAdapter -> adapter.clearAnswer()
						}
					}
				}
				.show()
		}
		binding.auto.setOnClickListener {
			adp.adapters.forEach { adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder?>? ->
				if (adapter is OptionAdapter) adapter.setLastOption()
				else if (adapter is RankAdapter) adapter.setLastRank()
			}
		}
		return binding.root
	}
	
	fun String.encodeNonAscii(): String {
		val sb = StringBuilder()
		forEach {
			if ((it in 'A'..'Z') || (it in 'a'..'z') || (it in '0'..'9')) sb.append(it) else "$it".toByteArray(Charsets.UTF_8)
				.forEach { byte ->
					sb.append("%${byte.toUByte().toString(16).uppercase().padStart(2, '0')}")
				}
		}
		return "$sb"
	}
	
	fun getEvaluation(
		rwid: String,
		wjid: String?,
		sxz: String?,
		pjrdm: String?,
		bpdm: String?,
		kcdm: String?,
		rwh: String?,
		pjzt: String?,
		bpmc: String?,
	                 ) {
		model.addAndNext("evaluationPattern/getQuestionnaireTopic?rwid=$rwid&wjid=$wjid&sxz=$sxz&pjrdm=$pjrdm&bpdm=$bpdm&kcdm=$kcdm&rwh=${rwh?.encodeNonAscii()}&pjzt=$pjzt&bpmc=$bpmc", 1)
	}
	
	fun saveEvaluation() {
		postEvaluation("2", 2)
	}
	
	fun submitEvaluation() {
		postEvaluation("1", 3)
	}
	
	fun postEvaluation(mode: String?, what: Int) {
		answers["pjzt"] = mode
		model.addAndNext("evaluationPattern/submitSaveEvaluation", "$answers", what)
	}
}

internal class OptionAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder?>() {
	val data = ArrayList<JSONObject>()
	var selected: Int = -1
	var option: String? = null
	var answer: JSONArray? = null
		set(answers) {
			field = answers
			option = answers?.let { if (it.isEmpty()) null else it.getString(0) }
			notifyItemRangeChanged(0, itemCount)
		}
	
	fun setOption(pos: Int) {
		if (selected != pos) {
			val old = selected
			selected = pos
			notifyItemChanged(old)
			notifyItemChanged(selected)
			answer!![0] = data[pos].getString("tmxxid")
		} else clearAnswer()
	}
	
	fun setLastOption() {
		setOption(data.size - 1)
	}
	
	fun clearAnswer() {
		answer!!.clear()
		val old = selected
		selected = -1
		option = null
		notifyItemChanged(old)
	}
	
	fun add(item: JSONObject) {
		data.add(item)
		notifyItemInserted(data.size - 1)
	}
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		return object :
			RecyclerView.ViewHolder(ItemOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false).root) {}
	}
	
	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		val pos = holder.getBindingAdapterPosition()
		val item = data[pos]
		if (selected == -1 && item.getString("tmxxid") == option) selected = pos
		ItemOptionBinding.bind(holder.itemView).apply {
			root.setOnClickListener { setOption(pos) }
			option.setChecked(selected == pos)
			option.text = item.getString("xxmc")
			root.updateAppearance(pos, itemCount)
		}
	}
	
	override fun getItemCount(): Int = data.size
}

internal class RankAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder?>() {
	var rank: Int = 0
	var answer: JSONArray? = null
		set(answers) {
			field = answers
			answers?.let { rank = if (it.isEmpty()) 100 else it.getString(0).toInt() }
			notifyItemChanged(0)
		}
	
	fun setLastRank() {
		rank = 100
		answer!![0] = "$rank"
		notifyItemChanged(0)
	}
	
	fun clearAnswer() {
		answer?.clear()
		rank = 100
		notifyItemChanged(0)
	}
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		return object : RecyclerView.ViewHolder(Slider(parent.context).apply {
			value = (if (rank == 0) 100 else rank).toFloat()
			stepSize = 1f
			valueFrom = 0f
			valueTo = 100f
			labelBehavior = LabelFormatter.LABEL_VISIBLE
			thumbHeight = 96
			addOnChangeListener { _: Slider?, value: Float, _: Boolean ->
				answer!![0] = value.toInt().toString()
			}
		}) {}
	}
	
	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
	}
	
	override fun getItemCount(): Int {
		return 1
	}
}

internal class BlanketAdapter : RecyclerView.Adapter<RecyclerViewHolder<DialogEditTextBinding>>() {
	var content: String? = null
	var answer: JSONArray? = null
		set(answers) {
			field = answers
			content = answers?.let { if (it.isEmpty()) null else it.getString(0) }
			notifyItemChanged(0)
		}
	
	override fun onCreateViewHolder(
		parent: ViewGroup,
		viewType: Int,
	                               ): RecyclerViewHolder<DialogEditTextBinding> {
		return object :
			RecyclerViewHolder<DialogEditTextBinding>(DialogEditTextBinding.inflate(LayoutInflater.from(parent.context), parent, false)) {}
	}
	
	fun setText(text: String?) {
		content = text
	}
	
	fun clearAnswer() {
		answer?.clear()
		content = null
		notifyItemChanged(0)
	}
	
	override fun onBindViewHolder(
		holder: RecyclerViewHolder<DialogEditTextBinding>,
		position: Int,
	                             ) {
		holder.binding?.apply {
			editLayout.setHint(R.string.please_enter_content)
			if (!content.isNullOrEmpty()) {
				answer!![0] = content
				edit.setText(content)
			}
			edit.addTextChangedListener(object : TextWatcher {
				override fun afterTextChanged(editable: Editable?) {
				}
				
				override fun beforeTextChanged(
					charSequence: CharSequence?,
					i: Int,
					i1: Int,
					i2: Int,
				                              ) {
				}
				
				override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
					if (TextUtils.isEmpty(s) && answer?.isEmpty() == false) {
						answer?.removeAt(0)
					} else answer!![0] = "$s"
				}
			})
			executePendingBindings()
		}
	}
	
	override fun getItemCount(): Int = 1
}