package com.sysu.edu.view;

import static com.sysu.edu.api.CommonUtil.toStringOrDefault;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sysu.edu.databinding.DialogEditTextBinding;

import java.util.Objects;

public class EditTextDialog {
    private final AlertDialog dialog;
    private final DialogEditTextBinding binding;
    String mValue = "";
    ValueChangeListener listener;

    public EditTextDialog(Context context) {
        binding = DialogEditTextBinding.inflate(LayoutInflater.from(context));
        dialog = new MaterialAlertDialogBuilder(context)
                .setView(binding.getRoot())
                .setPositiveButton(android.R.string.ok, (_, _) -> setValue(toStringOrDefault(binding.edit.getText())))
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        getEditText().addTextChangedListener(new TextWatcher() {
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
        if (!Objects.equals(mValue, value)) {
            mValue = value;
            if (!getText().equals(toStringOrDefault(value)))
                getEditText().setText(toStringOrDefault(value));
            if (listener != null) listener.onValueChange(value);
        }
    }

    public void show() {
        dialog.show();
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(72, 32, 72, 0);
        binding.getRoot().setLayoutParams(params);
    }

    public void setTitle(int title) {
        dialog.setTitle(title);
    }

    public void setTitle(String title) {
        dialog.setTitle(title);
    }

    public void setHint(int hint) {
        binding.editLayout.setHint(hint);
    }

    public void setHint(String hint) {
        binding.editLayout.setHint(hint);
        getEditText().setContentDescription(hint);
    }

    public void setValueChangeListener(ValueChangeListener listener) {
        this.listener = listener;
    }

    public String getText() {
        return toStringOrDefault(getEditText().getText());
    }

    public AlertDialog getDialog() {
        return dialog;
    }

    public TextInputEditText getEditText() {
        return binding.edit;
    }

    public void setPasswordMode() {
        binding.edit.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        binding.editLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
    }

    public interface ValueChangeListener {
        void onValueChange(String value);
    }
}
