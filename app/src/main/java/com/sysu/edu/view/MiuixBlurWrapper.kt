package com.sysu.edu.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur

@Composable
fun BlurContainer(
	blurRadius: Float = 20f,
	content: @Composable () -> Unit
) {
	Box(
		modifier = Modifier
			.fillMaxSize()
			.textureBlur(
				backdrop = rememberLayerBackdrop(),
				shape = RoundedCornerShape(12.dp),
				blurRadius = blurRadius
			)
	) {
		content()
	}
}