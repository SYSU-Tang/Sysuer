package com.sysu.edu.extra.ui;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

import com.sysu.edu.R;

public class InfoFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.info, rootKey);
//        for(int i=0;i<getPreferenceScreen().getPreferenceCount();i++){
//            getPreferenceScreen().getPreference(i).setOnPreferenceClickListener(preference ->{ startActivity(new Intent().setAction(Intent.ACTION_VIEW).setData(Uri.parse(Objects.requireNonNull(preference.getSummary()).toString())));return true;});
//        }
        }
}