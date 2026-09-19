package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ==========================================
// Material 3 Color Schemes - Unified Emerald & Gold
// ==========================================

private val DarkColorScheme = darkColorScheme(
    primary = IslamicDarkPrimary,
    onPrimary = IslamicDarkOnPrimary,
    primaryContainer = IslamicDarkPrimaryContainer,
    onPrimaryContainer = IslamicDarkOnPrimaryContainer,
    inversePrimary = IslamicLightPrimary,
    secondary = IslamicDarkSecondary,
    onSecondary = IslamicDarkOnSecondary,
    secondaryContainer = IslamicDarkSecondaryContainer,
    onSecondaryContainer = IslamicDarkOnSecondaryContainer,
    tertiary = IslamicDarkTertiary,
    onTertiary = IslamicDarkOnTertiary,
    background = IslamicDarkBackground,
    onBackground = IslamicDarkTextPrimary,
    surface = IslamicDarkSurface,
    onSurface = IslamicDarkTextPrimary,
    surfaceVariant = IslamicDarkSurfaceVariant,
    onSurfaceVariant = IslamicDarkTextSecondary,
    surfaceTint = IslamicDarkPrimary,
    inverseSurface = IslamicLightSurface,
    inverseOnSurface = IslamicLightTextPrimary,
    surfaceDim = Color(0xFF061710),
    surfaceBright = Color(0xFF163E2F),
    surfaceContainerLowest = Color(0xFF04120D),
    surfaceContainerLow = Color(0xFF0A2219),
    surfaceContainer = Color(0xFF0D281E),
    surfaceContainerHigh = Color(0xFF143629),
    surfaceContainerHighest = Color(0xFF1B4434),
    outline = IslamicDarkOutline,
    outlineVariant = IslamicDarkOutlineVariant,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    scrim = Color(0xFF000000)
)

private val LightColorScheme = lightColorScheme(
    primary = IslamicLightPrimary,
    onPrimary = IslamicLightOnPrimary,
    primaryContainer = IslamicLightPrimaryContainer,
    onPrimaryContainer = IslamicLightOnPrimaryContainer,
    inversePrimary = IslamicDarkPrimary,
    secondary = IslamicLightSecondary,
    onSecondary = IslamicLightOnSecondary,
    secondaryContainer = IslamicLightSecondaryContainer,
    onSecondaryContainer = IslamicLightOnSecondaryContainer,
    tertiary = IslamicLightTertiary,
    onTertiary = IslamicLightOnTertiary,
    background = IslamicLightBackground,
    onBackground = IslamicLightTextPrimary,
    surface = IslamicLightSurface,
    onSurface = IslamicLightTextPrimary,
    surfaceVariant = IslamicLightSurfaceVariant,
    onSurfaceVariant = IslamicLightTextSecondary,
    surfaceTint = IslamicLightPrimary,
    inverseSurface = IslamicDarkSurface,
    inverseOnSurface = IslamicDarkTextPrimary,
    surfaceDim = Color(0xFFD6E2DB),
    surfaceBright = Color(0xFFF8FAF7),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F6F3),
    surfaceContainer = Color(0xFFEDF3EE),
    surfaceContainerHigh = Color(0xFFE6EEE8),
    surfaceContainerHighest = Color(0xFFDFE8E2),
    outline = IslamicLightOutline,
    outlineVariant = IslamicLightOutlineVariant,
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    scrim = Color(0xFF000000)
)

// ==========================================
// Semantic Islamic Design System Extensions
// ==========================================

@Immutable
data class IslamicColors(
    val isDark: Boolean,
    val heroGradient: Brush,
    val onHero: Color,
    val heroAccent: Color,
    val headerGradient: Brush,
    val headerBorder: Color,
    val cardGradient: Brush,
    val cardBackground: Color,
    val cardBorder: Color,
    val ornamentColor: Color,
    val goldAccent: Color,
    val goldText: Color,
    val activeContainer: Color,
    val onActiveContainer: Color,
    val badgeContainer: Color,
    val badgeContent: Color,
    val selectedBadgeContainer: Color,
    val selectedBadgeContent: Color,
    val meetingModeContainer: Color,
    val meetingModeOnContainer: Color,
    val meetingModeBorder: Color,
    val meetingModeIcon: Color
)

