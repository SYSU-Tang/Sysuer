package com.sysu.edu.academic;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.databinding.FragmentQueryBinding;
import com.sysu.edu.model.JwxtModel;
import com.sysu.edu.preference.FilterPreference;
import com.sysu.edu.preference.PreferenceUtil;
import com.sysu.edu.preference.RangeSliderPreference;
import com.sysu.edu.preference.SliderPreference;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public class CourseQueryFilterFragment extends PreferenceFragmentCompat {
    
    JwxtModel model;
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        model.dispose();
    }
    
    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.course_query_filter, rootKey);
    }
    
    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        model = new JwxtModel(requireContext());
        FragmentQueryBinding binding = FragmentQueryBinding.inflate(inflater, container, false);
        binding.getRoot().addView(super.onCreateView(inflater, container, savedInstanceState));
        binding.fab.setOnClickListener(_ -> {
            Bundle bundle = new Bundle();
            bundle.putString("params", getParams().toString());
            Navigation.findNavController(binding.getRoot()).navigate(R.id.query_to_result, bundle, new NavOptions.Builder().build());
        });
        FilterPreference department = Objects.requireNonNull(findPreference("department"));
        FilterPreference classroom = Objects.requireNonNull(findPreference("classroom"));
        department.getValueLiveData().observe(requireActivity(), this::getTeachingBuilding);
        classroom.getValueLiveData().observe(requireActivity(), this::getClassroom);
        IntStream.range(0, 6).forEach(this::getData);
        model.getMessage().observe(requireActivity(), message -> {
            JSONObject response = message.getSecond();
            Integer code = response.getInteger("code");
            if (code == 200) {
                ArrayList<String> option = new ArrayList<>();
                ArrayList<String> number = new ArrayList<>();
                JSONArray data = response.getJSONArray("data");
                int what = message.getFirst();
                switch (what) {
                    case 0, 1, 2, 3, 4, 5 -> {
                        option.add("");
                        number.add("");
                        data.forEach(e -> {
                            JSONObject item = (JSONObject) e;
                            option.add(item.getString(List.of(
                                    "acadYearSemester", "campusName", "dataName", "dataName", "name", "departmentName"
                            ).get(what)));
                            number.add(item.getString(List.of(
                                    "acadYearSemester", "id", "dataNumber", "dataNumber", "id", "departmentNumber"
                            ).get(what)));
                        });
                        ListPreference preference = Objects.requireNonNull(findPreference(List.of(
                                "yearSemester", "campus", "classLevel", "teachingType", "teachingBuilding", "department"
                        ).get(what)));
                        preference.setEntries(option.toArray(new String[]{}));
                        preference.setEntryValues(number.toArray(new String[]{}));
                        if (what == 0) {
                            preference = Objects.requireNonNull(findPreference("endYear"));
                            preference.setEntries(option.toArray(new String[]{}));
                            preference.setEntryValues(number.toArray(new String[]{}));
                        }
                    }
                    case 6 -> {
                        data.forEach(e -> {
                            JSONObject item = (JSONObject) e;
                            option.add(item.getString("number"));
                            number.add(item.getString("id"));
                        });
                        classroom.setEntries(option.toArray(new String[]{}));
                        classroom.setEntryValues(number.toArray(new String[]{}));
                    }
                }
                model.nextAll();
            }
        });
        model.next();
        return binding.getRoot();
    }

