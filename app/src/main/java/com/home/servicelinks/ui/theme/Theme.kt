package com.home.servicelinks.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Sky400,
    onPrimary = Slate900,
    background = Slate900,
    surface = Slate800,
    surfaceVariant = Color(0xFF26354B),
    surfaceContainer = Slate800,
    surfaceContainerHigh = Color(0xFF223046),
    surfaceContainerHighest = Color(0xFF283852),
    surfaceContainerLow = Slate850,
    surfaceContainerLowest = Slate900,
    outline = Slate700,
    onSurface = Slate50,
    onBackground = Slate50,
    onSurfaceVariant = Slate400,
    secondaryContainer = Sky900,
    onSecondaryContainer = Sky400
)

private val LightColorScheme = lightColorScheme(
    primary = Sky600,
    onPrimary = Color.White,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F5F9),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF1F5F9),
    surfaceContainerHighest = Color(0xFFE2E8F0),
    surfaceContainerLow = Color(0xFFFAFAFA),
    surfaceContainerLowest = Color.White,
    outline = Color(0xFFCBD5E1),
    onSurface = Slate900,
    onBackground = Slate900,
    onSurfaceVariant = Color(0xFF64748B),
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Sky600
)

@Composable
fun LinksTheme(
    darkMode: String = "Dark",
    content: @Composable () -> Unit
) {
    val isDark = darkMode == "Dark" || (darkMode == "System" && isSystemInDarkTheme())
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            // Forces status bar icons to WHITE when isDark is true
            controller.isAppearanceLightStatusBars = !isDark
            controller.isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
