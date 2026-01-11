package com.sysu.edu.life;

import static com.sysu.edu.life.GymPreservation.encode;

import android.content.Context;
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
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.ItemFieldBinding;
import com.sysu.edu.databinding.RecyclerViewScrollBinding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GymPreservationListFragment extends Fragment {
    static final String authorization = "Bearer eyJhbGciOiJSUzI1NiIsImtpZCI6IjI1QjExQTk3MDQ0Qjc5MUVGN0I2MDAzOTdDMzk2MDJDQzA1RjY5NTYiLCJ4NXQiOiJKYkVhbHdSTGVSNzN0Z0E1ZkRsZ0xNQmZhVlkiLCJ0eXAiOiJKV1QifQ.eyJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA1LzA1L2lkZW50aXR5L2NsYWltcy9uYW1lIjoidGFuZ3hiNiIsImxvbmdUZXJtQXV0aGVudGljYXRpb25SZXF1ZXN0VG9rZW5Vc2VkIjoiZmFsc2UiLCJpc0Zyb21OZXdMb2dpbiI6InRydWUiLCJhdXRoZW50aWNhdGlvbkRhdGUiOiIyMDI2LTAxLTExVDA0OjU0OjAzLjU3NVoiLCJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA1LzA1L2lkZW50aXR5L2NsYWltcy9uYW1laWRlbnRpZmllciI6InRhbmd4YjYiLCJodHRwOi8vc2NpZW50aWEuY29tL2NsYWltcy9JbnN0aXR1dGlvbiI6IjRmYjM3ZTI4LTBhOTAtNGJiMC1iMGUwLWI3OThlY2U2ZjMyMCIsImh0dHA6Ly9zY2llbnRpYS5jb20vY2xhaW1zL0luc3RpdHV0aW9uTmFtZSI6Im1haWwuc3lzdSIsImh0dHA6Ly9zY2hlbWFzLnhtbHNvYXAub3JnL3dzLzIwMDUvMDUvaWRlbnRpdHkvY2xhaW1zL2VtYWlsYWRkcmVzcyI6InRhbmd4YjZAbWFpbC5zeXN1LmVkdS5jbiIsImh0dHA6Ly9zY2hlbWFzLnhtbHNvYXAub3JnL3dzLzIwMDUvMDUvaWRlbnRpdHkvY2xhaW1zL2dpdmVubmFtZSI6InRhbmd4YjYiLCJodHRwOi8vc2NpZW50aWEuY29tL2NsYWltcy9TY29wZSI6IlJCIiwibmJmIjoxNzY4MTE5NjkxLCJleHAiOjE3NjgxMjMyOTEsImlzcyI6Imh0dHBzOi8vbG9jYWxob3N0LyIsImF1ZCI6Imh0dHA6Ly9yZXNvdXJjZWJvb2tlcmFwaS5jbG91ZGFwcC5uZXQvIn0.Q1EGhlbla6ObbYkkzUSYXOZodcf868dj8yFrHCd_g4thzOadicts6mo6-K_w85PgkEQbCinilq5q8ITgWsbI2CR-mLrZAg2R6RnpPvqnU_L9-DHYrvLWHiUFUBE1V8xjq563MkezWHIJjC4ll7L2Y0H3OmUmVseVms4EViahiGItlE6u-ZCbUwYEIktXFa9ywpoQ0hGymbpZo4v-xYoMDJcuiEwJ4uWMKMTX-u5czi6hCBBlNB8ziorxrDNsHGyW5O8v7geNEiGl87VCNNeA4ZbXg1XxrWjeIvec5S7RHTwuJujaQLstNaSvtjOGGc63aqCIR3OtwQLNFZZd4zvMsA";
    static final String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0";
    final OkHttpClient http = new OkHttpClient.Builder().build();
    Handler handler;
    Params params;
    String token = "";
    StaggeredGridLayoutManager layoutManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        RecyclerViewScrollBinding binding = RecyclerViewScrollBinding.inflate(inflater, container, false);
        params = new Params(requireActivity());
        params.setCallback(this,this::getCampus);
//        LinearLayoutManager linear = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
//        binding.panel.setLayoutManager(linear);
//        DateAdapter adp = new DateAdapter(this);
//        binding.panel.setAdapter(adp);
        layoutManager = new StaggeredGridLayoutManager(params.getColumn(), StaggeredGridLayoutManager.VERTICAL);
        binding.getRoot().setLayoutManager(layoutManager);
        FieldAdapter fieldAdapter = new FieldAdapter(requireContext());
        fieldAdapter.setAction(position -> {
            Bundle bundle = new Bundle();
            bundle.putInt("position", position);
            bundle.putInt("code", requireArguments().getInt("code")+1);
            Navigation.findNavController(binding.getRoot()).navigate(R.id.action_gym_preservation_campus_fragment_to_gym_preservation_field_fragment, bundle);
        });
        binding.getRoot().setAdapter(fieldAdapter);
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                Bundle rdata = msg.getData();
                boolean isJson = !rdata.getBoolean("isJson");
                String json = rdata.getString("data");
                // System.out.println(json);
                JSONArray data;
                if (isJson) {
                    params.toast(R.string.educational_wifi_warning);
                    // params.gotoLogin(binding.toolbar, TargetUrl.GYM);
                    return;
                } else {
                    data = JSON.parseArray(json);
                }
                if (data != null) {
                    if (msg.what == 1) {
                        data.forEach(e -> fieldAdapter.add((JSONObject) e));
                    }
                }
            }
        };
