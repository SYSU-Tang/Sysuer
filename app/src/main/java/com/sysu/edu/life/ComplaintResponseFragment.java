package com.sysu.edu.life;

import static com.sysu.edu.api.CommonUtil.toStringOrDefault;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.CommonUtil;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.FragmentComplaintResponseBinding;

public class ComplaintResponseFragment extends Fragment {
    
    HttpManager http;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentComplaintResponseBinding binding = FragmentComplaintResponseBinding.inflate(inflater, container, false);
        ComplaintSquareFragment.SquareAdapter adapter = new ComplaintSquareFragment.SquareAdapter();
        binding.recyclerView.setAdapter(adapter);
        Params params = new Params(this);
        binding.phone.setEndIconOnClickListener(_ -> {
            String phone = null;
            if (binding.phone.getEditText() != null)
                phone = CommonUtil.toStringOrDefault(binding.phone.getEditText().getText());
            if (ComplaintModel.isInvalidPhone(phone))
                binding.phone.setError(getString(R.string.invalid_phone));
            else
                getResponse(phone);
        });
        if (binding.phone.getEditText() != null) {
            binding.phone.getEditText().addTextChangedListener(new TextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                
                }
                
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                
                }
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (ComplaintModel.isInvalidPhone(toStringOrDefault(s)))
                        binding.phone.setError(getString(R.string.invalid_phone));
                    else
                        binding.phone.setError(null);
                }
            });
        }
        binding.recyclerView.setLayoutManager(new StaggeredGridLayoutManager(params.getColumn(), StaggeredGridLayoutManager.VERTICAL));
        http = new HttpManager(new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                
                if (msg.what == -1) {
                    params.toast(R.string.no_net_connected);
                } else if (msg.getData().getBoolean("isJSON")) {
                    switch (msg.what) {
                        case 0 -> {
                            JSONObject response = JSONObject.parse(msg.obj.toString());
                            String code;
                            if (response.getBoolean("ok"))
                                code = response.getString("data").substring(0, 4);
                            else
                                params.toast(response.getString("msg"));
                        }
                        case 1 -> {
                            JSONObject response = JSONObject.parse(msg.obj.toString());
                            if (response.getBoolean("ok"))
                                response.getJSONArray("data").forEach(v -> adapter.add((JSONObject) v));
                            else
                                params.toast(response.getString("msg"));
                        }
                    }
                } else {
                    params.toast(R.string.educational_wifi_warning);
                }
            }
        });
        http.setParams(params);
        return binding.getRoot();
    }

//    void getCode(String phone) {
//        http.postRequest("https://xinfang.sysu.edu.cn/jsp_api/code_send", "{\"m\":\"" + phone + "\",\"t\":\"jsjb\"}", 0);
//    }
    
    void getResponse(String phone) {
        http.postRequest("https://xinfang.sysu.edu.cn/jsp_api/jsjb_list", "{\"mobile\":\"" + phone + "\"}", 1);
    }
}