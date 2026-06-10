package com.sysu.edu.browser;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

public class JavaScript {
    private JSONArray jsList = new JSONArray();
    
    public JavaScript(String jsList) {
        this.jsList = JSONArray.parse(jsList);
    }
    
    public JavaScript() {
    }
    
    public void add(String title, String description, String[] matches, String script) {
        jsList.add(JSONObject.parse(String.format("{\"title\": \"%s\",\"description\": \"%s\",\"matches\": %s,\"script\": \"%s\"}", title, description, Arrays.toString(matches), script)));
    }
    
    public void add(JSONObject item) {
        jsList.add(item);
    }
    
    public ArrayList<JSONObject> searchJS(String key) {
        ArrayList<JSONObject> list = new ArrayList<>();
        jsList.forEach(a -> {
            JSONObject item = (JSONObject) a;
            if (item.containsKey("state") && item.getInteger("state") == 1) {
                for (Object e : item.getJSONArray("matches")) {
                    if (Pattern.compile((String) e).matcher(key).find()) {
                        list.add(item);
                        break;
                    }
                }
            }
        });
        return list;
    }
    
    public ArrayList<JSONObject> searchJS(String key, boolean isActive) {
        ArrayList<JSONObject> list = new ArrayList<>();
        jsList.forEach(a -> {
            JSONObject item = (JSONObject) a;
            if (item.containsKey("state") && item.getInteger("state") == 1 && isActive && item.containsKey("run") && item.getInteger("run") == 1) {
                for (Object e : item.getJSONArray("matches")) {
                    if (Pattern.compile((String) e).matcher(key).find()) {
                        list.add(item);
                        break;
                    }
                }
            }
        });
        return list;
    }
    
    public void clear() {
        jsList.clear();
    }
}
