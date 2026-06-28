package com.sysu.edu.view

import com.sysu.edu.api.CommonUtil.toStringOrDefault
import com.sysu.edu.view.EditTextDialog.ValueChangeListener
import android.view.LayoutInflater
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.DialogInterface
import android.text.Editable
import android.text.TextWatcher
import android.widget.FrameLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class EditTextDialog(context: android.content.Context) {
	private val dialog: androidx.appcompat.app.AlertDialog
	private val binding: com.sysu.edu.databinding.DialogEditTextBinding
	var mValue: String? = ""
	var listener: ValueChangeListener? = null
	
	init {
		binding = com.sysu.edu.databinding.DialogEditTextBinding.inflate(LayoutInflater.from(context))
		dialog = MaterialAlertDialogBuilder(context).setView(binding.getRoot())
			.setPositiveButton(android.R.string.ok, DialogInterface.OnClickListener { _: DialogInterface?, _: kotlin.Int -> setValue(toStringOrDefault<Editable?>(binding.edit.getText())) })
			.setNegativeButton(android.R.string.cancel, null)
			.create()
		getEditText().addTextChangedListener(object : TextWatcher {
			override fun afterTextChanged(s: Editable?) {
			}
			
			override fun beforeTextChanged(s: kotlin.CharSequence?,
			                               start: kotlin.Int,
			                               count: kotlin.Int,
			                               after: kotlin.Int) {
			}
			
			override fun onTextChanged(s: kotlin.CharSequence,
			                           start: kotlin.Int,
			                           before: kotlin.Int,
			                           count: kotlin.Int) {
				setValue(s.toString())
			}
		})
	}
	
	fun getValue(): String? {
		return mValue
	}
	
	fun setValue(value: String?) {
		if (mValue != value) {
			mValue = value
			if (getText() != value) getEditText().setText(value ?: "")
			listener?.onValueChange(value)
		}
	}
	
	fun show() {
		dialog.show()
		val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
		params.setMargins(72, 32, 72, 0)
		binding.getRoot().setLayoutParams(params)
	}
	
	fun setTitle(title: kotlin.Int) {
		dialog.setTitle(title)
	}
	
	fun setTitle(title: String?) {
		dialog.setTitle(title)
	}
	
	fun setHint(hint: kotlin.Int) {
		binding.editLayout.setHint(hint)
	}
	
	fun setHint(hint: String?) {
		binding.editLayout.setHint(hint)
		getEditText().setContentDescription(hint)
	}
	
	fun setValueChangeListener(listener: ValueChangeListener?) {
		this.listener = listener
	}
	
	fun getText(): String {
		return getEditText().text.toString()
	}
	
	fun getDialog(): androidx.appcompat.app.AlertDialog {
		return dialog
	}
	
	fun getEditText(): TextInputEditText {
		return binding.edit
	}
	
	fun setPasswordMode() {
		binding.edit.setInputType(android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD)
		binding.editLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE)
	}
	
	interface ValueChangeListener {
		fun onValueChange(value: String?)
	}
}
