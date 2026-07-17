package com.sysu.edu.life

import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.model.XinfangModel
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.net.URLEncoder
import java.util.regex.Pattern

class ComplaintModel(val model: XinfangModel? = null) {
	private var lastSmsSendTime: Long = -1
	
	/**
	 * 校验必填项
	 * 
	 * @param fields       字段名 -> 值的映射
	 * @param requiredKeys 必填字段名集合
	 * @param fieldLabels  字段名 -> 中文标签映射（用于提示）
	 * @return 是否通过校验
	 */
	fun isValidateNotEmpty(fields: MutableMap<String?, String?>,
	                       requiredKeys: MutableSet<String?> = mutableSetOf("visitorName", "phone", "mobileCheckCode", "name", "description", "checkCode"),
	                       fieldLabels: MutableMap<String?, String?> = mutableMapOf("visitorName" to "姓名", "phone" to "手机号", "mobileCheckCode" to "短信验证码", "name" to "反映主题", "description" to "反映内容", "checkCode" to "验证码")): Boolean {
		requiredKeys.forEach { key ->
			val value = fields[key]
			if (value == null || value.trim { it <= ' ' }.isEmpty()) {
				val label = fieldLabels.getOrDefault(key, key)
				model?.contextUtil?.toast(label + "不能为空")
				return false
			}
		}
		return true
	}
	
	/**
	 * 完整表单校验（手机号 + 反映内容长度）
	 *
	 * @param phone       手机号
	 * @param description 反映内容
	 * @return 是否通过校验
	 */
	fun isValidateForm(phone: String?, description: String?): Boolean = when {
		isInvalidPhone(phone) -> {
			model?.contextUtil?.toast("手机号码填写有误,请输入有效的11位手机号码!")
			false
		}
		description != null && description.length > 1000 -> {
			model?.contextUtil?.toast("反映内容不能多于1000字!")
			false
		}
		else -> true
	}
	
	/**
	 * 检查是否可以发送短信（间隔≥60秒）
	 */
	fun canSendSms(): Boolean {
		val now = System.currentTimeMillis()
		return if (lastSmsSendTime > 0 && (now - lastSmsSendTime) < 60000) {
			model?.contextUtil?.toast("两次获取验证码必须间隔一分钟以上!")
			false
		}
		else true
	}
	
	/**
	 * 发送短信验证码（同步）
	 *
	 * @param phone 手机号
	 * @throws IOException 网络异常
	 */
	fun sendMobileCode(phone: String?) {
		if (isInvalidPhone(phone)) {
			model?.contextUtil?.toast("手机号码填写有误,请输入有效的11位手机号码!")
		}
		else if (canSendSms()) {
			model?.run("servlet/executeFun?className=MobileCode&ajaxType=get&function=sendMobileCode&type=mobile&subtype=jsjb&mobileNum=${URLEncoder.encode(phone, "utf8")}", "", null, object :
				Callback {
				override fun onFailure(call: Call, e: okio.IOException) {
					model.contextUtil.toast("短信验证码发送失败! HTTP " + e.message)
				}
				
				override fun onResponse(call: Call, response: Response) {
					if (!response.isSuccessful) {
						model.contextUtil.toast("短信验证码发送失败! HTTP " + response.code)
						return
					}
					val responseText = response.body.string()
					if (responseText.startsWith("syserror_")) {
						val errorMsg = responseText.substring("syserror_".length)
						model.contextUtil.toast("短信验证码发送失败:$errorMsg")
					}
					else {
						lastSmsSendTime = System.currentTimeMillis() // 更新发送时间
						println("发送成功, 响应: $responseText")
						model.contextUtil.toast("已发送短信验证码!")
					}
				}
			})
		}
	}
	
	/**
	 * 提交问题表单
	 *
	 * @param formFields   普通字段 (c 对象)
	 * @param hiddenFields 隐藏字段 (h 对象)
	 * @param attachments  附件数据列表 (f 数组) —— 按后端要求提供（如文件ID、Base64等）
	 * @throws IOException 网络异常或提交失败
	 */
	@Throws(IOException::class) fun submitForm(formFields: MutableMap<String?, String?>,
	                                           hiddenFields: JSONObject = JSONObject.of("yybz", "0", "visitConfig", "1"),
	                                           attachments: JSONArray) {
		if (!isValidateNotEmpty(formFields)) {
			val jsonBody = JSONObject.of("c", JSONObject(formFields), "h", hiddenFields, "f", attachments).toJSONString()
			//println("Request Body: $jsonBody")
			model?.addAndNext("jsp_api/fywt", jsonBody, 2)
		}
	}
	
	companion object {
		/**
		 * 校验手机号格式（11位数字，1 开头，第二位3-9）
		 */
		fun isInvalidPhone(phone: String?): Boolean =
			phone == null || !Pattern.matches("^1[3456789]\\d{9}$", phone)
	}
}
