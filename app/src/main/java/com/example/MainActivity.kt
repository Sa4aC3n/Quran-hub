package com.example

import android.app.Activity
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.haram.HaramLocation
import com.example.haram.HaramNotificationManager
import com.example.ui.components.AudioQualitySelector
import com.example.ui.components.FullAudioPlayerSheet
import com.example.ui.components.IslamicGeometricBackground
import com.example.ui.components.MiniAudioPlayer
import com.example.ui.components.OfflineStatusBanner
import com.example.ui.components.haram.HaramNotificationSettingsSheet
import com.example.ui.components.watch.SmartwatchMeetingModeSheet
import com.example.ui.components.StorageManagementDialog
import com.example.ui.components.SyncAdminDialog
import com.example.ui.components.update.AppUpdateDialog
import com.example.update.AppUpdateHelper
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Schedule
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.QuranReaderScreen
import com.example.ui.screens.ReciterDetailScreen
import com.example.ui.screens.RecitersScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SurahsScreen
import com.example.ui.screens.prayer.PrayerSubScreen
import com.example.ui.screens.prayer.PrayerTabScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.QuranViewModel
import com.example.i18n.Language
import com.example.i18n.LocaleManager
import com.example.i18n.LocalAppLanguage
import com.example.i18n.LocalAppStrings
import com.example.ui.components.LanguageSelectionModal

sealed class Screen {
    object Home : Screen()
    object Qibla : Screen()
    object Reciters : Screen()
    object Surahs : Screen()
    data class QuranReader(val surahNumber: Int = 1) : Screen()
    object Prayer : Screen()
    object Favorites : Screen()
    object Settings : Screen()
    object Search : Screen()
    object ReciterDetail : Screen()
    object Downloads : Screen()
}

class MainActivity : ComponentActivity() {

    private var quranViewModel: QuranViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleManager.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            val viewModel: QuranViewModel = viewModel()
            quranViewModel = viewModel
            val appTheme by viewModel.appTheme.collectAsState()

            LaunchedEffect(intent) {
                handleHaramIntent(intent, viewModel)
            }

