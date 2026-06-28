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
import java.util.function.Consumer

class GridDialog(private val activity: FragmentActivity) {
	val referenceIds: MutableList<Int?> = mutableListOf()
	private val menuBinding: DialogGridBinding = DialogGridBinding.inflate(activity.layoutInflater)
	val dialog: BottomSheetDialog = BottomSheetDialog(activity)
	private var selected = -1
	private var selectable = false
	private var multipleSelectable = false
	private var iconGravity = 0
	private var gravity = 0
	
	init {
		dialog.setContentView(menuBinding.root)
	}
	
	fun setColumn(column: Int) {
		(menuBinding.handler.layoutParams as GridLayout.LayoutParams).columnSpec = GridLayout.spec(GridLayout.UNDEFINED, column, GridLayout.FILL, 1.0f)
		menuBinding.grid.setColumnCount(column)
	}
	
	fun setIconGravity(gravity: Int) {
		iconGravity = gravity
		referenceIds.forEach { id: Int? -> (menuBinding.grid.findViewById<View?>(id!!) as MaterialButton).iconGravity = gravity }
	}
	
	fun setGravity(gravity: Int) {
		this.gravity = gravity
		referenceIds.forEach { id: Int? -> (menuBinding.grid.findViewById<View?>(id!!) as MaterialButton).setGravity(gravity) }
	}
	
	fun <T> loadMenu(menuTitle: MutableList<T?>,
	                 menuIcon: MutableList<Int?>,
	                 menuAction: MutableList<out Consumer<MaterialButton?>?>,
	                 type: Class<T?>?) {
		referenceIds.clear()
		menuTitle.forEachIndexed { i, v ->
			val menu = ItemButtonGridBinding.inflate(activity.layoutInflater, menuBinding.grid, false).root
			if (type == Int::class.java) menu.setText((v as Int?)!!)
			else menu.text = v as String?
			if (menuIcon.size > i && menuIcon[i] != 0) menu.setIconResource(menuIcon[i]!!)
			val id = View.generateViewId()
			referenceIds.add(id)
			menu.setId(id)
			menu.addOnCheckedChangeListener { _: MaterialButton?, isChecked: Boolean -> menu.strokeWidth = if (isChecked && (selectable || multipleSelectable)) 3 else 0 }
			menu.setOnClickListener {
				if (menuAction.size > i && menuAction[i] != null) menuAction[i]!!.accept(menu)
				if (multipleSelectable) toggleMenu(i)
				else if (selectable) selectMenu(i)
			}
			if (iconGravity != 0) menu.iconGravity = iconGravity
			if (gravity != 0) menu.setGravity(gravity)
			menuBinding.grid.addView(menu)
		}
	}
	
	fun show() {
		dialog.show()
		dialog.behavior.setState(BottomSheetBehavior.STATE_EXPANDED)
	}
	
	fun getMenu(position: Int): MaterialButton? {
		return if (position >= referenceIds.size || position < 0) null else menuBinding.grid.findViewById(referenceIds[position]!!)
	}
	
	fun setSelectable(selectable: Boolean) {
		this.selectable = selectable
	}
	
	fun setMultipleSelectable(multipleSelectable: Boolean) {
		this.multipleSelectable = multipleSelectable
	}
	
	fun selectMenu(position: Int) {
		val menu = getMenu(position)
		if (menu != null && position != selected) {
			menu.isChecked = true
			if (selected >= 0) getMenu(selected)?.isChecked = false
			selected = position
		}
	}
	
	fun toggleMenu(position: Int) {
		val menu = getMenu(position)
		if (menu != null) menu.isChecked = !menu.isChecked
	}
	
	fun toggleMenu(position: Int, toggle: Boolean) {
		getMenu(position)?.isChecked = toggle
	}
	
	fun clickMenu(position: Int) {
		getMenu(position)?.performClick()
	}
	
	/*public void multipleSelectMenu(int[] positions) {
        IntStream.of(positions).forEach(this::selectMenu);
    }*/
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
}
