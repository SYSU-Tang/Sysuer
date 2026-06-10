package com.sysu.edu.academic;

import android.os.Bundle;

import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.sysu.edu.BaseActivity;
import com.sysu.edu.R;
import com.sysu.edu.databinding.ActivityTrainingScheduleBinding;

public class TrainingProgramActivity extends BaseActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityTrainingScheduleBinding binding = ActivityTrainingScheduleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(binding.toolbar, navController, new AppBarConfiguration.Builder().setFallbackOnNavigateUpListener(() -> {
                supportFinishAfterTransition();
                return true;
            }).build());
        }
    }
}