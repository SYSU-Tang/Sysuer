package com.sysu.edu.browser;

import android.os.Bundle;

import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.sysu.edu.BaseActivity;
import com.sysu.edu.R;
import com.sysu.edu.databinding.ActivityJsActivityBinding;

public class JSActivity extends BaseActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityJsActivityBinding binding = ActivityJsActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        NavHostFragment fragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
        if (fragment != null)
            NavigationUI.setupWithNavController(binding.toolbar, fragment.getNavController(), new AppBarConfiguration.Builder().setFallbackOnNavigateUpListener(() -> {
                supportFinishAfterTransition();
                return false;
            }).build());
        getMenuInflater().inflate(R.menu.editor, binding.toolbar.getMenu());
    }
}