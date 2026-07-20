package com.sysu.edu.life

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.BaseFragment
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.databinding.RecyclerViewScrollBinding
import com.sysu.edu.model.GymModel
import com.sysu.edu.todo.TitleAdapter
import com.sysu.edu.view.PreferenceAdapter

class GymAccountFragment : BaseFragment() {
	val model: GymModel by lazy {
		GymModel(requireContext())
	}
	
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		super.onCreateView(inflater, container, savedInstanceState)
		val concatAdapter = ConcatAdapter(ConcatAdapter.Config.Builder()
			                                  .setIsolateViewTypes(true)
			                                  .build())
		val binding = RecyclerViewScrollBinding.inflate(inflater, container, false).apply {
			recyclerView.layoutManager = LinearLayoutManager(context)
			recyclerView.adapter = concatAdapter
			recyclerView.setBackgroundColor(model.contextUtil.getColorFromAttr(com.google.android.material.R.attr.colorSurfaceContainer))
		}
		model.message.observe(viewLifecycleOwner) { (code, response) ->
			when (code) {
				0 -> {
					val preferenceAdapter = PreferenceAdapter()
					preferenceAdapter.set(mutableListOf(R.string.type,
					                                    R.string.name,
					                                    R.string.student_id,
					                                    R.string.net_id),
					                      extractValue(response,
					                                   arrayOf("Type",
					                                           "Name",
					                                           "HostKey",
					                                           "UserId")),
					                      mutableListOf(R.drawable.help,
					                                    R.drawable.text,
					                                    R.drawable.school,
					                                    R.drawable.id),
					                      requireContext())
					concatAdapter.addAdapter(TitleAdapter(getString(R.string.account)))
					concatAdapter.addAdapter(preferenceAdapter)
					val cashAdapter = PreferenceAdapter()
					cashAdapter.set(mutableListOf(R.string.sport_credit, R.string.wallet),
					                extractValue(response, arrayOf("Credits", "CashWallet")),
					                mutableListOf(R.drawable.dashboard, R.drawable.money),
					                requireContext())
					concatAdapter.addAdapter(TitleAdapter(getString(R.string.wallet)))
					concatAdapter.addAdapter(cashAdapter)
					val idAdapter = PreferenceAdapter()
					arrayOf("validSwimmer", "IsAdmin").forEachIndexed { i, v ->
						idAdapter.add(getString(listOf(R.string.is_swimmer_valid,
						                               R.string.admin)[i]),
						              if (response.getBoolean(v)) getString(R.string.yes)
						              else getString(R.string.no),
						              R.drawable.help)
					}
					concatAdapter.addAdapter(TitleAdapter(getString(R.string.other)))
					concatAdapter.addAdapter(idAdapter)
					swimmer
				}
				1 -> response.getJSONArray("data").forEach { i: Any? ->
					val item = i as JSONObject
					val certAdapter = PreferenceAdapter()
					val list: ArrayList<String?> = extractValue(item,
					                                            arrayOf("Status",
					                                                    "ValidUntil",
					                                                    "PhysicalExamDate"))
					list[0] = if ("approved" == list[0]) getString(R.string.approved)
					else getString(R.string.disapproved)
					certAdapter.set(mutableListOf(R.string.status,
					                              R.string.valid_date,
					                              R.string.physical_exam_date),
					                list,
					                mutableListOf(if ("approved" == list[0]) R.drawable.uncheck else R.drawable.check,
					                              R.drawable.calendar,
					                              R.drawable.calendar),
					                requireContext())
					concatAdapter.addAdapter(TitleAdapter(getString(R.string.health_proof)))
					concatAdapter.addAdapter(certAdapter)
				}
			}
		}
		account
		return binding.root
	}
	
	val account: Unit
		get() {
			model.addAndNext("api/Credit/Me", 0)
		}
	val swimmer: Unit
		get() {
			model.addAndNext("api/swimmer/me", 1)
		}
}