package com.sysu.edu.academic;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.navigation.Navigation;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.databinding.FragmentAssistantInfoFilterBinding;
import com.sysu.edu.databinding.ItemFilterChipBinding;
import com.sysu.edu.model.JwxtModel;

public class AssistantInfoFilterFragment extends Fragment {
    
    final MutableLiveData<String> term = new MutableLiveData<>();
    final MutableLiveData<String> campus = new MutableLiveData<>();
    FragmentAssistantInfoFilterBinding binding;
    JwxtModel model;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        if (binding == null) {
            binding = FragmentAssistantInfoFilterBinding.inflate(inflater, container, false);
            PopupMenu pop = new PopupMenu(requireContext(), binding.term.getRoot());
            model = new JwxtModel(requireContext());
            binding.term.itemTitle.setText(R.string.term);
            binding.term.itemIcon.setImageResource(R.drawable.calendar);
            binding.term.getRoot().setOnClickListener(_ -> pop.show());
            binding.filter.setOnClickListener(_ -> {
                Bundle data = new Bundle();
                data.putString("term", term.getValue());
                data.putString("campus", campus.getValue());
                data.putString("courseNumber", String.valueOf(binding.courseNumber.getText()));
                data.putString("courseName", String.valueOf(binding.courseName.getText()));
                data.putString("teacherName", String.valueOf(binding.teacher.getText()));
                Navigation.findNavController(binding.getRoot()).navigate(R.id.filter_to_result, data);
            });
            term.observe(requireActivity(), acadYearSemester -> {
                if (acadYearSemester != null) {
                    binding.term.itemContent.setText(acadYearSemester);
                }
            });
            model.getMessage().observe(requireActivity(), message -> {
                JSONObject response = message.getSecond();
                if (response.getInteger("code") == 200) {
                    switch (message.getFirst()) {
                        case 0 -> {
                            response.getJSONArray("data").forEach(t -> pop.getMenu().add(((JSONObject) t).getString("acadYearSemester")).setOnMenuItemClickListener(_ -> {
                                term.setValue(((JSONObject) t).getString("acadYearSemester"));
                                return false;
                            }));
                            getCampuses();
                        }
                        case 1 -> response.getJSONArray("data").forEach(c -> {
                            ItemFilterChipBinding item = ItemFilterChipBinding.inflate(inflater, binding.campus, false);
                            item.getRoot().setText(((JSONObject) c).getString("campusName"));
                            item.getRoot().setOnCheckedChangeListener((_, isChecked) -> {
                                if (isChecked) {
                                    campus.setValue(((JSONObject) c).getString("id"));
                                }
                            });
                            binding.campus.addView(item.getRoot());
                        });
                    }
                    model.nextAll();
                }
            });
            getTerms();
            model.next();
        }
        return binding.getRoot();
    }
    
    
    void getTerms() {
        model.add("jwxt/base-info/acadyearterm/findAcadyeartermNamesBox", 0);
    }
    
    
    void getCampuses() {
        model.add("jwxt/base-info/campus/findCampusNamesBox", 1);
    }
}