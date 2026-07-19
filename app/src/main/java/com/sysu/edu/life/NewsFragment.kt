package com.sysu.edu.life

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.alibaba.fastjson2.JSONObject
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.sysu.edu.BaseFragment
import com.sysu.edu.api.CommonUtil.trim
import com.sysu.edu.browser.BrowserActivity
import com.sysu.edu.databinding.ItemNewsBinding
import com.sysu.edu.databinding.RecyclerViewScrollBinding
import com.sysu.edu.model.IportalModel
import com.sysu.edu.view.AdapterListener
import com.sysu.edu.view.RecyclerAdapter

class NewsFragment : BaseFragment() {
	var position: Int = 0
	lateinit var binding: RecyclerViewScrollBinding
	var page: Int = 1
	val model: IportalModel by lazy { IportalModel(requireContext()) }
	
	companion object {
		fun getInstance(position: Int): NewsFragment {
			val newsFragment = NewsFragment()
			newsFragment.position = position
			return newsFragment
		}
	}
	
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		super.onCreateView(inflater, container, savedInstanceState)
		val newsAdapter = NewsAdapter().apply {
			setParams(config)
			listener = object : AdapterListener {
				override fun onBind(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
				                    holder: RecyclerView.ViewHolder,
				                    position: Int) {
					val binding = ItemNewsBinding.bind(holder.itemView)
					val item = get(position)
					val context = holder.itemView.context
					binding.root.setOnClickListener { v: View? ->
						context.startActivity(Intent(context, BrowserActivity::class.java).setData(
							Uri.parse(item["url"])),
						                      ActivityOptionsCompat.makeSceneTransitionAnimation(
							                      context as Activity,
							                      v!!,
							                      "miniapp").toBundle())
					}
					binding.title.text = item.getOrDefault("title", "")
					binding.content.text = "#${item.getOrDefault("source", "")} #${
						item.getOrDefault("time", "")
					}"
					val img = trim(item["image"])
					if (!img.isEmpty()) Glide.with(context)
						.load(GlideUrl(img,
						               LazyHeaders.Builder()
							               .addHeader("Cookie", model.cookie)
							               .addHeader("Authorization", model.authorization)
							               .build()))
						.timeout(15000)
						.override(config.dpToPx(120), config.dpToPx(120))
						.optionalFitCenter()
						.transform(RoundedCorners(16))
						.into(binding.image)
				}
				
				override fun onCreate(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
				                      binding: ViewBinding?) {
				}
			}
		}
		binding = RecyclerViewScrollBinding.inflate(inflater).apply {
			recyclerView.layoutManager = GridLayoutManager(requireContext(), config.column)
			recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
				override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
					if (!recyclerView.canScrollVertically(1) && position != 0 && dy > 0) get()
				}
			})
			recyclerView.adapter = newsAdapter
		}
		model.message.observe(viewLifecycleOwner) { (code, response) ->
			(if (code == 3) response.getJSONArray("data")
			else response.getJSONObject("data").getJSONArray("records")).forEach { item: Any? ->
				val image: String = (item as JSONObject).getJSONArray("coversPicList")?.let {
					if (it.isNotEmpty() && !it.getJSONObject(0).isNullOrEmpty()) it.getJSONObject(0)
						.getString("outLink")
					else ""
				} ?: ""
				newsAdapter.add(mutableMapOf("title" to item.getString("title"),
				                             "image" to image,
				                             "url" to item.getString("url"),
				                             "time" to item.getString("createTime"),
				                             "source" to item.getJSONObject("source")
					                             .getString("seedName")))
			}
		}
		get()
		return binding.root
	}
	
	val news: Unit
		get() {
			model.addAndNext("ai_service/content-portal/recommend/query-recommend", "", 3)
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
	
	fun get() {
		when (position) {
			0 -> news
			1 -> subscription
			2 -> notice
			3 -> dailyNews
		}
	}
	
	fun baseRequest(code: String?, what: Int) {
		model.addAndNext("ai_service/content-portal/user/content/page",
		                 "{\"pageSize\":20,\"currentPage\":${page++},\"apiCode\":\"$code\",\"notice\":false}",
		                 what)
	}
	
	internal class NewsAdapter : RecyclerAdapter<MutableMap<String?, String?>>() {
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
			return object : RecyclerView.ViewHolder(ItemNewsBinding.inflate(LayoutInflater.from(
				parent.context), parent, false).root) {}
		}
	}
	
	override fun onDestroyView() {
		super.onDestroyView()
		model.dispose()
	}
}