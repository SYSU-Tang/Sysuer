package com.sysu.edu.academic;

import static com.sysu.edu.api.CommonUtil.extractValue;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.preference.PreferenceFragmentCompat;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.CommonUtil;
import com.sysu.edu.databinding.FragmentQueryBinding;
import com.sysu.edu.model.JwxtModel;
import com.sysu.edu.preference.FilterPreference;
import com.sysu.edu.preference.PreferenceUtil;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

import rikka.preference.SimpleMenuPreference;

public class AssistantEvaluationQueryFragment extends PreferenceFragmentCompat {
    FragmentQueryBinding binding;
    JwxtModel model;
    
    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.assisant_evaluation, rootKey);
    }
    
    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            model = new JwxtModel(requireContext());
            binding = FragmentQueryBinding.inflate(inflater, container, false);
            binding.getRoot().addView(super.onCreateView(inflater, binding.getRoot(), null));
            binding.fab.setOnClickListener(v -> {
                Bundle data = new Bundle();
                data.putString("params", getParams().toString());
                Navigation.findNavController(binding.getRoot()).navigate(R.id.assistant_evaluation_result, data,
                        new NavOptions.Builder().setEnterAnim(android.R.anim.fade_in).setExitAnim(android.R.anim.fade_out).build()
                        , new FragmentNavigator.Extras(Map.of(v, "query")));
            });
            FilterPreference unit = Objects.requireNonNull(findPreference("unit"));
            unit.getValueLiveData().observe(requireActivity(), this::getUnit);
            model.getMessage().observe(requireActivity(), message -> {
                JSONObject response = message.second;
                if (response.getInteger("code") == 200) {
                    switch (message.first) {
                        case 0 -> {
                            String[] years = extractValue(response.getJSONArray("data"), "acadYearSemester").toArray(new String[0]);
                            SimpleMenuPreference yearTerm = Objects.requireNonNull(findPreference("yearTerm"));
                            yearTerm.setEntries(years);
                            yearTerm.setEntryValues(years);
                            getUnit(unit.getValue());
                        }
                        case 1 -> {
                            CommonUtil.Tuple2<ArrayList<String>, ArrayList<String>> extractValue = extractValue(response.getJSONArray("data"), "departmentName", "departmentNumber");
                            unit.setEntries(extractValue.first.toArray(new String[0]));
                            unit.setEntryValues(extractValue.second.toArray(new String[0]));
                        }
                    }
                    model.nextAll();
                }
            });
            getYearTerm();
            model.next();
        }
        return binding.getRoot();
    }
    
    void getYearTerm() {
        model.add("jwxt/base-info/acadyearterm/findAcadyeartermNamesBox", 0);
    }
    
    void getUnit(String params) {
        model.add("jwxt/base-info/department/findCommonDepartmentPull?nameParm=" + params, 1);
    }
    
    JSONObject getParams() {
        PreferenceUtil preferenceUtil = new PreferenceUtil(this);
        preferenceUtil.insertMenuValue("yearTerm", "yearTerm");
        preferenceUtil.insertEditValue("teacher", "teacherName");
        preferenceUtil.insertEditValue("courseName", "courseName");
        preferenceUtil.insertFilterValue("unit", "openUnitNum");
        return preferenceUtil.getParams();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        model.dispose();
    }
}