//    public void getYearSemester() {
//        model.add("jwxt/base-info/acadyearterm/findAcadyeartermNamesBox", 0);
//    }
//
//    public void getCampus() {
//        model.add("jwxt/base-info/campus/findCampusNamesBox", 1);
//    }
//
//    public void getDepartment() {
//        model.add("jwxt/base-info/department/findCommonDepartmentPull", 2);
//    }
//
//    public void getLevel() {
//        model.add("jwxt/base-info/codedata/findcodedataNames?datableNumber=216", 3);
//    }
//
//    public void getType() {
//        model.add("jwxt/base-info/codedata/findcodedataNames?datableNumber=350", 4);
//    }
    
    public void getTeachingBuilding(String text) {
        model.add("jwxt/base-info/department/findCommonDepartmentPull?nameParm=" + text, 5);
    }
    
    public void getClassroom(String text) {
        model.add("jwxt/base-info/classroom/getClassRoomAllPull", String.format("{\"queryParam\":\"%s\"}", text), 6);
    }
    
    public void getData(int pos) {
        model.add(List.of("jwxt/base-info/acadyearterm/findAcadyeartermNamesBox",
                "jwxt/base-info/campus/findCampusNamesBox",
                "jwxt/base-info/codedata/findcodedataNames?datableNumber=216",
                "jwxt/base-info/codedata/findcodedataNames?datableNumber=350",
                "jwxt/base-info/teaching-building/pull",
                "jwxt/base-info/department/findCommonDepartmentPull").get(pos), pos);
    }
    
    public JSONObject getParams() {
        PreferenceUtil preferenceUtil = new PreferenceUtil(this);
        SliderPreference week = findPreference("week");
        RangeSliderPreference weekRange = findPreference("weekRange");
        RangeSliderPreference classRange = findPreference("classRange");
        if (week != null && week.getValue() != 0)
            preferenceUtil.insert("weekDay", String.valueOf(week.getValue()));
        if (weekRange != null) {
            if ((int) weekRange.getValues()[0] != 0)
                preferenceUtil.insert("beginWeek", String.valueOf((int) weekRange.getValues()[0]));
            if ((int) weekRange.getValues()[1] != 0)
                preferenceUtil.insert("endWeek", String.valueOf((int) weekRange.getValues()[1]));
        }
        if (classRange != null) {
            if ((int) classRange.getValues()[0] != 0)
                preferenceUtil.insert("beginLesson", String.valueOf((int) classRange.getValues()[0]));
            if ((int) classRange.getValues()[1] != 0)
                preferenceUtil.insert("endLesson", String.valueOf((int) classRange.getValues()[1]));
        }
        preferenceUtil.insertMenuValue("yearSemester", "yearTerm");
        preferenceUtil.insertMenuValue("endYear", "endYearTerm");
        preferenceUtil.insertMenuValue("classLevel", "classLevelNumber");
        preferenceUtil.insertMenuValue("campus", "openingSchoolNumber");
        preferenceUtil.insertMenuValue("courseType", "courseCategoryNumber");
        preferenceUtil.insertMenuValue("teachingBuilding", "teachingBuildingID");
        preferenceUtil.insertMenuValue("teachingType", "teachingTypeNumber");
        preferenceUtil.insertFilterValue("classroom", "classRoomID");//教室
        preferenceUtil.insertFilterValue("department", "openingUnitNumber");//开课单位
        preferenceUtil.insertEditValue("courseName", "courseName");//课程名称
        preferenceUtil.insertEditValue("teacher", "teachingNum");//教师
        preferenceUtil.insertEditValue("classNumber", "classNumber");//班号
        preferenceUtil.insertEditValue("className", "className");//教学班
        preferenceUtil.insertEditValue("courseNumber", "courseNumber");//课程编码
        return preferenceUtil.getParams();
        /*{"pageNo":1,"pageSize":10,"total":true,"param":{"yearTerm":"2025-1","endYearTerm":"2026-1","openingUnitNumber":"1","courseName":"名称","teachingNum":"教师","openingSchoolNumber":"5063559","courseCategoryNumber":"3286159","classLevelNumber":"1","classNumber":"班号","className":"教学班","teachingTypeNumber":"1","courseNumber":"编码","teachingBuildingID":"2513856","classRoomID":"2514104","weekDay":"1","beginWeek":"1","endWeek":"5","beginLesson":"2","endLesson":"3"}}*/
    }
}