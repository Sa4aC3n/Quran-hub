package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.provider.EmbeddedTafseerRepository
import com.example.data.provider.TafseerManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TafseerIntegrationTest {

    private lateinit var context: Context
    private lateinit var tafseerManager: TafseerManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("tafseer_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        tafseerManager = TafseerManager(context)
    }

    @Test
    fun `embedded tafseer returns authentic commentary for Surah At-Tawbah verse 2`() {
        val tafseer = EmbeddedTafseerRepository.getEmbeddedTafseer(
            tafseerId = 1,
            surahNumber = 9,
            ayahNumber = 2
        )
        assertNotNull(tafseer)
        assertTrue(tafseer!!.text.contains("أربعة أشهر") || tafseer.text.contains("المشركون"))
    }

    @Test
    fun `embedded tafseer returns authentic commentary for Surah Al-Mulk verse 2`() {
        val tafseer = EmbeddedTafseerRepository.getEmbeddedTafseer(
            tafseerId = 1,
            surahNumber = 67,
            ayahNumber = 2
        )
        assertNotNull(tafseer)
        assertTrue(tafseer!!.text.contains("الموت") && tafseer.text.contains("الحياة"))
    }

    @Test
    fun `tafseer manager returns available classical tafseer books`() = runBlocking {
        val books = tafseerManager.getAvailableTafseers()
        assertTrue(books.isNotEmpty())
        assertTrue(books.any { it.name.contains("الميسر") })
        assertTrue(books.any { it.name.contains("السعدي") })
        assertTrue(books.any { it.name.contains("الجلالين") })
        assertTrue(books.any { it.name.contains("ابن كثير") })
    }

    @Test
    fun `getAyahTafseer fallback provides authentic tafseer without throwing error`() = runBlocking {
        val result = tafseerManager.getAyahTafseer(
            tafseerId = 1,
            surahNumber = 9,
            ayahNumber = 2,
            cleanAyahText = "فَسِيحُواْ فِي ٱلۡأَرۡضِ أَرۡبَعَةَ أَشۡهُرٖ"
        )
        assertTrue(result.isSuccess)
        val text = result.getOrNull()?.text
        assertNotNull(text)
        assertTrue(text!!.isNotBlank())
    }
}
