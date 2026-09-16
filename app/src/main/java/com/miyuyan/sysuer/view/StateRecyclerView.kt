package com.miyuyan.sysuer.view

import kotlinx.coroutines.flow.StateFlow

interface RecyclerStateViewModel {
	val uiState: StateFlow<UiState>
	fun retry()
}

enum class UiState {
	Unstarted,

	Unauthorized,

	Loading,

	LoadMore,

	Empty,

	Error,

	Content,

}