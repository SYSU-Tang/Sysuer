package com.sysu.edu.todo

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.sysu.edu.BaseActivity
import com.sysu.edu.databinding.ActivityTodoBinding

class TodoActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val binding = ActivityTodoBinding.inflate(layoutInflater).apply {
			toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
			add.setOnClickListener { (fragment.getFragment<Fragment?>() as TodoFragment).todoManager.showTodoAddDialog() }
		}
		setContentView(binding.root)
	}
}
