package com.sysu.edu.academic;

import android.os.Bundle;
import android.view.WindowManager;

import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.sysu.edu.BaseActivity;
import com.sysu.edu.R;
import com.sysu.edu.databinding.ActivityRoomQueryBinding;

import java.util.Objects;

public class RoomQueryActivity extends BaseActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityRoomQueryBinding binding = ActivityRoomQueryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        NavHostFragment nav = (NavHostFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.fragment));
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        NavigationUI.setupWithNavController(binding.toolbar, nav.getNavController(), new AppBarConfiguration.Builder().setFallbackOnNavigateUpListener(() -> {
            supportFinishAfterTransition();
            return false;
        }).build());
    }
}