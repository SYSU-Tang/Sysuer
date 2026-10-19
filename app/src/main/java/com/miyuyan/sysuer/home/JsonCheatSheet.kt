package com.miyuyan.sysuer.home

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * fastjson2 → kotlinx.serialization 替换速查
 *
 * 仅做参考，本文件不会被业务代码引用。模块化迁移时按表对照即可。
 * 共享的 Json 实例（ignoreUnknownKeys 兼容旧数据缺字段，encodeDefaults 输出默认值）。
 */
private val json = Json {
	ignoreUnknownKeys = true
	encodeDefaults = true
	explicitNulls = false // 与 fastjson2 WriteNulls 行为更接近：不写 null 字段
}

// =============================================================================
// 场景 1：动态键取值（无 schema），对应 JSONObject.parse(json).getXxx("key")
// =============================================================================

/**
 * 读 String（fastjson2: getString），不存在 / 类型不对 / 解析失败 都返回 null
 */
fun JSONObject_getString(raw: String?, key: String): String? =
	runCatching {
		json.parseToJsonElement(raw ?: return@runCatching null)
			.jsonObject[key]
			?.jsonPrimitive
			?.contentOrNull
	}.getOrNull()

/**
 * 读 Int（fastjson2: getInteger）
 */
fun JSONObject_getInt(raw: String?, key: String): Int? =
	runCatching {
		json.parseToJsonElement(raw ?: return@runCatching null)
			.jsonObject[key]
			?.jsonPrimitive
			?.contentOrNull
			?.toIntOrNull()
	}.getOrNull()

/**
 * 读 JSON 对象子节点（fastjson2: getJSONObject）
 */
fun JSONObject_getObject(raw: String?, key: String): JsonObject? =
	runCatching {
		json.parseToJsonElement(raw ?: return@runCatching null)
			.jsonObject[key]
			?.jsonObject
	}.getOrNull()

/**
 * 读 JSON 数组（fastjson2: getJSONArray）
 */
fun JSONObject_getArray(raw: String?, key: String): List<JsonElement>? =
	runCatching {
		json.parseToJsonElement(raw ?: return@runCatching null)
			.jsonObject[key]
			?.jsonArray
	}.getOrNull()

// =============================================================================
// 场景 2：有 schema 序列化（对应 data class.toJSONString(Feature.FieldBased, Feature.WriteNulls)）
// =============================================================================

@Serializable
data class ExampleDto(
	val id: Int = 0,
	val name: String? = null,
	val url: String? = null,
)

/** 序列化为字符串；字段为 null 时不写入（与 WriteNulls 关闭时一致） */
fun toJsonString(value: ExampleDto): String = json.encodeToString(ExampleDto.serializer(), value)

// =============================================================================
// 场景 3：有 schema 反序列化（对应 JSONObject.parseObject(json, ServiceConfig::class.java)）
// =============================================================================

fun fromJsonString(raw: String?): ExampleDto? =
	runCatching { json.decodeFromString(ExampleDto.serializer(), raw ?: return@runCatching null) }
		.getOrNull()

// =============================================================================
// 场景 4：动态构造 JSON 字符串（无 data class，原 JSONObject() / put() / toString()）
// =============================================================================

/**
 * 用 kotlinx.serialization 的 JsonObjectBuilder 动态拼装：
 *
 * buildJsonObject {
 *     put("id", 1)
 *     put("name", "x")
 *     put("tags", buildJsonArray { add("a"); add("b") })
 * }.toString()
 */
