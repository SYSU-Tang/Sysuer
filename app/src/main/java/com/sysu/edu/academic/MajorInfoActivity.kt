package com.sysu.edu.academic;

import android.os.Bundle;
import android.view.MenuItem;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.tabs.TabLayoutMediator;
import com.sysu.edu.BaseActivity;
import com.sysu.edu.R;
import com.sysu.edu.databinding.ActivityPagerBinding;
import com.sysu.edu.model.JwxtModel;
import com.sysu.edu.view.Pager2Adapter;
import com.sysu.edu.view.StaggeredFragment;

import java.util.ArrayList;
import java.util.Objects;

public class MajorInfoActivity extends BaseActivity {
    JwxtModel model;
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        model.dispose();
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityPagerBinding binding = ActivityPagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        model = new JwxtModel(this);
        ArrayList<String> categories = new ArrayList<>();
        binding.toolbar.setTitle(R.string.major_info);
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        Pager2Adapter pager2Adapter = new Pager2Adapter(this);
        binding.pager.setAdapter(pager2Adapter);
        binding.toolbar.getMenu().add(R.string.export).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM).setIcon(R.drawable.export).setOnMenuItemClickListener(_ -> {
            int currentItem = binding.pager.getCurrentItem();
            ((StaggeredFragment) pager2Adapter.get(currentItem)).export(binding.toolbar, Objects.requireNonNull(Objects.requireNonNull(binding.tabs.getTabAt(currentItem)).getText()).toString());
            return true;
        });
        new TabLayoutMediator(binding.tabs, binding.pager, (tab, position) -> tab.setText(categories.get(position))).attach();
        model.getMessage().observe(this, message -> {
            JSONObject response = message.second;
            if (response != null && response.getInteger("code").equals(200)) {
                if (response.get("data") != null) {
                    if (message.first == 0) {
                        categories.clear();
                        response.getJSONArray("data").forEach(a -> {
                            categories.add(((JSONObject) a).getString("dataName"));
                            Bundle args = new Bundle();
                            args.putString("code", ((JSONObject) a).getString("dataNumber"));
                            pager2Adapter.add(MajorInfoFragment.newInstance(args));
                        });
                    }
                }
            }
        });
        getCategory();
    }
    
    void getCategory() {
        model.addAndNext("jwxt/base-info/codedata/findcodedataNames?datableNumber=135", 0);
    }
}