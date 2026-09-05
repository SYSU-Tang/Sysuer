package com.miyuyan.sysuer.view

import android.content.DialogInterface
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.miyuyan.sysuer.api.CommonUtil.toStringOrDefault
import com.miyuyan.sysuer.databinding.DialogEditTextBinding

class EditTextDialog(context: android.content.Context) {
	private val dialog: AlertDialog
	private val binding: DialogEditTextBinding = DialogEditTextBinding.inflate(LayoutInflater.from(context))
	var mValue: String? = null
	var listener: ValueChangeListener? = null
	
	init {
		dialog = MaterialAlertDialogBuilder(context).setView(binding.root)
			.setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int -> value = toStringOrDefault<Editable?>(binding.edit.getText()) }
			.setNegativeButton(android.R.string.cancel, null)
			.create()
		getEditText().addTextChangedListener(object : TextWatcher {
			override fun afterTextChanged(s: Editable?) {
			}
			
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
			}
			
			override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
				value = "$s"
			}
		})
	}
	
	var value: String?
		get() = mValue
		set(value) {
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
		binding.root.setLayoutParams(params)
	}
	
	fun setTitle(title: Int) {
		dialog.setTitle(title)
	}
	
	fun setTitle(title: String?) {
		dialog.setTitle(title)
	}
	
	fun setHint(hint: Int) {
		binding.editLayout.setHint(hint)
	}
	
	fun setValueChangeListener(listener: ValueChangeListener?) {
		this.listener = listener
	}
	
	fun getText(): String = getEditText().text.toString()
	fun getDialog(): AlertDialog = dialog
	fun getEditText(): TextInputEditText = binding.edit
	interface ValueChangeListener {
		fun onValueChange(value: String?)
	}
}
