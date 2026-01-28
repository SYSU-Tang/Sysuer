package com.sysu.edu.academic;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.sysu.edu.R;
import com.sysu.edu.databinding.ActivityAssistantInfoBinding;

import java.util.Objects;

public class AssistantInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityAssistantInfoBinding binding = ActivityAssistantInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        NavController navController = ((NavHostFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.fragment))).getNavController();
        NavigationUI.setupWithNavController(binding.toolbar, navController,new AppBarConfiguration.Builder().setFallbackOnNavigateUpListener(() -> {
        supportFinishAfterTransition();
        return false;
        }).build());
    }
}