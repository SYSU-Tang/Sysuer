package com.miyuyan.preference

import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation3.runtime.NavKey

@Composable
fun JumpPreference(
	key: Int,
	icon: Drawable,
	modifier: Modifier = Modifier,
	activity: Class<*>? = null,
	route: NavKey? = null,
	backStack: MutableList<NavKey>? = null,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
	onClick: (() -> Unit)? = null,
) {
	val context = LocalContext.current
	val resolvedOnClick: () -> Unit = onClick ?: {
		if (route != null && backStack != null) {
			backStack.add(route)
		} else if (activity != null) {
			context.startActivity(Intent(context, activity))
		}
	}
	Preference(
		onClick = resolvedOnClick,
		title = stringResource(key),
		modifier = modifier.then(
			if (sharedTransitionScope != null && animatedVisibilityScope != null) {
			with(sharedTransitionScope) {
				Modifier.sharedBounds(
					sharedContentState = rememberSharedContentState(
						key = route.toString()
					),
					animatedVisibilityScope = animatedVisibilityScope,
				)
			}
		} else Modifier),
		icon = {
			Icon(
				icon.toBitmap().asImageBitmap(), contentDescription = null,
				tint = MaterialTheme.colorScheme.primary,
			)
		},
		trailing = {
			Icon(
				tint = MaterialTheme.colorScheme.primary,
				imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
				contentDescription = null,
				modifier = Modifier.size(20.dp),
			)
		},
	)
}

@Composable
fun JumpPreference(
	key: Int,
	icon: Int,
	modifier: Modifier = Modifier,
	activity: Class<*>? = null,
	route: NavKey? = null,
	backStack: MutableList<NavKey>? = null,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
	onClick: (() -> Unit)? = null,
) {
	val context = LocalContext.current
	val resolvedOnClick: () -> Unit = onClick ?: {
		if (route != null && backStack != null) {
			backStack.add(route)
		} else if (activity != null) {
			context.startActivity(Intent(context, activity))
		}
	}
	Preference(
		onClick = resolvedOnClick,
		title = stringResource(key),
		modifier = modifier.then(
			if (sharedTransitionScope != null && animatedVisibilityScope != null) {
			with(sharedTransitionScope) {
				Modifier.sharedBounds(
					sharedContentState = rememberSharedContentState(
						key = route.toString()
					),
					animatedVisibilityScope = animatedVisibilityScope,
				)
			}
		} else Modifier),
		icon = {
			Icon(
				painterResource(icon),
				tint = MaterialTheme.colorScheme.primary,
				contentDescription = stringResource(key)
			)
		},
		trailing = {
			Icon(
				imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
				tint = MaterialTheme.colorScheme.primary,
				contentDescription = null,
				modifier = Modifier.size(20.dp),
			)
		},
	)
}