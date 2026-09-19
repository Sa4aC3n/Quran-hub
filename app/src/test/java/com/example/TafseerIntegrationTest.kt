package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.provider.EmbeddedTafseerRepository
import com.example.data.provider.TafseerManager
import com.example.data.provider.TafseerUnavailableException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    fun `embedded tafseer returns authentic distinct commentaries for Al-Fatiha across books`() {
        // Tafseer 1: Al-Muyassar
        val muyassar = EmbeddedTafseerRepository.getEmbeddedTafseer(1, 1, 1)
        assertNotNull(muyassar)
        assertTrue(muyassar!!.text.contains("أبدأ قراءتي مستعيناً باسم الله"))

        // Tafseer 2: Jalalayn
        val jalalayn = EmbeddedTafseerRepository.getEmbeddedTafseer(2, 1, 1)
        assertNotNull(jalalayn)
        assertTrue(jalalayn!!.text.contains("سورة الفاتحة مكية"))

        // Tafseer 3: As-Sa'di
        val saadi = EmbeddedTafseerRepository.getEmbeddedTafseer(3, 1, 1)
        assertNotNull(saadi)
        assertTrue(saadi!!.text.contains("أبتدئ بكل اسم لله تعالى"))

        // Tafseer 4: Ibn Kathir
        val ibnKathir = EmbeddedTafseerRepository.getEmbeddedTafseer(4, 1, 1)
        assertNotNull(ibnKathir)
        assertTrue(ibnKathir!!.text.contains("يقال لها الفاتحة"))
    }

    @Test
    fun `embedded tafseer returns null for unverified verses and never synthesizes generic commentary`() {
        // Surah 55 Ayah 1 is not in the embedded hardcoded sample; must return null rather than generating fake commentary
        val result = EmbeddedTafseerRepository.getEmbeddedTafseer(
            tafseerId = 1,
            surahNumber = 55,
            ayahNumber = 1
        )
        assertNull("Must return null when authentic commentary is not embedded, never synthesize generic text", result)
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
    fun `getAyahTafseer for embedded verse returns success with authentic commentary`() = runBlocking {
        val result = tafseerManager.getAyahTafseer(
            tafseerId = 1,
            surahNumber = 1,
            ayahNumber = 2
        )
        assertTrue(result.isSuccess)
        val text = result.getOrNull()?.text
        assertNotNull(text)
        assertTrue(text!!.contains("الثناء على الله بصفاته"))
    }

    @Test
    fun `getAyahTafseer fails honestly when offline and book text is unavailable`() = runBlocking {
        // Without internet and not embedded, must return failure with TafseerUnavailableException
        val result = tafseerManager.getAyahTafseer(
            tafseerId = 8, // Tabari
            surahNumber = 77,
            ayahNumber = 50
        )
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is TafseerUnavailableException || exception!!.message!!.contains("التفسير غير متاح"))
    }
}
