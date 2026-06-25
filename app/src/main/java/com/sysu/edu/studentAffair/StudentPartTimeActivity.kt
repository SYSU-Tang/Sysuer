package com.sysu.edu.studentAffair

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.GravityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI.onNavDestinationSelected
import androidx.navigation.ui.NavigationUI.setupWithNavController
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil.isEmpty
import com.sysu.edu.databinding.ActivityStudentPartTimeBinding
import com.sysu.edu.view.EditTextDialog

class StudentPartTimeActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val binding = ActivityStudentPartTimeBinding.inflate(layoutInflater)
		val viewModel = ViewModelProvider(this)[StudentPartTimeViewModel::class.java].apply {
			campusPop = PopupMenu(this@StudentPartTimeActivity, binding.campus, 0, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow)
			typePop = PopupMenu(this@StudentPartTimeActivity, binding.jobType, 0, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow)
			yearPop = PopupMenu(this@StudentPartTimeActivity, binding.year, 0, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow)
			jobNameDialog = EditTextDialog(this@StudentPartTimeActivity)
			jobNameDialog!!.setTitle(R.string.job_name)
			jobNameDialog!!.setHint(R.string.job_name)
			unitDialog = EditTextDialog(this@StudentPartTimeActivity)
			unitDialog!!.setTitle(R.string.employ_unit)
			unitDialog!!.setHint(R.string.employ_unit)
			yearName.observe(this@StudentPartTimeActivity, Observer { year: String? -> binding.year.text = if (isEmpty(year)) getString(R.string.year) else year })
			campusName.observe(this@StudentPartTimeActivity, Observer { campus: String? -> binding.campus.text = if (isEmpty(campus)) getString(R.string.campus) else campus })
			jobTypeName.observe(this@StudentPartTimeActivity, Observer { jobType: String? -> binding.jobType.text = if (isEmpty(jobType)) getString(R.string.job_type) else jobType })
			jobName.observe(this@StudentPartTimeActivity, Observer { jobName: String? -> binding.jobName.text = if (isEmpty(jobName)) getString(R.string.job_name) else jobName })
			unitName.observe(this@StudentPartTimeActivity, Observer { unit: String? -> binding.unit.text = if (isEmpty(unit)) getString(R.string.employ_unit) else unit })
		}
		setContentView(binding.root)
		val toggle = ActionBarDrawerToggle(this, binding.root, binding.toolbar, R.string.open, R.string.close)
		toggle.syncState()
		binding.root.addDrawerListener(toggle)
		val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment) as NavHostFragment
		val navController = navHostFragment.navController
		setupWithNavController(binding.navView, navController)
		binding.navView.setNavigationItemSelectedListener { item: MenuItem? ->
			onNavDestinationSelected(item!!, navController)
			binding.filter.animate()
				.alpha((if (item.itemId == R.id.recruitment_info) 1 else 0).toFloat())
				.withStartAction { binding.filter.visibility = View.VISIBLE }
				.withEndAction { binding.filter.visibility = if (item.itemId == R.id.recruitment_info) View.VISIBLE else View.GONE }
			binding.root.closeDrawer(GravityCompat.START, true)
			true
		}
		binding.year.setOnClickListener { viewModel.yearPop!!.show() }
		binding.campus.setOnClickListener { viewModel.campusPop!!.show() }
		binding.jobType.setOnClickListener { viewModel.typePop!!.show() }
		binding.jobName.setOnClickListener { viewModel.jobNameDialog!!.show() }
		binding.unit.setOnClickListener { viewModel.unitDialog!!.show() }
	}
}
