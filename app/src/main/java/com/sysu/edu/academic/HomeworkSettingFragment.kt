package com.sysu.edu.academic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.sysu.edu.BaseFragment
import com.sysu.edu.databinding.FragmentHomeworkMainBinding

class HomeworkSettingFragment : BaseFragment() {
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		val binding = FragmentHomeworkMainBinding.inflate(getLayoutInflater())
		return binding.root
	}
}
