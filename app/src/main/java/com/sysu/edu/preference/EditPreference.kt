package com.sysu.edu.preference

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil.trim
import com.sysu.edu.databinding.PreferenceEditBinding

class EditPreference(context: Context,
                     attrs: AttributeSet? = null,
                     defStyleAttr: Int = android.R.attr.editTextPreferenceStyle,
                     defStyleRes: Int = 0) : Preference(context, attrs, defStyleAttr, defStyleRes) {
	constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0, 0)
	
	private var mValue: String? = null
	
	init {
		layoutResource = R.layout.preference_edit
	}
	
	override fun onBindViewHolder(holder: PreferenceViewHolder) {
		PreferenceEditBinding.bind(holder.itemView).apply {
			textInputLayout.hint = title
			textInputLayout.startIconDrawable = getIcon()
			textField.setText(value)
			textField.addTextChangedListener(object : TextWatcher {
				override fun afterTextChanged(s: Editable?) {
				}
				
				override fun beforeTextChanged(s: CharSequence?,
				                               start: Int,
				                               count: Int,
				                               after: Int) {
				}
				
				override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
					value = "$s"
				}
			})
		}
	}
	
	var value: String?
		get() = mValue
		set(value) {
			mValue = value
			persistString(value)
		}
	
	override fun onSetInitialValue(defaultValue: Any?) {
		value = getPersistedString(trim(defaultValue as String?))
	}
}
