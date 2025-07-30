package com.sysu.edu.preference;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Preference extends androidx.preference.DropDownPreference {
    public Preference(@NonNull Context context) {
        super(context);
    }

    public Preference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public Preference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public Preference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onSetInitialValue(@Nullable Object defaultValue) {
        super.onSetInitialValue(defaultValue);
        setSummary(getEntries()[Integer.parseInt(getValue())]);
    }
    @Override
    protected boolean persistString(String value) {
        setSummary(getEntries()[Integer.parseInt(value)]);
        return super.persistString(value);
    }
}
