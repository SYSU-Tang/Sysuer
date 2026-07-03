package com.sysu.edu.preference

import android.content.Context
import android.util.AttributeSet
import rikka.preference.SimpleMenuPreference

class MenuPreference(context: Context,
                     attrs: AttributeSet? = null,
                     defStyleAttr: Int = rikka.preference.simplemenu.R.attr.simpleMenuPreferenceStyle,
                     defStyleRes: Int = 0) :
	SimpleMenuPreference(context, attrs, defStyleAttr, defStyleRes) {
	
	constructor(context: Context, attrs: AttributeSet? = null) : this(context, attrs, 0)
	
	override fun onSetInitialValue(defaultValue: Any?) {
		super.onSetInitialValue(defaultValue)
		setSummary(entries[value.toInt()])
	}
	
	override fun persistString(value: String): Boolean {
		setSummary(entries[value.toInt()])
		return super.persistString(value)
	}
}
