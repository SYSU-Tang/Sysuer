package com.sysu.edu.studentAffair;

import static com.sysu.edu.api.CommonUtil.extractValue;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.model.XgxtModel;
import com.sysu.edu.view.StaggeredFragment;

import java.util.List;

public class CVFragment extends StaggeredFragment {
    View view;
    XgxtModel model;
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        model.dispose();
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (view == null) {
            view = super.onCreateView(inflater, container, savedInstanceState);
            model = new XgxtModel(requireContext());
            model.getMessage().observe(requireActivity(), message -> {
                JSONObject data = message.second;
                if (data.containsKey("code") && data.getInteger("code") == 200) {
                    data = data.getJSONObject("data");
                    add(getString(R.string.cv), List.of("学号", "姓名", "培养单位", "专业", "培养层次", "电话号码", "邮箱", "最后修改时间", "家庭人均月收入(元)", "在校每月平均消费(元)", "爱好特长", "勤工助学经历", /*"", */"工作时间", "性别", "住宿地址"),
                            extractValue(data, new String[]{"xh", "xm", "pydw", "zymc", "pycc", "dhhm", "email", "zhxgsj", "jtrjysr", "zxmypjxf", "ahtc", "qgzxjls",/*"kqgzxsjs",*/"gzsjs", "xb", "ssdz"}));
                    data.getJSONArray("hjqks").forEach(i -> add(getString(R.string.award), List.of("颁奖单位", "颁奖日期", "奖项"),
                            extractValue((JSONObject) i, new String[]{"bjdw", "bjrq", "jxmc"})));
                    data.getJSONArray("rzjls").forEach(i -> add(getString(R.string.experience), List.of("工作单位", "工作开始年月", "工作结束年月", "工作职务", "证明人", "证明人单位"),
                            extractValue((JSONObject) i, new String[]{"gzdw", "gzksny", "gzjsny", "gzzw", "zmr", "zmrdwhzw"})));
                }
            });
            getCV();
        }
        return view;
    }
    
    void getCV() {
        model.addAndNext("qgzx/api/sm-qgzx/xsjl/get", 0);
    }
}