            val isDark = when (appTheme) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDark) {
                QuranAppRoot(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        quranViewModel?.let { handleHaramIntent(intent, it) }
    }

    private fun handleHaramIntent(intent: Intent?, viewModel: QuranViewModel) {
        if (intent == null) return
        val playHaram = intent.getBooleanExtra(HaramNotificationManager.EXTRA_PLAY_HARAM, false)
        if (playHaram) {
            val haramId = intent.getStringExtra(HaramNotificationManager.EXTRA_HARAM_ID)
            val location = HaramLocation.fromId(haramId)
            viewModel.playHaramLiveStream(location)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranAppRoot(
    viewModel: QuranViewModel = viewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Request notification permission for playback notification on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Collect UI States
    val reciters by viewModel.reciters.collectAsState()
    val filteredReciters by viewModel.filteredReciters.collectAsState()
    val surahs by viewModel.surahs.collectAsState()
    val filteredSurahs by viewModel.filteredSurahs.collectAsState()
    val selectedReciter by viewModel.selectedReciter.collectAsState()
    val selectedEdition by viewModel.selectedEdition.collectAsState()
    val editionsForReciter by viewModel.editionsForReciter.collectAsState()
    val searchReciterQuery by viewModel.searchReciterQuery.collectAsState()
    val searchSurahQuery by viewModel.searchSurahQuery.collectAsState()
    val selectedFilter by viewModel.selectedRiwayahFilter.collectAsState()
    val favoriteReciters by viewModel.favoriteReciters.collectAsState()
    val favoriteSurahs by viewModel.favoriteSurahs.collectAsState()
    val surahsViewMode by viewModel.surahsViewMode.collectAsState()
    val appTheme by viewModel.appTheme.collectAsState()
    val dailyReminderEnabled by viewModel.dailyReminderEnabled.collectAsState()
    val dailyReminderTime by viewModel.dailyReminderTime.collectAsState()
    val lastPlayedItem by viewModel.lastPlayedItem.collectAsState()
    val lastPlayedPositionMs by viewModel.lastPlayedPositionMs.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val downloadStates by viewModel.downloadStates.collectAsState()
    val isNetworkConnected by viewModel.isNetworkConnected.collectAsState()
    val isPlayerSheetExpanded by viewModel.isPlayerSheetExpanded.collectAsState()
    val preferredQuality by viewModel.preferredQuality.collectAsState()
    val wifiOnlyDownload by viewModel.wifiOnlyDownload.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val reviewItems by viewModel.reviewItems.collectAsState()
    val isQualityDialogVisible by viewModel.isQualityDialogVisible.collectAsState()
    val isSyncDialogVisible by viewModel.isSyncDialogVisible.collectAsState()
    val isStorageDialogVisible by viewModel.isStorageDialogVisible.collectAsState()

    // Offline Bulk Downloads & Storage states
    val bulkDownloadProgress by viewModel.bulkDownloadProgress.collectAsState()
    val offlineTextDownloadState by viewModel.offlineTextDownloadState.collectAsState()
    val recitersStorageBreakdown by viewModel.recitersStorageBreakdown.collectAsState()
    val storageSummary by viewModel.storageSummary.collectAsState()
    val downloadedFiles by viewModel.downloadedFiles.collectAsState()

    // Quran Reader states
    val currentSurahText by viewModel.currentSurahText.collectAsState()
    val isLoadingSurahText by viewModel.isLoadingSurahText.collectAsState()
    val readerFontScale by viewModel.readerFontScale.collectAsState()
    val readerTheme by viewModel.readerTheme.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val readingProgress by viewModel.readingProgress.collectAsState()
    val availableTafseers by viewModel.availableTafseers.collectAsState()
    val selectedTafseerId by viewModel.selectedTafseerId.collectAsState()
    val tafseerUiState by viewModel.tafseerUiState.collectAsState()
    val tafseerDownloadProgress by viewModel.tafseerDownloadProgress.collectAsState()
    val activeAyahNumber by viewModel.activeAyahNumber.collectAsState()
    val activeWordIndex by viewModel.activeWordIndex.collectAsState()

    val isMeetingAndWatchSheetVisible by viewModel.isMeetingAndWatchSheetVisible.collectAsState()
    val isMeetingAndWatchInitialTab by viewModel.isMeetingAndWatchInitialTab.collectAsState()
    val meetingState by viewModel.meetingModeManager.meetingState.collectAsState()

    // Haram Live Stream & Notification states
    val makkahNowPlaying by viewModel.makkahNowPlaying.collectAsState()
    val madinahNowPlaying by viewModel.madinahNowPlaying.collectAsState()
    val haramPreferences by viewModel.haramPreferences.collectAsState()
    val isHaramSettingsSheetVisible by viewModel.isHaramSettingsSheetVisible.collectAsState()

    // Language & Localization state
    val currentLanguage by LocaleManager.currentLanguage.collectAsState()
    val appStrings by LocaleManager.strings.collectAsState()
    var isLanguageModalOpen by remember { mutableStateOf(false) }

    // In-App Update Helper
    val appUpdateHelper = remember { AppUpdateHelper(context) }
    val appUpdateState by appUpdateHelper.updateState.collectAsState()
    val appVersionInfo = remember { appUpdateHelper.getCurrentVersionInfo() }

    DisposableEffect(Unit) {
        onDispose {
            appUpdateHelper.onDestroy()
        }
    }

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var globalSearchQuery by remember { mutableStateOf("") }

    // Back handling
    BackHandler(enabled = currentScreen !is Screen.Home) {
        currentScreen = when (currentScreen) {
            is Screen.ReciterDetail -> Screen.Reciters
            is Screen.Search -> Screen.Home
            is Screen.QuranReader -> Screen.Home
            is Screen.Downloads -> Screen.Home
            else -> Screen.Home
        }
    }

    // Listen to snackbar messages
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Dynamic RTL/LTR according to chosen language
    val layoutDirection = if (currentLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalAppLanguage provides currentLanguage,
        LocalAppStrings provides appStrings
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(bottom = if (playerState.currentItem != null) 70.dp else 0.dp)
                    )
                },
                bottomBar = {
                    Column(
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        // Mini Audio Player at Bottom
                        MiniAudioPlayer(
                            playerState = playerState,
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onNext = { viewModel.playNextSurah() },
                            onPrevious = { viewModel.playPreviousSurah() },
                            onClick = { viewModel.setPlayerSheetExpanded(true) }
                        )

                        // Bottom Navigation Bar (Visible on main tabs)
                        val isMainTab = currentScreen is Screen.Home ||
                                currentScreen is Screen.Reciters ||
                                currentScreen is Screen.Surahs ||
                                currentScreen is Screen.QuranReader ||
                                currentScreen is Screen.Prayer ||
                                currentScreen is Screen.Settings

                        if (isMainTab) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 3.dp,
                                modifier = Modifier.testTag("main_bottom_nav_bar")
                            ) {
                                NavigationBarItem(
                                    selected = currentScreen is Screen.Home,
                                    onClick = { currentScreen = Screen.Home },
                                    icon = { Icon(Icons.Default.Home, contentDescription = LocaleManager.t("nav_home", "الرئيسية")) },
                                    label = { Text(LocaleManager.t("nav_home", "الرئيسية"), fontSize = 11.sp, fontWeight = if (currentScreen is Screen.Home) FontWeight.Bold else FontWeight.Normal) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )

                                NavigationBarItem(
                                    selected = currentScreen is Screen.Reciters,
                                    onClick = { currentScreen = Screen.Reciters },
                                    icon = { Icon(Icons.Default.Person, contentDescription = LocaleManager.t("nav_reciters", "القراء")) },
                                    label = { Text(LocaleManager.t("nav_reciters", "القراء"), fontSize = 11.sp, fontWeight = if (currentScreen is Screen.Reciters) FontWeight.Bold else FontWeight.Normal) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )

                                NavigationBarItem(
                                    selected = currentScreen is Screen.Surahs,
                                    onClick = { currentScreen = Screen.Surahs },
                                    icon = { Icon(Icons.Default.MenuBook, contentDescription = LocaleManager.t("nav_surahs", "السور")) },
                                    label = { Text(LocaleManager.t("nav_surahs", "السور"), fontSize = 11.sp, fontWeight = if (currentScreen is Screen.Surahs) FontWeight.Bold else FontWeight.Normal) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )

                                NavigationBarItem(
                                    selected = currentScreen is Screen.QuranReader,
                                    onClick = {
                                        val targetSurah = readingProgress.surahNumber.coerceIn(1, 114)
                                        viewModel.loadSurahText(targetSurah)
                                        currentScreen = Screen.QuranReader(targetSurah)
                                    },
                                    icon = { Icon(Icons.Default.AutoStories, contentDescription = LocaleManager.t("nav_mushaf", "المصحف")) },
                                    label = { Text(LocaleManager.t("nav_mushaf", "المصحف"), fontSize = 11.sp, fontWeight = if (currentScreen is Screen.QuranReader) FontWeight.Bold else FontWeight.Normal) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )

                                NavigationBarItem(
                                    selected = currentScreen is Screen.Prayer,
                                    onClick = { currentScreen = Screen.Prayer },
                                    icon = { Icon(Icons.Default.Schedule, contentDescription = LocaleManager.t("nav_prayer", "الصلاة")) },
                                    label = { Text(LocaleManager.t("nav_prayer", "الصلاة"), fontSize = 11.sp, fontWeight = if (currentScreen is Screen.Prayer) FontWeight.Bold else FontWeight.Normal) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )

                                NavigationBarItem(
                                    selected = currentScreen is Screen.Settings,
                                    onClick = { currentScreen = Screen.Settings },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = LocaleManager.t("nav_settings", "الإعدادات")) },
                                    label = { Text(LocaleManager.t("nav_settings", "الإعدادات"), fontSize = 11.sp, fontWeight = if (currentScreen is Screen.Settings) FontWeight.Bold else FontWeight.Normal) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    IslamicGeometricBackground(modifier = Modifier.fillMaxSize())

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Offline notice banner
                        OfflineStatusBanner(
                            isConnected = isNetworkConnected,
                            onOpenDownloads = { currentScreen = Screen.Downloads }
                        )

                        // Navigation content
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                if (targetState is Screen.ReciterDetail || targetState is Screen.Search || targetState is Screen.Downloads) {
                                    slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                    slideOutHorizontally { width -> width } + fadeOut()
                                } else {
                                    fadeIn() togetherWith fadeOut()
                                }
                            },
                            label = "screen_navigation"
                        ) { screen ->
                            when (screen) {
                                is Screen.Home -> {
                                    HomeScreen(
                                        reciters = reciters,
                                        surahs = surahs,
                                        favoriteReciterIds = favoriteReciters,
                                        favoriteSurahNumbers = favoriteSurahs,
                                        lastPlayedItem = lastPlayedItem,
                                        lastPlayedPositionMs = lastPlayedPositionMs,
                                        playerState = playerState,
                                        preferredQuality = preferredQuality,
                                        makkahNowPlaying = makkahNowPlaying,
                                        madinahNowPlaying = madinahNowPlaying,
                                        isHaramNotificationsEnabled = haramPreferences.isEnabled,
                                        onPlayLiveHaram = { location -> viewModel.playHaramLiveStream(location) },
                                        onOpenHaramSettings = { viewModel.openHaramSettingsSheet() },
                                        onResumeLastPlayed = { item ->
                                            viewModel.playAudioItem(item)
                                        },
                                        onTogglePlayPause = { viewModel.togglePlayPause() },
                                        onReciterClick = { reciter ->
                                            viewModel.selectReciter(reciter)
                                            viewModel.setSearchSurahQuery("")
                                            currentScreen = Screen.ReciterDetail
                                        },
                                        onQuickPlayReciter = { reciter ->
                                            viewModel.selectReciter(reciter)
                                            val firstSurah = surahs.firstOrNull()
                                            if (firstSurah != null) {
                                                viewModel.playSurah(firstSurah, reciter)
                                            }
                                        },
                                        onSurahClick = { surah ->
                                            val defaultReciter = selectedReciter ?: reciters.firstOrNull()
                                            if (defaultReciter != null) {
                                                viewModel.selectReciter(defaultReciter)
                                                viewModel.playSurah(surah, defaultReciter)
                                            }
                                        },
                                        onNavigateToReciters = { currentScreen = Screen.Reciters },
                                        onNavigateToSurahs = { currentScreen = Screen.Surahs },
                                        onNavigateToFavorites = { currentScreen = Screen.Favorites },
                                        onNavigateToSearch = { currentScreen = Screen.Search },
                                        onNavigateToSettings = { currentScreen = Screen.Settings },
                                        onOpenStorageDialog = { currentScreen = Screen.Downloads },
                                        onPlayRandomRecitation = { viewModel.playRandomRecitation() },
                                        onPlayDailySuggestion = { viewModel.playDailySuggestion() },
                                        onNavigateToReader = { surahNum ->
                                            viewModel.loadSurahText(surahNum)
                                            currentScreen = Screen.QuranReader(surahNum)
                                        },
                                        currentLanguage = currentLanguage,
                                        onOpenLanguageModal = { isLanguageModalOpen = true },
                                        onShareApp = {
                                            val playStoreUrl = "https://play.google.com/store/apps/details?id=com.aistudio.quranaudio.mskdra"
                                            val shareMessage = "استمع للقرآن الكريم بأصوات كبار القراء وبأعلى جودة صوتية بدون إعلانات.\nحمّل تطبيق القرآن الكريم الآن:\n$playStoreUrl"
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, shareMessage)
                                                type = "text/plain"
                                            }
                                            val shareIntent = Intent.createChooser(sendIntent, "مشاركة تطبيق القرآن الكريم")
                                            context.startActivity(shareIntent)
                                        },
                                        onRateApp = {
                                            val appId = "com.aistudio.quranaudio.mskdra"
                                            try {
                                                val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appId")).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                                                }
                                                context.startActivity(marketIntent)
                                            } catch (e: ActivityNotFoundException) {
                                                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appId"))
                                                context.startActivity(webIntent)
                                            }
                                        }
                                    )
                                }

                                is Screen.QuranReader -> {
                                    QuranReaderScreen(
                                        currentSurahNumber = screen.surahNumber,
                                        allSurahs = surahs,
                                        surahText = currentSurahText,
                                        isLoadingText = isLoadingSurahText,
                                        readerFontScale = readerFontScale,
                                        readerTheme = readerTheme,
                                        bookmarks = bookmarks,
                                        playerState = playerState,
                                        activeReciter = selectedReciter ?: reciters.firstOrNull(),
                                        availableTafseers = availableTafseers,
                                        selectedTafseerId = selectedTafseerId,
                                        tafseerUiState = tafseerUiState,
                                        activeAyahNumber = activeAyahNumber,
                                        activeWordIndex = activeWordIndex,
                                        onSelectSurah = { sNum ->
                                            viewModel.loadSurahText(sNum)
                                            currentScreen = Screen.QuranReader(sNum)
                                        },
                                        onFontScaleChange = { viewModel.setReaderFontScale(it) },
                                        onThemeChange = { viewModel.setReaderTheme(it) },
                                        onSelectTafseer = { tafseerId ->
                                            viewModel.selectTafseer(tafseerId)
                                        },
                                        onLoadTafseer = { ayah, surahNum, tafseerId ->
                                            viewModel.loadTafseerForAyah(ayah, surahNum, tafseerId)
                                        },
                                        onDownloadSurahTafseer = { tafseerId, surahNum ->
                                            viewModel.downloadTafseerForSurah(tafseerId, surahNum)
                                        },
                                        onAddBookmark = { sNum, sName, aNum, page, juz, text, note ->
                                            viewModel.addBookmark(sNum, sName, aNum, page, juz, text, note)
                                        },
                                        onDeleteBookmark = { bookmarkId ->
                                            viewModel.deleteBookmark(bookmarkId)
                                        },
                                        onPlaySurahAudio = { surah, reciter ->
                                            val rec = reciter ?: selectedReciter ?: reciters.firstOrNull()
                                            if (rec != null) {
                                                viewModel.playSurah(surah, rec)
                                            }
                                        },
                                        onTogglePlayPause = { viewModel.togglePlayPause() },
                                        onSaveProgress = { sNum, sName, aNum, page, juz ->
                                            viewModel.saveReadingProgress(sNum, sName, aNum, page, juz)
                                        },
                                        onBack = { currentScreen = Screen.Home }
                                    )
                                }

                                is Screen.Reciters -> {
                                    RecitersScreen(
                                        reciters = filteredReciters,
                                        searchQuery = searchReciterQuery,
                                        onSearchQueryChange = { viewModel.setSearchReciterQuery(it) },
                                        selectedFilter = selectedFilter,
                                        onFilterSelect = { viewModel.setRiwayahFilter(it) },
                                        favoriteReciterIds = favoriteReciters,
                                        onToggleFavorite = { viewModel.toggleFavoriteReciter(it) },
                                        playerState = playerState,
                                        onReciterClick = { reciter ->
                                            viewModel.selectReciter(reciter)
                                            viewModel.setSearchSurahQuery("")
                                            currentScreen = Screen.ReciterDetail
                                        },
                                        onQuickPlayReciter = { reciter ->
                                            viewModel.selectReciter(reciter)
                                            val firstSurah = surahs.firstOrNull()
                                            if (firstSurah != null) {
                                                viewModel.playSurah(firstSurah, reciter)
                                            }
                                        }
                                    )
                                }

                                is Screen.Surahs -> {
                                    SurahsScreen(
                                        surahs = filteredSurahs,
                                        searchQuery = searchSurahQuery,
                                        onSearchQueryChange = { viewModel.setSearchSurahQuery(it) },
                                        viewMode = surahsViewMode,
                                        onViewModeChange = { viewModel.setSurahsViewMode(it) },
                                        favoriteSurahNumbers = favoriteSurahs,
                                        onToggleFavoriteSurah = { viewModel.toggleFavoriteSurah(it) },
                                        playerState = playerState,
                                        allReciters = reciters,
                                        selectedReciter = selectedReciter,
                                        onSelectReciter = { reciter ->
                                            viewModel.selectReciter(reciter)
                                        },
                                        onSurahClick = { surah ->
                                            val activeReciter = selectedReciter ?: reciters.firstOrNull()
                                            if (activeReciter != null) {
                                                viewModel.playSurah(surah, activeReciter)
                                            }
                                        },
                                        onReadSurah = { surah ->
                                            viewModel.loadSurahText(surah.number)
                                            currentScreen = Screen.QuranReader(surah.number)
                                        }
                                    )
                                }

                                is Screen.Qibla -> {
                                    PrayerTabScreen(
                                        prayerManager = viewModel.prayerManager,
                                        initialSubScreen = PrayerSubScreen.QiblaCompassDetail
                                    )
                                }

                                is Screen.Prayer -> {
                                    PrayerTabScreen(
                                        prayerManager = viewModel.prayerManager
                                    )
                                }

                                is Screen.Favorites -> {
                                    val favRecitersList = reciters.filter { favoriteReciters.contains(it.id) }
                                    val favSurahsList = surahs.filter { favoriteSurahs.contains(it.number) }

                                    FavoritesScreen(
                                        favoriteReciters = favRecitersList,
                                        favoriteSurahs = favSurahsList,
                                        favoriteReciterIds = favoriteReciters,
                                        favoriteSurahNumbers = favoriteSurahs,
                                        playlists = playlists,
                                        playerState = playerState,
                                        getPlaylistItems = { plId -> viewModel.getPlaylistItems(plId) },
                                        onCreatePlaylist = { name, desc, color -> viewModel.createPlaylist(name, desc, color) },
                                        onDeletePlaylist = { plId -> viewModel.deletePlaylist(plId) },
                                        onPlayPlaylist = { playlist, startIdx, shuffle -> viewModel.playPlaylist(playlist, startIdx, shuffle) },
                                        onRemovePlaylistItem = { itemId -> viewModel.removePlaylistItem(itemId) },
                                        onReciterClick = { reciter ->
                                            viewModel.selectReciter(reciter)
                                            viewModel.setSearchSurahQuery("")
                                            currentScreen = Screen.ReciterDetail
                                        },
                                        onQuickPlayReciter = { reciter ->
                                            viewModel.selectReciter(reciter)
                                            val firstSurah = surahs.firstOrNull()
                                            if (firstSurah != null) {
                                                viewModel.playSurah(firstSurah, reciter)
                                            }
                                        },
                                        onToggleFavoriteReciter = { viewModel.toggleFavoriteReciter(it) },
                                        onSurahClick = { surah ->
                                            val activeReciter = selectedReciter ?: reciters.firstOrNull()
                                            if (activeReciter != null) {
                                                viewModel.playSurah(surah, activeReciter)
                                            }
                                        },
                                        onToggleFavoriteSurah = { viewModel.toggleFavoriteSurah(it) },
                                        onExploreClicked = { currentScreen = Screen.Reciters }
                                    )
                                }

                                is Screen.Settings -> {
                                    SettingsScreen(
                                        currentTheme = appTheme,
                                        onThemeChange = { viewModel.setAppTheme(it) },
                                        autoPlayNext = playerState.autoPlayNext,
                                        onAutoPlayNextChange = { viewModel.setAutoPlayNext(it) },
                                        preferredQuality = preferredQuality,
                                        onOpenQualityDialog = { viewModel.setQualityDialogVisible(true) },
                                        wifiOnlyDownload = wifiOnlyDownload,
                                        onWifiOnlyDownloadChange = { viewModel.setWifiOnlyDownload(it) },
                                        dailyReminderEnabled = dailyReminderEnabled,
                                        dailyReminderTime = dailyReminderTime,
                                        onToggleDailyReminder = { viewModel.setDailyReminder(it, dailyReminderTime) },
                                        isMeetingModeActive = meetingState.isMeetingModeActive,
                                        meetingTitle = meetingState.activeMeetingTitle,
                                        onToggleMeetingMode = { viewModel.toggleMeetingMode() },
                                        onOpenMeetingModeSheet = { tab -> viewModel.openMeetingAndWatchSheet(tab) },
                                        isHaramNotificationsEnabled = haramPreferences.isEnabled,
                                        haramPreferences = haramPreferences,
                                        onOpenHaramSettings = { viewModel.openHaramSettingsSheet() },
                                        onToggleHaramNotifications = { viewModel.toggleHaramNotifications(it) },
                                        onOpenStorageDialog = { currentScreen = Screen.Downloads },
                                        onOpenSyncDialog = { viewModel.setSyncDialogVisible(true) },
                                        onShareApp = {
                                            val playStoreUrl = "https://play.google.com/store/apps/details?id=${context.packageName}"
                                            val shareMessage = "قال رسول الله ﷺ: «مَن دلَّ على خيرٍ فله مثلُ أجرِ فاعله» 🌿\n\nتطبيق القرآن الكريم: استمع لتلاوات خاشعة بأصوات كبار القراء، وتصفح المصحف الشريف والتفاسير، وأوقات الصلاة واتجاه القبلة، وبدون إعلانات.\n\nحمّل التطبيق الآن مجاناً من متجر Google Play:\n$playStoreUrl"
                                            val sendIntent: Intent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, shareMessage)
                                                type = "text/plain"
                                            }
                                            val shareIntent = Intent.createChooser(sendIntent, "مشاركة تطبيق القرآن الكريم مع الأهل والأصدقاء")
                                            context.startActivity(shareIntent)
                                        },
                                        onRateApp = {
                                            val appId = context.packageName
                                            try {
                                                val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appId")).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                                                }
                                                context.startActivity(marketIntent)
                                            } catch (e: ActivityNotFoundException) {
                                                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appId"))
                                                context.startActivity(webIntent)
                                            }
                                        },
                                        onCheckAppUpdate = {
                                            appUpdateHelper.checkForUpdate()
                                        },
                                        appVersionName = appVersionInfo.first,
                                        currentLanguage = currentLanguage,
                                        onOpenLanguageDialog = { isLanguageModalOpen = true }
                                    )
                                }

                                is Screen.Search -> {
                                    SearchScreen(
                                        query = globalSearchQuery,
                                        onQueryChange = { globalSearchQuery = it },
                                        allReciters = reciters,
                                        allSurahs = surahs,
                                        favoriteReciters = favoriteReciters,
                                        favoriteSurahs = favoriteSurahs,
                                        playerState = playerState,
                                        onReciterClick = { reciter ->
                                            viewModel.selectReciter(reciter)
                                            viewModel.setSearchSurahQuery("")
                                            currentScreen = Screen.ReciterDetail
                                        },
                                        onQuickPlayReciter = { reciter ->
                                            viewModel.selectReciter(reciter)
                                            val firstSurah = surahs.firstOrNull()
                                            if (firstSurah != null) {
                                                viewModel.playSurah(firstSurah, reciter)
                                            }
                                        },
                                        onToggleFavoriteReciter = { viewModel.toggleFavoriteReciter(it) },
                                        onSurahClick = { surah ->
                                            val activeReciter = selectedReciter ?: reciters.firstOrNull()
                                            if (activeReciter != null) {
                                                viewModel.playSurah(surah, activeReciter)
                                            }
                                        },
                                        onToggleFavoriteSurah = { viewModel.toggleFavoriteSurah(it) },
                                        onBack = { currentScreen = Screen.Home }
                                    )
                                }

                                is Screen.ReciterDetail -> {
                                    val activeReciter = selectedReciter ?: reciters.firstOrNull()
                                    if (activeReciter != null) {
                                        ReciterDetailScreen(
                                            reciter = activeReciter,
                                            editions = editionsForReciter,
                                            selectedEdition = selectedEdition,
                                            onSelectEdition = { viewModel.selectEdition(it) },
                                            surahs = surahs,
                                            searchQuery = searchSurahQuery,
                                            onSearchQueryChange = { viewModel.setSearchSurahQuery(it) },
                                            playerState = playerState,
                                            downloadStates = downloadStates,
                                            bulkDownloadProgress = bulkDownloadProgress,
                                            preferredQuality = preferredQuality,
                                            onSurahClick = { surah ->
                                                viewModel.playSurah(surah, activeReciter)
                                            },
                                            onDownloadSurah = { surah ->
                                                viewModel.downloadSurah(activeReciter, surah)
                                            },
                                            onStartBulkDownload = {
                                                viewModel.startBulkDownloadForReciter(activeReciter)
                                            },
                                            onPauseBulkDownload = {
                                                viewModel.pauseBulkDownload()
                                            },
                                            onResumeBulkDownload = {
                                                viewModel.resumeBulkDownload(activeReciter)
                                            },
                                            onCancelBulkDownload = {
                                                viewModel.cancelBulkDownload()
                                            },
                                            onDeleteReciterDownloads = {
                                                viewModel.deleteReciterDownloads(activeReciter.id, activeReciter.name)
                                            },
                                            onOpenQualityDialog = { viewModel.setQualityDialogVisible(true) },
                                            onBack = {
                                                currentScreen = Screen.Reciters
                                            }
                                        )
                                    } else {
                                        currentScreen = Screen.Reciters
                                    }
                                }

                                is Screen.Downloads -> {
                                    DownloadsScreen(
                                        storageSummary = storageSummary,
                                        downloadedFiles = downloadedFiles,
                                        recitersStorage = recitersStorageBreakdown,
                                        allReciters = reciters,
                                        allSurahs = surahs,
                                        bulkDownloadProgress = bulkDownloadProgress,
                                        offlineTextDownloadState = offlineTextDownloadState,
                                        wifiOnlyDownload = wifiOnlyDownload,
                                        playerState = playerState,
                                        onToggleWifiOnly = { viewModel.setWifiOnlyDownload(it) },
                                        onStartBulkDownload = { reciter ->
                                            viewModel.startBulkDownloadForReciter(reciter)
                                        },
                                        onPauseBulkDownload = {
                                            viewModel.pauseBulkDownload()
                                        },
                                        onResumeBulkDownload = { reciter ->
                                            viewModel.resumeBulkDownload(reciter)
                                        },
                                        onCancelBulkDownload = {
                                            viewModel.cancelBulkDownload()
                                        },
                                        onDeleteReciterDownloads = { reciterId, reciterName ->
                                            viewModel.deleteReciterDownloads(reciterId, reciterName)
                                        },
                                        onDeleteFile = { reciterId, surahNumber ->
                                            viewModel.deleteDownloadedSurah(reciterId, surahNumber)
                                        },
                                        onClearAll = {
                                            viewModel.clearAllDownloads()
                                        },
                                        onDownloadAllTexts = {
                                            viewModel.downloadAllQuranTexts()
                                        },
                                        onPlaySurah = { surah, reciter ->
                                            viewModel.playSurah(surah, reciter)
                                        },
                                        onPlayReciterOffline = { reciterId ->
                                            viewModel.playDownloadedReciter(reciterId)
                                        },
                                        onSelectReciter = { reciter ->
                                            viewModel.selectReciter(reciter)
                                            viewModel.setSearchSurahQuery("")
                                            currentScreen = Screen.ReciterDetail
                                        },
                                        onBack = {
                                            currentScreen = Screen.Home
                                        },
                                        tafseerDownloadProgress = tafseerDownloadProgress,
                                        onCancelTafseerDownload = {
                                            viewModel.cancelTafseerDownload()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Expanded Full Player Modal Sheet
            if (isPlayerSheetExpanded && playerState.currentItem != null) {
                val currentItem = playerState.currentItem!!
                val downloadKey = "${currentItem.reciterId}_${currentItem.surahNumber}"
                val currentDownloadState = downloadStates[downloadKey]

                FullAudioPlayerSheet(
                    playerState = playerState,
                    downloadState = currentDownloadState,
                    playlists = playlists,
                    activeAyahNumber = activeAyahNumber,
                    surahText = currentSurahText,
                    onDismiss = { viewModel.setPlayerSheetExpanded(false) },
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onSeekTo = { viewModel.seekTo(it) },
                    onSeekForward10 = { viewModel.seekForward10() },
                    onSeekRewind10 = { viewModel.seekRewind10() },
                    onNextSurah = { viewModel.playNextSurah() },
                    onPreviousSurah = { viewModel.playPreviousSurah() },
                    onSwitchSource = { sourceIdx -> viewModel.switchPlaybackSource(sourceIdx) },
                    onSetPlaybackSpeed = { speed -> viewModel.setPlaybackSpeed(speed) },
                    onSetRepeatMode = { mode -> viewModel.setRepeatMode(mode) },
                    onSetAutoPlayNext = { autoNext -> viewModel.setAutoPlayNext(autoNext) },
                    onDownloadSurah = {
                        val activeReciter = selectedReciter ?: reciters.find { it.id == currentItem.reciterId }
                        val activeSurah = surahs.find { it.number == currentItem.surahNumber }
                        if (activeReciter != null && activeSurah != null) {
                            viewModel.downloadSurah(activeReciter, activeSurah)
                        }
                    },
                    onOpenReader = { surahNumber ->
                        viewModel.loadSurahText(surahNumber)
                        currentScreen = Screen.QuranReader(surahNumber)
                    },
                    onStartSleepTimer = { mins, fade, mode -> viewModel.startSleepTimer(mins, fade, mode) },
                    onStartSleepTimerEndOfSurah = { fade -> viewModel.startSleepTimerEndOfSurah(fade) },
                    onAddSleepTimerMinutes = { mins -> viewModel.addSleepTimerMinutes(mins) },
                    onCancelSleepTimer = { viewModel.cancelSleepTimer() },
                    onSetRepeatCount = { count -> viewModel.setRepeatCount(count) },
                    onSetABLoopStart = { startMs -> viewModel.setABLoopStart(startMs) },
                    onSetABLoopEnd = { endMs -> viewModel.setABLoopEnd(endMs) },
                    onToggleABLoop = { active -> viewModel.toggleABLoop(active) },
                    onClearABLoop = { viewModel.clearABLoop() },
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onCreatePlaylist = { name, desc, color -> viewModel.createPlaylist(name, desc, color) },
                    onAddToPlaylist = { plId ->
                        val activeReciter = selectedReciter ?: reciters.find { it.id == currentItem.reciterId }
                        val activeSurah = surahs.find { it.number == currentItem.surahNumber }
                        if (activeReciter != null && activeSurah != null) {
                            viewModel.addSurahToPlaylist(plId, activeSurah, activeReciter)
                        }
                    },
                    isMeetingModeActive = meetingState.isMeetingModeActive,
                    onOpenMeetingModeSheet = { tab -> viewModel.openMeetingAndWatchSheet(tab) },
                    onToggleMeetingMode = { viewModel.toggleMeetingMode() }
                )
            }

            // Smartwatch & Meeting Mode Sheet
            if (isMeetingAndWatchSheetVisible) {
                SmartwatchMeetingModeSheet(
                    meetingModeManager = viewModel.meetingModeManager,
                    smartwatchBridgeManager = viewModel.smartwatchBridgeManager,
                    allReciters = reciters,
                    allSurahs = surahs,
                    onDismiss = { viewModel.closeMeetingAndWatchSheet() },
                    initialTab = isMeetingAndWatchInitialTab
                )
            }

            // Quality Selection Sheet
            if (isQualityDialogVisible) {
                ModalBottomSheet(
                    onDismissRequest = { viewModel.setQualityDialogVisible(false) },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(20.dp)
                    ) {
                        AudioQualitySelector(
                            selectedQuality = preferredQuality,
                            onQualitySelected = { quality ->
                                viewModel.setPreferredQuality(quality)
                                viewModel.setQualityDialogVisible(false)
                            }
                        )
                    }
                }
            }

            // Sync Dialog
            if (isSyncDialogVisible) {
                SyncAdminDialog(
                    syncStats = syncState,
                    reviewItems = reviewItems,
                    onDismiss = { viewModel.setSyncDialogVisible(false) },
                    onSyncQuery = { query -> viewModel.syncReciterFromArchive(query) }
                )
            }

            // Storage Management Dialog
            if (isStorageDialogVisible) {
                StorageManagementDialog(
                    storageSummary = viewModel.getStorageSummary(),
                    downloadedFiles = viewModel.getDownloadedFilesList(),
                    onDeleteFile = { reciterId, surahNum -> viewModel.deleteDownloadedSurah(reciterId, surahNum) },
                    onClearAll = { viewModel.clearAllDownloads() },
                    onDismiss = { viewModel.setStorageDialogVisible(false) }
                )
            }

            // Haram Notification Settings Bottom Sheet
            if (isHaramSettingsSheetVisible) {
                HaramNotificationSettingsSheet(
                    preferences = haramPreferences,
                    allSurahs = surahs,
                    onUpdatePreferences = { enabled, source, mode, preferred, maxDaily ->
                        viewModel.updateHaramPreferences(
                            isEnabled = enabled,
                            targetSource = source,
                            notificationMode = mode,
                            preferredSurahs = preferred,
                            maxDailyNotifications = maxDaily
                        )
                    },
                    onSendTestNotification = { location ->
                        viewModel.sendHaramTestNotification(location)
                    },
                    onDismiss = { viewModel.closeHaramSettingsSheet() }
                )
            }

            // Language Selection Modal
            if (isLanguageModalOpen) {
                LanguageSelectionModal(
                    currentLanguage = currentLanguage,
                    onLanguageSelected = { selectedLang ->
                        LocaleManager.setLanguage(context, selectedLang)
                    },
                    onDismiss = { isLanguageModalOpen = false }
                )
            }

            // In-App Update Dialog
            AppUpdateDialog(
                updateState = appUpdateState,
                onStartUpdate = {
                    (context as? Activity)?.let { activity ->
                        appUpdateHelper.startUpdate(activity)
                    } ?: appUpdateHelper.openPlayStore(context)
                },
                onCompleteUpdate = {
                    appUpdateHelper.completeUpdate()
                },
                onOpenPlayStore = {
                    appUpdateHelper.openPlayStore(context)
                },
                onDismiss = {
                    appUpdateHelper.dismissDialog()
                }
            )
        }
    }
}
