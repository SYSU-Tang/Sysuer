package com.miyuyan.sysuer.home

data class ServiceConfig(
	@JvmField val id: Int,
	@JvmField val name: String?,
	@JvmField val route: String?,
	@JvmField val activity: String?,
	@JvmField val url: String?,
	@JvmField val doc: String?,
	@JvmField val description: String?,
                        )