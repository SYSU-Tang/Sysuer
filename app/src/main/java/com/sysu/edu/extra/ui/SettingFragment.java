package com.sysu.edu.extra.ui;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

import com.sysu.edu.R;
import com.sysu.edu.preference.Preference;
import com.sysu.edu.preference.Theme;

import java.util.Objects;

public class SettingFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.setting, rootKey);
        Theme th = new Theme(requireActivity());
        th.setTheme();
        ((Preference) Objects.requireNonNull(findPreference("theme"))).setOnPreferenceChangeListener((preference, newValue) -> {
            // initTheme((String)((DropDownPreference) Objects.requireNonNull(findPreference("theme"))).getValue());
           // requireActivity().setResult(Activity.RESULT_OK);
            requireActivity().recreate();
            return true;
        });
        ((Preference) Objects.requireNonNull(findPreference("language"))).setOnPreferenceChangeListener((preference, v) -> {
                    //Language.setLanguage(requireActivity());
                    requireActivity().recreate();
                    return true;
                }
        );
    }
}