//        getCampus();
        send("");
        return binding.getRoot();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        layoutManager.setSpanCount(params.getColumn());
    }

    void sendRequest(String url, int what) {
        http.newCall(new Request.Builder()
                .url(url)
                .header("Accept", "application/json, text/plain, */*")
                .header("Cookie", token)
                .header("Authorization", authorization)
                .header("User-Agent", ua)
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message message = new Message();
                message.what = -1;
                handler.sendMessage(message);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message message = new Message();
                message.what = what;
                Bundle data = new Bundle();
                String dataString = response.body().string();
                data.putBoolean("isJson", Objects.requireNonNull(response.header("Content-Type", "")).startsWith("application/json"));
                data.putString("data", dataString);
                message.setData(data);
                // System.out.println(response.headers("Set-Cookie"));
                handler.sendMessage(message);
            }
        });
    }

    void getCampus() {
        sendRequest("https://gym.sysu.edu.cn/api/Campus/active", 1);
    }


    void getVenue() {
        sendRequest("https://gym.sysu.edu.cn/api/venuetype/all", 1);
    }

    public void send(String cookie) {
        new OkHttpClient().newCall(new Request.Builder()
                        .header("Cookie", cookie)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                        .header("Authorization", authorization)
                        .url("https://gym.sysu.edu.cn/").build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        Matcher prefixMatcher = Pattern.compile("prefix = '(.+)'").matcher(response.body().string());
                        String prefix = null;
                        String safelineBotChallenge = null;
                        if (prefixMatcher.find()) {
                            prefix = prefixMatcher.group(1);
                        }
                        List<String> cookie = response.headers("Set-Cookie");
                        String cookies = String.join("", cookie);
                        Matcher safelineBotChallengeMatcher = Pattern.compile("safeline_bot_challenge=(.+?);").matcher(cookies);
                        if (safelineBotChallengeMatcher.find()) {
                            safelineBotChallenge = safelineBotChallengeMatcher.group(1);
                        }
                        if (!Pattern.compile("safeline_bot_token=[^0]").matcher(cookies).find()) {
                            send(cookies + "; safeline_bot_challenge_ans=" + encode(prefix, safelineBotChallenge));
                        } else {
                            Matcher safelineBotTokenMatcher = Pattern.compile("(safeline_bot_token=.+?);").matcher(cookies);
                            if (safelineBotTokenMatcher.find()) {
                                token = safelineBotTokenMatcher.group(1);
                                System.out.println(requireArguments().getInt("code"));
                                if (Objects.equals(requireArguments().getInt("code"), 0)) {
                                    getCampus();
                                } else {
                                    getVenue();
                                }
                            }
                        }
                    }
                });
    }

    private static class FieldAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        Context context;

        ArrayList<JSONObject> data = new ArrayList<>();
        Consumer<Integer> action;

        public FieldAdapter(Context context) {
            super();
            this.context = context;
        }

        public void setAction(Consumer<Integer> action) {
            this.action = action;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(ItemFieldBinding.inflate(LayoutInflater.from(context), parent, false).getRoot()) {
            };
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ItemFieldBinding binding = ItemFieldBinding.bind(holder.itemView);
            binding.title.setText(data.get(position).getString("Name"));
            binding.getRoot().setOnClickListener(v -> action.accept(position));
            String imageUrl = data.get(position)
                    .getString("ImageUrl");
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(context).load(new GlideUrl(imageUrl, new LazyHeaders.Builder().addHeader("User-Agent", ua)
                        .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                        .build())).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).into(binding.image);
            }
        }

        public void add(JSONObject jsonObject) {
            data.add(jsonObject);
            notifyItemInserted(data.size() - 1);
        }

        public void clear() {
            int tmp = getItemCount();
            data.clear();
            notifyItemRangeRemoved(0, tmp);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }
}
