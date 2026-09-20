package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.download.AudioDownloadManager
import com.example.data.local.QuranRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ThemeColdStartUnitTest {

    private lateinit var context: Context
    private lateinit var repository: QuranRepository

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        repository = QuranRepository(context, AudioDownloadManager(context))
        repository.setAppTheme("system")
    }

    @Test
    fun testThemeColdStart_DefaultIsSystem() = runBlocking {
        context.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        val freshRepo = QuranRepository(context, AudioDownloadManager(context))
        val cachedTheme = freshRepo.getCachedAppTheme()
        assertEquals("system", cachedTheme)
    }

    @Test
    fun testThemeColdStart_InstantRestoreWithoutAsyncDelay() = runBlocking {
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
        // Set theme in DataStore first
        repository.setAppTheme("dark")

        // Simulate app upgrade: wipe the new app_theme_prefs mirror entirely
        context.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()

        // Immediate cold cached read returns safe default "system" without blocking or crude file scanning
        val initialCached = repository.getCachedAppTheme()
        assertEquals("system", initialCached)

        // Pipeline reinitialization recovers "dark" and reconciles the mirror
        repository.retryThemeInitialization()
        val state = repository.themeInitializationState.first { it is com.example.data.local.ThemeInitializationState.Ready && it.theme == "dark" }
        assertEquals("dark", (state as com.example.data.local.ThemeInitializationState.Ready).theme)

        // Verify mirror is now populated with "dark"
        val healedCached = repository.getCachedAppTheme()
        assertEquals("dark", healedCached)
    }

    @Test
    fun testInvalidTheme_FallbackToSystem() = runBlocking {
        repository.setAppTheme("invalid_theme_value")
        assertEquals("system", repository.getCachedAppTheme())
    }

    @Test
    fun testMirrorMismatch_DataStoreReconcilesMirror() = runBlocking {
        // Legitimate setting in DataStore
        repository.setAppTheme("light")

        // Force a corrupted/stale value into the mirror SharedPreferences
        context.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE)
            .edit().putString("app_theme", "corrupted_stale").commit()

        // Reconcile via retryThemeInitialization
        repository.retryThemeInitialization()
        val state = repository.themeInitializationState.first { it is com.example.data.local.ThemeInitializationState.Ready && it.theme == "light" }
        assertEquals("light", (state as com.example.data.local.ThemeInitializationState.Ready).theme)

        // Verify mirror was healed
        val healedCached = context.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE)
            .getString("app_theme", null)
        assertEquals("light", healedCached)
    }

    @Test
    fun testThemeInitializationState_ReachesReady() = runBlocking {
        repository.setAppTheme("dark")

        val state = repository.themeInitializationState.first { it is com.example.data.local.ThemeInitializationState.Ready }
        assertTrue(state is com.example.data.local.ThemeInitializationState.Ready)
        assertEquals("dark", (state as com.example.data.local.ThemeInitializationState.Ready).theme)
    }

    @Test
    fun testRetryThemeInitialization_RecoversState() = runBlocking {
        repository.retryThemeInitialization()

        val state = repository.themeInitializationState.first { it is com.example.data.local.ThemeInitializationState.Ready }
        assertTrue(state is com.example.data.local.ThemeInitializationState.Ready)
    }
}
