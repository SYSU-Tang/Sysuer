package com.sysu.edu.academic;

import static com.sysu.edu.api.CommonUtil.trim;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.ItemEvaluationBinding;
import com.sysu.edu.databinding.RecyclerViewScrollBinding;
import com.sysu.edu.model.PjxtModel;
import com.sysu.edu.view.AdapterListener;
import com.sysu.edu.view.RecyclerAdapter;

import java.util.Objects;

public class EvaluationCategoryFragment extends Fragment {
    
    StaggeredGridLayoutManager staggeredGridLayoutManager;
    Params params;
    PjxtModel model;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        RecyclerViewScrollBinding binding = RecyclerViewScrollBinding.inflate(inflater, container, false);
        params = new Params(this);
        model = new PjxtModel(requireContext());
        staggeredGridLayoutManager = new StaggeredGridLayoutManager(params.getColumn(), 1);
        binding.getRoot().setLayoutManager(staggeredGridLayoutManager);
        CategoryAdapter categoryAdapter = new CategoryAdapter();
        String[] keys = new String[]{"rwmc", "rwkssj", "rwjssj", "pjsl", "ypsl"};
        String[] values = new String[]{"%s", "起始时间：%s", "结束时间：%s", "总评数：%s", "已评数：%s"};
        String[] arguments = new String[]{"rwid", "firstwjid", "pjrdm"};
        categoryAdapter.setListener(new AdapterListener() {
            @Override
            public void onBind(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, RecyclerView.ViewHolder holder, int position) {
                ItemEvaluationBinding bind = ItemEvaluationBinding.bind(holder.itemView);
                Bundle args = new Bundle();
                JSONObject item = categoryAdapter.get(position);
                for (String param : arguments) args.putString(param, item.getString(param));
                View.OnClickListener listener = _ -> Navigation.findNavController(binding.getRoot()).navigate(R.id.from_category_to_course, args);
                bind.open.setOnClickListener(listener);
                holder.itemView.setOnClickListener(listener);
                bind.title.setCompoundDrawablesWithIntrinsicBounds(Integer.parseInt(item.getString("pjsl")) <= Integer.parseInt(item.getString("ypsl")) ? R.drawable.submit : R.drawable.window, 0, 0, 0);
                bind.title.setCompoundDrawablePadding(36);
                bind.title.setText(String.format(values[0], trim(item.getString(keys[0]))));
                StringBuilder val = new StringBuilder();
                for (int i = 1; i < keys.length; i++)
                    val.append(String.format(values[i], trim(item.getString(keys[i])))).append("\n");
                bind.startTime.setText(val.toString().trim());
            }
            
            @Override
            public void onCreate(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, ViewBinding binding) {
            
            }
        });
        binding.getRoot().setAdapter(categoryAdapter);
        model.getMessage().observe(requireActivity(), message -> {
            JSONObject data = message.second;
            if (Objects.equals(data.get("code"), "200")) if (message.first == 1)
                data.getJSONObject("result").getJSONArray("list").forEach(e -> categoryAdapter.add((JSONObject) e));
        });
        getEvaluation();
        return binding.getRoot();
    }
    
    
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        staggeredGridLayoutManager.setSpanCount(params.getColumn());
    }
    
    public void getEvaluation() {
        model.addAndNext("personnelEvaluation/listObtainPersonnelEvaluationTasks?pageNum=1&pageSize=10", 1);
    }
    
    public static class CategoryAdapter extends RecyclerAdapter<JSONObject> {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(ItemEvaluationBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot()) {
            };
        }
    }
}