package com.miyuyan.sysuer.view

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

interface AdapterListener {
	fun onBind(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
	           holder: RecyclerView.ViewHolder,
	           position: Int)
	
	fun onCreate(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>, binding: ViewBinding?)
}

internal interface Adapter2Listener<V : ViewDataBinding?> {
	fun onBind(adapter: RecyclerView.Adapter<RecyclerViewHolder<V?>?>?,
	           holder: RecyclerViewHolder<V?>?,
	           position: Int)
	
	fun onCreate(adapter: RecyclerView.Adapter<RecyclerViewHolder<V?>?>?, binding: V?)
}