package com.sysu.edu.ui.account;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.sysu.edu.R;
import com.sysu.edu.extra.SettingActivity;

import java.util.Objects;

public class AccountFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.account, rootKey);
        ActivityResultLauncher<Intent> launch = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
            if (o.getResultCode() == Activity.RESULT_OK) {
                requireActivity().recreate();
            }
        });
        ((Preference)Objects.requireNonNull(findPreference("setting"))).setOnPreferenceClickListener(preference -> {
                    launch.launch(new Intent(requireActivity(), SettingActivity.class),null);
                    return false;
                }
        );
    }
}
