package com.sysu.edu.life

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson2.JSONObject
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.sysu.edu.BaseFragment
import com.sysu.edu.R
import com.sysu.edu.api.AuthorizationJar
import com.sysu.edu.api.AuthorizationManager
import com.sysu.edu.api.CommonUtil.getHost
import com.sysu.edu.api.CommonUtil.toStringOrDefault
import com.sysu.edu.api.CommonUtil.trim
import com.sysu.edu.api.HttpManager
import com.sysu.edu.api.TargetUrl
import com.sysu.edu.browser.BrowserActivity
import com.sysu.edu.databinding.ItemNewsBinding
import com.sysu.edu.databinding.RecyclerViewScrollBinding
import com.sysu.edu.view.RecyclerAdapter

class NewsFragment(val position: Int) : BaseFragment() {
	val authorizationManager: AuthorizationManager = AuthorizationManager("https://iportal.sysu.edu.cn/", "https://iportal-443.webvpn.sysu.edu.cn/")
	val run: () -> Unit
	lateinit var http: HttpManager
	lateinit var binding: RecyclerViewScrollBinding
	var page: Int = 1
	
	init {
		run = listOf(this::news, this::subscription, this::notice, this::dailyNews)[position]
	}
	
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		if (savedInstanceState == null) {
			super.onCreateView(inflater, container, savedInstanceState)
			val newsAdapter = NewsAdapter().apply {
				setParams(config)
			}
			config.setCallback(run)
			binding = RecyclerViewScrollBinding.inflate(inflater).apply {
				recyclerView.layoutManager = GridLayoutManager(requireContext(), config.column)
				recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
					override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
						if (!recyclerView.canScrollVertically(1) && position != 0 && dy > 0) run()
					}
				})
				recyclerView.adapter = newsAdapter
			}
			http = HttpManager(object : Handler(Looper.getMainLooper()) {
				override fun handleMessage(msg: Message) {
					val json = msg.obj as String?
					if (json == null || msg.what == -1) config.toast(R.string.no_net_connected)
					else if (!msg.getData().getBoolean("isJSON")) {
						if (!authorizationManager.isAuthorized(json)) config.gotoLogin(if (authorizationManager.isAccessible) TargetUrl.NEWS else TargetUrl.NEWS_WEBVPN)
						else if (!authorizationManager.isAccessible(json)) run()
					} else {
						val response = JSONObject.parseObject(json)
						when (val code = response.getInteger("code")) {
							10000 -> {
								(if (msg.what == 3) response.getJSONArray("data") else response.getJSONObject("data")
									.getJSONArray("records")).forEach { item: Any? ->
									var image = ""
									(item as JSONObject).getJSONArray("coversPicList")
										?.takeIf {
											it.isNotEmpty() && !it.getJSONObject(0)
												.isNullOrEmpty() && !it.getJSONObject(0)
												.getString("outLink")
												?.also { link ->
													image = link
												}
												.isNullOrEmpty()
										}
										?.let {
											newsAdapter.add(mutableMapOf("title" to item.getString("title"), "image" to image, "url" to item.getString("url"), "time" to item.getString("createTime"), "source" to item.getJSONObject("source")
												.getString("seedName")))
										}
								}
							}
							10003 -> {
								config.toast("$code ${response.getString("message")}")
								config.gotoLogin(if (authorizationManager.isAccessible) TargetUrl.NEWS else TargetUrl.NEWS_WEBVPN)
							}
							496, 497 -> {
								config.toast(response.getString("message"))
								config.gotoLogin(if (authorizationManager.isAccessible) TargetUrl.NEWS else TargetUrl.NEWS_WEBVPN)
							}
						}
					} //今日中大
				}
			}).apply {
				setAuthorizationRequired(true)
				setAuthorizationJar(AuthorizationJar(requireContext()))
				setParams(this@NewsFragment.config)
			}
		}
		run()
		return binding.root
	}
	
	val news: Unit
		get() {
			http.postRequest(authorizationManager.host + "ai_service/content-portal/recommend/query-recommend", "", 3)
		}
	val subscription: Unit
		get() {
			baseRequest("3ytr4e6c", 2)
		}
	val notice: Unit
		get() {
			baseRequest("3ytunvv6", 4)
		}
	val dailyNews: Unit
		get() {
			baseRequest("4cef8rqw", 5)
		}
	
	fun baseRequest(code: String?, what: Int) {
		http.postRequest(authorizationManager.host + "ai_service/content-portal/user/content/page", "{\"pageSize\":20,\"currentPage\":" + page++ + ",\"apiCode\":\"" + code + "\",\"notice\":false}", what)
	}
	
	internal class NewsAdapter : RecyclerAdapter<MutableMap<String?, String?>>() {
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
			return object :
				RecyclerView.ViewHolder(ItemNewsBinding.inflate(LayoutInflater.from(parent.context), parent, false).root) {}
		}
		
		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
			val binding = ItemNewsBinding.bind(holder.itemView)
			val item = get(position)
			val context = holder.itemView.context
			holder.itemView.setOnClickListener { v: View? ->
				context.startActivity(Intent(context, BrowserActivity::class.java).setData(Uri.parse(item["url"])), ActivityOptionsCompat.makeSceneTransitionAnimation(context as Activity, v!!, "miniapp")
					.toBundle())
			}
			binding.title.text = item.getOrDefault("title", "")
			binding.content.text = "#${item.getOrDefault("source", "")} #${item.getOrDefault("time", "")}"
			val img = trim(item["image"])
			val authorizationJar = AuthorizationJar(context)
			if (!img.isEmpty()) Glide.with(context)
				.load(GlideUrl(img, LazyHeaders.Builder()
					.addHeader("Cookie", toStringOrDefault(authorizationJar.getCookie(img)))
					.addHeader("Authorization", toStringOrDefault(authorizationJar.getAuthorization(getHost(img))))
					.build()))
				.timeout(15000)
				.override(config?.dpToPx(120)?:360, config?.dpToPx(120)?:360)
				.optionalFitCenter()
				.transform(RoundedCorners(16))
				.into(binding.image)
			super.onBindViewHolder(holder, position)
		}
	}
}