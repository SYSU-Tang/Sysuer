package com.sysu.edu.academic;

import android.os.Bundle;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.tabs.TabLayoutMediator;
import com.sysu.edu.R;
import com.sysu.edu.databinding.ActivityCourseSelectionBinding;
import com.sysu.edu.view.Pager2Adapter;

import java.util.List;
import java.util.stream.IntStream;

public class CourseSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCourseSelectionBinding binding = ActivityCourseSelectionBinding.inflate(getLayoutInflater());
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        setContentView(binding.getRoot());
        Pager2Adapter pager2Adapter = new Pager2Adapter(this);
        binding.pager2.setAdapter(pager2Adapter);
        new TabLayoutMediator(binding.tab, binding.pager2, (tab, position) -> tab.setText(List.of(R.string.course_selection, R.string.preview, R.string.course_selected).get(position))).attach();
        IntStream.range(0, 3).forEach(i -> pager2Adapter.add(CourseSelectionContainerFragment.newInstance(i)));
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
}