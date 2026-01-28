package com.sysu.edu.preference;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SeekBarPreference;

import com.sysu.edu.R;
import com.sysu.edu.databinding.PreferenceSliderBinding;

public class SliderPreference extends SeekBarPreference {
    public SliderPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.preference_slider);
    }

    public SliderPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutResource(R.layout.preference_slider);
    }

    public SliderPreference(@NonNull Context context) {
        super(context);
        setLayoutResource(R.layout.preference_slider);
    }

    public SliderPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_slider);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        PreferenceSliderBinding binding = PreferenceSliderBinding.bind(holder.itemView);
        binding.seekbar.setStepSize(getSeekBarIncrement());
        binding.seekbar.setValueTo(getMax());
        binding.seekbar.setValueFrom(getMin());
        binding.title.setText(getTitle());
        binding.icon.setImageDrawable(getIcon());
        binding.seekbarValue.setVisibility(getShowSeekBarValue() ? View.VISIBLE : View.INVISIBLE);
        binding.seekbarValue.setText(String.valueOf(getValue()));
        binding.seekbar.setValue(getValue());
        binding.seekbar.addOnChangeListener((slider, v, b) -> binding.seekbarValue.setText(String.valueOf(v)));
    }

    @Override
    protected void onClick() {
        super.onClick();
    }
}
