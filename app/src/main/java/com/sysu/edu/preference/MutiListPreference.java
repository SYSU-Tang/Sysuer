package com.sysu.edu.preference;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.MultiSelectListPreference;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sysu.edu.R;

public class MutiListPreference extends MultiSelectListPreference {
    public MutiListPreference(@NonNull Context context) {
        this(context, null);
    }
    
    public MutiListPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.dialogPreferenceStyle);
    }
    
    public MutiListPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }
    
    public MutiListPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
    
    @Override
    protected void onClick() {
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(getContext());
        if (getTitle() != null) dialogBuilder.setTitle(getTitle());
        if (getPositiveButtonText() != null)
            dialogBuilder.setPositiveButton(getPositiveButtonText(), (_, _) -> {
                persistStringSet(getValues());
                notifyChanged();
            });
        if (getNegativeButtonText() != null)
            dialogBuilder.setNegativeButton(getNegativeButtonText(), (dialog, _) -> dialog.dismiss());
        if (getEntries() != null)
            dialogBuilder.setMultiChoiceItems(getEntries(), getSelectedItems(), (_, which, isChecked) -> {
                getValues().remove(getEntryValues()[which].toString());
                if (isChecked) getValues().add(getEntryValues()[which].toString());
                persistStringSet(getValues());
            });
        dialogBuilder.show();
    }
    
}
