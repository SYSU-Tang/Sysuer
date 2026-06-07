package com.sysu.edu.academic;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.google.android.material.tabs.TabLayoutMediator;
import com.sysu.edu.R;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.ActivityPagerBinding;
import com.sysu.edu.view.AdapterListener;
import com.sysu.edu.view.Pager2Adapter;
import com.sysu.edu.view.StaggeredFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhysicalFitnessTestResultActivity extends AppCompatActivity {

    Params params;
    HttpManager http;
    int position;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityPagerBinding binding = ActivityPagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setTitle(R.string.physical_fitness_test_result);
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        params = new Params(this);
        params.setCallback(this::getResult);
        Pager2Adapter adp = new Pager2Adapter(this);
        StaggeredFragment page1 = new StaggeredFragment();
        StaggeredFragment page2 = new StaggeredFragment();
        StaggeredFragment page3 = new StaggeredFragment();
        adp.add(page1).add(page2).add(page3);
        binding.pager.setAdapter(adp);
        new TabLayoutMediator(binding.tabs, binding.pager, (tab, position) -> tab.setText(List.of("体测成绩", "体育积分", "游泳").get(position))).attach();
        Handler handler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                int num = msg.what;
                if (num == -1) params.toast(R.string.no_net_connected);
                else {
                    if (msg.getData().getInt("code") == 302 || Pattern.compile("window\\.location\\.href.+?\"/caslogin\"", Pattern.DOTALL).matcher((String) msg.obj).find()) {
                        params.toast(R.string.login_warning);
                        params.gotoLogin(TargetUrl.TICE);
                    } else switch (num) {
                        case 0, 1, 2 -> {
                            ArrayList<String> urls = new ArrayList<>();
                            Matcher matcher = Pattern.compile("<a class=\"weui-cell weui-cell_access\".+?</a>", Pattern.DOTALL).matcher((String) msg.obj);
                            StaggeredFragment page = (StaggeredFragment) adp.get(num);
                            while (matcher.find()) {
                                Matcher matcher1 = Pattern.compile("<div class=\"weui-cell__bd\".*?<p.*?>(.+?)</p>.*?<div class=\"weui-cell__ft.*?\">.+?<span.+?>(.+?)(&nbsp;)?</span>", Pattern.DOTALL).matcher(matcher.group());
                                if (matcher1.find())
                                    page.add(matcher1.group(1), R.drawable.calendar, List.of(new String[]{"总成绩", "总积分", "是否达标"}[msg.what]), List.of(Objects.requireNonNull(matcher1.group(2)).trim()));
                                Matcher matcher2 = Pattern.compile("<a class=\"weui-cell weui-cell_access\" href=\"(.+?)\">", Pattern.DOTALL).matcher(matcher.group());
                                if (matcher2.find()) urls.add(matcher2.group(1));
                            }
                            page.setListener(new AdapterListener() {
                                @Override
                                public void onBind(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, RecyclerView.ViewHolder holder, int position) {
                                    page.getStaggeredAdapter().getTwoColumnsAdapter(position).setListener(new AdapterListener() {
                                        @Override
                                        public void onBind(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, RecyclerView.ViewHolder holder, int p) {
                                            holder.itemView.setOnClickListener(_ -> {
                                                if (adapter.getItemCount() == 1 && num != 2) {
                                                    if (num == 0) getDetail(urls.get(position));
                                                    if (num == 1)
                                                        getCreditDetail(urls.get(position));
                                                    PhysicalFitnessTestResultActivity.this.position = position;
                                                }
                                            });
                                        }

                                        @Override
                                        public void onCreate(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, ViewBinding binding) {

                                        }
                                    });
                                }

                                @Override
                                public void onCreate(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, ViewBinding binding) {

                                }
                            });
                            if (msg.what == 0) getCredit();
                            if (msg.what == 1) getSwim();
                        }
                        case 3 -> {
                            Matcher matcher = Pattern.compile("<a class=\"weui-cell\">.*?<div class=\"weui-cell__bd\">(.+?)</div>.*?<div class=\"weui-cell__ft\">(.+?)</div>.*?</a>", Pattern.DOTALL).matcher((String) msg.obj);
                            while (matcher.find())
                                page1.getStaggeredAdapter().addRow(position, Objects.requireNonNull(matcher.group(1)).replace("</span>", "~").replaceAll("<.+?>", "").replaceAll("\\s", "").trim(), Objects.requireNonNull(matcher.group(2)).replace("</span>", "~").replaceAll("<.+?>", "").replace("%s", "").trim());
                        }
                        case 4 -> {
                            Matcher matcher = Pattern.compile("<a class=\"weui-cell\">.+?</a>", Pattern.DOTALL).matcher((String) msg.obj);
                            while (matcher.find()) {
                                Matcher matcher1 = Pattern.compile("class=\"left_side\">(.+?)</div>.+?<p>(.+?)</p></div>", Pattern.DOTALL).matcher(matcher.group());
                                Matcher creditMatcher = Pattern.compile("class=\"ticeImg\">(.+?)<", Pattern.DOTALL).matcher(matcher.group());
                                if (creditMatcher.find())
                                    page2.getStaggeredAdapter().addRow(position, "积分", Objects.requireNonNull(creditMatcher.group(1)).trim());
                                while (matcher1.find())
                                    page2.getStaggeredAdapter().addRow(position, matcher1.group(1), matcher1.group(2));
                            }
                        }
                    }
                }
            }
        };
        http = new HttpManager(handler);
        http.setParams(params);
        http.setUA("Mozilla/5.0 (Linux; Android 15.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Mobile Safari/537.36");
        getResult();
    }

    void getDetail(String url) {
        http.getRequest("https://tice.sysu.edu.cn" + url, 3);
    }

    void getCreditDetail(String url) {
        http.getRequest("https://tice.sysu.edu.cn" + url, 4);
    }

    void getResult() {
        http.getRequest("https://tice.sysu.edu.cn/m/tice", 0);
    }

    void getCredit() {
        http.getRequest("https://tice.sysu.edu.cn/m/kwjfList", 1);
    }

    void getSwim() {
        http.getRequest("https://tice.sysu.edu.cn/m/tice/studentSwim", 2);
    }
}