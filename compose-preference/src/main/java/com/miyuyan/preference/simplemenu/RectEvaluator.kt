package com.miyuyan.preference.simplemenu

import android.animation.TypeEvaluator
import android.annotation.SuppressLint
import android.graphics.Rect

/**
 * This evaluator can be used to perform type interpolation between [Rect].
 */
internal class RectEvaluator(private val mMax: Rect?) : TypeEvaluator<Rect?> {
	private val mTemp = Rect()

	@SuppressLint("CheckResult")
	override fun evaluate(fraction: Float, startValue: Rect?, endValue: Rect?): Rect {
		if (startValue != null && endValue != null) {
			mTemp.left = startValue.left + ((endValue.left - startValue.left) * fraction).toInt()
			mTemp.top = startValue.top + ((endValue.top - startValue.top) * fraction).toInt()
			mTemp.right =
				startValue.right + ((endValue.right - startValue.right) * fraction).toInt()
			mTemp.bottom =
				startValue.bottom + ((endValue.bottom - startValue.bottom) * fraction).toInt()
			mTemp.setIntersect(mMax!!, mTemp)
		}
		return mTemp
	}
}
