package com.sysu.edu.academic;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.transition.TransitionInflater;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.sysu.edu.R;
import com.sysu.edu.api.CourseSelectionViewModel;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.FragmentCourseQueryBinding;
import com.sysu.edu.login.LoginActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class CourseQueryFragment extends Fragment {

    OkHttpClient http = new OkHttpClient.Builder().build();
    Handler handler;
    String cookie;
    HashMap<String,String>filter = new HashMap<>();
    HashMap<String,String>filterName = new HashMap<>();
    CourseSelectionViewModel vm;
    private FragmentCourseQueryBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setSharedElementEnterTransition(TransitionInflater.from(requireContext())
                .inflateTransition(android.R.transition.move));
        setSharedElementReturnTransition(TransitionInflater.from(requireContext())
                .inflateTransition(android.R.transition.move));
        vm = new ViewModelProvider(requireActivity()).get(CourseSelectionViewModel.class);

        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (savedInstanceState==null)
        {
            binding = FragmentCourseQueryBinding.inflate(inflater, container, false);
//            getParentFragmentManager().beginTransaction()
//                    // .setCustomAnimations()
//                    .addSharedElement(binding.submit, "miniapp")
//                    .addToBackStack(null)
//                    .commit();
            binding.container.setColumnCount(new Params(requireActivity()).getColumn());
            ActivityResultLauncher<Intent> launch = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
                if (o.getResultCode() == Activity.RESULT_OK) {
                    cookie = requireContext().getSharedPreferences("privacy", Context.MODE_PRIVATE).getString("Cookie", "");
                    getData(0);
                }
            });
            reset();
            cookie = requireContext().getSharedPreferences("privacy", Context.MODE_PRIVATE).getString("Cookie", "");
            handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    if (response != null) {
                        if (response.getInteger("code").equals(200)) {
                            switch (msg.what) {
                                case -1:
                                    Toast.makeText(requireContext(), getString(R.string.no_wifi_warning), Toast.LENGTH_LONG).show();
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
                                            filter.put(new String[]{"campus", "day", "section", "language", "special"}[a], itemCodes.get(i));
                                            filterName.put(new String[]{"campus", "day", "section", "language", "special"}[a], items.get(i));
                                        });
                                    }
                                    break;
                            }
                        } else if (response.getInteger("code").equals(50021000)) {
                            Toast.makeText(requireContext(), response.getString("message"), Toast.LENGTH_LONG).show();
                        } else if (response.getInteger("code").equals(53000007)) {
                            Toast.makeText(requireContext(), getString(R.string.login_warning), Toast.LENGTH_LONG).show();
                            launch.launch(new Intent(requireContext(), LoginActivity.class).putExtra("url", TargetUrl.JWXT));
                        }
                    }
                    super.handleMessage(msg);
                }
            };
            getData(0);
        }
        super.onCreateView(inflater,container,savedInstanceState);
        return binding.getRoot();
    }

    void reset() {
        filterName = vm.getFilter();
        binding.campuses.setText(filterName.get("campus"));
        binding.course.setText(filterName.get("course"));
        binding.days.setText(filterName.get("day"));
        binding.sections.setText(filterName.get("section"));
        binding.languages.setText(filterName.get("language"));
        binding.special.setText(filterName.get("special"));
        binding.school.setText(filterName.get("school"));
        binding.teacher.setText(filterName.get("teacher"));
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        binding.reset.setOnClickListener(view1 -> {
            vm.setFilter(new HashMap<>());
            reset();
        });
        binding.submit.setOnClickListener(v-> {
            super.onViewCreated(view, savedInstanceState);
//            Bundle arg = new Bundle();
//            arg.putSerializable("filter",getMap());
//            arg.putBoolean("isFilter",true);
//            System.out.println(getParentFragmentManager().findFragmentById(R.id.nav_host_fragment_content_course_selection));
//            System.out.println(R.id.selection_fragment);
//            Navigation.findNavController(view).getGraph().
//            ((CourseSelectionFragment) Objects.requireNonNull(Objects.requireNonNull(getParentFragmentManager().findFragmentById(R.id.nav_host_fragment_content_course_selection)).getChildFragmentManager().findFragmentById(R.id.selection_fragment))).setFilter(filter);

            //           Navigation.findNavController(view).navigateUp();
            vm.setReturnData(parseFilter(getMap()));
            vm.setFilter(filterName);
            // System.out.println(parseFilter(getMap()));
            Navigation.findNavController(view).navigateUp();
//            Navigation.findNavController(view).navigate(,arg, new NavOptions.Builder()
//                    .setEnterAnim(android.R.animator.fade_in)
//                    .setExitAnim(android.R.animator.fade_out)
//                    .build(), new FragmentNavigator.Extras(Map.of(v, "miniapp")));
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //binding = null;
    }

    void getData(int i){
        http.newCall(new Request.Builder().url(new String[]{"https://jwxt.sysu.edu.cn/jwxt/base-info/campus/findCampusNamesBox",
                        "https://jwxt.sysu.edu.cn/jwxt/base-info/codedata/findcodedataNames?datableNumber=233",
                        "https://jwxt.sysu.edu.cn/jwxt/base-info/AcadyeartermSet/minorName?schoolYear=2025-1",
                        "https://jwxt.sysu.edu.cn/jwxt/base-info/codedata/findcodedataNames?datableNumber=204",
                        "https://jwxt.sysu.edu.cn/jwxt/base-info/codedata/findcodedataNames?datableNumber=387"}[i])
                .header("Cookie",cookie)
                .header("Referer","https://jwxt.sysu.edu.cn/jwxt/mk/courseSelection/?code=jwxsd_xk&resourceName=%E9%80%89%E8%AF%BE")
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = new Message();
                msg.what=-1;
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what=1;
                msg.arg1=i;
                msg.obj=response.body().string();
                handler.sendMessage(msg);
            }
        });
    }
    public HashMap<String,String> getMap(){
        filter.put("course", Objects.requireNonNull(binding.course.getText()).toString());
        filter.put("teacher", Objects.requireNonNull(binding.teacher.getText()).toString());
        filter.put("school", Objects.requireNonNull(binding.school.getText()).toString());
        filterName.put("course", Objects.requireNonNull(binding.course.getText()).toString());
        filterName.put("teacher", Objects.requireNonNull(binding.teacher.getText()).toString());
        filterName.put("school", Objects.requireNonNull(binding.school.getText()).toString());
        return filter;
    }

    String parseFilter(HashMap<String,String> filter){
        StringBuilder str = new StringBuilder();
        String[] keys = new String[]{"course","campus", "day", "section", "school","teacher", "language", "special"};
        String[] key = new String[]{"courseName","studyCampusId","week","classTimes","courseUnitNum","teachingTeacherNum","teachingLanguageCode","specialClassCode"};
        for(int i=0;i<keys.length;i++){
            String v = filter.getOrDefault(keys[i], "");
            if (v != null && !v.isEmpty()) {
                str.append(String.format(",\"%s\":\"%s\"", key[i], v));
            }
        }
        return str.toString();
    }
}