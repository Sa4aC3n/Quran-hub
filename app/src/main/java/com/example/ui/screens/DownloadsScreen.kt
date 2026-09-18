package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.download.StorageFileInfo
import com.example.data.model.BulkDownloadProgress
import com.example.data.model.BulkDownloadStatus
import com.example.data.model.OfflineTextDownloadState
import com.example.data.model.PlayerState
import com.example.data.model.Reciter
import com.example.data.model.ReciterStorageInfo
import com.example.data.model.Surah
import com.example.ui.components.IslamicGeometricBackground
import com.example.ui.components.IslamicGradientCard
import com.example.ui.components.SurahNumberBadge
import com.example.ui.theme.Gold500
import com.example.ui.theme.Gold600

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    storageSummary: Triple<Int, Long, String>, // (count, bytes, formatted)
    downloadedFiles: List<StorageFileInfo>,
    recitersStorage: List<ReciterStorageInfo>,
    allReciters: List<Reciter>,
    allSurahs: List<Surah>,
    bulkDownloadProgress: BulkDownloadProgress,
    offlineTextDownloadState: OfflineTextDownloadState,
    wifiOnlyDownload: Boolean,
    playerState: PlayerState,
    onToggleWifiOnly: (Boolean) -> Unit,
    onStartBulkDownload: (Reciter) -> Unit,
    onPauseBulkDownload: () -> Unit,
    onResumeBulkDownload: (Reciter) -> Unit,
    onCancelBulkDownload: () -> Unit,
    onDeleteReciterDownloads: (reciterId: String, reciterName: String) -> Unit,
    onDeleteFile: (reciterId: String, surahNumber: Int) -> Unit,
    onClearAll: () -> Unit,
    onDownloadAllTexts: () -> Unit,
    onPlaySurah: (Surah, Reciter) -> Unit,
    onPlayReciterOffline: (reciterId: String) -> Unit,
    onSelectReciter: (Reciter) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showClearAllConfirm by remember { mutableStateOf(false) }
    var showDownloadReciterPicker by remember { mutableStateOf(false) }
    var reciterToDelete by remember { mutableStateOf<ReciterStorageInfo?>(null) }
    var expandedReciterId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "التحميلات والمصحف بدون نت",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${storageSummary.first} سورة محملة (${storageSummary.third})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_downloads")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (storageSummary.first > 0) {
                        IconButton(
                            onClick = { showClearAllConfirm = true },
                            modifier = Modifier.testTag("btn_clear_all_downloads")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "تفريغ الكل",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            IslamicGeometricBackground(modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
            // Storage Meter & Summary Hero Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                IslamicGradientCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "إجمالي التخزين المحلي",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = storageSummary.third,
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LibraryMusic,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${storageSummary.first} سورة",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Wi-Fi Only Switch Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (wifiOnlyDownload) Icons.Default.Wifi else Icons.Default.WifiOff,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "التحميل عبر Wi-Fi فقط",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = Color.White
                                )
                            }
                            Switch(
                                checked = wifiOnlyDownload,
                                onCheckedChange = onToggleWifiOnly,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = Color.White,
                                    uncheckedThumbColor = Color.White.copy(alpha = 0.7f),
                                    uncheckedTrackColor = Color.Black.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                }
            }

            // Active Bulk Download Card (if active or paused)
            AnimatedVisibility(
                visible = bulkDownloadProgress.status == BulkDownloadStatus.DOWNLOADING ||
                          bulkDownloadProgress.status == BulkDownloadStatus.PAUSED,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    progress = { bulkDownloadProgress.overallPercent / 100f },
                                    modifier = Modifier.size(28.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "جاري تحميل مصحف: ${bulkDownloadProgress.reciterName}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "سورة ${bulkDownloadProgress.currentSurahName} (${bulkDownloadProgress.completedSurahs} من ${bulkDownloadProgress.totalSurahs})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Row {
                                if (bulkDownloadProgress.status == BulkDownloadStatus.DOWNLOADING) {
                                    IconButton(onClick = onPauseBulkDownload) {
                                        Icon(
                                            imageVector = Icons.Default.Pause,
                                            contentDescription = "إيقاف مؤقت",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else {
                                    IconButton(onClick = {
                                        val rec = allReciters.find { it.id == bulkDownloadProgress.reciterId }
                                        if (rec != null) onResumeBulkDownload(rec)
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "استئناف",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                IconButton(onClick = onCancelBulkDownload) {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = "إلغاء",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { bulkDownloadProgress.overallPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }

            // Offline Quran Text Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "نصوص وتفاسير المصحف كاملاً",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (offlineTextDownloadState.isDownloading) "جاري تحميل سورة ${offlineTextDownloadState.currentSurahName} (${offlineTextDownloadState.downloadedSurahs}/114)..."
                                       else if (offlineTextDownloadState.isCompleted) "محفوظة بالكامل للقراءة دون اتصال"
                                       else "حفظ 114 سورة وتفسيرها للتشغيل بدون إنترنت",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (offlineTextDownloadState.isDownloading) {
                        CircularProgressIndicator(
                            progress = { offlineTextDownloadState.downloadedSurahs / 114f },
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (offlineTextDownloadState.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "محمل بالكامل",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        OutlinedButton(
                            onClick = onDownloadAllTexts,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("btn_download_quran_texts")
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تحميل الكل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Action: Download Complete Quran for a new Reciter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showDownloadReciterPicker = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_download_new_reciter_mushaf")
                ) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "تحميل مصحف قارئ كاملاً", fontWeight = FontWeight.Bold)
                }
            }

            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "القراء المحملون (${recitersStorage.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "كافة السور (${storageSummary.first})",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            // Search inside downloads
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_downloads_input"),
                placeholder = { Text("ابحث في التنزيلات المحلية...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Content per tab
            when (selectedTab) {
                0 -> {
                    // Reciters Grouped Tab
                    val filteredRecitersStorage = if (searchQuery.isBlank()) recitersStorage else {
                        val q = searchQuery.trim().lowercase()
                        recitersStorage.filter { it.reciterName.lowercase().contains(q) || it.riwayah.lowercase().contains(q) }
                    }

                    if (filteredRecitersStorage.isEmpty()) {
                        EmptyDownloadsState(
                            title = "لا توجد مصاحف محملة للقراء",
                            subtitle = "اضغط على زر 'تحميل مصحف قارئ كاملاً' لتنزيل 114 سورة والاستماع دون اتصال بالإنترنت",
                            onAction = { showDownloadReciterPicker = true }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredRecitersStorage, key = { it.reciterId }) { recInfo ->
                                val isExpanded = expandedReciterId == recInfo.reciterId
                                val matchingReciter = allReciters.find { it.id == recInfo.reciterId } ?: Reciter(
                                    id = recInfo.reciterId,
                                    name = recInfo.reciterName,
                                    riwayah = recInfo.riwayah,
                                    style = "مرتل",
                                    location = "",
                                    serverTemplates = emptyList()
                                )

                                DownloadedReciterCard(
                                    reciterInfo = recInfo,
                                    isExpanded = isExpanded,
                                    allSurahs = allSurahs,
                                    playerState = playerState,
                                    onToggleExpand = {
                                        expandedReciterId = if (isExpanded) null else recInfo.reciterId
                                    },
                                    onPlayAllOffline = { onPlayReciterOffline(recInfo.reciterId) },
                                    onDownloadRemaining = { onStartBulkDownload(matchingReciter) },
                                    onDeleteReciter = { reciterToDelete = recInfo },
                                    onPlaySurah = { surah -> onPlaySurah(surah, matchingReciter) },
                                    onDeleteSurah = { surahNum -> onDeleteFile(recInfo.reciterId, surahNum) }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // All Downloaded Surahs Flat Tab
                    val filteredFiles = if (searchQuery.isBlank()) downloadedFiles else {
                        val q = searchQuery.trim().lowercase()
                        downloadedFiles.filter { item ->
                            val surahName = allSurahs.find { it.number == item.surahNumber }?.name ?: ""
                            val reciterName = allReciters.find { it.id == item.reciterId }?.name ?: item.reciterId
                            surahName.contains(q) || reciterName.lowercase().contains(q) || item.surahNumber.toString() == q
                        }
                    }

                    if (filteredFiles.isEmpty()) {
                        EmptyDownloadsState(
                            title = "لا توجد سور محملة",
                            subtitle = "يمكنك تحميل السور بشكل فردي أو تحميل المصحف كاملاً للقارئ",
                            onAction = { showDownloadReciterPicker = true }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredFiles, key = { "${it.reciterId}_${it.surahNumber}" }) { item ->
                                val surah = allSurahs.find { it.number == item.surahNumber } ?: Surah(
                                    number = item.surahNumber,
                                    name = "سورة رقم ${item.surahNumber}",
                                    englishName = "",
                                    ayahs = 0,
                                    type = "مكية"
                                )
                                val reciter = allReciters.find { it.id == item.reciterId } ?: Reciter(
                                    id = item.reciterId,
                                    name = item.reciterId,
                                    riwayah = "حفص عن عاصم",
                                    style = "مرتل",
                                    location = "",
                                    serverTemplates = emptyList()
                                )
                                val isCurrentlyPlaying = playerState.currentItem?.surahNumber == item.surahNumber &&
                                                         playerState.currentItem?.reciterId == item.reciterId

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCurrentlyPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                                        else MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isCurrentlyPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { onPlaySurah(surah, reciter) }
                                        ) {
                                            SurahNumberBadge(
                                                number = surah.number,
                                                modifier = Modifier.size(38.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "سورة ${surah.name}",
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${reciter.name} • ${item.formattedSize}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { onPlaySurah(surah, reciter) }) {
                                                Icon(
                                                    imageVector = if (isCurrentlyPlaying && playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                    contentDescription = "تشغيل",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            IconButton(onClick = { onDeleteFile(item.reciterId, item.surahNumber) }) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "حذف السورة",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

    // Clear All Confirmation Dialog
    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = { Text("تفريغ كافة التنزيلات", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من رغبتك في حذف جميع السور والتلاوات المحملة (${storageSummary.first} سورة بمساحة ${storageSummary.third}) من الجهاز؟") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAll()
                        showClearAllConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("نعم، تفريغ الكل", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Reciter Delete Confirmation Dialog
    reciterToDelete?.let { recInfo ->
        AlertDialog(
            onDismissRequest = { reciterToDelete = null },
            title = { Text("حذف مصحف القارئ", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف ${recInfo.downloadedSurahsCount} سورة محملة للقارئ ${recInfo.reciterName} بمساحة ${recInfo.formattedSize}؟") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteReciterDownloads(recInfo.reciterId, recInfo.reciterName)
                        reciterToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { reciterToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Reciter Selector Dialog for Bulk Download
    if (showDownloadReciterPicker) {
        ReciterPickerForDownloadDialog(
            reciters = allReciters,
            onReciterSelected = { reciter ->
                showDownloadReciterPicker = false
                onStartBulkDownload(reciter)
            },
            onDismiss = { showDownloadReciterPicker = false }
        )
    }
}

@Composable
fun DownloadedReciterCard(
    reciterInfo: ReciterStorageInfo,
    isExpanded: Boolean,
    allSurahs: List<Surah>,
    playerState: PlayerState,
    onToggleExpand: () -> Unit,
    onPlayAllOffline: () -> Unit,
    onDownloadRemaining: () -> Unit,
    onDeleteReciter: () -> Unit,
    onPlaySurah: (Surah) -> Unit,
    onDeleteSurah: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isFullMushaf = reciterInfo.downloadedSurahsCount >= 114
    val percent = ((reciterInfo.downloadedSurahsCount * 100) / 114).coerceIn(0, 100)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (isFullMushaf) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    if (isFullMushaf) listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                                    else listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFullMushaf) Icons.Default.DownloadDone else Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = reciterInfo.reciterName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${reciterInfo.riwayah} • ${reciterInfo.formattedSize}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isFullMushaf) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = if (isFullMushaf) "مصحف كامل (114)" else "${reciterInfo.downloadedSurahsCount}/114 سورة",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { percent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = onPlayAllOffline,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تشغيل دون اتصال", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    if (!isFullMushaf) {
                        OutlinedButton(
                            onClick = onDownloadRemaining,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تحميل الباقي", fontSize = 11.sp)
                        }
                    }
                }

                Row {
                    TextButton(
                        onClick = onToggleExpand,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isExpanded) "إخفاء السور" else "عرض السور (${reciterInfo.downloadedSurahsCount})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDeleteReciter, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف سور القارئ",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Expanded list of surahs
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))

                    reciterInfo.downloadedSurahNumbers.forEach { surahNum ->
                        val surah = allSurahs.find { it.number == surahNum } ?: Surah(
                            number = surahNum,
                            name = "سورة $surahNum",
                            englishName = "",
                            ayahs = 0,
                            type = "مكية"
                        )
                        val isPlaying = playerState.currentItem?.surahNumber == surahNum &&
                                        playerState.currentItem?.reciterId == reciterInfo.reciterId

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    else Color.Transparent
                                )
                                .clickable { onPlaySurah(surah) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${surah.number}.",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.width(28.dp)
                                )
                                Text(
                                    text = "سورة ${surah.name}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onPlaySurah(surah) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying && playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "تشغيل",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteSurah(surahNum) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "حذف السورة",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyDownloadsState(
    title: String,
    subtitle: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("btn_empty_download_action")
            ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("تحميل مصحف كامل", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReciterPickerForDownloadDialog(
    reciters: List<Reciter>,
    onReciterSelected: (Reciter) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    val filtered = if (query.isBlank()) reciters else {
        val q = query.trim().lowercase()
        reciters.filter { it.name.lowercase().contains(q) || it.riwayah.lowercase().contains(q) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.fillMaxWidth(0.95f),
        title = {
            Column {
                Text(
                    text = "اختر القارئ لتنزيل المصحف كاملاً",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "سيتم تنزيل كافة السور (114 سورة) في الخلفية ~ 500-700 MB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("ابحث عن اسم القارئ...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered, key = { it.id }) { reciter ->
                        Card(
                            onClick = { onReciterSelected(reciter) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = reciter.name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${reciter.riwayah} • ${reciter.location}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = "تحميل",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}
