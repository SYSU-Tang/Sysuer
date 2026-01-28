package com.sysu.edu.academic;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.transition.TransitionInflater;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.sysu.edu.R;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.FragmentCourseQueryBinding;

import java.util.ArrayList;
import java.util.HashMap;


public class CourseFilterFragment extends Fragment {

    HttpManager http;
    HashMap<String, String> filterValue = new HashMap<>();
    HashMap<String, String> filterName = new HashMap<>();
    CourseSelectionViewModel vm;
    FragmentCourseQueryBinding binding;
    Params params;
    NavController navController;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setSharedElementEnterTransition(TransitionInflater.from(requireContext())
                .inflateTransition(android.R.transition.move));
        setSharedElementReturnTransition(TransitionInflater.from(requireContext())
                .inflateTransition(android.R.transition.move));
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        if (binding == null) {
            vm = new ViewModelProvider(requireActivity()).get(CourseSelectionViewModel.class);
            filterValue = vm.getFilterValue();
            filterName = vm.getFilterName();
            binding = FragmentCourseQueryBinding.inflate(inflater, container, false);
            binding.container.setColumnCount(new Params(requireActivity()).getColumn());
            params = new Params(requireActivity());
            params.setCallback(this, () -> getData(0));
            Handler handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    if (msg.what == -1) {
                        params.toast(getString(R.string.no_wifi_warning));
                        return;
                    }
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    if (response != null) {
                        if (response.getInteger("code").equals(200)) {
                            JSONArray data = response.getJSONArray("data");
                            if (data != null) {
                                if (msg.what < 4) {
                                    getData(msg.what + 1);
                                }
                                ArrayList<String> items = new ArrayList<>();
                                ArrayList<String> itemCodes = new ArrayList<>();
                                items.add("");
                                itemCodes.add("");
                                data.forEach(a -> {
                                    items.add(((JSONObject) a).getString(new String[]{"campusName", "dataName", "minorName", "dataName", "dataName"}[msg.what]));
                                    itemCodes.add(((JSONObject) a).getString(new String[]{"id", "dataNumber", "sectionNumber", "dataNumber", "dataNumber"}[msg.what]));
                                });
                                MaterialAutoCompleteTextView v = new MaterialAutoCompleteTextView[]{binding.campus, binding.days, binding.sections, binding.languages, binding.special}[msg.what];
                                v.setSimpleItems(items.toArray(new String[]{}));
                                final int a = msg.what;
                                v.setOnItemClickListener((adapterView, view, i, l) -> {
                                    filterValue.put(new String[]{"campus", "day", "section", "language", "special"}[a], itemCodes.get(i));
                                    filterName.put(new String[]{"campus", "day", "section", "language", "special"}[a], items.get(i));
                                });
                            }
                        } else if (response.getInteger("code").equals(50021000)) {
                            params.toast(response.getString("message"));
                        } else if (response.getInteger("code").equals(53000007)) {
                            params.toast(R.string.login_warning);
                            params.gotoLogin(binding.getRoot(), TargetUrl.JWXT);
                        }
                    }
                    super.handleMessage(msg);
                }
            };
            http = new HttpManager(handler);
            http.setParams(params);
            http.setReferrer("https://jwxt.sysu.edu.cn/jwxt/mk/courseSelection/?code=jwxsd_xk&resourceName=%E9%80%89%E8%AF%BE");
            getData(0);
            load();

        }
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        submit();
                    }
                }
        );
        return binding.getRoot();
    }

    void reset() {
        filterValue.clear();
        filterName.clear();
        vm.setFilterName(filterName);
        vm.setFilterValue(filterValue);
        load();
    }

    void load() {
        filterName = vm.getFilterName();
        filterValue = vm.getFilterValue();
        binding.campus.setText(filterName.getOrDefault("campus", ""), false);
        binding.course.setText(filterName.getOrDefault("course", ""));
        binding.days.setText(filterName.get("day"), false);
        binding.sections.setText(filterName.get("section"), false);
        binding.languages.setText(filterName.get("language"), false);
        binding.special.setText(filterName.getOrDefault("special", ""), false);
        binding.school.setText(filterName.get("school"));
        binding.teacher.setText(filterName.get("teacher"));
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        binding.reset.setOnClickListener(v -> reset());
        binding.submit.setOnClickListener(v -> submit());
    }

    private void submit() {
        vm.setReturnData(parseFilter(getMap()));
        vm.setFilterName(filterName);
        vm.setFilterValue(filterValue);
        navController.navigateUp();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
//        binding = null;
    }

    void getData(int i) {
        http.getRequest(new String[]{"https://jwxt.sysu.edu.cn/jwxt/base-info/campus/findCampusNamesBox",
                "https://jwxt.sysu.edu.cn/jwxt/base-info/codedata/findcodedataNames?datableNumber=233",
                "https://jwxt.sysu.edu.cn/jwxt/base-info/AcadyeartermSet/minorName?schoolYear=2025-1",
                "https://jwxt.sysu.edu.cn/jwxt/base-info/codedata/findcodedataNames?datableNumber=204",
                "https://jwxt.sysu.edu.cn/jwxt/base-info/codedata/findcodedataNames?datableNumber=387"}[i], i);
    }

    public HashMap<String, String> getMap() {
        filterValue.put("course", binding.course.getText() == null ? "" : binding.course.getText().toString());
        filterValue.put("teacher", binding.teacher.getText() == null ? "" : binding.teacher.getText().toString());
        filterValue.put("school", binding.school.getText() == null ? "" : binding.school.getText().toString());
        filterName.put("course", binding.course.getText() == null ? "" : binding.course.getText().toString());
        filterName.put("teacher", binding.teacher.getText() == null ? "" : binding.teacher.getText().toString());
        filterName.put("school", binding.school.getText() == null ? "" : binding.school.getText().toString());
        return filterValue;
    }

    String parseFilter(HashMap<String, String> filter) {
        StringBuilder str = new StringBuilder();
        String[] keys = new String[]{"course", "campus", "day", "section", "school", "teacher", "language", "special"};
        String[] key = new String[]{"courseName", "studyCampusId", "week", "classTimes", "courseUnitNum", "teachingTeacherNum", "teachingLanguageCode", "specialClassCode"};
        for (int i = 0; i < keys.length; i++) {
            String v = filter.getOrDefault(keys[i], "");
            if (v != null && !v.isEmpty()) {
                str.append(String.format(",\"%s\":\"%s\"", key[i], v));
            }
        }
        return str.toString();
    }
}