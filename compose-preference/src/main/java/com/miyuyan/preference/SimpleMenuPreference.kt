package com.miyuyan.preference

import android.content.Context
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.View
import androidx.core.content.withStyledAttributes
import androidx.preference.ListPreference
import androidx.preference.PreferenceViewHolder
import com.miyuyan.preference.simplemenu.SimpleMenuPopupWindow

/**
 * A version of [ListPreference] that use Simple Menus in Material Design 1 as drop down.
 * 
 * 
 * On pre-Lollipop, it will fallback to [ListPreference].
 */
class SimpleMenuPreference(
	context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int
) : ListPreference(context, attrs, defStyleAttr, defStyleRes) {
	private var mAnchor: View? = null
	private var mItemView: View? = null
	private var mPopupWindow: SimpleMenuPopupWindow? = null

	@JvmOverloads
	constructor(
		context: Context,
		attrs: AttributeSet? = null,
		defStyle: Int = R.attr.simpleMenuPreferenceStyle
	) : this(context, attrs, defStyle, R.style.Preference_SimpleMenuPreference)

	init {
		context.withStyledAttributes(
				attrs, R.styleable.SimpleMenuPreference, defStyleAttr, defStyleRes
		) {
			val popupStyle: Int = getResourceId(
					R.styleable.SimpleMenuPreference_android_popupMenuStyle,
					R.style.Widget_Preference_SimpleMenuPreference_PopupMenu
			)
			val popupTheme: Int = getResourceId(
					R.styleable.SimpleMenuPreference_android_popupTheme,
					R.style.ThemeOverlay_Preference_SimpleMenuPreference_PopupMenu
			)
			val popupContext = if (popupTheme != 0) {
				ContextThemeWrapper(context, popupTheme)
			} else {
				context
			}

			mPopupWindow = SimpleMenuPopupWindow(
					popupContext,
					attrs,
					R.styleable.SimpleMenuPreference_android_popupMenuStyle,
					popupStyle
			)
			mPopupWindow?.onItemClickListener =
				SimpleMenuPopupWindow.OnItemClickListener { i: Int ->
					val value = entryValues[i].toString()
					if (callChangeListener(value)) {
						setValue(value)
					}
				}
		}
	}

	override fun onClick() {
		if (entries.isNullOrEmpty()) {
			return
		}

		if (mPopupWindow == null) {
			return
		}

		mPopupWindow?.entries = entries
		mPopupWindow?.selectedItemIndex = findIndexOfValue(value)

		mPopupWindow?.show(mItemView!!, mItemView!!.parent as View, mAnchor?.x?.toInt() ?: 0)
	}

	override fun setEntries(entries: Array<CharSequence?>) {
		super.setEntries(entries)
		mPopupWindow?.requestMeasure()
	}

	override fun onBindViewHolder(view: PreferenceViewHolder) {
		super.onBindViewHolder(view)
		mItemView = view.itemView
		mAnchor = view.itemView.findViewById(android.R.id.empty)
	}

	companion object {
		@JvmField
		var isLightFixEnabled: Boolean = false
	}
}
