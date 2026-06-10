package com.sysu.edu.todo;

import android.os.Bundle;

import com.sysu.edu.BaseActivity;
import com.sysu.edu.databinding.ActivityTodoBinding;

public class TodoActivity extends BaseActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityTodoBinding binding = ActivityTodoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        TodoManager todoManager = ((TodoFragment) binding.fragment.getFragment()).getTodoManager();
        binding.add.setOnClickListener(_ -> todoManager.showTodoAddDialog());
    }
}
