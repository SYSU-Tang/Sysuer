package com.sysu.edu.academic;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayoutMediator;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.ActivityPagerBinding;
import com.sysu.edu.login.LoginActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AcademyNotification extends AppCompatActivity {

    ActivityPagerBinding binding;
    String cookie;
    Handler handler;
    OkHttpClient http = new OkHttpClient.Builder().build();
    AlertDialog dialog;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setTitle(R.string.academic_affair_notice);
        binding.toolbar.setNavigationOnClickListener(v -> supportFinishAfterTransition());
        Params params = new Params(this);
        cookie = params.getCookie();
        ActivityResultLauncher<Intent> launch = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
            if (o.getResultCode() == Activity.RESULT_OK) {
                cookie = params.getCookie();
                getNotices();
            }
        });
        Pager2Adapter adp = new Pager2Adapter(this);
        NewsFragment f1 = new NewsFragment();
        NewsFragment f2 = new NewsFragment();
        StaggeredListener l = new StaggeredListener() {
            @Override
            public void onBind(StaggeredFragment.StaggeredAdapter a, RecyclerView.ViewHolder holder, int position) {

            }

            @Override
            public void onCreate(StaggeredFragment.StaggeredAdapter a, ViewBinding binding) {

            }

            @Override
            public void onBind(NewsFragment.NewsAdp a, RecyclerView.ViewHolder holder, int position) {
                holder.itemView.setOnClickListener(v -> {
                    dialog.setTitle(a.data.get(position).getString("title"));
                    getContent(a.data.get(position).getString("id"));
                });
            }

            @Override
            public void onCreate(NewsFragment.NewsAdp a, ViewBinding binding) {

            }
        };
        f1.setListener(this, l);
        f2.setListener(this, l);
        adp.add(f1);
        adp.add(f2);
        binding.pager.setAdapter(adp);
        dialog = new MaterialAlertDialogBuilder(this).setMessage("").setPositiveButton(R.string.confirm, (dialogInterface, i) -> {
        }).create();
        new TabLayoutMediator(binding.tabs, binding.pager, (tab, position) -> tab.setText(new int[]{R.string.academic_affair_notice, R.string.school_affair_notice}[position])).attach();
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == -1) {
                    params.toast(getString(R.string.no_wifi_warning));
                } else {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    if (response != null && response.getInteger("code").equals(200)) {
                        if (response.get("data") != null) {
                            switch (msg.what) {
                                case 0:
                                case 1:
                                    response.getJSONObject("data").getJSONArray("list").forEach(a -> ((NewsFragment) adp.getItem(msg.what)).add(AcademyNotification.this, (JSONObject) a));
                                    break;
                                case 2:
                                    //System.out.println(response);
                                    dialog.setMessage(Html.fromHtml(response.getString("data"), Html.FROM_HTML_MODE_COMPACT));
                                    dialog.show();
                                    TextView text = Objects.requireNonNull(dialog.findViewById(android.R.id.message));
                                    text.setMovementMethod(LinkMovementMethod.getInstance());
                                    CharSequence str = text.getText();
                                    if (str instanceof Spannable) {
                                        Spannable sp = (Spannable) text.getText();
                                        SpannableStringBuilder style = new SpannableStringBuilder(str);
                                        style.clearSpans();
                                        for (URLSpan url : sp.getSpans(0, str.length(), URLSpan.class)) {
                                            try {
                                                style.setSpan(new MyClickSpan(url.getURL()), sp.getSpanStart(url), sp.getSpanEnd(url), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                            } catch (MalformedURLException e) {
                                                throw new RuntimeException(e);
                                            }
                                        }
                                        text.setText(style);
                                    }
                            }
                        }
                    } else {
                        params.toast(getString(R.string.login_warning));
                        launch.launch(new Intent(AcademyNotification.this, LoginActivity.class).putExtra("url", TargetUrl.JWXT));
                    }
                }
            }
        };
        getSchoolNotices();
        getNotices();
    }

    void getList(String a, int what) {
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/system-manage/info-delivery?column=" + a + "&deliveryObject=02&status=1&resourceCode=jwgld")
                .header("Cookie", cookie)
                .header("Referer", "https://jwxt.sysu.edu.cn/jwxt/")
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = new Message();
                msg.what = -1;
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what = what;
                msg.obj = response.body().string();
                handler.sendMessage(msg);
            }
        });
    }

    void getNotices() {
        getList("01", 0);
    }

    void getSchoolNotices() {
        getList("02", 1);
    }

    void getContent(String id) {
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/system-manage/info-delivery/noticeId?id=" + id)
                .header("Cookie", cookie)
                .header("Referer", "https://jwxt.sysu.edu.cn/jwxt/")
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = new Message();
                msg.what = -1;
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what = 2;
                msg.obj = response.body().string();
                handler.sendMessage(msg);
            }
        });
    }

    class MyClickSpan extends ClickableSpan {
        String text;
        String url;

        public MyClickSpan(String url) throws MalformedURLException {
            this.url = url;
            //System.out.println(url);
            if (url.startsWith("/")) {
                this.text = Uri.parse(url).getQueryParameter("fileName");
            }
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            ds.setUnderlineText(false);
            super.updateDrawState(ds);
        }

        @Override
        public void onClick(@NonNull View v) {
            if (url.startsWith("/")) {
                new OkHttpClient.Builder().build().newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn" + url)
                        .header("Cookie", AcademyNotification.this.cookie)
                        .header("Referer", "https://jwxt.sysu.edu.cn/jwxt/")
                        .build()).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        //String mediaType = Objects.requireNonNull(response.header().contentType()).toString();
                        // 回到主线程操纵界面
                        //runOnUiThread(() -> tv_result.setText("下载网络文件返回："+desc));
                        String path = Environment.getExternalStorageDirectory() + "/Download/" + text.replace("\n", "");
                        OutputStream outputStream = new FileOutputStream(path);
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = response.body().byteStream().read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                        outputStream.close();
                        Intent intent = new Intent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).setAction(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(new File(path)), "*/*");
                        v.getContext().startActivity(intent);
                    }
                });
            } else {
                v.getContext().startActivity(new Intent().setAction(Intent.ACTION_VIEW).setData(Uri.parse(url)));
            }
        }
    }
}