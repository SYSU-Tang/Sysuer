package com.miyuyan.sysuer.life

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.miyuyan.sysuer.BaseActivity
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.CommonUtil.trim
import com.miyuyan.sysuer.browser.BrowserActivity
import com.miyuyan.sysuer.databinding.ActivityNewsBinding
import com.miyuyan.sysuer.databinding.ItemPreferenceBinding
import com.miyuyan.sysuer.model.IportalModel
import com.miyuyan.sysuer.view.AdapterListener
import com.miyuyan.sysuer.view.Pager2Adapter
import com.miyuyan.sysuer.view.RecyclerAdapter
import kotlinx.coroutines.launch

class NewsActivity : BaseActivity() {
	val model: IportalModel by lazy { IportalModel(this) }
	lateinit var edit: EditText

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val adapter = Pager2Adapter(this)
		val suggestionAdapter = SuggestionAdapter().apply {
			listener = object : AdapterListener {
				override fun onBind(
					adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
					holder: RecyclerView.ViewHolder,
					position: Int
				) {
					holder.itemView.setOnClickListener { v: View ->
						startActivity(
								Intent(
										this@NewsActivity, BrowserActivity::class.java
								).setData(
										"https://${model.host}/searchWeb/#/index?searchWord=${
											get(position)
										}&module=default&size=10&current=1&sortType=score&searchType=3".toUri()
								),
								ActivityOptionsCompat
									.makeSceneTransitionAnimation(this@NewsActivity, v, "miniapp")
									.toBundle()
						)
					}
				}

				override fun onCreate(
					adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>, binding: ViewBinding?
				) {
				}
			}
		}
		(0..<4).forEach { adapter.add(NewsFragment.getInstance(it)) }
		val binding = ActivityNewsBinding.inflate(layoutInflater).apply {
			pager.adapter = adapter
			TabLayoutMediator(tabLayout, pager) { tab: TabLayout.Tab, position: Int ->
				tab.text = arrayOf("资讯", "公众号", "通知", "今日中大")[position]
			}.attach()
			sugs.adapter = suggestionAdapter
			sugs.layoutManager = GridLayoutManager(this@NewsActivity, 1)
		}
		setContentView(binding.root)
		lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.STARTED) {
				model.messageChannel.collect { (requestCode, data) ->
					if (requestCode == 1) {
						suggestionAdapter.clear()
						data.getJSONObject("data")?.getJSONArray("suggests")?.forEach {
							suggestionAdapter.add(it as String?)
						}
					}
				}
			}
		}
		edit = binding.searchView.editText.apply {
			setOnEditorActionListener { _: TextView?, _: Int, _: KeyEvent? ->
				binding.searchView.hide()
				false
			}
			addTextChangedListener(object : TextWatcher {
				override fun beforeTextChanged(
					charSequence: CharSequence?, i: Int, i1: Int, i2: Int
				) {
				}

				override fun onTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {
					if ("$text".isNotEmpty()) getSuggestions("$text")
				}

				override fun afterTextChanged(editable: Editable?) {
				}
			})
		}
	}

	private fun suggestions() {
		getSuggestions("${edit.text}")
	}

	fun getSuggestions(keyword: String) {
		val data = JSONObject.of("aliasName", "collection_data", "keyWord", keyword).toJSONString()
		model.enqueue("ai_service/search-server/needle/suggest", data = data, code = 1)
	}

	override fun onDestroy() {
		super.onDestroy()
		model.dispose()
	}

	internal class SuggestionAdapter : RecyclerAdapter<String?>() {
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
			return object : RecyclerView.ViewHolder(
					LayoutInflater.from(parent.context)
						.inflate(R.layout.item_preference, parent, false)
			) {}
		}

		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
			val binding = ItemPreferenceBinding.bind(holder.itemView)
			binding.itemTitle.text = trim(get(position))
			binding.itemContent.text = "${get(position)}"
			super.onBindViewHolder(holder, position)
		}
	}
}
