package com.sysu.edu.academic;

import android.os.Bundle;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.sysu.edu.R;
import com.sysu.edu.databinding.ActivityCourseQueryBinding;

import java.util.Objects;

public class CourseQueryActivity extends AppCompatActivity {

    ActivityCourseQueryBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCourseQueryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(v -> supportFinishAfterTransition());
        NavHostFragment nav = (NavHostFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.fragment));
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        NavigationUI.setupWithNavController(binding.toolbar, nav.getNavController(), new AppBarConfiguration.Builder().setFallbackOnNavigateUpListener(() -> {
            supportFinishAfterTransition();
            return false;
        }).build());

    }
}