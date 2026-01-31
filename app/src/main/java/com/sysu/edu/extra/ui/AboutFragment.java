package com.sysu.edu.extra.ui;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.sysu.edu.R;
import com.sysu.edu.extra.AboutActivity;

import java.util.Objects;

import rikka.preference.SimpleMenuPreference;

public class AboutFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.about, rootKey);
        try {
            PackageInfo version = requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0);
            ((Preference) Objects.requireNonNull(findPreference("version"))).setSummary(String.format("%s(%s)", version.versionName, version.versionCode));
            ((Preference) Objects.requireNonNull(findPreference("version"))).setOnPreferenceClickListener(preference -> {
                ((AboutActivity) requireActivity()).checkUpdate();
                return false;
            });
            ((SimpleMenuPreference) Objects.requireNonNull(findPreference("sponsor"))).setOnPreferenceChangeListener((a, b) -> {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("alipays://platformapi/startapp?saId=10000007&amp;clientVersion=3.7.0.0718&amp;qrcode=https%3A%2F%2Fqr.alipay.com%2Ftsx11036pqmv0tkfj6lq8c5%3F_s%3Dweb-other")));
                return false;
            });
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}