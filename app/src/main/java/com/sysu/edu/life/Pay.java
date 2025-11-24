package com.sysu.edu.life;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.tabs.TabLayoutMediator;
import com.sysu.edu.R;
import com.sysu.edu.academic.Pager2Adapter;
import com.sysu.edu.databinding.ActivityPagerBinding;

public class Pay extends AppCompatActivity {

    Pager2Adapter adp;
    ActivityPagerBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setTitle(R.string.pay);
        adp = new Pager2Adapter(this);
        for(int i=0;i<5;i++){
            adp.add(PayFragment.newInstance(i));
        }
        binding.pager.setAdapter(adp);
        //binding.appBarLayout.setElevation(0);

        new TabLayoutMediator(binding.tabs, binding.pager, (tab, position) -> tab.setText(new String[]{"待交费用","选交费用","交费情况","付款记录","退费记录"}[position])).attach();
        binding.toolbar.setNavigationOnClickListener(v->supportFinishAfterTransition());
    }
}