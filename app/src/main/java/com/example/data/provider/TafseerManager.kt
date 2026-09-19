package com.example.data.provider

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.remote.AyahTafseerResponse
import com.example.data.remote.TafseerApi
import com.example.data.remote.TafseerItem
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

/**
 * Authentic Tafseer Manager adhering strictly to classical commentaries.
 * Never fabricates or synthesizes generic commentary.
 * Uses versioned cache (tafseer_cache_v2) with atomic disk writes.
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

        // 1. Check verified persistent v2 local cache
        if (cacheFile.exists() && cacheFile.length() > 0L) {
            try {
                val cachedJson = cacheFile.readText()
                val response = gson.fromJson(cachedJson, AyahTafseerResponse::class.java)
                if (response != null && !response.text.isNullOrBlank() && response.tafseerId == tafseerId) {
                    return@withContext Result.success(response)
                }
            } catch (e: Exception) {
                cacheFile.delete()
            }
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
                                if (text.isNotBlank()) {
                                    val tafseerResp = AyahTafseerResponse(
                                        tafseerId = tafseerId,
                                        tafseerName = EmbeddedTafseerRepository.getTafseerBookName(tafseerId),
                                        ayahUrl = "",
                                        ayahNumber = ayahNumber,
                                        text = text.trim()
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
                                if (text.isNotBlank()) {
                                    val tafseerResp = AyahTafseerResponse(
                                        tafseerId = tafseerId,
                                        tafseerName = EmbeddedTafseerRepository.getTafseerBookName(tafseerId),
                                        ayahUrl = "",
                                        ayahNumber = ayahNumber,
                                        text = text.trim()
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

        // 5. Online Source C: Quran-Tafseer API (Strictly matching book ID)
        try {
            val response = api.getAyahTafseer(tafseerId, surahNumber, ayahNumber)
            if (!response.text.isNullOrBlank()) {
                val validatedResp = response.copy(
                    tafseerId = tafseerId,
                    tafseerName = EmbeddedTafseerRepository.getTafseerBookName(tafseerId)
                )
                saveTafseerToDiskAtomic(validatedResp, cacheFile)
                return@withContext Result.success(validatedResp)
            }
        } catch (e: Exception) {
            Log.d("TafseerManager", "Quran-Tafseer API attempt failed: ${e.message}")
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

    private fun saveTafseerToDiskAtomic(response: AyahTafseerResponse, targetFile: File) {
        val tempFile = File(cacheDir, "${targetFile.name}.tmp")
        try {
            val json = gson.toJson(response)
            FileOutputStream(tempFile).use { fos ->
                fos.write(json.toByteArray(Charsets.UTF_8))
                fos.flush()
                fos.fd.sync()
            }
            if (tempFile.exists() && tempFile.length() > 0L) {
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)
            }
        } catch (_: Exception) {
            if (tempFile.exists()) tempFile.delete()
        }
    }
}
