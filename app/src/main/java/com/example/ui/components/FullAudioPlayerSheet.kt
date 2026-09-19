package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapCalls
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DownloadState
import com.example.data.model.PlayerRepeatMode
import com.example.data.model.PlayerState
import com.example.data.model.Playlist
import com.example.data.model.SleepTimerMode
import com.example.data.model.SurahText
import com.example.ui.components.share.AyahShareModalSheet
import com.example.ui.theme.Gold400
import com.example.ui.theme.Gold500
import com.example.ui.theme.Gold600
import com.example.ui.theme.IslamicTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullAudioPlayerSheet(
    playerState: PlayerState,
    downloadState: DownloadState?,
    playlists: List<Playlist> = emptyList(),
    activeAyahNumber: Int? = null,
    surahText: SurahText? = null,
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekForward10: () -> Unit,
    onSeekRewind10: () -> Unit,
    onNextSurah: () -> Unit,
    onPreviousSurah: () -> Unit,
    onSwitchSource: (Int) -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onSetRepeatMode: (PlayerRepeatMode) -> Unit,
    onSetAutoPlayNext: (Boolean) -> Unit,
    onDownloadSurah: () -> Unit,
    onOpenReader: (Int) -> Unit = {},
    // Advanced Sleep Timer
    onStartSleepTimer: (minutes: Int, fadeOut: Boolean, mode: SleepTimerMode) -> Unit = { _, _, _ -> },
    onStartSleepTimerEndOfSurah: (fadeOut: Boolean) -> Unit = {},
    onAddSleepTimerMinutes: (Int) -> Unit = {},
    onCancelSleepTimer: () -> Unit = {},
    // Advanced Repetition & A-B Looping
    onSetRepeatCount: (Int) -> Unit = {},
    onSetABLoopStart: (Long) -> Unit = {},
    onSetABLoopEnd: (Long) -> Unit = {},
    onToggleABLoop: (Boolean) -> Unit = {},
    onClearABLoop: () -> Unit = {},
    onToggleShuffle: () -> Unit = {},
    // Custom Playlists
    onCreatePlaylist: (name: String, description: String, colorHex: String) -> Unit = { _, _, _ -> },
    onAddToPlaylist: (playlistId: String) -> Unit = {},
    // Smartwatch & Meeting Mode
    isMeetingModeActive: Boolean = false,
    onOpenMeetingModeSheet: (tab: Int) -> Unit = {},
    onToggleMeetingMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val item = playerState.currentItem ?: return

    var isUserDraggingSlider by remember { mutableStateOf(false) }
    var draggedSliderValue by remember { mutableFloatStateOf(0f) }
    var showSourceSelectorDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var showRepeatSettingsSheet by remember { mutableStateOf(false) }
    var showAddToPlaylistSheet by remember { mutableStateOf(false) }
    var showShareCardSheet by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "player_animation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (playerState.isPlaying) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            IslamicGeometricBackground(modifier = Modifier.matchParentSize())

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("btn_close_player")) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق المشغل",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "مشغل القرآن الكريم",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Sources Selector
                IconButton(
                    onClick = { showSourceSelectorDialog = true },
                    modifier = Modifier.testTag("btn_sources_menu")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapCalls,
                        contentDescription = "تغيير المصدر البديل",
                        tint = if (playerState.isFallbackActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Playlist Queue Status Indicator (if active)
            if (playerState.activePlaylistName != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "قائمة التشغيل: ${playerState.activePlaylistName} (${playerState.currentPlaylistIndex + 1}/${playerState.playlistQueue.size})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = onToggleShuffle,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "خلط القائمة",
                                tint = if (playerState.isShuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Central Reciter Photo Medallion with Luminous Gold Frame
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .scale(pulseScale),
                contentAlignment = Alignment.Center
            ) {
                // Background Glowing Halo Circle
                Box(
                    modifier = Modifier
                        .size(195.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Gold400.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Outer rotating Islamic star ornamental frame
                Box(
                    modifier = Modifier
                        .size(175.dp)
                        .rotate(if (playerState.isPlaying) rotationAngle else 0f)
                        .border(1.5.dp, Gold500.copy(alpha = 0.45f), RoundedCornerShape(36.dp))
                )

                // High Quality Reciter Avatar with Gold Frame
                ReciterAvatar(
                    reciterId = item.reciterId,
                    reciterName = item.reciterName,
                    size = 150.dp,
                    borderWidth = 3.5.dp,
                    isPlaying = playerState.isPlaying,
                    showPlayingBadge = false,
                    elevation = 8.dp
                )

                // Floating Surah Badge over bottom edge of Avatar
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF041910).copy(alpha = 0.88f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Gold400.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 6.dp)
                ) {
                    Text(
                        text = "سورة ${item.surahName}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Gold400,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Surah & Reciter Title Info
            Text(
                text = "سورة ${item.surahName}",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${item.reciterName} • ${item.riwayah}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Active Badges Row (Sleep Timer, Repeat Count, A-B Loop)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sleep Timer Active Pill
                if (playerState.isSleepTimerActive) {
                    Surface(
                        onClick = { showSleepTimerSheet = true },
                        shape = RoundedCornerShape(16.dp),
                        color = Gold500.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Gold500),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = null,
                                tint = Gold600,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val timerText = if (playerState.sleepTimerMode == SleepTimerMode.END_OF_SURAH) {
                                "نهاية السورة"
                            } else {
                                formatTimerDuration(playerState.sleepTimerRemainingSeconds)
                            }
                            Text(
                                text = "مؤقت النوم: $timerText",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Gold600
                            )
                        }
                    }
                }

                // Memorization Repeat Count Active Pill
                if (playerState.repeatSettings.targetCount > 0) {
                    Surface(
                        onClick = { showRepeatSettingsSheet = true },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Loop,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "تكرار للحفظ: ${playerState.repeatSettings.currentIteration}/${playerState.repeatSettings.targetCount}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // A-B Looping Active Pill
                if (playerState.repeatSettings.abLoop.isActive) {
                    Surface(
                        onClick = { showRepeatSettingsSheet = true },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🔁 تكرار أ-ب (تكرر ${playerState.repeatSettings.abLoop.loopCount}x)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Action Buttons Row (Read Surah & Share Moment Card)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Open Quran Reader Button
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            onDismiss()
                            onOpenReader(item.surahNumber)
                        }
                        .testTag("btn_read_surah_in_sheet")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "قراءة السورة",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "المصحف والتفسير 📖",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Share Current Moment / Ayah Card Button
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Gold500.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Gold500.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            showShareCardSheet = true
                        }
                        .testTag("btn_share_moment_card")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "مشاركة اللحظة",
                            modifier = Modifier.size(16.dp),
                            tint = Gold600
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (activeAyahNumber != null) "مشاركة الآية $activeAyahNumber 🌟" else "مشاركة اللحظة 🌟",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Gold600
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Time and Seek Slider
            val currentPos = if (isUserDraggingSlider) (draggedSliderValue * playerState.durationMs).toLong()
                             else playerState.currentPositionMs
            val duration = playerState.durationMs
            val sliderValue = if (duration > 0) (currentPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

            Slider(
                value = if (isUserDraggingSlider) draggedSliderValue else sliderValue,
                onValueChange = {
                    isUserDraggingSlider = true
                    draggedSliderValue = it
                },
                onValueChangeFinished = {
                    isUserDraggingSlider = false
                    val targetMs = (draggedSliderValue * playerState.durationMs).toLong()
                    onSeekTo(targetMs)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("player_seek_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDurationMs(currentPos),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatDurationMs(duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Playback Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 10s Rewind
                IconButton(
                    onClick = onSeekRewind10,
                    modifier = Modifier.size(46.dp).testTag("btn_rewind_10")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "رجوع 10 ثوان",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Previous Surah
                IconButton(
                    onClick = onPreviousSurah,
                    modifier = Modifier.size(52.dp).testTag("btn_prev_surah")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "السورة السابقة",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Large Main Play/Pause Button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .border(1.5.dp, IslamicTheme.colors.cardBorder, CircleShape)
                        .clickable(onClick = onTogglePlayPause)
                        .testTag("btn_main_play_pause"),
                    contentAlignment = Alignment.Center
                ) {
                    if (playerState.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(34.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "إيقاف مؤقت" else "تشغيل",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                // Next Surah
                IconButton(
                    onClick = onNextSurah,
                    modifier = Modifier.size(52.dp).testTag("btn_next_surah")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "السورة التالية",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // 10s Forward
                IconButton(
                    onClick = onSeekForward10,
                    modifier = Modifier.size(46.dp).testTag("btn_forward_10")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "تقديم 10 ثوان",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Enhanced Feature Toolbar: Sleep Timer, Repetition & A-B, Playlists, Speed, Download
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Sleep Timer Button
                IconButton(
                    onClick = { showSleepTimerSheet = true },
                    modifier = Modifier.testTag("btn_sleep_timer")
                ) {
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = "مؤقت النوم",
                        tint = if (playerState.isSleepTimerActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 2. Advanced Repetition & A-B Looping Button
                IconButton(
                    onClick = { showRepeatSettingsSheet = true },
                    modifier = Modifier.testTag("btn_repeat_settings")
                ) {
                    Icon(
                        imageVector = if (playerState.repeatMode == PlayerRepeatMode.REPEAT_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "خيارات التكرار والحفظ",
                        tint = if (playerState.repeatMode != PlayerRepeatMode.OFF || playerState.repeatSettings.targetCount > 0 || playerState.repeatSettings.abLoop.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 3. Add to Playlist Button
                IconButton(
                    onClick = { showAddToPlaylistSheet = true },
                    modifier = Modifier.testTag("btn_add_to_playlist")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistAdd,
                        contentDescription = "إضافة لقائمة تشغيل",
                        tint = if (playerState.activePlaylistId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 4. Playback Speed Selector
                Surface(
                    onClick = { showSpeedDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "سرعة التشغيل",
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${playerState.playbackSpeed}x",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 5. Download Surah Button
                IconButton(
                    onClick = onDownloadSurah,
                    modifier = Modifier.testTag("btn_download_surah")
                ) {
                    if (downloadState?.isDownloading == true) {
                        CircularProgressIndicator(
                            progress = { downloadState.progressPercent / 100f },
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    } else if (downloadState?.isDownloaded == true || playerState.isOfflineMode) {
                        Icon(
                            imageVector = Icons.Default.DownloadDone,
                            contentDescription = "تم التحميل",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "تحميل للاستماع دون إنترنت",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 6. Share Moment / Ayah Card Button
                IconButton(
                    onClick = { showShareCardSheet = true },
                    modifier = Modifier.testTag("btn_share_moment_toolbar")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "مشاركة بطاقة الآية",
                        tint = Gold500
                    )
                }

                // 7. Smartwatch & Meeting Mode Button
                IconButton(
                    onClick = { onOpenMeetingModeSheet(0) },
                    modifier = Modifier.testTag("btn_meeting_mode_player_toolbar")
                ) {
                    Icon(
                        imageVector = if (isMeetingModeActive) Icons.Default.NotificationsOff else Icons.Default.Watch,
                        contentDescription = "الساعة الذكية ووضع الاجتماعات",
                        tint = if (isMeetingModeActive) Color(0xFFEF5350) else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Auto Play Next Switch Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تشغيل تلقائي للسورة التالية",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = playerState.autoPlayNext,
                    onCheckedChange = onSetAutoPlayNext,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Gold400,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

    // ==========================================
    // 1. ADVANCED SLEEP TIMER BOTTOM SHEET
    // ==========================================
    if (showSleepTimerSheet) {
        var fadeOutEnabled by remember { mutableStateOf(playerState.sleepTimerFadeOutEnabled) }
        var customMinutes by remember { mutableIntStateOf(30) }

        ModalBottomSheet(
            onDismissRequest = { showSleepTimerSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = Gold600,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "مؤقت النوم الذكي 🌙",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (playerState.isSleepTimerActive) {
                        TextButton(
                            onClick = {
                                onCancelSleepTimer()
                                showSleepTimerSheet = false
                            }
                        ) {
                            Text("إلغاء المؤقت", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Active Timer Status Card
                if (playerState.isSleepTimerActive) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Gold500.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Gold500),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "المؤقت قيد التشغيل حالياً",
                                style = MaterialTheme.typography.labelMedium,
                                color = Gold600
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val timeRemaining = if (playerState.sleepTimerMode == SleepTimerMode.END_OF_SURAH) {
                                "سيتوقف عند نهاية سورة ${item.surahName}"
                            } else {
                                formatTimerDuration(playerState.sleepTimerRemainingSeconds)
                            }
                            Text(
                                text = timeRemaining,
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onAddSleepTimerMinutes(5) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("+5 د")
                                }
                                OutlinedButton(
                                    onClick = { onAddSleepTimerMinutes(15) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("+15 د")
                                }
                                OutlinedButton(
                                    onClick = { onAddSleepTimerMinutes(30) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("+30 د")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    text = "اختر مدة النوم:",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Quick Duration Chips
                val quickModes = listOf(
                    SleepTimerMode.MINUTES_5,
                    SleepTimerMode.MINUTES_15,
                    SleepTimerMode.MINUTES_30,
                    SleepTimerMode.MINUTES_45,
                    SleepTimerMode.MINUTES_60,
                    SleepTimerMode.END_OF_SURAH
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickModes.chunked(3).forEach { rowModes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowModes.forEach { mode ->
                                val isSelected = playerState.isSleepTimerActive && playerState.sleepTimerMode == mode
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (mode == SleepTimerMode.END_OF_SURAH) {
                                            onStartSleepTimerEndOfSurah(fadeOutEnabled)
                                        } else {
                                            onStartSleepTimer(mode.minutes, fadeOutEnabled, mode)
                                        }
                                        showSleepTimerSheet = false
                                    },
                                    label = {
                                        Text(
                                            text = mode.labelArabic,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Gold500,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom duration slider
                Text(
                    text = "أو اختر مدة مخصصة: $customMinutes دقيقة",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Slider(
                    value = customMinutes.toFloat(),
                    onValueChange = { customMinutes = it.toInt() },
                    valueRange = 1f..180f,
                    steps = 35,
                    colors = SliderDefaults.colors(
                        thumbColor = Gold500,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )

                Button(
                    onClick = {
                        onStartSleepTimer(customMinutes, fadeOutEnabled, SleepTimerMode.CUSTOM)
                        showSleepTimerSheet = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("بدء المؤقت لمدة $customMinutes دقيقة")
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // Volume Fade Out Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تلاشي تدريجي للصوت (Fade Out)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "يتم خفض مستوى الصوت بهدوء في آخر 30 ثانية لتسهيل النوم",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = fadeOutEnabled,
                        onCheckedChange = { fadeOutEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Gold400,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // ==========================================
    // 2. REPETITION & A-B LOOPING BOTTOM SHEET
    // ==========================================
    if (showRepeatSettingsSheet) {
        val repeatSettings = playerState.repeatSettings
        val abLoop = repeatSettings.abLoop

        ModalBottomSheet(
            onDismissRequest = { showRepeatSettingsSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "خيارات التكرار وتثبيت الحفظ 🔁",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 1: Standard Repeat Mode
                Text(
                    text = "1. نمط التكرار العام:",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = playerState.repeatMode == PlayerRepeatMode.OFF,
                        onClick = { onSetRepeatMode(PlayerRepeatMode.OFF) },
                        label = { Text("بدون تكرار") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = playerState.repeatMode == PlayerRepeatMode.REPEAT_ONE,
                        onClick = { onSetRepeatMode(PlayerRepeatMode.REPEAT_ONE) },
                        label = { Text("تكرار السورة") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = playerState.repeatMode == PlayerRepeatMode.REPEAT_ALL,
                        onClick = { onSetRepeatMode(PlayerRepeatMode.REPEAT_ALL) },
                        label = { Text("تكرار الكل") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Memorization Repeat Count
                Text(
                    text = "2. عداد تكرار السورة للمراجعة والحفظ:",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "يكرر السورة لعدد محدد من المرات ثم ينتقل للسورة التالية تلقائياً",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                val counts = listOf(0, 3, 5, 7, 10, 20)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    counts.forEach { count ->
                        val isSelected = repeatSettings.targetCount == count
                        val label = if (count == 0) "إلغاء" else "$count مرات"
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSetRepeatCount(count) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Section 3: A-B Looping Interval
                Text(
                    text = "3. تكرار مقطع صوتي مخصص (A - B):",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "حدد نقطة بداية ونهاية لتكرار آيات معينة مراراً لضبط التلاوة والحفظ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Point A
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "نقطة البداية (أ)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatDurationMs(abLoop.startPositionMs ?: 0L),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick = { onSetABLoopStart(playerState.currentPositionMs) },
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("تحديد (أ) الآن", fontSize = 11.sp)
                                }
                            }

                            // Point B
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "نقطة النهاية (ب)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatDurationMs(abLoop.endPositionMs ?: 0L),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Gold600
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick = { onSetABLoopEnd(playerState.currentPositionMs) },
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("تحديد (ب) الآن", fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (abLoop.isReady) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = abLoop.isActive,
                                        onCheckedChange = { onToggleABLoop(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Gold400,
                                            checkedTrackColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (abLoop.isActive) "التكرار نشط (تكرر ${abLoop.loopCount}x)" else "تفعيل التكرار",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }

                                TextButton(onClick = onClearABLoop) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "مسح",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("مسح النقاط", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // ==========================================
    // 3. ADD TO PLAYLIST BOTTOM SHEET
    // ==========================================
    if (showAddToPlaylistSheet) {
        var showCreateDialog by remember { mutableStateOf(false) }
        var newPlaylistName by remember { mutableStateOf("") }
        var newPlaylistDesc by remember { mutableStateOf("") }
        var selectedColorHex by remember { mutableStateOf("#10B981") }

        ModalBottomSheet(
            onDismissRequest = { showAddToPlaylistSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlaylistAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إضافة إلى قائمة تشغيل",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    TextButton(onClick = { showCreateDialog = !showCreateDialog }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("قائمة جديدة")
                    }
                }

                Text(
                    text = "سورة ${item.surahName} • بصوت ${item.reciterName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Create new playlist form
                AnimatedVisibility(visible = showCreateDialog) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            OutlinedTextField(
                                value = newPlaylistName,
                                onValueChange = { newPlaylistName = it },
                                label = { Text("اسم القائمة الجديدة (مثلاً: أذكار الصباح)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = newPlaylistDesc,
                                onValueChange = { newPlaylistDesc = it },
                                label = { Text("وصف اختياري") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Color Chips
                            val colors = listOf("#10B981", "#3B82F6", "#8B5CF6", "#F59E0B", "#EF4444", "#EC4899")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                colors.forEach { hex ->
                                    val isSelected = selectedColorHex == hex
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(parseHexColor(hex))
                                            .clickable { selectedColorHex = hex }
                                            .border(
                                                width = if (isSelected) 2.dp else 0.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    if (newPlaylistName.isNotBlank()) {
                                        onCreatePlaylist(newPlaylistName, newPlaylistDesc, selectedColorHex)
                                        newPlaylistName = ""
                                        newPlaylistDesc = ""
                                        showCreateDialog = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = newPlaylistName.isNotBlank()
                            ) {
                                Text("حفظ وإنشاء القائمة")
                            }
                        }
                    }
                }

                // Existing Playlists List
                if (playlists.isEmpty() && !showCreateDialog) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد قوائم تشغيل حالياً. اضغط على 'قائمة جديدة' لإنشاء أول قائمة تشغيل مخصصة لك!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    ) {
                        items(playlists, key = { it.id }) { pl ->
                            Surface(
                                onClick = {
                                    onAddToPlaylist(pl.id)
                                    showAddToPlaylistSheet = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(parseHexColor(pl.colorHex))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pl.name,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (pl.description.isNotBlank()) {
                                            Text(
                                                text = pl.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${pl.itemsCount} سور",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Source Selector Dialog
    if (showSourceSelectorDialog) {
        ModalBottomSheet(
            onDismissRequest = { showSourceSelectorDialog = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(20.dp)
            ) {
                Text(
                    text = "روابط ومصادر البث الصوتي البديلة",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "يتحول المشغل تلقائيًا للمصدر التالي عند انقطاع أحد الروابط. يمكنك أيضاً التبديل يدوياً:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                item.audioSources.forEachIndexed { index, url ->
                    val isSelected = playerState.currentSourceIndex == index && !playerState.isOfflineMode
                    val sourceName = when (index) {
                        0 -> "المصدر 1: خادم mp3quran المباشر (أساسي)"
                        1 -> "المصدر 2: خادم Quranic Audio CDN (احتياطي 1)"
                        2 -> "المصدر 3: شبكة الصوتيات الإسلامية العالمية (احتياطي 2)"
                        3 -> "المصدر 4: خادم الأرشيف المفتوح Archive.org (احتياطي 3)"
                        else -> "المصدر ${index + 1} (احتياطي إضافي)"
                    }

                    Surface(
                        onClick = {
                            onSwitchSource(index)
                            showSourceSelectorDialog = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sourceName,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = url,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "نشط الآن",
                                    tint = Gold600,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Playback Speed Dialog
    if (showSpeedDialog) {
        ModalBottomSheet(
            onDismissRequest = { showSpeedDialog = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(20.dp)
            ) {
                Text(
                    text = "اختر سرعة التلاوة",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                    speeds.forEach { speed ->
                        val isSelected = playerState.playbackSpeed == speed
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onSetPlaybackSpeed(speed)
                                showSpeedDialog = false
                            },
                            label = { Text("${speed}x") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Moment Sharing Modal Sheet
    if (showShareCardSheet) {
        AyahShareModalSheet(
            surahNumber = item.surahNumber,
            surahName = item.surahName,
            initialAyahNumber = activeAyahNumber ?: 1,
            surahText = surahText,
            reciterName = item.reciterName,
            onDismiss = { showShareCardSheet = false }
        )
    }
}

private fun parseHexColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        Color(0xFF0B1F3A)
    }
}

private fun formatTimerDuration(secondsRemaining: Long): String {
    val mins = secondsRemaining / 60
    val secs = secondsRemaining % 60
    return String.format("%02d:%02d", mins, secs)
}

private fun formatDurationMs(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
