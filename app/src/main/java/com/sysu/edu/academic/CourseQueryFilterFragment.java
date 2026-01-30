package com.sysu.edu.academic;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.FragmentCourseQueryFilterBinding;
import com.sysu.edu.preference.EditPreference;
import com.sysu.edu.preference.FilterPreference;
import com.sysu.edu.preference.RangeSliderPreference;
import com.sysu.edu.preference.SliderPreference;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import rikka.preference.SimpleMenuPreference;

public class CourseQueryFilterFragment extends PreferenceFragmentCompat {

    HttpManager http;
    FragmentCourseQueryFilterBinding binding;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.course_query_filter, rootKey);
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        if (binding == null) {
            binding = FragmentCourseQueryFilterBinding.inflate(inflater, container, false);
            binding.getRoot().addView(super.onCreateView(inflater, container, savedInstanceState));
            binding.fab.setOnClickListener(v -> {
                Navigation.findNavController(binding.getRoot()).navigate(R.id.query_to_result);
            });
            Params params = new Params(requireActivity());
            params.setCallback(this, () -> getData(0));
            Handler handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    super.handleMessage(msg);
                    if (msg.what == -1) {
                        params.toast(R.string.no_wifi_warning);
                    } else {
                        JSONObject response = JSONObject.parseObject((String) msg.obj);
                        Integer code = response.getInteger("code");
                        ArrayList<String> option = new ArrayList<>();
                        ArrayList<String> number = new ArrayList<>();
                        option.add("");
                        number.add("");
                        if (code == 200) {
                            if (msg.what < 6) {
                                response.getJSONArray("data").forEach(e -> {
                                    option.add(((JSONObject) e).getString(List.of(
                                            "acadYearSemester", "campusName", "dataName", "dataName", "name", "departmentName"
                                    ).get(msg.what)));
                                    number.add(((JSONObject) e).getString(List.of(
                                            "acadYearSemester", "campusNumber", "dataNumber", "dataNumber", "id", "departmentNumber"
                                    ).get(msg.what)));
                                });
                                ListPreference preference = Objects.requireNonNull(getPreferenceManager().findPreference(List.of(
                                        "yearSemester", "campus", "classLevel", "courseType", "teachingBuilding", "department"
                                ).get(msg.what)));
                                preference.setEntries(option.toArray(new String[]{}));
                                preference.setEntryValues(number.toArray(new String[]{}));
                                if (msg.what == 0) {
                                    preference = Objects.requireNonNull(getPreferenceManager().findPreference("endYear"));
                                    preference.setEntries(option.toArray(new String[]{}));
                                    preference.setEntryValues(number.toArray(new String[]{}));
                                }
                                if (msg.what < 5) getData(msg.what + 1);
                            }
                        } else if (code == 53000007) {
                            params.toast(R.string.login_warning);
                            params.gotoLogin(getView(), TargetUrl.JWXT);
                        } else {
                            params.toast(response.getString("message"));
                        }
                    }
                }
            };
            http = new HttpManager(handler);
            http.setParams(params);
            http.setReferrer("https://jwxt.sysu.edu.cn/jwxt/mk/");
            getData(0);
            FilterPreference department = Objects.requireNonNull(getPreferenceManager().findPreference("department"));
            department.getValueLiveData().observe(requireActivity(), v -> {
                System.out.println("department: " + department.getValueLiveData().getValue());
            });
        }
        return binding.getRoot();
    }

    public void getYearSemester() {
        http.getRequest("https://jwxt.sysu.edu.cn/jwxt/base-info/acadyearterm/findAcadyeartermNamesBox", 0);
    }

    public void getCampus() {
        http.getRequest("https://jwxt.sysu.edu.cn/jwxt/base-info/campus/findCampusNamesBox", 1);
    }

    public void getDepartment() {
        http.getRequest("https://jwxt.sysu.edu.cn/jwxt/base-info/department/findCommonDepartmentPull", 2);
    }

    public void getLevel() {
        http.getRequest("https://jwxt.sysu.edu.cn/jwxt/base-info/codedata/findcodedataNames?datableNumber=216", 3);
    }

    public void getType() {
        http.getRequest("https://jwxt.sysu.edu.cn/jwxt/base-info/codedata/findcodedataNames?datableNumber=350", 4);
    }

    public void getTeachingBuilding() {
        http.getRequest("https://jwxt.sysu.edu.cn/jwxt/base-info/teaching-building/pull", 5);
    }

    public void getTeachingBuilding(String text) {
        System.out.println(text);
//        http.getRequest("https://jwxt.sysu.edu.cn/jwxt/base-info/department/findCommonDepartmentPull?nameParm=" + text, 5);
    }

    public void getData(int pos) {
        http.getRequest(List.of("https://jwxt.sysu.edu.cn/jwxt/base-info/acadyearterm/findAcadyeartermNamesBox",
                "https://jwxt.sysu.edu.cn/jwxt/base-info/campus/findCampusNamesBox",
                "https://jwxt.sysu.edu.cn/jwxt/base-info/codedata/findcodedataNames?datableNumber=216",
                "https://jwxt.sysu.edu.cn/jwxt/base-info/codedata/findcodedataNames?datableNumber=350",
                "https://jwxt.sysu.edu.cn/jwxt/base-info/teaching-building/pull",
                "https://jwxt.sysu.edu.cn/jwxt/base-info/department/findCommonDepartmentPull").get(pos), pos);
    }

    public JSONObject getParams() {
        JSONObject params = new JSONObject();
        SliderPreference week = getPreferenceManager().findPreference("week");
        RangeSliderPreference weekRange = getPreferenceManager().findPreference("weekRange");
        RangeSliderPreference classRange = getPreferenceManager().findPreference("classRange");
        SimpleMenuPreference endYear = getPreferenceManager().findPreference("endYear");
        if (week != null && week.getValue() != 0) {
            params.put("weekDay", String.valueOf(week.getValue()));
        }
        if (weekRange != null) {
            if (weekRange.getValues()[0] == 0)
                params.put("beginWeek", String.valueOf(weekRange.getValues()[0]));
            if (weekRange.getValues()[1] == 0)
                params.put("endWeek", String.valueOf(weekRange.getValues()[1]));
        }
        if (classRange != null) {
            if (classRange.getValues()[0] == 0)
                params.put("beginLesson", String.valueOf(classRange.getValues()[0]));
            if (classRange.getValues()[1] == 0)
                params.put("endLesson", String.valueOf(classRange.getValues()[1]));
        }

        insertMenuValue(params, "yearSemester", "yearTerm");
        insertMenuValue(params, "endYear", "endYearTerm");

        return params;
        /*{"pageNo":1,"pageSize":10,"total":true,"param":{"yearTerm":"2025-1","endYearTerm":"2026-1","openingUnitNumber":"1","courseName":"名称","teachingNum":"教师","openingSchoolNumber":"5063559","courseCategoryNumber":"3286159","classLevelNumber":"1","classNumber":"班号","className":"教学班","teachingTypeNumber":"1","courseNumber":"编码","teachingBuildingID":"2513856","classRoomID":"2514104","weekDay":"1","beginWeek":"1","endWeek":"5","beginLesson":"2","endLesson":"3"}}*/
    }

    private void insertMenuValue(JSONObject params, String key, String value) {
        SimpleMenuPreference preference = getPreferenceManager().findPreference(key);
        if (preference != null && !preference.getValue().isEmpty()) {
            params.put(value, preference.getValue());
        }
    }

    private void insertEditValue(JSONObject params, String key, String value) {
        EditPreference preference = getPreferenceManager().findPreference(key);
        if (preference != null) {
            params.put(value, preference.getValue());
        }
    }
}