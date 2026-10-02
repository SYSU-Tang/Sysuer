package com.miyuyan.sysuer.view

import android.content.Context
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import com.miyuyan.preference.R
import com.miyuyan.preference.simplemenu.SimpleMenuPopupWindow

class MaterialPopupMenu<T>(context: Context) {
	private val names = mutableListOf<String>()
	private val values = mutableListOf<T>()
	var name
		get() = names.getOrNull(selectedIndex)
		set(n) {
			selectedIndex = names.indexOf(n)
		}
	var value
		get() = values.getOrNull(selectedIndex)
		set(v) {
			selectedIndex = values.indexOf(v)
		}
	var selectedIndex: Int = -1
		set(i) {
			if (i != field) {
				field = i
				pop.selectedItemIndex = i
				onValueChange?.invoke(value)
				onSelectChange?.invoke(selectedIndex)
				onNameChange?.invoke(name)
			}
		}
	val pop = SimpleMenuPopupWindow(
			ContextThemeWrapper(
					context, R.style.ThemeOverlay_Preference_SimpleMenuPreference_PopupMenu
			),
			null,
			R.styleable.SimpleMenuPreference_android_popupMenuStyle,
			rikka.material.preference.R.style.Widget_Preference_SimpleMenuPreference_PopupMenu_Material3
	).apply {
		onItemClickListener = {
			selectedIndex = it
			dismiss()
		}
		selectedItemIndex = this@MaterialPopupMenu.selectedIndex
	}

	fun show(
		anchor: View, container: View = anchor.parent as View, offset: Int = anchor.x.toInt()
	) {
		pop.show(anchor, container, offset)
	}

	/**
	 * @param alignView 弹窗左边缘对齐到的参考 View（如列表项的标题），
	 * 用窗口坐标计算偏移，避免嵌套布局内边距导致的错位
	 */
	fun show(anchor: View, container: View = anchor.parent as View, alignView: View) {
		val viewLocation = IntArray(2)
		val containerLocation = IntArray(2)
		alignView.getLocationInWindow(viewLocation)
		container.getLocationInWindow(containerLocation)
		pop.show(anchor, container, viewLocation[0] - containerLocation[0])
	}

	fun addItem(item: String) {
		names.add(item)
		pop.entries = names.toTypedArray()
		pop.requestMeasure()
	}

	fun addItem(item: String, value: T) {
		names.add(item)
		values.add(value)
		pop.entries = names.toTypedArray()
		pop.requestMeasure()
	}

	fun setItems(entries: List<String>, reset: Boolean = false) {
		names.clear()
		if (reset) selectedIndex = -1
		names.addAll(entries)
		pop.entries = names.toTypedArray()
		pop.requestMeasure()
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
		selectedIndex = -1
		names.clear()
		values.clear()
	}

	var onValueChange: ((T?) -> Unit)? = null
	var onSelectChange: ((Int?) -> Unit)? = null
	var onNameChange: ((String?) -> Unit)? = null
}