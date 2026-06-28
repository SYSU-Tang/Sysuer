package com.sysu.edu.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.TextUtils
import com.google.android.material.R
import com.haibin.calendarview.Calendar
import com.haibin.calendarview.MonthView
import com.sysu.edu.api.CommonUtil.trim
import com.sysu.edu.api.ContextUtil
import kotlin.math.min

/**
 * 演示一个变态需求的月视图
 */
class FeeMonthView(context: Context) : MonthView(context) {
	/**
	 * 自定义魅族标记的文本画笔
	 */
	private val mTextPaint = Paint().apply {
		textSize = dipToPx(context, 8f).toFloat()
		setColor(-0x1)
		isAntiAlias = true
		isFakeBoldText = true
	}
	
	/**
	 * 24节气画笔
	 */
	private val mSolarTermTextPaint = Paint().apply {
		setColor(-0xb76201)
		isAntiAlias = true
		textAlign = Paint.Align.CENTER
	}
	
	/**
	 * 背景圆点
	 */
	private val mPointPaint = Paint().apply {
		isAntiAlias = true
		style = Paint.Style.FILL
		textAlign = Paint.Align.CENTER
		setColor(Color.RED)
	}
	
	/**
	 * 今天的背景色
	 */
	private val mCurrentDayPaint = Paint().apply {
		isAntiAlias = true
		style = Paint.Style.FILL
		setColor(-0x151516)
	}
	
	/**
	 * 圆点半径
	 */
	private val mPointRadius: Float
	private val mPadding: Int
	private val mCircleRadius: Float
	
	/**
	 * 自定义魅族标记的圆形背景
	 */
	private val mSchemeBasicPaint = Paint().apply {
		isAntiAlias = true
		style = Paint.Style.FILL
		textAlign = Paint.Align.CENTER
		isFakeBoldText = true
		setColor(Color.WHITE)
	}
	private val mSchemeBaseLine: Float
	private val other: Int
	private var mRadius = 0
	
	init {
		val contextUtil = ContextUtil(context)
		other = contextUtil.getColorFromAttr(R.attr.colorTertiary)
		mCircleRadius = dipToPx(getContext(), 7f).toFloat()
		mPadding = dipToPx(getContext(), 3f)
		mPointRadius = dipToPx(context, 2f).toFloat()
		val metrics = mSchemeBasicPaint.getFontMetrics()
		mSchemeBaseLine = mCircleRadius - metrics.descent + (metrics.bottom - metrics.top) / 2 + dipToPx(getContext(), 1f)
	}
	
	override fun onPreviewHook() {
		mSolarTermTextPaint.textSize = mCurMonthLunarTextPaint.textSize
		mRadius = min(mItemWidth, mItemHeight) / 2
	}
	
	override fun onDrawSelected(canvas: Canvas,
	                            calendar: Calendar?,
	                            x: Int,
	                            y: Int,
	                            hasScheme: Boolean): Boolean {
		val cx = x + mItemWidth / 2
		val cy = y + mItemHeight / 2
		canvas.drawCircle(cx.toFloat(), cy.toFloat(), mRadius.toFloat(), mSelectedPaint)
		return true
	}
	
	override fun onDrawScheme(canvas: Canvas?, calendar: Calendar?, x: Int, y: Int) {
		mPointPaint.setColor(if (isSelected(calendar))Color.WHITE else Color.GRAY)
	//        canvas.drawCircle(x + (float) mItemWidth / 2, y + mItemHeight - 3 * mPadding, mPointRadius, mPointPaint);
	}
	
	override fun onDrawText(canvas: Canvas,
	                        calendar: Calendar,
	                        x: Int,
	                        y: Int,
	                        hasScheme: Boolean,
	                        isSelected: Boolean) {
		val cx = x + mItemWidth / 2
		val cy = y + mItemHeight / 2
		val top = y - mItemHeight / 6
		if (calendar.isCurrentDay && !isSelected) canvas.drawCircle(cx.toFloat(), cy.toFloat(), mRadius.toFloat(), mCurrentDayPaint)
		val scheme = trim(calendar.scheme)
		if (hasScheme) { //            canvas.drawCircle(x + mItemWidth - mPadding - mCircleRadius / 2, y + mPadding + mCircleRadius, mCircleRadius, mSchemeBasicPaint);
			mTextPaint.setColor(calendar.schemeColor)
			canvas.drawText(scheme, x + mItemWidth - mPadding - mCircleRadius, y + mPadding + mSchemeBaseLine, mTextPaint)
		} //当然可以换成其它对应的画笔就不麻烦，
		//        if (calendar.isWeekend() && calendar.isCurrentMonth()) {
		//            mCurMonthTextPaint.setColor(0xFF489dff);
		//            mCurMonthLunarTextPaint.setColor(0xFF489dff);
		//            mSchemeTextPaint.setColor(0xFF489dff);
		//            mSchemeLunarTextPaint.setColor(0xFF489dff);
		//            mOtherMonthLunarTextPaint.setColor(0xFF489dff);
		//            mOtherMonthTextPaint.setColor(0xFF489dff);
		//        } else {
		//
		//        }
		mCurMonthTextPaint.setColor(other)
		mCurMonthLunarTextPaint.setColor(other)
		mSchemeTextPaint.setColor(other)
		mSchemeLunarTextPaint.setColor(other)
		mOtherMonthTextPaint.setColor(-0x111112)
		mOtherMonthLunarTextPaint.setColor(-0x111112)
		if (isSelected) {
			canvas.drawText(calendar.day.toString(), cx.toFloat(), mTextBaseLine + top, mSelectTextPaint)
			canvas.drawText(scheme, cx.toFloat(), mTextBaseLine + y + mItemHeight.toFloat() / 10, mSelectedLunarTextPaint)
		}
		else if (hasScheme) {
			canvas.drawText(calendar.day.toString(), cx.toFloat(), mTextBaseLine + top, if (calendar.isCurrentMonth) mSchemeTextPaint else mOtherMonthTextPaint)
			canvas.drawText(scheme, cx.toFloat(), mTextBaseLine + y + mItemHeight.toFloat() / 10, if (!TextUtils.isEmpty(calendar.solarTerm)) mSolarTermTextPaint else mSchemeLunarTextPaint)
		}
		else {
			canvas.drawText(calendar.day.toString(), cx.toFloat(), mTextBaseLine + top, if (calendar.isCurrentDay) mCurDayTextPaint else if (calendar.isCurrentMonth) mCurMonthTextPaint else mOtherMonthTextPaint)
			canvas.drawText(calendar.lunar, cx.toFloat(), mTextBaseLine + y + mItemHeight.toFloat() / 10, if (calendar.isCurrentDay) mCurDayLunarTextPaint else if (calendar.isCurrentMonth) if (!TextUtils.isEmpty(calendar.solarTerm)) mSolarTermTextPaint else mCurMonthLunarTextPaint else mOtherMonthLunarTextPaint)
		}
	}
	
	companion object {
		/**
		 * dp转px
		 *
		 * @param context context
		 * @param dpValue dp
		 * @return px
		 */
		private fun dipToPx(context: Context, dpValue: Float): Int =
			(dpValue * context.resources.displayMetrics.density + 0.5f).toInt()
	}
}