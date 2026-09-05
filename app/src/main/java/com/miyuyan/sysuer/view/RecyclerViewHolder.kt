package com.miyuyan.sysuer.view

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView

open class RecyclerViewHolder<T : ViewDataBinding?>(binding: T) :
	RecyclerView.ViewHolder(binding!!.root) {
	val binding: T? = binding
}
