package com.miyuyan.sysuer.extra

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.miyuyan.sysuer.databinding.ActivityDeveloperBinding

class DeveloperActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		ActivityDeveloperBinding.inflate(layoutInflater).apply {
			setContentView(root)
		}
	}
}