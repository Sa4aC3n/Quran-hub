package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.download.AudioDownloadManager
import com.example.data.local.QuranRepository
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
}
