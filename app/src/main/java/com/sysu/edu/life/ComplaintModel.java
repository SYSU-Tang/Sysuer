package com.sysu.edu.life;

import com.alibaba.fastjson2.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ComplaintModel {

    // 服务器地址（请根据实际环境配置）
    private static final String BASE_URL = "https://xinfang.sysu.edu.cn";  // 例如 "http://your-server.com"

    // 接口路径
    private static final String SEND_CODE_URL = "/servlet/executeFun";
    private static final String CHECK_CODE_URL = "/servlet/checkcode";
    private static final String SUBMIT_URL = "/jsp_api/fywt";

    private final OkHttpClient httpClient;
    private final MessageCallback messageCallback;
    // 短信验证码发送间隔控制（单位：毫秒）
    private long lastSmsSendTime = -1;

    public ComplaintModel(MessageCallback callback) {
        messageCallback = callback;
        httpClient = new OkHttpClient.Builder()
                .build();
    }

    /**
     * 校验手机号格式（11位数字，1 开头，第二位3-9）
     */
    public static boolean isInvalidPhone(String phone) {
        return phone == null || !Pattern.matches("^1[3456789]\\d{9}$", phone);
    }

    /**
     * 校验反映内容长度（不超过1000字）
     */
    public static boolean isValidDescription(String description) {
        return description == null || description.length() <= 1000;
    }

    /**
     * 校验必填项
     *
     * @param fields       字段名 -> 值的映射
     * @param requiredKeys 必填字段名集合
     * @param fieldLabels  字段名 -> 中文标签映射（用于提示）
     * @return 是否通过校验
     */
    public boolean isValidateNotEmpty(Map<String, String> fields,
                                    Set<String> requiredKeys,
                                    Map<String, String> fieldLabels) {
        for (String key : requiredKeys) {
            String value = fields.get(key);
            if (value == null || value.trim().isEmpty()) {
                String label = fieldLabels.getOrDefault(key, key);
                showMessage(label + "不能为空", "warning");
                return false;
            }
        }
        return true;
    }

    /**
     * 完整表单校验（手机号 + 反映内容长度）
     *
     * @param phone       手机号
     * @param description 反映内容
     * @return 是否通过校验
     */
    public boolean isValidateForm(String phone, String description) {
        if (isInvalidPhone(phone)) {
            showMessage("手机号码填写有误,请输入有效的11位手机号码!", "warning");
            return false;
        }
        if (description != null && description.length() > 1000) {
            showMessage("反映内容不能多于1000字!", "warning");
            return false;
        }
        return true;
    }

    /**
     * 检查是否可以发送短信（间隔≥60秒）
     */
    public boolean canSendSms() {
        long now = System.currentTimeMillis();
        if (lastSmsSendTime > 0 && (now - lastSmsSendTime) < 60000) {
            showMessage("两次获取验证码必须间隔一分钟以上!", "warning");
            return false;
        }
        return true;
    }

    /**
     * 发送短信验证码（同步）
     *
     * @param phone 手机号
     * @throws IOException 网络异常
     */
    public void sendMobileCode(String phone) throws IOException {
        if (isInvalidPhone(phone)) {
            showMessage("手机号码填写有误,请输入有效的11位手机号码!", "warning");
            return;
        }
        if (!canSendSms()) {
            return;
        }

        // 构造 URL
        String url = BASE_URL + SEND_CODE_URL
                + "?className=MobileCode&ajaxType=get&function=sendMobileCode&type=mobile&subtype=jsjb"
                + "&mobileNum=" + java.net.URLEncoder.encode(phone, "UTF-8");

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(new byte[0]))  // POST 无 body
                .addHeader("X-MC", "MC")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                showMessage("短信验证码发送失败! HTTP " + response.code(), "error");
                return;
            }
            String responseText = response.body().string();
            // 原JS中有 unescape，Java中直接处理字符串即可
            if (responseText.startsWith("syserror_")) {
                String errorMsg = responseText.substring("syserror_".length());
                showMessage("短信验证码发送失败:" + errorMsg, "warning");
            } else {
                lastSmsSendTime = System.currentTimeMillis();  // 更新发送时间
                System.out.println("发送成功, 响应: " + responseText);
                showMessage("已发送短信验证码!", "success");
            }
        } catch (IOException e) {
            showMessage("短信验证码发送失败: " + e.getMessage(), "error");
            throw e;
        }
    }

    /**
     * 获取图形验证码图片的完整 URL（已带随机时间戳，避免缓存）
     */
    public String getCheckCodeImageUrl() {
        long timestamp = System.currentTimeMillis();
        return BASE_URL + CHECK_CODE_URL + "?rand=photorand&random=" + timestamp;
    }

    /**
     * 提交问题表单
     *
     * @param formFields   普通字段 (c 对象)
     * @param hiddenFields 隐藏字段 (h 对象)
     * @param attachments  附件数据列表 (f 数组) —— 按后端要求提供（如文件ID、Base64等）
     * @throws IOException 网络异常或提交失败
     */
    public void submitForm(Map<String, String> formFields,
                           Map<String, String> hiddenFields,
                           List<String> attachments) throws IOException {
        // 构造请求体 JSON
        JSONObject body = new JSONObject();
        body.put("c", formFields == null ? new JSONObject() : new JSONObject(formFields));
        body.put("h", hiddenFields == null ? new JSONObject() : new JSONObject(hiddenFields));
        body.put("f", attachments == null ? new ArrayList<>() : attachments);

        String jsonBody = body.toJSONString();
        System.out.println("Request Body: " + jsonBody);

        Request request = new Request.Builder()
                .url(BASE_URL + SUBMIT_URL)
                .post(RequestBody.create(jsonBody,MediaType.parse("application/json; charset=utf-8")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String respBody = response.body().string();
            if (response.isSuccessful()) {
                JSONObject respJson = JSONObject.parse(respBody);
                Boolean ok = respJson.getBoolean("ok");
                if (ok != null && ok) {
                    showMessage("提交成功!", "success");
                    // 原JS中有页面跳转，由调用方自行处理
                } else {
                    String msg = respJson.getString("msg");
                    showMessage(msg != null ? msg : "提交失败", "error");
                }
            } else {
                showMessage("网络异常! HTTP " + response.code(), "error");
                throw new IOException("Server responded with " + response.code());
            }
        } catch (IOException e) {
            showMessage("网络异常: " + e.getMessage(), "error");
            throw e;
        }
    }


    private void showMessage(String msg, String type) {
        if (messageCallback != null) {
            messageCallback.onMessage(msg, type);
        } else {
            System.out.println("[" + type.toUpperCase() + "] " + msg);
        }
    }

    // 消息回调接口
    public interface MessageCallback {
        void onMessage(String msg, String type); // type: "success", "warning", "error"
    }
}
