package com.sysu.edu.academic;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.sysu.edu.R;
import com.sysu.edu.databinding.FragmentContainerBinding;

import java.util.List;
import java.util.Objects;

public class CourseSelectionContainerFragment extends Fragment {
    
    NavController nav;
    
    public static CourseSelectionContainerFragment newInstance(int position) {
        Bundle args = new Bundle();
        args.putInt("position", position);
        CourseSelectionContainerFragment fragment = new CourseSelectionContainerFragment();
        fragment.setArguments(args);
        return fragment;
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentContainerBinding binding = FragmentContainerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        nav = ((NavHostFragment) Objects.requireNonNull(getChildFragmentManager().findFragmentById(R.id.nav_host_fragment))).getNavController();
        int graph = List.of(R.navigation.course_selection_nav, R.navigation.course_preview_nav, R.navigation.course_selected_nav).get(requireArguments().getInt("position"));
        nav.setGraph(graph);
        NavigationUI.setupWithNavController(requireActivity().findViewById(R.id.toolbar), nav, new AppBarConfiguration.Builder().setFallbackOnNavigateUpListener(() -> {
            requireActivity().supportFinishAfterTransition();
            return true;
        }).build());
    }
    
    @Override
    public void onResume() {
        NavigationUI.setupWithNavController(requireActivity().findViewById(R.id.toolbar), nav, new AppBarConfiguration.Builder().setFallbackOnNavigateUpListener(() -> {
            requireActivity().supportFinishAfterTransition();
            return false;
        }).build());
        super.onResume();
    }
}
