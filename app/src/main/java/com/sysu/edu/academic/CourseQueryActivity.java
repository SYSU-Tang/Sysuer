package com.sysu.edu.academic;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.fragment.NavHostFragment;

import com.alibaba.fastjson2.JSONObject;
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
        binding.toolbar.setNavigationOnClickListener(v->supportFinishAfterTransition());
        binding.toolbar.getMenu().add(getString(R.string.reset)).setIcon(R.drawable.reset).setOnMenuItemClickListener(menuItem -> {
            recreate();
            return false;
        }).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        NavHostFragment nav = (NavHostFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_course_query));
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        binding.fab.setOnClickListener(view -> {
            CourseQueryFragment fragment = (CourseQueryFragment) nav.getChildFragmentManager().getFragments().get(0);
            if (fragment != null) {
                setResult(RESULT_OK,getIntent().putExtra("filter",fragment.getMap()));
                supportFinishAfterTransition();
                JSONObject.from(fragment.getMap());
            }
        });
    }
}