package com.miyuyan.sysuer.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.databinding.ItemPreferenceBinding

class PreferenceAdapter : RecyclerAdapter<JSONObject>() {
	var hideNull: Boolean = false
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		return object : RecyclerView.ViewHolder(ItemPreferenceBinding.inflate(LayoutInflater.from(
			parent.context), parent, false).root) {}
	}
	
	fun set(titles: MutableList<Int?>,
	        contents: MutableList<String?>,
	        icons: MutableList<Int?>,
	        context: Context) {
		clear()
		titles.indices.forEach { add(context.getString(titles[it]!!), contents[it], icons[it]) }
	}
	
	fun add(title: String?, content: String?, icon: Int?) {
		add(JSONObject.of("title", title, "content", content, "icon", icon))
	}
	
	fun set(titles: MutableList<String?>,
	        contents: MutableList<String?>,
	        icons: MutableList<Int?>) {
		clear()
		titles.indices.forEach {
			add(titles.getOrNull(it), contents.getOrNull(it), icons.getOrNull(it))
		}
	}
	
	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		//val pos = holder.getBindingAdapterPosition()
		val item = get(position)
		val content = item.getString("content", "")
		ItemPreferenceBinding.bind(holder.itemView).apply {
			itemTitle.text = item.getString("title", "")
			itemContent.text = if (content.isNullOrEmpty()) holder.itemView.context.getString(R.string.none) else content
			root.setOnClickListener {}
			itemContent.visibility = if (hideNull && content.isNullOrEmpty()) View.GONE else View.VISIBLE
			if (item.getInteger("icon") != null) itemIcon.setImageResource(item.getInteger("icon")) //        else itemIcon.setImageResource(R.drawable.account);
			root.updateAppearance(position, itemCount)
		}
		super.onBindViewHolder(holder, position)
	}
}
