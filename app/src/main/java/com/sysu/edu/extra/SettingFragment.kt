package com.sysu.edu.extra;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

import com.sysu.edu.R;
import com.sysu.edu.browser.BrowserPreference;
import com.sysu.edu.preference.MenuPreference;

import java.util.Objects;

import rikka.material.preference.MaterialSwitchPreference;

public class SettingFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);
        ((MenuPreference) Objects.requireNonNull(findPreference("theme"))).setOnPreferenceChangeListener((_, _) -> {
            requireActivity().recreate();
            return true;
        });
        ((MenuPreference) Objects.requireNonNull(findPreference("icon_theme"))).setOnPreferenceChangeListener((_, newValue) -> {
            PackageManager pm = requireActivity().getPackageManager();
            String pkg = requireContext().getPackageName();
            pm.setComponentEnabledSetting(new ComponentName(requireActivity().getBaseContext(), pkg + ".MainActivity"), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            pm.setComponentEnabledSetting(new ComponentName(requireActivity().getBaseContext(), pkg + ".MainActivityLight"), (new int[]{1, 2, 2})[Integer.parseInt((String) newValue)], PackageManager.DONT_KILL_APP);
            pm.setComponentEnabledSetting(new ComponentName(requireActivity().getBaseContext(), pkg + ".MainActivityDark"), (new int[]{2, 1, 2})[Integer.parseInt((String) newValue)], PackageManager.DONT_KILL_APP);
            pm.setComponentEnabledSetting(new ComponentName(requireActivity().getBaseContext(), pkg + ".MainActivityDefault"), (new int[]{2, 2, 1})[Integer.parseInt((String) newValue)], PackageManager.DONT_KILL_APP);
            return true;
        });
        ((MenuPreference) Objects.requireNonNull(findPreference("language"))).setOnPreferenceChangeListener((_, _) -> {
                    requireActivity().recreate();
                    return true;
                }
        );
        ((MenuPreference) Objects.requireNonNull(findPreference("fontSize"))).setOnPreferenceChangeListener((_, _) -> {
                    requireActivity().recreate();
                    return true;
                }
        );
        BrowserPreference browserPreference = new BrowserPreference(requireContext());
        ((MaterialSwitchPreference) Objects.requireNonNull(findPreference("image_blocked"))).setChecked(browserPreference.isImageBlocked());
        ((MaterialSwitchPreference) Objects.requireNonNull(findPreference("image_blocked"))).setOnPreferenceChangeListener((_, newValue) -> {
            browserPreference.setImageBlocked((boolean) newValue);
            return true;
        });
        ((MaterialSwitchPreference) Objects.requireNonNull(findPreference("js_enabled"))).setChecked(browserPreference.isJSEnabled());
        ((MaterialSwitchPreference) Objects.requireNonNull(findPreference("js_enabled"))).setOnPreferenceChangeListener((_, newValue) -> {
            browserPreference.setJSEnabled((boolean) newValue);
            return true;
        });
        ((MaterialSwitchPreference) Objects.requireNonNull(findPreference("save_mobile_data_mode"))).setChecked(browserPreference.isSaveMobileDataMode());
        ((MaterialSwitchPreference) Objects.requireNonNull(findPreference("save_mobile_data_mode"))).setOnPreferenceChangeListener((_, newValue) -> {
            browserPreference.setSaveMobileDataMode((boolean) newValue);
            return true;
        });
        ((MaterialSwitchPreference) Objects.requireNonNull(findPreference("privacy_mode"))).setChecked(browserPreference.isPrivacyMode());
        ((MaterialSwitchPreference) Objects.requireNonNull(findPreference("privacy_mode"))).setOnPreferenceChangeListener((_, newValue) -> {
            browserPreference.setPrivacyMode((boolean) newValue);
            return true;
        });
        ((MaterialSwitchPreference) Objects.requireNonNull(findPreference("mobile_mode"))).setChecked(browserPreference.isSaveMobileDataMode());
        ((MaterialSwitchPreference) Objects.requireNonNull(findPreference("mobile_mode"))).setOnPreferenceChangeListener((_, newValue) -> {
            browserPreference.setSaveMobileDataMode((boolean) newValue);
            return true;
        });
//        ((MaterialSwitchPreference) Objects.requireNonNull(findPreference("third_party_cookie_accept"))).setChecked(browserPreference.isThirdPartyCookieAccept());
//        ((MaterialSwitchPreference) Objects.requireNonNull(findPreference("cookie_accept"))).setOnPreferenceChangeListener((_, newValue) -> {
//            browserPreference.setCookieAccept((boolean) newValue);
//            return true;
//        });
    }
}