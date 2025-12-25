package com.sysu.edu.login;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MenuItem;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.tabs.TabLayoutMediator;
import com.sysu.edu.R;
import com.sysu.edu.academic.Pager2Adapter;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.ActivityLoginBinding;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity {

    Handler handler;
    WebView web;
    SharedPreferences privacy;
    ActivityLoginBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityLoginBinding.inflate(getLayoutInflater());
        LoginViewModel model = new ViewModelProvider(this).get(LoginViewModel.class);
        model.getPassword().observe(this, s -> privacy.edit().putString("password", s).apply());
        model.getAccount().observe(this, s -> privacy.edit().putString("username", s).apply());
        setContentView(binding.getRoot());
        privacy = getSharedPreferences("privacy", 0);
        model.setAccount(privacy.getString("username",""));
        model.setPassword(privacy.getString("password",""));
        binding.pager2.setAdapter(new Pager2Adapter(this).add(new LoginWebFragment()).add(new LoginFragment()));
        binding.tool.setNavigationOnClickListener(v->finishAfterTransition());
        model.setTarget(getIntent().getStringExtra("url")==null ?"https://jwxt.sysu.edu.cn/jwxt/yd/index/#/Home":getIntent().getStringExtra("url"));
        model.setUrl(TargetUrl.LOGIN);
        binding.tool.getMenu().add(R.string.confirm).setIcon(R.drawable.submit).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM).setOnMenuItemClickListener(menuItem -> {
            web.loadUrl(Objects.requireNonNull(getIntent().getStringExtra("url")));
            return false;
        });
        new TabLayoutMediator(binding.options, binding.pager2, (tab, i) -> tab.setText(new int[]{R.string.web_login,R.string.password_login}[i])).attach();
        model.getLogin().observe(this,b->{
            if(b){
                SharedPreferences.Editor edit = privacy.edit();
                //System.out.println(sessionId);
                String cookie = model.getCookie().getValue();
                Matcher match = Pattern.compile("ibps-1.0.1-token=(.+?);").matcher(cookie + ";");
                if(match.find()) {
                    edit.putString("token", match.group(1));
                }
                //System.out.println(cookie);
                edit.putString("Cookie",cookie);
                edit.putString("username", model.getAccount().getValue());
                edit.putString("password", model.getPassword().getValue());
                edit.apply();
                setResult(RESULT_OK);
                finishAfterTransition();
            }
        });
        handler=new Handler(getMainLooper()){
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

    }
}