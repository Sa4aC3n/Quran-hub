package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Ayah
import com.example.data.model.SurahText
import com.example.data.provider.PersistenceResult
import com.example.data.provider.QuranManifest
import com.example.data.provider.QuranTextProvider
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class QuranTextPersistenceTest {

    private lateinit var context: Context
    private lateinit var provider: QuranTextProvider
    private lateinit var storageDir: File
    private val gson = Gson()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        provider = QuranTextProvider(context)
        storageDir = File(context.filesDir, "quran_text_v2")
        storageDir.deleteRecursively()
        storageDir.mkdirs()
    }

    private fun createSampleFatiha(): SurahText {
        return SurahText(
            number = 1,
            name = "الفاتحة",
            englishName = "Al-Fatihah",
            englishNameTranslation = "The Opening",
            revelationType = "مكية",
            numberOfAyahs = 7,
            ayahs = listOf(
                Ayah(1, 1, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", 1, 1, 1, 1, 1, false),
                Ayah(2, 2, "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ", 1, 1, 1, 1, 1, false),
                Ayah(3, 3, "الرَّحْمَٰنِ الرَّحِيمِ", 1, 1, 1, 1, 1, false),
                Ayah(4, 4, "مَالِكِ يَوْمِ الدِّينِ", 1, 1, 1, 1, 1, false),
                Ayah(5, 5, "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", 1, 1, 1, 1, 1, false),
                Ayah(6, 6, "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ", 1, 1, 1, 1, 1, false),
                Ayah(7, 7, "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ", 1, 1, 1, 1, 1, false)
            )
        )
    }

    @Test
    fun testSaveAndReadSurah_SuccessWithSchema2AndCompleteProvenance() {
        val fatiha = createSampleFatiha()
        val result = provider.saveSurahToDiskAtomic(
            surah = fatiha,
            expectedSurahNumber = 1,
            sourceProvider = "King Fahd Complex",
            editionIdentifier = "Mushaf al-Madinah 1430H",
            datasetVersion = "1430H-v1"
        )
        assertTrue("Atomic save must succeed", result is PersistenceResult.Success)

        val file = File(storageDir, "surah_1.json")
        assertTrue(file.exists())

        val readBack = provider.readSurahFromFile(file, 1)
        assertNotNull("Must successfully read back verified surah", readBack)
        assertEquals(7, readBack!!.ayahs.size)
        assertEquals("الفاتحة", readBack.name)
    }

    @Test
    fun testReadSurah_RejectsUnversionedOrV1Schema() {
        val fatiha = createSampleFatiha()
        val file = File(storageDir, "surah_1.json")

        // 1. Write envelope without schema version
        val unversioned = JsonObject().apply {
            addProperty("surahNumber", 1)
            add("surahText", gson.toJsonTree(fatiha))
        }
        file.writeText(gson.toJson(unversioned))
        assertNull("Must reject unversioned content", provider.readSurahFromFile(file, 1))

        // 2. Write envelope with schemaVersion = 1
        val v1Envelope = JsonObject().apply {
            addProperty("schemaVersion", 1)
            addProperty("surahNumber", 1)
            add("surahText", gson.toJsonTree(fatiha))
        }
        file.writeText(gson.toJson(v1Envelope))
        assertNull("Must reject legacy v1 unverified schema", provider.readSurahFromFile(file, 1))
    }

    @Test
    fun testReadSurah_RejectsMissingProvenance() {
        val fatiha = createSampleFatiha()
        val file = File(storageDir, "surah_1.json")

        // Missing sourceProvider
        val missingProvider = JsonObject().apply {
            addProperty("schema_version", 2)
            addProperty("surah_number", 1)
            addProperty("edition_identifier", "test_edition")
            addProperty("dataset_version", "v1")
            add("surah_text", gson.toJsonTree(fatiha))
        }
        file.writeText(gson.toJson(missingProvider))
        assertNull("Must reject when source_provider is missing", provider.readSurahFromFile(file, 1))

        // Empty dataset_version
        val emptyVersion = JsonObject().apply {
            addProperty("schema_version", 2)
            addProperty("surah_number", 1)
            addProperty("source_provider", "King Fahd Complex")
            addProperty("edition_identifier", "test_edition")
            addProperty("dataset_version", "")
            add("surah_text", gson.toJsonTree(fatiha))
        }
        file.writeText(gson.toJson(emptyVersion))
        assertNull("Must reject when dataset_version is empty", provider.readSurahFromFile(file, 1))
    }

    @Test
    fun testSaveSurah_RejectsBlankProvenanceParameters() {
        val fatiha = createSampleFatiha()
        val result = provider.saveSurahToDiskAtomic(
            surah = fatiha,
            expectedSurahNumber = 1,
            sourceProvider = "",
            editionIdentifier = "edition",
            datasetVersion = "v1"
        )
        assertTrue("Must fail when sourceProvider is blank", result is PersistenceResult.Failure)
    }

    @Test
    fun testReadSurah_RejectsSurahNumberMismatch() {
        val fatiha = createSampleFatiha()
        val file = File(storageDir, "surah_1.json")

        val envelope = JsonObject().apply {
            addProperty("schema_version", 2)
            addProperty("surah_number", 2) // Mismatched!
            addProperty("source_provider", "King Fahd Complex")
            addProperty("edition_identifier", "Mushaf al-Madinah")
            addProperty("dataset_version", "v1")
            add("surah_text", gson.toJsonTree(fatiha))
        }
        file.writeText(gson.toJson(envelope))
        assertNull("Must reject when envelope surah number does not match expected", provider.readSurahFromFile(file, 1))
    }
}
