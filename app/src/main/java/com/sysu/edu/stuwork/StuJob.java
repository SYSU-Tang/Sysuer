package com.sysu.edu.stuwork;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;

import com.sysu.edu.R;
import com.sysu.edu.databinding.ActivityStudentJobBinding;

public class StuJob extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityStudentJobBinding binding = ActivityStudentJobBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        //binding.getRoot().openDrawer(GravityCompat.START,true);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, binding.getRoot(), binding.toolbar, R.string.open, R.string.close);
        toggle.syncState();
        binding.getRoot().addDrawerListener(toggle);
    }
}
