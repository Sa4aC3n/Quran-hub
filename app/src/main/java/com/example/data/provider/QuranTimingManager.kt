package com.example.data.provider

import android.content.Context
import android.util.Log
import com.example.data.model.Ayah
import com.example.data.model.SurahText
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class WordTiming(
    val wordIndex: Int,
    val text: String,
    val startMs: Long,
    val endMs: Long
)

data class AyahTiming(
    val ayahNumber: Int,
    val startMs: Long,
    val endMs: Long,
    val words: List<WordTiming> = emptyList()
)

data class SurahTiming(
    val surahNumber: Int,
    val durationMs: Long,
    val ayahs: List<AyahTiming>
)

class QuranTimingManager(context: Context) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val memoryCache = ConcurrentHashMap<String, SurahTiming>()

    /**
     * Map common reciter IDs / names to Quran.com recitation IDs for accurate segment timestamps
     * 7: Mishari Rashid Al-Afasy
     * 2: AbdulBaset AbdulSamad (Murattal)
     * 3: Abdur-Rahman as-Sudais
     * 4: Abu Bakr al-Shatri
     * 6: Mahmud Khalil Al-Husary
     * 12: Maher Al-Muaiqly
     * 10: Sa'ud ash-Shuraym
     * 1: Saad Al-Ghamdi
     * 8: Muhammad Siddiq Al-Minshawi (Murattal)
     * 5: Hani ar-Rifai
     * 9: Mohamed al-Tablawi
     */
    private fun getRecitationId(reciterId: String, reciterName: String): Int? {
        val clean = (reciterId + " " + reciterName).lowercase()
        return when {
            clean.contains("afasy") || clean.contains("عفاسي") || clean.contains("mishar") -> 7
            clean.contains("sudais") || clean.contains("سديس") -> 3
            clean.contains("shatri") || clean.contains("شاطري") -> 4
            clean.contains("husary") || clean.contains("حصري") -> 6
            clean.contains("muaiqly") || clean.contains("معيقلي") -> 12
            clean.contains("shuraym") || clean.contains("شريم") -> 10
            clean.contains("basit") || clean.contains("باسط") -> 2
            clean.contains("ghamadi") || clean.contains("غامدي") -> 1
            clean.contains("minshawi") || clean.contains("منشاوي") -> 8
            clean.contains("rifai") || clean.contains("رفاعي") -> 5
            clean.contains("tablawi") || clean.contains("طبلاوي") -> 9
            else -> null // Strictly return null if no calibrated timing exists
        }
    }

    suspend fun getTimingForSurah(
        surahNumber: Int,
        reciterId: String,
        reciterName: String,
        surahText: SurahText?,
        audioDurationMs: Long
    ): SurahTiming? = withContext(Dispatchers.IO) {
        val cacheKey = "${surahNumber}_${reciterId}"
        memoryCache[cacheKey]?.let { return@withContext it }

        val recitationId = getRecitationId(reciterId, reciterName)
        if (recitationId != null) {
            try {
                val url = "https://api.quran.com/api/v4/chapter_recitations/$recitationId/$surahNumber?segments=true"
                val request = Request.Builder().url(url).build()
                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val parsed = parseQuranDotComSegments(body, surahNumber, surahText)
                        if (parsed != null && parsed.ayahs.isNotEmpty()) {
                            memoryCache[cacheKey] = parsed
                            return@withContext parsed
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d("QuranTimingManager", "Could not fetch online timing: ${e.message}")
            }
        }

        // Return null when timing data is unavailable (do not invent fake timings)
        null
    }

    private fun parseQuranDotComSegments(
        jsonString: String,
        surahNumber: Int,
        surahText: SurahText?
    ): SurahTiming? {
        return try {
            val json = gson.fromJson(jsonString, QuranComAudioFileResponse::class.java)
            val audioFile = json.audioFile ?: return null
            val rawDurationMs = (audioFile.duration ?: 0f) * 1000L
            val verseTimings = audioFile.verseTimings ?: emptyList()

            val ayahTimingList = verseTimings.mapIndexed { index, vt ->
                val ayahNum = index + 1
                val startMs = vt.timestampFrom ?: 0L
                val endMs = vt.timestampTo ?: startMs
                val rawSegments = vt.segments ?: emptyList()

                val verseRawText = surahText?.ayahs?.getOrNull(index)?.text ?: ""
                val cleanVerseText = QuranTextProvider.cleanBismillahFromVerse(
                    verseRawText,
                    surahNumber,
                    ayahNum
                )
                val words = cleanVerseText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }

                val wordTimings = if (rawSegments.isNotEmpty()) {
                    rawSegments.mapIndexed { wIdx, seg ->
                        val wordPosZeroBased = (seg.getOrNull(0)?.toInt() ?: (wIdx + 1)) - 1
                        val wStart = seg.getOrNull(1)?.toLong() ?: startMs
                        val wEnd = seg.getOrNull(2)?.toLong() ?: endMs
                        val text = words.getOrNull(wordPosZeroBased) ?: words.getOrNull(wIdx) ?: ""
                        WordTiming(
                            wordIndex = wordPosZeroBased.coerceAtLeast(0),
                            text = text,
                            startMs = wStart,
                            endMs = wEnd
                        )
                    }
                } else {
                    emptyList()
                }

                AyahTiming(
                    ayahNumber = ayahNum,
                    startMs = startMs,
                    endMs = endMs,
                    words = wordTimings
                )
            }

            SurahTiming(
                surahNumber = surahNumber,
                durationMs = if (rawDurationMs > 0) rawDurationMs.toLong() else ayahTimingList.lastOrNull()?.endMs ?: 0L,
                ayahs = ayahTimingList
            )
        } catch (e: Exception) {
            Log.e("QuranTimingManager", "Error parsing segments", e)
            null
        }
    }

    /**
     * Find active Ayah and Word from current playback position in milliseconds.
     * Returns null for wordIdx if no calibrated word timestamps exist for this segment.
     */
    fun findActivePosition(
        timing: SurahTiming?,
        positionMs: Long
    ): Pair<Int?, Int?> {
        if (timing == null || timing.ayahs.isEmpty()) return Pair(null, null)

        val activeAyah = timing.ayahs.find { positionMs in it.startMs..it.endMs }
            ?: timing.ayahs.find { positionMs >= it.startMs && positionMs <= (it.endMs + 300L) }

        val ayahNum = activeAyah?.ayahNumber
        val wordIdx = activeAyah?.words?.find { positionMs in it.startMs..it.endMs }?.wordIndex

        return Pair(ayahNum, wordIdx)
    }

    private data class QuranComAudioFileResponse(
        @SerializedName("audio_file") val audioFile: QuranComAudioFile? = null
    )

    private data class QuranComAudioFile(
        @SerializedName("duration") val duration: Float? = null,
        @SerializedName("verse_timings") val verseTimings: List<QuranComVerseTiming>? = null
    )

    private data class QuranComVerseTiming(
        @SerializedName("verse_key") val verseKey: String? = null,
        @SerializedName("timestamp_from") val timestampFrom: Long? = null,
        @SerializedName("timestamp_to") val timestampTo: Long? = null,
        @SerializedName("duration") val duration: Long? = null,
        @SerializedName("segments") val segments: List<List<Double>>? = null
    )
}
