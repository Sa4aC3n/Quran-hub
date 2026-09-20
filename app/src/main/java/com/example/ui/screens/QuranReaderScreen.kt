package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyRow
import com.example.data.model.Ayah
import com.example.data.model.PlayerState
import com.example.data.model.QuranBookmark
import com.example.data.model.QuranDisplayMode
import com.example.data.model.ReaderTheme
import com.example.data.model.Reciter
import com.example.data.model.Surah
import com.example.data.model.SurahText
import com.example.data.provider.QuranTextProvider
import com.example.ui.components.IslamicGeometricCornerDecorations
import com.example.ui.components.IslamicOrnamentalDivider
import com.example.ui.components.IslamicStarMedallion
import com.example.data.provider.TafseerUiState
import com.example.data.provider.TafseerDownloadProgress
import com.example.data.remote.TafseerItem
import com.example.ui.components.IslamicGeometricBackground
import com.example.ui.components.share.AyahShareModalSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranReaderScreen(
    currentSurahNumber: Int,
    allSurahs: List<Surah>,
    surahText: SurahText?,
    isLoadingText: Boolean,
    readerFontScale: Float,
    readerTheme: ReaderTheme,
    bookmarks: List<QuranBookmark>,
    playerState: PlayerState,
    activeReciter: Reciter?,
    availableTafseers: List<TafseerItem> = emptyList(),
    selectedTafseerId: Int = 1,
    tafseerUiState: TafseerUiState = TafseerUiState(),
    tafseerDownloadProgress: TafseerDownloadProgress? = null,
    activeAyahNumber: Int? = null,
    activeWordIndex: Int? = null,
    onSelectSurah: (Int) -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onThemeChange: (ReaderTheme) -> Unit,
    onSelectTafseer: (Int) -> Unit = {},
    onLoadTafseer: (Ayah, Int, Int?) -> Unit = { _, _, _ -> },
    onDownloadSurahTafseer: ((tafseerId: Int, surahNumber: Int) -> Unit)? = null,
    onCancelDownloadSurahTafseer: (() -> Unit)? = null,
    onAddBookmark: (surahNumber: Int, surahName: String, ayahNumber: Int, page: Int, juz: Int, text: String, note: String) -> Unit,
    onDeleteBookmark: (String) -> Unit,
    onPlaySurahAudio: (Surah, Reciter?) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSaveProgress: (surahNumber: Int, surahName: String, ayahNumber: Int, page: Int, juz: Int) -> Unit,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var displayMode by remember { mutableStateOf(QuranDisplayMode.AYAH_BY_AYAH) }
    var showSurahSelectorDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showBookmarksDialog by remember { mutableStateOf(false) }
    var showDuaKhatmDialog by remember { mutableStateOf(false) }
    var selectedAyahForAction by remember { mutableStateOf<Ayah?>(null) }
    var showTafsirSheet by remember { mutableStateOf(false) }
    var showAyahCardShareSheet by remember { mutableStateOf(false) }
    var shareCardAyahNumber by remember { mutableStateOf(1) }

    val currentSurahMeta = allSurahs.find { it.number == currentSurahNumber } ?: allSurahs.firstOrNull()
    val isAudioPlayingThisSurah = playerState.isPlaying && playerState.currentItem?.surahNumber == currentSurahNumber

    // Colors according to chosen reader theme
    val bgColor = Color(readerTheme.bgHex)
    val textColor = Color(readerTheme.textHex)
    val cardBg = Color(readerTheme.cardBgHex)
    val accentColor = Color(readerTheme.accentHex)

    // Auto-scroll when active ayah changes during audio playback
    LaunchedEffect(activeAyahNumber, isAudioPlayingThisSurah) {
        if (isAudioPlayingThisSurah && activeAyahNumber != null && activeAyahNumber > 0) {
            val targetIndex = (activeAyahNumber - 1).coerceAtLeast(0)
            try {
                listState.animateScrollToItem(targetIndex)
            } catch (_: Exception) {}
        }
    }

    val isSurahValid = surahText != null && surahText.number == currentSurahNumber

    // Save reading progress on open strictly when surah text matches current surah identity
    LaunchedEffect(currentSurahNumber, isSurahValid) {
        if (currentSurahMeta != null && isSurahValid && surahText != null) {
            val firstAyah = surahText.ayahs.firstOrNull()
            onSaveProgress(
                surahText.number,
                surahText.name,
                firstAyah?.numberInSurah ?: 1,
                firstAyah?.page ?: 1,
                firstAyah?.juz ?: 1
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showSurahSelectorDialog = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "سورة ${currentSurahMeta?.name ?: "المصحف"}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = textColor
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "▼",
                                    fontSize = 10.sp,
                                    color = accentColor
                                )
                            }
                            Text(
                                text = "${currentSurahMeta?.type ?: "مكية"} • ${currentSurahMeta?.ayahs ?: 7} آية • جزء ${surahText?.ayahs?.firstOrNull()?.juz ?: 1}",
                                fontSize = 11.sp,
                                color = textColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "رجوع",
                                tint = textColor
                            )
                        }
                    } else {
                        IconButton(onClick = { showSurahSelectorDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "فهرس السور",
                                tint = accentColor
                            )
                        }
                    }
                },
                actions = {
                    // Toggle Display Mode (Ayah list / Page format)
                    IconButton(onClick = {
                        displayMode = if (displayMode == QuranDisplayMode.AYAH_BY_AYAH)
                            QuranDisplayMode.MUSHAF_PAGE
                        else
                            QuranDisplayMode.AYAH_BY_AYAH
                    }) {
                        Icon(
                            imageVector = if (displayMode == QuranDisplayMode.AYAH_BY_AYAH)
                                Icons.Default.ViewHeadline
                            else
                                Icons.Default.ViewAgenda,
                            contentDescription = "تبديل نمط العرض",
                            tint = textColor
                        )
                    }

                    // Bookmarks Button
                    IconButton(onClick = { showBookmarksDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = "العلامات المرجعية",
                            tint = textColor
                        )
                    }

                    // Reader Settings (Font size / Themes)
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = "خيارات الخط والألوان",
                            tint = textColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgColor,
                    titleContentColor = textColor
                )
            )
        },
        containerColor = bgColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            IslamicGeometricBackground(
                modifier = Modifier.fillMaxSize(),
                patternColor = accentColor.copy(alpha = 0.035f),
                spacing = 46.dp
            )

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
            // Audio Sync Bar (Listen to Surah while reading)
            AudioReadingSyncBar(
                surah = currentSurahMeta,
                activeReciter = activeReciter,
                isPlaying = isAudioPlayingThisSurah,
                accentColor = accentColor,
                textColor = textColor,
                cardBg = cardBg,
                onTogglePlay = {
                    if (currentSurahMeta != null) {
                        if (playerState.currentItem?.surahNumber == currentSurahNumber) {
                            onTogglePlayPause()
                        } else {
                            onPlaySurahAudio(currentSurahMeta, activeReciter)
                        }
                    }
                }
            )

            if (isLoadingText) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = accentColor)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "جاري تحميل نص السورة الكريمة...",
                            color = textColor.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                }
            } else if (isSurahValid && surahText != null) {
                if (displayMode == QuranDisplayMode.AYAH_BY_AYAH) {
                    // Ayah by Ayah List View
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("quran_ayahs_list"),
                        contentPadding = PaddingValues(bottom = 90.dp, top = 8.dp)
                    ) {
                        // Surah Header Banner
                        item {
                            SurahHeaderBanner(
                                surah = currentSurahMeta,
                                surahText = surahText,
                                accentColor = accentColor,
                                textColor = textColor,
                                cardBg = cardBg
                            )
                        }

                        // Basmalah Banner (except At-Tawbah 9)
                        if (currentSurahNumber != 9 && currentSurahNumber != 1) {
                            item {
                                BasmalahBanner(
                                    accentColor = accentColor,
                                    fontScale = readerFontScale
                                )
                            }
                        }

                        // Ayahs
                        items(surahText.ayahs, key = { "${surahText.number}_${it.numberInSurah}" }) { ayah ->
                            val isBookmarked = bookmarks.any {
                                it.surahNumber == surahText.number && it.ayahNumber == ayah.numberInSurah
                            }

                            val cleanText = QuranTextProvider.cleanBismillahFromVerse(
                                ayah.text,
                                surahText.number,
                                ayah.numberInSurah
                            )

                            val isActive = isAudioPlayingThisSurah && (activeAyahNumber == ayah.numberInSurah)

                            AyahCardItem(
                                ayah = ayah,
                                surahName = surahText.name,
                                surahNumber = surahText.number,
                                fontScale = readerFontScale,
                                textColor = textColor,
                                accentColor = accentColor,
                                cardBg = cardBg,
                                isBookmarked = isBookmarked,
                                isActiveAyah = isActive,
                                activeWordIndex = if (isActive) activeWordIndex else null,
                                onAyahClick = {
                                    selectedAyahForAction = ayah
                                },
                                onBookmarkToggle = {
                                    if (isBookmarked) {
                                        val bm = bookmarks.find {
                                            it.surahNumber == surahText.number && it.ayahNumber == ayah.numberInSurah
                                        }
                                        if (bm != null) onDeleteBookmark(bm.id)
                                    } else {
                                        onAddBookmark(
                                            surahText.number,
                                            surahText.name,
                                            ayah.numberInSurah,
                                            ayah.page,
                                            ayah.juz,
                                            cleanText,
                                            ""
                                        )
                                    }
                                },
                                onShowTafsir = {
                                    selectedAyahForAction = ayah
                                    showTafsirSheet = true
                                    onLoadTafseer(ayah, surahText.number, selectedTafseerId)
                                }
                            )
                        }

                        // Bottom Navigation Between Surahs
                        item {
                            SurahBottomNavigationControls(
                                currentSurahNumber = currentSurahNumber,
                                totalSurahs = allSurahs.size,
                                accentColor = accentColor,
                                textColor = textColor,
                                cardBg = cardBg,
                                onPreviousSurah = {
                                    if (currentSurahNumber > 1) {
                                        onSelectSurah(currentSurahNumber - 1)
                                    }
                                },
                                onNextSurah = {
                                    if (currentSurahNumber < allSurahs.size) {
                                        onSelectSurah(currentSurahNumber + 1)
                                    }
                                },
                                onOpenDuaKhatm = { showDuaKhatmDialog = true }
                            )
                        }
                    }
                } else {
                    // Continuous Mushaf Page View
                    ContinuousMushafView(
                        surah = currentSurahMeta,
                        surahText = surahText,
                        fontScale = readerFontScale,
                        textColor = textColor,
                        accentColor = accentColor,
                        cardBg = cardBg,
                        activeAyahNumber = if (isAudioPlayingThisSurah) activeAyahNumber else null,
                        activeWordIndex = if (isAudioPlayingThisSurah) activeWordIndex else null,
                        onAyahClick = { ayah ->
                            selectedAyahForAction = ayah
                        },
                        onOpenDuaKhatm = { showDuaKhatmDialog = true }
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "تعذر تحميل نص السورة الكريمة",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = textColor,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "يرجى التحقق من الاتصال بالإنترنت أو إعادة المحاولة",
                                fontSize = 13.sp,
                                color = textColor.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { onSelectSurah(currentSurahNumber) },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إعادة المحاولة", fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

    // 1. Ayah Action Modal Bottom Sheet
    if (selectedAyahForAction != null && !showTafsirSheet) {
        val ayah = selectedAyahForAction!!
        val isBookmarked = bookmarks.any {
            it.surahNumber == currentSurahNumber && it.ayahNumber == ayah.numberInSurah
        }
        val cleanAyahText = QuranTextProvider.cleanBismillahFromVerse(
            ayah.text,
            currentSurahNumber,
            ayah.numberInSurah
        )

        ModalBottomSheet(
            onDismissRequest = { selectedAyahForAction = null },
            containerColor = cardBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(20.dp)
            ) {
                Text(
                    text = "سورة ${currentSurahMeta?.name ?: ""} - الآية (${ayah.numberInSurah})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = accentColor
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "﴿ $cleanAyahText ﴾",
                    fontSize = 18.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Ayah Share Card Highlight Button
                Button(
                    onClick = {
                        shareCardAyahNumber = ayah.numberInSurah
                        showAyahCardShareSheet = true
                        selectedAyahForAction = null
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp).testTag("btn_share_designed_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مشاركة اللحظة (بطاقة تصميم مخصصة) 🌟", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Tafsir Button
                    OutlinedButton(
                        onClick = {
                            showTafsirSheet = true
                            onLoadTafseer(ayah, surahText?.number ?: currentSurahNumber, selectedTafseerId)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor)
                    ) {
                        Icon(Icons.Outlined.AutoStories, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("كتب التفسير", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    // Bookmark Button
                    OutlinedButton(
                        onClick = {
                            val activeSurahNum = surahText?.number ?: currentSurahNumber
                            val activeSurahName = surahText?.name ?: currentSurahMeta?.name ?: ""
                            if (isBookmarked) {
                                val bm = bookmarks.find {
                                    it.surahNumber == activeSurahNum && it.ayahNumber == ayah.numberInSurah
                                }
                                if (bm != null) onDeleteBookmark(bm.id)
                            } else {
                                onAddBookmark(
                                    activeSurahNum,
                                    activeSurahName,
                                    ayah.numberInSurah,
                                    ayah.page,
                                    ayah.juz,
                                    cleanAyahText,
                                    ""
                                )
                            }
                            selectedAyahForAction = null
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor)
                    ) {
                        Icon(
                            if (isBookmarked) Icons.Default.Bookmark else Icons.Outlined.Bookmark,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isBookmarked) "إزالة الفاصلة" else "حفظ فاصلة", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Copy Ayah
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText(
                                "Quran Ayah",
                                "﴿ $cleanAyahText ﴾ [سورة ${currentSurahMeta?.name}: ${ayah.numberInSurah}]"
                            )
                            clipboard.setPrimaryClip(clip)
                            selectedAyahForAction = null
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("نسخ النص", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    // Quick Text Share Ayah
                    OutlinedButton(
                        onClick = {
                            val playStoreUrl = "https://play.google.com/store/apps/details?id=com.aistudio.quranaudio.mskdra"
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "﴿ $cleanAyahText ﴾\n\n[سورة ${currentSurahMeta?.name} - الآية ${ayah.numberInSurah}]\n\n✦ حمّل تطبيق القرآن الكريم الآن:\n$playStoreUrl"
                                )
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة الآية الكريمة"))
                            selectedAyahForAction = null
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor)
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مشاركة نصية", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // 2. Tafsir Sheet (Multi-Tafseer Support)
    if (showTafsirSheet && selectedAyahForAction != null) {
        val ayah = selectedAyahForAction!!
        val cleanAyahText = QuranTextProvider.cleanBismillahFromVerse(
            ayah.text,
            currentSurahNumber,
            ayah.numberInSurah
        )
        ModalBottomSheet(
            onDismissRequest = {
                showTafsirSheet = false
                selectedAyahForAction = null
            },
            containerColor = cardBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "تفسير الآية ${ayah.numberInSurah} • سورة ${currentSurahMeta?.name}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = accentColor
                        )
                        Text(
                            text = "المصدر: ${tafseerUiState.tafseerName}",
                            fontSize = 12.sp,
                            color = textColor.copy(alpha = 0.7f)
                        )
                    }
                    IconButton(onClick = {
                        showTafsirSheet = false
                        selectedAyahForAction = null
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = textColor)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tafseer Selector Chips
                Text(
                    text = "اختر كتاب التفسير:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(6.dp))

                val tafseerOptions = if (availableTafseers.isNotEmpty()) {
                    availableTafseers
                } else {
                    listOf(
                        TafseerItem(1, "التفسير الميسر", "مجمع الملك فهد", "عربي"),
                        TafseerItem(2, "تفسير السعدي", "عبد الرحمن السعدي", "عربي"),
                        TafseerItem(3, "تفسير ابن كثير", "ابن كثير", "عربي"),
                        TafseerItem(4, "تفسير القرطبي", "القرطبي", "عربي"),
                        TafseerItem(5, "تفسير الطبري", "ابن جرير الطبري", "عربي"),
                        TafseerItem(6, "تفسير البغوي", "البغوي", "عربي"),
                        TafseerItem(7, "تفسير الجلالين", "جلال الدين المحلي والسيوطي", "عربي"),
                        TafseerItem(8, "التفسير الوسيط", "محمد سيد طنطاوي", "عربي")
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(tafseerOptions, key = { it.id }) { tItem ->
                        val isSelected = (tafseerUiState.tafseerId == tItem.id) || (selectedTafseerId == tItem.id && tafseerUiState.tafseerId == 0)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) accentColor else bgColor,
                            border = if (isSelected) null else BorderStroke(1.dp, textColor.copy(alpha = 0.2f)),
                            modifier = Modifier.clickable {
                                onSelectTafseer(tItem.id)
                                onLoadTafseer(ayah, surahText?.number ?: currentSurahNumber, tItem.id)
                            }
                        ) {
                            Text(
                                text = tItem.name,
                                color = if (isSelected) Color.White else textColor,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Ayah Text Box
                Text(
                    text = "﴿ $cleanAyahText ﴾",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tafseer Content View
                if (tafseerUiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = accentColor,
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "جاري تحميل التفسير...",
                                fontSize = 13.sp,
                                color = textColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else if (!tafseerUiState.tafseerText.isNullOrBlank()) {
                    Text(
                        text = tafseerUiState.tafseerText!!,
                        fontSize = 15.sp,
                        lineHeight = 26.sp,
                        color = textColor.copy(alpha = 0.95f),
                        textAlign = TextAlign.Justify
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bgColor, RoundedCornerShape(10.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = tafseerUiState.errorMessage ?: "التفسير غير متاح حاليًا لهذا الكتاب",
                            fontSize = 13.sp,
                            color = textColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                onLoadTafseer(ayah, surahText?.number ?: currentSurahNumber, selectedTafseerId)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إعادة المحاولة", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Action buttons (Copy & Share Tafsir)
                val currentTafsirContent = tafseerUiState.tafseerText ?: ayah.tafsir ?: ""
                if (currentTafsirContent.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText(
                                    "Quran Tafsir",
                                    "﴿ $cleanAyahText ﴾\n[سورة ${currentSurahMeta?.name}: ${ayah.numberInSurah}]\n\nتفسير (${tafseerUiState.tafseerName}):\n$currentTafsirContent"
                                )
                                clipboard.setPrimaryClip(clip)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("نسخ التفسير", fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedButton(
                            onClick = {
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "﴿ $cleanAyahText ﴾\n[سورة ${currentSurahMeta?.name} - الآية ${ayah.numberInSurah}]\n\nتفسير (${tafseerUiState.tafseerName}):\n$currentTafsirContent"
                                    )
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "مشاركة التفسير"))
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor)
                        ) {
                            Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مشاركة", fontSize = 12.sp)
                        }
                    }
                }

                if (onDownloadSurahTafseer != null) {
                    val activeSurahNum = surahText?.number ?: currentSurahNumber
                    val isCurrentDownloading = tafseerDownloadProgress != null &&
                        tafseerDownloadProgress.isDownloading &&
                        tafseerDownloadProgress.tafseerId == selectedTafseerId &&
                        tafseerDownloadProgress.surahNumber == activeSurahNum
                    val isCurrentCompleted = tafseerDownloadProgress != null &&
                        tafseerDownloadProgress.isCompleted &&
                        tafseerDownloadProgress.tafseerId == selectedTafseerId &&
                        tafseerDownloadProgress.surahNumber == activeSurahNum

                    Spacer(modifier = Modifier.height(10.dp))
                    if (isCurrentDownloading) {
                        val total = tafseerDownloadProgress!!.totalAyahs.coerceAtLeast(1)
                        val persisted = tafseerDownloadProgress.persistedAyahs
                        val fraction = (persisted.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.12f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "جاري حفظ تفسير (${tafseerDownloadProgress.tafseerName}) لسورة ${tafseerDownloadProgress.surahName}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (onCancelDownloadSurahTafseer != null) {
                                        TextButton(
                                            onClick = onCancelDownloadSurahTafseer,
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "إلغاء التنزيل", modifier = Modifier.size(14.dp), tint = accentColor)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("إلغاء", fontSize = 11.sp, color = accentColor)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "تم حفظ $persisted من أصل $total آية (${(fraction * 100).toInt()}%)",
                                    fontSize = 11.sp,
                                    color = textColor.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                androidx.compose.material3.LinearProgressIndicator(
                                    progress = { fraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = accentColor,
                                    trackColor = textColor.copy(alpha = 0.15f)
                                )
                            }
                        }
                    } else if (isCurrentCompleted) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.08f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تفسير (${tafseerDownloadProgress.tafseerName}) لسورة ${currentSurahMeta?.name ?: ""} محفوظ كاملاً دون اتصال",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = accentColor
                                )
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                onDownloadSurahTafseer(selectedTafseerId, activeSurahNum)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تحميل تفسير سورة ${currentSurahMeta?.name ?: ""} بالكامل دون اتصال",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // 3. Reader Settings Sheet (Font Size & Themes)
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = cardBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(20.dp)
            ) {
                Text(
                    text = "تخصيص القراءة والمظهر",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Font Size Slider
                Text(
                    text = "حجم خط المصحف: ${(readerFontScale * 100).toInt()}%",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(6.dp))
                Slider(
                    value = readerFontScale,
                    onValueChange = { onFontScaleChange(it) },
                    valueRange = 0.8f..1.8f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("أصغر (A-)", fontSize = 12.sp, color = textColor.copy(alpha = 0.6f))
                    Text("افتراضي", fontSize = 12.sp, color = textColor.copy(alpha = 0.6f))
                    Text("أكبر (A+)", fontSize = 12.sp, color = textColor.copy(alpha = 0.6f))
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = textColor.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))

                // Theme Selection
                Text(
                    text = "سمة وألوان المصحف:",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReaderTheme.values().forEach { theme ->
                        val isSelected = theme == readerTheme
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(theme.bgHex))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(theme.accentHex) else Color(theme.textHex).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { onThemeChange(theme) }
                                .padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = "﴿ ق ﴾",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(theme.textHex)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = theme.labelArabic,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = Color(theme.textHex)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // 4. Surah Index / Selector Dialog
    if (showSurahSelectorDialog) {
        SurahIndexDialog(
            allSurahs = allSurahs,
            currentSurahNumber = currentSurahNumber,
            onSelectSurah = { sNum ->
                onSelectSurah(sNum)
                showSurahSelectorDialog = false
            },
            onDismiss = { showSurahSelectorDialog = false }
        )
    }

    // 5. Bookmarks List Dialog
    if (showBookmarksDialog) {
        BookmarksListDialog(
            bookmarks = bookmarks,
            onSelectBookmark = { bm ->
                onSelectSurah(bm.surahNumber)
                showBookmarksDialog = false
            },
            onDeleteBookmark = onDeleteBookmark,
            onDismiss = { showBookmarksDialog = false }
        )
    }

    // 6. Dua Khatm Dialog
    if (showDuaKhatmDialog) {
        DuaKhatmQuranDialog(onDismiss = { showDuaKhatmDialog = false })
    }

    // 7. Moment Sharing Modal Sheet (Card Generator)
    if (showAyahCardShareSheet) {
        AyahShareModalSheet(
            surahNumber = currentSurahNumber,
            surahName = currentSurahMeta?.name ?: "",
            initialAyahNumber = shareCardAyahNumber,
            surahText = surahText,
            reciterName = activeReciter?.name ?: "الشيخ مشاري العفاسي",
            onDismiss = { showAyahCardShareSheet = false }
        )
    }
}

@Composable
fun AudioReadingSyncBar(
    surah: Surah?,
    activeReciter: Reciter?,
    isPlaying: Boolean,
    accentColor: Color,
    textColor: Color,
    cardBg: Color,
    onTogglePlay: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(accentColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isPlaying) "تلاوة صوتية تعمل الآن" else "استمع لسورة ${surah?.name ?: ""}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = "بصوت القارئ: ${activeReciter?.name ?: "مشاري العفاسي"}",
                        fontSize = 11.sp,
                        color = textColor.copy(alpha = 0.7f)
                    )
                }
            }

            Button(
                onClick = onTogglePlay,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isPlaying) "إيقاف مؤقت" else "تشغيل التلاوة",
                    fontSize = 11.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun SurahHeaderBanner(
    surah: Surah?,
    surahText: SurahText?,
    accentColor: Color,
    textColor: Color,
    cardBg: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, accentColor.copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Authentic geometric corner decorations
            IslamicGeometricCornerDecorations(
                modifier = Modifier.matchParentSize(),
                color = accentColor.copy(alpha = 0.5f),
                cornerLength = 16.dp,
                margin = 6.dp
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top ornament star medallion
                IslamicStarMedallion(
                    modifier = Modifier.size(24.dp),
                    color = accentColor.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "سُورَةُ ${surah?.name ?: ""}",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${surah?.type ?: "مكية"} • ${surah?.ayahs ?: 7} آيات",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ترتيبها ${surah?.number ?: 1}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

@Composable
fun BasmalahBanner(accentColor: Color, fontScale: Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IslamicOrnamentalDivider(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .padding(bottom = 8.dp),
            color = accentColor.copy(alpha = 0.5f)
        )

        Text(
            text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
            fontSize = (22 * fontScale).sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            textAlign = TextAlign.Center
        )

        IslamicOrnamentalDivider(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .padding(top = 8.dp),
            color = accentColor.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun AyahCardItem(
    ayah: Ayah,
    surahName: String,
    surahNumber: Int = 2,
    fontScale: Float,
    textColor: Color,
    accentColor: Color,
    cardBg: Color,
    isBookmarked: Boolean,
    isActiveAyah: Boolean = false,
    activeWordIndex: Int? = null,
    onAyahClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onShowTafsir: () -> Unit
) {
    val cleanAyahText = QuranTextProvider.cleanBismillahFromVerse(
        ayah.text,
        surahNumber,
        ayah.numberInSurah
    )

    val itemCardBg = if (isActiveAyah) accentColor.copy(alpha = 0.12f) else cardBg
    val borderModifier = if (isActiveAyah) {
        Modifier.border(1.5.dp, accentColor.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
    } else {
        Modifier
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .then(borderModifier)
            .clickable { onAyahClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = itemCardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ayah badge
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(if (isActiveAyah) accentColor else accentColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${ayah.numberInSurah}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActiveAyah) Color.White else accentColor
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Quick Tafsir Icon
                    IconButton(
                        onClick = onShowTafsir,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoStories,
                            contentDescription = "التفسير",
                            tint = if (isActiveAyah) accentColor else textColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Bookmark Icon
                    IconButton(
                        onClick = onBookmarkToggle,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "فاصلة مرجعية",
                            tint = if (isBookmarked) accentColor else textColor.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Ayah Text with Uthmani style and live word synchronization
            val words = remember(cleanAyahText) { cleanAyahText.trim().split(Regex("\\s+")) }

            val ayahAnnotatedString = buildAnnotatedString {
                if (isActiveAyah && activeWordIndex != null && activeWordIndex in words.indices) {
                    words.forEachIndexed { index, word ->
                        if (index == activeWordIndex) {
                            withStyle(
                                style = SpanStyle(
                                    background = accentColor.copy(alpha = 0.28f),
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append(word)
                            }
                        } else {
                            withStyle(
                                style = SpanStyle(
                                    color = textColor,
                                    fontWeight = FontWeight.Medium
                                )
                            ) {
                                append(word)
                            }
                        }
                        if (index < words.size - 1) {
                            append(" ")
                        }
                    }
                } else if (isActiveAyah) {
                    withStyle(
                        style = SpanStyle(
                            color = accentColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append(cleanAyahText)
                    }
                } else {
                    append(cleanAyahText)
                }

                withStyle(
                    style = SpanStyle(
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(" ﴿${ayah.numberInSurah}﴾")
                }
            }

            Text(
                text = ayahAnnotatedString,
                fontSize = (21 * fontScale).sp,
                lineHeight = (36 * fontScale).sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ContinuousMushafView(
    surah: Surah?,
    surahText: SurahText,
    fontScale: Float,
    textColor: Color,
    accentColor: Color,
    cardBg: Color,
    activeAyahNumber: Int? = null,
    activeWordIndex: Int? = null,
    onAyahClick: (Ayah) -> Unit,
    onOpenDuaKhatm: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 90.dp)
    ) {
        SurahHeaderBanner(
            surah = surah,
            surahText = surahText,
            accentColor = accentColor,
            textColor = textColor,
            cardBg = cardBg
        )

        if (surah?.number != 9 && surah?.number != 1) {
            BasmalahBanner(accentColor = accentColor, fontScale = fontScale)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Continuous text rendering with active ayah & word highlights
                val annotatedString = buildAnnotatedString {
                    surahText.ayahs.forEach { ayah ->
                        val cleanText = QuranTextProvider.cleanBismillahFromVerse(
                            ayah.text,
                            surah?.number ?: 2,
                            ayah.numberInSurah
                        )
                        val isActive = ayah.numberInSurah == activeAyahNumber
                        val words = cleanText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
                        if (isActive && activeWordIndex != null && activeWordIndex in words.indices) {
                            words.forEachIndexed { wIdx, word ->
                                if (wIdx == activeWordIndex) {
                                    withStyle(
                                        style = SpanStyle(
                                            background = accentColor.copy(alpha = 0.35f),
                                            color = accentColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    ) {
                                        append(word)
                                    }
                                } else {
                                    withStyle(
                                        style = SpanStyle(
                                            color = textColor,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    ) {
                                        append(word)
                                    }
                                }
                                if (wIdx < words.size - 1) append(" ")
                            }
                        } else if (isActive) {
                            withStyle(
                                style = SpanStyle(
                                    background = accentColor.copy(alpha = 0.18f),
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append(cleanText)
                            }
                        } else {
                            append(cleanText)
                        }
                        append(" ")
                        withStyle(style = SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) {
                            append("﴿${ayah.numberInSurah}﴾")
                        }
                        append(" ")
                    }
                }

                Text(
                    text = annotatedString,
                    fontSize = (22 * fontScale).sp,
                    lineHeight = (42 * fontScale).sp,
                    color = textColor,
                    textAlign = TextAlign.Justify,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onOpenDuaKhatm,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.MenuBook, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("دعاء ختم القرآن الكريم")
        }
    }
}

@Composable
fun SurahBottomNavigationControls(
    currentSurahNumber: Int,
    totalSurahs: Int,
    accentColor: Color,
    textColor: Color,
    cardBg: Color,
    onPreviousSurah: () -> Unit,
    onNextSurah: () -> Unit,
    onOpenDuaKhatm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = onPreviousSurah,
                enabled = currentSurahNumber > 1,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.SkipPrevious, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("السورة السابقة")
            }

            OutlinedButton(
                onClick = onNextSurah,
                enabled = currentSurahNumber < totalSurahs,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("السورة التالية")
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.SkipNext, contentDescription = null)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onOpenDuaKhatm,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = accentColor)
            Spacer(modifier = Modifier.width(6.dp))
            Text("دعاء ختم القرآن الكريم المبارك")
        }
    }
}

@Composable
fun SurahIndexDialog(
    allSurahs: List<Surah>,
    currentSurahNumber: Int,
    onSelectSurah: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = if (searchQuery.isBlank()) {
        allSurahs
    } else {
        val q = searchQuery.trim().lowercase()
        allSurahs.filter { it.name.contains(q) || it.number.toString() == q || it.englishName.lowercase().contains(q) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "فهرس سور القرآن الكريم (114 سورة)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث برقم أو اسم السورة...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                ) {
                    items(filtered) { surah ->
                        val isSelected = surah.number == currentSurahNumber
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { onSelectSurah(surah.number) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${surah.number}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "سورة ${surah.name}",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${surah.type} • ${surah.ayahs} آية",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

@Composable
fun BookmarksListDialog(
    bookmarks: List<QuranBookmark>,
    onSelectBookmark: (QuranBookmark) -> Unit,
    onDeleteBookmark: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "الفواصل المرجعية المحفوظة", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            if (bookmarks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لم تقم بحفظ أي علامات مرجعية بعد.\nاضغط على رمز الفاصلة بجانب أي آية لحفظها والرجوع إليها بسهولة.",
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                ) {
                    items(bookmarks) { bm ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onSelectBookmark(bm) },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "سورة ${bm.surahName} - الآية (${bm.ayahNumber})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (bm.ayahText.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "﴿ ${bm.ayahText.take(60)}... ﴾",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "صفحة ${bm.pageNumber} • جزء ${bm.juzNumber}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onDeleteBookmark(bm.id) }) {
                                    Icon(Icons.Default.Close, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

@Composable
fun DuaKhatmQuranDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "دعاء ختم القرآن الكريم",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 4.dp)
            ) {
                val duaText = """
اللَّهُمَّ ارْحَمْنِي بِالقُرْآنِ وَاجْعَلْهُ لِي إِمَاماً وَنُوراً وَهُدًى وَرَحْمَةً.

اللَّهُمَّ ذَكِّرْنِي مِنْهُ مَا نَسِيتُ وَعَلِّمْنِي مِنْهُ مَا جَهِلْتُ وَارْزُقْنِي تِلاَوَتَهُ آنَاءَ اللَّيْلِ وَأَطْرَافَ النَّهَارِ وَاجْعَلْهُ لِي حُجَّةً يَا رَبَّ العَالَمِينَ.

اللَّهُمَّ أَصْلِحْ لِي دِينِي الَّذِي هُوَ عِصْمَةُ أَمْرِي، وَأَصْلِحْ لِي دُنْيَايَ الَّتِي فِيهَا مَعَاشِي، وَأَصْلِحْ لِي آخِرَتِي الَّتِي فِيهَا مَعَادِي، وَاجْعَلِ الحَيَاةَ زِيَادَةً لِي فِي كُلِّ خَيْرٍ وَاجْعَلِ المَوْتَ رَاحَةً لِي مِنْ كُلِّ شَرٍّ.

اللَّهُمَّ اجْعَلْ خَيْرَ عُمْرِي آخِرَهُ وَخَيْرَ عَمَلِي خَوَاتِمَهُ وَخَيْرَ أَيَّامِي يَوْمَ أَلْقَاكَ فِيهِ.

اللَّهُمَّ إِنِّي أَسْأَلُكَ عِيشَةً هَنِيَّةً وَمِيتَةً سَوِيَّةً وَمَرَدّاً غَيْرَ مُخْزٍ وَلاَ فَاضِحٍ.

اللَّهُمَّ إِنِّي أَسْأَلُكَ خَيْرَ المَسْأَلَةِ وَخَيْرَ الدُّعَاءِ وَخَيْرَ النَّجَاحِ وَخَيْرَ العِلْمِ وَخَيْرَ العَمَلِ وَخَيْرَ الثَّوَابِ وَخَيْرَ الحَيَاةِ وَخَيْرَ المَمَاتِ وَثَبِّتْنِي وَثَقِّلْ مَوَازِينِي وَحَقِّقْ إِيمَانِي.

اللَّهُمَّ لاَ تَدَعْ لَنَا ذَنْباً إِلاَّ غَفَرْتَهُ وَلاَ هَمّاً إِلاَّ فَرَّجْتَهُ وَلاَ دَيْناً إِلاَّ قَضَيْتَهُ وَلاَ حَاجَةً مِنْ حَوَائِجِ الدُّنْيَا وَالآخِرَةِ إِلاَّ قَضَيْتَهَا يَا أَرْحَمَ الرَّاحِمِينَ.

وَصَلَّى اللهُ عَلَى نَبِيِّنَا مُحَمَّدٍ وَعَلَى آلِهِ وَصَحْبِهِ وَسَلَّمَ تَسْلِيماً كَثِيراً.
                """.trimIndent()

                Text(
                    text = duaText,
                    fontSize = 15.sp,
                    lineHeight = 26.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("آمين يا رب العالمين")
            }
        }
    )
}
