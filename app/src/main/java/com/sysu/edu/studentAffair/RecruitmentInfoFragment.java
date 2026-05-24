package com.sysu.edu.studentAffair;

import static com.sysu.edu.api.CommonUtil.extractValue;
import static com.sysu.edu.api.CommonUtil.isEmpty;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.model.XgxtModel;
import com.sysu.edu.view.StaggeredFragment;

import java.util.List;

public class RecruitmentInfoFragment extends StaggeredFragment {
    
    StudentPartTimeViewModel viewModel;
    Integer total = -1;
    Integer page = 1;
    XgxtModel model;
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        model.dispose();
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        model = new XgxtModel(requireContext());
        viewModel = new ViewModelProvider(requireActivity()).get(StudentPartTimeViewModel.class);
        viewModel.jobNameDialog.setValueChangeListener(v -> {
            viewModel.jobName.setValue(v);
            reset();
            getRecruitment();
        });
        viewModel.unitDialog.setValueChangeListener(v -> {
            viewModel.unitName.setValue(v);
            reset();
            getRecruitment();
        });
        setScrollBottom(() -> {
            if ((page - 1) * 10 < total) getRecruitment();
        });
        model.getMessage().observe(requireActivity(), message -> {
            JSONObject data = message.getSecond();
            if (data.containsKey("code") && data.getInteger("code") == 200) {
                switch (message.getFirst()) {
                    case 0 -> {
                        total = data.getJSONObject("data").getInteger("total");
                        data.getJSONObject("data").getJSONArray("list").forEach(i -> add(((JSONObject) i).getString("qgzxgwmc"), List.of("岗位名称", "岗位类型", "所在校区", "岗位地址", "开始时间", "结束时间", "状态", "设岗单位"),
                                extractValue((JSONObject) i, new String[]{"qgzxgwmc", "qgzxgwlxmc", "qgzxszxymc", "qgzxdwdz", "qgzxgwzpkssj", "qgzxgwzpjssj", "state", "sgdwmc"})));
                    }
                    case 1, 2, 3 -> {
                        Menu menu = List.of(viewModel.yearPop, viewModel.campusPop, viewModel.typePop).get(message.getFirst() - 1).getMenu();
                        if (menu.hasVisibleItems()) break;
                        MutableLiveData<String> name = List.of(viewModel.yearName, viewModel.campusName, viewModel.jobTypeName).get(message.getFirst() - 1);
                        MutableLiveData<String> liveData = List.of(viewModel.year, viewModel.campus, viewModel.jobType).get(message.getFirst() - 1);
                        menu.add(R.string.all).setOnMenuItemClickListener(_ -> {
                            liveData.setValue("");
                            name.setValue("");
                            reset();
                            getRecruitment();
                            return true;
                        });
                        data.getJSONArray("data").forEach(i -> menu.add(((JSONObject) i).getString("label")).setOnMenuItemClickListener(_ -> {
                            liveData.setValue(((JSONObject) i).getString("value"));
                            name.setValue(((JSONObject) i).getString("label"));
                            reset();
                            getRecruitment();
                            return true;
                        }));
                    }
                }
                model.nextAll();
            }
        });
        getYear();
        getCampus();
        getJobType();
        getRecruitment();
        return view;
    }
    
    private void reset() {
        page = 1;
        total = -1;
        clear();
    }
    
    void getRecruitment() {
        String url = "qgzx/api/sm-qgzx/gwsq?pageSize=10&pageNum=" + page++;
        if (!isEmpty(viewModel.year.getValue()))
            url += "&qgzxnd=" + viewModel.year.getValue();
        if (!isEmpty(viewModel.jobType.getValue()))
            url += "&gwlxids=" + viewModel.jobType.getValue();
        if (!isEmpty(viewModel.campus.getValue()))
            url += "&xqids=" + viewModel.campus.getValue();
        if (!isEmpty(viewModel.jobName.getValue()))
            url += "&qgzxgwmc=" + viewModel.jobName.getValue();
        if (!isEmpty(viewModel.unitDialog.getValue()))
            url += "&sgdwmc=" + viewModel.unitDialog.getValue();
        model.addAndNext(url, 0);
    }
    
    void getYear() {
        model.add("qgzx/api/sm-qgzx/gwsq/ndlist/get", 1);
    }
    
    void getCampus() {
        model.add("qgzx/api/sm-qgzx/gwsq/xylist/get", 2);
    }
    
    void getJobType() {
        model.add("qgzx/api/sm-qgzx/gwsq/gwlxlist/get", 3);
    }
}
