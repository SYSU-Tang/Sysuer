package com.miyuyan.preference.simplemenu

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.view.forEachIndexed
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Helper class to create and start animation of Simple Menu.
 * 
 * 
 * TODO let params styleable
 */
internal object SimpleMenuAnimation {
	@JvmStatic
	fun startEnterAnimation(
		background: CustomBoundsDrawable?,
		view: View?,
		width: Int,
		height: Int,
		centerX: Int,
		centerY: Int,
		start: Rect?,
		itemHeight: Int,
		elevation: Int,
		selectedIndex: Int
	) {
		val holder = PropertyHolder(background, view)
		val backgroundAnimator = createBoundsAnimator(
				holder, width, height, centerX, centerY, start
		)
		val elevationAnimator = createElevationAnimator(view, elevation.toFloat())

		AnimatorSet().apply {
			playTogether(backgroundAnimator, elevationAnimator)
			setDuration(backgroundAnimator.duration)
		}.start()

		val delay: Long = 0

		if (view is ViewGroup) {
			view.forEachIndexed { i, v ->
				val offset = selectedIndex - i
				startChild(
						v,
						delay + 30L * abs(offset),
						if (offset == 0) 0 else (itemHeight * 0.2).toInt() * (if (offset < 0) -1 else 1)
				)
			}
		}
	}

	private fun startChild(child: View, delay: Long, translationY: Int) {
		child.setAlpha(0f)

		val alphaAnimator: Animator = ObjectAnimator.ofFloat(child, "alpha", 0.0f, 1.0f).apply {
			duration = 200
			interpolator = AccelerateInterpolator()
		}

		val translationAnimator: Animator =
			ObjectAnimator.ofFloat(child, "translationY", translationY.toFloat(), 0f).apply {
				duration = 275
				interpolator = DecelerateInterpolator()
			}
		AnimatorSet().apply {
			playTogether(alphaAnimator, translationAnimator)
			setStartDelay(delay)
		}.start()
	}

	private fun getBounds(
		width: Int, height: Int, centerX: Int, centerY: Int
	): Array<Rect?> {
		val endWidth = max(centerX, width - centerX)
		val endHeight = max(centerY, height - centerY)

		val endLeft = centerX - endWidth
		val endRight = centerX + endWidth
		val endTop = centerY - endHeight
		val endBottom = centerY + endHeight

		val end = Rect(endLeft, endTop, endRight, endBottom)
		val max = Rect(0, 0, width, height)

		return arrayOf(end, max)
	}

	private fun createBoundsAnimator(
		holder: PropertyHolder?, width: Int, height: Int, centerX: Int, centerY: Int, start: Rect?
	): Animator {
		val speed = 4096

		val endWidth = max(centerX, width - centerX)
		val endHeight = max(centerY, height - centerY)

		val rect = getBounds(width, height, centerX, centerY)
		val end = rect[0]
		val max = rect[1]

		var mduration = (max(endWidth, endHeight).toFloat() / speed * 1000).toLong()
		mduration = max(mduration, 150)
		mduration = min(mduration, 300)

		val animator: Animator = ObjectAnimator.ofObject(
				holder, SimpleMenuBoundsProperty.BOUNDS, RectEvaluator(max), start, end
		).apply {
			interpolator = DecelerateInterpolator()
			duration = mduration
		}
		return animator
	}

	private fun createElevationAnimator(view: View?, elevation: Float): Animator =
		ObjectAnimator.ofFloat(
				view, View.TRANSLATION_Z, -elevation, 0f
		).apply {
			interpolator = FastOutSlowInInterpolator()
		}
}
