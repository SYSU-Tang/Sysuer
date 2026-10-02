package com.miyuyan.preference.simplemenu

import android.graphics.Rect
import android.view.View

/**
 * Holder class holds background drawable and content view.
 */
internal class PropertyHolder(
	private val background: CustomBoundsDrawable?, val contentView: View?
) {
	var bounds: Rect
		get() = this.background?.getBounds() ?: Rect()
		set(value) {
			this.background?.setCustomBounds(value)
			this.contentView?.invalidateOutline()
		}
}
