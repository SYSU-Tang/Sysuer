package com.sysu.edu.preference

import android.content.Context
import android.util.AttributeSet
import rikka.preference.SimpleMenuPreference

class MenuPreference : SimpleMenuPreference {
	constructor(context: Context) : super(context)
	constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
	constructor(context: Context,
	            attrs: AttributeSet?,
	            defStyleAttr: Int) : super(context, attrs, defStyleAttr)
	
	constructor(context: Context,
	            attrs: AttributeSet?,
	            defStyleAttr: Int,
	            defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)
	
	override fun onSetInitialValue(defaultValue: Any?) {
		super.onSetInitialValue(defaultValue)
		setSummary(entries[value.toInt()])
	}
	
	override fun persistString(value: String): Boolean {
		setSummary(entries[value.toInt()])
		return super.persistString(value)
	}
}
