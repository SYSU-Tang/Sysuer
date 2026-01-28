package com.sysu.edu.preference;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceViewHolder;

import com.sysu.edu.R;
import com.sysu.edu.databinding.PreferenceFilterBinding;

public class FilterPreference extends ListPreference {



    public FilterPreference(@NonNull Context context) {
        super(context);
        setLayoutResource(R.layout.preference_filter);
    }

    public FilterPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_filter);
    }

    public FilterPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutResource(R.layout.preference_filter);
    }

    public FilterPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.preference_filter);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        PreferenceFilterBinding binding = PreferenceFilterBinding.bind(holder.itemView);
        binding.textInputLayout.setStartIconDrawable(getIcon());
        binding.textInputLayout.setHint(getTitle());
        if (getEntries() != null)
            binding.textField.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, getEntries()));
    }

    @Override
    public void setTitle(@Nullable CharSequence title) {
        super.setTitle(title);
    }

    @Override
    protected void onClick() {

    }
}
