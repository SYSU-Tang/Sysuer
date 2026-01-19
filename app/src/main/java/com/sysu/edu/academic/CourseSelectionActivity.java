package com.sysu.edu.academic;

import android.os.Bundle;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.tabs.TabLayout;
import com.sysu.edu.R;
import com.sysu.edu.databinding.ActivityCourseSelectionBinding;

import java.util.Objects;

public class CourseSelectionActivity extends AppCompatActivity {

    ActivityCourseSelectionBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCourseSelectionBinding.inflate(getLayoutInflater());
        binding.toolbar.setNavigationOnClickListener(view -> supportFinishAfterTransition());
        setContentView(binding.getRoot());
        //getSupportFragmentManager().beginTransaction().setReorderingAllowed(true).commit();
        NavController navController =((NavHostFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.course_selection_fragment))).getNavController();
       // NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_course_selection);
        AppBarConfiguration appBarConfiguration =
                new AppBarConfiguration.Builder().setFallbackOnNavigateUpListener(() -> false).build();
        NavigationUI.setupWithNavController(binding.toolbar, navController, appBarConfiguration);
        binding.tab.addTab(binding.tab.newTab().setText(R.string.course_selection));
        binding.tab.addTab(binding.tab.newTab().setText(R.string.preview));
        binding.tab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                navController.navigate(new int[]{R.id.selection_navigation,R.id.preview_navigation}[tab.getPosition()], null,new NavOptions.Builder().setLaunchSingleTop(true).build());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        binding.toolbar.setNavigationOnClickListener(view -> supportFinishAfterTransition());
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
}