package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.download.AudioDownloadManager
import com.example.data.download.StorageFileInfo
import com.example.data.local.QuranRepository
import com.example.data.local.ThemeInitializationState
import com.example.data.model.AudioQualityLevel
import com.example.data.model.Ayah
import com.example.data.model.BulkDownloadProgress
import com.example.data.model.BulkDownloadStatus
import com.example.data.model.DownloadState
import com.example.data.model.OfflineTextDownloadState
import com.example.data.model.QuranTextUiState
import com.example.data.model.Playlist
import com.example.data.model.PlaylistItem
import com.example.data.model.PlayerEvent
import com.example.data.model.PlayerRepeatMode
import com.example.data.model.PlayerState
import com.example.data.model.QuranBookmark
import com.example.data.model.ReaderTheme
import com.example.data.model.ReadingProgress
import com.example.data.model.RecitationEdition
import com.example.data.model.Reciter
import com.example.data.model.ReciterStorageInfo
import com.example.data.model.ReviewItem
import com.example.data.model.SleepTimerMode
import com.example.data.model.Surah
import com.example.data.model.SurahAudioItem
import com.example.data.model.SurahText
import com.example.data.model.SyncStatistics
import com.example.data.provider.AyahTiming
import com.example.data.provider.PersistenceResult
import com.example.data.provider.QuranTimingManager
import com.example.data.provider.SurahTiming
import com.example.data.provider.TafseerDownloadProgress
import com.example.data.provider.TafseerManager
import com.example.data.provider.TafseerUiState
import com.example.data.remote.TafseerItem
import com.example.haram.HaramLiveStreamManager
import com.example.haram.HaramLocation
import com.example.haram.HaramNotificationManager
import com.example.haram.HaramNotificationMode
import com.example.haram.HaramNotificationPreferences
import com.example.haram.HaramNowPlaying
import com.example.haram.HaramTargetSource
import com.example.playback.AudioPlayerManager
import com.example.prayer.manager.PrayerManager
import com.example.watch.MeetingModeManager
import com.example.watch.SmartwatchBridgeManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecitersViewModel(private val repository: QuranRepository) : ViewModel() {

    // متغير بيحمل قايمة القراء الحالية اللي الشاشة بتعرضها
    private val _reciters = MutableStateFlow<List<Reciter>>(emptyList())
    val reciters: StateFlow<List<Reciter>> = _reciters

    init {
        // بمجرد ما الشاشة تفتح، ابدأ اجلب القراء
        viewModelScope.launch {
            repository.getRecitersWithLiveRefresh { updatedList ->
                _reciters.value = updatedList   // كل مرة القايمة تتحدث، الشاشة بتتحدث تلقائيًا معاها
            }
        }
    }
}

class QuranViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    val downloadManager = AudioDownloadManager(context)
    val repository = QuranRepository(context, downloadManager)
    val playerManager = AudioPlayerManager(context, viewModelScope)
    val tafseerManager = TafseerManager(context)
    val timingManager = QuranTimingManager(context)
    val meetingModeManager = MeetingModeManager(context, viewModelScope)
    val smartwatchBridgeManager = SmartwatchBridgeManager(context, meetingModeManager)
    val prayerManager = PrayerManager(context, viewModelScope)
    val haramNotificationManager = HaramNotificationManager(context)
    val haramLiveStreamManager = HaramLiveStreamManager(context, viewModelScope, haramNotificationManager)

    val haramPreferences: StateFlow<HaramNotificationPreferences> = haramNotificationManager.preferencesFlow
    val makkahNowPlaying: StateFlow<HaramNowPlaying> = haramLiveStreamManager.makkahNowPlaying
    val madinahNowPlaying: StateFlow<HaramNowPlaying> = haramLiveStreamManager.madinahNowPlaying

    private val _isHaramSettingsSheetVisible = MutableStateFlow(false)
    val isHaramSettingsSheetVisible: StateFlow<Boolean> = _isHaramSettingsSheetVisible.asStateFlow()

    private val _isMeetingAndWatchSheetVisible = MutableStateFlow(false)
    val isMeetingAndWatchSheetVisible: StateFlow<Boolean> = _isMeetingAndWatchSheetVisible.asStateFlow()

    private val _isMeetingAndWatchInitialTab = MutableStateFlow(0)
    val isMeetingAndWatchInitialTab: StateFlow<Int> = _isMeetingAndWatchInitialTab.asStateFlow()

    val reciters: StateFlow<List<Reciter>> = repository.recitersFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _surahs = MutableStateFlow<List<Surah>>(emptyList())
    val surahs: StateFlow<List<Surah>> = _surahs.asStateFlow()

    private val _selectedReciter = MutableStateFlow<Reciter?>(null)
    val selectedReciter: StateFlow<Reciter?> = _selectedReciter.asStateFlow()

    private val _selectedEdition = MutableStateFlow<RecitationEdition?>(null)
    val selectedEdition: StateFlow<RecitationEdition?> = _selectedEdition.asStateFlow()

    private val _editionsForReciter = MutableStateFlow<List<RecitationEdition>>(emptyList())
    val editionsForReciter: StateFlow<List<RecitationEdition>> = _editionsForReciter.asStateFlow()

    private val _searchReciterQuery = MutableStateFlow("")
    val searchReciterQuery: StateFlow<String> = _searchReciterQuery.asStateFlow()

    private val _searchSurahQuery = MutableStateFlow("")
    val searchSurahQuery: StateFlow<String> = _searchSurahQuery.asStateFlow()

    private val _selectedRiwayahFilter = MutableStateFlow("الكل")
    val selectedRiwayahFilter: StateFlow<String> = _selectedRiwayahFilter.asStateFlow()

    private val _isNetworkConnected = MutableStateFlow(true)
    val isNetworkConnected: StateFlow<Boolean> = _isNetworkConnected.asStateFlow()

    private val _isPlayerSheetExpanded = MutableStateFlow(false)
    val isPlayerSheetExpanded: StateFlow<Boolean> = _isPlayerSheetExpanded.asStateFlow()

    private val _isQualityDialogVisible = MutableStateFlow(false)
    val isQualityDialogVisible: StateFlow<Boolean> = _isQualityDialogVisible.asStateFlow()

    private val _isSyncDialogVisible = MutableStateFlow(false)
    val isSyncDialogVisible: StateFlow<Boolean> = _isSyncDialogVisible.asStateFlow()

    private val _isStorageDialogVisible = MutableStateFlow(false)
    val isStorageDialogVisible: StateFlow<Boolean> = _isStorageDialogVisible.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    val playerState: StateFlow<PlayerState> = playerManager.playerState
    val downloadStates: StateFlow<Map<String, DownloadState>> = downloadManager.downloadStates
    val syncState: StateFlow<SyncStatistics> = repository.syncState
    val reviewItems: StateFlow<List<ReviewItem>> = repository.reviewItemsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val preferredQuality: StateFlow<AudioQualityLevel> = repository.preferredQualityFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, AudioQualityLevel.KBPS_320)

    val wifiOnlyDownload: StateFlow<Boolean> = repository.wifiOnlyDownloadFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    val offlineMode: StateFlow<Boolean> = repository.offlineModeFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val lastPlayedItem: StateFlow<SurahAudioItem?> = repository.lastPlayedFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val favoriteReciters: StateFlow<Set<String>> = repository.favoriteRecitersFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    val favoriteSurahs: StateFlow<Set<Int>> = repository.favoriteSurahsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    val surahsViewMode: StateFlow<String> = repository.surahsViewModeFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, "grid_3")

    val themeInitializationState: StateFlow<ThemeInitializationState> = repository.themeInitializationState

    val appTheme: StateFlow<String> = repository.appThemeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, repository.getCachedAppTheme())

    val dailyReminderEnabled: StateFlow<Boolean> = repository.dailyReminderEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val dailyReminderTime: StateFlow<String> = repository.dailyReminderTimeFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, "20:00")

    val lastPlayedPositionMs: StateFlow<Long> = repository.lastPlayedPositionMsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, 0L)

    val bulkDownloadProgress: StateFlow<BulkDownloadProgress> = downloadManager.bulkDownloadProgress
    val offlineTextDownloadState: StateFlow<OfflineTextDownloadState> = downloadManager.offlineTextDownloadState

    val storageSummary: StateFlow<Triple<Int, Long, String>> = downloadStates
        .map { downloadManager.getStorageSummary() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), downloadManager.getStorageSummary())

    val downloadedFiles: StateFlow<List<StorageFileInfo>> = downloadStates
        .map { downloadManager.getDownloadedFilesList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), downloadManager.getDownloadedFilesList())

    val recitersStorageBreakdown: StateFlow<List<ReciterStorageInfo>> = combine(
        reciters,
        downloadStates
    ) { allReciters, _ ->
        downloadManager.getRecitersStorageBreakdown(allReciters)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Quran Reader State
    val readerFontScale: StateFlow<Float> = repository.readerFontScaleFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, 1.0f)

    val readerTheme: StateFlow<ReaderTheme> = repository.readerThemeFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, ReaderTheme.WARM_PAPER)

    val readingProgress: StateFlow<ReadingProgress> = repository.readingProgressFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, ReadingProgress())

    val bookmarks: StateFlow<List<QuranBookmark>> = repository.bookmarksFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Custom Playlists State
    val playlists: StateFlow<List<Playlist>> = repository.playlistsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private var loadSurahJob: Job? = null
    private var currentSurahRequestId: Long = 0L

    private val _quranTextUiState = MutableStateFlow<QuranTextUiState>(QuranTextUiState.Idle(1))
    val quranTextUiState: StateFlow<QuranTextUiState> = _quranTextUiState.asStateFlow()

    private val _currentSurahText = MutableStateFlow<SurahText?>(null)
    val currentSurahText: StateFlow<SurahText?> = _currentSurahText.asStateFlow()

    private val _isLoadingSurahText = MutableStateFlow(false)
    val isLoadingSurahText: StateFlow<Boolean> = _isLoadingSurahText.asStateFlow()

    private val _selectedAyahForTafsir = MutableStateFlow<Ayah?>(null)
    val selectedAyahForTafsir: StateFlow<Ayah?> = _selectedAyahForTafsir.asStateFlow()

    // Multiple Tafseer Management
    private val _availableTafseers = MutableStateFlow<List<TafseerItem>>(emptyList())
    val availableTafseers: StateFlow<List<TafseerItem>> = _availableTafseers.asStateFlow()

    private val _selectedTafseerId = MutableStateFlow(tafseerManager.getSelectedTafseerId())
    val selectedTafseerId: StateFlow<Int> = _selectedTafseerId.asStateFlow()

    private val _tafseerUiState = MutableStateFlow(TafseerUiState(tafseerId = tafseerManager.getSelectedTafseerId()))
    val tafseerUiState: StateFlow<TafseerUiState> = _tafseerUiState.asStateFlow()

    private val _tafseerDownloadProgress = MutableStateFlow<TafseerDownloadProgress?>(null)
    val tafseerDownloadProgress: StateFlow<TafseerDownloadProgress?> = _tafseerDownloadProgress.asStateFlow()
    private var tafseerDownloadJob: Job? = null

    // Read-Along Audio & Word Timing
    private val _currentSurahTiming = MutableStateFlow<SurahTiming?>(null)
    val currentSurahTiming: StateFlow<SurahTiming?> = _currentSurahTiming.asStateFlow()

    val activePlaybackPosition: StateFlow<Pair<Int?, Int?>> = combine(
        playerManager.playerState,
        _currentSurahTiming,
        _currentSurahText
    ) { pState, timing, sText ->
        val currentSurah = sText?.number ?: pState.currentItem?.surahNumber
        if (pState.isPlaying && pState.currentItem?.surahNumber == currentSurah && timing != null) {
            timingManager.findActivePosition(timing, pState.currentPositionMs)
        } else {
            Pair(null, null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(null, null))

    val activeAyahNumber: StateFlow<Int?> = activePlaybackPosition
        .map { it.first }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeWordIndex: StateFlow<Int?> = activePlaybackPosition
        .map { it.second }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val filteredReciters: StateFlow<List<Reciter>> = combine(
        reciters,
        _searchReciterQuery,
        _selectedRiwayahFilter,
        favoriteReciters
    ) { allReciters, query, filter, favs ->
        var list = allReciters
        if (filter != "الكل") {
            list = when (filter) {
                "المفضلة" -> list.filter { favs.contains(it.id) }
                "مرتل" -> list.filter { it.style.contains("مرتل") }
                "مجود" -> list.filter { it.style.contains("مجود") }
                else -> list.filter { it.riwayah.contains(filter) }
            }
        }
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.name.lowercase().contains(q) ||
                it.riwayah.lowercase().contains(q) ||
                it.location.lowercase().contains(q)
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredSurahs: StateFlow<List<Surah>> = combine(
        _surahs,
        _searchSurahQuery
    ) { allSurahs, query ->
        if (query.isBlank()) {
            allSurahs
        } else {
            val q = query.trim().lowercase()
            allSurahs.filter {
                it.name.contains(q) ||
                it.englishName.lowercase().contains(q) ||
                it.number.toString() == q ||
                it.formattedNumber.contains(q)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        initializeData()
        observeNetwork()
        observePlayerEvents()
        loadAvailableTafseers()

        playerManager.onTrackEndedCallback = {
            playNextSurah()
        }
        playerManager.onNextTrackCallback = {
            playNextSurah()
        }
        playerManager.onPreviousTrackCallback = {
            playPreviousSurah()
        }

        viewModelScope.launch {
            playerManager.playerState.collect { pState ->
                val currentItem = pState.currentItem
                if (currentItem != null && pState.isPlaying) {
                    val sNum = currentItem.surahNumber
                    if (_currentSurahTiming.value?.surahNumber != sNum) {
                        loadTimingForSurah(sNum, _selectedReciter.value, pState.durationMs)
                    }
                }
                // Sync with smartwatch tile and wearable media session
                smartwatchBridgeManager.updatePlaybackState(
                    playerState = pState,
                    favoriteRecitersCount = favoriteReciters.value.size,
                    currentAyahNumber = activeAyahNumber.value
                )
            }
        }

        // Configure Meeting Mode Callbacks
        meetingModeManager.onMeetingActivatedAction = { isManual ->
            if (playerManager.playerState.value.isPlaying && meetingModeManager.meetingState.value.pausePlaybackOnMeeting) {
                val currentItem = playerManager.playerState.value.currentItem
                meetingModeManager.savePlaybackSnapshot(
                    surahNumber = currentItem?.surahNumber,
                    surahName = currentItem?.surahName,
                    ayahNumber = activeAyahNumber.value ?: 1,
                    reciterName = currentItem?.reciterName,
                    positionMs = playerManager.playerState.value.currentPositionMs
                )
                playerManager.pause()
                viewModelScope.launch {
                    _snackbarMessage.emit("🔕 تم تفعيل وضع الاجتماعات وكتم التلاوة بنجاح")
                }
            }
        }

        meetingModeManager.onMeetingEndedAction = { lastSurah, lastAyah, reciter ->
            viewModelScope.launch {
                _snackbarMessage.emit(" انتهى الاجتماع • يمكنك استئناف الاستماع من حيث توقفت")
            }
        }

        // Configure Smartwatch Wrist Commands
        smartwatchBridgeManager.onWatchTogglePlayPause = {
            togglePlayPause()
        }
        smartwatchBridgeManager.onWatchNextSurah = {
            playNextSurah()
        }
        smartwatchBridgeManager.onWatchPreviousSurah = {
            playPreviousSurah()
        }
        smartwatchBridgeManager.onWatchSelectSurah = { surahNum ->
            val surah = _surahs.value.find { it.number == surahNum }
            if (surah != null) {
                playSurah(surah)
            }
        }
        smartwatchBridgeManager.onWatchSelectReciter = { reciter ->
            selectReciter(reciter)
        }
        smartwatchBridgeManager.onWatchResumeLastSaved = { surahNum, ayahNum ->
            val surah = _surahs.value.find { it.number == surahNum }
            if (surah != null) {
                playSurah(surah)
            }
        }

        viewModelScope.launch {
            repository.autoPlayNextFlow.collect { enabled ->
                playerManager.setAutoPlayNext(enabled)
            }
        }
        viewModelScope.launch {
            repository.playbackSpeedFlow.collect { speed ->
                playerManager.setPlaybackSpeed(speed)
            }
        }
    }

    private fun initializeData() {
        viewModelScope.launch {
            try {
                _surahs.value = repository.getSurahs()
                repository.initializeDatabase()

                val recList = repository.recitersFlow.first()
                if (recList.isNotEmpty() && _selectedReciter.value == null) {
                    selectReciter(recList.first())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun observeNetwork() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager != null) {
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            _isNetworkConnected.value = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isNetworkConnected.value = true
                }

                override fun onLost(network: Network) {
                    _isNetworkConnected.value = false
                }
            })
        }
    }

    private fun observePlayerEvents() {
        viewModelScope.launch {
            playerManager.playerEvents.collect { event ->
                when (event) {
                    is PlayerEvent.FallbackSourceSwitched -> {
                        _snackbarMessage.emit("تم الانتقال تلقائياً إلى مصدر بديل (${event.sourceIndex}/${event.totalSources}) - ${event.quality}")
                    }
                    is PlayerEvent.PlaybackError -> {
                        _snackbarMessage.emit(event.message)
                    }
                    is PlayerEvent.ToastMessage -> {
                        _snackbarMessage.emit(event.message)
                    }
                }
            }
        }
    }

    fun selectReciter(reciter: Reciter) {
        _selectedReciter.value = reciter
        val currentSurah = _currentSurahText.value?.number ?: playerState.value.currentItem?.surahNumber
        if (currentSurah != null) {
            loadTimingForSurah(currentSurah, reciter)
        }
        viewModelScope.launch {
            val editions = repository.getEditionsForReciter(reciter.id)
            _editionsForReciter.value = editions
            _selectedEdition.value = editions.firstOrNull()
        }
    }

    fun selectEdition(edition: RecitationEdition) {
        _selectedEdition.value = edition
    }

    fun playSurah(surah: Surah, reciter: Reciter? = null, edition: RecitationEdition? = null) {
        val activeReciter = reciter ?: _selectedReciter.value ?: reciters.value.firstOrNull() ?: return
        _selectedReciter.value = activeReciter
        val activeEdition = edition ?: _selectedEdition.value

        viewModelScope.launch {
            val quality = preferredQuality.value
            val editionId = activeEdition?.editionId ?: "${activeReciter.id}_default"
            val riwayah = activeEdition?.riwayah ?: activeReciter.riwayah
            val style = activeEdition?.recitationType ?: activeReciter.style

            val audioItem = repository.buildAudioItemForEdition(
                reciterId = activeReciter.id,
                reciterName = activeReciter.name,
                editionId = editionId,
                riwayah = riwayah,
                recitationType = style,
                surah = surah,
                preferredQuality = quality
            )

            playerManager.playSurah(audioItem, preferredQuality = quality)
            repository.saveLastPlayed(audioItem, 0L)
        }
    }

    fun playAudioItem(item: SurahAudioItem) {
        val reciter = reciters.value.find { it.id == item.reciterId }
        if (reciter != null) {
            _selectedReciter.value = reciter
        }
        playerManager.playSurah(item, preferredQuality = preferredQuality.value)
        viewModelScope.launch {
            repository.saveLastPlayed(item, 0L)
        }
    }

    fun playNextSurah() {
        val state = playerState.value
        // If playing from a custom playlist queue
        if (state.activePlaylistId != null && state.playlistQueue.isNotEmpty()) {
            val queue = state.playlistQueue
            val curIdx = state.currentPlaylistIndex
            val nextIdx = if (state.isShuffleEnabled) {
                if (queue.size > 1) {
                    var rnd = (0 until queue.size).random()
                    while (rnd == curIdx) {
                        rnd = (0 until queue.size).random()
                    }
                    rnd
                } else 0
            } else {
                if (curIdx < queue.size - 1) curIdx + 1 else 0
            }
            playPlaylistItem(queue[nextIdx], nextIdx)
            return
        }

        val current = state.currentItem ?: return
        val nextSurahNum = if (current.surahNumber < 114) current.surahNumber + 1 else 1
        val nextSurah = _surahs.value.find { it.number == nextSurahNum }
        val reciter = reciters.value.find { it.id == current.reciterId } ?: _selectedReciter.value
        if (nextSurah != null && reciter != null) {
            playSurah(nextSurah, reciter)
        }
    }

    fun playPreviousSurah() {
        val state = playerState.value
        // If playing from a custom playlist queue
        if (state.activePlaylistId != null && state.playlistQueue.isNotEmpty()) {
            val queue = state.playlistQueue
            val curIdx = state.currentPlaylistIndex
            val prevIdx = if (curIdx > 0) curIdx - 1 else queue.size - 1
            playPlaylistItem(queue[prevIdx], prevIdx)
            return
        }

        val current = state.currentItem ?: return
        val prevSurahNum = if (current.surahNumber > 1) current.surahNumber - 1 else 114
        val prevSurah = _surahs.value.find { it.number == prevSurahNum }
        val reciter = reciters.value.find { it.id == current.reciterId } ?: _selectedReciter.value
        if (prevSurah != null && reciter != null) {
            playSurah(prevSurah, reciter)
        }
    }

    // Sleep Timer Controls
    fun startSleepTimer(minutes: Int, fadeOut: Boolean = true, mode: SleepTimerMode = SleepTimerMode.CUSTOM) {
        playerManager.startSleepTimer(minutes, fadeOut, mode)
    }

    fun startSleepTimerEndOfSurah(fadeOut: Boolean = true) {
        playerManager.startSleepTimerEndOfSurah(fadeOut)
    }

    fun addSleepTimerMinutes(extraMinutes: Int) {
        playerManager.addSleepTimerMinutes(extraMinutes)
    }

    fun cancelSleepTimer() {
        playerManager.cancelSleepTimer()
        viewModelScope.launch {
            _snackbarMessage.emit("تم إلغاء مؤقت النوم")
        }
    }

    // Repetition & A-B Looping Controls
    fun setRepeatCount(targetCount: Int) {
        playerManager.setRepeatCount(targetCount)
    }

    fun setABLoopStart(ms: Long) {
        playerManager.setABLoopStart(ms)
    }

    fun setABLoopEnd(ms: Long) {
        playerManager.setABLoopEnd(ms)
    }

    fun toggleABLoop(active: Boolean) {
        playerManager.toggleABLoopActive(active)
    }

    fun toggleABLoopActive(active: Boolean) {
        playerManager.toggleABLoopActive(active)
    }

    fun clearABLoop() {
        playerManager.clearABLoop()
    }

    fun toggleShuffle(): Boolean {
        return playerManager.toggleShuffle()
    }

    // Custom Playlist Management
    fun getPlaylistItems(playlistId: String) = repository.getPlaylistItemsFlow(playlistId)

    fun createPlaylist(name: String, description: String = "", colorHex: String = "#10B981") {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createPlaylist(name, description, colorHex)
            _snackbarMessage.emit("تم إنشاء قائمة التشغيل بنجاح")
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            if (playerState.value.activePlaylistId == playlistId) {
                playerManager.setPlaylistQueue(null, null, emptyList(), -1, false)
            }
            repository.deletePlaylist(playlistId)
            _snackbarMessage.emit("تم حذف قائمة التشغيل")
        }
    }

    fun addSurahToPlaylist(playlistId: String, surah: Surah, reciter: Reciter) {
        viewModelScope.launch {
            repository.addSurahToPlaylist(
                playlistId = playlistId,
                surahNumber = surah.number,
                surahName = surah.name,
                reciterId = reciter.id,
                reciterName = reciter.name,
                riwayah = reciter.riwayah
            )
            _snackbarMessage.emit("تمت إضافة سورة ${surah.name} إلى القائمة")
        }
    }

    fun removePlaylistItem(itemId: String) {
        viewModelScope.launch {
            repository.removePlaylistItem(itemId)
            _snackbarMessage.emit("تمت إزالة السورة من القائمة")
        }
    }

    fun playPlaylist(playlist: Playlist, startIndex: Int = 0, shuffle: Boolean = false) {
        viewModelScope.launch {
            val items = repository.getPlaylistItems(playlist.id)
            if (items.isEmpty()) {
                _snackbarMessage.emit("قائمة التشغيل فارغة، أضف سوراً إليها أولاً")
                return@launch
            }
            val initialIndex = if (shuffle) (0 until items.size).random() else startIndex.coerceIn(0, items.size - 1)
            playerManager.setPlaylistQueue(
                playlistId = playlist.id,
                playlistName = playlist.name,
                queue = items,
                startIndex = initialIndex,
                isShuffle = shuffle
            )
            playPlaylistItem(items[initialIndex], initialIndex)
        }
    }

    private fun playPlaylistItem(item: PlaylistItem, index: Int) {
        viewModelScope.launch {
            val surah = _surahs.value.find { it.number == item.surahNumber } ?: Surah(
                number = item.surahNumber,
                name = item.surahName,
                englishName = "",
                ayahs = 0,
                type = "مكية"
            )
            val reciter = reciters.value.find { it.id == item.reciterId } ?: Reciter(
                id = item.reciterId,
                name = item.reciterName,
                riwayah = item.riwayah,
                style = "مرتل",
                location = "",
                serverTemplates = emptyList()
            )
            _selectedReciter.value = reciter

            val quality = preferredQuality.value
            val editionId = "${reciter.id}_default"
            val audioItem = repository.buildAudioItemForEdition(
                reciterId = reciter.id,
                reciterName = reciter.name,
                editionId = editionId,
                riwayah = item.riwayah,
                recitationType = reciter.style,
                surah = surah,
                preferredQuality = quality
            )
            playerManager.playSurah(audioItem, preferredQuality = quality)
            playerManager.setPlaylistQueue(
                playlistId = playerState.value.activePlaylistId,
                playlistName = playerState.value.activePlaylistName,
                queue = playerState.value.playlistQueue,
                startIndex = index,
                isShuffle = playerState.value.isShuffleEnabled
            )
        }
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    fun seekForward10() {
        playerManager.seekForward10s()
    }

    fun seekRewind10() {
        playerManager.seekRewind10s()
    }

    fun switchPlaybackSource(sourceIndex: Int) {
        playerManager.switchSourceManually(sourceIndex)
        viewModelScope.launch {
            _snackbarMessage.emit("تم التبديل إلى المصدر رقم ${sourceIndex + 1}")
        }
    }

    fun setPreferredQuality(quality: AudioQualityLevel) {
        viewModelScope.launch {
            repository.setPreferredQuality(quality)
            playerManager.switchQuality(quality)
            _snackbarMessage.emit("تم تغيير جودة الصوت إلى: ${quality.labelArabic}")
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        playerManager.setPlaybackSpeed(speed)
        viewModelScope.launch {
            repository.setPlaybackSpeed(speed)
        }
    }

    fun setRepeatMode(mode: PlayerRepeatMode) {
        playerManager.setRepeatMode(mode)
    }

    fun setAutoPlayNext(enabled: Boolean) {
        playerManager.setAutoPlayNext(enabled)
        viewModelScope.launch {
            repository.setAutoPlayNext(enabled)
        }
    }

    fun setWifiOnlyDownload(enabled: Boolean) {
        viewModelScope.launch {
            repository.setWifiOnlyDownload(enabled)
            if (enabled) {
                _snackbarMessage.emit("تم تفعيل التنزيل عبر Wi-Fi فقط")
            } else {
                _snackbarMessage.emit("تم السماح بالتنزيل عبر شبكة الهاتف والـ Wi-Fi")
            }
        }
    }

    fun setSearchReciterQuery(query: String) {
        _searchReciterQuery.value = query
    }

    fun setSearchSurahQuery(query: String) {
        _searchSurahQuery.value = query
    }

    fun setRiwayahFilter(filter: String) {
        _selectedRiwayahFilter.value = filter
    }

    fun toggleFavoriteReciter(reciterId: String) {
        viewModelScope.launch {
            repository.toggleFavoriteReciter(reciterId)
        }
    }

    fun toggleFavoriteSurah(surahNumber: Int) {
        viewModelScope.launch {
            repository.toggleFavoriteSurah(surahNumber)
        }
    }

    fun setSurahsViewMode(mode: String) {
        viewModelScope.launch {
            repository.setSurahsViewMode(mode)
        }
    }

    fun setAppTheme(theme: String) {
        viewModelScope.launch {
            repository.setAppTheme(theme)
        }
    }

    fun setDailyReminder(enabled: Boolean, time: String) {
        viewModelScope.launch {
            repository.setDailyReminder(enabled, time)
            if (enabled) {
                _snackbarMessage.emit("تم تفعيل التذكير اليومي في تمام الساعة $time")
            } else {
                _snackbarMessage.emit("تم إيقاف التذكير اليومي")
            }
        }
    }

    fun playRandomRecitation() {
        val allReciters = reciters.value
        val allSurahs = surahs.value
        if (allReciters.isNotEmpty() && allSurahs.isNotEmpty()) {
            val randomReciter = allReciters.random()
            val randomSurah = allSurahs.random()
            selectReciter(randomReciter)
            playSurah(randomSurah, randomReciter)
            viewModelScope.launch {
                _snackbarMessage.emit("تلاوة عشوائية: سورة ${randomSurah.name} بصوت ${randomReciter.name}")
            }
        }
    }

    fun playDailySuggestion() {
        val allReciters = reciters.value
        val allSurahs = surahs.value
        if (allReciters.isNotEmpty() && allSurahs.isNotEmpty()) {
            // E.g. Surah Al-Kahf (18), Al-Mulk (67), Yaseen (36), Ar-Rahman (55), Al-Baqarah (2)
            val suggestedSurahNumbers = listOf(18, 67, 36, 55, 2, 56, 112)
            val randomSurahNum = suggestedSurahNumbers.random()
            val targetSurah = allSurahs.find { it.number == randomSurahNum } ?: allSurahs.first()
            val preferredReciter = allReciters.find { it.id == "mishari_alafasy" } ?: allReciters.first()
            selectReciter(preferredReciter)
            playSurah(targetSurah, preferredReciter)
            viewModelScope.launch {
                _snackbarMessage.emit("وردك اليوم: سورة ${targetSurah.name} بصوت ${preferredReciter.name}")
            }
        }
    }

    fun setPlayerSheetExpanded(expanded: Boolean) {
        _isPlayerSheetExpanded.value = expanded
    }

    fun setQualityDialogVisible(visible: Boolean) {
        _isQualityDialogVisible.value = visible
    }

    fun setSyncDialogVisible(visible: Boolean) {
        _isSyncDialogVisible.value = visible
    }

    fun setStorageDialogVisible(visible: Boolean) {
        _isStorageDialogVisible.value = visible
    }

    fun syncReciterFromArchive(query: String) {
        viewModelScope.launch {
            _snackbarMessage.emit("بدء فحص ومزامنة أرشيف الإنترنت عن: $query")
            val res = repository.syncInternetArchiveForReciter(query)
            if (res.isSuccess) {
                _snackbarMessage.emit("تمت المزامنة بنجاح! تم تحديث التلاوات وإزالة التكرارات")
            } else {
                _snackbarMessage.emit("فشلت المزامنة: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun downloadSurah(reciter: Reciter, surah: Surah, preferredFormat: String = "flac") {
        viewModelScope.launch {
            val edition = _selectedEdition.value
            val audioItem = repository.buildAudioItemForEdition(
                reciterId = reciter.id,
                reciterName = reciter.name,
                editionId = edition?.editionId ?: "${reciter.id}_default",
                riwayah = edition?.riwayah ?: reciter.riwayah,
                recitationType = edition?.recitationType ?: reciter.style,
                surah = surah,
                preferredQuality = preferredQuality.value
            )

            val wifiOnly = wifiOnlyDownload.value
            _snackbarMessage.emit("بدء تحميل سورة ${surah.name} بصوت ${reciter.name} (${preferredFormat.uppercase()})...")

            downloadManager.downloadSurah(
                reciterId = reciter.id,
                surahNumber = surah.number,
                audioSources = audioItem.audioSources,
                preferredFormat = preferredFormat,
                wifiOnly = wifiOnly,
                onComplete = { success, msg ->
                    viewModelScope.launch {
                        if (success) {
                            _snackbarMessage.emit("اكتمل تحميل سورة ${surah.name} بنجاح للاستماع دون إنترنت")
                        } else {
                            _snackbarMessage.emit(msg ?: "فشل تحميل سورة ${surah.name} من كافة المصادر")
                        }
                    }
                }
            )
        }
    }

    fun startBulkDownloadForReciter(reciter: Reciter) {
        viewModelScope.launch {
            val allSurahs = _surahs.value
            val wifiOnly = wifiOnlyDownload.value
            _snackbarMessage.emit("بدء تنزيل المصحف كاملاً بصوت ${reciter.name}...")

            downloadManager.startBulkDownloadForReciter(
                reciter = reciter,
                surahs = allSurahs,
                wifiOnly = wifiOnly,
                getAudioSources = { surah, r ->
                    val edition = _selectedEdition.value
                    val audioItem = repository.buildAudioItemForEdition(
                        reciterId = r.id,
                        reciterName = r.name,
                        editionId = edition?.editionId ?: "${r.id}_default",
                        riwayah = edition?.riwayah ?: r.riwayah,
                        recitationType = edition?.recitationType ?: r.style,
                        surah = surah,
                        preferredQuality = preferredQuality.value
                    )
                    audioItem.audioSources
                },
                onFinished = { success, completed, failed ->
                    viewModelScope.launch {
                        if (success) {
                            _snackbarMessage.emit("اكتمل تنزيل مصحف ${reciter.name} كاملاً ($completed سورة)")
                        } else {
                            _snackbarMessage.emit("انتهى التنزيل: تم تحميل $completed سورة، وفشل $failed")
                        }
                    }
                }
            )
        }
    }

    fun pauseBulkDownload() {
        downloadManager.pauseBulkDownload()
        viewModelScope.launch {
            _snackbarMessage.emit("تم إيقاف التنزيل مؤقتاً")
        }
    }

    fun resumeBulkDownload(reciter: Reciter) {
        startBulkDownloadForReciter(reciter)
    }

    fun cancelBulkDownload() {
        downloadManager.cancelBulkDownload()
        viewModelScope.launch {
            _snackbarMessage.emit("تم إلغاء تنزيل المصحف")
        }
    }

    fun deleteReciterDownloads(reciterId: String, reciterName: String) {
        val count = downloadManager.deleteReciterDownloads(reciterId)
        viewModelScope.launch {
            _snackbarMessage.emit("تم حذف $count سورة محملة للقارئ $reciterName")
        }
    }

    fun downloadAllQuranTexts() {
        viewModelScope.launch {
            _snackbarMessage.emit("بدء حفظ نصوص المصحف الشريف كاملاً (114 سورة) للاستخدام دون اتصال...")
            downloadManager.downloadAllQuranTexts(
                fetcher = { surahNum ->
                    try {
                        val persistResult = repository.ensureSurahPersisted(surahNum)
                        persistResult is PersistenceResult.Success
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        false
                    }
                },
                onFinished = { success, count ->
                    viewModelScope.launch {
                        if (success) {
                            _snackbarMessage.emit("تم حفظ نصوص المصحف الشريف كاملاً (114 سورة) بنجاح للاستخدام دون اتصال")
                        } else {
                            _snackbarMessage.emit("تم حفظ $count من أصل 114 سورة. يمكنك إعادة المحاولة لاستكمال السور الناقصة.")
                        }
                    }
                }
            )
        }
    }

    fun playDownloadedReciter(reciterId: String) {
        val reciter = reciters.value.find { it.id == reciterId } ?: return
        val downloadedSurahNums = downloadManager.getDownloadedSurahNumbersForReciter(reciterId)
        if (downloadedSurahNums.isNotEmpty()) {
            val firstSurahNum = downloadedSurahNums.first()
            val firstSurah = surahs.value.find { it.number == firstSurahNum } ?: surahs.value.first()
            selectReciter(reciter)
            playSurah(firstSurah, reciter)
        }
    }

    fun deleteDownloadedSurah(reciterId: String, surahNumber: Int) {
        val surah = _surahs.value.find { it.number == surahNumber }
        downloadManager.deleteDownloadedSurah(reciterId, surahNumber)
        viewModelScope.launch {
            _snackbarMessage.emit("تم حذف سورة ${surah?.name ?: ""} من التحميلات المحلية")
        }
    }

    fun clearAllDownloads() {
        downloadManager.clearAllDownloads()
        viewModelScope.launch {
            _snackbarMessage.emit("تم تفريغ كافة التلاوات المحملة وحذفها من الذاكرة")
        }
    }

    fun getStorageSummary(): Triple<Int, Long, String> {
        return downloadManager.getStorageSummary()
    }

    fun getDownloadedFilesList(): List<StorageFileInfo> {
        return downloadManager.getDownloadedFilesList()
    }

    // Quran Reader Functions
    fun loadSurahText(surahNumber: Int) {
        loadSurahJob?.cancel()
        val requestId = ++currentSurahRequestId

        // Ensure old text from another surah is not rendered under new surah's identity
        if (_currentSurahText.value?.number != surahNumber) {
            _currentSurahText.value = null
        }
        _quranTextUiState.value = QuranTextUiState.Loading(surahNumber)
        _isLoadingSurahText.value = true

        loadSurahJob = viewModelScope.launch {
            try {
                val text = repository.getSurahText(surahNumber)
                if (requestId == currentSurahRequestId && text.number == surahNumber) {
                    _currentSurahText.value = text
                    _quranTextUiState.value = QuranTextUiState.Success(
                        surahNumber = surahNumber,
                        surahText = text,
                        isLocal = repository.isSurahDownloaded(surahNumber)
                    )
                    loadTimingForSurah(surahNumber, _selectedReciter.value)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                if (requestId == currentSurahRequestId) {
                    _currentSurahText.value = null
                    val msg = "تعذر تحميل نص سورة ${com.example.data.provider.QuranManifest.getSurahNameArabic(surahNumber)} دون اتصال بالإنترنت"
                    _quranTextUiState.value = QuranTextUiState.Unavailable(surahNumber, msg)
                    _snackbarMessage.emit(msg)
                }
            } finally {
                if (requestId == currentSurahRequestId) {
                    _isLoadingSurahText.value = false
                }
            }
        }
    }

    fun saveReadingProgress(surahNumber: Int, surahName: String, ayahNumber: Int, page: Int, juz: Int) {
        viewModelScope.launch {
            repository.saveReadingProgress(
                ReadingProgress(
                    surahNumber = surahNumber,
                    surahName = surahName,
                    ayahNumber = ayahNumber,
                    pageNumber = page,
                    juzNumber = juz,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun setReaderFontScale(scale: Float) {
        viewModelScope.launch {
            repository.setReaderFontScale(scale.coerceIn(0.7f, 2.0f))
        }
    }

    fun setReaderTheme(theme: ReaderTheme) {
        viewModelScope.launch {
            repository.setReaderTheme(theme)
        }
    }

    fun addBookmark(surahNumber: Int, surahName: String, ayahNumber: Int, pageNumber: Int, juzNumber: Int, ayahText: String, note: String = "") {
        viewModelScope.launch {
            repository.addBookmark(
                QuranBookmark(
                    surahNumber = surahNumber,
                    surahName = surahName,
                    ayahNumber = ayahNumber,
                    pageNumber = pageNumber,
                    juzNumber = juzNumber,
                    ayahText = ayahText,
                    note = note
                )
            )
            _snackbarMessage.emit("تم حفظ الآية $ayahNumber من سورة $surahName في الفواصل المرجعية")
        }
    }

    fun deleteBookmark(id: String) {
        viewModelScope.launch {
            repository.deleteBookmark(id)
            _snackbarMessage.emit("تمت إزالة الفاصلة المرجعية")
        }
    }

    fun setSelectedAyahForTafsir(ayah: Ayah?) {
        _selectedAyahForTafsir.value = ayah
    }

    fun loadAvailableTafseers() {
        viewModelScope.launch {
            val list = tafseerManager.getAvailableTafseers()
            _availableTafseers.value = list
        }
    }

    fun selectTafseer(tafseerId: Int) {
        _selectedTafseerId.value = tafseerId
        tafseerManager.setSelectedTafseerId(tafseerId)
        val selectedItem = _availableTafseers.value.find { it.id == tafseerId }
        val currentAyah = _selectedAyahForTafsir.value
        val currentSurah = _currentSurahText.value?.number ?: 1
        if (currentAyah != null) {
            loadTafseerForAyah(currentAyah, currentSurah, tafseerId)
        } else {
            _tafseerUiState.value = _tafseerUiState.value.copy(
                tafseerId = tafseerId,
                tafseerName = selectedItem?.name ?: "التفسير الميسر"
            )
        }
    }

    fun loadTafseerForAyah(ayah: Ayah, surahNumber: Int, overrideTafseerId: Int? = null) {
        val tafseerId = overrideTafseerId ?: _selectedTafseerId.value
        val tafseerName = _availableTafseers.value.find { it.id == tafseerId }?.name ?: "التفسير الميسر"
        _selectedAyahForTafsir.value = ayah

        viewModelScope.launch {
            _tafseerUiState.value = TafseerUiState(
                isLoading = true,
                tafseerId = tafseerId,
                tafseerName = tafseerName,
                surahNumber = surahNumber,
                ayahNumber = ayah.numberInSurah
            )

            val result = tafseerManager.getAyahTafseer(
                tafseerId = tafseerId,
                surahNumber = surahNumber,
                ayahNumber = ayah.numberInSurah,
                cleanAyahText = ayah.text
            )

            result.onSuccess { response ->
                _tafseerUiState.value = TafseerUiState(
                    isLoading = false,
                    tafseerText = response.text,
                    tafseerName = response.tafseerName.ifBlank { tafseerName },
                    tafseerId = tafseerId,
                    surahNumber = surahNumber,
                    ayahNumber = ayah.numberInSurah,
                    errorMessage = null
                )
            }.onFailure { err ->
                _tafseerUiState.value = TafseerUiState(
                    isLoading = false,
                    tafseerText = null,
                    tafseerName = tafseerName,
                    tafseerId = tafseerId,
                    surahNumber = surahNumber,
                    ayahNumber = ayah.numberInSurah,
                    errorMessage = err.message ?: "تعذر جلب التفسير"
                )
            }
        }
    }

    fun downloadTafseerForSurah(tafseerId: Int, surahNumber: Int) {
        tafseerDownloadJob?.cancel()
        tafseerDownloadJob = viewModelScope.launch {
            val bookName = com.example.data.provider.EmbeddedTafseerRepository.getTafseerBookName(tafseerId)
            val surahName = com.example.data.provider.QuranManifest.getSurahNameArabic(surahNumber)
            _snackbarMessage.emit("بدء حفظ تفسير ($bookName) لسورة $surahName للاستخدام دون اتصال...")
            val result = tafseerManager.downloadTafseerForSurah(
                tafseerId = tafseerId,
                surahNumber = surahNumber,
                onProgress = { progress ->
                    _tafseerDownloadProgress.value = progress
                }
            )
            result.onSuccess { count ->
                _snackbarMessage.emit("تم حفظ تفسير ($bookName) لسورة $surahName كاملاً ($count آية) دون اتصال")
            }.onFailure { err ->
                if (err !is kotlinx.coroutines.CancellationException) {
                    _snackbarMessage.emit(err.message ?: "فشل استكمال حفظ التفسير")
                }
            }
        }
    }

    fun cancelTafseerDownload() {
        tafseerDownloadJob?.cancel()
        tafseerDownloadJob = null
        tafseerManager.cancelTafseerDownload()
        _tafseerDownloadProgress.value = null
        viewModelScope.launch {
            _snackbarMessage.emit("تم إلغاء تنزيل التفسير")
        }
    }

    fun isTafseerPersisted(tafseerId: Int, surahNumber: Int, ayahNumber: Int): Boolean {
        return tafseerManager.isTafseerAyahPersisted(tafseerId, surahNumber, ayahNumber)
    }

    fun getPersistedTafseerAyahsCount(tafseerId: Int, surahNumber: Int): Int {
        return tafseerManager.getPersistedTafseerAyahsCount(tafseerId, surahNumber)
    }

    fun loadTimingForSurah(surahNumber: Int, reciter: Reciter? = null, durationMs: Long = 0) {
        viewModelScope.launch {
            val targetReciter = reciter ?: _selectedReciter.value
            val rId = targetReciter?.id ?: ""
            val rName = targetReciter?.name ?: ""
            val sText = _currentSurahText.value
            val dMs = if (durationMs > 0) durationMs else playerManager.playerState.value.durationMs

            val timing = if (rId.isNotBlank() || rName.isNotBlank()) {
                timingManager.getTimingForSurah(
                    surahNumber = surahNumber,
                    reciterId = rId,
                    reciterName = rName,
                    surahText = sText,
                    audioDurationMs = dMs
                )
            } else null
            _currentSurahTiming.value = timing
        }
    }

    fun openMeetingAndWatchSheet(tab: Int = 0) {
        _isMeetingAndWatchInitialTab.value = tab
        _isMeetingAndWatchSheetVisible.value = true
    }

    fun closeMeetingAndWatchSheet() {
        _isMeetingAndWatchSheetVisible.value = false
    }

    fun toggleMeetingMode() {
        meetingModeManager.toggleManualMeetingMode()
    }

    // --- Haram Live Streams & Notifications ---

    fun openHaramSettingsSheet() {
        _isHaramSettingsSheetVisible.value = true
    }

    fun closeHaramSettingsSheet() {
        _isHaramSettingsSheetVisible.value = false
    }

    fun playHaramLiveStream(location: HaramLocation = HaramLocation.MAKKAH) {
        haramLiveStreamManager.playHaramLiveStream(playerManager, location)
        viewModelScope.launch {
            _snackbarMessage.emit("جاري تشغيل البث المباشر: ${location.arabicName} ${location.emoji}")
        }
    }

    fun updateHaramPreferences(
        isEnabled: Boolean? = null,
        targetSource: HaramTargetSource? = null,
        notificationMode: HaramNotificationMode? = null,
        preferredSurahs: Set<Int>? = null,
        maxDailyNotifications: Int? = null
    ) {
        haramNotificationManager.updatePreferences(
            isEnabled = isEnabled,
            targetSource = targetSource,
            notificationMode = notificationMode,
            preferredSurahs = preferredSurahs,
            maxDailyNotifications = maxDailyNotifications
        )
    }

    fun sendHaramTestNotification(location: HaramLocation = HaramLocation.MAKKAH) {
        haramLiveStreamManager.sendTestNotification(location)
    }

    fun toggleHaramNotifications(enable: Boolean) {
        haramNotificationManager.updatePreferences(isEnabled = enable)
        viewModelScope.launch {
            if (enable) {
                _snackbarMessage.emit("تم تفعيل إشعارات (الآن يُقرأ في الحرم) 🕋 بنجاح")
            } else {
                _snackbarMessage.emit("تم إيقاف إشعارات الحرم")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        haramLiveStreamManager.stopMetadataPolling()
        playerManager.release()
    }
}
