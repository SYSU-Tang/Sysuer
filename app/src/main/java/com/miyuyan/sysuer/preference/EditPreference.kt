package com.miyuyan.sysuer.preference

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.databinding.PreferenceEditBinding

class EditPreference(context: Context,
                     attrs: AttributeSet? = null,
                     defStyleAttr: Int = android.R.attr.editTextPreferenceStyle,
                     defStyleRes: Int = 0) : Preference(context, attrs, defStyleAttr, defStyleRes) {
	constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
	
	init {
		layoutResource = R.layout.preference_edit
	}
	
	private var internalValueChange = false
	override fun onBindViewHolder(holder: PreferenceViewHolder) {
		PreferenceEditBinding.bind(holder.itemView).apply {
			textInputLayout.hint = title
			textInputLayout.startIconDrawable = getIcon()
			textField.tag?.let {
				if (it is TextWatcher) {
					textField.removeTextChangedListener(it)
				}
			}
			val currentText = value ?: ""
			root.setOnClickListener { onPreferenceClickListener?.onPreferenceClick(this@EditPreference) }
			if (textField.text.toString() != currentText) {
				textField.setText(currentText)
			}
			val watcher = object : TextWatcher {
				override fun afterTextChanged(s: Editable?) {}
				override fun beforeTextChanged(s: CharSequence?,
				                               start: Int,
				                               count: Int,
				                               after: Int) {
				}
				
				override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
					val newValue = "$s"
					if (value != newValue) {
						internalValueChange = true
						value = newValue
						internalValueChange = false
					}
				}
			}
			textField.addTextChangedListener(watcher)
			textField.tag = watcher
		}
	}
	
	var value: String? = null
		set(value) {
			if (field != value) {
				field = value
				persistString(value)
				if (!internalValueChange) {
					notifyChanged()
				}
			}
		}
	
	override fun onSetInitialValue(defaultValue: Any?) {
		value = getPersistedString(defaultValue as? String)
	}
}
