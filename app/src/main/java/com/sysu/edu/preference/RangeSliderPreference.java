package com.sysu.edu.preference;

import android.content.Context;
import android.content.res.TypedArray;
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


    private float[] mValue;

    public RangeSliderPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.preference_range_slider);
        try (TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.rangeSliderPreferenceStyle, defStyleAttr, defStyleRes)) {
            mValue = new float[]{
                    a.getFloat(R.styleable.rangeSliderPreferenceStyle_valueFrom, getMin()),
                    a.getFloat(R.styleable.rangeSliderPreferenceStyle_valueTo, getMax())
            };
            a.recycle();
        } catch (Exception e) {
//            throw new RuntimeException(e);
        }
    }

    public RangeSliderPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public RangeSliderPreference(@NonNull Context context) {
        this(context, null);
    }

    public RangeSliderPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.rangeSliderPreferenceStyle);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        PreferenceRangeSliderBinding binding = PreferenceRangeSliderBinding.bind(holder.itemView);
        binding.seekbar.setStepSize(getSeekBarIncrement());
        binding.seekbar.setValueTo(getMax());
        binding.seekbar.setValueFrom(getMin());
        float[] values = getValues();
        binding.seekbar.setValues(values[0], values[1]);
        binding.title.setText(getTitle());
        binding.icon.setImageDrawable(getIcon());
        binding.seekbarValue.setVisibility(getShowSeekBarValue() ? View.VISIBLE : View.GONE);
        binding.seekbarValue.setText(String.format(Locale.getDefault(), "%.0f~%.0f", values[0], values[1]));
        binding.seekbar.addOnChangeListener((slider, v, b) -> setValues(new float[]{slider.getValues().get(0), slider.getValues().get(1)}));
        binding.getRoot().setOnClickListener(v -> onClick());
    }

    public float[] getValues() {
        return mValue;
    }

    public void setValues(float[] values) {
        mValue = values;
        notifyChanged();
        //setValue(values);
    }
}
