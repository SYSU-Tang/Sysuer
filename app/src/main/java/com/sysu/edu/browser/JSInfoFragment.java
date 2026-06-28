package com.sysu.edu.browser;

import static com.sysu.edu.api.CommonUtil.isEmpty;
import static com.sysu.edu.api.CommonUtil.trim;

import android.content.ContentValues;
import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.preference.EditPreference;
import com.sysu.edu.preference.PreferenceUtil;

import java.util.Objects;

import rikka.material.preference.MaterialSwitchPreference;
import rikka.preference.SimpleMenuPreference;

public class JSInfoFragment extends PreferenceFragmentCompat {
    
    
    BrowserHelper db;
    
    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.js_info, rootKey);
    }
    
    void save(ContentValues value, String id) {
        value.clear();
        getData().forEach((k, v) -> {
            if (!isEmpty(v))
                value.put(k, v.toString());
        });
        db.getWritableDatabase().update("js", value, "id = ?", new String[]{id});
    }
    
    public JSONObject getData() {
        PreferenceUtil preferenceUtil = new PreferenceUtil(this);
        preferenceUtil.insertEditValue("title", "title");
        preferenceUtil.insertEditValue("description", "description");
        preferenceUtil.insertEditValue("author", "author");
        preferenceUtil.insert("matches", JSONArray.from(((EditPreference) Objects.requireNonNull(findPreference("matches"))).getValue().split(",")).toString());
        preferenceUtil.insertMenuValue("run", "run");
        preferenceUtil.insertSwitchValue("state", "state", 0, 1);
        return preferenceUtil.getParams();
    }
    
    public void setData(JSONObject info) {
        ((EditPreference) Objects.requireNonNull(findPreference("title"))).setValue(info.getString("title"));
        ((EditPreference) Objects.requireNonNull(findPreference("description"))).setValue(info.getString("description"));
        ((EditPreference) Objects.requireNonNull(findPreference("author"))).setValue(info.getString("author"));
        ((EditPreference) Objects.requireNonNull(findPreference("matches"))).setValue(String.join(",", JSONArray.parseArray(trim(info.getString("matches"))).toList(String.class)));
        ((MaterialSwitchPreference) Objects.requireNonNull(findPreference("state"))).setChecked(info.getInteger("state") == 0);
        ((SimpleMenuPreference) Objects.requireNonNull(findPreference("run"))).setValue(info.getString("run"));
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        db = new BrowserHelper(requireContext());
        NavController navController = Navigation.findNavController(view);
        var data = JSONObject.parseObject(requireArguments().getString("item"));
        var value = new ContentValues();
        if (data != null) setData(data);
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (data != null) save(value, data.getString("id"));
                navController.navigateUp();
            }
        });
        ((Preference) Objects.requireNonNull(findPreference("edit"))).setOnPreferenceClickListener(_ -> {
            if (data != null) {
                Bundle bundle = new Bundle();
                bundle.putString("item", data.toString());
                navController.navigate(R.id.info_to_editor, bundle);
            }
            return false;
        });
        ((Preference) Objects.requireNonNull(findPreference("save"))).setOnPreferenceClickListener(_ -> {
            if (data != null) save(value, data.getString("id"));
            return false;
        });
        ((Preference) Objects.requireNonNull(findPreference("delete"))).setOnPreferenceClickListener(_ -> {
            if (data != null) {
                db.getWritableDatabase().delete("js", "id = ?", new String[]{data.getString("id")});
                navController.navigateUp();
            }
            return false;
        });
        /*
        if (data != null) {
            Preference state = Objects.requireNonNull(findPreference("state"));
            state.setTitle(data.getInteger("state") == 1 ? getString(R.string.enable) : getString(R.string.disable));
            state.setOnPreferenceClickListener(_ -> {
                int i = 1 - data.getInteger("state");
                data.put("state", i);
                state.setTitle(i == 1 ? getString(R.string.enable) : getString(R.string.disable));
                save(value, data.getString("id"));
                return false;
            });
        }*/
        super.onViewCreated(view, savedInstanceState);
    }
}
