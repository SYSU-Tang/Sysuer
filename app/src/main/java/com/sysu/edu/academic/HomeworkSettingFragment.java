package com.sysu.edu.academic;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.sysu.edu.databinding.FragmentHomeworkMainBinding;

public class HomeworkSettingFragment extends Fragment {
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FragmentHomeworkMainBinding binding = FragmentHomeworkMainBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }
}
