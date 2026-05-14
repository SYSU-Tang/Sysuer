package com.sysu.edu.api;

import static android.text.TextUtils.isEmpty;

import android.util.Log;

import androidx.annotation.NonNull;

import com.alibaba.fastjson2.JSONObject;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.crypto.Cipher;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginManager {
    
    private static final int TIMEOUT = 30;
    private static CookieManager cookieManager;
    private final CookieStore cookieJar = new CookieStore();
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
            .followRedirects(true)
            .cookieJar(cookieJar)
            .build();
    private final OkHttpClient directClient = new OkHttpClient.Builder().followRedirects(false).cookieJar(cookieJar).build();
    private final AuthorizationManager casAuthorizationManager = new AuthorizationManager("https://cas.sysu.edu.cn", "https://cas.sysu.edu.cn");
    AuthorizationJar authorizationJar;
    LoginListener loginListener;
    boolean isLogin = false;
    
    private String getPublicKey() {
        try {
            return client.newCall(new Request.Builder()
                    .url(casAuthorizationManager.getBaseUrl() + "/esc-sso/api/v3/auth/policy").build()).execute().body().string();
        } catch (IOException _) {
        }
        return "";
    }
    
    private String doLogin(String username, String password, String publicKeyId) {
        try {
            Response response = client.newCall(new Request.Builder()
                    .post(RequestBody.create("{\"authType\":\"webLocalAuth\",\"dataField\":{\"username\":\"" + username + "\",\"password\":\"" + password + "\",\"publicKeyId\":\"" + publicKeyId + "\"}}", MediaType.parse("application/json")))
                    .url("https://cas.sysu.edu.cn/esc-sso/api/v3/auth/doLogin").build()).execute();
            return response.body().string();
        } catch (IOException _) {
            onError("404", "登录失败");
        }
        return "";
    }
    
    
    private void request(String path) {
        String url = path.startsWith("http") ? path : casAuthorizationManager.getBaseUrl() + path;
        try {
            Response response = client.newCall(new Request.Builder().url(url).build()).execute();
            String body = response.body().string();
            System.out.println(response.headers().toMultimap());
            System.out.println(body);
            if (Objects.requireNonNull(response.header("Content-Type", "")).contains("application/json")) {
                String redirect = redirect(body);
                if (redirect == null) return;
                request(redirect);
            }
        } catch (IOException _) {
        }
    }
    
    private String loginForGym(String path) throws IOException {
        String url = path.startsWith("http") ? path : casAuthorizationManager.getBaseUrl() + path;
        Response response = client.newCall(new Request.Builder().header("Accept", "application/json, text/plain, */*")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36")
                .url(url).build()).execute();
        String a = response.body().string();
        if (Objects.requireNonNull(response.header("Content-Type", "")).contains("application/json")) {
            String redirect = redirect(a);
            if (redirect == null) return a;
            return loginForGym(redirect);
        }
        return a;
    }
    
    
    /**
     * 解析重定向 URL
     *
     * @param response 响应 JSON 字符串
     * @return 重定向 URL
     *
     */
    private String redirect(String response) {
        JSONObject json = JSONObject.parse(response);
        if ("0".equals(json.getString("code"))) {
            if (json.containsKey("data")) return json.getJSONObject("data").getString("redirect");
            else return json.getString("redirect");
        } else {
//            System.out.println("Login error: " + json.getString("code") + " " + json);
            onError(json.getString("code"), response);
            return null;
        }
    }
    
    /**
     * 登录，使用 AuthorizationJar 中的用户名和密码登录
     *
     * @param service 登录服务
     * @return 是否登录成功
     *
     */
    public boolean login(String service) {
        try {
            return authorizationJar != null && login(authorizationJar.getUserName(), authorizationJar.getPassword(), service);
        } catch (ExecutionException | InterruptedException e) {
            Log.e("LoginManager", "login: ", e);
            return false;
        }
    }
    
    /**
     * 登录，使用指定的用户名和密码登录
     *
     * @param username 用户名
     * @param password 密码
     * @param service  登录服务
     * @return 是否登录成功
     *
     */
    public boolean login(String username, String password, String service) throws ExecutionException, InterruptedException {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String host = HttpUrl.get(service).host();
                String targetBaseUrl = HttpUrl.get(service).scheme() + "://" + host + "/";
//                cookieJar.initCookieStore(targetBaseUrl);
//                cookieJar.clear(targetBaseUrl);
                cookieJar.add("https://cas.sysu.edu.cn", new Cookie.Builder().name("device_trust_Cookie").value("true").domain("cas.sysu.edu.cn").build());
                if (service.contains("webvpn") || TargetUrl.PORTAL.equals(service)) {
                    casAuthorizationManager.setAccessible(false);
                    JSONObject publicKey = JSONObject.parse(getPublicKey()).getJSONObject("data").getJSONObject("param");
                    String redirect = redirect(doLogin(username, encrypt(publicKey.getString("publicKey"), password), publicKey.getString("publicKeyId")));
                    if (redirect == null) return false;
                    request(redirect + "?service=https%3A%2F%2Fwebvpn.sysu.edu.cn%2Fusers%2Fauth%2Fcas%2Fcallback%3Furl");
                    request("https://webvpn.sysu.edu.cn/vpn_key/update");
                    List<Cookie> webvpnKey = cookieJar.loadForRequest(HttpUrl.get("https://webvpn.sysu.edu.cn/vpn_key/update")).stream().filter(e -> "_webvpn_key".equals(e.name())).collect(Collectors.toList());
                    cookieJar.saveFromResponse(HttpUrl.get(service), webvpnKey);
                    cookieJar.saveFromResponse(HttpUrl.get(casAuthorizationManager.getBaseUrl()), webvpnKey);
                    switch (service) {
                        case TargetUrl.NEWS_WEBVPN ->
                                setAuthorization(host, getNewsAuthorization(service));
                        case TargetUrl.GYM_WEBVPN -> {
                            getGymToken(targetBaseUrl);
                            cookieJar.copy(targetBaseUrl, "https://gym.webvpn.sysu.edu.cn");
                            setAuthorization(host, getGymAuthorization(targetBaseUrl));
                        }
                        case TargetUrl.PORTAL -> {
                            cookieJar.saveFromResponse(HttpUrl.get("https://mportal.sysu.edu.cn"), webvpnKey);
                            loginForPortal();
                            cookieJar.copy("https://portal.sysu.edu.cn", "https://mportal.sysu.edu.cn");
                            return false;
                        }
                        case TargetUrl.XGXT_WEBVPN -> {
                            request("/esc-sso/login?service=" + service);
                            getXGXTToken(service, targetBaseUrl);
                        }
                        default -> request("/esc-sso/login?service=" + service);
                    }
                } else {
                    JSONObject publicKey = JSONObject.parse(getPublicKey()).getJSONObject("data").getJSONObject("param");
                    String redirect = redirect(doLogin(username, encrypt(publicKey.getString("publicKey"), password), publicKey.getString("publicKeyId")));
                    if (redirect == null) return false;
                    request(redirect + "?service=" + service);
                    switch (service) {
                        case TargetUrl.GYM -> {
                            getGymToken(targetBaseUrl);
//                            cookieJar.copy(targetBaseUrl, "https://gym.webvpn.sysu.edu.cn");
                            setAuthorization(host, getGymAuthorization(targetBaseUrl));
                        }
                        case TargetUrl.PAY -> {
                            String token = getPayToken(service);
                            setToken(host, token);
                            cookieJar.saveFromResponse(HttpUrl.get(service), List.of(new Cookie.Builder().name("ibps-1.0.1-token").value(token).domain("pay.sysu.edu.cn").build()));
                        }
                        case TargetUrl.ZHNY -> {
                            if (authorizationJar != null)
                                authorizationJar.setAuthorization(host, getZHNYAuthoritarian(service));
                        }
                        case TargetUrl.XGXT -> getXGXTToken(service, targetBaseUrl);
                        case TargetUrl.NEWS ->
                                setAuthorization(host, getNewsAuthorization(service));
                        case TargetUrl.LMS -> setToken(host, getLmsToken());
                    }
                }
                onSuccess();
            } catch (Exception e) {
                Log.e("LoginManager", e.getMessage(), e);
            }
            return isLogin;
        }).get();
    }
    
    public void setOnLoginListener(LoginListener loginListener) {
        this.loginListener = loginListener;
    }
    
    public void onError(String code, String message) {
        isLogin = false;
        if (loginListener != null)
            loginListener.onError(code, message);
    }
    
    public void onSuccess() {
        isLogin = true;
        if (loginListener != null)
            loginListener.onSuccess();
    }
    
    private void loginForPortal() throws IOException {
//        Response response =
        client.newCall(new Request.Builder().header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                .url("https://portal.sysu.edu.cn/newClient/auth?service=https%3A%2F%2Fportal.sysu.edu.cn%2FnewClient%2F%23%2FnewPortal%2Findex").build()).execute();
    }
    
    private void setToken(String host, String token) {
        if (authorizationJar != null)
            authorizationJar.setToken(host, token);
    }
    
    /*
     * 设置认证
     * @param host 主机
     * @param auth 认证
     * */
    private void setAuthorization(String host, String auth) {
        if (authorizationJar != null) authorizationJar.setAuthorization(host, "Bearer " + auth);
    }
    
    private void getXGXTToken(String service, String targetBaseUrl) throws IOException {
        client.newCall(new Request.Builder().url(targetBaseUrl + "sso/login?realm=sysuRealm&ticket=" + getTicket(service) + "&service=" + service)
                .post(RequestBody.create("", MediaType.parse("application/x-www-form-urlencoded")))
                .build()).execute();
    }
    
    private String getPayToken(String service) throws IOException {
        return JSONObject.parse(client.newCall(new Request.Builder().url("https://pay.sysu.edu.cn/client/api/client/auth/netId/login")
                .header("Referer", "https://pay.sysu.edu.cn/")
                .post(RequestBody.create("{\"key\":\"https://cas.sysu.edu.cn/cas/serviceValidate?service=https://pay.sysu.edu.cn/sso&ticket=" + getTicket(service) + "\"}", MediaType.parse("application/json")))
                .build()).execute().body().string()).getString("data");
    }
    
    private String getLmsToken() throws IOException {
        String response = client.newCall(new Request.Builder().url("https://lms.sysu.edu.cn/my/")
                .build()).execute().body().string();
        Matcher matcher = Pattern.compile("\"sesskey\":\"(.+?)\"").matcher(response);
        if (matcher.find()) return matcher.group(1);
        else onError("403", "获取 LMS 会话密钥失败");
        return "";
    }
    
    private String getZHNYAuthoritarian(String service) throws IOException {
        return JSONObject.parse(client.newCall(new Request.Builder().url("https://zhny.sysu.edu.cn/kbp/auth/third/h5/casLogin/" + getTicket(service))
                .build()).execute().body().string()).getString("data");
    }
    
    private String encrypt(String publicKeyBase64, String plainText) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keyFactory.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64))));
        return Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes("UTF-8")));
    }
    
    public String getTicket(String service) throws IOException {
        String location = directClient.newCall(new Request.Builder().url(casAuthorizationManager.getBaseUrl() + "/esc-sso/login?service=" + service).build()).execute().header("Location");
        return location == null ? "" : HttpUrl.get(location).queryParameter("ticket");
    }
    
    public void setAuthorization(AuthorizationJar authorizationJar) {
        this.authorizationJar = authorizationJar;
        
    }
    
    public void setCookieManager(com.sysu.edu.api.CookieManager cookieManager) {
        LoginManager.cookieManager = cookieManager;
    }
    
    public void getGymToken(String targetBaseUrl) throws IOException {
        Matcher re = Pattern.compile("prefix = '(.+?)'").matcher(loginForGym(targetBaseUrl));
        String prefix = "";
        List<Cookie> filterChallenge = cookieJar.loadForRequest(HttpUrl.get(targetBaseUrl)).stream().filter(e -> "safeline_bot_challenge".equals(e.name())).collect(Collectors.toCollection(ArrayList::new));
        if (re.find()) prefix = re.group(1);
        if (!filterChallenge.isEmpty() && !isEmpty(prefix))
            cookieJar.saveFromResponse(HttpUrl.get(targetBaseUrl), List.of(new Cookie.Builder().domain(HttpUrl.get(targetBaseUrl).host()).name("safeline_bot_challenge_ans").value(Answer.encode(prefix, filterChallenge.get(0).value())).build()));
    }
    
    public String getNewsAuthorization(String url) {
        return getAuthorization(new Request.Builder().url(casAuthorizationManager.getBaseUrl() + "/esc-sso/login?service=" + url).build());
    }
    
    public String getGymAuthorization(String targetBaseUrl) {
        return getAuthorization(new Request.Builder().url(targetBaseUrl + "authsport/Account/Auth?response_type=token&client_id=sysu_2021&redirect_uri=https%3A%2F%2gym.sysu.edu.cn%2F%23&client_id=unnc&scope=PE").header("Accept", "application/json, text/plain, */*")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36").build());
    }
    
    public String getAuthorization(Request request) {
        try {
            Response response = client.newCall(request).execute();
            String location;
            if (response.priorResponse() != null && (location = response.priorResponse().header("location")) != null && location.contains("access_token")) {
                Matcher matcher = Pattern.compile("access_token=(.*?)&").matcher(location);
                if (matcher.find())
                    return matcher.group(1);
            }
        } catch (IOException _) {
        }
        return "";
    }
    
    public interface LoginListener {
        void onSuccess();
        
        void onError(String code, String message);
    }
    
    static class CookieStore implements CookieJar {
        private final HashMap<String, List<Cookie>> _cookieStore = new HashMap<>();

//        public CookieStore() {
//        }

//        private void initCookieStore(String host) {
//            Set<String> cookieSet = cookieManager.get(host);
//            for (String cookie : cookieSet) {
//                String[] nameValue = cookie.trim().split("=", 2);
//                add(host, new Cookie.Builder().domain(host).name(nameValue[0]).value(nameValue[1]).build());
//            }
//        }
        
        @Override
        public void saveFromResponse(HttpUrl url, @NonNull List<Cookie> cookies) {
            String host = url.host();
            List<Cookie> currentCookies = _cookieStore.get(host);
            List<Cookie> responseCookies = new ArrayList<>(cookies);
            List<String> keys = responseCookies.stream().map(Cookie::name).collect(Collectors.toCollection(ArrayList::new));
            if (currentCookies != null && !responseCookies.isEmpty()
                    && !currentCookies.isEmpty())
                currentCookies.stream().filter(currentCookie -> !responseCookies.contains(currentCookie) && (!currentCookie.value().isEmpty()) && (!keys.contains(currentCookie.name()))).forEach(responseCookies::add);
            _cookieStore.put(host, responseCookies);
            cookieManager.set(host, responseCookies.stream().map(Cookie::toString).collect(Collectors.toCollection(HashSet::new)));
        }
        
        @NonNull
        @Override
        public List<Cookie> loadForRequest(HttpUrl url) {
            List<Cookie> cookies = _cookieStore.get(url.host());
            List<Cookie> loginCookies = new ArrayList<>();
            if (cookies != null && !cookies.isEmpty())
                loginCookies = cookies.stream().filter(currentCookie -> !currentCookie.value().isEmpty()).collect(Collectors.toList());
            return loginCookies;
        }
        
        public void copy(String from, String to) {
            saveFromResponse(HttpUrl.get(to), loadForRequest(HttpUrl.get(from)));
        }
        
        public String toString(String url) {
            return loadForRequest(HttpUrl.get(url)).stream().map(Cookie::toString).collect(Collectors.joining("; "));
        }
        
        public void add(String baseUrl, Cookie cookie) {
            saveFromResponse(HttpUrl.get(baseUrl), List.of(cookie));
        }
    }
    
    static class Answer {
        /**
         * 计算字符串的SHA1哈希值，返回十六进制字符串
         */
        public static String hexSha1(String input) throws NoSuchAlgorithmException {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(input.getBytes());
            return bytesToHex(hash);
        }
        
        /**
         * 将字节数组转换为十六进制字符串
         */
        private static String bytesToHex(byte[] bytes) {
            StringBuilder hexString = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        }
        
        /**
         * 将十六进制字符串转换为二进制字符串 每个十六进制字符转换为4位二进制
         */
        public static String hexToBinary(String hexStr) {
            StringBuilder binaryStr = new StringBuilder();
            for (int i = 0; i < hexStr.length(); i++) {
                char c = hexStr.charAt(i);
                int value = Character.digit(c, 16);
                // 转换为4位二进制，前面补0
                String binary = String.format("%4s", Integer.toBinaryString(value)).replace(' ', '0');
                binaryStr.append(binary);
            }
            return binaryStr.toString();
        }
        
        /**
         * 模拟JS中的bin_sha1函数
         */
        public static String binSha1(String input) throws NoSuchAlgorithmException {
            String hexHash = hexSha1(input);
            return hexToBinary(hexHash);
        }
        
        /**
         * 找到满足条件的suffix
         */
        public static String findSuffix(String prefix, int leadingZeroBit) throws NoSuchAlgorithmException {
            int cnt = 0;
            while (true) {
                String suffix = Integer.toHexString(cnt);
                String hashBinary = binSha1(prefix + suffix);
                // 检查前leadingZeroBit位是否全为0
                if (hashBinary.substring(0, leadingZeroBit).equals("0".repeat(leadingZeroBit))) {
                    return suffix;
                }
                cnt++;
            }
        }
        
        /**
         * 计算最终的safeline_bot_challenge_ans cookie值
         */
        public static String getFinalCookie(String safelineBotChallenge, String prefix, int leadingZeroBit)
                throws NoSuchAlgorithmException {
            return safelineBotChallenge + findSuffix(prefix, leadingZeroBit);
        }
        
        public static String encode(String prefix, String safelineBotChallenge) {
            try {
                return getFinalCookie(safelineBotChallenge, prefix, 9);
            } catch (NoSuchAlgorithmException e) {
                System.err.println("SHA-1 算法不可用: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("发生错误: " + e.getMessage());
            }
            return "";
        }
    }
}