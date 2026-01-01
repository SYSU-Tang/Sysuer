package com.sysu.edu.academic;

import android.content.Context;
import android.graphics.drawable.Drawable;
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
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.ItemEvaluationBinding;
import com.sysu.edu.databinding.RecyclerViewScrollBinding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class EvaluationCategoryFragment extends Fragment {
    Params params;
    Handler handler;
    RecyclerViewScrollBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        binding = RecyclerViewScrollBinding.inflate(inflater, container, false);
        params = new Params(requireActivity());
        params.setCallback(this, this::getEvaluation);
        StaggeredGridLayoutManager sgm = new StaggeredGridLayoutManager(params.getColumn(), 1);
        binding.getRoot().setLayoutManager(sgm);
        CategoryAdapter adp = new CategoryAdapter(requireContext());
        adp.setKeys(new String[]{"rwmc", "rwkssj", "rwjssj", "pjsl", "ypsl"});
        adp.setValues(new String[]{"%s", "起始时间：%s", "结束时间：%s", "总评数：%s", "已评数：%s"});
        adp.setParams(new String[]{"rwid", "firstwjid", "pjrdm"});
        adp.setNavigation(R.id.from_category_to_course);
        binding.getRoot().setAdapter(adp);
        getEvaluation();
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == 1) {
                    JSONObject data = JSON.parseObject((String) msg.obj);
                    if (data.get("code").equals("200")) {
                        data.getJSONObject("result").getJSONArray("list").forEach(e -> adp.add((JSONObject) e));
                    } else {
                        params.gotoLogin(getView(), "https://pjxt.sysu.edu.cn");
                    }
                } else if (msg.what == -1) {
                    params.toast(R.string.no_wifi_warning);
                    params.gotoLogin(getView(), "https://pjxt.sysu.edu.cn");
                }
            }
        };
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public void getEvaluation() {
        new OkHttpClient.Builder().build().newCall(new Request.Builder().url("https://pjxt.sysu.edu.cn/personnelEvaluation/listObtainPersonnelEvaluationTasks?pageNum=1&pageSize=10")
                .header("Cookie", params.getCookie())
                //.addHeader("Cookie", "JSESSIONID=F547A1B2729098E0B101716397DC48DC;INCO=9b1595d95278e78f17d51a5f35287020;")
                // .post(RequestBody.create("{\"acadYear\":\"2024-2\",\"examWeekId\":\"1864116471884476417\",\"examWeekName\":\"18-19周期末考\",\"examDate\":\"\"}", MediaType.parse("application/json")))
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message message = new Message();
                message.what = -1;
                handler.sendMessage(message);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what = 1;
                msg.obj = response.body().string();
                handler.sendMessage(msg);
            }
        });
    }

    public static class CategoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        Context context;
        ArrayList<JSONObject> data = new ArrayList<>();
        String[] keys;
        String[] values;
        String[] params;
        int nav;

        public CategoryAdapter(Context context) {
            super();
            this.context = context;
        }

        public void add(JSONObject e) {
            data.add(e);
            notifyItemInserted(data.size() - 1);
        }

        public void clear() {
            int tmp = getItemCount();
            data.clear();
            notifyItemMoved(0, tmp);
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(ItemEvaluationBinding.inflate(LayoutInflater.from(context), parent, false).getRoot()) {
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
            for (String param : params) {
                args.putString(param, data.get(position).getString(param));
            }
            binding.open.setOnClickListener(v -> ((NavHostFragment) Objects.requireNonNull(((AppCompatActivity) context).getSupportFragmentManager().findFragmentById(R.id.fragment))).getNavController().navigate(nav, args));
            holder.itemView.setOnClickListener(v -> {
            });
            Drawable drawable = AppCompatResources.getDrawable(context, Integer.parseInt(data.get(position).getString("pjsl"))<=Integer.parseInt(data.get(position).getString("ypsl"))  ? R.drawable.submit : R.drawable.window);
            if (drawable != null) {
                drawable.setBounds(0, 0, 72, 72);
            }
            binding.title.setCompoundDrawables(drawable, null, null, null);
            binding.title.setCompoundDrawablePadding(36);
            binding.title.setText(String.format(values[0], data.get(position).getString(keys[0]) == null ? "" : data.get(position).getString(keys[0])));
            StringBuilder val = new StringBuilder();
            for (int i = 1; i < keys.length; i++) {
                val.append(String.format(values[i], data.get(position).getString(keys[i]) == null ? "" : data.get(position).getString(keys[i])));
                val.append("\n");
            }
            binding.startTime.setText(val.toString().trim());

        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }
}