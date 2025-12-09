package com.sysu.edu.todo;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.sysu.edu.databinding.FragmentTodoBinding;
import com.sysu.edu.todo.info.TodoAdapter;

public class TodoFragment extends Fragment {

    FragmentTodoBinding binding;
    TodoAdapter adp;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentTodoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        adp = getAdapter(requireContext());
        binding.recyclerView.setAdapter(adp);
        binding.recyclerView.setLayoutManager(new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL));
        //adp.add(new TodoInfo().setTitle("title"));
        TodoViewModel viewModel = new ViewModelProvider(requireActivity()).get(TodoViewModel.class);
        viewModel.getTodoItem().observe(getViewLifecycleOwner(), adp::add);
        super.onViewCreated(view, savedInstanceState);
    }

    public TodoAdapter getAdapter(Context context) {
        if (adp == null)
            adp = new TodoAdapter(context);
        return adp;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}