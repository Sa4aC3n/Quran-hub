package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import com.example.ui.components.ReciterAvatar
import com.example.ui.components.IslamicGeometricBackground
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
import com.example.data.model.AudioQualityLevel
import com.example.data.model.BulkDownloadProgress
import com.example.data.model.BulkDownloadStatus
import com.example.data.model.DownloadState
import com.example.data.model.PlayerState
import com.example.data.model.RecitationEdition
import com.example.data.model.Reciter
import com.example.data.model.Surah
import com.example.ui.components.IslamicGradientCard
import com.example.ui.components.SurahNumberBadge
import com.example.ui.theme.Gold400
import com.example.ui.theme.Gold500
import com.example.ui.theme.Gold600

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReciterDetailScreen(
    reciter: Reciter,
    editions: List<RecitationEdition>,
    selectedEdition: RecitationEdition?,
    onSelectEdition: (RecitationEdition) -> Unit,
    surahs: List<Surah>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    playerState: PlayerState,
    downloadStates: Map<String, DownloadState>,
    bulkDownloadProgress: BulkDownloadProgress = BulkDownloadProgress(),
    preferredQuality: AudioQualityLevel,
    onSurahClick: (Surah) -> Unit,
    onDownloadSurah: (Surah) -> Unit,
    onStartBulkDownload: () -> Unit = {},
    onPauseBulkDownload: () -> Unit = {},
    onResumeBulkDownload: () -> Unit = {},
    onCancelBulkDownload: () -> Unit = {},
    onDeleteReciterDownloads: () -> Unit = {},
    onOpenQualityDialog: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ReciterAvatar(
                            reciter = reciter,
                            size = 38.dp,
                            borderWidth = 1.5.dp,
                            isPlaying = playerState.currentItem?.reciterId == reciter.id,
                            showPlayingBadge = false
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = reciter.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${reciter.riwayah} • ${reciter.location}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_back_to_home")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "الرجوع للقراء",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenQualityDialog,
                        modifier = Modifier.testTag("btn_detail_quality")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HighQuality,
                            contentDescription = "جودة الصوت",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
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
            // Header Search & Hero
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // Editions Selector (if multiple editions exist for this reciter)
                if (editions.size > 1) {
                    Text(
                        text = "المصاحف والتسجيلات المتاحة:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 6.dp)
                    ) {
                        items(editions) { edition ->
                            val isSelected = selectedEdition?.editionId == edition.editionId
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectEdition(edition) },
                                label = {
                                    Text(edition.name)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                // Reciter Hero Card
                if (searchQuery.isEmpty()) {
                    IslamicGradientCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ReciterAvatar(
                                reciter = reciter,
                                size = 72.dp,
                                borderWidth = 2.5.dp,
                                isPlaying = playerState.currentItem?.reciterId == reciter.id,
                                elevation = 4.dp
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (reciter.style.contains("مجود")) Gold500.copy(alpha = 0.2f)
                                                else MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = selectedEdition?.name ?: reciter.style,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (reciter.style.contains("مجود")) Gold600 else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${selectedEdition?.surahCount ?: 114} سورة",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = reciter.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = "بجودة ${preferredQuality.labelArabic}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Quick Play All button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                                        )
                                    )
                                    .clickable {
                                        surahs.firstOrNull()?.let { onSurahClick(it) }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .testTag("btn_play_from_start"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "تشغيل من البداية",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "بدء",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Full Mushaf Offline Download Card
                    val downloadedSurahsForThisReciter = surahs.count { surah ->
                        downloadStates["${reciter.id}_${surah.number}"]?.isDownloaded == true
                    }
                    val isFullMushafDownloaded = downloadedSurahsForThisReciter >= surahs.size && surahs.isNotEmpty()
                    val isThisReciterBulkDownloading = bulkDownloadProgress.reciterId == reciter.id &&
                            (bulkDownloadProgress.status == BulkDownloadStatus.DOWNLOADING || bulkDownloadProgress.status == BulkDownloadStatus.PAUSED)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isFullMushafDownloaded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                            else if (isThisReciterBulkDownloading) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isFullMushafDownloaded) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            else if (isThisReciterBulkDownloading) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = if (isFullMushafDownloaded) Icons.Default.DownloadDone
                                                      else if (isThisReciterBulkDownloading) Icons.Default.CloudDownload
                                                      else Icons.Default.FileDownload,
                                        contentDescription = null,
                                        tint = if (isFullMushafDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (isFullMushafDownloaded) "المصحف كامل ومحفوظ دون إنترنت"
                                                   else if (isThisReciterBulkDownloading) "جاري تنزيل المصحف كاملاً (${bulkDownloadProgress.completedSurahs}/${bulkDownloadProgress.totalSurahs})"
                                                   else "تحميل المصحف كاملاً (${downloadedSurahsForThisReciter}/${surahs.size} سورة)",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isFullMushafDownloaded) "جميع السور (114 سورة) متاحة للاستماع في أي وقت بدون شبكة"
                                                   else if (isThisReciterBulkDownloading) "سورة ${bulkDownloadProgress.currentSurahName} (${bulkDownloadProgress.currentSurahProgressPercent}%)"
                                                   else "تنزيل كافة السور بنقرة واحدة للاستماع دون اتصال",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (isThisReciterBulkDownloading) {
                                    Row {
                                        if (bulkDownloadProgress.status == BulkDownloadStatus.DOWNLOADING) {
                                            IconButton(onClick = onPauseBulkDownload, modifier = Modifier.size(34.dp)) {
                                                Icon(Icons.Default.Pause, contentDescription = "إيقاف مؤقت", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        } else {
                                            IconButton(onClick = onResumeBulkDownload, modifier = Modifier.size(34.dp)) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = "استئناف", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                        IconButton(onClick = onCancelBulkDownload, modifier = Modifier.size(34.dp)) {
                                            Icon(Icons.Default.Cancel, contentDescription = "إلغاء", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                } else if (isFullMushafDownloaded) {
                                    IconButton(
                                        onClick = onDeleteReciterDownloads,
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف التنزيلات",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                        )
                                    }
                                } else {
                                    Button(
                                        onClick = onStartBulkDownload,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("btn_bulk_download_mushaf")
                                    ) {
                                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تحميل الكل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (isThisReciterBulkDownloading) {
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { bulkDownloadProgress.overallPercent / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    strokeCap = StrokeCap.Round
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Internal Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_surahs_input"),
                    placeholder = {
                        Text(
                            text = "ابحث عن سورة بالاسم أو الرقم (مثال: 18 أو الكهف)...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "بحث",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "مسح",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            // Surahs List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp)
            ) {
                if (surahs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لم يتم العثور على سورة مطابقة",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(surahs, key = { it.number }) { surah ->
                        val isCurrentSurah = playerState.currentItem?.surahNumber == surah.number &&
                                             playerState.currentItem?.reciterId == reciter.id
                        val isPlaying = isCurrentSurah && playerState.isPlaying
                        val downloadKey = "${reciter.id}_${surah.number}"
                        val downloadState = downloadStates[downloadKey]

                        SurahItemCard(
                            surah = surah,
                            isCurrentSurah = isCurrentSurah,
                            isPlaying = isPlaying,
                            downloadState = downloadState,
                            onClick = { onSurahClick(surah) },
                            onDownloadClick = { onDownloadSurah(surah) }
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
fun SurahItemCard(
    surah: Surah,
    isCurrentSurah: Boolean,
    isPlaying: Boolean,
    downloadState: DownloadState?,
    onClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("surah_item_${surah.number}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentSurah) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isCurrentSurah) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentSurah) 3.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Surah Number
                SurahNumberBadge(
                    number = surah.number,
                    isSelected = isCurrentSurah,
                    size = 42.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "سورة ${surah.name}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isCurrentSurah) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )

                        if (isPlaying) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "قيد التشغيل",
                                tint = Gold600,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${surah.englishName} • ${surah.ayahs} آية • ${surah.type}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Actions (Download + Play indicator)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onDownloadClick,
                    modifier = Modifier.size(40.dp).testTag("btn_download_surah_${surah.number}")
                ) {
                    if (downloadState?.isDownloading == true) {
                        CircularProgressIndicator(
                            progress = { downloadState.progressPercent / 100f },
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    } else if (downloadState?.isDownloaded == true) {
                        Icon(
                            imageVector = Icons.Default.DownloadDone,
                            contentDescription = "تم التحميل للاستماع دون إنترنت",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "تحميل السورة",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCurrentSurah) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "تشغيل سورة ${surah.name}",
                        tint = if (isCurrentSurah) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
