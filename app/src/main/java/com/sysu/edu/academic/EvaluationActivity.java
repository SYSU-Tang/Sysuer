package com.sysu.edu.academic;

import android.os.Bundle;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.sysu.edu.R;
import com.sysu.edu.databinding.ActivityEvaluationBinding;

import java.util.Objects;

public class EvaluationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityEvaluationBinding binding = ActivityEvaluationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        //binding.toolbar.setNavigationOnClickListener(v -> supportFinishAfterTransition());
        NavigationUI.setupWithNavController(binding.toolbar, ((NavHostFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.fragment))).getNavController(), new AppBarConfiguration.Builder().setFallbackOnNavigateUpListener(() -> {
            supportFinishAfterTransition();
            return true;
        }).build());
    }
}
