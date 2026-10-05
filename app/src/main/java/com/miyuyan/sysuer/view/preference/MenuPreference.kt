package com.miyuyan.sysuer.view.preference

import android.content.Context
import android.util.AttributeSet
import rikka.preference.SimpleMenuPreference
import rikka.preference.simplemenu.R

class MenuPreference @JvmOverloads constructor(context: Context,
                                               attrs: AttributeSet? = null,
                                               defStyleAttr: Int = R.attr.simpleMenuPreferenceStyle,
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
