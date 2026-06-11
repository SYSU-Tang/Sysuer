package com.sysu.edu.life;

import static com.sysu.edu.api.CommonUtil.trim;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.tabs.TabLayoutMediator;
import com.sysu.edu.BaseActivity;
import com.sysu.edu.R;
import com.sysu.edu.api.AuthorizationJar;
import com.sysu.edu.api.AuthorizationManager;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.browser.BrowserActivity;
import com.sysu.edu.databinding.ActivityNewsBinding;
import com.sysu.edu.view.AdapterListener;
import com.sysu.edu.view.Pager2Adapter;
import com.sysu.edu.view.RecyclerAdapter;

import java.util.Objects;
import java.util.stream.IntStream;

public class NewsActivity extends BaseActivity {
    
    HttpManager http;
    AuthorizationManager authorizationManager;
    EditText edit;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityNewsBinding binding = ActivityNewsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        authorizationManager = new AuthorizationManager("https://iportal.sysu.edu.cn/", "https://iportal-443.webvpn.sysu.edu.cn/");
        Pager2Adapter adapter = new Pager2Adapter(this);
        IntStream.range(0, 4).forEach(i -> adapter.add(new NewsFragment(i)));
        binding.pager.setAdapter(adapter);
        new TabLayoutMediator(binding.tabLayout, binding.pager, (tab, position) -> tab.setText(new String[]{"资讯", "公众号", "通知", "今日中大"}[position])).attach();
        SuggestionAdapter suggestionAdapter = new SuggestionAdapter();
        
        suggestionAdapter.setListener(new AdapterListener() {
            @Override
            public void onBind(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, RecyclerView.ViewHolder holder, int position) {
                holder.itemView.setOnClickListener(v -> startActivity(new Intent(NewsActivity.this, BrowserActivity.class).setData(Uri.parse(String.format("https://iportal.sysu.edu.cn/searchWeb/#/index?searchWord=%s&module=default&size=10&current=1&sortType=score&searchType=3", suggestionAdapter.get(position)))), ActivityOptionsCompat.makeSceneTransitionAnimation(NewsActivity.this, v, "miniapp").toBundle()));
            }
            
            @Override
            public void onCreate(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, ViewBinding binding) {
            
            }
        });
        Params params = new Params(this);
        params.setCallback(this::getSuggestions);
        binding.sugs.setAdapter(suggestionAdapter);
        binding.sugs.setLayoutManager(new GridLayoutManager(this, 1));
        http = new HttpManager(new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                Bundle rdata = msg.getData();
                boolean isJSON = rdata.getBoolean("isJSON");
                String json = rdata.getString("data");
                if (json == null) {
                    params.toast(R.string.no_net_connected);
                    return;
                }
                System.out.println(json);
                if (!isJSON) {
                    if (!authorizationManager.isAuthorized(json)) {
                        params.toast(R.string.login_warning);
                        params.gotoLogin(authorizationManager.isAccessible() ? TargetUrl.NEWS : TargetUrl.NEWS_WEBVPN);
                        return;
                    }
                    if (!authorizationManager.isAccessible(json)) {
                        params.toast(R.string.educational_wifi_warning);
                        getSuggestions();
                        return;
                    }
                    
                }
                JSONObject data = Objects.requireNonNull(JSONObject.parseObject(json));
                Object code = data.get("code");
                if (Objects.equals(code, "0000")) {
                    if (msg.what == 1) {
                        suggestionAdapter.clear();
                        data.getJSONObject("data").getJSONArray("suggests").forEach(e -> suggestionAdapter.add((String) e));
                    } else if (Objects.equals(code, 496)) {
                        params.toast(data.getString("message"));
                        params.gotoLogin(authorizationManager.isAccessible() ? TargetUrl.NEWS : TargetUrl.NEWS_WEBVPN);
                    }
                    //suggestion
                } else {
                    params.toast(data.getString("code") + "\n" + data.getString("message"));
                }
            }
        });
        http.setParams(params);
        http.setAuthorizationRequired(true);
        http.setAuthorizationJar(new AuthorizationJar(this));
        edit = binding.searchView.getEditText();
        edit.setOnEditorActionListener((_, _, _) -> {
            binding.searchView.hide();
            return false;
        });
        edit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            
            }
            
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (!edit.getText().toString().isEmpty()) getSuggestions(edit.getText().toString());
            }
            
            @Override
            public void afterTextChanged(Editable editable) {
            
            }
        });
    }
    
    void getSuggestions() {
        getSuggestions(edit.getText().toString());
    }
    
    void getSuggestions(String keyword) {
        http.postRequest(authorizationManager.getHost() + "ai_service/search-server/needle/suggest", String.format("{\"aliasName\":\"collection_data\",\"keyWord\":\"%s\"}", keyword), 1);
    }
    
    static class SuggestionAdapter extends RecyclerAdapter<String> {
        
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sug, parent, false)) {
            };
        }
        
        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ((TextView) holder.itemView).setText(trim(get(position)));
            super.onBindViewHolder(holder, position);
        }
        
    }
}