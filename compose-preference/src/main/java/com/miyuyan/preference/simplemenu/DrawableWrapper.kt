package com.miyuyan.preference.simplemenu

import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.Region
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.DrawableCompat

open class DrawableWrapper(drawable: Drawable?) : Drawable(), Drawable.Callback {
	private var mDrawable: Drawable? = null

	var wrappedDrawable: Drawable?
		get() = mDrawable
		set(drawable) {
			mDrawable?.callback = null
			mDrawable = drawable
			drawable?.setCallback(this)
		}

	init {
		wrappedDrawable = drawable
	}

	override fun getOutline(outline: Outline) {
		mDrawable?.getOutline(outline)
	}

	override fun draw(canvas: Canvas) {
		mDrawable?.draw(canvas)
	}

	override fun onBoundsChange(bounds: Rect) {
		mDrawable?.bounds = bounds
	}

	override fun setChangingConfigurations(configs: Int) {
		mDrawable?.changingConfigurations = configs
	}

	override fun getChangingConfigurations(): Int {
		return mDrawable?.changingConfigurations ?: 0
	}


	override fun setFilterBitmap(filter: Boolean) {
		mDrawable?.setFilterBitmap(filter)
	}

	override fun setAlpha(alpha: Int) {
		mDrawable?.alpha = alpha
	}

	override fun setColorFilter(cf: ColorFilter?) {
		mDrawable?.colorFilter = cf
	}

	override fun isStateful(): Boolean = mDrawable?.isStateful ?: false

	override fun setState(stateSet: IntArray): Boolean = mDrawable?.setState(stateSet) ?: false

	override fun getState(): IntArray {
		return mDrawable?.state ?: intArrayOf()
	}

	override fun jumpToCurrentState() {
		mDrawable?.jumpToCurrentState()
	}

	override fun getCurrent(): Drawable {
		return mDrawable?.current ?: this
	}

	override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
		return super.setVisible(visible, restart) || mDrawable?.setVisible(
				visible,
				restart
		) ?: false
	}

	@Deprecated("Deprecated in Java")
	override fun getOpacity(): Int = mDrawable?.opacity ?: PixelFormat.UNKNOWN

	override fun getTransparentRegion(): Region? {
		return mDrawable?.transparentRegion
	}

	override fun getIntrinsicWidth(): Int {
		return mDrawable?.intrinsicWidth ?: 0
	}

	override fun getIntrinsicHeight(): Int {
		return mDrawable?.intrinsicHeight ?: 0
	}

	override fun getMinimumWidth(): Int {
		return mDrawable?.getMinimumWidth() ?: 0
	}

	override fun getMinimumHeight(): Int {
		return mDrawable?.getMinimumHeight() ?: 0
	}

	override fun getPadding(padding: Rect): Boolean {
		return mDrawable?.getPadding(padding) ?: false
	}

	override fun invalidateDrawable(who: Drawable) {
		invalidateSelf()
	}

	override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
		scheduleSelf(what, `when`)
	}

	override fun unscheduleDrawable(who: Drawable, what: Runnable) {
		unscheduleSelf(what)
	}

	override fun onLevelChange(level: Int): Boolean {
		return mDrawable!!.setLevel(level)
	}

	override fun setAutoMirrored(mirrored: Boolean) {
		mDrawable?.setAutoMirrored(mirrored)
	}

	override fun isAutoMirrored(): Boolean {
		return mDrawable?.isAutoMirrored ?: false
	}

	override fun setTint(tint: Int) {
		DrawableCompat.setTint(mDrawable!!, tint)
	}

	override fun setTintList(tint: ColorStateList?) {
		mDrawable?.setTintList(tint)
	}

	override fun setTintMode(tintMode: PorterDuff.Mode?) {
		mDrawable?.setTintMode(tintMode)
	}

	override fun setHotspot(x: Float, y: Float) {
		mDrawable?.setHotspot(x, y)
	}

	override fun setHotspotBounds(left: Int, top: Int, right: Int, bottom: Int) {
		DrawableCompat.setHotspotBounds(mDrawable!!, left, top, right, bottom)
	}
}