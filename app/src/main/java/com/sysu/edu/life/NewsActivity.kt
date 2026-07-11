package com.sysu.edu.life

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.api.AuthorizationJar
import com.sysu.edu.api.AuthorizationManager
import com.sysu.edu.api.CommonUtil.trim
import com.sysu.edu.api.HttpManager
import com.sysu.edu.api.TargetUrl
import com.sysu.edu.browser.BrowserActivity
import com.sysu.edu.databinding.ActivityNewsBinding
import com.sysu.edu.view.AdapterListener
import com.sysu.edu.view.Pager2Adapter
import com.sysu.edu.view.RecyclerAdapter

class NewsActivity : BaseActivity() {
	lateinit var http: HttpManager
	val authorizationManager: AuthorizationManager = AuthorizationManager("https://iportal.sysu.edu.cn/", "https://iportal-443.webvpn.sysu.edu.cn/")
	lateinit var edit: EditText
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val adapter = Pager2Adapter(this)
		val suggestionAdapter = SuggestionAdapter().apply {
			listener=object : AdapterListener {
				override fun onBind(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
				                    holder: RecyclerView.ViewHolder,
				                    position: Int) {
					holder.itemView.setOnClickListener { v: View? ->
						startActivity(Intent(this@NewsActivity, BrowserActivity::class.java).setData("https://iportal.sysu.edu.cn/searchWeb/#/index?searchWord=${get(position)}&module=default&size=10&current=1&sortType=score&searchType=3".toUri()), ActivityOptionsCompat.makeSceneTransitionAnimation(this@NewsActivity, v!!, "miniapp")
							.toBundle())
					}
				}
				
				override fun onCreate(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
				                      binding: ViewBinding?) {
				}
			}
		}
		(0..<4).forEach { adapter.add(NewsFragment(it)) }
		val binding = ActivityNewsBinding.inflate(layoutInflater).apply {
			pager.adapter = adapter
			TabLayoutMediator(tabLayout, pager) { tab: TabLayout.Tab?, position: Int -> tab?.text = arrayOf("资讯", "公众号", "通知", "今日中大")[position] }.attach()
			sugs.adapter = suggestionAdapter
			sugs.layoutManager = GridLayoutManager(this@NewsActivity, 1)
		}
		setContentView(binding.root)
		config.setCallback { suggestions }
		http = HttpManager(object : Handler(mainLooper) {
			override fun handleMessage(msg: Message) {
				val rdata = msg.getData()
				val isJSON = rdata.getBoolean("isJSON")
				val json = rdata.getString("data")
				if (json == null) {
					config.toast(R.string.no_net_connected)
					return
				}
				if (!isJSON) {
					if (!authorizationManager.isAuthorized(json)) {
						config.toast(R.string.login_warning)
						config.gotoLogin(if (authorizationManager.isAccessible) TargetUrl.NEWS else TargetUrl.NEWS_WEBVPN)
						return
					}
					if (!authorizationManager.isAccessible(json)) {
						config.toast(R.string.educational_wifi_warning)
						suggestions
						return
					}
				}
				val data = JSONObject.parseObject(json)
				when (data.get("code")) {
					"0000" -> {
						if (msg.what == 1) {
							suggestionAdapter.clear()
							data.getJSONObject("data")
								.getJSONArray("suggests")
								.forEach { suggestionAdapter.add(it as String?) }
						} else if (data.get("code") == 496) {
							config.toast(data.getString("message"))
							config.gotoLogin(if (authorizationManager.isAccessible) TargetUrl.NEWS else TargetUrl.NEWS_WEBVPN)
						} //suggestion
					}
					else -> {
						config.toast(data.getString("code") + "\n" + data.getString("message"))
					}
				}
			}
		}).apply {
			setParams(this@NewsActivity.config)
			setAuthorizationRequired(true)
			setAuthorizationJar(AuthorizationJar(this@NewsActivity))
		}
		edit = binding.searchView.editText.apply {
			setOnEditorActionListener { _: TextView?, _: Int, _: KeyEvent? ->
				binding.searchView.hide()
				false
			}
			addTextChangedListener(object : TextWatcher {
				override fun beforeTextChanged(charSequence: CharSequence?,
				                               i: Int,
				                               i1: Int,
				                               i2: Int) {
				}
				
				override fun onTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {
					if ("$text".isNotEmpty()) getSuggestions("$text")
				}
				
				override fun afterTextChanged(editable: Editable?) {
				}
			})
		}
	}
	
	val suggestions: Unit
		get() {
			getSuggestions("${edit.text}")
		}
	
	fun getSuggestions(keyword: String) {
		http.postRequest(authorizationManager.host + "ai_service/search-server/needle/suggest", "{\"aliasName\":\"collection_data\",\"keyWord\":\"$keyword\"}", 1)
	}
	
	internal class SuggestionAdapter : RecyclerAdapter<String?>() {
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
			return object : RecyclerView.ViewHolder(LayoutInflater.from(parent.context)
														.inflate(R.layout.item_sug, parent, false)) {}
		}
		
		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
			(holder.itemView as TextView).text = trim(get(position))
			super.onBindViewHolder(holder, position)
		}
	}
}