package com.sysu.edu.academic;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.tabs.TabLayout;
import com.sysu.edu.databinding.CalendarBinding;

import java.io.IOException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CalendarActivity extends AppCompatActivity {

    Handler handler;
    int top=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        CalendarBinding binding = CalendarBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        binding.tool.setNavigationOnClickListener(view -> finishAfterTransition());
        binding.scroll.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            //top = binding.content.getChildAt(2).getTop();
            if(top>scrollY&&binding.tabs.getSelectedTabPosition()==1&&scrollY<oldScrollY){
                Objects.requireNonNull(binding.tabs.getTabAt(0)).select();
            }else if(top<=scrollY&&binding.tabs.getSelectedTabPosition()==0&&scrollY>oldScrollY){
                Objects.requireNonNull(binding.tabs.getTabAt(1)).select();
            }

        });
        binding.tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

                if(binding.content.getChildCount()>2){
                    top = binding.content.getChildAt(2).getTop();}
                switch (binding.tabs.getSelectedTabPosition()){
                    case 0:if(binding.scroll.getScrollY()>=top){binding.scroll.smoothScrollTo(0,0);}
                        break;
                    case 1:
                        if(binding.scroll.getScrollY()<=top){binding.scroll.smoothScrollTo(0,top);}
                        break;
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        handler = new Handler(getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch(msg.what){
                    case -1:
                        Toast.makeText(CalendarActivity.this,"请检查网络",Toast.LENGTH_LONG).show();
                        break;
                    case 1:
                        Matcher matcher = Pattern.compile("(<strong>.+?)(?=<strong>)").matcher(msg.obj +"<strong>");
                        while(matcher.find()){
                            Matcher m = Pattern.compile("<strong>(.+?)<").matcher(Objects.requireNonNull(matcher.group(1)));
                            if(m.find()){binding.tabs.addTab(binding.tabs.newTab().setText(m.group(1)));}
                            Matcher n = Pattern.compile("src=\"(.+?)\"").matcher(Objects.requireNonNull(matcher.group(1)));
                            while(n.find()){
                                ImageView image = new ImageView(CalendarActivity.this);
                                Glide.with(CalendarActivity.this).load("https://jwb.sysu.edu.cn/"+n.group(1)).skipMemoryCache(false).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(image);
                                binding.content.addView(image);
                            }
                            binding.progressBar.setVisibility(View.GONE);
                        }
                        break;
                }
            }
        };
        new OkHttpClient.Builder().build().newCall(new Request.Builder().url("https://jwb.sysu.edu.cn/school-calendar").build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = new Message();
                msg.what=-1;
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what=1;
                Matcher matcher1 = Pattern.compile("block-region-left.+?>([\\s\\S]+?)<.+?block-region-left-below").matcher(response.body().string());//
                // .usePattern()
                if(matcher1.find()){
                    msg.obj= Pattern.compile("</?(?!img|strong).+?>|\\s+").matcher(Objects.requireNonNull(matcher1.group(1))).replaceAll("");
                    handler.sendMessage(msg);
                }
            }
        });
    }
}