package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = Emerald900,
    primaryContainer = EmeraldPrimaryContainerDark,
    onPrimaryContainer = EmeraldPrimaryOnDarkContainer,
    secondary = Gold400,
    onSecondary = Gold800,
    secondaryContainer = Gold700,
    onSecondaryContainer = Gold100,
    tertiary = Emerald600,
    background = PolishBgDark,
    onBackground = PolishTextLight,
    surface = PolishSurfaceDark,
    onSurface = PolishTextLight,
    surfaceVariant = PolishSurfaceVariantDark,
    onSurfaceVariant = PolishTextMutedDark,
    outline = PolishBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = Color.White,
    primaryContainer = NavyLightSurface,
    onPrimaryContainer = NavyDark,
    secondary = GoldPrimary,
    onSecondary = NavyDark,
    secondaryContainer = Gold100,
    onSecondaryContainer = Gold800,
    tertiary = GoldLight,
    onTertiary = NavyDark,
    background = LuxuryBgLight,
    onBackground = LuxuryTextDark,
    surface = LuxurySurfaceLight,
    onSurface = LuxuryTextDark,
    surfaceVariant = LuxurySurfaceVariantLight,
    onSurfaceVariant = LuxuryTextSecondary,
    outline = LuxuryBorderLight,
    outlineVariant = LuxuryBorderLight.copy(alpha = 0.5f)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep bespoke Islamic Emerald palette consistent
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
