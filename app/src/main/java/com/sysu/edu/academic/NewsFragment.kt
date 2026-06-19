package com.sysu.edu.academic

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.api.Config
import com.sysu.edu.databinding.ItemNewsBinding
import com.sysu.edu.databinding.RecyclerViewScrollBinding
import com.sysu.edu.view.AdapterListener
import com.sysu.edu.view.RecyclerAdapter

class NewsFragment : Fragment() {
	val newsAdapter: NewsAdapter = NewsAdapter()
	var staggeredGridLayoutManager: StaggeredGridLayoutManager? = null
	var config: Config? = null
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		val binding = RecyclerViewScrollBinding.inflate(inflater)
		config = Config(this)
		staggeredGridLayoutManager = StaggeredGridLayoutManager(config!!.column, StaggeredGridLayoutManager.VERTICAL)
		binding.getRoot().setLayoutManager(staggeredGridLayoutManager)
		binding.getRoot().setAdapter(newsAdapter)
		return binding.getRoot()
	}
	
	fun add(json: JSONObject?) {
		newsAdapter.add(json)
	}
	
	fun setListener(listener: AdapterListener?) {
		newsAdapter.setListener(listener)
	}
	
	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)
		staggeredGridLayoutManager!!.setSpanCount(config!!.column)
	}
	
	class NewsAdapter : RecyclerAdapter<JSONObject?>() {
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
			return object :
				RecyclerView.ViewHolder(ItemNewsBinding.inflate(LayoutInflater.from(parent.context))
											.getRoot()) {}
		}
		
		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
			ItemNewsBinding.bind(holder.itemView).apply {
				title.text = get(position)?.getString("title")
				content.text = get(position)?.getString("deliveryDate")
			}			// AppCompatImageView image = holder.itemView.findViewById(R.id.image);			//            Drawable latest = AppCompatResources.getDrawable(context,R.drawable.latest);			//            if (latest != null) {			//                latest.setBounds(0,0,72,72);			//            }			//            title.setCompoundDrawablePadding(12);
			//            title.setCompoundDrawables(latest,null,null,null);
			//        }		//        String img = data.get(position).get("image");
			//        if (img != null && !img.isEmpty()) {
			//            Glide.with(context).load(img)
			//                    // .diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true)
			//                    .placeholder(R.drawable.logo)
			//                    .override(400).fitCenter().transform(new RoundedCorners(16))
			//                    .into(image);
			//        }
			super.onBindViewHolder(holder, position)
		}
	}
}
