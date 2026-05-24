package com.sysu.edu.extra;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

import com.sysu.edu.R;
import com.sysu.edu.api.ContextUtil;
import com.sysu.edu.databinding.ActivityPrivacyBinding;

public class PrivacyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityPrivacyBinding binding = ActivityPrivacyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        ContextUtil contextUtil = new ContextUtil(this);
        binding.toolbar.getMenu().add(R.string.edit).setIcon(R.drawable.edit).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM).setOnMenuItemClickListener(_ -> {
            contextUtil.changeAccount(null, "sysu.edu.cn",null);
            return false;
        });
    }
}