package com.sysu.edu.academic;

import static com.sysu.edu.api.CommonUtil.toStringOrDefault;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
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

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class EvaluationCourseFragment extends Fragment {
    
    int page = 1;
    EvaluationViewModel vm;
    PjxtModel model;
    StaggeredGridLayoutManager sgm;
    Params params;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        page = 1;
        RecyclerViewScrollBinding binding = RecyclerViewScrollBinding.inflate(inflater, container, false);
        params = new Params(this);
        model = new PjxtModel(requireContext());
        vm = new ViewModelProvider(this).get(EvaluationViewModel.class);
        sgm = new StaggeredGridLayoutManager(params.getColumn(), 1);
        binding.getRoot().setLayoutManager(sgm);
        String[] keys = new String[]{"kcmc", "skjsmc", "kcdlmc", "kkyxmc", "bjmc", "kcdm", "xnxqmc", "lsjgzt"};
        String[] values = new String[]{"%s", "教师：%s", "课程类型：%s", "开课院系：%s", "教学班号：%s", "课程代码：%s", "学期：%s", "评价状态：%s"};
        String[] arguments = new String[]{"rwid", "wjid", "sxz", "pjrdm", "bpdm", "kcdm", "rwh", "lsjgzt", "bpmc"};
        CourseEvaluationAdapter adp = new CourseEvaluationAdapter();
        adp.setListener(new AdapterListener() {
            @Override
            public void onBind(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, RecyclerView.ViewHolder holder, int position) {
                ItemEvaluationBinding bind = ItemEvaluationBinding.bind(holder.itemView);
                Bundle args = new Bundle();
                Context context = holder.itemView.getContext();
                JSONObject item = adp.get(position);
                for (String arg : arguments)
                    args.putString(arg, item.getString(arg));
                Drawable drawable = AppCompatResources.getDrawable(context, Objects.equals(item.getString("lsjgzt"), "2") ? R.drawable.submit : R.drawable.window);
                if (drawable != null) drawable.setBounds(0, 0, 72, 72);
                bind.title.setCompoundDrawables(drawable, null, null, null);
                bind.title.setCompoundDrawablePadding(36);
                View.OnClickListener action = _ -> Navigation.findNavController(binding.getRoot()).navigate(R.id.from_course_to_evaluation, args);
                bind.open.setOnClickListener(action);
                holder.itemView.setOnClickListener(action);
                bind.title.setText(String.format(values[0], toStringOrDefault(item.getString(keys[0]))));
                StringBuilder val = new StringBuilder();
                for (int i = 1; i < keys.length; i++)
                    val.append(String.format(values[i], Objects.equals(keys[i], "lsjgzt") ? Map.of("0", "待评价", "2", "已评价", "3", "已保存").getOrDefault(item.getString(keys[i]), "未知") : toStringOrDefault(item.getString(keys[i]), ""))).append("\n");
                bind.startTime.setText(val.toString().trim());
            }
            
            @Override
            public void onCreate(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, ViewBinding binding) {
            }
        });
        binding.getRoot().setAdapter(adp);
        String type = requireArguments().getString("firstwjid");
        String rwid = requireArguments().getString("rwid");
        String pjrdm = requireArguments().getString("pjrdm");
        model.getMessage().observe(requireActivity(), message -> {
            JSONObject response = message.second;
            if (response.get("code").equals("200")) if (message.first == 1) {
                JSONObject result = response.getJSONObject("result");
                result.getJSONArray("list").forEach(e -> adp.add((JSONObject) e));
                if (result.getInteger("total") / 20.0 > page)
                    getEvaluation(type, rwid, pjrdm);
            }
        });
        if (type != null && rwid != null && pjrdm != null)
            getEvaluation(type, rwid, pjrdm);
        return binding.getRoot();
    }
    
    public void getEvaluation(String wjid, String rwid, String pjrdm) {
        model.addAndNext(String.format(Locale.getDefault(), "personnelEvaluation/listEcaluationRalationshipEnriry?pjrdm=%s&wjid=%s&rwid=%s&pageNum=%d&pageSize=20", pjrdm, wjid, rwid, page++), 1);
    }
    
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        sgm.setSpanCount(params.getColumn());
    }
    
    static class CourseEvaluationAdapter extends RecyclerAdapter<JSONObject> {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(ItemEvaluationBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot()) {
            };
        }
    }
}