package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.dedup.AudioDeduplicationManager
import com.example.data.download.AudioDownloadManager
import com.example.data.local.db.AppDatabase
import com.example.data.local.db.AudioSourceEntity
import com.example.data.local.db.EditionEntity
import com.example.data.local.db.ReciterEntity
import com.example.data.local.db.ReviewItemEntity
import com.example.data.local.db.SurahRecordingEntity
import com.example.data.local.db.SyncStatsEntity
import com.example.data.local.db.BookmarkEntity
import com.example.data.local.db.PlaylistEntity
import com.example.data.local.db.PlaylistItemEntity
import com.example.data.model.AudioQualityLevel
import com.example.data.model.AudioSource
import com.example.data.model.Playlist
import com.example.data.model.PlaylistItem
import com.example.data.model.QuranBookmark
import com.example.data.model.QuranData
import com.example.data.model.ReaderTheme
import com.example.data.model.ReadingProgress
import com.example.data.model.RecitationEdition
import com.example.data.model.Reciter
import com.example.data.model.ReviewItem
import com.example.data.model.Surah
import com.example.data.model.SurahAudioItem
import com.example.data.model.SurahText
import com.example.data.model.SyncStatistics
import com.example.data.provider.InternetArchiveProvider
import com.example.data.provider.QuranTextProvider
import com.example.data.remote.Mp3QuranApi
import com.example.data.remote.Mp3QuranReciterDto
import com.example.data.remote.Mp3QuranRecitersResponse
import com.google.gson.Gson
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "quran_settings")

