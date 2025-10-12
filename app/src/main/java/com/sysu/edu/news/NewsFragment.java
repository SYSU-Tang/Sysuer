package com.sysu.edu.news;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
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
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.google.android.material.textview.MaterialTextView;
import com.sysu.edu.R;
import com.sysu.edu.academic.BrowserActivity;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.RecyclerViewScrollBinding;
import com.sysu.edu.extra.LoginActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NewsFragment extends Fragment {
    String cookie;
    int position;
    RecyclerViewScrollBinding binding;
    OkHttpClient http = new OkHttpClient.Builder().authenticator((route, response) -> {
//                System.out.println("Authenticating for response: " + response);
//                System.out.println("Challenges: " + response.challenges());
                String credential = Credentials.basic("tangxb6", "Tang@1245");
                System.out.println(credential);
                return response.request().newBuilder()
                        .header("Authorization", credential)
                        .build();
            }).build();
    Handler handler;
    int page=1;
    Runnable run;
    String authorization = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsidXNlcl9tYW5hZ2VyIl0sImNsaWVudF9pZF9zeXMiOiJ6c3NlYXJjaF8xMDAwNTAiLCJ1c2VyX25hbWUiOiIyNDMwODE1MiIsInNjb3BlIjpbImFsbCJdLCJuYW1lIjoiMjQzMDgxNTIiLCJleHAiOjE3NTkzOTg1MjUsImF1dGhvcml0aWVzIjpbIkFETUlOIl0sImp0aSI6Inp5Z3RBeEhDdkx0ckFMODdnWWJuNDhxWUlyNCIsImNsaWVudF9pZCI6IjE2NzNmMDFkOTYxYTYxMGZlOTIyMGVmZjBkN2IzYmM0IiwidXNlcm5hbWUiOiIyNDMwODE1MiJ9.viBuKujwPQO9ai5orJsJtloWhwZhDThl40O_kfJFK_k";//"Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsidXNlcl9tYW5hZ2VyIl0sImNsaWVudF9pZF9zeXMiOiJ6c2g1XzEwMDA0MCIsInVzZXJfbmFtZSI6IjI0MzA4MTUyIiwic2NvcGUiOlsiYWxsIl0sIm5hbWUiOiIyNDMwODE1MiIsImV4cCI6MTc1MTk4OTIyNiwiYXV0aG9yaXRpZXMiOlsiQURNSU4iXSwianRpIjoiZFFqR1Q5Q25Ia1lWUDY0VmlGZFZURExCU1lNIiwiY2xpZW50X2lkIjoiMTY3M2YwMWQ5NjFhNjEwZmU5MjIwZWZmMGQ3YjNiYzQiLCJ1c2VybmFtZSI6IjI0MzA4MTUyIn0.wYTyy8gBr37xItZW2qJp81W2T-17-E9y4RQiODLj9pQ";
    Params params;
    ActivityResultLauncher<Intent> launch;

    public NewsFragment(String cookie, int pos) {
        //this.cookie = "login_token_ec583190dcd12bca757dd13df10f59c3=ad6e129cb0c2e7ad6d842afa0e0ebf31; username_ec583190dcd12bca757dd13df10f59c3=tangxb6; login_sn_ec583190dcd12bca757dd13df10f59c3=0c3845934e6ec207f5b898ed0d3dd86f;";//cookie + ";_webvpn_key=eyJhbGciOiJIUzI1NiJ9.eyJ1c2VyIjoidGFuZ3hiNiIsImdyb3VwcyI6WzNdLCJpYXQiOjE3NDM5Mjg1OTUsImV4cCI6MTc0NDAxNDk5NX0.luGDbfa_19Ye5TBVpwo3gaZPXldD7gsnSqGkX6IJHb0;";
        this.position = pos;
        //System.out.println(cookie);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
                if (savedInstanceState == null) {
            binding = RecyclerViewScrollBinding.inflate(inflater);
            params = new Params(requireActivity());
            cookie = params.getCookie();
            //cookie = "authorization=Bearer%20eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsidXNlcl9tYW5hZ2VyIl0sImNsaWVudF9pZF9zeXMiOiJ6c3NlYXJjaF8xMDAwNTAiLCJ1c2VyX25hbWUiOiIyNDMwODE1MiIsInNjb3BlIjpbImFsbCJdLCJuYW1lIjoiMjQzMDgxNTIiLCJleHAiOjE3NTkzOTg1MjUsImF1dGhvcml0aWVzIjpbIkFETUlOIl0sImp0aSI6Inp5Z3RBeEhDdkx0ckFMODdnWWJuNDhxWUlyNCIsImNsaWVudF9pZCI6IjE2NzNmMDFkOTYxYTYxMGZlOTIyMGVmZjBkN2IzYmM0IiwidXNlcm5hbWUiOiIyNDMwODE1MiJ9.viBuKujwPQO9ai5orJsJtloWhwZhDThl40O_kfJFK_k";
            RecyclerView list = binding.recyclerView;
            list.setLayoutManager(new GridLayoutManager(requireContext(), params.getColumn()));
            list.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    if (recyclerView.canScrollVertically(1) && position != 0) {
                        run.run();
                    }
                    super.onScrolled(recyclerView, dx, dy);
                }
            });
            Adp adp = new Adp(requireActivity());
            list.setAdapter(adp);
            handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    Bundle rdata = msg.getData();
                    boolean isJson = !rdata.getBoolean("isJson");
                    String json = rdata.getString("data");
                    System.out.println(json);
                    JSONObject data;
                    if (isJson) {
                        return;
                    } else {
                        data = JSON.parseObject(json);
                    }
                    if (data == null) {
                        return;
                    }

                    if (msg.what == -1) {
                        params.toast(R.string.no_wifi_warning);
                    } else {
                        Integer code = data.getInteger("code");
                        JSONObject response = null;
                        if (msg.what != 3) {
                            response = data.getJSONObject("data");
                        }
                        if (code == 10000) {
                            switch (msg.what) {
                                case 2:
                                    response.getJSONArray("records").forEach(e -> {
                                        String title = ((JSONObject) e).getString("title");
                                        JSONArray cover = ((JSONObject) e).getJSONArray("coversPicList");
                                        String image = "";
                                        if (cover != null && !cover.isEmpty()) {
                                            if (cover.getJSONObject(0) != null && cover.getJSONObject(0).getString("outLink") != null) {
                                                image = cover.getJSONObject(0).getString("outLink");
                                            }
                                        }
                                        String url = ((JSONObject) e).getString("url");
                                        String time = ((JSONObject) e).getString("createTime");
                                        String source = ((JSONObject) e).getJSONObject("source").getString("seedName");
                                        adp.add(title, image, url, time, source);
                                    });
                                    break;
                                case 3:
                                    data.getJSONArray("data").forEach(e -> {
                                        String title = ((JSONObject) e).getString("title");
                                        JSONArray cover = ((JSONObject) e).getJSONArray("coversPicList");
                                        String image = "";
                                        if (cover != null && !cover.isEmpty()) {
                                            image = cover.getJSONObject(0).getString("outLink");
                                        }
                                        if (image == null) {
                                            image = "";
                                        }
                                        String url = ((JSONObject) e).getString("url");
                                        String time = ((JSONObject) e).getString("createTime");
                                        String source = ((JSONObject) e).getJSONObject("source").getString("seedName");
                                        adp.add(title, image, url, time, source);
                                    });
                                    break;
                                case 4:
                                    response.getJSONArray("records").forEach(e -> {
                                        String title = ((JSONObject) e).getString("title");
                                        JSONArray cover = ((JSONObject) e).getJSONArray("coversPicList");
                                        String image = "";
                                        if (cover != null && cover.getJSONObject(0) != null && !cover.isEmpty() && cover.getJSONObject(0).getString("outLink") != null) {
                                            image = cover.getJSONObject(0).getString("outLink");
                                        }
                                        String url = ((JSONObject) e).getString("url");
                                        String time = ((JSONObject) e).getString("createTime");
                                        String source = ((JSONObject) e).getJSONObject("source").getString("seedName");
                                        adp.add(title, image, url, time, source);
                                    });
                                    //通知
                                    break;
                                case 5:
                                    response.getJSONArray("records").forEach(e -> {
                                        String title = ((JSONObject) e).getString("title");
                                        JSONArray cover = ((JSONObject) e).getJSONArray("coversPicList");
                                        String image = "";
                                        if (cover != null && cover.getJSONObject(0) != null && !cover.isEmpty() && cover.getJSONObject(0).getString("outLink") != null) {
                                            image = cover.getJSONObject(0).getString("outLink");
                                        }
                                        String url = ((JSONObject) e).getString("url");
                                        String time = ((JSONObject) e).getString("createTime");
                                        String source = ((JSONObject) e).getJSONObject("source").getString("seedName");
                                        adp.add(title, image, url, time, source);
                                    });
                                    break;
                            }
                        } else if (code == 10003) {
                            params.toast(data.getString("message"));
                            launch.launch(new Intent(requireContext(), LoginActivity.class).putExtra("url", "https://cas.sysu.edu.cn/cas/login?service=https://portal.sysu.edu.cn/newClient/shiro-cas"));
                        }
                    } //今日中大

                }
            };
        }
        run = () -> {
            List.of(this::getNews, this::getSubscription, this::getNotice, (Runnable) this::getDailyNews).get(position).run();
            page += 1;
        };
        run.run();
        launch = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
            if (o.getResultCode() == Activity.RESULT_OK) {
                cookie = params.getCookie();
                run.run();
            }
        });
        //getAuthorization();
        return binding.getRoot();
    }

    void getNews() {
        http.newCall(new Request.Builder().url("https://iportal.sysu.edu.cn/ai_service/content-portal/recommend/query-recommend")

                // .post(RequestBody.create("{\"pageSize\":20,\"currentPage\":1,\"apiCode\":\"3ytr4e6c\",\"notice\":false}", MediaType.parse("application/json")))
                .post(RequestBody.create("", MediaType.parse("application/json")))
                //.header("Content-Length","0")
                .header("Authorization", authorization)
                //.header("Cookie", cookie)
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message message = new Message();
                message.what=-1;
                handler.sendMessage(message);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what = 3;
                Bundle data = new Bundle();
                data.putBoolean("isJson", Objects.requireNonNull(response.header("Content-Type", "")).startsWith("application/json"));
                data.putString("data",response.body().string());
                msg.setData(data);
                handler.sendMessage(msg);
            }
        });
    }

    void getSubscription() {
        http.newCall(new Request.Builder().url("https://iportal.sysu.edu.cn/ai_service/content-portal/user/content/page")
                .post(RequestBody.create("{\"pageSize\":20,\"currentPage\":"+page+",\"apiCode\":\"3ytr4e6c\",\"notice\":false}", MediaType.parse("application/json")))
                //.header("Content-type", "application/json")
                .header("Authorization", authorization)
                //.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                //.header("Cookie", cookie)
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message message = new Message();
                message.what=-1;
                handler.sendMessage(message);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what = 2;
                Bundle data = new Bundle();
                data.putBoolean("isJson", Objects.requireNonNull(response.header("Content-Type", "")).startsWith("application/json"));
                data.putString("data",response.body().string());
                msg.setData(data);
                handler.sendMessage(msg);
            }
        });
    }

