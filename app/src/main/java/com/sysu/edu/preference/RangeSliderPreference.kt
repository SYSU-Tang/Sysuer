package com.sysu.edu.preference

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.content.withStyledAttributes
import androidx.preference.PreferenceViewHolder
import androidx.preference.SeekBarPreference
import com.google.android.material.slider.RangeSlider
import com.sysu.edu.R
import com.sysu.edu.databinding.PreferenceRangeSliderBinding
import java.util.Locale

class RangeSliderPreference(context: Context,
                            attrs: AttributeSet? = null,
                            defStyleAttr: Int = R.attr.rangeSliderPreferenceStyle,
                            defStyleRes: Int = 0) :
	SeekBarPreference(context, attrs, defStyleAttr, defStyleRes) {
	private lateinit var mValue: FloatArray
	
	init {
		layoutResource = R.layout.preference_range_slider
		try {
			context.withStyledAttributes(attrs, R.styleable.rangeSliderPreferenceStyle, defStyleAttr, defStyleRes) {
				mValue = floatArrayOf(getFloat(R.styleable.rangeSliderPreferenceStyle_valueFrom, min.toFloat()), getFloat(R.styleable.rangeSliderPreferenceStyle_valueTo, max.toFloat()))
			}
		} catch (_: Exception) { //            throw new RuntimeException(e);
		}
	}
	
	override fun onBindViewHolder(holder: PreferenceViewHolder) {
		PreferenceRangeSliderBinding.bind(holder.itemView).apply {
			seekbar.stepSize = seekBarIncrement.toFloat()
			seekbar.valueTo = max.toFloat()
			seekbar.valueFrom = min.toFloat()
			val values = values
			seekbar.setValues(values[0], values[1])
			title.text = this@RangeSliderPreference.title
			icon.setImageDrawable(getIcon())
			seekbarValue.visibility = if (showSeekBarValue) View.VISIBLE else View.GONE
			seekbarValue.text = String.format(Locale.getDefault(), "%.0f~%.0f", values[0], values[1])
			seekbar.addOnChangeListener { slider: RangeSlider?, _: Float, _: Boolean ->
				this@RangeSliderPreference.values = floatArrayOf(slider!!.values[0], slider.values[1])
			}
			root.setOnClickListener { onClick() }
		}
	}
	
	var values: FloatArray
		get() = mValue
		set(values) {
			mValue = values
			notifyChanged() //setValue(values);
		}
}
