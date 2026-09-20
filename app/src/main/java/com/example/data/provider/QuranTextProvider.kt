package com.example.data.provider

import android.content.Context
import com.example.data.model.Ayah
import com.example.data.model.SurahText
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

sealed class PersistenceResult {
    data class Success(val surahNumber: Int, val file: File) : PersistenceResult()
    data class Failure(val surahNumber: Int, val reason: String) : PersistenceResult()
}

class QuranTextUnavailableException(val surahNumber: Int, message: String) : Exception(message)

/**
 * Storage envelope for authentic Quran surahs ensuring schema evolution safety.
 */
data class SurahStorageEnvelope(
    @SerializedName("schema_version", alternate = ["schemaVersion"]) val schemaVersion: Int = 2,
    @SerializedName("surah_number", alternate = ["surahNumber"]) val surahNumber: Int,
    @SerializedName("source_provider", alternate = ["sourceProvider"]) val sourceProvider: String,
    @SerializedName("edition_identifier", alternate = ["editionIdentifier"]) val editionIdentifier: String,
    @SerializedName("dataset_version", alternate = ["datasetVersion"]) val datasetVersion: String,
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    @SerializedName("surah_text", alternate = ["surahText"]) val surahText: SurahText
)

data class CachedSurah(
    val surahText: SurahText,
    val sourceProvider: String,
    val editionIdentifier: String,
    val datasetVersion: String
)

data class FetchedSurahPayload(
    val surahText: SurahText,
    val sourceProvider: String,
    val editionIdentifier: String,
    val datasetVersion: String
)

/**
 * Authentic, verified Quran text provider with atomic disk persistence compatible with API 24/25+.
 * Validates all texts against the canonical QuranManifest (114 surahs, strict verse counts and sequencing).
 * Never generates synthetic or placeholder verses upon network or parsing failure.
 */
class QuranTextProvider(private val context: Context) {

    private val cache = ConcurrentHashMap<Int, CachedSurah>()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    private val storageDir: File by lazy {
        File(context.filesDir, "quran_text_v2").apply { mkdirs() }
    }

    init {
        // Clean up legacy unverified v1 cache if present
        try {
            val legacyCache = File(context.cacheDir, "quran_text_cache")
            if (legacyCache.exists()) {
                legacyCache.deleteRecursively()
            }
        } catch (_: Exception) {}
    }

