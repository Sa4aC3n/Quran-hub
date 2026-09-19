package com.example

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class ThemeContrastUnitTest {

    private fun calculateRelativeLuminance(color: Color): Double {
        fun channelLuminance(channel: Float): Double {
            val c = channel.toDouble()
            return if (c <= 0.04045) {
                c / 12.92
            } else {
                ((c + 0.055) / 1.055).pow(2.4)
            }
        }

        val r = channelLuminance(color.red)
        val g = channelLuminance(color.green)
        val b = channelLuminance(color.blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun contrastRatio(color1: Color, color2: Color): Double {
        val lum1 = calculateRelativeLuminance(color1)
        val lum2 = calculateRelativeLuminance(color2)
        val lighter = max(lum1, lum2)
        val darker = min(lum1, lum2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    @Test
    fun testPrimaryOnPrimaryContrast_LightAndDark() {
        val lightContrast = contrastRatio(IslamicLightPrimary, IslamicLightOnPrimary)
        assertTrue("Light primary/onPrimary should exceed 4.5:1, was $lightContrast", lightContrast >= 4.5)
        assertTrue("Light primary/onPrimary should reach ~7.73:1, was $lightContrast", lightContrast >= 7.0)

        val darkContrast = contrastRatio(IslamicDarkPrimary, IslamicDarkOnPrimary)
        assertTrue("Dark primary/onPrimary should exceed 4.5:1, was $darkContrast", darkContrast >= 4.5)
        assertTrue("Dark primary/onPrimary should reach ~7.42:1, was $darkContrast", darkContrast >= 7.0)
    }

    @Test
    fun testHeroBannerContrast_LightAndDark() {
        val heroLightTop = Color(0xFF0E563E)
        val heroLightBottom = Color(0xFF0A4430)
        val onHero = Color(0xFFFFFFFF)
        val heroAccentLight = Color(0xFFFCE9BE)

        val contrastOnHeroLightTop = contrastRatio(onHero, heroLightTop)
        val contrastOnHeroLightBottom = contrastRatio(onHero, heroLightBottom)
        val contrastAccentLightTop = contrastRatio(heroAccentLight, heroLightTop)
        val contrastAccentLightBottom = contrastRatio(heroAccentLight, heroLightBottom)

        assertTrue("onHero on light hero top must exceed 7.0:1, was $contrastOnHeroLightTop", contrastOnHeroLightTop >= 7.0)
        assertTrue("onHero on light hero bottom must exceed 7.0:1, was $contrastOnHeroLightBottom", contrastOnHeroLightBottom >= 7.0)
        assertTrue("heroAccent on light hero top must exceed 7.0:1, was $contrastAccentLightTop", contrastAccentLightTop >= 7.0)
        assertTrue("heroAccent on light hero bottom must exceed 7.0:1, was $contrastAccentLightBottom", contrastAccentLightBottom >= 7.0)

        val heroDarkTop = Color(0xFF0D3828)
        val heroDarkBottom = Color(0xFF051911)
        val heroAccentDark = Gold400 // Color(0xFFECC276)

        val contrastOnHeroDarkTop = contrastRatio(onHero, heroDarkTop)
        val contrastOnHeroDarkBottom = contrastRatio(onHero, heroDarkBottom)
        val contrastAccentDarkTop = contrastRatio(heroAccentDark, heroDarkTop)
        val contrastAccentDarkBottom = contrastRatio(heroAccentDark, heroDarkBottom)

        assertTrue("onHero on dark hero top must exceed 10.0:1, was $contrastOnHeroDarkTop", contrastOnHeroDarkTop >= 10.0)
        assertTrue("onHero on dark hero bottom must exceed 15.0:1, was $contrastOnHeroDarkBottom", contrastOnHeroDarkBottom >= 15.0)
        assertTrue("heroAccent on dark hero top must exceed 7.0:1, was $contrastAccentDarkTop", contrastAccentDarkTop >= 7.0)
        assertTrue("heroAccent on dark hero bottom must exceed 10.0:1, was $contrastAccentDarkBottom", contrastAccentDarkBottom >= 10.0)
    }

    @Test
    fun testAudioPlayerControlsContrast() {
        // Light Theme Audio Player Main Button
        val lightButtonBg = IslamicLightPrimary // 0xFF145E45
        val lightButtonIcon = IslamicLightOnPrimary // 0xFFFFFFFF
        val lightContrast = contrastRatio(lightButtonIcon, lightButtonBg)
        assertTrue("Audio player play button in light mode must exceed 7.0:1, was $lightContrast", lightContrast >= 7.0)

        // Dark Theme Audio Player Main Button
        val darkButtonBg = IslamicDarkPrimary // 0xFF5ED8A1
        val darkButtonIcon = IslamicDarkOnPrimary // 0xFF003825
        val darkContrast = contrastRatio(darkButtonIcon, darkButtonBg)
        assertTrue("Audio player play button in dark mode must exceed 7.0:1, was $darkContrast", darkContrast >= 7.0)

        // Verification of Old Flaw vs New Fix:
        // White on IslamicDarkPrimary is only ~1.78:1 (FAILED WCAG!)
        val oldWhiteOnDarkPrimary = contrastRatio(Color.White, IslamicDarkPrimary)
        assertTrue("White on dark primary must be low (< 2.0:1), proving old flaw: $oldWhiteOnDarkPrimary", oldWhiteOnDarkPrimary < 2.0)

        // New Fix: onPrimary (0xFF003825) on IslamicDarkPrimary (0xFF5ED8A1) is ~7.42:1 (PASSES WCAG AAA!)
        assertTrue("Fixed onPrimary on dark primary must exceed 7.0:1, was $darkContrast", darkContrast >= 7.0)
    }

    @Test
    fun testGoldTextContrast() {
        // Light Gold Text on white and light background
        val lightGoldText = GoldTextLight // 0xFF7A5809
        val whiteBg = Color(0xFFFFFFFF)
        val lightBg = IslamicLightBackground // 0xFFF8FAF7

        val contrastOnWhite = contrastRatio(lightGoldText, whiteBg)
        val contrastOnLight = contrastRatio(lightGoldText, lightBg)

        assertTrue("GoldTextLight on white must exceed 4.5:1 (WCAG AA), was $contrastOnWhite", contrastOnWhite >= 4.5)
        assertTrue("GoldTextLight on light background must exceed 4.5:1, was $contrastOnLight", contrastOnLight >= 4.5)

        // Dark Gold Text on nocturnal dark surface
        val darkGoldText = GoldTextDark // 0xFFECC276
        val darkSurface = IslamicDarkSurface // 0xFF0D281E
        val contrastOnDark = contrastRatio(darkGoldText, darkSurface)
        assertTrue("GoldTextDark on dark surface must exceed 7.0:1 (WCAG AAA), was $contrastOnDark", contrastOnDark >= 7.0)
    }

    @Test
    fun testMeetingModeContrast() {
        // Light Mode
        val lightContainer = MeetingRedLightContainer // 0xFFFFEBEE
        val lightText = MeetingRedLightText // 0xFFC62828
        val lightIcon = MeetingRedLightIcon // 0xFFB71C1C

        val textLightContrast = contrastRatio(lightText, lightContainer)
        val iconLightContrast = contrastRatio(lightIcon, lightContainer)

        assertTrue("Meeting mode text in light mode must exceed 4.5:1, was $textLightContrast", textLightContrast >= 4.5)
        assertTrue("Meeting mode icon in light mode must exceed 3.0:1, was $iconLightContrast", iconLightContrast >= 3.0)

        // Dark Mode
        val darkContainer = MeetingRedDarkContainer // 0xFF2C1314
        val darkText = MeetingRedDarkText // 0xFFFFCDD2
        val darkIcon = MeetingRedDarkIcon // 0xFFFF8A80

        val textDarkContrast = contrastRatio(darkText, darkContainer)
        val iconDarkContrast = contrastRatio(darkIcon, darkContainer)

        assertTrue("Meeting mode text in dark mode must exceed 7.0:1, was $textDarkContrast", textDarkContrast >= 7.0)
        assertTrue("Meeting mode icon in dark mode must exceed 4.5:1, was $iconDarkContrast", iconDarkContrast >= 4.5)
    }
}
