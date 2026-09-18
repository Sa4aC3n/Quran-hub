package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.haram.HaramLiveStreamManager
import com.example.haram.HaramLocation
import com.example.haram.HaramNotificationManager
import com.example.haram.HaramNotificationMode
import com.example.haram.HaramNowPlaying
import com.example.haram.HaramTargetSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HaramNotificationTest {

    private lateinit var context: Context
    private lateinit var notificationManager: HaramNotificationManager
    private lateinit var liveStreamManager: HaramLiveStreamManager
    private val testScope = TestScope()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Clear prefs for test isolation
        context.getSharedPreferences("haram_notification_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        notificationManager = HaramNotificationManager(context)
        liveStreamManager = HaramLiveStreamManager(context, testScope, notificationManager)
    }

    @Test
    fun `preferences default to opt-in disabled`() {
        val prefs = notificationManager.loadPreferences()
        assertFalse("Haram notifications should be disabled by default (Opt-In)", prefs.isEnabled)
    }

    @Test
    fun `enabling preferences persists correctly`() {
        notificationManager.updatePreferences(
            isEnabled = true,
            targetSource = HaramTargetSource.MAKKAH_ONLY,
            notificationMode = HaramNotificationMode.ONLY_PREFERRED_SURAHS,
            preferredSurahs = setOf(1, 18, 36),
            maxDailyNotifications = 3
        )

        val updated = notificationManager.loadPreferences()
        assertTrue(updated.isEnabled)
        assertEquals(HaramTargetSource.MAKKAH_ONLY, updated.targetSource)
        assertEquals(HaramNotificationMode.ONLY_PREFERRED_SURAHS, updated.notificationMode)
        assertEquals(setOf(1, 18, 36), updated.preferredSurahNumbers)
        assertEquals(3, updated.maxDailyNotifications)
    }

    @Test
    fun `canSendNotification respects opt-in toggle`() {
        val nowPlaying = HaramNowPlaying(
            location = HaramLocation.MAKKAH,
            surahNumber = 18,
            surahName = "الكهف"
        )

        // When disabled
        notificationManager.updatePreferences(isEnabled = false)
        assertFalse(notificationManager.canSendNotification(nowPlaying))

        // When enabled
        notificationManager.updatePreferences(isEnabled = true, targetSource = HaramTargetSource.BOTH)
        assertTrue(notificationManager.canSendNotification(nowPlaying))
    }

    @Test
    fun `canSendNotification filters by location and preferred surahs`() {
        notificationManager.updatePreferences(
            isEnabled = true,
            targetSource = HaramTargetSource.MADINAH_ONLY,
            notificationMode = HaramNotificationMode.ONLY_PREFERRED_SURAHS,
            preferredSurahs = setOf(18)
        )

        val makkahNowPlaying = HaramNowPlaying(
            location = HaramLocation.MAKKAH,
            surahNumber = 18,
            surahName = "الكهف"
        )
        val madinahKahf = HaramNowPlaying(
            location = HaramLocation.MADINAH,
            surahNumber = 18,
            surahName = "الكهف"
        )
        val madinahBaqarah = HaramNowPlaying(
            location = HaramLocation.MADINAH,
            surahNumber = 2,
            surahName = "البقرة"
        )

        assertFalse("Should not allow Makkah when MADINAH_ONLY is selected", notificationManager.canSendNotification(makkahNowPlaying))
        assertTrue("Should allow Madinah Al-Kahf", notificationManager.canSendNotification(madinahKahf))
        assertFalse("Should reject non-preferred Surah", notificationManager.canSendNotification(madinahBaqarah))
    }

    @Test
    fun `createSurahAudioItem produces valid stream audio item with fallbacks`() {
        val nowPlaying = HaramNowPlaying(
            location = HaramLocation.MAKKAH,
            surahNumber = 2,
            surahName = "البقرة",
            reciterName = "أئمة الحرم المكي الشريف"
        )

        val audioItem = liveStreamManager.createSurahAudioItem(nowPlaying)
        assertEquals("haram_makkah_live", audioItem.reciterId)
        assertEquals(2, audioItem.surahNumber)
        assertEquals("البقرة", audioItem.surahName)
        assertTrue(audioItem.audioSources.isNotEmpty())
        assertTrue(audioItem.detailedSources.isNotEmpty())
    }
}
