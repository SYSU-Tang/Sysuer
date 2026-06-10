package com.sysu.edu.extra;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.ActivityInfoBinding;

import java.util.ArrayList;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityInfoBinding binding = ActivityInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        final ArrayList<Long> click = new ArrayList<>();
        Params params = new Params(this);
        binding.toolbar.setNavigationOnClickListener(_ -> finishAfterTransition());
        binding.icon.setOnClickListener(_ -> {
            if (click.isEmpty() || System.currentTimeMillis() - click.get(click.size() - 1) < 500) {
                if (click.size() == 4) {
                    params.toast(params.isDeveloper() ? R.string.developer_disabled : R.string.developer_enabled);
                    params.setDeveloper(!params.isDeveloper());
                    click.clear();
                } else click.add(System.currentTimeMillis());
            } else click.clear();
        });
    }
}


