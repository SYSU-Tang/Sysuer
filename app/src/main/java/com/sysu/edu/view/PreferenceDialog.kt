package com.sysu.edu.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.sysu.edu.databinding.DialogKeyValueBinding

class PreferenceDialog(context: Context) {
	private val dialog: BottomSheetDialog = BottomSheetDialog(context)
	private val binding: DialogKeyValueBinding = DialogKeyValueBinding.inflate(LayoutInflater.from(context))
	private lateinit var adapter: PreferenceAdapter
	
	init {
		dialog.setContentView(binding.root)
		init(context)
	}
	
	private fun init(context: Context?) {
		binding.recyclerView.root.layoutManager=LinearLayoutManager(context)
		adapter = PreferenceAdapter()
		binding.recyclerView.root.adapter = adapter
	}
	
	fun show() {
		dialog.show()
	}
	
	fun dismiss() {
		dialog.dismiss()
	}
	
	fun getAdapter(): PreferenceAdapter {
		return adapter
	}
	
	fun setPositiveButton(text: String?, onClick: View.OnClickListener?) {
		binding.positive.visibility = View.VISIBLE
		binding.positive.text = text
		binding.positive.setOnClickListener(onClick)
	}
	
	fun setNegativeButton(text: String?, onClick: View.OnClickListener?) {
		binding.negative.visibility = View.VISIBLE
		binding.negative.text = text
		binding.negative.setOnClickListener(onClick)
	}
	
	fun add(title: String?, content: String?, icon: Int?) {
		adapter.add(title, content, icon)
	}
	
	fun clear() {
		adapter.clear()
	}
}
