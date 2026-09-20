package com.example.data.provider

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.remote.AyahTafseerResponse
import com.example.data.remote.TafseerApi
import com.example.data.remote.TafseerItem
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class TafseerUiState(
    val isLoading: Boolean = false,
    val tafseerText: String? = null,
    val tafseerName: String = "التفسير الميسر",
    val tafseerId: Int = 1,
    val surahNumber: Int = 1,
    val ayahNumber: Int = 1,
    val isOfflineSource: Boolean = false,
    val errorMessage: String? = null
)

class TafseerUnavailableException(val tafseerId: Int, message: String) : Exception(message)

data class TafseerStorageEnvelope(
    @SerializedName("schema_version") val schemaVersion: Int = 2,
    @SerializedName("tafseer_id") val tafseerId: Int,
    @SerializedName("surah_number") val surahNumber: Int,
    @SerializedName("ayah_number") val ayahNumber: Int,
    @SerializedName("source_provider") val sourceProvider: String,
    @SerializedName("dataset_version") val datasetVersion: String,
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    @SerializedName("response") val response: AyahTafseerResponse
)

sealed class TafseerRangeResult {
    data class CompleteSuccess(
        val totalPersistedAyahs: Int,
        val totalSurahs: Int
    ) : TafseerRangeResult()

    data class PartialSuccess(
        val totalPersistedAyahs: Int,
        val successfulSurahs: Int,
        val failedSurahs: Int,
        val details: String
    ) : TafseerRangeResult()

    data class Failure(
        val reason: String
    ) : TafseerRangeResult()
}

sealed class TafseerPersistenceResult {
    data class Success(val tafseerId: Int, val surahNumber: Int, val ayahNumber: Int, val file: File) : TafseerPersistenceResult()
    data class Failure(val tafseerId: Int, val surahNumber: Int, val ayahNumber: Int, val reason: String) : TafseerPersistenceResult()
}

data class TafseerDownloadProgress(
    val tafseerId: Int,
    val tafseerName: String = "",
    val surahNumber: Int,
    val surahName: String = "",
    val totalAyahs: Int,
    val attemptedAyahs: Int,
    val persistedAyahs: Int,
    val failedAyahs: Int,
    val isCompleted: Boolean,
    val isDownloading: Boolean = true,
    val errorMessage: String? = null
)

/**
 * Authentic Tafseer Manager adhering strictly to classical commentaries.
 * Never fabricates or synthesizes generic commentary.
 * Uses versioned cache (tafseer_cache_v2) with atomic disk writes (API 24/25+ compatible).
 */
class TafseerManager(context: Context) {

    private val appContext = context.applicationContext
    private val api = TafseerApi.create()
    private val prefs: SharedPreferences = appContext.getSharedPreferences("tafseer_prefs", Context.MODE_PRIVATE)
    private val cacheDir = File(appContext.filesDir, "tafseer_cache_v2").apply { mkdirs() }
    private val gson = Gson()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var activeDownloadJob: kotlinx.coroutines.Job? = null

    init {
        // Invalidate legacy unverified v1 cache if present
        try {
            val legacyCache = File(appContext.cacheDir, "tafseer_cache")
            if (legacyCache.exists()) {
                legacyCache.deleteRecursively()
            }
        } catch (_: Exception) {}
    }

