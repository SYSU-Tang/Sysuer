package com.sysu.edu.browser;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.sysu.edu.databinding.ActivityHtmlViewBinding;

public class HtmlViewActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityHtmlViewBinding binding = ActivityHtmlViewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
    }
}