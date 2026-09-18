package com.example.ui.screens.prayer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.ui.components.IslamicGeometricBackground
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prayer.calculator.HijriCalendarHelper
import com.example.prayer.manager.PrayerManager
import com.example.ui.theme.Gold500

/**
 * Main "الصلاة" Tab Screen.
 * Contains:
 *  - Top quick summary: Next prayer name & live countdown timer.
 *  - Card 1: "مواقيت الصلاة" (Prayer Times) -> Opens detailed screen.
 *  - Card 2: "القبلة" (Qibla Direction) -> Opens detailed compass screen.
 */
sealed class PrayerSubScreen {
    object MainCards : PrayerSubScreen()
    object PrayerTimesDetail : PrayerSubScreen()
    object QiblaCompassDetail : PrayerSubScreen()
}

@Composable
fun PrayerTabScreen(
    prayerManager: PrayerManager,
    initialSubScreen: PrayerSubScreen = PrayerSubScreen.MainCards,
    modifier: Modifier = Modifier
) {
    var currentSubScreen by remember(initialSubScreen) { mutableStateOf(initialSubScreen) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        IslamicGeometricBackground(modifier = Modifier.fillMaxSize())

        AnimatedContent(
            targetState = currentSubScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "prayer_tab_nav",
            modifier = Modifier.fillMaxSize()
        ) { targetState ->
            when (targetState) {
                is PrayerSubScreen.MainCards -> {
                    PrayerTabMainCardsView(
                        prayerManager = prayerManager,
                        onOpenPrayerTimes = { currentSubScreen = PrayerSubScreen.PrayerTimesDetail },
                        onOpenQibla = { currentSubScreen = PrayerSubScreen.QiblaCompassDetail }
                    )
                }
                is PrayerSubScreen.PrayerTimesDetail -> {
                    PrayerTimesDetailScreen(
                        prayerManager = prayerManager,
                        onBack = { currentSubScreen = PrayerSubScreen.MainCards }
                    )
                }
                is PrayerSubScreen.QiblaCompassDetail -> {
                    QiblaCompassDetailScreen(
                        prayerManager = prayerManager,
                        onBack = { currentSubScreen = PrayerSubScreen.MainCards }
                    )
                }
            }
        }
    }
}

@Composable
private fun PrayerTabMainCardsView(
    prayerManager: PrayerManager,
    onOpenPrayerTimes: () -> Unit,
    onOpenQibla: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dayPrayerTimes by prayerManager.dayPrayerTimes.collectAsState()
    val qiblaData by prayerManager.qiblaData.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "الصلاة والقبلة",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dayPrayerTimes.dateHijri,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = dayPrayerTimes.location.cityName,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Quick Summary Hero Banner: Next Prayer + Live Countdown
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الصلاة القادمة",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = dayPrayerTimes.nextPrayer?.timeFormatted ?: "",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Gold500,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                val nextName = dayPrayerTimes.nextPrayer?.type?.arabicName ?: "الفجر"
                Text(
                    text = "صلاة $nextName",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                val countdown = HijriCalendarHelper.formatDurationArabic(dayPrayerTimes.timeUntilNextMillis)
                Text(
                    text = "متبقي على الأذان: $countdown",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Gold500
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Two Cards Side-by-Side: "مواقيت الصلاة" & "القبلة"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card 1: "مواقيت الصلاة" (Prayer Times)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpenPrayerTimes)
                    .testTag("card_prayer_times"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
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
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "فتح مواقيت الصلاة",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(180f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "مواقيت الصلاة",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "الأذان والتنبيهات",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = "٥ صلوات يومية",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Card 2: "القبلة" (Qibla Direction)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpenQibla)
                    .testTag("card_qibla_compass"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
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
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Gold500.copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "🕋",
                                    fontSize = 20.sp
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "فتح بوصلة القبلة",
                            tint = Gold500,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(180f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "اتجاه القبلة",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "بوصلة تفاعلية دقيقة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Gold500.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${HijriCalendarHelper.toArabicDigits(qiblaData.qiblaAngle.toInt())}° من الشمال",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Gold500,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Today's Prayer Schedule Header & List
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "جدول صلوات اليوم",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "عرض التفاصيل",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onOpenPrayerTimes)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            dayPrayerTimes.prayers.forEach { prayer ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (prayer.isNext) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = if (prayer.isNext) 1.5.dp else 1.dp,
                        color = if (prayer.isNext) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (prayer.isNext) 2.dp else 0.5.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (prayer.isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(8.dp)
                            ) {}

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = "صلاة ${prayer.type.arabicName}",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (prayer.isNext) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (prayer.isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )

                            if (prayer.isNext) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = "القادمة",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Text(
                            text = prayer.timeFormatted,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (prayer.isNext) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (prayer.isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}
