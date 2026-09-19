package com.example

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.data.model.PlayerState
import com.example.data.model.SurahAudioItem
import com.example.ui.components.MiniAudioPlayer
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class AudioPlayerComponentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleAudioItem = SurahAudioItem(
        reciterId = "mishari_alafasy",
        reciterName = "مشاري العفاسي",
        riwayah = "حفص عن عاصم",
        surahNumber = 1,
        surahName = "الفاتحة",
        ayahsCount = 7,
        revelationType = "مكية",
        audioSources = listOf("https://server8.mp3quran.net/afs/001.mp3")
    )

    @Test
    fun testMiniAudioPlayer_LightMode() {
        var clicked = false
        val state = PlayerState(
            currentItem = sampleAudioItem,
            isPlaying = true,
            isBuffering = false,
            currentPositionMs = 15000L,
            durationMs = 60000L
        )

        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                MiniAudioPlayer(
                    playerState = state,
                    onTogglePlayPause = { clicked = true },
                    onNext = {},
                    onPrevious = {},
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("btn_mini_play_pause").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_mini_play_pause").performClick()
        assertTrue("Play/Pause button click should trigger callback", clicked)
    }

    @Test
    fun testMiniAudioPlayer_DarkMode() {
        var clicked = false
        val state = PlayerState(
            currentItem = sampleAudioItem,
            isPlaying = true,
            isBuffering = false,
            currentPositionMs = 15000L,
            durationMs = 60000L
        )

        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                MiniAudioPlayer(
                    playerState = state,
                    onTogglePlayPause = { clicked = true },
                    onNext = {},
                    onPrevious = {},
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("btn_mini_play_pause").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_mini_play_pause").performClick()
        assertTrue("Play/Pause button click should trigger callback in dark mode", clicked)
    }

    @Test
    fun testMiniAudioPlayer_BufferingState() {
        val state = PlayerState(
            currentItem = sampleAudioItem,
            isPlaying = true,
            isBuffering = true,
            currentPositionMs = 0L,
            durationMs = 60000L
        )

        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                MiniAudioPlayer(
                    playerState = state,
                    onTogglePlayPause = {},
                    onNext = {},
                    onPrevious = {},
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("btn_mini_play_pause").assertIsDisplayed()
    }

    @Test
    fun testMiniAudioPlayer_ArabicRTL() {
        val state = PlayerState(
            currentItem = sampleAudioItem,
            isPlaying = false,
            isBuffering = false,
            currentPositionMs = 0L,
            durationMs = 60000L
        )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MyApplicationTheme(darkTheme = false) {
                    MiniAudioPlayer(
                        playerState = state,
                        onTogglePlayPause = {},
                        onNext = {},
                        onPrevious = {},
                        onClick = {}
                    )
                }
            }
        }
        composeTestRule.onNodeWithTag("btn_mini_play_pause").assertIsDisplayed()
    }

    @Test
    fun testMiniAudioPlayer_EnglishLTR() {
        val state = PlayerState(
            currentItem = sampleAudioItem,
            isPlaying = false,
            isBuffering = false,
            currentPositionMs = 0L,
            durationMs = 60000L
        )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                MyApplicationTheme(darkTheme = true) {
                    MiniAudioPlayer(
                        playerState = state,
                        onTogglePlayPause = {},
                        onNext = {},
                        onPrevious = {},
                        onClick = {}
                    )
                }
            }
        }
        composeTestRule.onNodeWithTag("btn_mini_play_pause").assertIsDisplayed()
    }

    @Test
    fun testMiniAudioPlayer_FontScaling_1_5x() {
        val state = PlayerState(
            currentItem = sampleAudioItem,
            isPlaying = false,
            isBuffering = false,
            currentPositionMs = 0L,
            durationMs = 60000L
        )

        composeTestRule.setContent {
            val currentDensity = LocalDensity.current
            val scaledDensity = Density(density = currentDensity.density, fontScale = 1.5f)
            CompositionLocalProvider(LocalDensity provides scaledDensity) {
                MyApplicationTheme(darkTheme = false) {
                    MiniAudioPlayer(
                        playerState = state,
                        onTogglePlayPause = {},
                        onNext = {},
                        onPrevious = {},
                        onClick = {}
                    )
                }
            }
        }
        composeTestRule.onNodeWithTag("btn_mini_play_pause").assertIsDisplayed()
    }
}
