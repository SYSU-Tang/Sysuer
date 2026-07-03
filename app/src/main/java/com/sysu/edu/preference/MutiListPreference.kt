package com.sysu.edu.preference

import android.content.Context
import android.content.DialogInterface
import android.util.AttributeSet
import androidx.preference.MultiSelectListPreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sysu.edu.R

class MutiListPreference(context: Context,
                         attrs: AttributeSet? = null,
                         defStyleAttr: Int = R.attr.dialogPreferenceStyle,
                         defStyleRes: Int = 0) :
	MultiSelectListPreference(context, attrs, defStyleAttr, defStyleRes) {
	constructor(context: Context,
	            attrs: AttributeSet?) : this(context, attrs, R.attr.dialogPreferenceStyle, 0)
	
	override fun onClick() {
		val dialogBuilder = MaterialAlertDialogBuilder(context)
		if (title != null) dialogBuilder.setTitle(title)
		if (positiveButtonText != null) dialogBuilder.setPositiveButton(positiveButtonText) { _: DialogInterface?, _: Int ->
			persistStringSet(values)
			notifyChanged()
		}
		if (negativeButtonText != null) dialogBuilder.setNegativeButton(negativeButtonText) { dialog: DialogInterface?, _: Int -> dialog!!.dismiss() }
		if (entries != null) dialogBuilder.setMultiChoiceItems(entries, getSelectedItems()) { _: DialogInterface?, which: Int, isChecked: Boolean ->
			values.remove("${entryValues[which]}")
			if (isChecked) values.add("${entryValues[which]}")
			persistStringSet(values)
		}
		dialogBuilder.show()
	}
}
