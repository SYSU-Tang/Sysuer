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
        this(context, attrs, defStyleAttr, 0);
    }

    public SliderPreference(@NonNull Context context) {
        this(context, null);
    }

    public SliderPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.sliderPreferenceStyle);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        PreferenceSliderBinding binding = PreferenceSliderBinding.bind(holder.itemView);
        binding.seekbar.setStepSize(getSeekBarIncrement());
        binding.seekbar.setValueTo(getMax());
        binding.seekbar.setValueFrom(getMin());
        binding.title.setText(getTitle());
        binding.icon.setImageDrawable(getIcon());
        binding.seekbarValue.setVisibility(getShowSeekBarValue() ? View.VISIBLE : View.GONE);
        binding.seekbarValue.setText(String.valueOf(getValue()));
        binding.seekbar.setValue(getValue());
        binding.seekbar.addOnChangeListener((slider, v, b) -> setValue((int) v));
        binding.getRoot().setOnClickListener(v -> onClick());
    }

    @Override
    protected void onClick() {
        super.onClick();
    }

    @Override
    public int getValue() {
        return super.getValue() == 0 ? getMin() : super.getValue();
    }
}
