package com.sysu.edu.academic;

import static com.sysu.edu.api.CommonUtil.extractValue;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.model.JwxtModel;
import com.sysu.edu.view.StaggeredFragment;

import java.util.List;
import java.util.Locale;

public class MajorInfoFragment extends StaggeredFragment {
    
    int page = 0;
    int total = -1;
    String code;
    JwxtModel model;
    
    public static MajorInfoFragment newInstance(Bundle args) {
        MajorInfoFragment fragment = new MajorInfoFragment();
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        code = requireArguments().getString("code");
        model = new JwxtModel(requireContext());
        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView v, int dx, int dy) {
                if (!v.canScrollVertically(1) && total > page * 10) getList();
            }
        });
        model.getMessage().observe(requireActivity(), message -> {
            JSONObject response = message.getSecond();
            if (response != null && response.getInteger("code").equals(200)) {
                if (response.get("data") != null) {
                    JSONObject data = response.getJSONObject("data");
                    if (message.getFirst() == 0) {
                        if (total == -1) total = data.getInteger("total");
                        data.getJSONArray("rows").forEach(a -> add(((JSONObject) a).getString("name"), List.of("专业代码", "专业名称", "学制", "修业年限", "学科门类", "学位授予门类"),
                                extractValue((JSONObject) a, new String[]{"code", "name", "educationalSystem", "maxStudyYear", "disciplineCateName", "degreeGrantName"})));
                    }
                }
            }
        });
        getList();
        return view;
    }
    
    void getList() {
        model.addAndNext("jwxt/base-info/profession-direction/list", String.format(Locale.getDefault(), "{\"pageNo\":%d,\"pageSize\":10,\"total\":true,\"param\":{\"majorProfessionDircetion\":\"0\",\"disciplineCateCode\":\"%s\"}}", ++page, code), 0);
    }
}