    private val defaultTafseers = listOf(
        TafseerItem(id = 1, name = "التفسير الميسر", language = "ar", author = "مجمع الملك فهد لطباعة المصحف الشريف", bookName = "التفسير الميسر"),
        TafseerItem(id = 2, name = "تفسير الجلالين", language = "ar", author = "جلال الدين المحلي وجلال الدين السيوطي", bookName = "تفسير الجلالين"),
        TafseerItem(id = 3, name = "تفسير السعدي", language = "ar", author = "الشيخ عبد الرحمن بن ناصر السعدي", bookName = "تيسير الكريم الرحمن"),
        TafseerItem(id = 4, name = "تفسير ابن كثير", language = "ar", author = "الحافظ عماد الدين ابن كثير", bookName = "تفسير القرآن العظيم"),
        TafseerItem(id = 9, name = "المختصر في التفسير", language = "ar", author = "مركز تفسير للدراسات القرآنية", bookName = "المختصر في التفسير"),
        TafseerItem(id = 5, name = "التفسير الوسيط", language = "ar", author = "الإمام الأكبر د. محمد سيد طنطاوي", bookName = "التفسير الوسيط"),
        TafseerItem(id = 6, name = "تفسير البغوي", language = "ar", author = "أبو محمد الحسين بن مسعود البغوي", bookName = "معالم التنزيل"),
        TafseerItem(id = 7, name = "تفسير القرطبي", language = "ar", author = "أبو عبد الله محمد بن أحمد القرطبي", bookName = "الجامع لأحكام القرآن"),
        TafseerItem(id = 8, name = "تفسير الطبري", language = "ar", author = "محمد بن جرير الطبري", bookName = "جامع البيان عن تأويل آي القرآن")
    )

    fun getSelectedTafseerId(): Int {
        return prefs.getInt("selected_tafseer_id", 1)
    }

    fun setSelectedTafseerId(id: Int) {
        prefs.edit().putInt("selected_tafseer_id", id).apply()
    }

