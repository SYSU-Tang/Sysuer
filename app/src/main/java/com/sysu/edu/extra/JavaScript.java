package com.sysu.edu.extra;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

public class JavaScript {
    private final JSONArray jsList;

    public JavaScript(String jsList) {
        this.jsList = JSONArray.parse(jsList);
    }

    public void add(String title, String description, String[] matches, String script) {
        jsList.add(JSONObject.parse(String.format("{\"title\": \"%s\",\"description\": \"%s\",\"matches\": %s,\"script\": \"%s\"}", title, description, Arrays.toString(matches), script)));
    }

    public ArrayList<JSONObject> searchJS(String key) {
        ArrayList<JSONObject> list = new ArrayList<>();
        jsList.forEach(a -> {
            //System.out.println(a);
            for (Object e : ((JSONObject) a).getJSONArray("matches")) {
                Pattern pattern = Pattern.compile((String) e);
                if (pattern.matcher(key).find()) {
                    list.add((JSONObject) a);
                    break;
                }
            }
        });
        return list;
    }

    public String[] getTitles(ArrayList<JSONObject> json) {
        ArrayList<String> items = new ArrayList<>();
        json.forEach(a -> items.add(a.getString("title")));
        return items.toArray(new String[]{});
    }
}
