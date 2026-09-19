package com.example.ui.screens.prayer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prayer.calculator.HijriCalendarHelper
import com.example.prayer.location.PrayerLocationManager
import com.example.prayer.manager.PrayerManager
import com.example.prayer.model.AsrJuristic
import com.example.prayer.model.CalculationMethod
import com.example.prayer.model.IqamahSettings
import com.example.prayer.model.PrayerTimeItem
import com.example.prayer.model.PrayerTimeOffsets
import com.example.prayer.model.PrayerType
import com.example.prayer.notification.AdhanVoicesCatalog
import com.example.ui.theme.IslamicTheme
import kotlinx.coroutines.launch

/**
 * Detailed Prayer Times Screen.
 * Displays accurate astronomical times for all 5 daily prayers + sunrise,
 * live countdown, Adhan voice customization with audio preview, calculation methods,
 * Iqamah reminders, manual minute adjustments, and battery optimization guidance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimesDetailScreen(
    prayerManager: PrayerManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val dayPrayerTimes by prayerManager.dayPrayerTimes.collectAsState()
    val calculationMethod by prayerManager.calculationMethod.collectAsState()
    val asrJuristic by prayerManager.asrJuristic.collectAsState()
    val notificationSettings by prayerManager.notificationSettings.collectAsState()
    val prayerOffsets by prayerManager.prayerOffsets.collectAsState()
    val isPlayingPreview by prayerManager.isPlayingPreview.collectAsState()
    val isLocating by prayerManager.locationManager.isLocating.collectAsState()

    var showCityDialog by remember { mutableStateOf(false) }
    var showVoicesSheet by remember { mutableStateOf(false) }
    var showMethodDialog by remember { mutableStateOf(false) }
    var showOffsetsDialog by remember { mutableStateOf(false) }
    var showIqamahDialog by remember { mutableStateOf(false) }
    var showBatteryGuideDialog by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            scope.launch {
                prayerManager.refreshLocationGps()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            prayerManager.stopVoicePreview()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "مواقيت الصلاة والأذان",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_back_prayer_times")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (prayerManager.locationManager.hasLocationPermission()) {
                                scope.launch { prayerManager.refreshLocationGps() }
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        modifier = Modifier.testTag("btn_gps_refresh")
                    ) {
                        if (isLocating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "تحديث الموقع عبر GPS"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = Color.Transparent,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Date & City Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = dayPrayerTimes.dateHijri,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = dayPrayerTimes.dateGregorian,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            OutlinedButton(
                                onClick = { showCityDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("btn_change_city")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${dayPrayerTimes.location.cityName}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // 2. Next Prayer Countdown Hero Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    border = BorderStroke(1.dp, IslamicTheme.colors.cardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(IslamicTheme.colors.heroGradient)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "الصلاة القادمة",
                                style = MaterialTheme.typography.labelMedium,
                                color = IslamicTheme.colors.onHero.copy(alpha = 0.85f)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            val nextName = dayPrayerTimes.nextPrayer?.type?.arabicName ?: "الفجر"
                            Text(
                                text = "صلاة $nextName",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = IslamicTheme.colors.onHero
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Large Arabic Countdown
                            val countdownFormatted = HijriCalendarHelper.formatDurationArabic(dayPrayerTimes.timeUntilNextMillis)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.Black.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "متبقي: $countdownFormatted",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = IslamicTheme.colors.heroAccent
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Progress Indicator
                            val animatedProgress by animateFloatAsState(
                                targetValue = dayPrayerTimes.progressPercent,
                                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                                label = "prayer_progress"
                            )

                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = IslamicTheme.colors.heroAccent,
                                trackColor = IslamicTheme.colors.onHero.copy(alpha = 0.25f)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "الآن: ${dayPrayerTimes.currentPrayer?.type?.arabicName ?: "—"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IslamicTheme.colors.onHero.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = "الموعد: ${dayPrayerTimes.nextPrayer?.timeFormatted ?: ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IslamicTheme.colors.onHero.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Prayers List Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "مواقيت اليوم",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = calculationMethod.arabicName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 4. Prayer Items List
            items(dayPrayerTimes.prayers) { prayerItem ->
                PrayerRowItem(
                    item = prayerItem,
                    onToggleNotification = { prayerManager.togglePrayerNotification(prayerItem.type) }
                )
            }

            // 5. Settings & Customization Options Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "خيارات وتخصيص الأذان",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Adhan Voice Picker Button
            item {
                val currentVoice = AdhanVoicesCatalog.getVoiceById(notificationSettings.selectedVoiceId)
                PrayerSettingCard(
                    title = "صوت الأذان والمؤذن",
                    subtitle = "${currentVoice.name} (${currentVoice.muezzin})",
                    icon = Icons.Default.VolumeUp,
                    onClick = { showVoicesSheet = true }
                )
            }

            // Calculation Method Picker Button
            item {
                PrayerSettingCard(
                    title = "طريقة حساب المواقيت",
                    subtitle = calculationMethod.arabicName,
                    icon = Icons.Default.AccessTime,
                    onClick = { showMethodDialog = true }
                )
            }

            // Iqamah Notification Settings
            item {
                val iqamahStatus = if (notificationSettings.iqamahSettings.enabled) "مفعّل" else "معطّل"
                PrayerSettingCard(
                    title = "تنبيه وقت إقامة الصلاة",
                    subtitle = "الحالة: $iqamahStatus • تنبيه باقتراب الإقامة في المسجد",
                    icon = Icons.Default.NotificationsActive,
                    onClick = { showIqamahDialog = true }
                )
            }

            // Manual Offsets (+/- minutes)
            item {
                PrayerSettingCard(
                    title = "الضبط اليدوي للدقائق (+/-)",
                    subtitle = "تعديل فارق الدقائق لمطابقة مسجد الحي",
                    icon = Icons.Default.Tune,
                    onClick = { showOffsetsDialog = true }
                )
            }

            // Battery Optimization Helper
            item {
                PrayerSettingCard(
                    title = "إرشادات ضمان عمل الأذان في الخلفية",
                    subtitle = "استثناء التطبيق من توفير الطاقة على هواتف سامسونج، شاومي، هواوي",
                    icon = Icons.Default.BatteryAlert,
                    onClick = { showBatteryGuideDialog = true }
                )
            }
        }
    }

    // Modal: Adhan Voices Bottom Sheet
    if (showVoicesSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                prayerManager.stopVoicePreview()
                showVoicesSheet = false
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "اختر صوت الأذان",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "يمكنك الاستماع لمعاينة صوت الأذان قبل اعتماده كمنبّه رئيسي",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                AdhanVoicesCatalog.VOICES.forEach { voice ->
                    val isSelected = notificationSettings.selectedVoiceId == voice.id
                    val isPlayingThis = isPlayingPreview == voice.id

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                prayerManager.setAdhanVoice(voice.id)
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { prayerManager.setAdhanVoice(voice.id) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = voice.name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = voice.muezzin,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Preview Audio Button
                            IconButton(
                                onClick = { prayerManager.playVoicePreview(voice.id) }
                            ) {
                                Icon(
                                    imageVector = if (isPlayingThis) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = "معاينة الصوت",
                                    tint = if (isPlayingThis) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        prayerManager.stopVoicePreview()
                        showVoicesSheet = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("تم وحفظ الاختيار")
                }
            }
        }
    }

    // Dialog: City Selection
    if (showCityDialog) {
        CitySelectionDialog(
            currentCityName = dayPrayerTimes.location.cityName,
            onSelectCity = { preset ->
                prayerManager.selectPresetCity(preset)
                showCityDialog = false
            },
            onUseGps = {
                showCityDialog = false
                if (prayerManager.locationManager.hasLocationPermission()) {
                    scope.launch { prayerManager.refreshLocationGps() }
                } else {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            },
            onDismiss = { showCityDialog = false }
        )
    }

    // Dialog: Calculation Method Selection
    if (showMethodDialog) {
        CalculationMethodDialog(
            currentMethod = calculationMethod,
            currentAsrJuristic = asrJuristic,
            onSelectMethod = { method ->
                prayerManager.setCalculationMethod(method)
            },
            onSelectAsrJuristic = { juristic ->
                prayerManager.setAsrJuristic(juristic)
            },
            onDismiss = { showMethodDialog = false }
        )
    }

    // Dialog: Iqamah Settings
    if (showIqamahDialog) {
        IqamahSettingsDialog(
            currentSettings = notificationSettings.iqamahSettings,
            onSave = { updated ->
                prayerManager.setIqamahSettings(updated)
                showIqamahDialog = false
            },
            onDismiss = { showIqamahDialog = false }
        )
    }

    // Dialog: Manual Offsets
    if (showOffsetsDialog) {
        ManualOffsetsDialog(
            currentOffsets = prayerOffsets,
            onSave = { updated ->
                prayerManager.setPrayerOffsets(updated)
                showOffsetsDialog = false
            },
            onDismiss = { showOffsetsDialog = false }
        )
    }

    // Dialog: Battery Optimization Guidance
    if (showBatteryGuideDialog) {
        BatteryOptimizationGuideDialog(
            onOpenSettings = {
                try {
                    val intent = Intent().apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                        } else {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Fallback
                }
                showBatteryGuideDialog = false
            },
            onDismiss = { showBatteryGuideDialog = false }
        )
    }
}

@Composable
fun PrayerRowItem(
    item: PrayerTimeItem,
    onToggleNotification: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCurrentOrNext = item.isNext || item.isCurrent
    val backgroundColor = when {
        item.isNext -> IslamicTheme.colors.activeContainer
        item.isCurrent -> IslamicTheme.colors.badgeContainer
        item.isPassed -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = when {
        item.isNext -> MaterialTheme.colorScheme.primary
        item.isCurrent -> IslamicTheme.colors.goldAccent
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(if (isCurrentOrNext) 1.5.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Prayer Name + Status Badge
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.type.arabicName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = if (isCurrentOrNext) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isCurrentOrNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )

                        if (item.isNext) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = IslamicTheme.colors.selectedBadgeContainer
                            ) {
                                Text(
                                    text = "القادمة",
                                    fontSize = 10.sp,
                                    color = IslamicTheme.colors.selectedBadgeContent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (item.isCurrent) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = IslamicTheme.colors.badgeContainer
                            ) {
                                Text(
                                    text = "الحالية",
                                    fontSize = 10.sp,
                                    color = IslamicTheme.colors.goldText,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (item.minutesOffset != 0) {
                        val sign = if (item.minutesOffset > 0) "+${item.minutesOffset}" else "${item.minutesOffset}"
                        Text(
                            text = "تعديل: $sign دقيقة",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Prayer Formatted Time & Notification Toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.timeFormatted,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isCurrentOrNext) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (item.isPassed && !item.isCurrent) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(12.dp))

                if (item.type.isActualPrayer) {
                    IconButton(
                        onClick = onToggleNotification,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isNotificationEnabled) Icons.Default.Alarm else Icons.Default.AlarmOff,
                            contentDescription = "تفعيل الأذان",
                            tint = if (item.isNotificationEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PrayerSettingCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "تعديل",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun CitySelectionDialog(
    currentCityName: String,
    onSelectCity: (PrayerLocationManager.CityPreset) -> Unit,
    onUseGps: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCities = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            PrayerLocationManager.PRESET_CITIES
        } else {
            PrayerLocationManager.PRESET_CITIES.filter {
                it.nameAr.contains(searchQuery.trim()) || it.countryAr.contains(searchQuery.trim())
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "اختر المدينة لحساب المواقيت",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onUseGps,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.MyLocation, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تحديد موقعي الحالي تلقائياً عبر GPS")
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث عن مدينة أو دولة...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    items(filteredCities) { city ->
                        val isSelected = city.nameAr == currentCityName
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectCity(city) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = city.nameAr,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = city.countryAr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun CalculationMethodDialog(
    currentMethod: CalculationMethod,
    currentAsrJuristic: AsrJuristic,
    onSelectMethod: (CalculationMethod) -> Unit,
    onSelectAsrJuristic: (AsrJuristic) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "طريقة الحساب والمذهب",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "طريقة حساب مواقيت الصلاة:",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                CalculationMethod.entries.forEach { method ->
                    val isSelected = method == currentMethod
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectMethod(method) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectMethod(method) }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = method.arabicName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                            Text(
                                text = method.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "المذهب الفقهي لحساب وقت العصر:",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                AsrJuristic.entries.forEach { juristic ->
                    val isSelected = juristic == currentAsrJuristic
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectAsrJuristic(juristic) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectAsrJuristic(juristic) }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = juristic.arabicName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("حفظ")
            }
        }
    )
}

@Composable
fun IqamahSettingsDialog(
    currentSettings: IqamahSettings,
    onSave: (IqamahSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var isEnabled by remember { mutableStateOf(currentSettings.enabled) }
    var fajrMin by remember { mutableStateOf(currentSettings.fajrMinutes) }
    var dhuhrMin by remember { mutableStateOf(currentSettings.dhuhrMinutes) }
    var asrMin by remember { mutableStateOf(currentSettings.asrMinutes) }
    var maghribMin by remember { mutableStateOf(currentSettings.maghribMinutes) }
    var ishaMin by remember { mutableStateOf(currentSettings.ishaMinutes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "إعدادات تنبيه إقامة الصلاة",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تفعيل تنبيه الإقامة",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "فارق الدقائق بين الأذان والإقامة لكل صلاة:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                IqamahMinutePickerRow("الفجر", fajrMin, { fajrMin = it })
                IqamahMinutePickerRow("الظهر", dhuhrMin, { dhuhrMin = it })
                IqamahMinutePickerRow("العصر", asrMin, { asrMin = it })
                IqamahMinutePickerRow("المغرب", maghribMin, { maghribMin = it })
                IqamahMinutePickerRow("العشاء", ishaMin, { ishaMin = it })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        IqamahSettings(
                            enabled = isEnabled,
                            fajrMinutes = fajrMin,
                            dhuhrMinutes = dhuhrMin,
                            asrMinutes = asrMin,
                            maghribMinutes = maghribMin,
                            ishaMinutes = ishaMin
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun IqamahMinutePickerRow(
    name: String,
    currentMinutes: Int,
    onChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "صلاة $name:")
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { if (currentMinutes > 5) onChange(currentMinutes - 5) },
                modifier = Modifier.size(32.dp)
            ) {
                Text("–", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = "${HijriCalendarHelper.toArabicDigits(currentMinutes)} دقيقة",
                modifier = Modifier.padding(horizontal = 6.dp),
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = { if (currentMinutes < 60) onChange(currentMinutes + 5) },
                modifier = Modifier.size(32.dp)
            ) {
                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ManualOffsetsDialog(
    currentOffsets: PrayerTimeOffsets,
    onSave: (PrayerTimeOffsets) -> Unit,
    onDismiss: () -> Unit
) {
    var fajr by remember { mutableStateOf(currentOffsets.fajr) }
    var sunrise by remember { mutableStateOf(currentOffsets.sunrise) }
    var dhuhr by remember { mutableStateOf(currentOffsets.dhuhr) }
    var asr by remember { mutableStateOf(currentOffsets.asr) }
    var maghrib by remember { mutableStateOf(currentOffsets.maghrib) }
    var isha by remember { mutableStateOf(currentOffsets.isha) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "الضبط اليدوي للمواقيت (+/- دقائق)",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "يمكنك إضافة أو خصم دقائق لتتطابق مواقيت التطبيق مع مسجد الحي بالضبط:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OffsetAdjustRow("الفجر", fajr, { fajr = it })
                OffsetAdjustRow("الشروق", sunrise, { sunrise = it })
                OffsetAdjustRow("الظهر", dhuhr, { dhuhr = it })
                OffsetAdjustRow("العصر", asr, { asr = it })
                OffsetAdjustRow("المغرب", maghrib, { maghrib = it })
                OffsetAdjustRow("العشاء", isha, { isha = it })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        PrayerTimeOffsets(
                            fajr = fajr,
                            sunrise = sunrise,
                            dhuhr = dhuhr,
                            asr = asr,
                            maghrib = maghrib,
                            isha = isha
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("حفظ التعديلات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun OffsetAdjustRow(
    name: String,
    currentOffset: Int,
    onChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "$name:")
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { if (currentOffset > -30) onChange(currentOffset - 1) },
                modifier = Modifier.size(32.dp)
            ) {
                Text("–", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            val display = if (currentOffset > 0) "+${HijriCalendarHelper.toArabicDigits(currentOffset)}" else HijriCalendarHelper.toArabicDigits(currentOffset)
            Text(
                text = "$display د",
                modifier = Modifier.padding(horizontal = 6.dp),
                fontWeight = FontWeight.Bold,
                color = if (currentOffset != 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            IconButton(
                onClick = { if (currentOffset < 30) onChange(currentOffset + 1) },
                modifier = Modifier.size(32.dp)
            ) {
                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun BatteryOptimizationGuideDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "ضمان تشغيل الأذان في الخلفية",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "لضمان إطلاق صوت الأذان في موعده الدقيق حتى عندما يكون الهاتف مقفلاً تماماً أو شاشة الهاتف مغلقة، يُرجى تطبيق الخطوات التالية حسب نوع هاتفك:",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(10.dp))

                GuideSection("هواتف سامسونج (Samsung)", "الإعدادات ← التطبيقات ← تطبيق القرآن ← البطارية ← اختر 'غير مقيّد' (Unrestricted).")
                GuideSection("هواتف شاومي وبوكو (Xiaomi / Poco)", "الإعدادات ← إدارة التطبيقات ← تطبيق القرآن ← تفعيل 'التشغيل التلقائي' (Autostart) + توفير البطارية ← 'لا توجد قيود'.")
                GuideSection("هواتف هواوي وهونر (Huawei / Honor)", "الإعدادات ← البطارية ← بدء تشغيل التطبيقات ← تطبيق القرآن ← إدارة يدوية وتفعيل الكل.")
                GuideSection("هواتف أوبو وريلمي وفيفو (Oppo / Realme / Vivo)", "الإعدادات ← البطارية ← إدارة طاقة التطبيقات ← تطبيق القرآن ← السماح بالنشاط في الخلفية.")
            }
        },
        confirmButton = {
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("فتح إعدادات البطارية")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("حسناً فهمت")
            }
        }
    )
}

@Composable
fun GuideSection(title: String, desc: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "• $title",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
