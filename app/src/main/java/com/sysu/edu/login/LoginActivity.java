package com.sysu.edu.login;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.tabs.TabLayoutMediator;
import com.sysu.edu.R;
import com.sysu.edu.academic.Pager2Adapter;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.ActivityLoginBinding;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity {

    Handler handler;
    WebView web;
    ActivityLoginBinding binding;

    public static void initModel(FragmentActivity activity, LoginViewModel model, String target, Runnable afterLogin) {
        SharedPreferences privacy = activity.getSharedPreferences("privacy", 0);
        model.getPassword().observe(activity, s -> privacy.edit().putString("password", s).apply());
        model.getAccount().observe(activity, s -> privacy.edit().putString("username", s).apply());
        model.setAccount(privacy.getString("username", ""));
        model.setPassword(privacy.getString("password", ""));
        model.setTarget(target);
        model.setUrl(TargetUrl.LOGIN);
        model.getLogin().observe(activity, b -> {
            if (b) {
                SharedPreferences.Editor edit = privacy.edit();
                String cookie = model.getCookie().getValue();
                Matcher match = Pattern.compile("ibps-1.0.1-token=(.+?);").matcher(cookie + ";");
                if (match.find()) {
                    edit.putString("token", match.group(1));
                }
                edit.putString("Cookie", cookie);
                edit.apply();
                afterLogin.run();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.pager2.setAdapter(new Pager2Adapter(this).add(new LoginWebFragment()).add(new LoginFragment()));
        binding.tool.setNavigationOnClickListener(v -> finishAfterTransition());
        /*binding.tool.getMenu().add(R.string.confirm).setIcon(R.drawable.submit).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM).setOnMenuItemClickListener(menuItem -> {
            web.loadUrl(Objects.requireNonNull(getIntent().getStringExtra("url")));
            return false;
        });*/
        new TabLayoutMediator(binding.options, binding.pager2, (tab, i) -> tab.setText(new int[]{R.string.web_login, R.string.password_login}[i])).attach();
        initModel(this, new ViewModelProvider(this).get(LoginViewModel.class), getIntent().getStringExtra("url") == null ? "https://jwxt.sysu.edu.cn/jwxt/yd/index/#/Home" : getIntent().getStringExtra("url"), () -> {
            setResult(RESULT_OK);
            supportFinishAfterTransition();
        });
       /* handler=new Handler(getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                //sessionId = (String) msg.obj;
                //Glide.with(Login.this).load(new GlideUrl("https://cas.sysu.edu.cn/cas/captcha.jsp",new LazyHeaders.Builder().addHeader("Cookie",sessionId).build())).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).into(binding.ca);
                //                        Glide.with(Login.this).load(new GlideUrl("https://cas.sysu.edu.cn/cas/captcha.jsp",new LazyHeaders.Builder().addHeader("Cookie",sessionId).build())).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).into(new CustomTarget<Drawable>(){
                //                            @Override
                //                            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                //                                ((TextInputLayout)findViewById(R.id.cac)).setEndIconDrawable(resource);
                //                            }
                //
                //                            @Override
                //                            public void onLoadCleared(@Nullable Drawable placeholder) {
                //
                //                            }
                //                        });
                // ((TextInputLayout)findViewById(R.id.cac)).setEndIconDrawable(Glide.with(Login.this).asDrawable().load(new GlideUrl("https://cas.sysu.edu.cn/cas/captcha.jsp",new LazyHeaders.Builder().addHeader("Cookie",sessionId).build())).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).submit().get());
            }
        };
//        new OkHttpClient.Builder().build().newCall(new Request.Builder().loginUrl("https://cas.sysu.edu.cn/cas/login?service=https://jwxt.sysu.edu.cn/jwxt/api/sso/cas/login?pattern=student-login").build()).enqueue(new Callback() {
//            @Override
//            public void onFailure(@NonNull Call call, @NonNull IOException e) {
//
//            }
//
//            @Override
//            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
//                String cookie = response.header("Set-Cookie");
//                Message msg=new Message();
//                msg.what=1;
//                msg.obj=cookie;
//                handler.sendMessage(msg);
//            }
//        });

    }*/
    }
}