package com.sysu.edu.preference

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.withStyledAttributes
import androidx.lifecycle.MutableLiveData
import androidx.preference.ListPreference
import androidx.preference.PreferenceViewHolder
import com.sysu.edu.R
import com.sysu.edu.databinding.PreferenceFilterBinding

class FilterPreference(context: Context,
                       attrs: AttributeSet? = null,
                       defStyleAttr: Int = R.attr.filterPreferenceStyle,
                       defStyleRes: Int = 0) : ListPreference(context, attrs, defStyleAttr, defStyleRes) {
	
	constructor(context: Context, attrs: AttributeSet? = null) : this(context, attrs, R.attr.filterPreferenceStyle)
	
	val valueLiveData: MutableLiveData<String?> = MutableLiveData<String?>()
	var isFilter: Boolean = false
	var canEdit: Boolean = false
	var textWatcher: TextWatcher? = null
	
	init {
		layoutResource = R.layout.preference_filter
		try {
			context.withStyledAttributes(attrs, R.styleable.filterPreferenceStyle, defStyleAttr, defStyleRes) {
				valueLiveData.value = getString(R.styleable.filterPreferenceStyle_value)
				isFilter = getBoolean(R.styleable.filterPreferenceStyle_isFilter, true)
				canEdit = getBoolean(R.styleable.filterPreferenceStyle_canEdit, false)
			}
		} catch (_: Exception) { //            throw new RuntimeException(e);
		}
	}
	
	override fun onBindViewHolder(holder: PreferenceViewHolder) {
		super.onBindViewHolder(holder)
		PreferenceFilterBinding.bind(holder.itemView).apply {
			textInputLayout.startIconDrawable = getIcon()
			textInputLayout.hint = title
			textField.setText(valueLiveData.getValue(), isFilter)
			textField.setInputType(if (canEdit) InputType.TYPE_CLASS_TEXT else InputType.TYPE_NULL)
			textField.setSelection(textField.getText().length)
			if (entries != null) textField.setAdapter<ArrayAdapter<CharSequence?>?>(ArrayAdapter(context, android.R.layout.simple_list_item_1, entries))
			textField.setOnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
				setValueIndex(position)
				valueLiveData.value = "${entries[position]}"
			}        /*
        if (textField.isFocused())
            textField.showDropDown();*/
			
			textField.addTextChangedListener(textWatcher ?: object : TextWatcher {
				override fun afterTextChanged(s: Editable?) {
				}
				
				override fun beforeTextChanged(s: CharSequence?,
				                               start: Int,
				                               count: Int,
				                               after: Int) {
				}
				
				override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
					if ("$s" != valueLiveData.getValue()) valueLiveData.value = "$s"
				}
			})
			root.setOnClickListener { textField.showDropDown() }
		}
	}
	
	override fun onClick() {
	}
	
	fun setIsFilter(filter: Boolean) {
		isFilter = filter
		notifyChanged()
	}
	
	override fun setEntries(entries: Array<CharSequence?>?) {
		super.setEntries(entries)
		notifyChanged()
	}
}
