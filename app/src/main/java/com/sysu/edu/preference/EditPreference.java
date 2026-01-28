package com.sysu.edu.preference;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.sysu.edu.R;
import com.sysu.edu.databinding.PreferenceEditBinding;

public class EditPreference extends Preference {
    public EditPreference(@NonNull Context context) {
        super(context);
        setLayoutResource(R.layout.preference_edit);
    }

    public EditPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_edit);
    }

    public EditPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutResource(R.layout.preference_edit);
    }

    public EditPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.preference_edit);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        PreferenceEditBinding binding = PreferenceEditBinding.bind(holder.itemView);
        binding.textInputLayout.setHint(getTitle());
        binding.textInputLayout.setStartIconDrawable(getIcon());

    }
}
