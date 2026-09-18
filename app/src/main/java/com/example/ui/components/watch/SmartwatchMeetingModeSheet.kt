package com.example.ui.components.watch

import android.Manifest
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HapticVibrationPattern
import com.example.data.model.NotificationCategory
import com.example.data.model.Reciter
import com.example.data.model.Surah
import com.example.data.model.WatchSyncSnapshot
import com.example.ui.theme.Gold400
import com.example.ui.theme.Gold500
import com.example.ui.theme.Gold600
import com.example.watch.MeetingModeManager
import com.example.watch.SmartwatchBridgeManager
import com.example.watch.WatchCommand

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SmartwatchMeetingModeSheet(
    meetingModeManager: MeetingModeManager,
    smartwatchBridgeManager: SmartwatchBridgeManager,
    allReciters: List<Reciter>,
    allSurahs: List<Surah>,
    onDismiss: () -> Unit,
    initialTab: Int = 0,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val meetingState by meetingModeManager.meetingState.collectAsState()
    val watchSnapshot by smartwatchBridgeManager.watchSnapshot.collectAsState()

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        meetingModeManager.setAutoCalendarDetect(isGranted)
        if (isGranted) {
            Toast.makeText(context, "تم تفعيل مزامنة التقويم التلقائية 📅", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "يلزم إذن قراءة التقويم لاكتشاف الاجتماعات تلقائياً", Toast.LENGTH_SHORT).show()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("btn_close_meeting_sheet")) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "الساعة الذكية ووضع الاجتماعات ⌚🔕",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "كتم فوري واهتزاز خفيف وتحكم سلس من المعصم",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (meetingState.isEnabled) Color(0xFFEF5350) else MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (meetingState.isEnabled) Icons.Default.NotificationsOff else Icons.Default.Watch,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Navigation Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = if (selectedTab == 0) Color(0xFFE57373) else MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.DoNotDisturbOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (selectedTab == 0) Color(0xFFE57373) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "وضع الاجتماعات 🔕",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Watch,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "التحكم من الساعة ⌚",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (selectedTab == 0) {
                // ==========================================
                // TAB 1: MEETING MODE (وضع الاجتماعات)
                // ==========================================
                MeetingModeTabContent(
                    meetingState = meetingState,
                    meetingModeManager = meetingModeManager,
                    calendarPermissionLauncher = { calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR) },
                    smartwatchBridgeManager = smartwatchBridgeManager
                )
            } else {
                // ==========================================
                // TAB 2: SMARTWATCH WRIST CONTROLS (التحكم من الساعة)
                // ==========================================
                SmartwatchWristTabContent(
                    watchSnapshot = watchSnapshot,
                    smartwatchBridgeManager = smartwatchBridgeManager,
                    allReciters = allReciters,
                    allSurahs = allSurahs
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MeetingModeTabContent(
    meetingState: com.example.data.model.MeetingModeState,
    meetingModeManager: MeetingModeManager,
    calendarPermissionLauncher: () -> Unit,
    smartwatchBridgeManager: SmartwatchBridgeManager
) {
    val context = LocalContext.current

    // 1. Primary Status Card
    val statusBgColor by animateColorAsState(
        targetValue = if (meetingState.isEnabled) Color(0xFF2C1314) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        label = "status_bg"
    )
    val statusBorderColor by animateColorAsState(
        targetValue = if (meetingState.isEnabled) Color(0xFFEF5350) else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
        label = "status_border"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = statusBgColor),
        border = BorderStroke(1.5.dp, statusBorderColor)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (meetingState.isEnabled) Color(0xFFEF5350).copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (meetingState.isEnabled) Icons.Default.NotificationsOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = if (meetingState.isEnabled) Color(0xFFEF5350) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (meetingState.isEnabled) "وضع الاجتماعات نشط 🔕" else "وضع الاجتماعات متوقف 🔔",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (meetingState.isEnabled) Color(0xFFFFCDD2) else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (meetingState.isEnabled) {
                                meetingState.activeMeetingTitle ?: "كتم الصوت وإيقاف التلاوة واهتزاز خفيف"
                            } else "التشغيل والتنبيهات تعمل بالوضع العادي",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (meetingState.isEnabled) Color(0xFFE57373) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = meetingState.isEnabled,
                    onCheckedChange = { meetingModeManager.toggleManualMeetingMode() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFEF5350),
                        uncheckedThumbColor = MaterialTheme.colorScheme.primary,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.testTag("switch_meeting_mode_toggle")
                )
            }

            // Remaining Time Countdown Banner
            AnimatedVisibility(visible = meetingState.isEnabled && meetingState.remainingDurationMinutes != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x33000000),
                        border = BorderStroke(1.dp, Color(0x33EF5350))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = Color(0xFFFF8A80),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "متبقي على انتهاء الاجتماع: ${meetingState.remainingDurationMinutes} دقيقة",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFFF8A80)
                            )
                        }
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 2. Quick Timed Meeting Duration Presets
    Text(
        text = "بدء اجتماع سريع مؤقت:",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Start
    )

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(15 to "15 د", 30 to "30 د", 45 to "45 د", 60 to "60 د").forEach { (mins, label) ->
            val isSelected = meetingState.isEnabled && meetingState.remainingDurationMinutes == mins
            OutlinedButton(
                onClick = { meetingModeManager.startMeetingTimer(mins, "اجتماع لمدة $mins دقيقة") },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("btn_timer_$mins"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSelected) Color(0xFFEF5350).copy(alpha = 0.2f) else Color.Transparent,
                    contentColor = if (isSelected) Color(0xFFEF5350) else MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(
                    if (isSelected) 1.5.dp else 1.dp,
                    if (isSelected) Color(0xFFEF5350) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            ) {
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // 3. Smart Auto-Detection Settings (Calendar & DND)
    Text(
        text = "التفعيل التلقائي الذكي:",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Start
    )

    Spacer(modifier = Modifier.height(8.dp))

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Calendar Sync Item
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("مزامنة تقويم Google والاجتماعات", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        Text("كتم التلاوة تلقائياً عند بدء أي اجتماع مجدول بالتقويم", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(
                    checked = meetingState.isAutoCalendarDetectEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            if (meetingModeManager.calendarDetector.hasCalendarPermission()) {
                                meetingModeManager.setAutoCalendarDetect(true)
                            } else {
                                calendarPermissionLauncher()
                            }
                        } else {
                            meetingModeManager.setAutoCalendarDetect(false)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

            // DND Sync Item
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.DoNotDisturbOn,
                        contentDescription = null,
                        tint = Gold500,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("التزامن مع وضع عدم الإزعاج (DND)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        Text("التحول للاجتماعات فور تفعيل وضع عدم الإزعاج بالجهاز", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(
                    checked = meetingState.isDndSyncEnabled,
                    onCheckedChange = { meetingModeManager.setDndSync(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Gold500,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // 4. Meeting Mode Actions & Haptic Customization
    Text(
        text = "إجراءات وسلوك وضع الاجتماعات:",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Start
    )

    Spacer(modifier = Modifier.height(8.dp))

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Auto pause playback toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("إيقاف التلاوة فوراً عند بدء الاجتماع", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    Text("كتم الصوت مباشرة دون أي تسريب صوتي مفاجئ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = meetingState.pausePlaybackOnMeeting,
                    onCheckedChange = { meetingModeManager.setPausePlaybackOnMeeting(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

            // Vibrate only notifications
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("توجيه التنبيهات لاهتزاز صامت على الساعة فقط", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    Text("منع أي صوت تنبيه على الهاتف أثناء الاجتماع", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = meetingState.vibrateOnlyNotifications,
                    onCheckedChange = { meetingModeManager.setVibrateOnlyNotifications(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

            // Auto resume prompt
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("اقتراح استئناف التلاوة بعد انتهاء الاجتماع", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    Text("اهتزازة لطيفة تسألك إن كنت ترغب بمتابعة ما كنت تسمعه", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = meetingState.autoResumePromptAfterMeeting,
                    onCheckedChange = { meetingModeManager.setAutoResumePrompt(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // 5. Test Haptic Vibration Patterns (أنماط الاهتزاز المميزة)
    Text(
        text = "أنماط الاهتزاز الذكية (Haptic Patterns):",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Start
    )
    Text(
        text = "أنماط نبضات مختلفة تمكنك من معرفة نوع التنبيه دون النظر للساعة",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Start
    )

    Spacer(modifier = Modifier.height(10.dp))

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HapticVibrationPattern.values().forEach { pattern ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = Gold500,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(pattern.titleAr, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            Text(pattern.descriptionAr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Button(
                        onClick = {
                            meetingModeManager.hapticManager.triggerHaptic(pattern)
                            Toast.makeText(context, "📳 ${pattern.titleAr}", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.height(36.dp).testTag("btn_test_haptic_${pattern.name}")
                    ) {
                        Text("جرّب 📳", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartwatchWristTabContent(
    watchSnapshot: WatchSyncSnapshot,
    smartwatchBridgeManager: SmartwatchBridgeManager,
    allReciters: List<Reciter>,
    allSurahs: List<Surah>
) {
    val context = LocalContext.current

    // 1. Realistic Smartwatch Dial / Display Mockup (Galaxy Watch / Apple Watch Style)
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "معاينة واجهة الساعة (Now Playing & Tile):",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )
        Text(
            text = "التحكم اللمسي المباشر من واجهة الساعة وWear OS Tile",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Watch Bezel & Screen
        Box(
            modifier = Modifier
                .size(240.dp)
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF020617))
                    )
                )
                .border(6.dp, Color(0xFF334155), CircleShape)
                .border(8.dp, Gold500.copy(alpha = 0.3f), CircleShape)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // Meeting status tiny pill
                if (watchSnapshot.isMeetingModeActive) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEF5350).copy(alpha = 0.25f),
                        border = BorderStroke(0.5.dp, Color(0xFFEF5350))
                    ) {
                        Text(
                            text = "🔕 وضع الاجتماع",
                            fontSize = 9.sp,
                            color = Color(0xFFFFCDD2),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Surah Name
                Text(
                    text = "سورة ${watchSnapshot.currentSurahName}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )

                // Reciter Name
                Text(
                    text = watchSnapshot.reciterName,
                    fontSize = 10.sp,
                    color = Gold400,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Watch Playback Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { smartwatchBridgeManager.dispatchWatchCommand(WatchCommand.PREVIOUS_SURAH) },
                        modifier = Modifier.size(32.dp).testTag("watch_btn_prev")
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "السابق", tint = Color.White, modifier = Modifier.size(18.dp))
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { smartwatchBridgeManager.dispatchWatchCommand(WatchCommand.TOGGLE_PLAY_PAUSE) }
                            .testTag("watch_btn_play_pause"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (watchSnapshot.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "تشغيل/إيقاف",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = { smartwatchBridgeManager.dispatchWatchCommand(WatchCommand.NEXT_SURAH) },
                        modifier = Modifier.size(32.dp).testTag("watch_btn_next")
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "التالي", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Quick meeting toggle from watch
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (watchSnapshot.isMeetingModeActive) Color(0xFFEF5350).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f),
                    border = BorderStroke(0.5.dp, if (watchSnapshot.isMeetingModeActive) Color(0xFFEF5350) else Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier.clickable {
                        smartwatchBridgeManager.dispatchWatchCommand(WatchCommand.TOGGLE_MEETING_MODE)
                    }.testTag("watch_btn_meeting_quick_toggle")
                ) {
                    Text(
                        text = if (watchSnapshot.isMeetingModeActive) "إلغاء الكتم 🔔" else "كتم اجتماع 🔕",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 2. Wear OS Tile & watchOS Complication ("متابعة من حيث توقفت")
    Text(
        text = "واجهة الساعة (Wear OS Tile / Complication):",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Start
    )

    Spacer(modifier = Modifier.height(8.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Gold500.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Replay, contentDescription = null, tint = Gold600, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("متابعة من حيث توقفت بلمسة واحدة", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        Text(
                            text = "سورة ${watchSnapshot.lastSavedSurahName} (الآية ${watchSnapshot.lastSavedAyahNumber}) • ${watchSnapshot.lastSavedReciterName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    smartwatchBridgeManager.dispatchWatchCommand(WatchCommand.RESUME_LAST_SAVED)
                    Toast.makeText(context, "جاري استئناف سورة ${watchSnapshot.lastSavedSurahName}...", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(42.dp).testTag("btn_watch_resume_tile"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("استئناف الاستماع الآن 🎧", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // 3. Standalone Watch Quick Surah Switcher
    Text(
        text = "اختيار سريع لسورة من الساعة:",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Start
    )

    Spacer(modifier = Modifier.height(8.dp))

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        val popularSurahs = allSurahs.filter { it.number in listOf(1, 18, 36, 55, 56, 67, 112, 113, 114) }
            .ifEmpty { allSurahs.take(8) }

        items(popularSurahs) { surah ->
            val isCurrent = watchSnapshot.currentSurahNumber == surah.number
            Surface(
                onClick = {
                    smartwatchBridgeManager.dispatchWatchCommand(WatchCommand.SELECT_SURAH, surah.number)
                    Toast.makeText(context, "تم اختيار سورة ${surah.name}", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(12.dp),
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = surah.name,
                        fontSize = 12.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 4. Standalone Watch Favorite Reciter Switcher
    Text(
        text = "اختيار القارئ المفضل مباشرة من الساعة:",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Start
    )

    Spacer(modifier = Modifier.height(8.dp))

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        val displayReciters = allReciters.take(8)
        items(displayReciters) { reciter ->
            val isCurrent = watchSnapshot.reciterName.contains(reciter.name) || reciter.name.contains(watchSnapshot.reciterName)
            Surface(
                onClick = {
                    smartwatchBridgeManager.dispatchWatchCommand(WatchCommand.SELECT_RECITER, reciter)
                    Toast.makeText(context, "تم اختيار القارئ: ${reciter.name}", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(12.dp),
                color = if (isCurrent) Gold500.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, if (isCurrent) Gold500 else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isCurrent) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Gold600, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = reciter.name,
                        fontSize = 12.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrent) Gold600 else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
