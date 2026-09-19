package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.download.AudioDownloadManager
import com.example.data.local.QuranRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ThemeColdStartUnitTest {

    @Test
    fun testThemeColdStart_DefaultIsSystem() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = QuranRepository(context, AudioDownloadManager(context))

        // Reset prefs for clean test
        context.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()

        val cachedTheme = repository.getCachedAppTheme()
        assertEquals("system", cachedTheme)
    }

    @Test
    fun testThemeColdStart_InstantRestoreWithoutAsyncDelay() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = QuranRepository(context, AudioDownloadManager(context))

        // 1. User selects "light" theme
        repository.setAppTheme("light")
        assertEquals("light", repository.getCachedAppTheme())

        // 2. User selects "dark" theme
        repository.setAppTheme("dark")
        assertEquals("dark", repository.getCachedAppTheme())

        // 3. User selects "system"
        repository.setAppTheme("system")
        assertEquals("system", repository.getCachedAppTheme())
    }

    @Test
    fun testUpgradeScenario_DataStoreHasDark_MirrorMissing() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = QuranRepository(context, AudioDownloadManager(context))

        // Set theme in DataStore first
        repository.setAppTheme("dark")

        // Simulate app upgrade: wipe the new app_theme_prefs mirror entirely
        context.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()

        // Immediate cold cached read returns safe default "system" without blocking or crude file scanning
        val initialCached = repository.getCachedAppTheme()
        assertEquals("system", initialCached)

        // Flow collection from DataStore authoritatively recovers "dark" and reconciles the mirror
        val flowValue = repository.appThemeFlow.first { it == "dark" }
        assertEquals("dark", flowValue)

        // Verify mirror is now populated with "dark"
        val healedCached = repository.getCachedAppTheme()
        assertEquals("dark", healedCached)
    }

    @Test
    fun testInvalidTheme_FallbackToSystem() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = QuranRepository(context, AudioDownloadManager(context))

        repository.setAppTheme("invalid_theme_value")
        assertEquals("system", repository.getCachedAppTheme())
    }

    @Test
    fun testMirrorMismatch_DataStoreReconcilesMirror() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = QuranRepository(context, AudioDownloadManager(context))

        // Legitimate setting in DataStore
        repository.setAppTheme("light")

        // Force a corrupted/stale value into the mirror SharedPreferences
        context.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE)
            .edit().putString("app_theme", "corrupted_stale").commit()

        // When DataStore flow is collected, it validates and heals the mirror
        val flowValue = repository.appThemeFlow.first()
        assertEquals("light", flowValue)

        // Verify mirror was healed
        val healedCached = context.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE)
            .getString("app_theme", null)
        assertEquals("light", healedCached)
    }
}
