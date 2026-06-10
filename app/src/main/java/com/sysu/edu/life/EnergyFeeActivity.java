package com.sysu.edu.life;

import android.os.Bundle;

import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.sysu.edu.BaseActivity;
import com.sysu.edu.R;
import com.sysu.edu.databinding.ActivityWaterEletricityFeeBinding;

public class EnergyFeeActivity extends BaseActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityWaterEletricityFeeBinding binding = ActivityWaterEletricityFeeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        NavHostFragment fragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
        if (fragment != null) {
            NavController nav = fragment.getNavController();
            NavigationUI.setupWithNavController(binding.toolbar, nav, new AppBarConfiguration.Builder().setFallbackOnNavigateUpListener(() -> {
                supportFinishAfterTransition();
                return false;
            }).build());
            NavigationUI.setupWithNavController(binding.bottomNavigation, nav);
        }
    }
}
