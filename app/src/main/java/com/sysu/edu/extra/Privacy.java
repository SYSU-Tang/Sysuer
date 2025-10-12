package com.sysu.edu.extra;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.sysu.edu.databinding.ActivityPrivacyBinding;

public class Privacy extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityPrivacyBinding binding = ActivityPrivacyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.tool.setNavigationOnClickListener(e -> finishAfterTransition());

    }

}