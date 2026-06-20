package com.sysu.edu.academic

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.button.MaterialButton
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.databinding.ItemCardBinding
import com.sysu.edu.model.XgxtModel
import com.sysu.edu.view.AdapterListener
import com.sysu.edu.view.StaggeredFragment
import java.util.function.Consumer

class LeaveReturnListFragment : StaggeredFragment() {
	lateinit var model: XgxtModel
	override fun onDestroyView() {
		super.onDestroyView()
		model.dispose()
	}
	
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View? {
		val view = super.onCreateView(inflater, container, savedInstanceState)
		model = XgxtModel(requireContext())
		val viewModel = ViewModelProvider(requireActivity())[LeaveReturnRegistrationViewModel::class.java]
		viewModel.year.observe(getViewLifecycleOwner(), Observer { year: String? -> this.getList(year) })
		model.message.observe(requireActivity(), Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			val response = message.second
			if (response.getInteger("code") == 200) {
				clear()
				response.getJSONArray("data").forEach(Consumer { e: Any? ->
					add((e as JSONObject).getString("gzmc"), if (e.getInteger("gzztm") == 1) R.drawable.uncheck else R.drawable.check, resources.getStringArray(R.array.registration_keys)
						.toList(), extractValue(e, arrayOf("blxn", "lxdjsj", "gzsm", "jjrmc", "jjrrq", "gzzt", "zt")))
				})
				setListener(object : AdapterListener {
					override fun onBind(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>?,
					                    holder: RecyclerView.ViewHolder,
					                    position: Int) {
						val item = response.getJSONArray("data").getJSONObject(position)
						val isRegistering = item.getInteger("gzztm") == 1
						val status = item.getString("zt")
						holder.itemView.findViewById<MaterialButton>(R.id.button).apply {
							setText(if (isRegistering) if ("registering" == status) R.string.start_registration else R.string.modify_registration else R.string.view_detail)
							setOnClickListener {
								if (isRegistering) requireActivity().supportFragmentManager.beginTransaction()
									.replace(R.id.leave_return_list_fragment, LeaveReturnRegistrationFragment::class.java, Bundle().apply {
										putString("Id", item.getString("cjlfxgzId"))
									})
									.addToBackStack(null)
									.commit()
							}
						}
					}
					
					override fun onCreate(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>?,
					                      binding: ViewBinding) {
						(binding as ItemCardBinding).root.addView(MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonTonalStyle).apply {
							setId(R.id.button)
							setLayoutParams(LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
												.apply {
													gravity = Gravity.END
													setMargins(0, 0, model.contextUtil.dpToPx(16), model.contextUtil.dpToPx(16))
												})
						})
					}
				})
			}
		})
		return view
	}
	
	fun getList(year: String?) {
		model.addAndNext("jjrlfx/api/sm-jjrlfx/student/work-list?blxn=$year", 0)
	}
}