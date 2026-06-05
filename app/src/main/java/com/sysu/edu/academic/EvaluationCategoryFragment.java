package com.sysu.edu.academic;

import static com.sysu.edu.api.CommonUtil.trim;

import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.ItemEvaluationBinding;
import com.sysu.edu.databinding.RecyclerViewScrollBinding;
import com.sysu.edu.view.RecyclerAdapter;

import java.util.Objects;

public class EvaluationCategoryFragment extends Fragment {
    HttpManager http;
    StaggeredGridLayoutManager staggeredGridLayoutManager;
    Params params;
    EvaluationViewModel vm;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        RecyclerViewScrollBinding binding = RecyclerViewScrollBinding.inflate(inflater, container, false);
        params = new Params(this);
        params.setCallback(this::getEvaluation);
        staggeredGridLayoutManager = new StaggeredGridLayoutManager(params.getColumn(), 1);
        binding.getRoot().setLayoutManager(staggeredGridLayoutManager);
        CategoryAdapter categoryAdapter = new CategoryAdapter();
        categoryAdapter.setKeys(new String[]{"rwmc", "rwkssj", "rwjssj", "pjsl", "ypsl"});
        categoryAdapter.setValues(new String[]{"%s", "起始时间：%s", "结束时间：%s", "总评数：%s", "已评数：%s"});
        categoryAdapter.setParams(new String[]{"rwid", "firstwjid", "pjrdm"});
        categoryAdapter.setNavigation(R.id.from_category_to_course);
        binding.getRoot().setAdapter(categoryAdapter);
        vm = new ViewModelProvider(this).get(EvaluationViewModel.class);
        http = new HttpManager(new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.getData().getInt("code") == 200) {
                    if (msg.what == -1) params.toast(R.string.no_net_connected);
                    else if (msg.getData().getBoolean("isJSON")) {
                        if (msg.what == 1) {
                            JSONObject data = JSON.parseObject((String) msg.obj);
                            if (data != null && Objects.equals(data.get("code"), "200"))
                                data.getJSONObject("result").getJSONArray("list").forEach(e -> categoryAdapter.add((JSONObject) e));
                            else
                                params.gotoLogin(vm.authorizationManager.isAccessible() ? TargetUrl.PJXT : TargetUrl.PJXT_WEBVPN);
                        }
                    } else {
                        vm.authorizationManager.isAccessible((String) msg.obj);
                        getEvaluation();
                    }
                } else
                    params.gotoLogin(vm.authorizationManager.isAccessible() ? TargetUrl.PJXT : TargetUrl.PJXT_WEBVPN);
            }
        });
        http.setParams(params);
        getEvaluation();
        return binding.getRoot();
    }
    
    
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        staggeredGridLayoutManager.setSpanCount(params.getColumn());
    }
    
    public void getEvaluation() {
        http.getRequest(vm.authorizationManager.getBaseUrl() + "personnelEvaluation/listObtainPersonnelEvaluationTasks?pageNum=1&pageSize=10", 1);
    }
    
    public static class CategoryAdapter extends RecyclerAdapter<JSONObject> {
        
        String[] keys;
        String[] values;
        String[] params;
        int nav;
        
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(ItemEvaluationBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot()) {
            };
        }
        
        public void setKeys(String[] keys) {
            this.keys = keys;
        }
        
        public void setValues(String[] values) {
            this.values = values;
        }
        
        public void setParams(String[] params) {
            this.params = params;
        }
        
        public void setNavigation(int nav) {
            this.nav = nav;
        }
        
        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ItemEvaluationBinding binding = ItemEvaluationBinding.bind(holder.itemView);
            Bundle args = new Bundle();
            JSONObject item = get(position);
            for (String param : params) args.putString(param, item.getString(param));
            binding.open.setOnClickListener(_ -> ((NavHostFragment) Objects.requireNonNull(((AppCompatActivity) binding.getRoot().getContext()).getSupportFragmentManager().findFragmentById(R.id.fragment))).getNavController().navigate(nav, args));
            holder.itemView.setOnClickListener(_ -> {
            });
            binding.title.setCompoundDrawablesWithIntrinsicBounds(Integer.parseInt(item.getString("pjsl")) <= Integer.parseInt(item.getString("ypsl")) ? R.drawable.submit : R.drawable.window, 0, 0, 0);
            binding.title.setCompoundDrawablePadding(36);
            binding.title.setText(String.format(values[0], trim(item.getString(keys[0]))));
            StringBuilder val = new StringBuilder();
            for (int i = 1; i < keys.length; i++)
                val.append(String.format(values[i], trim(item.getString(keys[i])))).append("\n");
            binding.startTime.setText(val.toString().trim());
            super.onBindViewHolder(holder, position);
        }
    }
}