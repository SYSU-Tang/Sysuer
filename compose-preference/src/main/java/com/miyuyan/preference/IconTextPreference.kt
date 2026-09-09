package com.miyuyan.preference

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource

@Composable
fun IconTextPreference(
	title: String,
	icon: Int,
	modifier: Modifier = Modifier,
	summary: String? = null,
) {
	Preference(
		onClick = null,
		title = title,
		modifier = modifier,
		icon = {
			Icon(
				painterResource(icon),
				contentDescription = title,
				tint = MaterialTheme.colorScheme.primary
			)
		},
		summary = summary,
	)
}