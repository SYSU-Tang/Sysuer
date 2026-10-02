package com.miyuyan.preference.simplemenu

import android.annotation.SuppressLint
import android.graphics.HardwareRenderer
import android.graphics.Point
import android.os.Build
import android.view.Display
import android.view.View
import android.widget.PopupWindow

@SuppressLint("PrivateApi")
internal object Light {
	/**
	 * Android uses `displaySize.x / 2 - windowLeft` as the x-coordinate of light source ([source code](http://androidxref.com/9.0.0_r3/xref/frameworks/base/core/java/android/view/ThreadedRenderer.java#1021)).
	 * <br></br>If the window is on the left of the screen, the light source will be at the right to the window
	 * causing shadow on the left side. This make our PopupWindow looks weird.
	 * 
	 * This method reset the x-coordinate of light source to `windowLeft + 56dp` by using multiply reflections.
	 * 
	 * @param window PopupWindow
	 */
	@JvmStatic
	fun resetLightCenterForPopupWindow(window: PopupWindow) {
		try {
			val threadedRendererClass = Class.forName("android.view.ThreadedRenderer")
			val attachInfoClass = Class.forName($$"android.view.View$AttachInfo")

			val view = window.contentView.getRootView()
			val getThreadedRendererMethod =
				View::class.java.getDeclaredMethod("getThreadedRenderer")
			getThreadedRendererMethod.isAccessible = true
			val threadedRenderer = getThreadedRendererMethod.invoke(view)

			val attachInfoField = View::class.java.getDeclaredField("mAttachInfo")
			attachInfoField.isAccessible = true
			val attachInfo = attachInfoField.get(view)

			val pointField = attachInfoClass.getDeclaredField("mPoint")
			pointField.isAccessible = true
			val displaySize = pointField.get(attachInfo) as Point?

			val displayField = attachInfoClass.getDeclaredField("mDisplay")
			displayField.isAccessible = true
			val display = displayField.get(attachInfo) as Display?

			display!!.getRealSize(displaySize)

			val windowLeftField = attachInfoClass.getDeclaredField("mWindowLeft")
			windowLeftField.isAccessible = true
			val mWindowLeft = windowLeftField.getInt(attachInfo)

			val windowTopField = attachInfoClass.getDeclaredField("mWindowTop")
			windowTopField.isAccessible = true
			val mWindowTop = windowTopField.getInt(attachInfo)

			val lightYField = threadedRendererClass.getDeclaredField("mLightY")
			lightYField.isAccessible = true
			val mLightY = lightYField.getFloat(threadedRenderer)

			val lightZField = threadedRendererClass.getDeclaredField("mLightZ")
			lightZField.isAccessible = true
			val mLightZ = lightZField.getFloat(threadedRenderer)

			val lightRadiusField = threadedRendererClass.getDeclaredField("mLightRadius")
			lightRadiusField.isAccessible = true
			val mLightRadius = lightRadiusField.getFloat(threadedRenderer)

			val lightY = mLightY - mWindowTop

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				if (threadedRenderer != null) {
					(threadedRenderer as HardwareRenderer).setLightSourceGeometry(
							mWindowLeft.toFloat(), lightY, mLightZ, mLightRadius
					)
				}
			} else {
				val nativeProxyField = threadedRendererClass.getDeclaredField("mNativeProxy")
				nativeProxyField.isAccessible = true
				val mNativeProxy = nativeProxyField.getLong(threadedRenderer)

				val nSetLightCenterMethod = threadedRendererClass.getDeclaredMethod(
						"nSetLightCenter",
						Long::class.javaPrimitiveType,
						Float::class.javaPrimitiveType,
						Float::class.javaPrimitiveType,
						Float::class.javaPrimitiveType
				)
				nSetLightCenterMethod.isAccessible = true
				nSetLightCenterMethod.invoke(
						null, mNativeProxy, mWindowLeft.toFloat(), lightY, mLightZ
				)
			}
		} catch (tr: Throwable) {
			tr.printStackTrace()
		}
	}
}
