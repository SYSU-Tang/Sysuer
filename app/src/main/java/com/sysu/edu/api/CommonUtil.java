package com.sysu.edu.api;

import android.content.Context;

import androidx.annotation.NonNull;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import okhttp3.HttpUrl;

/**
 * 通用工具类
 */
public class CommonUtil {
    
    /**
     * 从 JSONObject 中提取指定键的值
     *
     * @param data JSONObject 数据
     * @param keys 要提取的键数组
     * @return 包含提取值的 ArrayList
     *
     */
    public static ArrayList<String> extractValue(JSONObject data, String[] keys) {
        ArrayList<String> values = new ArrayList<>();
        for (String i : keys) values.add(data.getString(i));
        return values;
    }
    
    /**
     * 从 JSONObject 中提取指定键的值
     *
     * @param data JSONObject 数据
     * @param keys 要提取的键列表
     * @return 包含提取值的 ArrayList
     */
    public static ArrayList<String> extractValue(JSONObject data, List<String> keys) {
        ArrayList<String> values = new ArrayList<>();
        for (String i : keys) values.add(data.getString(i));
        return values;
    }
    
    /**
     * 将boolean值转换为字符串"1"或"0"
     *
     * @param b 要转换的 boolean 值
     * @return 转换后的字符串"1"或"0"
     *
     */
    public static String bool2str(boolean b) {
        return b ? "1" : "0";
    }
    
    /**
     * 检查字符串是否为空或仅包含空格
     *
     * @param str 要检查的字符串
     * @return 如果字符串为空或仅包含空格，则返回true；否则返回false
     *
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * 检查对象是否为空或仅包含空格
     *
     * @param str 要检查的对象
     * @return 如果对象为空或仅包含空格，则返回true；否则返回false
     *
     */
    public static <T> boolean isEmpty(T str) {
        return str == null || str.toString().trim().isEmpty();
    }
    
    
    /**
     * 对字符串进行trim操作，若字符串为空则返回空字符串
     *
     * @param str 要进行修剪操作的字符串
     * @return trim后的字符串，若原字符串为空则返回空字符串
     *
     */
    public static String trim(String str) {
        return str == null ? "" : str.trim();
    }
    
    /**
     * 从资源 ID 列表中获取对应的字符串数组
     *
     * @param context  上下文对象
     * @param resource 资源 ID 数组
     * @return 包含对应字符串的数组
     *
     */
    public static String[] getString(Context context, int[] resource) {
        return Arrays.stream(resource).mapToObj(context::getString).toArray(String[]::new);
    }
    
    /**
     * 从资源 ID 列表中获取对应的字符串数组
     *
     * @param context  上下文对象
     * @param resource 资源 ID 列表
     * @return 包含对应字符串的数组
     *
     */
    public static String[] getString(Context context, List<Integer> resource) {
        return resource.stream().mapToInt(Integer::intValue).mapToObj(context::getString).toArray(String[]::new);
    }
    
    /**
     * 从 JSONArray 中提取指定键的值
     *
     * @param array    JSONArray 数据
     * @param nameKey  要提取的键名
     * @param valueKey 要提取的值键名
     * @return 包含提取值的 Tuple2 对象，其中第一个元素为名称数组，第二个元素为值数组
     *
     */
    public static Tuple2<ArrayList<String>, ArrayList<String>> extractValue(JSONArray array, String nameKey, String valueKey) {
        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> values = new ArrayList<>();
        array.forEach(i -> {
            JSONObject item = (JSONObject) i;
            names.add(item.getString(nameKey));
            values.add(item.getString(valueKey));
        });
        return new Tuple2<>(names, values);
    }
    
    /**
     * 从 JSONArray 中提取指定键的值
     *
     * @param array   JSONArray
     * @param nameKey 要提取的键名
     * @return 包含提取值的 ArrayList
     *
     */
    public static ArrayList<String> extractValue(JSONArray array, String nameKey) {
        ArrayList<String> names = new ArrayList<>();
        array.forEach(i -> names.add(((JSONObject) i).getString(nameKey)));
        return names;
    }
    
    /**
     * 将boolean值转换为整数1或0
     *
     * @param bool 要转换的 boolean 值
     * @return 转换后的整数1或0
     *
     */
    public static int bool2int(boolean bool) {
        return bool ? 1 : 0;
    }
    
    /**
     * 将对象转换为字符串，若对象为空则返回空字符串
     *
     * @param t 要转换的对象
     * @return 转换后的字符串，若对象为空则返回空字符串
     *
     */
    public static <T> String toStringOrDefault(T t) {
        return toStringOrDefault(t, "");
    }
    
    /**
     * 将对象转换为字符串，若对象为空则返回默认值
     *
     * @param t            要转换的对象
     * @param defaultValue 默认值，若对象为空则返回该值
     * @return 转换后的字符串，若对象为空则返回默认值
     *
     */
    public static <T> String toStringOrDefault(T t, String defaultValue) {
        return t == null ? defaultValue : t.toString();
    }
    
    public static Integer toIntegerOrDefault(Integer t, Integer defaultValue) {
        return t == null ? defaultValue : t;
    }
    
    public static Integer toIntegerOrDefault(String t, Integer defaultValue) {
        return isEmpty(t) ? defaultValue : Integer.parseInt(t);
    }
    
    /**
     * 从 URL 中提取主机名
     *
     * @param url 要提取主机名的 URL
     * @return 提取的主机名
     *
     */
    public static String getHost(String url) {
        return HttpUrl.get(url).host();
    }
    
    /**
     * 简单的元组类，用于存储两个值
     *
     * @param <T>  第一个值的类型
     * @param <T1> 第二个值的类型
     */
    public static class Tuple2<T, T1> {
        
        public T first;
        public T1 second;
        
        public Tuple2(T first, T1 second) {
            this.first = first;
            this.second = second;
        }
        
        public Tuple2() {
        }
        
        @NonNull
        @Override
        public String toString() {
            return "(" + first + ", " + second + ")";
        }
        
        public T getFirst() {
            return first;
        }
        
        public void setFirst(T first) {
            this.first = first;
        }
        
        public T1 getSecond() {
            return second;
        }
        
        public void setSecond(T1 second) {
            this.second = second;
        }
        
        @Override
        public boolean equals(Object o) {
            return o instanceof Tuple2<?, ?> tuple2 && Objects.equals(first, tuple2.first) && Objects.equals(second, tuple2.second);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(first, second);
        }
    }
    
}
