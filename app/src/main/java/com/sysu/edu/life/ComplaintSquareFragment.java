package com.sysu.edu.life;

import static com.sysu.edu.api.CommonUtil.toStringOrDefault;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.ItemComplaintSquareBinding;
import com.sysu.edu.databinding.RecyclerViewScrollBinding;
import com.sysu.edu.view.RecyclerAdapter;

import io.noties.markwon.Markwon;
import io.noties.markwon.html.HtmlPlugin;

public class ComplaintSquareFragment extends Fragment {

    HttpManager http;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        RecyclerViewScrollBinding binding = RecyclerViewScrollBinding.inflate(getLayoutInflater());
        SquareAdapter adapter = new SquareAdapter();
        binding.getRoot().setAdapter(adapter);
        Params params = new Params(this);
        binding.getRoot().setLayoutManager(new StaggeredGridLayoutManager(params.getColumn(), StaggeredGridLayoutManager.VERTICAL));
        http = new HttpManager(new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == -1) {
                    params.toast(R.string.no_net_connected);
                } else if (msg.getData().getBoolean("isJSON")) {
                    if (msg.what == 0) {
                        JSONObject response = JSONObject.parse(msg.obj.toString());
                        if (response.getBoolean("ok"))
                            response.getJSONArray("data").forEach(v -> adapter.add((JSONObject) v));
                        else
                            params.toast(response.getString("msg"));
                    }
                } else {
                    params.toast(R.string.educational_wifi_warning);
                }
            }
        });
        http.setParams(params);
        getSquare();
        return binding.getRoot();
    }

    void getSquare() {
        http.postRequest("https://xinfang.sysu.edu.cn/jsp_api/hsgc", "", 0);
    }

    static class SquareAdapter extends RecyclerAdapter<JSONObject> {

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_complaint_square, parent, false)) {
            };
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            JSONObject item = get(position);
            ItemComplaintSquareBinding binding = ItemComplaintSquareBinding.bind(holder.itemView);
            Context context = holder.itemView.getContext();
//            Integer status = item.getInteger("status");
            binding.title.setText(item.getString("name"));
            binding.detail.setText(String.format("#%s  #%s", item.getString("createDate"), toStringOrDefault(item.getString("questionType"), "未分类")));
            binding.request.setText(toStringOrDefault(item.getString("description"), "暂无公开答复内容"));
            Markwon.builder(context).usePlugin(HtmlPlugin.create()).build().setMarkdown(binding.response, item.getString("dfnr"));
        }
    }
}