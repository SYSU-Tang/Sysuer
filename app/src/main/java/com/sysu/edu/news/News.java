package com.sysu.edu.news;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.sysu.edu.R;
import com.sysu.edu.academic.BrowserActivity;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.ActivityNewsBinding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class News extends AppCompatActivity {
    ActivityNewsBinding binding;
    OkHttpClient http = new OkHttpClient.Builder().build();
    Handler handler;
    String cookie = "login_token_ec583190dcd12bca757dd13df10f59c3=ad6e129cb0c2e7ad6d842afa0e0ebf31; username_ec583190dcd12bca757dd13df10f59c3=tangxb6; login_sn_ec583190dcd12bca757dd13df10f59c3=0c3845934e6ec207f5b898ed0d3dd86f;";//cookie + ";_webvpn_key=eyJhbGciOiJIUzI1NiJ9.eyJ1c2VyIjoidGFuZ3hiNiIsImdyb3VwcyI6WzNdLCJpYXQiOjE3NDM5Mjg1OTUsImV4cCI6MTc0NDAxNDk5NX0.luGDbfa_19Ye5TBVpwo3gaZPXldD7gsnSqGkX6IJHb0;";
    Params params;
    //String authorization = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsidXNlcl9tYW5hZ2VyIl0sImNsaWVudF9pZF9zeXMiOiJ6c2g1XzEwMDA0MCIsInVzZXJfbmFtZSI6IjI0MzA4MTUyIiwic2NvcGUiOlsiYWxsIl0sIm5hbWUiOiIyNDMwODE1MiIsImV4cCI6MTc1MTk4OTIyNiwiYXV0aG9yaXRpZXMiOlsiQURNSU4iXSwianRpIjoiZFFqR1Q5Q25Ia1lWUDY0VmlGZFZURExCU1lNIiwiY2xpZW50X2lkIjoiMTY3M2YwMWQ5NjFhNjEwZmU5MjIwZWZmMGQ3YjNiYzQiLCJ1c2VybmFtZSI6IjI0MzA4MTUyIn0.wYTyy8gBr37xItZW2qJp81W2T-17-E9y4RQiODLj9pQ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNewsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        class Adapter extends FragmentStateAdapter {
            //  final List<NewsFragment> pages = List.of(new NewsFragment(cookie,0),new NewsFragment(cookie,1));
            final ArrayList<NewsFragment> fs = new ArrayList<>();

            public Adapter(@NonNull FragmentActivity fragmentActivity) {
                super(fragmentActivity);
            }

            @NonNull
            @Override
            public Fragment createFragment(int position) {
                NewsFragment fragment = new NewsFragment(cookie, position);
                fs.add(fragment);
                return fragment;
            }

            @Override
            public int getItemCount() {
                return 4;
            }
//            public NewsFragment getItem(int i){
//                return fs.get(i);
//            }
        }
        Adapter adapter = new Adapter(this);
        binding.pager.setAdapter(adapter);
        new TabLayoutMediator(binding.tabLayout, binding.pager, (tab, position) -> tab.setText(new String[]{"资讯", "公众号", "通知", "今日中大"}[position])).attach();
        handler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
            }
        };
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                //adapter.getItem(binding.pager.getCurrentItem()).run.run();
            }
        });
        SugAdp sug = new SugAdp(this);
        params = new Params(this);
        binding.sugs.setAdapter(sug);
        binding.sugs.setLayoutManager(new GridLayoutManager(this, 1));
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                Bundle rdata = msg.getData();
                boolean isJson = !rdata.getBoolean("isJson");
                String json = rdata.getString("data");
                JSONObject data;
                if (isJson) {
                    return;
                } else {
                    data = JSON.parseObject(json);
                }
                if (data == null) {
                    return;
                }
                    if (Objects.equals(data.get("code"), "0000")) {
                        if (msg.what == 1) {
                        sug.clear();
                        data.getJSONObject("data").getJSONArray("suggests").forEach(e -> sug.add((String) e));
                    } else if (Objects.equals(data.get("code"), 496)) {
                        params.toast(data.getString("message"));
                    }
                    //suggestion
                }
            }
        };
        EditText edit = binding.searchView.getEditText();
        edit.setOnEditorActionListener((textView, i, keyEvent) -> {
            binding.searchView.hide();
            return false;
        });
        edit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (!edit.getText().toString().isEmpty()) {
                    getSug(edit.getText().toString());
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    void getSug(String keyword) {
        http.newCall(new Request.Builder().url("https://iportal.sysu.edu.cn/ai_service/search-server/needle/suggest")
                .post(RequestBody.create(String.format("{\"aliasName\":\"collection_data\",\"keyWord\":\"%s\"}", keyword), MediaType.parse("application/json")))
                //.header("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsidXNlcl9tYW5hZ2VyIl0sImNsaWVudF9pZF9zeXMiOiJ6c3NlYXJjaF8xMDAwNTAiLCJ1c2VyX25hbWUiOiIyNDMwODE1MiIsInNjb3BlIjpbImFsbCJdLCJuYW1lIjoiMjQzMDgxNTIiLCJleHAiOjE3NTkzOTg1MjUsImF1dGhvcml0aWVzIjpbIkFETUlOIl0sImp0aSI6Inp5Z3RBeEhDdkx0ckFMODdnWWJuNDhxWUlyNCIsImNsaWVudF9pZCI6IjE2NzNmMDFkOTYxYTYxMGZlOTIyMGVmZjBkN2IzYmM0IiwidXNlcm5hbWUiOiIyNDMwODE1MiJ9.viBuKujwPQO9ai5orJsJtloWhwZhDThl40O_kfJFK_k")//"Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsidXNlcl9tYW5hZ2VyIl0sImNsaWVudF9pZF9zeXMiOiJ6c2g1XzEwMDA0MCIsInVzZXJfbmFtZSI6IjI0MzA4MTUyIiwic2NvcGUiOlsiYWxsIl0sIm5hbWUiOiIyNDMwODE1MiIsImV4cCI6MTc1MTk4OTIyNiwiYXV0aG9yaXRpZXMiOlsiQURNSU4iXSwianRpIjoiZFFqR1Q5Q25Ia1lWUDY0VmlGZFZURExCU1lNIiwiY2xpZW50X2lkIjoiMTY3M2YwMWQ5NjFhNjEwZmU5MjIwZWZmMGQ3YjNiYzQiLCJ1c2VybmFtZSI6IjI0MzA4MTUyIn0.wYTyy8gBr37xItZW2qJp81W2T-17-E9y4RQiODLj9pQ")
                //.header("Cookie", cookie)
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what = 1;
                Bundle data = new Bundle();
                data.putBoolean("isJson", Objects.requireNonNull(response.header("Content-Type", "")).startsWith("application/json"));
                data.putString("data", response.body().string());
                msg.setData(data);
                handler.sendMessage(msg);
            }
        });
    }
}

class SugAdp extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    final FragmentActivity context;
    ArrayList<String> data = new ArrayList<>();

    public SugAdp(FragmentActivity context) {
        super();
        this.context = context;
    }

    public void add(String a) {
        int tmp = getItemCount();
        data.add(a);
        notifyItemInserted(tmp);
    }

    public void clear() {
        int tmp = getItemCount();
        data.clear();
        notifyItemRangeRemoved(0, tmp);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TextView view = (TextView) LayoutInflater.from(context).inflate(R.layout.item_sug, parent, false);
        return new RecyclerView.ViewHolder(view) {
        };
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((TextView) holder.itemView).setText(data.get(position));
        holder.itemView.setOnClickListener(v -> context.startActivity(new Intent(context, BrowserActivity.class).setData(Uri.parse(String.format("https://iportal.sysu.edu.cn/searchWeb/#/index?searchWord=%s&module=default&size=10&current=1&sortType=score&searchType=3", data.get(position)))), ActivityOptionsCompat.makeSceneTransitionAnimation(context, v, "miniapp").toBundle()));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}