    /**
     * Reads and authenticates a surah text file using Android AtomicFile.
     * Enforces lock protection, opens through AtomicFile.openRead without pre-checking file existence,
     * and strictly validates schemaVersion, envelope identity, provider, and canonical text invariants.
     */
    fun readSurahFromFile(file: File, expectedSurahNumber: Int): SurahText? {
        val lock = StorageLockManager.getLockFor(file)
        return synchronized(lock) {
            try {
                val atomicFile = android.util.AtomicFile(file)
                val json = try {
                    atomicFile.openRead().use { stream ->
                        stream.bufferedReader(Charsets.UTF_8).readText()
                    }
                } catch (_: java.io.FileNotFoundException) {
                    return@synchronized null
                } catch (_: Exception) {
                    return@synchronized null
                }

                if (json.isBlank()) return@synchronized null

                val jsonObject = try {
                    gson.fromJson(json, JsonObject::class.java)
                } catch (_: Exception) {
                    null
                } ?: return@synchronized null

                val schemaVersion = when {
                    jsonObject.has("schema_version") -> jsonObject.get("schema_version").asInt
                    jsonObject.has("schemaVersion") -> jsonObject.get("schemaVersion").asInt
                    else -> return@synchronized null // Reject unversioned content
                }

                if (schemaVersion != 2) {
                    return@synchronized null // Reject unsupported schemas and v1 unverified migrations
                }

                val envSurahNum = (jsonObject.get("surah_number") ?: jsonObject.get("surahNumber"))?.asInt ?: return@synchronized null
                if (envSurahNum != expectedSurahNumber) return@synchronized null

                val provider = (jsonObject.get("source_provider") ?: jsonObject.get("sourceProvider"))?.asString
                if (provider.isNullOrBlank()) return@synchronized null

                val edition = (jsonObject.get("edition_identifier") ?: jsonObject.get("editionIdentifier"))?.asString
                if (edition.isNullOrBlank()) return@synchronized null

                val datasetVer = (jsonObject.get("dataset_version") ?: jsonObject.get("datasetVersion"))?.asString
                if (datasetVer.isNullOrBlank()) return@synchronized null

                val surahTextElem = jsonObject.get("surah_text") ?: jsonObject.get("surahText") ?: return@synchronized null
                val surah = gson.fromJson(surahTextElem, SurahText::class.java)

                if (surah != null &&
                    surah.number == expectedSurahNumber &&
                    QuranManifest.validateSurah(surah, expectedSurahNumber) is QuranManifest.ValidationResult.Valid
                ) {
                    cache[expectedSurahNumber] = CachedSurah(
                        surahText = surah,
                        sourceProvider = provider,
                        editionIdentifier = edition,
                        datasetVersion = datasetVer
                    )
                    surah
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Checks whether the authentic surah text is already stored locally on disk
     * and strictly matches expectedSurahNumber.
     */
    fun isSurahDownloaded(surahNumber: Int, expectedSurahNumber: Int = surahNumber): Boolean {
        if (surahNumber != expectedSurahNumber || surahNumber !in 1..QuranManifest.TOTAL_SURAHS) return false
        val file = File(storageDir, "surah_$surahNumber.json")
        return readSurahFromFile(file, expectedSurahNumber) != null
    }

    /**
     * Returns the set of verified downloaded surah numbers (1..114).
     */
    fun getVerifiedDownloadedSurahNumbers(): Set<Int> {
        val result = mutableSetOf<Int>()
        for (i in 1..QuranManifest.TOTAL_SURAHS) {
            if (isSurahDownloaded(i, i)) {
                result.add(i)
            }
        }
        return result
    }

    /**
     * Returns the total count of fully downloaded and verified surahs.
     */
    fun getDownloadedSurahsCount(): Int {
        return getVerifiedDownloadedSurahNumbers().size
    }

    /**
     * Ensures that a surah is permanently and authentically persisted on disk.
     * Never considers memory cache alone as proof of offline download.
     */
    suspend fun ensureSurahPersisted(surahNumber: Int, expectedSurahNumber: Int = surahNumber): PersistenceResult = withContext(Dispatchers.IO) {
        if (surahNumber != expectedSurahNumber || surahNumber !in 1..QuranManifest.TOTAL_SURAHS) {
            return@withContext PersistenceResult.Failure(surahNumber, "رقم السورة غير متطابق أو غير صالح")
        }

        // 1. Check if already properly persisted and verified on disk
        if (isSurahDownloaded(surahNumber, expectedSurahNumber)) {
            val file = File(storageDir, "surah_$surahNumber.json")
            return@withContext PersistenceResult.Success(surahNumber, file)
        }

        // 2. If present in memory cache, attempt to persist it and verify
        cache[surahNumber]?.let { memCached ->
            val result = saveSurahToDiskAtomic(
                surah = memCached.surahText,
                expectedSurahNumber = expectedSurahNumber,
                sourceProvider = memCached.sourceProvider,
                editionIdentifier = memCached.editionIdentifier,
                datasetVersion = memCached.datasetVersion
            )
            if (result is PersistenceResult.Success) {
                return@withContext result
            }
        }

        // 3. Check bundled authentic curated baseline
        loadCuratedBundledSurah(surahNumber)?.let { bundled ->
            val result = saveSurahToDiskAtomic(
                surah = bundled,
                expectedSurahNumber = expectedSurahNumber,
                sourceProvider = "King Fahd Complex for Printing the Holy Quran",
                editionIdentifier = "Mushaf al-Madinah 1430H",
                datasetVersion = "1430H-v1"
            )
            if (result is PersistenceResult.Success) {
                return@withContext result
            }
        }

        // 4. Fetch full Uthmani text online and persist
        val remote = fetchSurahFromRemote(surahNumber)
        if (remote != null) {
            return@withContext saveSurahToDiskAtomic(
                surah = remote.surahText,
                expectedSurahNumber = expectedSurahNumber,
                sourceProvider = remote.sourceProvider,
                editionIdentifier = remote.editionIdentifier,
                datasetVersion = remote.datasetVersion
            )
        }

        PersistenceResult.Failure(surahNumber, "تعذر جلب نص السورة الكريمة لحفظها دون اتصال")
    }

    /**
     * Retrieves the authentic text for a surah:
     * 1. Checks memory cache.
     * 2. Checks verified atomic persistent disk storage.
     * 3. Checks verified bundled seeds for key landmark surahs.
     * 4. Fetches online from authoritative Quran APIs, validates against QuranManifest, and persists atomically.
     * Throws QuranTextUnavailableException if unavailable. Never returns synthetic fallback.
     */
    suspend fun getSurahText(surahNumber: Int): SurahText = withContext(Dispatchers.IO) {
        if (surahNumber !in 1..QuranManifest.TOTAL_SURAHS) {
            throw QuranTextUnavailableException(surahNumber, "رقم السورة $surahNumber غير صالح")
        }

        // 1. Check in-memory verified cache
        cache[surahNumber]?.let { cached ->
            if (QuranManifest.validateSurah(cached.surahText, surahNumber) is QuranManifest.ValidationResult.Valid) {
                return@withContext cached.surahText
            } else {
                cache.remove(surahNumber)
            }
        }

        // 2. Check atomic local disk storage
        val localFile = File(storageDir, "surah_$surahNumber.json")
        val localSurah = readSurahFromFile(localFile, surahNumber)
        if (localSurah != null) {
            return@withContext localSurah
        }

        // 3. Check bundled authentic curated baseline for offline opening
        loadCuratedBundledSurah(surahNumber)?.let { bundled ->
            if (QuranManifest.validateSurah(bundled, surahNumber) is QuranManifest.ValidationResult.Valid) {
                val provider = "King Fahd Complex for Printing the Holy Quran"
                val edition = "Mushaf al-Madinah 1430H"
                val version = "1430H-v1"
                saveSurahToDiskAtomic(
                    surah = bundled,
                    expectedSurahNumber = surahNumber,
                    sourceProvider = provider,
                    editionIdentifier = edition,
                    datasetVersion = version
                )
                cache[surahNumber] = CachedSurah(
                    surahText = bundled,
                    sourceProvider = provider,
                    editionIdentifier = edition,
                    datasetVersion = version
                )
                return@withContext bundled
            }
        }

        // 4. Fetch full Uthmani text via primary and secondary Quran Cloud APIs
        val fetchedPayload = fetchSurahFromRemote(surahNumber)
        if (fetchedPayload != null) {
            val validation = QuranManifest.validateSurah(fetchedPayload.surahText, surahNumber)
            if (validation is QuranManifest.ValidationResult.Valid) {
                saveSurahToDiskAtomic(
                    surah = fetchedPayload.surahText,
                    expectedSurahNumber = surahNumber,
                    sourceProvider = fetchedPayload.sourceProvider,
                    editionIdentifier = fetchedPayload.editionIdentifier,
                    datasetVersion = fetchedPayload.datasetVersion
                )
                cache[surahNumber] = CachedSurah(
                    surahText = fetchedPayload.surahText,
                    sourceProvider = fetchedPayload.sourceProvider,
                    editionIdentifier = fetchedPayload.editionIdentifier,
                    datasetVersion = fetchedPayload.datasetVersion
                )
                return@withContext fetchedPayload.surahText
            }
        }

        // 5. If completely unavailable, throw explicit exception
        throw QuranTextUnavailableException(
            surahNumber = surahNumber,
            message = "تعذر تحميل نص سورة ${QuranManifest.getSurahNameArabic(surahNumber)} دون اتصال بالإنترنت"
        )
    }

    private fun fetchSurahFromRemote(surahNumber: Int): FetchedSurahPayload? {
        val urls = listOf(
            Triple("https://api.alquran.cloud/v1/surah/$surahNumber/quran-uthmani", "api.alquran.cloud", "quran-uthmani"),
            Triple("https://api.alquran.cloud/v1/surah/$surahNumber/ar.alafasy", "api.alquran.cloud", "ar.alafasy")
        )

        for ((url, provider, edition) in urls) {
            try {
                val request = Request.Builder().url(url).build()
                val response = httpClient.newCall(request).execute()
                response.use { res ->
                    if (res.isSuccessful) {
                        val body = res.body?.string()
                        if (!body.isNullOrBlank()) {
                            val root = gson.fromJson(body, JsonObject::class.java)
                            if (root.has("data")) {
                                val dataObj = root.getAsJsonObject("data")
                                val sNum = dataObj.get("number").asInt
                                if (sNum != surahNumber) return@use // Mismatched surah response; reject

                                val sName = dataObj.get("name").asString
                                val englishName = dataObj.get("englishName").asString
                                val revType = dataObj.get("revelationType").asString
                                val count = dataObj.get("numberOfAyahs").asInt
                                val canonicalCount = QuranManifest.getCanonicalAyahCount(surahNumber)

                                if (count != canonicalCount) return@use // Incomplete count; reject

                                val ayahsArray = dataObj.getAsJsonArray("ayahs")
                                if (ayahsArray.size() != canonicalCount) return@use // Incomplete payload; reject

                                val ayahsList = mutableListOf<Ayah>()
                                for (i in 0 until ayahsArray.size()) {
                                    val aObj = ayahsArray.get(i).asJsonObject
                                    val ayahNumber = aObj.get("number").asInt
                                    val numInSurah = aObj.get("numberInSurah").asInt
                                    var text = aObj.get("text").asString
                                    val juz = if (aObj.has("juz")) aObj.get("juz").asInt else 1
                                    val page = if (aObj.has("page")) aObj.get("page").asInt else 1
                                    val hizbQuarter = if (aObj.has("hizbQuarter")) aObj.get("hizbQuarter").asInt else 1
                                    val sajda = aObj.get("sajda")?.let {
                                        if (it.isJsonPrimitive && it.asJsonPrimitive.isBoolean) it.asBoolean else false
                                    } ?: false

                                    // Clean prepended Bismillah for surahs 2..8 and 10..114 on verse 1
                                    if (numInSurah == 1 && sNum != 1) {
                                        text = cleanBismillahFromVerse(text, sNum, numInSurah)
                                    }

                                    ayahsList.add(
                                        Ayah(
                                            number = ayahNumber,
                                            numberInSurah = numInSurah,
                                            text = text,
                                            juz = juz,
                                            page = page,
                                            hizbQuarter = hizbQuarter,
                                            sajda = sajda,
                                            tafsir = null // Authentic tafseer is handled independently by book
                                        )
                                    )
                                }

                                val result = SurahText(
                                    number = sNum,
                                    name = sName,
                                    englishName = englishName,
                                    englishNameTranslation = revType,
                                    revelationType = if (revType.contains("Meccan", true)) "مكية" else "مدنية",
                                    numberOfAyahs = canonicalCount,
                                    ayahs = ayahsList
                                )

                                if (QuranManifest.validateSurah(result) is QuranManifest.ValidationResult.Valid) {
                                    return FetchedSurahPayload(
                                        surahText = result,
                                        sourceProvider = provider,
                                        editionIdentifier = edition,
                                        datasetVersion = "UNKNOWN"
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return null
    }

    /**
     * Clears in-memory verified cache.
     */
    fun clearCache() {
        cache.clear()
    }

    /**
     * Deletes the persistent file for a surah (for testing and cleanup).
     */
    fun deleteSurahFile(surahNumber: Int): Boolean {
        val targetFile = File(storageDir, "surah_$surahNumber.json")
        val lock = StorageLockManager.getLockFor(targetFile)
        return synchronized(lock) {
            cache.remove(surahNumber)
            try {
                android.util.AtomicFile(targetFile).delete()
                true
            } catch (_: Exception) {
                targetFile.delete()
            }
        }
    }

    /**
     * Atomically writes the validated surah to persistent disk storage using Android AtomicFile (API 24/25+ compatible).
     * Enforces strict read-back verification and never leaves corrupted or partial files.
     */
    fun saveSurahToDiskAtomic(
        surah: SurahText,
        expectedSurahNumber: Int,
        sourceProvider: String,
        editionIdentifier: String,
        datasetVersion: String
    ): PersistenceResult {
        if (sourceProvider.isBlank() || editionIdentifier.isBlank() || datasetVersion.isBlank()) {
            return PersistenceResult.Failure(surah.number, "بيانات مصدر السورة (المزود/الطبعة/الإصدار) غير مكتملة")
        }
        val validation = QuranManifest.validateSurah(surah, expectedSurahNumber)
        if (validation !is QuranManifest.ValidationResult.Valid) {
            val reason = (validation as? QuranManifest.ValidationResult.Invalid)?.reason ?: "بيانات السورة غير صالحة"
            return PersistenceResult.Failure(surah.number, reason)
        }
        val targetFile = File(storageDir, "surah_${surah.number}.json")
        val lock = StorageLockManager.getLockFor(targetFile)

        synchronized(lock) {
            val atomicFile = android.util.AtomicFile(targetFile)
            var fos: FileOutputStream? = null
            try {
                val envelope = SurahStorageEnvelope(
                    schemaVersion = 2,
                    surahNumber = surah.number,
                    sourceProvider = sourceProvider,
                    editionIdentifier = editionIdentifier,
                    datasetVersion = datasetVersion,
                    timestamp = System.currentTimeMillis(),
                    surahText = surah
                )
                val json = gson.toJson(envelope)
                val bytes = json.toByteArray(Charsets.UTF_8)

                fos = atomicFile.startWrite()
                fos.write(bytes)
                fos.flush()
                fos.fd.sync()
                atomicFile.finishWrite(fos)
                fos = null
            } catch (e: Exception) {
                if (fos != null) {
                    atomicFile.failWrite(fos)
                }
                return PersistenceResult.Failure(surah.number, "خطأ أثناء حفظ السورة على القرص: ${e.message}")
            }

            // Read-back verification from targetFile to ensure full integrity
            val readBack = readSurahFromFile(targetFile, expectedSurahNumber)
            return if (readBack != null) {
                cache[surah.number] = CachedSurah(
                    surahText = readBack,
                    sourceProvider = sourceProvider,
                    editionIdentifier = editionIdentifier,
                    datasetVersion = datasetVersion
                )
                PersistenceResult.Success(surah.number, targetFile)
            } else {
                PersistenceResult.Failure(surah.number, "فشل التحقق من صحة الملف بعد الحفظ الدائم والقراءة")
            }
        }
    }

    private fun loadCuratedBundledSurah(surahNumber: Int): SurahText? {
        // Authentic verified baseline texts for landmark surahs
        return when (surahNumber) {
            1 -> SurahText(
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
            112 -> SurahText(
                number = 112,
                name = "الإخلاص",
                englishName = "Al-Ikhlas",
                englishNameTranslation = "Sincerity",
                revelationType = "مكية",
                numberOfAyahs = 4,
                ayahs = listOf(
                    Ayah(6222, 1, "قُلْ هُوَ اللَّهُ أَحَدٌ", 30, 7, 604, 1, 240, false),
                    Ayah(6223, 2, "اللَّهُ الصَّمَدُ", 30, 7, 604, 1, 240, false),
                    Ayah(6224, 3, "لَمْ يَلِدْ وَلَمْ يُولَدْ", 30, 7, 604, 1, 240, false),
                    Ayah(6225, 4, "وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ", 30, 7, 604, 1, 240, false)
                )
            )
            113 -> SurahText(
                number = 113,
                name = "الفلق",
                englishName = "Al-Falaq",
                englishNameTranslation = "Daybreak",
                revelationType = "مكية",
                numberOfAyahs = 5,
                ayahs = listOf(
                    Ayah(6226, 1, "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ", 30, 7, 604, 1, 240, false),
                    Ayah(6227, 2, "مِن شَرِّ مَا خَلَقَ", 30, 7, 604, 1, 240, false),
                    Ayah(6228, 3, "وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ", 30, 7, 604, 1, 240, false),
                    Ayah(6229, 4, "وَمِن شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ", 30, 7, 604, 1, 240, false),
                    Ayah(6230, 5, "وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ", 30, 7, 604, 1, 240, false)
                )
            )
            114 -> SurahText(
                number = 114,
                name = "الناس",
                englishName = "An-Nas",
                englishNameTranslation = "Mankind",
                revelationType = "مكية",
                numberOfAyahs = 6,
                ayahs = listOf(
                    Ayah(6231, 1, "قُلْ أَعُوذُ بِرَبِّ النَّاسِ", 30, 7, 604, 1, 240, false),
                    Ayah(6232, 2, "مَلِكِ النَّاسِ", 30, 7, 604, 1, 240, false),
                    Ayah(6233, 3, "إِلَٰهِ النَّاسِ", 30, 7, 604, 1, 240, false),
                    Ayah(6234, 4, "مِن شَرِّ الْوَسْوَاسِ الْخَنَّاسِ", 30, 7, 604, 1, 240, false),
                    Ayah(6235, 5, "الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ", 30, 7, 604, 1, 240, false),
                    Ayah(6236, 6, "مِنَ الْجِنَّةِ وَالنَّاسِ", 30, 7, 604, 1, 240, false)
                )
            )
            else -> null
        }
    }

    companion object {
        /**
         * Cleans prefixed Bismillah from verse text when received from certain digital APIs.
         * Rules:
         * - Surah 1 (Al-Fatiha): Verse 1 IS the Bismillah. Never stripped!
         * - Surah 9 (At-Tawbah): Does not have Bismillah. Never stripped!
         * - Surah 27 (An-Naml), Ayah 30: Contains Bismillah inside the ayah narrative. Never stripped!
         * - Other surahs: Only the first verse (ayahNumberInSurah == 1) has its prefixed Bismillah stripped.
         */
        fun cleanBismillahFromVerse(rawText: String, surahNumber: Int = 2, ayahNumberInSurah: Int = 1): String {
            if (ayahNumberInSurah != 1 || surahNumber == 1) return rawText.trim()
            val text = rawText.trim()

            val knownPrefixes = listOf(
                "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
                "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ",
                "بِسْمِ ٱللَّهِ ٱلرَّحْمٰنِ ٱلرَّحِيمِ",
                "بِسْمِ اللَّهِ الرَّحْمٰنِ الرَّحِيمِ",
                "بِسْمِ اللهِ الرَّحْمٰنِ الرَّحِيمِ",
                "بِسْمِ اللهِ الرَّحْمَنِ الرَّحِيمِ",
                "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                "بِسْمِ ٱللَّهِ ٱلرَّحْمَـنِ ٱلرَّحِيمِ",
                "بسم الله الرحمن الرحيم"
            )

            for (prefix in knownPrefixes) {
                if (text.startsWith(prefix)) {
                    val candidate = text.substring(prefix.length).trim()
                    if (candidate.isNotEmpty()) return candidate
                }
            }

            // Normalization-based fallback
            val normalized = normalizeArabic(text)
            val normPrefix = "بسم الله الرحمن الرحيم"
            if (normalized.startsWith(normPrefix)) {
                var baseCount = 0
                val targetBaseCount = normPrefix.replace(" ", "").length
                var cutIndex = -1
                for (i in text.indices) {
                    val char = text[i]
                    if (isArabicLetter(char)) {
                        baseCount++
                        if (baseCount == targetBaseCount) {
                            cutIndex = i + 1
                            break
                        }
                    }
                }
                if (cutIndex in 1 until text.length) {
                    val candidate = text.substring(cutIndex).trim()
                    if (candidate.isNotEmpty()) return candidate
                }
            }

            return text
        }

        private fun normalizeArabic(input: String): String {
            return input
                .replace(Regex("[\u064B-\u065F\u0670\u06D6-\u06ED]"), "")
                .replace("ٱ", "ا")
                .replace("أ", "ا")
                .replace("إ", "ا")
                .replace("آ", "ا")
                .replace("ـ", "")
                .trim()
        }

        private fun isArabicLetter(c: Char): Boolean {
            return c in '\u0621'..'\u064A' || c == '\u0671' || c == 'ٱ'
        }
    }
}
