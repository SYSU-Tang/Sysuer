package com.sysu.edu.extra;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

public class JavaScript {
    public HashMap<String, String> jsList;
    public JavaScript(String s){
        jsList = JSONObject.parseObject(s, new TypeReference<>() {
        });
    }
    public void add(String url, String js){
        jsList.put(url,js);
    }
    public ArrayList<String> searchJS(String key){
        ArrayList<String> list = new ArrayList<>();
        jsList.forEach((a,b)->{
            Pattern pattern = Pattern.compile(a);
            if(pattern.matcher(key).find()){
                list.add(b);
            }
        });
        return list;
    }
}
