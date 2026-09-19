package com.example.data.provider

import android.content.Context
import com.example.data.model.Ayah
import com.example.data.model.SurahText
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class QuranTextUnavailableException(val surahNumber: Int, message: String) : Exception(message)

/**
 * Authentic, verified Quran text provider with atomic disk persistence.
 * Validates all texts against the canonical QuranManifest (114 surahs, strict verse counts and sequencing).
 * Never generates synthetic or placeholder verses upon network or parsing failure.
 */
class QuranTextProvider(private val context: Context) {

    private val cache = ConcurrentHashMap<Int, SurahText>()
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
     * Checks whether the authentic surah text is already stored locally on disk.
     */
    fun isSurahDownloaded(surahNumber: Int): Boolean {
        if (surahNumber !in 1..QuranManifest.TOTAL_SURAHS) return false
        val file = File(storageDir, "surah_$surahNumber.json")
        if (!file.exists() || file.length() == 0L) return false
        return try {
            val text = file.readText()
            val surah = gson.fromJson(text, SurahText::class.java) ?: return false
            QuranManifest.validateSurah(surah) is QuranManifest.ValidationResult.Valid
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Returns the total count of fully downloaded and verified surahs.
     */
    fun getDownloadedSurahsCount(): Int {
        var count = 0
        for (i in 1..QuranManifest.TOTAL_SURAHS) {
            if (isSurahDownloaded(i)) count++
        }
        return count
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
            if (QuranManifest.validateSurah(cached) is QuranManifest.ValidationResult.Valid) {
                return@withContext cached
            } else {
                cache.remove(surahNumber)
            }
        }

        // 2. Check atomic local disk storage
        val localFile = File(storageDir, "surah_$surahNumber.json")
        if (localFile.exists() && localFile.length() > 0L) {
            try {
                val json = localFile.readText()
                val localSurah = gson.fromJson(json, SurahText::class.java)
                if (localSurah != null) {
                    val validation = QuranManifest.validateSurah(localSurah)
                    if (validation is QuranManifest.ValidationResult.Valid) {
                        cache[surahNumber] = localSurah
                        return@withContext localSurah
                    } else {
                        // File corrupted or tampered; purge it
                        localFile.delete()
                    }
                }
            } catch (_: Exception) {
                localFile.delete()
            }
        }

        // 3. Check bundled authentic curated baseline for offline opening
        loadCuratedBundledSurah(surahNumber)?.let { bundled ->
            if (QuranManifest.validateSurah(bundled) is QuranManifest.ValidationResult.Valid) {
                saveSurahToDiskAtomic(bundled)
                cache[surahNumber] = bundled
                return@withContext bundled
            }
        }

        // 4. Fetch full Uthmani text via primary and secondary Quran Cloud APIs
        val fetchedSurah = fetchSurahFromRemote(surahNumber)
        if (fetchedSurah != null) {
            val validation = QuranManifest.validateSurah(fetchedSurah)
            if (validation is QuranManifest.ValidationResult.Valid) {
                saveSurahToDiskAtomic(fetchedSurah)
                cache[surahNumber] = fetchedSurah
                return@withContext fetchedSurah
            }
        }

        // 5. If completely unavailable, throw explicit exception
        throw QuranTextUnavailableException(
            surahNumber = surahNumber,
            message = "تعذر تحميل نص سورة ${QuranManifest.getSurahNameArabic(surahNumber)} دون اتصال بالإنترنت"
        )
    }

    private fun fetchSurahFromRemote(surahNumber: Int): SurahText? {
        val urls = listOf(
            "https://api.alquran.cloud/v1/surah/$surahNumber/quran-uthmani",
            "https://api.alquran.cloud/v1/surah/$surahNumber/ar.alafasy"
        )

        for (url in urls) {
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
                                    return result
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
     * Atomically writes the validated surah to persistent disk storage.
     * Uses a temporary file (.tmp) and an atomic rename to prevent partial/corrupt files.
     */
    fun saveSurahToDiskAtomic(surah: SurahText): Boolean {
        if (QuranManifest.validateSurah(surah) !is QuranManifest.ValidationResult.Valid) {
            return false
        }
        val targetFile = File(storageDir, "surah_${surah.number}.json")
        val tempFile = File(storageDir, "surah_${surah.number}.tmp")

        return try {
            val json = gson.toJson(surah)
            FileOutputStream(tempFile).use { fos ->
                fos.write(json.toByteArray(Charsets.UTF_8))
                fos.flush()
                fos.fd.sync()
            }
            if (tempFile.exists() && tempFile.length() > 0L) {
                if (targetFile.exists()) {
                    targetFile.delete()
                }
                val renamed = tempFile.renameTo(targetFile)
                if (renamed) {
                    cache[surah.number] = surah
                    true
                } else {
                    tempFile.delete()
                    false
                }
            } else {
                tempFile.delete()
                false
            }
        } catch (_: Exception) {
            if (tempFile.exists()) tempFile.delete()
            false
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
