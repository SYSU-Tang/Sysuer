package com.miyuyan.sysuer.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import rikka.preference.simplemenu.SimpleMenuPopupWindow

@SuppressLint("RestrictedApi")
class MaterialPopupMenu<T>(context: Context) {
	private val names = mutableListOf<String>()
	private val values = mutableListOf<T>()
	var name
		get() = selectedIndex?.let { names.getOrNull(it) }
		set(n) {
			selectedIndex = names.indexOf(n)
		}
	var value
		get() = selectedIndex?.let { values.getOrNull(it) }
		set(v) {
			selectedIndex = values.indexOf(v)
		}
	var selectedIndex: Int? = null
		set(i) {
			if (i in names.indices) {
				field = i
				i?.let { pop.setSelectedIndex(it) }
				onValueChange?.invoke(value)
				onSelectChange?.invoke(selectedIndex)
				onNameChange?.invoke(name)
			}
		}
	val pop = SimpleMenuPopupWindow(
			ContextThemeWrapper(
					context,
					rikka.preference.simplemenu.R.style.ThemeOverlay_Preference_SimpleMenuPreference_PopupMenu
			),
			null,
			rikka.preference.simplemenu.R.styleable.SimpleMenuPreference_android_popupMenuStyle,
			rikka.material.preference.R.style.Widget_Preference_SimpleMenuPreference_PopupMenu_Material3
	).apply {
		onItemClickListener = {
			selectedIndex = it
			dismiss()
		}
		setSelectedIndex(this@MaterialPopupMenu.selectedIndex ?: -1)
	}

	fun show(
		anchor: View,
		container: View? = anchor.parent as View?,
		offset: Int = anchor.x.toInt()
	) {
		pop.show(anchor, container, offset)
	}

	fun addItem(item: String) {
		names.add(item)
		pop.setEntries(names.toTypedArray())
	}

	fun addItem(item: String, value: T) {
		names.add(item)
		values.add(value)
		pop.setEntries(names.toTypedArray())
	}
	fun setItems(entries: List<String>, reset: Boolean = false) {
		names.clear()
		if (reset) selectedIndex = null
		names.addAll(entries)
		pop.setEntries(entries.toTypedArray())
	}

	fun setValues(entryValues: List<T>) {
		values.clear()
		values.addAll(entryValues)
	}

	fun select(index: Int) {
		selectedIndex = index
	}

	fun dismiss() {
		pop.dismiss()
	}

	fun clear() {
		selectedIndex = null
		names.clear()
		values.clear()
	}

	var onValueChange: ((T?) -> Unit)? = null
	var onSelectChange: ((Int?) -> Unit)? = null
	var onNameChange: ((String?) -> Unit)? = null
}