package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class QuranData(
    @Json(name = "surahs") val surahs: List<Surah>,
    @Json(name = "reciters") val reciters: List<Reciter>
)

@JsonClass(generateAdapter = true)
data class Surah(
    @Json(name = "number") val number: Int,
    @Json(name = "name") val name: String,
    @Json(name = "englishName") val englishName: String,
    @Json(name = "ayahs") val ayahs: Int,
    @Json(name = "type") val type: String // "مكية" or "مدنية"
) {
    val formattedNumber: String
        get() = String.format("%03d", number)
}

@JsonClass(generateAdapter = true)
data class Reciter(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "riwayah") val riwayah: String,
    @Json(name = "style") val style: String,
    @Json(name = "location") val location: String,
    @Json(name = "serverTemplates") val serverTemplates: List<String>,
    val editionsSummary: List<String> = emptyList(),
    val totalEditionsCount: Int = 1
)

enum class AudioQualityLevel(
    val key: String,
    val labelArabic: String,
    val labelEnglish: String,
    val approximateBitrateKbps: Int,
    val isLossless: Boolean
) {
    KBPS_320("320k", "320 kbps (جودة فائقة HD)", "320 kbps HD", 320, false),
    KBPS_192("192k", "192 kbps (جودة عالية HQ)", "192 kbps HQ", 192, false),
    KBPS_128("128k", "128 kbps (جودة قياسية Standard)", "128 kbps Standard", 128, false),
    KBPS_64("64k", "64 kbps (توفير البيانات Data Saver)", "64 kbps Data Saver", 64, false);

    companion object {
        fun fromKey(key: String?): AudioQualityLevel {
            if (key == null) return KBPS_192
            val lower = key.lowercase()
            return when {
                lower.contains("320") -> KBPS_320
                lower.contains("192") -> KBPS_192
                lower.contains("128") -> KBPS_128
                lower.contains("64") || lower.contains("32") -> KBPS_64
                else -> KBPS_192
            }
        }

        fun fromFormatAndBitrate(format: String?, bitrateKbps: Int?): AudioQualityLevel {
            return when (bitrateKbps) {
                in 300..1000 -> KBPS_320
                in 160..299 -> KBPS_192
                in 96..159 -> KBPS_128
                in 1..95 -> KBPS_64
                else -> KBPS_192
            }
        }
    }
}

data class AudioSource(
    val id: String = java.util.UUID.randomUUID().toString(),
    val source: String = "Internet Archive", // "Internet Archive", "mp3quran", etc.
    val sourceIdentifier: String? = null,
    val format: String = "mp3", // "mp3", "ogg", "m4a"
    val qualityLevel: AudioQualityLevel = AudioQualityLevel.KBPS_192,
    val bitrateKbps: Int? = null,
    val url: String,
    val fileSize: Long? = null,
    val durationSeconds: Long? = null,
    val license: String? = null
) {
    val formattedFileSize: String
        get() {
            val bytes = fileSize ?: return ""
            if (bytes <= 0) return ""
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(java.util.Locale.US, "%.1f GB", gb)
                mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
                else -> String.format(java.util.Locale.US, "%.0f KB", kb)
            }
        }
}

data class RecitationEdition(
    val editionId: String,
    val reciterId: String,
    val reciterName: String,
    val name: String,
    val riwayah: String,
    val recitationType: String, // "مجوّد", "مرتّل", "حفلات وتلاوات نادرة", "المصحف المعلم"
    val isCompleteQuran: Boolean = false,
    val surahCount: Int = 0,
    val availableSurahNumbers: List<Int> = emptyList(),
    val archiveIdentifier: String? = null,
    val license: String? = null
)

data class DiscoveredReciter(
    val canonicalId: String,
    val arabicName: String,
    val englishName: String,
    val country: String = "",
    val aliases: List<String> = emptyList(),
    val editions: List<RecitationEdition> = emptyList()
)

data class SurahAudioItem(
    val reciterId: String,
    val reciterName: String,
    val editionId: String = "",
    val riwayah: String,
    val recitationType: String = "مرتّل",
    val surahNumber: Int,
    val surahName: String,
    val ayahsCount: Int,
    val revelationType: String,
    val audioSources: List<String>,
    val detailedSources: List<AudioSource> = emptyList(),
    val localFilePath: String? = null,
    val activeQuality: AudioQualityLevel = AudioQualityLevel.KBPS_192,
    val isCompleteQuran: Boolean = false
) {
    fun getSourceForQuality(preferred: AudioQualityLevel): Pair<String, AudioSource?> {
        if (detailedSources.isEmpty()) {
            return (audioSources.firstOrNull() ?: "") to null
        }
        // 1. Try exact requested quality
        val exact = detailedSources.find { it.qualityLevel == preferred }
        if (exact != null) return exact.url to exact

        // 2. Fallback to highest bitrate available
        val best = detailedSources.maxByOrNull { it.qualityLevel.approximateBitrateKbps } ?: detailedSources.first()
        return best.url to best
    }
}

enum class SleepTimerMode(val labelArabic: String, val minutes: Int) {
    OFF("موقف", 0),
    MINUTES_5("5 دقائق", 5),
    MINUTES_15("15 دقيقة", 15),
    MINUTES_30("30 دقيقة", 30),
    MINUTES_45("45 دقيقة", 45),
    MINUTES_60("60 دقيقة (ساعة)", 60),
    END_OF_SURAH("عند نهاية السورة", -1),
    CUSTOM("مدة مخصصة", -2)
}

