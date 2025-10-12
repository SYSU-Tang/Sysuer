package com.sysu.edu.extra;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.sysu.edu.databinding.ActivitySettingBinding;
import com.sysu.edu.preference.Language;

public class SettingActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState!=null){
            setResult(RESULT_OK);
        }
        ActivitySettingBinding binding = ActivitySettingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Language.setLanguage(this);
        binding.tool.setNavigationOnClickListener(v-> finishAfterTransition());
        }
}