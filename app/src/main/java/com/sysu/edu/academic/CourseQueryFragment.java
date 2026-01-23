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
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.FragmentCourseQueryBinding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class CourseQueryFragment extends Fragment {

    final OkHttpClient http = new OkHttpClient.Builder().build();
    Handler handler;
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
            handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    if (msg.what == -1) {
                        params.toast(getString(R.string.no_wifi_warning));
                        return;
                    }
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    if (response != null) {
                        if (response.getInteger("code").equals(200)) {
                            switch (msg.what) {
                                case -1:
                                    break;
                                case 1:
                                    JSONArray data = response.getJSONArray("data");
                                    if (data != null) {
                                        if (msg.arg1 < 4) {
                                            getData(msg.arg1 + 1);
                                        }
                                        ArrayList<String> items = new ArrayList<>();
                                        ArrayList<String> itemCodes = new ArrayList<>();
                                        items.add("");
                                        itemCodes.add("");
                                        data.forEach(a -> {
                                            items.add(((JSONObject) a).getString(new String[]{"campusName", "dataName", "minorName", "dataName", "dataName"}[msg.arg1]));
                                            itemCodes.add(((JSONObject) a).getString(new String[]{"id", "dataNumber", "sectionNumber", "dataNumber", "dataNumber"}[msg.arg1]));
                                        });
                                        MaterialAutoCompleteTextView v = new MaterialAutoCompleteTextView[]{binding.campuses, binding.days, binding.sections, binding.languages, binding.special}[msg.arg1];
                                        v.setSimpleItems(items.toArray(new String[]{}));
                                        final int a = msg.arg1;
                                        v.setOnItemClickListener((adapterView, view, i, l) -> {
                                            filterValue.put(new String[]{"campus", "day", "section", "language", "special"}[a], itemCodes.get(i));
                                            filterName.put(new String[]{"campus", "day", "section", "language", "special"}[a], items.get(i));
                                        });
                                    }
                                    break;
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
        binding.campuses.setText(filterName.getOrDefault("campus", ""), false);
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
        http.newCall(new Request.Builder().url(new String[]{"https://jwxt.sysu.edu.cn/jwxt/base-info/campus/findCampusNamesBox",
                        "https://jwxt.sysu.edu.cn/jwxt/base-info/codedata/findcodedataNames?datableNumber=233",
                        "https://jwxt.sysu.edu.cn/jwxt/base-info/AcadyeartermSet/minorName?schoolYear=2025-1",
                        "https://jwxt.sysu.edu.cn/jwxt/base-info/codedata/findcodedataNames?datableNumber=204",
                        "https://jwxt.sysu.edu.cn/jwxt/base-info/codedata/findcodedataNames?datableNumber=387"}[i])
                .header("Cookie", params.getCookie())
                .header("Referer", "https://jwxt.sysu.edu.cn/jwxt/mk/courseSelection/?code=jwxsd_xk&resourceName=%E9%80%89%E8%AF%BE")
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = new Message();
                msg.what = -1;
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what = 1;
                msg.arg1 = i;
                msg.obj = response.body().string();
                handler.sendMessage(msg);
            }
        });
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