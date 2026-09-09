package com.miyuyan.sysuer.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.miyuyan.sysuer.api.SettingManager

//private val DarkColorScheme = darkColorScheme(primary = SysuRedPrimary, secondary = SysuRedSecondary, tertiary = SysuRedTertiary)
//private val LightColorScheme = lightColorScheme(primary = SysuGreenPrimary, secondary = SysuGreenSecondary, tertiary = SysuGreenTertiary)

private val LightColorScheme = lightColorScheme(
	primary = LightPrimary,
	onPrimary = LightOnPrimary,
	primaryContainer = LightPrimaryContainer,
	onPrimaryContainer = LightOnPrimaryContainer,
	secondary = LightSecondary,
	onSecondary = LightOnSecondary,
	secondaryContainer = LightSecondaryContainer,
	onSecondaryContainer = LightOnSecondaryContainer,
	background = LightBackground,
	onBackground = LightOnBackground,
	surface = LightSurface,
	onSurface = LightOnSurface,
	surfaceVariant = LightSurfaceVariant,
	onSurfaceVariant = LightOnSurfaceVariant,
	outline = LightOutline
)

private val DarkColorScheme = darkColorScheme(
	primary = DarkPrimary,
	onPrimary = DarkOnPrimary,
	primaryContainer = DarkPrimaryContainer,
	onPrimaryContainer = DarkOnPrimaryContainer,
	secondary = DarkSecondary,
	onSecondary = DarkOnSecondary,
	secondaryContainer = DarkSecondaryContainer,
	onSecondaryContainer = DarkOnSecondaryContainer,
	background = DarkBackground,
	onBackground = DarkOnBackground,
	surface = DarkSurface,
	onSurface = DarkOnSurface,
	surfaceVariant = DarkSurfaceVariant,
	onSurfaceVariant = DarkOnSurfaceVariant,
	outline = DarkOutline
)

@Composable fun SysuerTheme(
	settingManager: SettingManager? = null,
	darkTheme: Boolean = settingManager?.isDarkTheme ?: isSystemInDarkTheme(),
	dynamicColor: Boolean = settingManager?.isDynamicColor ?: true,
	content: @Composable () -> Unit,
                           ) {
	val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
		val context = LocalContext.current
		if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
	}
	else if (darkTheme) DarkColorScheme
	else LightColorScheme
	
	MaterialTheme(colorScheme = colorScheme, typography = Typography, shapes = ExpressiveShapes, content = content)
}
