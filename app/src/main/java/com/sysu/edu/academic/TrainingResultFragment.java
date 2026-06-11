package com.sysu.edu.academic;

import static com.sysu.edu.api.CommonUtil.extractValue;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.databinding.FragmentTrainingResultBinding;
import com.sysu.edu.model.JwxtModel;
import com.sysu.edu.view.StaggeredFragment;

import java.util.List;

public class TrainingResultFragment extends Fragment {
    int page = 0;
    int total = -1;
    JwxtModel model;
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        model.dispose();
    }
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentTrainingResultBinding binding = FragmentTrainingResultBinding.inflate(inflater, container, false);
        model = new JwxtModel(requireActivity());
        StaggeredFragment staggeredFragment = new StaggeredFragment();
        getParentFragmentManager().beginTransaction().add(R.id.result, staggeredFragment).commit();
        staggeredFragment.setScrollBottom(() -> {
            if (total > page * 10)
                getSelectedCourses();
        });
        binding.export.setOnClickListener(v -> staggeredFragment.export(v, getString(R.string.result)));
        model.getMessage().observe(requireActivity(), message -> {
            JSONObject response = message.second;
            if (response.getIntValue("code") == 200) {
                if (message.first == 1) {
                    JSONObject data = response.getJSONObject("data");
                    total = data.getInteger("total");
                    data.getJSONArray("rows").forEach(o -> staggeredFragment.add(((JSONObject) o).getString("name"), R.drawable.book, List.of("专业", "年级", "学院", "培养类别", "修业年限", "学科门类", "学位", "专业代码", "专业ID"),
                            extractValue((JSONObject) o, new String[]{"professionName", "grade", "manageUnitName", "trainTypeName", "educationalSystem", "disciplineCateName", "degreeGrantName", "professionCode", "professionId"})));
                }
            }
        });
        getSelectedCourses();
        return binding.getRoot();
    }
    
    
    void getSelectedCourses(String unit, String grade, String profession, String trainType) {
        model.addAndNext("jwxt/training-programe/training-programe/undergradute/profession-info", String.format("{\"pageNo\":%s,\"pageSize\":10,\"total\":true,\"param\":{\"manageUnitNum\":\"%s\",\"grade\":\"%s\",\"professionCode\":\"%s\",\"trainTypeCode\":\"%s\"}}", ++page, unit, grade, profession, trainType), 1);
    }
    
    void getSelectedCourses() {
        getSelectedCourses(requireArguments().getString("unit"), requireArguments().getString("grade"), requireArguments().getString("profession"), requireArguments().getString("type"));
    }
}