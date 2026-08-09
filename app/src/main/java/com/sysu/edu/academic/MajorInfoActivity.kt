package com.sysu.edu.academic

import android.os.Bundle
import android.view.MenuItem
import androidx.lifecycle.Observer
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.databinding.ActivityPagerBinding
import com.sysu.edu.model.JwxtModel
import com.sysu.edu.view.Pager2Adapter
import com.sysu.edu.view.StaggerFragment

class MajorInfoActivity : BaseActivity() {
	lateinit var model: JwxtModel
	override fun onDestroy() {
		super.onDestroy()
		model.dispose()
	}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		model = JwxtModel(this)
		val pager2Adapter = Pager2Adapter(this)
		val categories = mutableListOf<String?>()
		val binding = ActivityPagerBinding.inflate(layoutInflater).apply {
			toolbar.setTitle(R.string.major_info)
			toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
			pager.adapter = pager2Adapter
			toolbar.menu.add(R.string.export)
				.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM)
				.setIcon(R.drawable.export)
				.setOnMenuItemClickListener {
					val currentItem = pager.currentItem
					val fragment = (pager2Adapter.get(currentItem) as StaggerFragment)
					fragment.export(toolbar, tabLayout.getTabAt(currentItem)?.text.toString())
					true
				}
			TabLayoutMediator(tabLayout, pager) { tab: TabLayout.Tab?, position: Int -> tab!!.text = categories[position] }.attach()
		}
		setContentView(binding.getRoot())
		model.message.observe(this, Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			val response = message.second
			if (response.getInteger("code") == 200 && response.get("data") != null) if (message.first == 0) {
				categories.clear()
				response.getJSONArray("data").forEach { a: Any? ->
					categories.add((a as JSONObject).getString("dataName"))
					pager2Adapter.add(MajorInfoFragment.newInstance(Bundle().apply {
						putString("code", a.getString("dataNumber"))
					}))
				}
			}
		})
		category
	}
	
	val category: Unit
		get() {
			model.addAndNext("jwxt/base-info/codedata/findcodedataNames?datableNumber=135", 0)
		}
}