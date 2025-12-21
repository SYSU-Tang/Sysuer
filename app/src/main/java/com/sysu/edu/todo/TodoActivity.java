package com.sysu.edu.todo;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.sysu.edu.databinding.ActivityTodoBinding;

public class TodoActivity extends AppCompatActivity {


    private InitTodo initTodo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityTodoBinding binding = ActivityTodoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.tool.setNavigationOnClickListener(view -> supportFinishAfterTransition());
        binding.add.setOnClickListener(view -> {
            initTodo.showTodoAddDialog();
        });
        initTodo = new InitTodo(this,binding.fragmentTodo.getFragment());

    }
}
