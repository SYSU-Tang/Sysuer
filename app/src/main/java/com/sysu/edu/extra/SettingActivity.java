package com.sysu.edu.extra;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.sysu.edu.databinding.ActivitySettingBinding;
import com.sysu.edu.preference.Language;

public class SettingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            setResult(RESULT_OK);
        }
        ActivitySettingBinding binding = ActivitySettingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Language.setLanguage(this);
        binding.tool.setNavigationOnClickListener(v -> finishAfterTransition());
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        Configuration configuration = newBase.getResources().getConfiguration();
        String fontValue = PreferenceManager.getDefaultSharedPreferences(newBase).getString("fontSize", "0");
        if (!fontValue.equals("0")) {
            configuration.fontScale = new float[]{0.5f, 0.75f, 1.0f, 1.25f, 1.5f}[Integer.parseInt(fontValue) - 1];
        }
        applyOverrideConfiguration(configuration);
    }
}