//    void getAuthorization() {
//        http.newCall(new Request.Builder().url("https://iportal.sysu.edu.cn/ai_service/auth-center/account/zscasLogin?clientid=zssearch_100050;zsshow")
//                .header("Cookie", cookie)
//                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36 Edg/134.0.0.0")
//                .build()).enqueue(new Callback() {
//            @Override
//            public void onFailure(@NonNull Call call, @NonNull IOException e) {
//                Message message = new Message();
//                message.what=-1;
//                handler.sendMessage(message);
//            }
//
//            @Override
//            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
//                Message msg = new Message();
//                msg.what = 1;
//                Bundle data = new Bundle();
//                data.putBoolean("isJson", Objects.requireNonNull(response.header("Content-Type", "")).startsWith("application/json"));
//                data.putString("data", response.body().string());
//                msg.setData(data);
//                handler.sendMessage(msg);
//            }
//        });
//    }

    void getNotice() {
        http.newCall(new Request.Builder().url("https://iportal.sysu.edu.cn/ai_service/content-portal/user/content/page")
                .post(RequestBody.create("{\"pageSize\":20,\"currentPage\":"+page+",\"apiCode\":\"3ytunvv6\",\"notice\":false}", MediaType.parse("application/json")))
                .header("Authorization", authorization)
                //.header("Cookie", cookie)
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message message = new Message();
                message.what=-1;
                handler.sendMessage(message);
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what = 4;
                Bundle data = new Bundle();
                data.putBoolean("isJson", Objects.requireNonNull(response.header("Content-Type", "")).startsWith("application/json"));
                data.putString("data",response.body().string());
                msg.setData(data);
                handler.sendMessage(msg);
            }
        });
    }
    void getDailyNews() {
        http.newCall(new Request.Builder().url("https://iportal.sysu.edu.cn/ai_service/content-portal/user/content/page")
                .post(RequestBody.create("{\"pageSize\":20,\"currentPage\":"+page+",\"apiCode\":\"4cef8rqw\",\"notice\":false}", MediaType.parse("application/json")))
                .header("Authorization", authorization)
                //.header("Cookie", cookie)
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message message = new Message();
                message.what=-1;
                handler.sendMessage(message);
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what = 5;
                Bundle data = new Bundle();
                data.putBoolean("isJson", Objects.requireNonNull(response.header("Content-Type", "")).startsWith("application/json"));
                data.putString("data",response.body().string());
                msg.setData(data);
                handler.sendMessage(msg);
            }
        });
    }
