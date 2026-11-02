package com.miyuyan.sysuer.api

import kotlinx.serialization.json.Json


val SysuerJson = Json {
	ignoreUnknownKeys = true
	explicitNulls = true
	encodeDefaults = true
}