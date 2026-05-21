package com.sysu.edu.life;

import static com.sysu.edu.api.CommonUtil.toStringOrDefault;
import static com.sysu.edu.api.CommonUtil.trim;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.sysu.edu.R;
import com.sysu.edu.api.AuthorizationJar;
import com.sysu.edu.api.AuthorizationManager;
import com.sysu.edu.api.CommonUtil;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.browser.BrowserActivity;
import com.sysu.edu.databinding.ItemNewsBinding;
import com.sysu.edu.databinding.RecyclerViewScrollBinding;
import com.sysu.edu.view.RecyclerAdapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewsFragment extends Fragment {
    
    final int position;
    final AuthorizationManager authorizationManager = new AuthorizationManager("https://iportal.sysu.edu.cn/", "https://iportal-443.webvpn.sysu.edu.cn/");
    final Runnable run;
    HttpManager http;
    RecyclerViewScrollBinding binding;
    int page = 1;
    
    public NewsFragment(int pos) {
        position = pos;
        run = List.of((Runnable) this::getNews, this::getSubscription, this::getNotice, this::getDailyNews).get(position);
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            binding = RecyclerViewScrollBinding.inflate(inflater);
            Params params = new Params(this);
            params.setCallback(run);
            binding.recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), params.getColumn()));
            binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    if (!recyclerView.canScrollVertically(1) && position != 0 && dy > 0) run.run();
                }
            });
            NewsAdapter newsAdapter = new NewsAdapter();
            newsAdapter.setParams(params);
            binding.recyclerView.setAdapter(newsAdapter);
            http = new HttpManager(new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    String json = (String) msg.obj;
                    if (json == null || msg.what == -1) params.toast(R.string.no_net_connected);
                    else if (!msg.getData().getBoolean("isJSON")) {
                        if (!authorizationManager.isAuthorized(json))
                            params.gotoLogin(authorizationManager.isAccessible() ? TargetUrl.NEWS : TargetUrl.NEWS_WEBVPN);
                        else if (!authorizationManager.isAccessible(json)) run.run();
                    } else {
                        JSONObject response = JSONObject.parseObject(json);
                        Integer code = response.getInteger("code");
                        if (code == 10000) {
                            (msg.what == 3 ? response.getJSONArray("data") : response.getJSONObject("data").getJSONArray("records")).forEach(e -> {
                                JSONObject item = (JSONObject) e;
                                JSONArray cover = item.getJSONArray("coversPicList");
                                String image = "";
                                if (cover != null && !cover.isEmpty() && cover.getJSONObject(0) != null && cover.getJSONObject(0).getString("outLink") != null)
                                    image = cover.getJSONObject(0).getString("outLink");
                                newsAdapter.add(new HashMap<>(Map.of("title", item.getString("title"), "image", image, "url", item.getString("url"), "time", item.getString("createTime"), "source", item.getJSONObject("source").getString("seedName"))));
                            });
                        } else if (code == 10003) {
                            params.toast(code + " " + response.getString("message"));
                            params.gotoLogin(authorizationManager.isAccessible() ? TargetUrl.NEWS : TargetUrl.NEWS_WEBVPN);
                        } else if (code == 496 || code == 497) {
                            params.toast(response.getString("message"));
                            params.gotoLogin(authorizationManager.isAccessible() ? TargetUrl.NEWS : TargetUrl.NEWS_WEBVPN);
                        }
                    } //今日中大
                }
            });
            http.setAuthorizationRequired(true);
            http.setAuthorizationJar(new AuthorizationJar(requireContext()));
            http.setParams(params);
        }
        run.run();
        return binding.getRoot();
    }
    
    void getNews() {
        http.postRequest(authorizationManager.getBaseUrl() + "ai_service/content-portal/recommend/query-recommend", "", 3);
    }
    
    void getSubscription() {
        baseRequest("3ytr4e6c", 2);
    }
    
    void getNotice() {
        baseRequest("3ytunvv6", 4);
    }
    
    void getDailyNews() {
        baseRequest("4cef8rqw", 5);
    }
    
    void baseRequest(String code, int what) {
        http.postRequest(authorizationManager.getBaseUrl() + "ai_service/content-portal/user/content/page", "{\"pageSize\":20,\"currentPage\":" + page++ + ",\"apiCode\":\"" + code + "\",\"notice\":false}", what);
    }
    
    static class NewsAdapter extends RecyclerAdapter<HashMap<String, String>> {
        
        
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(ItemNewsBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot()) {
            };
        }
        
        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ItemNewsBinding binding = ItemNewsBinding.bind(holder.itemView);
            HashMap<String, String> item = get(position);
            Context context = holder.itemView.getContext();
            holder.itemView.setOnClickListener(v -> context.startActivity(new Intent(context, BrowserActivity.class).setData(Uri.parse(item.get("url"))), ActivityOptionsCompat.makeSceneTransitionAnimation((Activity) context, v, "miniapp").toBundle()));
            binding.title.setText(item.getOrDefault("title", ""));
            binding.content.setText(String.format("#%s #%s", item.getOrDefault("source", ""), item.getOrDefault("time", "")));
            String img = trim(item.get("image"));
            AuthorizationJar authorizationJar = new AuthorizationJar(context);
            if (!img.isEmpty())
                Glide.with(context).load(new GlideUrl(img, new LazyHeaders.Builder()
                                .addHeader("Cookie", toStringOrDefault(authorizationJar.getCookie(img)))
                                .addHeader("Authorization", toStringOrDefault(authorizationJar.getAuthorization(CommonUtil.getHost(img))))
                                .build()))
                        .timeout(15000)
                        .override(params.dpToPx(120), params.dpToPx(120)).optionalFitCenter().transform(new RoundedCorners(16))
                        .into(binding.image);
            super.onBindViewHolder(holder, position);
        }
        
    }
}