//    void getContent() {
//        http.newCall(new Request.Builder().url("https://iportal.sysu.edu.cn/ai_service/content-portal/user/content/page")
//                .post(RequestBody.create("{\"pageSize\":20,\"currentPage\":"+page+",\"apiCode\":\"4cef8rqw\",\"notice\":false}", MediaType.parse("application/json")))
//                //.header("Authorization", authorization)
//                .header("Cookie", cookie).build()).enqueue(new Callback() {
//            @Override
//            public void onFailure(@NonNull Call call, @NonNull IOException e) {
//                Message message = new Message();
//                message.what=-1;
//                handler.sendMessage(message);
//            }
//            @Override
//            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
//                Message msg = new Message();
//                msg.what = 5;
//                Bundle data = new Bundle();
//                data.putBoolean("isJson", Objects.requireNonNull(response.header("Content-Type", "")).startsWith("application/json"));
//                data.putString("data",response.body().string());
//                msg.setData(data);
//                handler.sendMessage(msg);
//            }
//        });
//    }
}
class Adp extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    ArrayList<HashMap<String, String>> data = new ArrayList<>();
    FragmentActivity context;
    //String cookie;

    public Adp(FragmentActivity context) {
        super();
        this.context=context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecyclerView.ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_news, parent, false)) {
        };
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MaterialTextView title = holder.itemView.findViewById(R.id.title);
        MaterialTextView content = holder.itemView.findViewById(R.id.content);
        AppCompatImageView image = holder.itemView.findViewById(R.id.image);
        holder.itemView.setOnClickListener(v -> context.startActivity(new Intent(context, BrowserActivity.class).setData(Uri.parse(data.get(position).get("url"))), ActivityOptionsCompat.makeSceneTransitionAnimation(context,v,"miniapp").toBundle())
                //context.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(data.get(position).get("url"))), ActivityOptionsCompat.makeSceneTransitionAnimation(context, v, "miniapp").toBundle())
        );
        title.setText(data.get(position).getOrDefault("title",""));
        content.setText(String.format("#%s #%s",data.get(position).getOrDefault("source", "") , data.get(position).getOrDefault("time", "")));
        String img = data.get(position).get("image");
        if (img != null && !img.isEmpty()) {
            Glide.with(context).load(img)
                    // .diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true)
                    .placeholder(R.drawable.logo)
                    .override(new Params(context).dpToPx(120),new Params(context).dpToPx(120)).optionalFitCenter().transform(new RoundedCorners(16))
                    .into(image);
        }
    }
    void add(String title, String image, String url,String time,String source) {
        data.add(new HashMap<>(Map.of("title", title, "image", image, "url", url,"time",time,"source",source)));
        notifyItemInserted(getItemCount() - 1);
    }
    //    Adp setCookie(String cookie)
//    {
//        this.cookie=cookie;
//        return this;
//    }
    @Override
    public int getItemCount() {
        return data.size();
    }
}