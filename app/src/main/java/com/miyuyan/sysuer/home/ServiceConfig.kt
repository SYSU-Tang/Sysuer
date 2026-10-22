package com.miyuyan.sysuer.home

import kotlinx.serialization.Serializable

@Serializable
data class ServiceConfig(
	@JvmField val id: Int = 0,
	@JvmField val name: String? = null,
	@JvmField val route: String? = null,
	@JvmField val activity: String? = null,
	@JvmField val url: String? = null,
	@JvmField val doc: String? = null,
	@JvmField val description: String? = null,
)

@Serializable
data class ServiceGroup(
	val id: Int = 0,
	val name: String = "",
	val description: String? = null,
	val items: List<ServiceConfig> = emptyList(),
)