package com.sysu.edu.view

import android.content.DialogInterface
import android.view.View
import android.widget.GridLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.sysu.edu.R
import com.sysu.edu.databinding.DialogGridBinding
import com.sysu.edu.databinding.ItemButtonGridBinding

class GridMenuDialog(private val activity: FragmentActivity) {
	val referenceIds: MutableList<Int> = mutableListOf()
	private val menuBinding: DialogGridBinding = DialogGridBinding.inflate(activity.layoutInflater)
	val dialog: BottomSheetDialog = BottomSheetDialog(activity)
	private var selected = -1
	private var selectable = false
	private var multipleSelectable = false
	private var iconGravity = -1
	private var gravity = -1
	
	init {
		dialog.setContentView(menuBinding.root)
	}
	
	fun setColumn(column: Int) {
		(menuBinding.handler.layoutParams as GridLayout.LayoutParams).columnSpec = GridLayout.spec(GridLayout.UNDEFINED, column, GridLayout.FILL, 1.0f)
		menuBinding.grid.setColumnCount(column)
	}
	
	fun setIconGravity(gravity: Int) {
		iconGravity = gravity
		referenceIds.forEach { id -> (menuBinding.grid.findViewById<View?>(id) as MaterialButton).iconGravity = gravity }
	}
	
	fun setGravity(gravity: Int) {
		this.gravity = gravity
		referenceIds.forEach { id -> (menuBinding.grid.findViewById<View?>(id) as MaterialButton).gravity = gravity }
	}
	
	fun <T> set(menuTitle: MutableList<T>,
	            menuIcon: MutableList<Int?>,
	            menuAction: MutableList<out onGridMenuClickListener?>) {
		referenceIds.clear()
		menuTitle.forEachIndexed { i, v ->
			add<T>(v, menuIcon[i], menuAction.getOrNull(i))
		}
	}
	
	fun <T> add(title: T?, menuIcon: Int?, menuAction: onGridMenuClickListener?) {
		val menu = ItemButtonGridBinding.inflate(activity.layoutInflater, menuBinding.grid, false).root
		if (title is Int) menu.setText(title)
		else if (title is String) menu.text = title
		if (menuIcon != null) menu.setIconResource(menuIcon)
		val id = View.generateViewId()
		val position = referenceIds.size
		referenceIds.add(id)
		menu.id = id
		menu.addOnCheckedChangeListener { _: MaterialButton?, isChecked: Boolean -> menu.strokeWidth = if (isChecked && (selectable || multipleSelectable)) 3 else 0 }
		menu.setOnClickListener {
			menuAction?.onClick(menu)
			if (multipleSelectable) menu.isChecked = !menu.isChecked
			else if (selectable) selectMenu(position)
		}
		if (iconGravity != -1) menu.iconGravity = iconGravity
		if (gravity != -1) menu.gravity = gravity
		menuBinding.grid.addView(menu)
	}
	
	fun add(menu: GridMenuItem) {
		add(menu.title, menu.icon, menu.action)
	}
	
	fun show() {
		dialog.show()
		dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
	}
	
	fun getMenu(position: Int): MaterialButton? = if (position in referenceIds.indices) menuBinding.grid.findViewById(referenceIds[position]) else null
	
	fun setSelectable(selectable: Boolean) {
		this.selectable = selectable
	}
	
	fun setMultipleSelectable(multipleSelectable: Boolean) {
		this.multipleSelectable = multipleSelectable
	}
	
	fun selectMenu(position: Int) {
		if (position != selected) {
			getMenu(position)?.run{
				isChecked = true
			}
			if (selected >= 0) getMenu(selected)?.isChecked = false
			selected = position
		}
	}
	
	fun toggleMenu(position: Int, toggle: Boolean) {
		getMenu(position)?.isChecked = toggle
	}
	
	fun clickMenu(position: Int) {
		getMenu(position)?.performClick()
	}
	
	fun setTogglable(positions: IntArray, togglable: Boolean) {
		positions.forEach { setTogglable(it, togglable) }
	}
	
	fun setTogglable(position: Int, togglable: Boolean) {
		getMenu(position)?.run {
			val colorStateList = AppCompatResources.getColorStateList(activity, R.color.toggle)
			isToggleCheckedStateOnClick = togglable
			isCheckable = togglable
			setIconTint(if (togglable) colorStateList else null)
			setTextColor(if (togglable) colorStateList else null)
		}
	}
	
	fun setPositiveButton(text: CharSequence?, action: DialogInterface.OnClickListener) {
		menuBinding.positive.text = text
		menuBinding.positive.setOnClickListener { action.onClick(this.dialog, DialogInterface.BUTTON_POSITIVE) }
		menuBinding.buttonGroup.setVisibility(View.VISIBLE)
	}
	
	fun setPositiveButton(text: Int, action: DialogInterface.OnClickListener) {
		menuBinding.positive.setText(text)
		menuBinding.positive.setOnClickListener { action.onClick(this.dialog, DialogInterface.BUTTON_POSITIVE) }
		menuBinding.buttonGroup.setVisibility(View.VISIBLE)
	}
	
	fun setNegativeButton(text: CharSequence?, action: DialogInterface.OnClickListener) {
		menuBinding.negative.text = text
		menuBinding.negative.setOnClickListener { action.onClick(this.dialog, DialogInterface.BUTTON_NEGATIVE) }
		menuBinding.buttonGroup.setVisibility(View.VISIBLE)
	}
	
	fun setNegativeButton(text: Int, action: DialogInterface.OnClickListener) {
		menuBinding.negative.setText(text)
		menuBinding.negative.setOnClickListener { action.onClick(this.dialog, DialogInterface.BUTTON_NEGATIVE) }
		menuBinding.buttonGroup.setVisibility(View.VISIBLE)
	}
	
	fun setNeutralButton(text: CharSequence?, action: DialogInterface.OnClickListener) {
		menuBinding.neutral.text = text
		menuBinding.neutral.setOnClickListener { action.onClick(this.dialog, DialogInterface.BUTTON_NEUTRAL) }
		menuBinding.buttonGroup.setVisibility(View.VISIBLE)
	}
	
	fun setNeutralButton(text: Int, action: DialogInterface.OnClickListener) {
		menuBinding.neutral.setText(text)
		menuBinding.neutral.setOnClickListener { action.onClick(this.dialog, DialogInterface.BUTTON_NEUTRAL) }
		menuBinding.buttonGroup.setVisibility(View.VISIBLE)
	}
	
	fun dismiss() {
		dialog.dismiss()
	}
	
	class GridMenuItem {
		var title: CharSequence? = null
		var icon: Int? = null
		var action: onGridMenuClickListener? = null
		
		companion object {
			fun of(title: CharSequence?,
			       icon: Int?,
			       action: onGridMenuClickListener?): GridMenuItem {
				val item = GridMenuItem()
				item.title = title
				item.icon = icon
				item.action = action
				return item
			}
		}
	}
	
	fun interface onGridMenuClickListener {
		fun onClick(menu: MaterialButton?)
	}
}