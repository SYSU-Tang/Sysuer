package com.miyuyan.sysuer.api

import kotlinx.serialization.json.Json


val json = Json {
	ignoreUnknownKeys = true
	explicitNulls = true
	encodeDefaults = true
}