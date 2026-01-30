package com.sysu.edu.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceViewHolder;

import com.sysu.edu.R;
import com.sysu.edu.databinding.PreferenceFilterBinding;

public class FilterPreference extends ListPreference {


    boolean isFilter;
    boolean canEdit;
    MutableLiveData<String> valueLiveData = new MutableLiveData<>();
    TextWatcher textWatcher;

    public FilterPreference(@NonNull Context context) {
        this(context, null);
    }

    public FilterPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.filterPreferenceStyle);
    }

    public FilterPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public FilterPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.preference_filter);
        try (TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.filterPreferenceStyle, defStyleAttr, defStyleRes)) {
            valueLiveData.setValue(a.getString(R.styleable.filterPreferenceStyle_value));
            isFilter = a.getBoolean(R.styleable.filterPreferenceStyle_isFilter, true);
            canEdit = a.getBoolean(R.styleable.filterPreferenceStyle_canEdit, false);
            a.recycle();
        } catch (Exception e) {
//            throw new RuntimeException(e);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        PreferenceFilterBinding binding = PreferenceFilterBinding.bind(holder.itemView);
        binding.textInputLayout.setStartIconDrawable(getIcon());
        binding.textInputLayout.setHint(getTitle());
        binding.textField.setText(valueLiveData.getValue(), isFilter);
        binding.textField.setInputType(canEdit ? InputType.TYPE_CLASS_TEXT : InputType.TYPE_NULL);
        if (getEntries() != null)
            binding.textField.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, getEntries()));
        binding.textField.setOnItemClickListener((parent, view, position, id) -> setValueIndex(position));
        if (binding.textField.isFocused())
            binding.textField.showDropDown();
        if (textWatcher == null) {
            textWatcher = new TextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (!s.toString().equals(valueLiveData.getValue()))
                        valueLiveData.setValue(s.toString());
                }
            };
            binding.textField.addTextChangedListener(textWatcher);
        }
        binding.getRoot().setOnClickListener(v -> {
            binding.textField.showDropDown();
        });

    }

    @Override
    protected void onClick() {

    }

    public MutableLiveData<String> getValueLiveData() {
        return valueLiveData;
    }

    /*@Override
    public String getValue() {
        return valueLiveData.getValue();
    }*/

    public void setIsFilter(boolean filter) {
        isFilter = filter;
        notifyChanged();
    }

    public void setEntriesAndValues(CharSequence[] values) {
        setEntries(values);
        setEntryValues(values);
        notifyChanged();
    }

}
