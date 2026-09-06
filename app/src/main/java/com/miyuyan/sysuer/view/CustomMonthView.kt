package com.miyuyan.sysuer.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.TextUtils
import android.util.TypedValue
import androidx.compose.ui.unit.dp
import com.google.android.material.R
import com.haibin.calendarview.Calendar
import com.haibin.calendarview.MonthView
import kotlin.math.min

/**
 * 演示一个变态需求的月视图
 */
class CustomMonthView(context: Context) : MonthView(context) {
	/**
	 * 自定义魅族标记的文本画笔
	 */
	private val mTextPaint = Paint()

	/**
	 * 24节气画笔
	 */
	private val mSolarTermTextPaint = Paint()

	/**
	 * 背景圆点
	 */
	private val mPointPaint = Paint()

	/**
	 * 今天的背景色
	 */
	private val mCurrentDayPaint = Paint()

	/**
	 * 圆点半径
	 */
	private val mPointRadius: Float
	private val mPadding: Float
	private val mCircleRadius: Float

	/**
	 * 自定义魅族标记的圆形背景
	 */
	private val mSchemeBasicPaint = Paint()
	private val mSchemeBaseLine: Float
	private var other = Color.GRAY
	private var mRadius = 0

	init {
		val typedValue = TypedValue()
		if (context.theme.resolveAttribute(R.attr.colorTertiary, typedValue, true)) other =
			typedValue.data
		mTextPaint.apply {
			textSize = 8.dp.value
			setColor(-0x1)
			isAntiAlias = true
			isFakeBoldText = true
		}
		mSolarTermTextPaint.apply {
			setColor(-0xb76201)
			isAntiAlias = true
			textAlign = Paint.Align.CENTER
		}
		mSchemeBasicPaint.apply {
			isAntiAlias = true
			style = Paint.Style.FILL
			textAlign = Paint.Align.CENTER
			isFakeBoldText = true
			setColor(Color.WHITE)
		}
		mCurrentDayPaint.apply {
			isAntiAlias = true
			style = Paint.Style.FILL
			setColor(-0x151516)
		}
		mPointPaint.apply {
			isAntiAlias = true
			style = Paint.Style.FILL
			textAlign = Paint.Align.CENTER
			setColor(Color.RED)
		}
		mCircleRadius = 7f.dp.value
		mPadding = 3f.dp.value
		mPointRadius = 2f.dp.value
		val metrics = mSchemeBasicPaint.getFontMetrics()
		mSchemeBaseLine =
			mCircleRadius - metrics.descent + (metrics.bottom - metrics.top) / 2 + 1f.dp.value
	}

	override fun onPreviewHook() {
		mSolarTermTextPaint.textSize = mCurMonthLunarTextPaint.textSize
		mRadius = min(mItemWidth, mItemHeight) / 11 * 5
	}

	override fun onDestroy() {
	}

	override fun onDrawSelected(
		canvas: Canvas, calendar: Calendar, x: Int, y: Int, hasScheme: Boolean
	): Boolean {
		canvas.drawCircle(
			(x + mItemWidth / 2).toFloat(),
			(y + mItemHeight / 2).toFloat(),
			mRadius.toFloat(),
			mSelectedPaint
		)
		return true
	}

	override fun onDrawScheme(canvas: Canvas, calendar: Calendar, x: Int, y: Int) {
		mPointPaint.setColor(if (isSelected(calendar)) Color.WHITE else Color.GRAY)
		canvas.drawCircle(
			x + mItemWidth.toFloat() / 2,
			(y + mItemHeight - 3 * mPadding),
			mPointRadius,
			mPointPaint
		)
	}

	override fun onDrawText(
		canvas: Canvas, calendar: Calendar, x: Int, y: Int, hasScheme: Boolean, isSelected: Boolean
	) {
		val cx = x + mItemWidth / 2
		val cy = y + mItemHeight / 2
		val top = y - mItemHeight / 6
		if (calendar.isCurrentDay && !isSelected) canvas.drawCircle(
			cx.toFloat(), cy.toFloat(), mRadius.toFloat(), mCurrentDayPaint
		)

		if (hasScheme) {
			canvas.drawCircle(
				x + mItemWidth - mPadding - mCircleRadius / 2,
				y + mPadding + mCircleRadius,
				mCircleRadius,
				mSchemeBasicPaint
			)
			mTextPaint.setColor(calendar.schemeColor)
			canvas.drawText(
				calendar.scheme,
				x + mItemWidth - mPadding - mCircleRadius,
				y + mPadding + mSchemeBaseLine,
				mTextPaint
			)
		}
		if (calendar.isWeekend && calendar.isCurrentMonth) {
			mCurMonthTextPaint.setColor(-0xb76201)
			mCurMonthLunarTextPaint.setColor(-0xb76201)
			mSchemeTextPaint.setColor(-0xb76201)
			mSchemeLunarTextPaint.setColor(-0xb76201)
			mOtherMonthLunarTextPaint.setColor(-0xb76201)
			mOtherMonthTextPaint.setColor(-0xb76201)
		} else {
			mCurMonthTextPaint.setColor(other)
			mCurMonthLunarTextPaint.setColor(other)
			mSchemeTextPaint.setColor(-0xcccccd)
			mSchemeLunarTextPaint.setColor(-0x303031)
			mOtherMonthTextPaint.setColor(-0x111112)
			mOtherMonthLunarTextPaint.setColor(-0x111112)
		}

		if (isSelected) {
			canvas.drawText(
				calendar.day.toString(), cx.toFloat(), mTextBaseLine + top, mSelectTextPaint
			)
			canvas.drawText(
				calendar.lunar,
				cx.toFloat(),
				mTextBaseLine + y + mItemHeight.toFloat() / 10,
				mSelectedLunarTextPaint
			)
		} else if (hasScheme) {
			canvas.drawText(
				calendar.day.toString(),
				cx.toFloat(),
				mTextBaseLine + top,
				if (calendar.isCurrentMonth) mSchemeTextPaint else mOtherMonthTextPaint
			)
			canvas.drawText(
				calendar.lunar,
				cx.toFloat(),
				mTextBaseLine + y + mItemHeight.toFloat() / 10,
				if (!TextUtils.isEmpty(calendar.solarTerm)) mSolarTermTextPaint else mSchemeLunarTextPaint
			)
		} else {
			canvas.drawText(
				calendar.day.toString(),
				cx.toFloat(),
				mTextBaseLine + top,
				if (calendar.isCurrentDay) mCurDayTextPaint else if (calendar.isCurrentMonth) mCurMonthTextPaint else mOtherMonthTextPaint
			)
			canvas.drawText(
				calendar.lunar,
				cx.toFloat(),
				mTextBaseLine + y + mItemHeight.toFloat() / 10,
				if (calendar.isCurrentDay) mCurDayLunarTextPaint else if (calendar.isCurrentMonth) if (!TextUtils.isEmpty(
						calendar.solarTerm
					)
				) mSolarTermTextPaint else mCurMonthLunarTextPaint else mOtherMonthLunarTextPaint
			)
		}
	}

}