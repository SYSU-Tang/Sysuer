package com.sysu.edu.life;

import android.os.Bundle;

import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.sysu.edu.BaseActivity;
import com.sysu.edu.R;
import com.sysu.edu.databinding.ActivityGymPreservationBinding;

import java.util.Objects;

public class GymReservationActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityGymPreservationBinding binding = ActivityGymPreservationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        NavHostFragment fragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
        NavController navController = Objects.requireNonNull(fragment).getNavController();
        NavigationUI.setupWithNavController(binding.toolbar, navController, new AppBarConfiguration.Builder().setFallbackOnNavigateUpListener(() -> {
            supportFinishAfterTransition();
            return false;
        }).build());
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
    }
}