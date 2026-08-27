package com.sysu.edu.nav

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay

@Composable fun <T : Any> SysuerNavDisplay(backStack: List<T>, entryProvider: (key: T) -> NavEntry<T>) {
	NavDisplay(backStack = backStack,/* transitionSpec = {
		slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it / 3 } + fadeOut()
	}, popTransitionSpec = {
		slideInHorizontally { -it / 3 } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
	},*/ predictivePopTransitionSpec = {
		fadeIn() togetherWith fadeOut()
	}, entryProvider = entryProvider)
}