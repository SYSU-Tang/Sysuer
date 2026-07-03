package com.sysu.edu.preference

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.preference.PreferenceViewHolder
import androidx.preference.SeekBarPreference
import com.google.android.material.slider.Slider
import com.sysu.edu.R
import com.sysu.edu.databinding.PreferenceSliderBinding

class SliderPreference(context: Context,
                       attrs: AttributeSet? = null,
                       defStyleAttr: Int = R.attr.sliderPreferenceStyle,
                       defStyleRes: Int = 0) :
	SeekBarPreference(context, attrs, defStyleAttr, defStyleRes) {
	init {
		layoutResource = R.layout.preference_slider
	}
	
	override fun onBindViewHolder(holder: PreferenceViewHolder) {
		PreferenceSliderBinding.bind(holder.itemView).apply {
			seekbar.isEnabled = isEnabled
			title.setEnabled(isEnabled)
			seekbarValue.setEnabled(isEnabled)
			root.setEnabled(isEnabled)
			seekbar.stepSize = seekBarIncrement.toFloat()
			seekbar.valueTo = max.toFloat()
			seekbar.valueFrom = min.toFloat()
			title.text = this@SliderPreference.title
			icon.setImageDrawable(getIcon())
			seekbarValue.visibility = if (showSeekBarValue) View.VISIBLE else View.GONE
			seekbarValue.text = "$value"
			seekbar.value = value.toFloat()
			seekbar.addOnChangeListener { _: Slider?, v: Float, _: Boolean -> value = v.toInt() }
			root.setOnClickListener { onClick() }
		}
	}
	
	override fun getValue(): Int = if (super.getValue() == 0) min else super.getValue()
}