    suspend fun getAvailableTafseers(): List<TafseerItem> = withContext(Dispatchers.IO) {
        val cachedListJson = prefs.getString("cached_tafseer_list", null)
        if (!cachedListJson.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<TafseerItem>>() {}.type
                val list: List<TafseerItem> = gson.fromJson(cachedListJson, type)
                if (list.isNotEmpty()) return@withContext list
            } catch (_: Exception) {}
        }
        defaultTafseers
    }

    /**
     * Reads and authenticates a cached tafseer file using Android AtomicFile.
     * Enforces schema validation and verifiable provenance.
     */
    fun readTafseerFromFile(file: File, expectedTafseerId: Int, expectedSurah: Int, expectedAyah: Int): AyahTafseerResponse? {
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

                val schemaVersion = if (jsonObject.has("schema_version")) {
                    jsonObject.get("schema_version").asInt
                } else {
                    1
                }

                val response = when (schemaVersion) {
                    2 -> {
                        if (jsonObject.has("response")) {
                            val envTafseerId = jsonObject.get("tafseer_id")?.asInt ?: return@synchronized null
                            val envSurah = jsonObject.get("surah_number")?.asInt ?: return@synchronized null
                            val envAyah = jsonObject.get("ayah_number")?.asInt ?: return@synchronized null
                            val provider = jsonObject.get("source_provider")?.asString
                            if (envTafseerId != expectedTafseerId || envSurah != expectedSurah || envAyah != expectedAyah || provider.isNullOrBlank()) {
                                return@synchronized null
                            }
                            gson.fromJson(jsonObject.get("response"), AyahTafseerResponse::class.java)
                        } else {
                            gson.fromJson(json, AyahTafseerResponse::class.java)
                        }
                    }
                    1 -> {
                        gson.fromJson(json, AyahTafseerResponse::class.java)
                    }
                    else -> return@synchronized null
                }

                if (response != null &&
                    !response.text.isNullOrBlank() &&
                    response.tafseerId == expectedTafseerId &&
                    response.surahNumber == expectedSurah &&
                    response.ayahNumber == expectedAyah &&
                    response.sourceProvider.isNotBlank()
                ) {
                    response
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Checks whether an individual ayah commentary is verified and persisted on disk.
     */
    fun isTafseerAyahPersisted(tafseerId: Int, surahNumber: Int, ayahNumber: Int): Boolean {
        val cacheKey = "tafseer_${tafseerId}_${surahNumber}_${ayahNumber}.json"
        val cacheFile = File(cacheDir, cacheKey)
        return readTafseerFromFile(cacheFile, tafseerId, surahNumber, ayahNumber) != null
    }

    /**
     * Counts verified persisted ayahs for a given surah and tafseer book.
     */
    fun getPersistedTafseerAyahsCount(tafseerId: Int, surahNumber: Int): Int {
        val totalAyahs = QuranManifest.getCanonicalAyahCount(surahNumber)
        var count = 0
        for (ayah in 1..totalAyahs) {
            if (isTafseerAyahPersisted(tafseerId, surahNumber, ayah)) {
                count++
            }
        }
        return count
    }

    /**
     * Fetches authentic commentary for the specified book and ayah.
     * Guaranteed to NEVER fabricate commentary or mix books.
     */
    suspend fun getAyahTafseer(
        tafseerId: Int,
        surahNumber: Int,
        ayahNumber: Int,
        cleanAyahText: String = ""
    ): Result<AyahTafseerResponse> = withContext(Dispatchers.IO) {
        val cacheKey = "tafseer_${tafseerId}_${surahNumber}_${ayahNumber}.json"
        val cacheFile = File(cacheDir, cacheKey)

        // 1. Check verified persistent v2 local cache with complete identity check
        val cachedResponse = readTafseerFromFile(cacheFile, tafseerId, surahNumber, ayahNumber)
        if (cachedResponse != null) {
            return@withContext Result.success(cachedResponse)
        }

        // 2. Check embedded authentic verified repository for this specific book
        val embeddedTafseer = EmbeddedTafseerRepository.getEmbeddedTafseer(
            tafseerId = tafseerId,
            surahNumber = surahNumber,
            ayahNumber = ayahNumber
        )
        if (embeddedTafseer != null && embeddedTafseer.text.isNotBlank()) {
            saveTafseerToDiskAtomic(embeddedTafseer, cacheFile)
            return@withContext Result.success(embeddedTafseer)
        }

        // 3. Online Source A: QuranEnc API (Strictly for supported authentic books)
        val quranEncKey = when (tafseerId) {
            1 -> "arabic_moyassar"
            3 -> "arabic_saadi"
            6 -> "arabic_baghaway"
            9 -> "arabic_mokhtasar"
            else -> null
        }

        if (quranEncKey != null) {
            try {
                val url = "https://quranenc.com/api/v1/translation/aya/$quranEncKey/$surahNumber/$ayahNumber"
                val request = Request.Builder().url(url).build()
                val response = httpClient.newCall(request).execute()
                response.use { res ->
                    if (res.isSuccessful) {
                        val body = res.body?.string()
                        if (!body.isNullOrBlank()) {
                            val json = gson.fromJson(body, JsonObject::class.java)
                            val resultObj = json.getAsJsonObject("result")
                            if (resultObj != null && resultObj.has("translation")) {
                                val text = resultObj.get("translation").asString
                                // Verify response matches requested surah/ayah if returned by provider
                                val resSura = if (resultObj.has("sura")) resultObj.get("sura").asString.toIntOrNull() else surahNumber
                                val resAya = if (resultObj.has("aya")) resultObj.get("aya").asString.toIntOrNull() else ayahNumber
                                if (resSura == surahNumber && resAya == ayahNumber && text.isNotBlank()) {
                                    val tafseerResp = AyahTafseerResponse(
                                        tafseerId = tafseerId,
                                        tafseerName = EmbeddedTafseerRepository.getTafseerBookName(tafseerId),
                                        ayahUrl = "",
                                        ayahNumber = ayahNumber,
                                        surahNumber = surahNumber,
                                        text = text.trim(),
                                        sourceProvider = "quranenc.com/$quranEncKey",
                                        schemaVersion = 2
                                    )
                                    saveTafseerToDiskAtomic(tafseerResp, cacheFile)
                                    return@withContext Result.success(tafseerResp)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d("TafseerManager", "QuranEnc attempt failed: ${e.message}")
            }
        }

        // 4. Online Source B: Al-Quran Cloud API (Strictly for supported authentic books)
        val alQuranCloudKey = when (tafseerId) {
            1 -> "ar.muyassar"
            2 -> "ar.jalalayn"
            7 -> "ar.qurtubi"
            else -> null
        }

        if (alQuranCloudKey != null) {
            try {
                val url = "https://api.alquran.cloud/v1/ayah/$surahNumber:$ayahNumber/$alQuranCloudKey"
                val request = Request.Builder().url(url).build()
                val response = httpClient.newCall(request).execute()
                response.use { res ->
                    if (res.isSuccessful) {
                        val body = res.body?.string()
                        if (!body.isNullOrBlank()) {
                            val json = gson.fromJson(body, JsonObject::class.java)
                            val dataObj = json.getAsJsonObject("data")
                            if (dataObj != null && dataObj.has("text")) {
                                val text = dataObj.get("text").asString
                                val numInSurah = if (dataObj.has("numberInSurah")) dataObj.get("numberInSurah").asInt else ayahNumber
                                val sNum = if (dataObj.has("surah") && dataObj.getAsJsonObject("surah").has("number")) {
                                    dataObj.getAsJsonObject("surah").get("number").asInt
                                } else surahNumber

                                if (sNum == surahNumber && numInSurah == ayahNumber && text.isNotBlank()) {
                                    val tafseerResp = AyahTafseerResponse(
                                        tafseerId = tafseerId,
                                        tafseerName = EmbeddedTafseerRepository.getTafseerBookName(tafseerId),
                                        ayahUrl = "",
                                        ayahNumber = ayahNumber,
                                        surahNumber = surahNumber,
                                        text = text.trim(),
                                        sourceProvider = "api.alquran.cloud/$alQuranCloudKey",
                                        schemaVersion = 2
                                    )
                                    saveTafseerToDiskAtomic(tafseerResp, cacheFile)
                                    return@withContext Result.success(tafseerResp)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d("TafseerManager", "AlQuranCloud attempt failed: ${e.message}")
            }
        }

        // 5. Online Source C: Quran-Tafseer API (Strictly validating book and ayah identity)
        val sourceCId = when (tafseerId) {
            1 -> 1 // ar-tafsir-muyassar
            2 -> 2 // ar-tafseer-al-jalalayn
            3 -> 3 // ar-tafseer-al-saadi
            4 -> 4 // ar-tafsir-ibn-kathir
            5 -> 5 // ar-tafsir-waseet
            6 -> 6 // ar-tafsir-baghawi
            7 -> 7 // ar-tafsir-qurtubi
            8 -> 8 // ar-tafsir-tabari
            else -> null // ID 9 (المختصر) is NOT hosted on api.quran-tafseer.com
        }

        if (sourceCId != null) {
            try {
                val response = api.getAyahTafseer(sourceCId, surahNumber, ayahNumber)
                if (!response.text.isNullOrBlank()) {
                    val idMatches = (response.tafseerId == sourceCId)
                    val ayahMatches = (response.ayahNumber == ayahNumber)

                    var urlMatches = true
                    if (!response.ayahUrl.isNullOrBlank()) {
                        val parts = response.ayahUrl.trim('/').split('/')
                        if (parts.size >= 3 && parts[0] == "quran") {
                            val urlSura = parts[1].toIntOrNull()
                            val urlAya = parts[2].toIntOrNull()
                            if (urlSura != null && urlAya != null) {
                                urlMatches = (urlSura == surahNumber && urlAya == ayahNumber)
                            }
                        }
                    }

                    if (idMatches && ayahMatches && urlMatches) {
                        val validatedResp = AyahTafseerResponse(
                            tafseerId = tafseerId,
                            tafseerName = EmbeddedTafseerRepository.getTafseerBookName(tafseerId),
                            ayahUrl = response.ayahUrl,
                            ayahNumber = ayahNumber,
                            surahNumber = surahNumber,
                            text = response.text.trim(),
                            sourceProvider = "api.quran-tafseer.com/tafseer/$sourceCId",
                            schemaVersion = 2
                        )
                        saveTafseerToDiskAtomic(validatedResp, cacheFile)
                        return@withContext Result.success(validatedResp)
                    }
                }
            } catch (e: Exception) {
                Log.d("TafseerManager", "Quran-Tafseer API attempt failed: ${e.message}")
            }
        }

        // 6. When unavailable both locally and remotely: return honest failure
        val bookName = EmbeddedTafseerRepository.getTafseerBookName(tafseerId)
        Result.failure(
            TafseerUnavailableException(
                tafseerId,
                "التفسير غير متاح حاليًا لـ ($bookName) دون اتصال بالإنترنت"
            )
        )
    }

    /**
     * Guarantees that a tafseer entry is persisted on disk and verified by read-back.
     */
    suspend fun ensureTafseerPersisted(
        tafseerId: Int,
        surahNumber: Int,
        ayahNumber: Int
    ): TafseerPersistenceResult = withContext(Dispatchers.IO) {
        val cacheKey = "tafseer_${tafseerId}_${surahNumber}_${ayahNumber}.json"
        val cacheFile = File(cacheDir, cacheKey)

        if (isTafseerAyahPersisted(tafseerId, surahNumber, ayahNumber)) {
            return@withContext TafseerPersistenceResult.Success(tafseerId, surahNumber, ayahNumber, cacheFile)
        }

        val result = getAyahTafseer(tafseerId, surahNumber, ayahNumber)
        if (result.isSuccess) {
            val resp = result.getOrNull()
            if (resp != null) {
                val saveResult = saveTafseerToDiskAtomic(resp, cacheFile)
                if (saveResult is TafseerPersistenceResult.Success) {
                    return@withContext saveResult
                }
            }
        }

        TafseerPersistenceResult.Failure(
            tafseerId,
            surahNumber,
            ayahNumber,
            "تعذر جلب التفسير وتأكيد حفظه على القرص"
        )
    }

    /**
     * Cancels any active Tafseer downloading job.
     */
    fun cancelTafseerDownload() {
        activeDownloadJob?.cancel()
        activeDownloadJob = null
    }

    /**
     * Downloads and authenticates tafseer for a specific surah.
     * Reports real progress based strictly on verified disk-persisted items.
     */
    suspend fun downloadTafseerForSurah(
        tafseerId: Int,
        surahNumber: Int,
        onProgress: (TafseerDownloadProgress) -> Unit = {}
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (surahNumber !in 1..QuranManifest.TOTAL_SURAHS) {
            return@withContext Result.failure(IllegalArgumentException("رقم السورة غير صالح: $surahNumber"))
        }

        val totalAyahs = QuranManifest.getCanonicalAyahCount(surahNumber)
        val surahName = QuranManifest.getSurahNameArabic(surahNumber)
        val bookName = EmbeddedTafseerRepository.getTafseerBookName(tafseerId)
        var persistedCount = 0
        var failedCount = 0
        var attemptedCount = 0

        for (ayah in 1..totalAyahs) {
            ensureActive()

            attemptedCount++
            if (isTafseerAyahPersisted(tafseerId, surahNumber, ayah)) {
                persistedCount++
            } else {
                val persistResult = ensureTafseerPersisted(tafseerId, surahNumber, ayah)
                if (persistResult is TafseerPersistenceResult.Success) {
                    persistedCount++
                } else {
                    failedCount++
                }
            }

            onProgress(
                TafseerDownloadProgress(
                    tafseerId = tafseerId,
                    tafseerName = bookName,
                    surahNumber = surahNumber,
                    surahName = surahName,
                    totalAyahs = totalAyahs,
                    attemptedAyahs = attemptedCount,
                    persistedAyahs = persistedCount,
                    failedAyahs = failedCount,
                    isCompleted = (persistedCount == totalAyahs),
                    isDownloading = (persistedCount + failedCount < totalAyahs)
                )
            )
        }

        if (persistedCount == totalAyahs) {
            Result.success(persistedCount)
        } else {
            Result.failure(
                Exception("تم حفظ $persistedCount من أصل $totalAyahs آية لتفسير ($bookName) لسورة $surahName. تعذر حفظ $failedCount آية.")
            )
        }
    }

    /**
     * Downloads and authenticates tafseer for a range of surahs.
     * Rejects invalid ranges instead of silent clamping.
     * Returns structured CompleteSuccess, PartialSuccess, or Failure.
     */
    suspend fun downloadTafseerRange(
        tafseerId: Int,
        fromSurah: Int,
        toSurah: Int,
        onProgress: (TafseerDownloadProgress) -> Unit = {}
    ): TafseerRangeResult = withContext(Dispatchers.IO) {
        if (fromSurah !in 1..QuranManifest.TOTAL_SURAHS ||
            toSurah !in 1..QuranManifest.TOTAL_SURAHS ||
            fromSurah > toSurah
        ) {
            return@withContext TafseerRangeResult.Failure(
                "نطاق السور غير صالح: من $fromSurah إلى $toSurah (يجب أن يكون بين 1 و ${QuranManifest.TOTAL_SURAHS})"
            )
        }

        var totalPersisted = 0
        var successfulSurahs = 0
        var failedSurahs = 0
        val totalSurahsInRange = toSurah - fromSurah + 1

        for (surahNum in fromSurah..toSurah) {
            ensureActive()

            val surahRes = downloadTafseerForSurah(tafseerId, surahNum, onProgress)
            if (surahRes.isSuccess) {
                successfulSurahs++
                totalPersisted += surahRes.getOrDefault(0)
            } else {
                failedSurahs++
            }
        }

        when {
            successfulSurahs == totalSurahsInRange -> {
                TafseerRangeResult.CompleteSuccess(
                    totalPersistedAyahs = totalPersisted,
                    totalSurahs = successfulSurahs
                )
            }
            successfulSurahs > 0 -> {
                TafseerRangeResult.PartialSuccess(
                    totalPersistedAyahs = totalPersisted,
                    successfulSurahs = successfulSurahs,
                    failedSurahs = failedSurahs,
                    details = "تم حفظ $successfulSurahs سورة بنجاح من أصل $totalSurahsInRange، وتعر حفظ $failedSurahs سورة."
                )
            }
            else -> {
                TafseerRangeResult.Failure("تعذر حفظ التفسير لجميع سور النطاق ($fromSurah إلى $toSurah)")
            }
        }
    }

    /**
     * Atomically writes the tafseer response to persistent disk storage using Android AtomicFile (API 24/25+ compatible).
     * Enforces schema envelope and strict read-back verification.
     */
    fun saveTafseerToDiskAtomic(response: AyahTafseerResponse, targetFile: File): TafseerPersistenceResult {
        if (response.text.isBlank()) {
            return TafseerPersistenceResult.Failure(response.tafseerId, response.surahNumber, response.ayahNumber, "نص التفسير فارغ")
        }
        val lock = StorageLockManager.getLockFor(targetFile)
        synchronized(lock) {
            val atomicFile = android.util.AtomicFile(targetFile)
            var fos: FileOutputStream? = null
            try {
                val envelope = TafseerStorageEnvelope(
                    schemaVersion = 2,
                    tafseerId = response.tafseerId,
                    surahNumber = response.surahNumber,
                    ayahNumber = response.ayahNumber,
                    sourceProvider = response.sourceProvider,
                    datasetVersion = "1430H",
                    timestamp = System.currentTimeMillis(),
                    response = response
                )
                val json = gson.toJson(envelope)
                val bytes = json.toByteArray(Charsets.UTF_8)
                fos = atomicFile.startWrite()
                fos.write(bytes)
                fos.flush()
                try {
                    fos.fd.sync()
                } catch (_: Exception) {}
                atomicFile.finishWrite(fos)
                fos = null
            } catch (e: Exception) {
                if (fos != null) {
                    atomicFile.failWrite(fos)
                }
                return TafseerPersistenceResult.Failure(response.tafseerId, response.surahNumber, response.ayahNumber, "خطأ أثناء حفظ التفسير: ${e.message}")
            }

            // Read-back verification
            val readBack = readTafseerFromFile(targetFile, response.tafseerId, response.surahNumber, response.ayahNumber)
            return if (readBack != null) {
                TafseerPersistenceResult.Success(response.tafseerId, response.surahNumber, response.ayahNumber, targetFile)
            } else {
                TafseerPersistenceResult.Failure(response.tafseerId, response.surahNumber, response.ayahNumber, "فشل التحقق من قراءة التفسير من القرص بعد الحفظ")
            }
        }
    }
}