class QuranRepository(
    private val context: Context,
    val downloadManager: AudioDownloadManager
) {
    private val database = AppDatabase.getDatabase(context)
    private val dao = database.quranDao()
    private val archiveProvider = InternetArchiveProvider()
    private val dedupManager = AudioDeduplicationManager()
    private val mp3QuranApi = Mp3QuranApi.create()
    private val gson = Gson()

    private val moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private var cachedQuranData: QuranData? = null

    private val _syncState = MutableStateFlow(SyncStatistics())
    val syncState: StateFlow<SyncStatistics> = _syncState.asStateFlow()

    companion object {
        val KEY_LAST_RECITER_ID = stringPreferencesKey("last_reciter_id")
        val KEY_LAST_RECITER_NAME = stringPreferencesKey("last_reciter_name")
        val KEY_LAST_SURAH_NUM = intPreferencesKey("last_surah_num")
        val KEY_LAST_SURAH_NAME = stringPreferencesKey("last_surah_name")
        val KEY_LAST_EDITION_ID = stringPreferencesKey("last_edition_id")
        val KEY_LAST_POSITION_MS = longPreferencesKey("last_position_ms")
        val KEY_AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val KEY_PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val KEY_REPEAT_MODE = stringPreferencesKey("repeat_mode")
        val KEY_PREFERRED_QUALITY = stringPreferencesKey("preferred_quality")
        val KEY_WIFI_ONLY_DOWNLOAD = booleanPreferencesKey("wifi_only_download")
        val KEY_OFFLINE_MODE = booleanPreferencesKey("offline_mode")
        val KEY_FAVORITE_RECITERS = stringSetPreferencesKey("favorite_reciters")
        val KEY_FAVORITE_SURAHS = stringSetPreferencesKey("favorite_surahs")
        val KEY_SURAHS_VIEW_MODE = stringPreferencesKey("surahs_view_mode")
        val KEY_APP_THEME = stringPreferencesKey("app_theme")
        val KEY_DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val KEY_DAILY_REMINDER_TIME = stringPreferencesKey("daily_reminder_time")
        val KEY_READER_FONT_SCALE = floatPreferencesKey("reader_font_scale")
        val KEY_READER_THEME = stringPreferencesKey("reader_theme")
        val KEY_LAST_READ_SURAH = intPreferencesKey("last_read_surah")
        val KEY_LAST_READ_SURAH_NAME = stringPreferencesKey("last_read_surah_name")
        val KEY_LAST_READ_AYAH = intPreferencesKey("last_read_ayah")
        val KEY_LAST_READ_PAGE = intPreferencesKey("last_read_page")
        val KEY_LAST_READ_JUZ = intPreferencesKey("last_read_juz")
        val KEY_LAST_READ_TIME = longPreferencesKey("last_read_time")
    }

    val textProvider = QuranTextProvider(context)

    suspend fun initializeDatabase() = withContext(Dispatchers.IO) {
        val existingCount = dao.getRecitersCount()
        if (existingCount < 50) {
            // Seed database from bundled reciters.json assets & curated sources
            seedDatabase()
        }
        updateSyncStatsFromDb()

        // Asynchronously fetch full live list from mp3quran.net in the background
        try {
            syncLiveMp3QuranReciters()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getRecitersWithLiveRefresh(onUpdate: (List<Reciter>) -> Unit) = withContext(Dispatchers.IO) {
        // 1. First provide existing local reciters
        val currentLocal = dao.getAllReciters().first()
        if (currentLocal.isNotEmpty()) {
            onUpdate(currentLocal.map { entity ->
                Reciter(
                    id = entity.id,
                    name = entity.arabicName,
                    riwayah = entity.defaultRiwayah,
                    style = "مرتل ومجود",
                    location = entity.country,
                    serverTemplates = emptyList(),
                    totalEditionsCount = entity.totalEditionsCount
                )
            })
        }

        // 2. Fetch live and update
        try {
            syncLiveMp3QuranReciters()
            val updatedLocal = dao.getAllReciters().first()
            if (updatedLocal.isNotEmpty()) {
                onUpdate(updatedLocal.map { entity ->
                    Reciter(
                        id = entity.id,
                        name = entity.arabicName,
                        riwayah = entity.defaultRiwayah,
                        style = "مرتل ومجود",
                        location = entity.country,
                        serverTemplates = emptyList(),
                        totalEditionsCount = entity.totalEditionsCount
                    )
                })
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun seedDatabase() = withContext(Dispatchers.IO) {
        _syncState.value = _syncState.value.copy(
            isSyncing = true,
            syncProgressMessage = "جاري تهيئة قاعدة بيانات القراء والتلاوات..."
        )

        val surahs = getSurahs()
        val surahNameMap = surahs.associateBy { it.number }

        // 1. Load bundled reciters.json
        val recitersToInsert = mutableListOf<ReciterEntity>()
        val editionsToInsert = mutableListOf<EditionEntity>()
        val recordingsToInsert = mutableListOf<SurahRecordingEntity>()
        val sourcesToInsert = mutableListOf<AudioSourceEntity>()

        try {
            val assetStream = context.assets.open("reciters.json")
            val jsonText = assetStream.bufferedReader().use { it.readText() }
            val parsedResponse = gson.fromJson(jsonText, Mp3QuranRecitersResponse::class.java)

            for (dto in parsedResponse.reciters) {
                val reciterId = dto.id.toString()
                if (reciterId == "0" || dto.name.isBlank()) continue

                val moshafs = dto.moshafs
                val defaultRiwayah = moshafs.firstOrNull()?.name ?: "حفص عن عاصم"

                recitersToInsert.add(
                    ReciterEntity(
                        id = reciterId,
                        arabicName = dto.name,
                        englishName = dto.name,
                        country = dto.letter ?: "قارئ معتمد",
                        aliases = listOf(dto.name),
                        totalEditionsCount = if (moshafs.isNotEmpty()) moshafs.size else 1,
                        defaultRiwayah = defaultRiwayah
                    )
                )

                for (moshaf in moshafs) {
                    val editionId = "${reciterId}_${moshaf.id}"
                    val editionName = moshaf.name ?: "حفص عن عاصم"
                    val isComplete = moshaf.surahTotal >= 114

                    editionsToInsert.add(
                        EditionEntity(
                            editionId = editionId,
                            reciterId = reciterId,
                            reciterName = dto.name,
                            name = editionName,
                            riwayah = editionName,
                            recitationType = if (editionName.contains("مجود")) "مجوّد" else "مرتّل",
                            isCompleteQuran = isComplete,
                            surahCount = if (moshaf.surahTotal > 0) moshaf.surahTotal else 114,
                            archiveIdentifier = null,
                            license = "mp3quran.net"
                        )
                    )

                    val surahNums = if (moshaf.surahList.isNotEmpty()) moshaf.surahList else (1..114).toList()
                    val serverUrl = moshaf.server.trimEnd('/')

                    for (surahNum in surahNums) {
                        val recId = "${editionId}_surah_${String.format("%03d", surahNum)}"
                        val sName = surahNameMap[surahNum]?.name ?: "السورة $surahNum"

                        recordingsToInsert.add(
                            SurahRecordingEntity(
                                recordingId = recId,
                                reciterId = reciterId,
                                reciterName = dto.name,
                                editionId = editionId,
                                riwayah = editionName,
                                recitationType = if (editionName.contains("مجود")) "مجوّد" else "مرتّل",
                                surahNumber = surahNum,
                                surahName = sName,
                                title = "سورة $sName - ${dto.name}",
                                isCompleteQuran = isComplete
                            )
                        )

                        if (serverUrl.isNotBlank()) {
                            val audioUrl = "$serverUrl/${String.format("%03d", surahNum)}.mp3"
                            sourcesToInsert.add(
                                AudioSourceEntity(
                                    sourceUrl = audioUrl,
                                    recordingId = recId,
                                    sourceName = "mp3quran.net (الرئيسي)",
                                    sourceIdentifier = "mp3quran_${editionId}_$surahNum",
                                    format = "mp3",
                                    qualityKey = "192k",
                                    bitrateKbps = 192,
                                    fileSize = null,
                                    durationSeconds = null,
                                    license = "Free Distribution"
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Also add curated Internet Archive entries
        val curatedEntries = archiveProvider.getCuratedArchiveSeedEntries()
        val dedupResult = dedupManager.processAndDeduplicate(curatedEntries)

        dao.insertDiscoveredData(
            reciters = (recitersToInsert + dedupResult.reciters).distinctBy { it.id },
            editions = (editionsToInsert + dedupResult.editions).distinctBy { it.editionId },
            recordings = (recordingsToInsert + dedupResult.recordings).distinctBy { it.recordingId },
            sources = (sourcesToInsert + dedupResult.sources).distinctBy { it.sourceUrl },
            reviewItems = dedupResult.reviewItems
        )

        updateSyncStatsFromDb()
    }

    suspend fun syncLiveMp3QuranReciters(): Result<SyncStatistics> = withContext(Dispatchers.IO) {
        try {
            _syncState.value = _syncState.value.copy(
                isSyncing = true,
                syncProgressMessage = "جاري جلب قائمة القراء الكاملة من mp3quran.net..."
            )

            val response = mp3QuranApi.getReciters(language = "ar")
            val surahs = getSurahs()
            val surahNameMap = surahs.associateBy { it.number }

            val recitersToInsert = mutableListOf<ReciterEntity>()
            val editionsToInsert = mutableListOf<EditionEntity>()
            val recordingsToInsert = mutableListOf<SurahRecordingEntity>()
            val sourcesToInsert = mutableListOf<AudioSourceEntity>()

            for (dto in response.reciters) {
                val reciterId = dto.id.toString()
                if (reciterId == "0" || dto.name.isBlank()) continue

                val moshafs = dto.moshafs
                val defaultRiwayah = moshafs.firstOrNull()?.name ?: "حفص عن عاصم"

                recitersToInsert.add(
                    ReciterEntity(
                        id = reciterId,
                        arabicName = dto.name,
                        englishName = dto.name,
                        country = dto.letter ?: "قارئ معتمد",
                        aliases = listOf(dto.name),
                        totalEditionsCount = if (moshafs.isNotEmpty()) moshafs.size else 1,
                        defaultRiwayah = defaultRiwayah
                    )
                )

                for (moshaf in moshafs) {
                    val editionId = "${reciterId}_${moshaf.id}"
                    val editionName = moshaf.name ?: "حفص عن عاصم"
                    val isComplete = moshaf.surahTotal >= 114

                    editionsToInsert.add(
                        EditionEntity(
                            editionId = editionId,
                            reciterId = reciterId,
                            reciterName = dto.name,
                            name = editionName,
                            riwayah = editionName,
                            recitationType = if (editionName.contains("مجود")) "مجوّد" else "مرتّل",
                            isCompleteQuran = isComplete,
                            surahCount = if (moshaf.surahTotal > 0) moshaf.surahTotal else 114,
                            archiveIdentifier = null,
                            license = "mp3quran.net"
                        )
                    )

                    val surahNums = if (moshaf.surahList.isNotEmpty()) moshaf.surahList else (1..114).toList()
                    val serverUrl = moshaf.server.trimEnd('/')

                    for (surahNum in surahNums) {
                        val recId = "${editionId}_surah_${String.format("%03d", surahNum)}"
                        val sName = surahNameMap[surahNum]?.name ?: "السورة $surahNum"

                        recordingsToInsert.add(
                            SurahRecordingEntity(
                                recordingId = recId,
                                reciterId = reciterId,
                                reciterName = dto.name,
                                editionId = editionId,
                                riwayah = editionName,
                                recitationType = if (editionName.contains("مجود")) "مجوّد" else "مرتّل",
                                surahNumber = surahNum,
                                surahName = sName,
                                title = "سورة $sName - ${dto.name}",
                                isCompleteQuran = isComplete
                            )
                        )

                        if (serverUrl.isNotBlank()) {
                            val audioUrl = "$serverUrl/${String.format("%03d", surahNum)}.mp3"
                            sourcesToInsert.add(
                                AudioSourceEntity(
                                    sourceUrl = audioUrl,
                                    recordingId = recId,
                                    sourceName = "mp3quran.net (الرئيسي)",
                                    sourceIdentifier = "mp3quran_${editionId}_$surahNum",
                                    format = "mp3",
                                    qualityKey = "192k",
                                    bitrateKbps = 192,
                                    fileSize = null,
                                    durationSeconds = null,
                                    license = "Free Distribution"
                                )
                            )
                        }
                    }
                }
            }

            dao.insertDiscoveredData(
                reciters = recitersToInsert.distinctBy { it.id },
                editions = editionsToInsert.distinctBy { it.editionId },
                recordings = recordingsToInsert.distinctBy { it.recordingId },
                sources = sourcesToInsert.distinctBy { it.sourceUrl },
                reviewItems = emptyList()
            )

            updateSyncStatsFromDb()
            Result.success(_syncState.value)
        } catch (e: Exception) {
            e.printStackTrace()
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                syncProgressMessage = "اكتمل التحديث من البيانات المخزنة"
            )
            Result.failure(e)
        }
    }

    suspend fun syncInternetArchiveForReciter(query: String): Result<SyncStatistics> = withContext(Dispatchers.IO) {
        try {
            _syncState.value = _syncState.value.copy(
                isSyncing = true,
                syncProgressMessage = "جاري البحث في أرشيف الإنترنت عن: $query..."
            )

            // Search archive.org
            val docs = archiveProvider.searchArchiveItems(
                query = "$query AND mediatype:audio",
                rows = 15
            )

            val rawEntries = mutableListOf<AudioDeduplicationManager.RawAudioEntry>()
            for ((index, doc) in docs.withIndex()) {
                _syncState.value = _syncState.value.copy(
                    syncProgressMessage = "فحص ومعالجة المجموعة (${index + 1}/${docs.size}): ${doc.title ?: doc.identifier}..."
                )
                val itemEntries = archiveProvider.fetchItemRecordings(doc)
                rawEntries.addAll(itemEntries)
            }

            _syncState.value = _syncState.value.copy(
                syncProgressMessage = "إزالة التكرار وربط السور ومطابقة الصيغ..."
            )

            val dedupResult = dedupManager.processAndDeduplicate(rawEntries)

            dao.insertDiscoveredData(
                reciters = dedupResult.reciters,
                editions = dedupResult.editions,
                recordings = dedupResult.recordings,
                sources = dedupResult.sources,
                reviewItems = dedupResult.reviewItems
            )

            updateSyncStatsFromDb()

            Result.success(_syncState.value)
        } catch (e: Exception) {
            e.printStackTrace()
            _syncState.value = _syncState.value.copy(isSyncing = false, syncProgressMessage = "خطأ في المزامنة: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun updateSyncStatsFromDb() = withContext(Dispatchers.IO) {
        val recitersCount = dao.getRecitersCount()
        val editionsCount = dao.getEditionsCount()
        val totalSources = dao.getTotalSourcesCount()
        val reviewCount = dao.getReviewItemsCount()

        val updated = SyncStatistics(
            discoveredRecitersCount = recitersCount,
            discoveredEditionsCount = editionsCount,
            discoveredSurahsCount = 114,
            duplicatesRemovedCount = dedupManager.duplicatesRemovedCount.get(),
            validUrlsCount = totalSources,
            failedUrlsCount = 0,
            itemsNeedingReviewCount = reviewCount,
            lastSyncTimestamp = System.currentTimeMillis(),
            isSyncing = false,
            syncProgressMessage = "قاعدة البيانات محدثة ($recitersCount قارئ)"
        )
        _syncState.value = updated
    }

    suspend fun getQuranData(): QuranData = withContext(Dispatchers.IO) {
        cachedQuranData?.let { return@withContext it }
        try {
            val inputStream = context.assets.open("quran_data.json")
            val reader = InputStreamReader(inputStream)
            val adapter = moshi.adapter(QuranData::class.java)
            val data = adapter.fromJson(reader.readText()) ?: QuranData(emptyList(), emptyList())
            inputStream.close()
            cachedQuranData = data
            data
        } catch (e: Exception) {
            QuranData(emptyList(), emptyList())
        }
    }

    suspend fun getSurahs(): List<Surah> = withContext(Dispatchers.IO) {
        getQuranData().surahs
    }

    suspend fun getSurahByNumber(number: Int): Surah? = withContext(Dispatchers.IO) {
        getSurahs().find { it.number == number }
    }

    // Room Flows
    val recitersFlow: Flow<List<Reciter>> = dao.getAllReciters().map { entities ->
        entities.map { entity ->
            Reciter(
                id = entity.id,
                name = entity.arabicName,
                riwayah = entity.defaultRiwayah,
                style = "مرتل ومجود",
                location = entity.country,
                serverTemplates = emptyList(),
                totalEditionsCount = entity.totalEditionsCount
            )
        }
    }

    fun getEditionsForReciterFlow(reciterId: String): Flow<List<RecitationEdition>> =
        dao.getEditionsForReciter(reciterId).map { list ->
            list.map { it.toModel() }
        }

    suspend fun getEditionsForReciter(reciterId: String): List<RecitationEdition> = withContext(Dispatchers.IO) {
        dao.getEditionsListForReciter(reciterId).map { it.toModel() }
    }

    val reviewItemsFlow: Flow<List<ReviewItem>> = dao.getAllReviewItems().map { list ->
        list.map {
            ReviewItem(
                identifier = it.identifier,
                filename = it.filename,
                title = it.title,
                creator = it.creator,
                reason = it.reason,
                timestamp = it.timestamp
            )
        }
    }

    suspend fun buildAudioItemForEdition(
        reciterId: String,
        reciterName: String,
        editionId: String,
        riwayah: String,
        recitationType: String,
        surah: Surah,
        preferredQuality: AudioQualityLevel = AudioQualityLevel.KBPS_320
    ): SurahAudioItem = withContext(Dispatchers.IO) {
        val num3 = String.format("%03d", surah.number)
        val num = surah.number.toString()

        val recordingId = "${editionId}_surah_$num3"
        val sourceEntities = dao.getSourcesListForRecording(recordingId)

        val detailedSources = sourceEntities.map { entity ->
            AudioSource(
                source = entity.sourceName,
                sourceIdentifier = entity.sourceIdentifier,
                format = entity.format,
                qualityLevel = AudioQualityLevel.fromKey(entity.qualityKey),
                bitrateKbps = entity.bitrateKbps,
                url = entity.sourceUrl,
                fileSize = entity.fileSize,
                durationSeconds = entity.durationSeconds,
                license = entity.license
            )
        }.toMutableList()

        val urls = detailedSources.map { it.url }.toMutableList()

        // If no sources found in room, construct backup urls
        if (urls.isEmpty()) {
            val allEditions = dao.getEditionsListForReciter(reciterId)
            for (ed in allEditions) {
                val edRecId = "${ed.editionId}_surah_$num3"
                val edSources = dao.getSourcesListForRecording(edRecId)
                edSources.forEach {
                    if (!urls.contains(it.sourceUrl)) {
                        urls.add(it.sourceUrl)
                    }
                }
            }
        }

        // Add reliable fallback CDNs so playback never fails
        val fallbackCdn = "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/$num.mp3"
        if (!urls.contains(fallbackCdn)) {
            urls.add(fallbackCdn)
        }

        val downloadedFile = downloadManager.getExistingDownloadedFile(reciterId, surah.number)
        val localPath = downloadedFile?.absolutePath

        SurahAudioItem(
            reciterId = reciterId,
            reciterName = reciterName,
            editionId = editionId,
            riwayah = riwayah,
            recitationType = recitationType,
            surahNumber = surah.number,
            surahName = surah.name,
            ayahsCount = surah.ayahs,
            revelationType = surah.type,
            audioSources = urls,
            detailedSources = detailedSources,
            localFilePath = localPath,
            activeQuality = preferredQuality
        )
    }

    // DataStore Preferences
    val preferredQualityFlow: Flow<AudioQualityLevel> = context.dataStore.data.map { prefs ->
        val key = prefs[KEY_PREFERRED_QUALITY]
        AudioQualityLevel.fromKey(key)
    }

    val wifiOnlyDownloadFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_WIFI_ONLY_DOWNLOAD] ?: true
    }

    val offlineModeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_OFFLINE_MODE] ?: false
    }

    val lastPlayedFlow: Flow<SurahAudioItem?> = context.dataStore.data.map { prefs ->
        val reciterId = prefs[KEY_LAST_RECITER_ID] ?: return@map null
        val surahNum = prefs[KEY_LAST_SURAH_NUM] ?: return@map null
        val reciterName = prefs[KEY_LAST_RECITER_NAME] ?: ""
        val editionId = prefs[KEY_LAST_EDITION_ID] ?: ""
        val surah = getSurahs().find { it.number == surahNum }
        val preferredQ = AudioQualityLevel.fromKey(prefs[KEY_PREFERRED_QUALITY])

        if (surah != null) {
            buildAudioItemForEdition(
                reciterId = reciterId,
                reciterName = reciterName,
                editionId = editionId,
                riwayah = "حفص عن عاصم",
                recitationType = "مرتّل ومجوّد",
                surah = surah,
                preferredQuality = preferredQ
            )
        } else null
    }

    val autoPlayNextFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_PLAY_NEXT] ?: true
    }

    val playbackSpeedFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_PLAYBACK_SPEED] ?: 1.0f
    }

    val favoriteRecitersFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_FAVORITE_RECITERS] ?: emptySet()
    }

    val favoriteSurahsFlow: Flow<Set<Int>> = context.dataStore.data.map { prefs ->
        prefs[KEY_FAVORITE_SURAHS]?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    }

    val surahsViewModeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SURAHS_VIEW_MODE] ?: "grid_3"
    }

    val appThemeFlow: Flow<String> = flow {
        // 1. Emit verified local mirror immediately for zero-flicker cold-start
        val initialTheme = getCachedAppTheme()
        emit(initialTheme)

        // 2. Collect authoritative DataStore asynchronously
        context.dataStore.data
            .catch { e ->
                // Guard: Never emit empty preferences on transient failure; preserve current state
                Log.w("QuranRepository", "DataStore read error occurred, retaining active theme", e)
            }
            .collect { prefs ->
                val raw = prefs[KEY_APP_THEME]
                if (raw != null) {
                    val validatedTheme = if (raw in listOf("light", "dark", "system")) raw else "system"
                    // Reconcile mirror cache on every emission from DataStore (source of truth)
                    try {
                        val sp = context.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE)
                        val current = sp.getString("app_theme", null)
                        if (current != validatedTheme) {
                            sp.edit().putString("app_theme", validatedTheme).commit()
                        }
                    } catch (_: Exception) {}
                    emit(validatedTheme)
                }
            }
    }.distinctUntilChanged()

    val dailyReminderEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DAILY_REMINDER_ENABLED] ?: false
    }

    val dailyReminderTimeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DAILY_REMINDER_TIME] ?: "20:00"
    }

    val lastPlayedPositionMsFlow: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_POSITION_MS] ?: 0L
    }

    suspend fun saveLastPlayed(item: SurahAudioItem, positionMs: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_RECITER_ID] = item.reciterId
            prefs[KEY_LAST_RECITER_NAME] = item.reciterName
            prefs[KEY_LAST_SURAH_NUM] = item.surahNumber
            prefs[KEY_LAST_SURAH_NAME] = item.surahName
            prefs[KEY_LAST_EDITION_ID] = item.editionId
            prefs[KEY_LAST_POSITION_MS] = positionMs
        }
    }

    suspend fun toggleFavoriteSurah(surahNumber: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_FAVORITE_SURAHS] ?: emptySet()
            val strNum = surahNumber.toString()
            if (current.contains(strNum)) {
                prefs[KEY_FAVORITE_SURAHS] = current - strNum
            } else {
                prefs[KEY_FAVORITE_SURAHS] = current + strNum
            }
        }
    }

    suspend fun setSurahsViewMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SURAHS_VIEW_MODE] = mode
        }
    }

    suspend fun setAppTheme(theme: String) {
        val validTheme = if (theme in listOf("light", "dark", "system")) theme else "system"
        // 1. DataStore is authoritative: write to DataStore first
        context.dataStore.edit { prefs ->
            prefs[KEY_APP_THEME] = validTheme
        }
        // 2. Update mirror cache only after successful DataStore persistence
        try {
            context.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("app_theme", validTheme)
                .apply()
        } catch (_: Exception) {}
    }

    fun getCachedAppTheme(): String {
        return try {
            val sp = context.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE)
            val cached = sp.getString("app_theme", null)
            if (cached != null && cached in listOf("light", "dark", "system")) {
                cached
            } else {
                "system"
            }
        } catch (_: Exception) {
            "system"
        }
    }

    suspend fun setDailyReminder(enabled: Boolean, time: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DAILY_REMINDER_ENABLED] = enabled
            prefs[KEY_DAILY_REMINDER_TIME] = time
        }
    }

    suspend fun setPreferredQuality(quality: AudioQualityLevel) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PREFERRED_QUALITY] = quality.key
        }
    }

    suspend fun setWifiOnlyDownload(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WIFI_ONLY_DOWNLOAD] = enabled
        }
    }

    suspend fun setOfflineMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_OFFLINE_MODE] = enabled
        }
    }

    suspend fun setAutoPlayNext(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_PLAY_NEXT] = enabled
        }
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PLAYBACK_SPEED] = speed
        }
    }

    suspend fun toggleFavoriteReciter(reciterId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_FAVORITE_RECITERS] ?: emptySet()
            if (current.contains(reciterId)) {
                prefs[KEY_FAVORITE_RECITERS] = current - reciterId
            } else {
                prefs[KEY_FAVORITE_RECITERS] = current + reciterId
            }
        }
    }

    // Reader Preferences & Bookmarks
    val readerFontScaleFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_READER_FONT_SCALE] ?: 1.0f
    }

    val readerThemeFlow: Flow<ReaderTheme> = context.dataStore.data.map { prefs ->
        val id = prefs[KEY_READER_THEME] ?: "sepia"
        ReaderTheme.values().find { it.id == id } ?: ReaderTheme.WARM_PAPER
    }

    val readingProgressFlow: Flow<ReadingProgress> = context.dataStore.data.map { prefs ->
        ReadingProgress(
            surahNumber = prefs[KEY_LAST_READ_SURAH] ?: 1,
            surahName = prefs[KEY_LAST_READ_SURAH_NAME] ?: "الفاتحة",
            ayahNumber = prefs[KEY_LAST_READ_AYAH] ?: 1,
            pageNumber = prefs[KEY_LAST_READ_PAGE] ?: 1,
            juzNumber = prefs[KEY_LAST_READ_JUZ] ?: 1,
            timestamp = prefs[KEY_LAST_READ_TIME] ?: System.currentTimeMillis()
        )
    }

    val bookmarksFlow: Flow<List<QuranBookmark>> = dao.getAllBookmarks().map { entities ->
        entities.map { entity ->
            QuranBookmark(
                id = entity.id,
                surahNumber = entity.surahNumber,
                surahName = entity.surahName,
                ayahNumber = entity.ayahNumber,
                pageNumber = entity.pageNumber,
                juzNumber = entity.juzNumber,
                ayahText = entity.ayahText,
                note = entity.note,
                timestamp = entity.timestamp
            )
        }
    }

    suspend fun saveReadingProgress(progress: ReadingProgress) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_READ_SURAH] = progress.surahNumber
            prefs[KEY_LAST_READ_SURAH_NAME] = progress.surahName
            prefs[KEY_LAST_READ_AYAH] = progress.ayahNumber
            prefs[KEY_LAST_READ_PAGE] = progress.pageNumber
            prefs[KEY_LAST_READ_JUZ] = progress.juzNumber
            prefs[KEY_LAST_READ_TIME] = System.currentTimeMillis()
        }
    }

    suspend fun setReaderFontScale(scale: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_READER_FONT_SCALE] = scale
        }
    }

    suspend fun setReaderTheme(theme: ReaderTheme) {
        context.dataStore.edit { prefs ->
            prefs[KEY_READER_THEME] = theme.id
        }
    }

    suspend fun addBookmark(bookmark: QuranBookmark) {
        dao.insertBookmark(
            BookmarkEntity(
                id = bookmark.id,
                surahNumber = bookmark.surahNumber,
                surahName = bookmark.surahName,
                ayahNumber = bookmark.ayahNumber,
                pageNumber = bookmark.pageNumber,
                juzNumber = bookmark.juzNumber,
                ayahText = bookmark.ayahText,
                note = bookmark.note,
                timestamp = bookmark.timestamp
            )
        )
    }

    suspend fun deleteBookmark(id: String) {
        dao.deleteBookmark(id)
    }

    suspend fun isBookmarked(surahNumber: Int, ayahNumber: Int): Boolean {
        return dao.isBookmarked(surahNumber, ayahNumber)
    }

    suspend fun getSurahText(surahNumber: Int): SurahText {
        return textProvider.getSurahText(surahNumber)
    }

    fun isSurahDownloaded(surahNumber: Int): Boolean {
        return textProvider.isSurahDownloaded(surahNumber)
    }

    fun getDownloadedSurahsCount(): Int {
        return textProvider.getDownloadedSurahsCount()
    }

    suspend fun ensureSurahPersisted(surahNumber: Int): com.example.data.provider.PersistenceResult {
        return textProvider.ensureSurahPersisted(surahNumber)
    }

    // Custom Playlists Repository
    val playlistsFlow: Flow<List<Playlist>> = dao.getAllPlaylists().map { entities ->
        entities.map { entity ->
            val count = dao.getPlaylistItemCount(entity.id)
            Playlist(
                id = entity.id,
                name = entity.name,
                description = entity.description,
                createdAt = entity.createdAt,
                colorHex = entity.colorHex,
                itemsCount = count
            )
        }
    }

    fun getPlaylistItemsFlow(playlistId: String): Flow<List<PlaylistItem>> {
        return dao.getPlaylistItems(playlistId).map { list ->
            list.map {
                PlaylistItem(
                    id = it.id,
                    playlistId = it.playlistId,
                    surahNumber = it.surahNumber,
                    surahName = it.surahName,
                    reciterId = it.reciterId,
                    reciterName = it.reciterName,
                    riwayah = it.riwayah,
                    orderIndex = it.orderIndex,
                    durationSeconds = it.durationSeconds
                )
            }
        }
    }

    suspend fun createPlaylist(name: String, description: String = "", colorHex: String = "#10B981"): String {
        val id = java.util.UUID.randomUUID().toString()
        dao.insertPlaylist(
            PlaylistEntity(
                id = id,
                name = name.trim(),
                description = description.trim(),
                createdAt = System.currentTimeMillis(),
                colorHex = colorHex
            )
        )
        return id
    }

    suspend fun deletePlaylist(playlistId: String) {
        dao.clearPlaylistItems(playlistId)
        dao.deletePlaylist(playlistId)
    }

    suspend fun addSurahToPlaylist(
        playlistId: String,
        surahNumber: Int,
        surahName: String,
        reciterId: String,
        reciterName: String,
        riwayah: String = "حفص عن عاصم"
    ) {
        val currentCount = dao.getPlaylistItemCount(playlistId)
        dao.insertPlaylistItem(
            PlaylistItemEntity(
                id = java.util.UUID.randomUUID().toString(),
                playlistId = playlistId,
                surahNumber = surahNumber,
                surahName = surahName,
                reciterId = reciterId,
                reciterName = reciterName,
                riwayah = riwayah,
                orderIndex = currentCount,
                durationSeconds = null
            )
        )
    }

    suspend fun removePlaylistItem(itemId: String) {
        dao.deletePlaylistItem(itemId)
    }

    suspend fun getPlaylistItems(playlistId: String): List<PlaylistItem> {
        return dao.getPlaylistItemsList(playlistId).map {
            PlaylistItem(
                id = it.id,
                playlistId = it.playlistId,
                surahNumber = it.surahNumber,
                surahName = it.surahName,
                reciterId = it.reciterId,
                reciterName = it.reciterName,
                riwayah = it.riwayah,
                orderIndex = it.orderIndex,
                durationSeconds = it.durationSeconds
            )
        }
    }

    private fun EditionEntity.toModel() = RecitationEdition(
        editionId = editionId,
        reciterId = reciterId,
        reciterName = reciterName,
        name = name,
        riwayah = riwayah,
        recitationType = recitationType,
        isCompleteQuran = isCompleteQuran,
        surahCount = surahCount,
        archiveIdentifier = archiveIdentifier,
        license = license
    )
}
