package com.miyuyan.sysuer.preference

import android.content.Context
import android.util.AttributeSet
import rikka.preference.SimpleMenuPreference

class MenuPreference @JvmOverloads constructor(context: Context,
                                               attrs: AttributeSet? = null,
                                               defStyleAttr: Int = rikka.preference.simplemenu.R.attr.simpleMenuPreferenceStyle,
                                               defStyleRes: Int = 0) :
	SimpleMenuPreference(context, attrs, defStyleAttr, defStyleRes) {
	override fun onSetInitialValue(defaultValue: Any?) {
		super.onSetInitialValue(defaultValue)
		setSummary(entries[value.toInt()])
	}
	
	override fun persistString(value: String): Boolean {
		setSummary(entries[value.toInt()])
		return super.persistString(value)
	}
}