data class ABLoopState(
    val isActive: Boolean = false,
    val startPositionMs: Long? = null,
    val endPositionMs: Long? = null,
    val loopCount: Int = 0
) {
    val isReady: Boolean
        get() = startPositionMs != null && endPositionMs != null && endPositionMs > startPositionMs
}

data class RepeatSettings(
    val mode: PlayerRepeatMode = PlayerRepeatMode.OFF,
    val targetCount: Int = 0, // 0 = unlimited / normal, 1, 3, 5, 7, 10 for memorization
    val currentIteration: Int = 1,
    val abLoop: ABLoopState = ABLoopState()
)

data class Playlist(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val colorHex: String = "#10B981",
    val itemsCount: Int = 0
)

data class PlaylistItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val playlistId: String,
    val surahNumber: Int,
    val surahName: String,
    val reciterId: String,
    val reciterName: String,
    val riwayah: String = "حفص عن عاصم",
    val orderIndex: Int = 0,
    val durationSeconds: Long? = null
)

data class PlayerState(
    val currentItem: SurahAudioItem? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val currentSourceIndex: Int = 0,
    val totalSourcesCount: Int = 0,
    val currentSourceUrl: String = "",
    val currentAudioSource: AudioSource? = null,
    val activeQuality: AudioQualityLevel = AudioQualityLevel.KBPS_192,
    val isFallbackActive: Boolean = false,
    val fallbackMessage: String? = null,
    val playbackSpeed: Float = 1.0f,
    val repeatMode: PlayerRepeatMode = PlayerRepeatMode.OFF,
    val repeatSettings: RepeatSettings = RepeatSettings(),
    val autoPlayNext: Boolean = true,
    val isOfflineMode: Boolean = false,
    // Sleep Timer
    val isSleepTimerActive: Boolean = false,
    val sleepTimerRemainingSeconds: Long = 0L,
    val sleepTimerMode: SleepTimerMode = SleepTimerMode.OFF,
    val sleepTimerFadeOutEnabled: Boolean = true,
    // Custom Playlist Queue
    val activePlaylistId: String? = null,
    val activePlaylistName: String? = null,
    val playlistQueue: List<PlaylistItem> = emptyList(),
    val currentPlaylistIndex: Int = -1,
    val isShuffleEnabled: Boolean = false
)

enum class PlayerRepeatMode {
    OFF,
    REPEAT_ONE,
    REPEAT_ALL
}

data class DownloadState(
    val isDownloaded: Boolean = false,
    val isDownloading: Boolean = false,
    val progressPercent: Int = 0,
    val localPath: String? = null,
    val format: String = "mp3",
    val quality: AudioQualityLevel = AudioQualityLevel.KBPS_192,
    val fileSize: Long = 0L
)

enum class BulkDownloadStatus {
    IDLE,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    ERROR,
    CANCELLED
}

data class BulkDownloadProgress(
    val status: BulkDownloadStatus = BulkDownloadStatus.IDLE,
    val reciterId: String = "",
    val reciterName: String = "",
    val riwayah: String = "",
    val totalSurahs: Int = 114,
    val completedSurahs: Int = 0,
    val currentSurahNumber: Int = 0,
    val currentSurahName: String = "",
    val currentSurahProgressPercent: Int = 0,
    val totalBytesDownloaded: Long = 0L,
    val failedSurahsCount: Int = 0,
    val errorMessage: String? = null
) {
    val overallPercent: Int
        get() = if (totalSurahs > 0) ((completedSurahs * 100) / totalSurahs).coerceIn(0, 100) else 0
    val isRunning: Boolean
        get() = status == BulkDownloadStatus.DOWNLOADING
}

data class ReciterStorageInfo(
    val reciterId: String,
    val reciterName: String,
    val riwayah: String,
    val downloadedSurahsCount: Int,
    val totalSurahsCount: Int = 114,
    val totalSizeBytes: Long,
    val formattedSize: String,
    val downloadedSurahNumbers: List<Int>
)

data class OfflineTextDownloadState(
    val isDownloading: Boolean = false,
    val totalSurahs: Int = 114,
    val downloadedSurahs: Int = 0,
    val currentSurahName: String = "",
    val isCompleted: Boolean = false
)

data class ReviewItem(
    val identifier: String,
    val filename: String,
    val title: String,
    val creator: String,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class SyncStatistics(
    val discoveredRecitersCount: Int = 0,
    val discoveredEditionsCount: Int = 0,
    val discoveredSurahsCount: Int = 0,
    val duplicatesRemovedCount: Int = 0,
    val validUrlsCount: Int = 0,
    val failedUrlsCount: Int = 0,
    val itemsNeedingReviewCount: Int = 0,
    val lastSyncTimestamp: Long = 0L,
    val isSyncing: Boolean = false,
    val syncProgressMessage: String = ""
)

sealed class PlayerEvent {
    data class FallbackSourceSwitched(
        val sourceIndex: Int,
        val totalSources: Int,
        val surahName: String,
        val reciterName: String,
        val quality: String
    ) : PlayerEvent()

    data class PlaybackError(val message: String) : PlayerEvent()
    data class ToastMessage(val message: String) : PlayerEvent()
}
