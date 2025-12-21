package com.sysu.edu.academic;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.snackbar.Snackbar;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.ItemEvaluationBinding;
import com.sysu.edu.databinding.RecyclerViewScrollBinding;
import com.sysu.edu.extra.LoginActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class EvaluationCourseFragment extends Fragment {
    Params params;
    Handler handler;
    int page = 1;
    ActivityResultLauncher<Intent> launch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        RecyclerViewScrollBinding binding = RecyclerViewScrollBinding.inflate(inflater, container, false);
        params = new Params(requireActivity());
        StaggeredGridLayoutManager sgm = new StaggeredGridLayoutManager(params.getColumn(), 1);
        binding.getRoot().setLayoutManager(sgm);
        CourseEvaluationAdapter adp = new CourseEvaluationAdapter(requireContext());
        binding.getRoot().setAdapter(adp);
        adp.setKeys(new String[]{"kcmc", "skjsmc", "kcdlmc", "kkyxmc", "bjmc", "kcdm", "xnxqmc", "lsjgzt"});
        adp.setValues(new String[]{"%s", "教师：%s", "课程类型：%s", "开课院系：%s", "教学班号：%s", "课程代码：%s", "学期：%s", "评价状态：%s"});
        adp.setParams(new String[]{"rwid", "wjid", "sxz", "pjrdm", "bpdm", "kcdm", "rwh", "lsjgzt", "bpmc"});
        adp.setNavigation(R.id.from_course_to_evaluation);
        String type = requireArguments().getString("firstwjid");
        String rwid = requireArguments().getString("rwid");
        String account = requireArguments().getString("pjrdm");
        if (type != null && rwid != null && account != null) {
            getEvaluation(type, rwid, account, page);
        }
        launch = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
            if (o.getResultCode() == Activity.RESULT_OK) {
                page = 1;
                getEvaluation(type, rwid, account, page);
            }
        });
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == 1) {
                    JSONObject data = JSON.parseObject((String) msg.obj);
                    if (data.get("code").equals("200")) {
                        data.getJSONObject("result").getJSONArray("list").forEach(e -> adp.add((JSONObject) e));
                        if (data.getJSONObject("result").getInteger("total") / 20.0 > page) {
                            getEvaluation(type, rwid, account, ++page);
                        }
                    } else {
                        launch.launch(new Intent(requireContext(), LoginActivity.class));
                    }
                } else if (msg.what == -1) {
                    params.toast(R.string.no_wifi_warning);
                    Snackbar.make(binding.getRoot(), "去登录", Snackbar.LENGTH_LONG).setAction("登录", v -> launch.launch(new Intent(requireContext(), LoginActivity.class).putExtra("url", "https://pjxt.sysu.edu.cn"))).show();
                }
            }
        };
        return binding.getRoot();
    }

    public void getEvaluation(String wjid, String rwid, String account, int page) {
        new OkHttpClient.Builder().build().newCall(new Request.Builder().url(String.format("https://pjxt.sysu.edu.cn/personnelEvaluation/listEcaluationRalationshipEnriry?pjrdm=%s&wjid=%s&rwid=%s&pageNum=%d&pageSize=20", account, wjid, rwid, page))
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
}
class CourseEvaluationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    Context context;
    ArrayList<JSONObject> data = new ArrayList<>();
    String[] keys;
    String[] values;
    String[] params;
    int nav;

    public CourseEvaluationAdapter(Context context) {
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
        Drawable drawable = context.getDrawable(Objects.equals(data.get(position).getString("lsjgzt"), "2") ? R.drawable.submit : R.drawable.window);
        if (drawable != null) {
            drawable.setBounds(0, 0, 72, 72);
        }
        binding.title.setCompoundDrawables(drawable, null, null, null);
        binding.title.setCompoundDrawablePadding(36);
        binding.open.setOnClickListener(v -> ((NavHostFragment) Objects.requireNonNull(((AppCompatActivity) context).getSupportFragmentManager().findFragmentById(R.id.fragment))).getNavController().navigate(nav, args));
        holder.itemView.setOnClickListener(v -> {
        });
        binding.title.setText(String.format(values[0], data.get(position).getString(keys[0]) == null ? "" : data.get(position).getString(keys[0])));
        StringBuilder val = new StringBuilder();
        for (int i = 1; i < keys.length; i++) {
            val.append(String.format(values[i], Objects.equals(keys[i], "lsjgzt") ? Map.of("0", "待评价", "2", "已评价", "3", "已保存").getOrDefault(data.get(position).getString(keys[i]), "未知") :data.get(position).getString(keys[i]) == null ? "" : data.get(position).getString(keys[i])));
            val.append("\n");
        }
        binding.startTime.setText(val.toString().trim());

    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}