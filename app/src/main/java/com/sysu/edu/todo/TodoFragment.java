package com.sysu.edu.todo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.sysu.edu.databinding.FragmentTodoBinding;
public class TodoFragment extends Fragment {

    FragmentTodoBinding binding;
    ConcatAdapter concatAdapter = new ConcatAdapter(new ConcatAdapter.Config.Builder().setIsolateViewTypes(true).build());
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentTodoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        binding.recyclerView.setAdapter(concatAdapter);
        binding.recyclerView.setLayoutManager(new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL));
        //TodoViewModel viewModel = new ViewModelProvider(requireActivity()).get(TodoViewModel.class);
        super.onViewCreated(view, savedInstanceState);
    }

   /* public TodoAdapter getTodoAdapter(Context context) {
        if (todoAdapter == null)
            todoAdapter = new TodoAdapter(context);
        return todoAdapter;
    }*/
//    public void addTitleAdapter(TitleAdapter titleAdp) {
//        concatAdapter.addAdapter(titleAdp);
//    }
//    public void addTodoAdapter(TodoAdapter todoAdapter) {
//        concatAdapter.addAdapter(todoAdapter);
//    }
    public ConcatAdapter getConcatAdapter() {
        return concatAdapter;
    }
//    public TitleAdapter getTitleAdapter(Context context) {
//        if (titleAdp == null)
//            titleAdp = new TitleAdapter(context);
//        return titleAdp;
//    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}