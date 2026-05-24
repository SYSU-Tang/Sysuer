package com.sysu.edu.academic;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayoutMediator;
import com.sysu.edu.R;
import com.sysu.edu.browser.BrowserActivity;
import com.sysu.edu.databinding.ActivityPagerBinding;
import com.sysu.edu.model.JwxtModel;
import com.sysu.edu.view.AdapterListener;
import com.sysu.edu.view.Pager2Adapter;

import java.util.stream.IntStream;

public class AcademyNotification extends AppCompatActivity {
    JwxtModel model;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityPagerBinding binding = ActivityPagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setTitle(R.string.academic_affair_notice);
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        model = new JwxtModel(this);
        Pager2Adapter pager2Adapter = new Pager2Adapter(this);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this).setMessage("").setPositiveButton(R.string.confirm, (_, _) -> {
        }).create();
        AdapterListener listener = new AdapterListener() {
            @Override
            public void onBind(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, RecyclerView.ViewHolder holder, int position) {
                holder.itemView.setOnClickListener(_ -> {
                    JSONObject item = ((NewsFragment.NewsAdapter) adapter).get(position);
                    dialog.setTitle(item.getString("title"));
                    getContent(item.getString("id"));
                });
            }
            
            @Override
            public void onCreate(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, ViewBinding binding) {
            }
        };
        IntStream.range(0, 2).forEach(_ -> {
            NewsFragment fragment = new NewsFragment();
            fragment.setListener(listener);
            pager2Adapter.add(fragment);
        });
        binding.pager.setAdapter(pager2Adapter);
        new TabLayoutMediator(binding.tabs, binding.pager, (tab, position) -> tab.setText(new int[]{R.string.academic_affair_notice, R.string.school_affair_notice}[position])).attach();
        model.getMessage().observe(this, message -> {
            JSONObject response = message.getSecond();
            if (response.getInteger("code").equals(200)) {
                switch (message.getFirst()) {
                    case 0, 1 ->
                            response.getJSONObject("data").getJSONArray("list").forEach(a -> ((NewsFragment) pager2Adapter.get(message.getFirst())).add((JSONObject) a));
                    case 2 ->
                            startActivity(new Intent(this, BrowserActivity.class).putExtra("data", ("""
                                            <!DOCTYPE html><html><head><style>
                                            body{
                                            padding: 24px !important;
                                            }
                                            a,body,p,span{
                                            font-size: 2.5rem !important;
                                            line-height: 2.0 !important;
                                             }
                                             table{
                                            table-layout: auto !important;
                                            width: 100% !important;
                                             }
                                             table,th, td
                                                    {
                                            font-size: 1.0rem !important;
                                            line-height: 1.0 !important;
                                                    border-collapse: collapse !important;
                                                    border: 2px solid windowtext !important;
                                                    }
                                            </style></head><body>
                                            """ + response.getString("data") + "</body></html>").trim()),
                                    ActivityOptionsCompat.makeSceneTransitionAnimation(this, binding.toolbar, "miniapp").toBundle());
                }
                model.nextAll();
            }
        });
        getSchoolNotices();
        getNotices();
        model.next();
    }
    
    void getList(String column, int what) {
        model.add("jwxt/system-manage/info-delivery?column=" + column + "&deliveryObject=02&status=1&resourceCode=jwgld", what);
    }
    
    void getNotices() {
        getList("01", 0);
    }
    
    void getSchoolNotices() {
        getList("02", 1);
    }
    
    void getContent(String id) {
        model.addAndNext("jwxt/system-manage/info-delivery/noticeId?id=" + id, 2);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        model.dispose();
    }
}