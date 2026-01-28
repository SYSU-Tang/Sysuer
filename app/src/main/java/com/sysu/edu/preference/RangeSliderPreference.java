package com.sysu.edu.preference;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SeekBarPreference;

import com.sysu.edu.R;
import com.sysu.edu.databinding.PreferenceRangeSliderBinding;

import java.util.Locale;

public class RangeSliderPreference extends SeekBarPreference {
    public RangeSliderPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.preference_range_slider);
    }

    public RangeSliderPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutResource(R.layout.preference_slider);
    }

    public RangeSliderPreference(@NonNull Context context) {
        super(context);
        setLayoutResource(R.layout.preference_range_slider);
    }

    public RangeSliderPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_range_slider);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        PreferenceRangeSliderBinding binding = PreferenceRangeSliderBinding.bind(holder.itemView);
        binding.seekbar.setStepSize(getSeekBarIncrement());
        binding.seekbar.setValueTo(getMax());
        binding.seekbar.setValueFrom(getMin());
        float value = getValue();
        binding.seekbar.setValues(value, value);
        binding.title.setText(getTitle());
        binding.icon.setImageDrawable(getIcon());
        binding.seekbarValue.setVisibility(getShowSeekBarValue() ? View.VISIBLE : View.INVISIBLE);
        binding.seekbarValue.setText(String.valueOf(getValue()));
        binding.seekbarValue.setText(String.format(Locale.getDefault(), "%.0f-%.0f", value, value));
        binding.seekbar.addOnChangeListener((slider, v, b) -> binding.seekbarValue.setText(String.format(Locale.getDefault(), "%.0f-%.0f", slider.getValues().get(0), slider.getValues().get(1))));
    }

    @Override
    protected void onClick() {
        super.onClick();
    }
}
