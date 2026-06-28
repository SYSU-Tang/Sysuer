package com.sysu.edu.life

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.viewbinding.ViewBinding
import com.alibaba.fastjson2.JSONObject
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.sysu.edu.BaseFragment
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.databinding.ItemFieldBinding
import com.sysu.edu.databinding.RecyclerViewScrollBinding
import com.sysu.edu.model.GymModel
import com.sysu.edu.view.AdapterListener
import com.sysu.edu.view.RecyclerAdapter

class GymListFragment : BaseFragment() {
	lateinit var layoutManager: StaggeredGridLayoutManager
	lateinit var binding: RecyclerViewScrollBinding
	lateinit var model: GymModel
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		super.onCreateView(inflater, container, savedInstanceState)
		model = GymModel(requireContext())
		layoutManager = StaggeredGridLayoutManager(config.column, StaggeredGridLayoutManager.VERTICAL)
		viewModel = ViewModelProvider(requireActivity())[GymReservationViewModel::class.java]
		val fieldAdapter = FieldAdapter().apply {
			action = { id: String? ->
				findNavController(binding.root).navigate(R.id.campus_to_field, Bundle().apply {
					putString("id", id)
					putInt("code", requireArguments().getInt("code") + 1)
				})
			}
			setParams(config)
			setListener(object : AdapterListener {
				override fun onBind(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>?,
				                    holder: RecyclerView.ViewHolder?,
				                    position: Int) {
					holder?.let { ItemFieldBinding.bind(it.itemView) }.apply {
						get(position)?.getString("ImageUrl")?.takeIf { it.isNotEmpty() }?.let {
							Glide.with(requireContext())
								.load(GlideUrl(it, LazyHeaders.Builder()
									.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
									.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
									.addHeader("Cookie", model.cookie)
									.addHeader("Authorization", model.authorization)
									.build()))
								.skipMemoryCache(true)
								.diskCacheStrategy(DiskCacheStrategy.NONE)
								.into(this?.image!!)
						}
					}
				}
				
				override fun onCreate(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>?,
				                      binding: ViewBinding?) {
				}
			})
		}
		binding = RecyclerViewScrollBinding.inflate(inflater, container, false).apply {
			root.layoutManager = layoutManager
			root.adapter = fieldAdapter
		}
		model.message.observe(requireActivity(), Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			message.second.getJSONArray("data")?.takeUnless { it.isEmpty() }?.let {
				when (message.first) {
					1 -> it.forEach { e -> fieldAdapter.add(e as JSONObject) }
					2 -> it.forEach { e ->
						if ((e as JSONObject).getString("Campus") == requireArguments().getString("id")) fieldAdapter.add(e)
					}
				}
			}
		})
		info
		return binding.root
	}
	
	private val info: Unit
		get() {
			if (requireArguments().getInt("code") == 0) campus
			else venue
		}
	
	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)
		layoutManager.setSpanCount(config.column)
	}
	
	val campus: Unit
		get() {
			model.addAndNext("api/Campus/active", 1)
		}
	val venue: Unit
		get() {
			model.addAndNext("api/venuetype/all", 2)
		}
	
	private class FieldAdapter : RecyclerAdapter<JSONObject>() {
		var action: ((String?) -> Unit)? = null
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
			return object :
				RecyclerView.ViewHolder(ItemFieldBinding.inflate(LayoutInflater.from(parent.context), parent, false).root) {}
		}
		
		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
			val item = get(position)
			ItemFieldBinding.bind(holder.itemView).apply {
				title.text = item.getString("Name")
				root.setOnClickListener { action?.invoke(item.getString("Identity")) }
			}
			
			super.onBindViewHolder(holder, position)
		}
	}
	
	companion object {
		var viewModel: GymReservationViewModel? = null
	}
}
