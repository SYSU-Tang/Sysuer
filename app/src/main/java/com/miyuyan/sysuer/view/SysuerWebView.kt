package com.miyuyan.sysuer.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.webkit.WebView
import androidx.core.view.NestedScrollingChild2
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.ViewCompat

class SysuerWebView @JvmOverloads constructor(context: Context,
                                              attrs: AttributeSet? = null,
                                              defStyleAttr: Int = 0) : WebView(context,
                                                                               attrs,
                                                                               defStyleAttr),
                                                                       NestedScrollingChild2 {
	private val childHelper = NestedScrollingChildHelper(this)
	private val consumed = IntArray(2)
	private val offsetInWindow = IntArray(2)
	private var lastY = 0f
	private var nestedOffsetY = 0
	
	init {
		isNestedScrollingEnabled = true
	}
	
	override fun onTouchEvent(ev: MotionEvent): Boolean {
		val event = MotionEvent.obtain(ev)
		var handled: Boolean
		
		when (event.actionMasked) {
			MotionEvent.ACTION_DOWN -> {                // reset bookkeeping
				nestedOffsetY = 0
				lastY = event.y                // start nested scroll for touch
				startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL,
				                  ViewCompat.TYPE_TOUCH)                // let WebView process the down as usual
				if (!isFocused) {
					requestFocus()
				}
				handled = super.onTouchEvent(event)
			}
			MotionEvent.ACTION_MOVE -> {
				val y = event.y
				var dy = (lastY - y).toInt()                // 1) offer parents a chance to pre-consume
				if (dispatchNestedPreScroll(0,
				                            dy,
				                            consumed,
				                            offsetInWindow,
				                            ViewCompat.TYPE_TOUCH)) {
					dy -= consumed[1]
					event.offsetLocation(0f, offsetInWindow[1].toFloat())
					nestedOffsetY += offsetInWindow[1]
				}
				
				lastY = y - offsetInWindow[1]                // 2) let WebView handle scrolling for its content
				val oldScrollY = scrollY                // We call super to allow WebView's internal touch handling (fling, etc.)
				handled = super.onTouchEvent(event)
				val scrolledByWebView = scrollY - oldScrollY                // 3) if WebView couldn't fully consume, dispatch the remaining to parent
				val unconsumedY = dy - scrolledByWebView
				if (dispatchNestedScroll(0,
				                         scrolledByWebView,
				                         0,
				                         unconsumedY,
				                         offsetInWindow,
				                         ViewCompat.TYPE_TOUCH)) {                    // parent scrolled some distance; adjust lastY & event
					lastY -= offsetInWindow[1]
					event.offsetLocation(0f, offsetInWindow[1].toFloat())
					nestedOffsetY += offsetInWindow[1]
				}
			}
			MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {                // stop nested scroll
				stopNestedScroll(ViewCompat.TYPE_TOUCH)
				handled = super.onTouchEvent(event)
			}
			else -> handled = super.onTouchEvent(event)
		}
		
		event.recycle()
		return handled
	}    // NestedScrollingChild2 / NestedScrollingChild delegation
	
	override fun setNestedScrollingEnabled(enabled: Boolean) {
		childHelper.isNestedScrollingEnabled = enabled
	}
	
	override fun isNestedScrollingEnabled(): Boolean = childHelper.isNestedScrollingEnabled
	override fun startNestedScroll(axes: Int, type: Int): Boolean =
		childHelper.startNestedScroll(axes, type)
	
	override fun stopNestedScroll(type: Int): Unit = childHelper.stopNestedScroll(type)
	override fun hasNestedScrollingParent(type: Int): Boolean =
		childHelper.hasNestedScrollingParent(type)
	
	override fun dispatchNestedPreScroll(dx: Int,
	                                     dy: Int,
	                                     consumed: IntArray?,
	                                     offsetInWindow: IntArray?,
	                                     type: Int): Boolean =
		childHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)
	
	override fun dispatchNestedScroll(dxConsumed: Int,
	                                  dyConsumed: Int,
	                                  dxUnconsumed: Int,
	                                  dyUnconsumed: Int,
	                                  offsetInWindow: IntArray?,
	                                  type: Int): Boolean = childHelper.dispatchNestedScroll(
		dxConsumed,
		dyConsumed,
		dxUnconsumed,
		dyUnconsumed,
		offsetInWindow,
		type)
	
	// Also provide compatibility implementations (NestedScrollingChild)
	override fun startNestedScroll(axes: Int): Boolean = childHelper.startNestedScroll(axes)
	override fun stopNestedScroll(): Unit = childHelper.stopNestedScroll()
	override fun hasNestedScrollingParent(): Boolean = childHelper.hasNestedScrollingParent()
	override fun dispatchNestedPreScroll(dx: Int,
	                                     dy: Int,
	                                     consumed: IntArray?,
	                                     offsetInWindow: IntArray?): Boolean =
		childHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)
	
	override fun dispatchNestedScroll(dxConsumed: Int,
	                                  dyConsumed: Int,
	                                  dxUnconsumed: Int,
	                                  dyUnconsumed: Int,
	                                  offsetInWindow: IntArray?): Boolean =
		childHelper.dispatchNestedScroll(dxConsumed,
		                                 dyConsumed,
		                                 dxUnconsumed,
		                                 dyUnconsumed,
		                                 offsetInWindow)
	
	override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean =
		childHelper.dispatchNestedPreFling(velocityX, velocityY)
	
	override fun dispatchNestedFling(velocityX: Float,
	                                 velocityY: Float,
	                                 consumed: Boolean): Boolean =
		childHelper.dispatchNestedFling(velocityX, velocityY, consumed)
}