package com.miyuyan.sysuer.view

import androidx.lifecycle.LiveData

interface RecyclerStateViewModel {
	val uiState: LiveData<UiState>
	fun retry()
}

enum class UiState {
	Unstarted,

	Loading,

	LoadMore,

	Empty,

	Error,

	Content
}