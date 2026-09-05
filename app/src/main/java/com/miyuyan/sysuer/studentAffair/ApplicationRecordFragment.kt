package com.miyuyan.sysuer.studentAffair

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.miyuyan.sysuer.R

class ApplicationRecordFragment : Fragment() {
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View? {
		return inflater.inflate(R.layout.fragment_application_record, container, false)
	}
}