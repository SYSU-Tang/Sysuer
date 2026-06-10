package com.sysu.edu.preference;

import static android.text.TextUtils.isEmpty;

import androidx.preference.PreferenceFragmentCompat;

import com.alibaba.fastjson2.JSONObject;

import rikka.material.preference.MaterialSwitchPreference;
import rikka.preference.SimpleMenuPreference;

public class PreferenceUtil {
    
    private final PreferenceFragmentCompat fragment;
    private final JSONObject params;
    
    public PreferenceUtil(PreferenceFragmentCompat fragment) {
        this.fragment = fragment;
        params = new JSONObject();
    }
    
    public void insertMenuValue(String preferenceKey, String paramsKey) {
        SimpleMenuPreference preference = fragment.findPreference(preferenceKey);
        if (preference != null && !isEmpty(preference.getValue()))
            params.put(paramsKey, preference.getValue());
    }
    
    public void insertEditValue(String preferenceKey, String paramsKey) {
        EditPreference preference = fragment.findPreference(preferenceKey);
        if (preference != null) params.put(paramsKey, preference.getValue());
    }
    
    public void insertSliderValue(String preferenceKey, String paramsKey) {
        SliderPreference preference = fragment.findPreference(preferenceKey);
        if (preference != null && preference.getValue() != 0)
            params.put(paramsKey, preference.getValue());
    }
    
    public void insertFilterValue(String preferenceKey, String paramsKey) {
        FilterPreference preference = fragment.findPreference(preferenceKey);
        if (preference != null) params.put(paramsKey, preference.getValue());
    }
    
    public <T> void insertSwitchValue(String preferenceKey, String paramsKey, T ifChecked, T ifNotChecked) {
        MaterialSwitchPreference preference = fragment.findPreference(preferenceKey);
        if (preference != null)
            params.put(paramsKey, preference.isChecked() ? ifChecked : ifNotChecked);
    }
    
    public void insertSwitchValue(String preferenceKey, String paramsKey) {
        MaterialSwitchPreference preference = fragment.findPreference(preferenceKey);
        if (preference != null)
            params.put(paramsKey, preference.isChecked());
    }
    
    
    public void insert(String key, Object value) {
        params.put(key, value);
    }
    
    public JSONObject getParams() {
        return params;
    }
}
