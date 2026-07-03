package com.sysu.edu.preference;

import static com.sysu.edu.api.CommonUtil.trim;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.sysu.edu.R;
import com.sysu.edu.databinding.PreferenceEditBinding;

public class EditPreference extends Preference {
    
    private String mValue;
    
    public EditPreference(@NonNull Context context) {
        this(context, null);
    }
    
    public EditPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, android.R.attr.editTextPreferenceStyle);
    }
    
    public EditPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }
    
    public EditPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.preference_edit);
    }
    
    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        PreferenceEditBinding binding = PreferenceEditBinding.bind(holder.itemView);
        binding.textInputLayout.setHint(getTitle());
        binding.textInputLayout.setStartIconDrawable(getIcon());
        binding.textField.setText(getValue());
        binding.textField.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            
            }
            
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setValue(s.toString());
            }
        });
    }
    
    public String getValue() {
        return mValue;
    }
    
    public void setValue(String value) {
        mValue = value;
        persistString(value);
    }
    
    @Override
    protected void onSetInitialValue(@Nullable Object defaultValue) {
        setValue(getPersistedString(trim((String) defaultValue)));
    }
}
