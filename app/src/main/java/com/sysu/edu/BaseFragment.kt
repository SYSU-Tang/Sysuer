package com.sysu.edu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.sysu.edu.api.Config

open class BaseFragment : Fragment() {
	lateinit var config: Config
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View? {
		config = Config(this)
		return super.onCreateView(inflater, container, savedInstanceState)
	}
	
	override fun onDestroy() {
		super.onDestroy()
		if (::config.isInitialized) config.contextUtil.disposable.dispose()
	}
}