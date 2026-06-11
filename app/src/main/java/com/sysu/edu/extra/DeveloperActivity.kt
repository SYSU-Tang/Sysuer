package com.sysu.edu.extra

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sysu.edu.databinding.ActivityDeveloperBinding

class DeveloperActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		ActivityDeveloperBinding.inflate(layoutInflater).apply {
			setContentView(root)
		}
	}
}