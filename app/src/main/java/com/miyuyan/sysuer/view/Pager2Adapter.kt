package com.miyuyan.sysuer.view

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class Pager2Adapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
	val fragments: MutableList<Fragment> = mutableListOf()
	override fun createFragment(position: Int): Fragment = fragments[position]
	fun add(e: Fragment): Pager2Adapter {
		fragments.add(e)
		notifyItemInserted(itemCount - 1)
		return this
	}
	fun get(position: Int): Fragment = fragments[position]
	override fun getItemCount(): Int = fragments.size
	val isEmpty: Boolean
		get() = fragments.isEmpty()
}