val LocalIslamicColors = staticCompositionLocalOf {
    IslamicColors(
        isDark = true,
        heroGradient = Brush.verticalGradient(listOf(Color(0xFF0C3827), Color(0xFF071E15))),
        onHero = Color(0xFFFFFFFF),
        heroAccent = Gold400,
        headerGradient = Brush.verticalGradient(listOf(Color(0xFF0D3828), Color(0xFF071E15))),
        headerBorder = Gold500.copy(alpha = 0.35f),
        cardGradient = Brush.verticalGradient(listOf(Color(0xFF0E2F23), Color(0xFF071F16))),
        cardBackground = IslamicDarkSurface,
        cardBorder = Gold500.copy(alpha = 0.35f),
        ornamentColor = Gold400.copy(alpha = 0.22f),
        goldAccent = Gold400,
        goldText = GoldTextDark,
        activeContainer = Color(0xFF0E3828),
        onActiveContainer = Gold400,
        badgeContainer = Gold500.copy(alpha = 0.2f),
        badgeContent = Gold400,
        selectedBadgeContainer = IslamicDarkPrimary,
        selectedBadgeContent = IslamicDarkOnPrimary,
        meetingModeContainer = MeetingRedDarkContainer,
        meetingModeOnContainer = MeetingRedDarkText,
        meetingModeBorder = MeetingRedDarkBorder.copy(alpha = 0.45f),
        meetingModeIcon = MeetingRedDarkIcon
    )
}

val LocalIsDarkTheme = staticCompositionLocalOf { false }

object IslamicTheme {
    val colors: IslamicColors
        @Composable
        @ReadOnlyComposable
        get() = LocalIslamicColors.current

    val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalIsDarkTheme.current
}

private fun createIslamicColors(isDark: Boolean): IslamicColors {
    return if (isDark) {
        IslamicColors(
            isDark = true,
            heroGradient = Brush.verticalGradient(
                listOf(
                    Color(0xFF0D3828),
                    Color(0xFF08261B),
                    Color(0xFF051911)
                )
            ),
            onHero = Color(0xFFFFFFFF),
            heroAccent = Gold400,
            headerGradient = Brush.verticalGradient(
                listOf(
                    Color(0xFF0D3828),
                    Color(0xFF09291D),
                    Color(0xFF071E15)
                )
            ),
            headerBorder = Gold500.copy(alpha = 0.35f),
            cardGradient = Brush.verticalGradient(
                listOf(
                    Color(0xFF09291D),
                    Color(0xFF061E15)
                )
            ),
            cardBackground = IslamicDarkSurface,
            cardBorder = Gold500.copy(alpha = 0.35f),
            ornamentColor = Gold400.copy(alpha = 0.22f),
            goldAccent = Gold400,
            goldText = GoldTextDark,
            activeContainer = Color(0xFF0E3828),
            onActiveContainer = Gold400,
            badgeContainer = Gold500.copy(alpha = 0.2f),
            badgeContent = Gold400,
            selectedBadgeContainer = IslamicDarkPrimary,
            selectedBadgeContent = IslamicDarkOnPrimary,
            meetingModeContainer = MeetingRedDarkContainer,
            meetingModeOnContainer = MeetingRedDarkText,
            meetingModeBorder = MeetingRedDarkBorder.copy(alpha = 0.5f),
            meetingModeIcon = MeetingRedDarkIcon
        )
    } else {
        IslamicColors(
            isDark = false,
            heroGradient = Brush.verticalGradient(
                listOf(
                    Color(0xFF0E563E),
                    Color(0xFF0A4430)
                )
            ),
            onHero = Color(0xFFFFFFFF),
            heroAccent = Color(0xFFFCE9BE),
            headerGradient = Brush.verticalGradient(
                listOf(
                    Color(0xFF0E563E),
                    Color(0xFF0A4430),
                    Color(0xFF083827)
                )
            ),
            headerBorder = Gold400.copy(alpha = 0.45f),
            cardGradient = Brush.verticalGradient(
                listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFF7FAF7)
                )
            ),
            cardBackground = IslamicLightSurface,
            cardBorder = IslamicLightOutline,
            ornamentColor = IslamicLightSecondary.copy(alpha = 0.18f),
            goldAccent = IslamicLightSecondary,
            goldText = GoldTextLight,
            activeContainer = Color(0xFFD6EFE3),
            onActiveContainer = IslamicLightPrimary,
            badgeContainer = IslamicLightSecondaryContainer,
            badgeContent = IslamicLightOnSecondaryContainer,
            selectedBadgeContainer = IslamicLightPrimary,
            selectedBadgeContent = Color(0xFFFFFFFF),
            meetingModeContainer = MeetingRedLightContainer,
            meetingModeOnContainer = MeetingRedLightText,
            meetingModeBorder = MeetingRedLightBorder,
            meetingModeIcon = MeetingRedLightIcon
        )
    }
}

// ==========================================
// App Theme Entry Point
// ==========================================

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

    val islamicColors = createIslamicColors(darkTheme)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                // When in light theme, use dark icons for status bar and navigation bar
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalIsDarkTheme provides darkTheme,
        LocalIslamicColors provides islamicColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

