package com.sysu.edu.academic;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

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
        NavController navController =((NavHostFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_course_selection))).getNavController();
       // NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_course_selection);
        AppBarConfiguration appBarConfiguration =
                new AppBarConfiguration.Builder().setFallbackOnNavigateUpListener(() -> false).build();
        NavigationUI.setupWithNavController(binding.toolbar, navController, appBarConfiguration);
        binding.toolbar.setNavigationOnClickListener(view -> {
            if (Objects.requireNonNull(navController.getCurrentDestination()).getId()==R.id.selection_fragment) {
                supportFinishAfterTransition();
            }else{
                navController.navigateUp();
            }
        });
    }
}