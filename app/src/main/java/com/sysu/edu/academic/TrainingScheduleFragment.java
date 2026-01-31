package com.sysu.edu.academic;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.transition.TransitionInflater;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.chip.Chip;
import com.sysu.edu.R;
import com.sysu.edu.databinding.FragmentTrainingScheduleBinding;

import java.util.ArrayList;
import java.util.Map;

public class TrainingScheduleFragment extends Fragment {

    final MutableLiveData<String> unit = new MutableLiveData<>();
    final MutableLiveData<String> profession = new MutableLiveData<>();
    final MutableLiveData<String> type = new MutableLiveData<>();
    final MutableLiveData<String> grade = new MutableLiveData<>();
    FragmentTrainingScheduleBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSharedElementReturnTransition(TransitionInflater.from(requireContext())
                .inflateTransition(android.R.transition.move));

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (binding == null) {
            binding = FragmentTrainingScheduleBinding.inflate(inflater);
            binding.unit.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    ((TrainingSchedule) requireActivity()).getColleges(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });
            binding.profession.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    ((TrainingSchedule) requireActivity()).getProfessions(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });
            binding.query.setOnClickListener(v -> {
                Bundle arg = new Bundle();
                arg.putString("unit", unit.getValue() == null ? "" : unit.getValue());
                arg.putString("profession", profession.getValue() == null ? "" : profession.getValue());
                arg.putString("grade", grade.getValue() == null ? "" : grade.getValue());
                arg.putString("type", type.getValue() == null ? "" : type.getValue());

                Navigation.findNavController(requireActivity(), R.id.fragment).navigate(R.id.confirmationAction,
                        arg, null
                        , new FragmentNavigator.Extras(Map.of(v, "result"))
                );
            });
        }
        return binding.getRoot();
    }

    public void deal(int what, JSONObject data) {
        switch (what) {
            case 1: {
                ArrayList<String> list = new ArrayList<>();
                ArrayList<String> unitIDs = new ArrayList<>();
                data.getJSONArray("data").forEach(e -> {
                    unitIDs.add(((JSONObject) e).getString("departmentNumber"));
                    list.add(((JSONObject) e).getString("departmentName"));
                });
                binding.unit.setSimpleItems(list.toArray(new String[]{}));
                if (binding.unit.hasFocus()) {
                    binding.unit.showDropDown();
                }
                binding.unit.setOnItemClickListener((parent, view, position, id) -> unit.setValue(unitIDs.get(position)));
                break;
            } // 处理学院
            case 2: {
                ArrayList<String> list = new ArrayList<>();
                ArrayList<String> professionIDs = new ArrayList<>();
                data.getJSONArray("data").forEach(e -> {
                    professionIDs.add(((JSONObject) e).getString("dataNumber"));
                    list.add(((JSONObject) e).getString("dataName"));
                });
                binding.grade.setMinValue(1);
                binding.grade.setMaxValue(list.size());
                binding.grade.setDisplayedValues(list.toArray(new String[0]));
                binding.grade.setOnValueChangedListener((slider, value, fromUser) -> grade.setValue(professionIDs.get(fromUser - 1)));
                binding.grade.setValue(list.size());
                grade.postValue(professionIDs.get(list.size() - 1));
                break;
            } // 处理年级
            case 3: {
                data.getJSONArray("data").forEach(e -> {
                    Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_filter_chip, binding.types, false);
                    chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        if (isChecked) {
                            type.setValue(((JSONObject) e).getString("dataNumber"));
                        }
                    });
                    chip.setText(((JSONObject) e).getString("dataName"));
                    binding.types.addView(chip);
                });
                ((Chip) binding.types.getChildAt(0)).setChecked(true);
                break;
            } // 处理类型
            case 4: {
                ArrayList<String> list = new ArrayList<>();
                ArrayList<String> professionIDs = new ArrayList<>();
                data.getJSONArray("data").forEach(e -> {
                    professionIDs.add(((JSONObject) e).getString("code"));
                    list.add(((JSONObject) e).getString("name"));
                });
                binding.profession.setSimpleItems(list.toArray(new String[]{}));
                if (binding.profession.hasFocus()) {
                    binding.profession.showDropDown();
                }
                binding.profession.setOnItemClickListener((parent, view, position, id) ->
                        profession.setValue(professionIDs.get(position)));
                break; // 处理专业
            }
        }

    }

}