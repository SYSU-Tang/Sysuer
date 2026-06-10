package com.sysu.edu.extra;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.sysu.edu.R;

import java.util.Objects;

public class AboutFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.about, rootKey);
        try {
            PackageInfo version = requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0);
            Preference versionPreference = Objects.requireNonNull(findPreference("version"));
            versionPreference.setSummary(String.format("%s(%s)", version.versionName, version.versionCode));
//            versionPreference.setOnPreferenceClickListener(_ -> {
//                ((AboutActivity) requireActivity()).checkUpdate();
//                return false;
//            });
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}