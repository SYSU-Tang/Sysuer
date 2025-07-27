package com.sysu.edu.extra.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PrivacyFragment extends PreferenceFragmentCompat {
    OkHttpClient http = new OkHttpClient.Builder().build();
    boolean init = false;
    Handler handler;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        if(!init) {
            setPreferencesFromResource(R.xml.privacy, rootKey);
            getInfo();
            handler = new Handler(Looper.getMainLooper()){
                @Override
                public void handleMessage(@NonNull Message msg) {
                    //msg.what
                    System.out.println((String) msg.obj);
                    JSONObject info = JSONArray.parse((String) msg.obj).getJSONObject(0);
                     String[] keys = new String[]{"UserId", "HostKey", "Name"};
                    for(int i=0;i< keys.length;i++){
                        Preference name = getPreferenceManager().findPreference((new String[]{"netid","account","name"})[i]);
                        if (name != null) {
                            name.setOnPreferenceClickListener(preference -> {
                                ClipboardManager clipboardManager = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                                clipboardManager.setPrimaryClip(ClipData.newPlainText(name.getTitle(),name.getSummary()));
                                Toast.makeText(requireContext(),"已复制",Toast.LENGTH_LONG).show();
                                return true;
                            });
                            name.setSummary(info.getString(keys[i]));
                        }
                    }
                }
            };
        }
    }
    void getInfo(){
        http.newCall(new Request.Builder()
                .url("https://gym-443.webvpn.sysu.edu.cn/api/swimmer/me")
                .header("User-Agent","SYSU")
                .header("Authorization","Bearer eyJhbGciOiJSUzI1NiIsImtpZCI6IjI1QjExQTk3MDQ0Qjc5MUVGN0I2MDAzOTdDMzk2MDJDQzA1RjY5NTYiLCJ4NXQiOiJKYkVhbHdSTGVSNzN0Z0E1ZkRsZ0xNQmZhVlkiLCJ0eXAiOiJKV1QifQ.eyJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA1LzA1L2lkZW50aXR5L2NsYWltcy9uYW1lIjoidGFuZ3hiNiIsImh0dHA6Ly9zY2hlbWFzLnhtbHNvYXAub3JnL3dzLzIwMDUvMDUvaWRlbnRpdHkvY2xhaW1zL25hbWVpZGVudGlmaWVyIjoidGFuZ3hiNiIsImh0dHA6Ly9zY2llbnRpYS5jb20vY2xhaW1zL0luc3RpdHV0aW9uIjoiNGZiMzdlMjgtMGE5MC00YmIwLWIwZTAtYjc5OGVjZTZmMzIwIiwiaHR0cDovL3NjaWVudGlhLmNvbS9jbGFpbXMvSW5zdGl0dXRpb25OYW1lIjoibWFpbC5zeXN1IiwiaHR0cDovL3NjaGVtYXMueG1sc29hcC5vcmcvd3MvMjAwNS8wNS9pZGVudGl0eS9jbGFpbXMvZW1haWxhZGRyZXNzIjoidGFuZ3hiNkBtYWlsLnN5c3UuZWR1LmNuIiwiaHR0cDovL3NjaGVtYXMueG1sc29hcC5vcmcvd3MvMjAwNS8wNS9pZGVudGl0eS9jbGFpbXMvZ2l2ZW5uYW1lIjoidGFuZ3hiNiIsImh0dHA6Ly9zY2llbnRpYS5jb20vY2xhaW1zL1Njb3BlIjoiUkIiLCJuYmYiOjE3NTM0MzA5OTQsImV4cCI6MTc1MzQzNDU5NCwiaXNzIjoiaHR0cHM6Ly9sb2NhbGhvc3QvIiwiYXVkIjoiaHR0cDovL3Jlc291cmNlYm9va2VyYXBpLmNsb3VkYXBwLm5ldC8ifQ.cilrLUoi6BHj_Hyfy9HuqglivAJ_9VVpDtsOAyZMC_SqWjnJmbvf5ex6RZf_UgmR1F37rKq58XeacRkHUUUQ-lOrrHdcfEvQFQd7V9wizeMpstTFS5WPcaDj86e6c_8trk5VyycVFOFrvW2Cu0PyDAWTwomDXFV4Na7A7AUHWeijkD0Ne6WG4FTiYDG49RHf18RPQGD8JmeTvihplMM1t7arrfYz4j-ni6gTlA80lAIQxvAPs80-q1ZDvVN9pFuaN-C2alIjFxM39iYqp5-x6-DSsfXbteiK_V5AnyyzJ4vfXF-cDHumk3nLbGk_URte38CMRiEViCRXhov3-0uUPg")
                .header("Accept","application/json, text/plain, */*")
                .header("Cookie","token_ec583190dcd12bca757dd13df10f59c3=594c188bae947989025e139002f8c374; sn_ec583190dcd12bca757dd13df10f59c3=360b7022ac411b79e43c01affcea5064; _webvpn_key=eyJhbGciOiJIUzI1NiJ9.eyJ1c2VyIjoidGFuZ3hiNiIsImdyb3VwcyI6WzNdLCJpYXQiOjE3NTM0MjYzMTYsImV4cCI6MTc1MzUxMjcxNn0.zT8EaI1FrbNxXn1OTcNzSBTZyMLerMxuob4z6Xe7XeU; safeline_bot_token=AH3ZruYAAAAAAAAAAAAAAACvo8BAmAEAAPBi/BnXHjmgWCy/zB+R2q4sshRh")
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.obj=response.body().string();
                handler.sendMessage(msg);
                //System.out.println();
            }
        });
    }
}