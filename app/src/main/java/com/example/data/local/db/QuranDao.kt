package com.example.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranDao {

    // Reciters
    @Query("SELECT * FROM reciters ORDER BY arabicName ASC")
    fun getAllReciters(): Flow<List<ReciterEntity>>

    @Query("SELECT * FROM reciters WHERE id = :id LIMIT 1")
    suspend fun getReciterById(id: String): ReciterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReciters(reciters: List<ReciterEntity>)

    // Editions
    @Query("SELECT * FROM editions WHERE reciterId = :reciterId ORDER BY isCompleteQuran DESC, name ASC")
    fun getEditionsForReciter(reciterId: String): Flow<List<EditionEntity>>

    @Query("SELECT * FROM editions WHERE reciterId = :reciterId")
    suspend fun getEditionsListForReciter(reciterId: String): List<EditionEntity>

    @Query("SELECT * FROM editions WHERE editionId = :editionId LIMIT 1")
    suspend fun getEditionById(editionId: String): EditionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEditions(editions: List<EditionEntity>)

    // Surah Recordings
    @Query("SELECT * FROM surah_recordings WHERE editionId = :editionId ORDER BY surahNumber ASC")
    fun getSurahRecordingsForEdition(editionId: String): Flow<List<SurahRecordingEntity>>

    @Query("SELECT * FROM surah_recordings WHERE reciterId = :reciterId ORDER BY surahNumber ASC")
    fun getSurahRecordingsForReciter(reciterId: String): Flow<List<SurahRecordingEntity>>

    @Query("SELECT * FROM surah_recordings WHERE recordingId = :recordingId LIMIT 1")
    suspend fun getSurahRecordingById(recordingId: String): SurahRecordingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurahRecordings(recordings: List<SurahRecordingEntity>)

    // Audio Sources
    @Query("SELECT * FROM audio_sources WHERE recordingId = :recordingId")
    fun getSourcesForRecording(recordingId: String): Flow<List<AudioSourceEntity>>

    @Query("SELECT * FROM audio_sources WHERE recordingId = :recordingId")
    suspend fun getSourcesListForRecording(recordingId: String): List<AudioSourceEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAudioSources(sources: List<AudioSourceEntity>)

    // Review Items
    @Query("SELECT * FROM review_items ORDER BY timestamp DESC")
    fun getAllReviewItems(): Flow<List<ReviewItemEntity>>

    @Query("SELECT COUNT(*) FROM review_items")
    suspend fun getReviewItemsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviewItems(items: List<ReviewItemEntity>)

    @Query("DELETE FROM review_items")
    suspend fun clearReviewItems()

    // Sync Statistics
    @Query("SELECT * FROM sync_statistics WHERE id = 1 LIMIT 1")
    fun getSyncStats(): Flow<SyncStatsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSyncStats(stats: SyncStatsEntity)

    // Counts
    @Query("SELECT COUNT(*) FROM reciters")
    suspend fun getRecitersCount(): Int

    @Query("SELECT COUNT(*) FROM editions")
    suspend fun getEditionsCount(): Int

    @Query("SELECT COUNT(*) FROM audio_sources")
    suspend fun getTotalSourcesCount(): Int

    // Bookmarks
    @Query("SELECT * FROM quran_bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM quran_bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: String)

    @Query("DELETE FROM quran_bookmarks WHERE surahNumber = :surahNumber AND ayahNumber = :ayahNumber")
    suspend fun deleteBookmarkBySurahAndAyah(surahNumber: Int, ayahNumber: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM quran_bookmarks WHERE surahNumber = :surahNumber AND ayahNumber = :ayahNumber)")
    suspend fun isBookmarked(surahNumber: Int, ayahNumber: Int): Boolean

    // Custom Playlists
    @Query("SELECT * FROM custom_playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM custom_playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylistById(id: String): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM custom_playlists WHERE id = :id")
    suspend fun deletePlaylist(id: String)

    @Query("SELECT * FROM custom_playlist_items WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    fun getPlaylistItems(playlistId: String): Flow<List<PlaylistItemEntity>>

    @Query("SELECT * FROM custom_playlist_items WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    suspend fun getPlaylistItemsList(playlistId: String): List<PlaylistItemEntity>

    @Query("SELECT COUNT(*) FROM custom_playlist_items WHERE playlistId = :playlistId")
    suspend fun getPlaylistItemCount(playlistId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(item: PlaylistItemEntity)

    @Query("DELETE FROM custom_playlist_items WHERE id = :id")
    suspend fun deletePlaylistItem(id: String)

    @Query("DELETE FROM custom_playlist_items WHERE playlistId = :playlistId")
    suspend fun clearPlaylistItems(playlistId: String)

    @Transaction
    suspend fun insertDiscoveredData(
        reciters: List<ReciterEntity>,
        editions: List<EditionEntity>,
        recordings: List<SurahRecordingEntity>,
        sources: List<AudioSourceEntity>,
        reviewItems: List<ReviewItemEntity>
    ) {
        insertReciters(reciters)
        insertEditions(editions)
        insertSurahRecordings(recordings)
        insertAudioSources(sources)
        if (reviewItems.isNotEmpty()) {
            insertReviewItems(reviewItems)
        }
    }
}
