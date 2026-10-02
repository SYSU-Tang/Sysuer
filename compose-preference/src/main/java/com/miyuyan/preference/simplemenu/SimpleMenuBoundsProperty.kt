package com.miyuyan.preference.simplemenu

import android.graphics.Rect
import android.os.Build
import android.util.Property

internal class SimpleMenuBoundsProperty(name: String?) :
	Property<PropertyHolder?, Rect?>(Rect::class.java, name) {
	override fun get(holder: PropertyHolder?): Rect? {
		return holder?.bounds
	}

	override fun set(holder: PropertyHolder?, value: Rect?) {
		if (value != null) {
			holder?.bounds = value
		}

		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
			holder?.contentView?.invalidate()
		}
	}

	companion object {
		val BOUNDS: Property<PropertyHolder?, Rect?> = SimpleMenuBoundsProperty("bounds")
	}
}
