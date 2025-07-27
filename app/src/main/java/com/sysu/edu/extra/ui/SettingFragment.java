package com.sysu.edu.extra.ui;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

import com.sysu.edu.R;
import com.sysu.edu.preference.Preference;
import com.sysu.edu.preference.ThemeHelper;

import java.util.Objects;

public class SettingFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.setting, rootKey);
        ThemeHelper th = new ThemeHelper(requireActivity());
        th.setTheme();
        ((Preference)Objects.requireNonNull(findPreference("theme"))).setOnPreferenceChangeListener((preference, newValue) -> {
           // initTheme((String)((DropDownPreference) Objects.requireNonNull(findPreference("theme"))).getValue());
            requireActivity().recreate();
            return true;
        });
    }
}