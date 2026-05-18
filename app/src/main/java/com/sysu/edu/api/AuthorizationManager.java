package com.sysu.edu.api;

import java.util.regex.Pattern;
/**
 * 认证管理器
 * 用于处理认证相关的操作，包括判断是否认证和是否可访问
 */
public class AuthorizationManager {

    private final String origin; // 原始 URL
    private final String substitute; // 替代 URL
    boolean isAuthorized = true; // 是否认证
    boolean isAccessible = true; // 是否可访问

    /**
     * 构造函数
     * @param origin 原始 URL
     * @param substitute 替代 URL
     */
    public AuthorizationManager(String origin, String substitute) {
        this.origin = origin;
        this.substitute = substitute;
    }

    /**
     * 获取根 URL
     * @return 根 URL，根据是否可访问返回原始 URL 或替代 URL
     */
    public String getBaseUrl() {
        return isAccessible ? origin : substitute;
    }

    /**
     * 判断内容是否可访问
     * @param content 要判断的内容
     * @return 如果内容中不包含"Access Forbidden"，则返回true；否则返回false
     */
    public boolean isAccessible(String content) {
        boolean isInaccessible = Pattern.compile("Access Forbidden").matcher(content).find();
        if(isInaccessible) isAccessible = false;
        return !isInaccessible;
    }

    /**
     * 判断是否可访问
     * @return 如果可访问，则返回true；否则返回false
     */
    public boolean isAccessible() {
        return isAccessible;
    }

    /**
     * 设置是否可访问
     * @param accessible 如果为true，则表示可访问；否则表示不可访问
     */
    public void setAccessible(boolean accessible) {
        isAccessible = accessible;
    }

    /*public boolean isAuthorized() {
        return isAuthorized;
    }*/

    /**
     * 判断内容是否认证
     * @param content 要判断的内容
     * @return 如果内容中不包含"中山大学统一身份认证"，则返回true；否则返回false
     */
    public boolean isAuthorized(String content) {
        boolean isContentUnauthorized = Pattern.compile("中山大学统一身份认证").matcher(content).find();
        if(isContentUnauthorized) isAuthorized = false;
        return !isContentUnauthorized;
    }
}
