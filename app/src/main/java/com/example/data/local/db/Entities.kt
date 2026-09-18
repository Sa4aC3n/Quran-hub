package com.example.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reciters")
data class ReciterEntity(
    @PrimaryKey val id: String,
    val arabicName: String,
    val englishName: String,
    val country: String,
    val aliases: List<String> = emptyList(),
    val totalEditionsCount: Int = 1,
    val defaultRiwayah: String = "حفص عن عاصم"
)

@Entity(tableName = "editions")
data class EditionEntity(
    @PrimaryKey val editionId: String,
    val reciterId: String,
    val reciterName: String,
    val name: String,
    val riwayah: String,
    val recitationType: String,
    val isCompleteQuran: Boolean,
    val surahCount: Int,
    val archiveIdentifier: String? = null,
    val license: String? = null
)

@Entity(tableName = "surah_recordings")
data class SurahRecordingEntity(
    @PrimaryKey val recordingId: String, // deduplication key: reciterId_editionId_surahNumber
    val reciterId: String,
    val reciterName: String,
    val editionId: String,
    val riwayah: String,
    val recitationType: String,
    val surahNumber: Int,
    val surahName: String,
    val title: String?,
    val isCompleteQuran: Boolean,
    val needsReview: Boolean = false,
    val reviewReason: String? = null
)

@Entity(
    tableName = "audio_sources",
    primaryKeys = ["sourceUrl"]
)
data class AudioSourceEntity(
    val sourceUrl: String,
    val recordingId: String,
    val sourceName: String,
    val sourceIdentifier: String?,
    val format: String,
    val qualityKey: String,
    val bitrateKbps: Int?,
    val fileSize: Long?,
    val durationSeconds: Long?,
    val license: String?
)

@Entity(tableName = "review_items")
data class ReviewItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val identifier: String,
    val filename: String,
    val title: String,
    val creator: String,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "sync_statistics")
data class SyncStatsEntity(
    @PrimaryKey val id: Int = 1,
    val discoveredRecitersCount: Int,
    val discoveredEditionsCount: Int,
    val discoveredSurahsCount: Int,
    val duplicatesRemovedCount: Int,
    val validUrlsCount: Int,
    val failedUrlsCount: Int,
    val itemsNeedingReviewCount: Int,
    val lastSyncTimestamp: Long
)

@Entity(tableName = "quran_bookmarks")
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val surahNumber: Int,
    val surahName: String,
    val ayahNumber: Int,
    val pageNumber: Int,
    val juzNumber: Int,
    val ayahText: String,
    val note: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val colorHex: String = "#10B981"
)

@Entity(
    tableName = "custom_playlist_items",
    indices = [androidx.room.Index(value = ["playlistId", "orderIndex"])]
)
data class PlaylistItemEntity(
    @PrimaryKey val id: String,
    val playlistId: String,
    val surahNumber: Int,
    val surahName: String,
    val reciterId: String,
    val reciterName: String,
    val riwayah: String = "حفص عن عاصم",
    val orderIndex: Int = 0,
    val durationSeconds: Long? = null
)


