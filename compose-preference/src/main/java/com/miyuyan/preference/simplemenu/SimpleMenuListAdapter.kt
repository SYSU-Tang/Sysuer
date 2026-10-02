package com.miyuyan.preference.simplemenu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckedTextView
import androidx.recyclerview.widget.RecyclerView
import com.miyuyan.preference.R
import com.miyuyan.preference.simplemenu.SimpleMenuPopupWindow.Companion.DIALOG
import com.miyuyan.preference.simplemenu.SimpleMenuPopupWindow.Companion.HORIZONTAL

class SimpleMenuListAdapter(private val mWindow: SimpleMenuPopupWindow) :
	RecyclerView.Adapter<SimpleMenuListAdapter.ViewHolder>() {
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		return ViewHolder(
				LayoutInflater.from(parent.context)
					.inflate(R.layout.simple_menu_item, parent, false)
		)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.bind(mWindow, position)
	}

	override fun getItemCount(): Int = mWindow.entries.size


	class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
		var mCheckedTextView: CheckedTextView = itemView.findViewById(android.R.id.text1)

		private var mWindow: SimpleMenuPopupWindow? = null

		init {
			itemView.setOnClickListener(this)
		}

		fun bind(window: SimpleMenuPopupWindow, position: Int) {
			mWindow = window
			mCheckedTextView.text = mWindow?.entries[position]
			mCheckedTextView.isChecked = position == mWindow?.selectedItemIndex
			mCheckedTextView.setMaxLines(if (mWindow?.mode == DIALOG) Int.MAX_VALUE else 1)

			val padding = mWindow?.listPaddings[mWindow!!.mode]?.get(HORIZONTAL)
			val paddingVertical = mCheckedTextView.paddingTop
			mCheckedTextView.setPadding(
					padding ?: 0, paddingVertical, padding ?: 0, paddingVertical
			)
		}

		override fun onClick(view: View?) {
			mWindow?.onItemClickListener?.onClick(getAdapterPosition())
			if (mWindow?.isShowing == true) {
				mWindow?.dismiss()
			}
		}
	}
}
