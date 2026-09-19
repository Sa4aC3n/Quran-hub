package com.example.ui.screens.prayer

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prayer.calculator.HijriCalendarHelper
import com.example.prayer.manager.PrayerManager
import com.example.ui.theme.IslamicTheme
import kotlin.math.cos
import kotlin.math.sin

/**
 * Interactive Qibla Compass Screen.
 * Provides high-accuracy Great-Circle bearing towards the Holy Kaaba in Mecca,
 * real-time sensor fusion with smoothing, haptic alignment feedback, and calibration guides.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaCompassDetailScreen(
    prayerManager: PrayerManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val qiblaData by prayerManager.qiblaData.collectAsState()
    val locationInfo by prayerManager.locationManager.currentLocation.collectAsState()

    // Start compass sensor when screen is entered, stop on dispose
    DisposableEffect(Unit) {
        prayerManager.startCompass()
        onDispose {
            prayerManager.stopCompass()
        }
    }

    // Trigger haptic feedback when entering aligned state
    var wasAligned by remember { mutableStateOf(false) }
    LaunchedEffect(qiblaData.isAligned) {
        if (qiblaData.isAligned && !wasAligned) {
            triggerAlignmentHaptic(context)
        }
        wasAligned = qiblaData.isAligned
    }

    val animatedRotation by animateFloatAsState(
        targetValue = -qiblaData.userHeading,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "compass_rotation"
    )

    val alignedBorderColor by animateColorAsState(
        targetValue = if (qiblaData.isAligned) IslamicTheme.colors.goldAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        animationSpec = tween(300),
        label = "aligned_border"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "بوصلة القبلة المشرفة",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_back_qibla")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { prayerManager.recalculate() },
                        modifier = Modifier.testTag("btn_refresh_qibla")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تحديث"
                        )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Location & Mecca Distance Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${locationInfo.cityName}، ${locationInfo.countryName}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "المسافة إلى الكعبة: ${HijriCalendarHelper.toArabicDigits(qiblaData.distanceKm.toInt())} كم",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Bearing Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "${HijriCalendarHelper.toArabicDigits(qiblaData.qiblaAngle.toInt())}°",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Alignment Status Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = if (qiblaData.isAligned) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    if (qiblaData.isAligned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (qiblaData.isAligned) Icons.Default.CheckCircle else Icons.Default.NearMe,
                        contentDescription = null,
                        tint = if (qiblaData.isAligned) MaterialTheme.colorScheme.primary else IslamicTheme.colors.goldAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (qiblaData.isAligned) {
                            "✨ أنت تواجه اتجاه القبلة المشرفة بدقة الآن"
                        } else {
                            val diff = qiblaData.relativeAngle.toInt()
                            if (diff > 0) {
                                "حرّك الهاتف يميناً بمقدار ${HijriCalendarHelper.toArabicDigits(diff)}°"
                            } else {
                                "حرّك الهاتف يساراً بمقدار ${HijriCalendarHelper.toArabicDigits(-diff)}°"
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (qiblaData.isAligned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Interactive Compass Dial
            Box(
                modifier = Modifier
                    .size(310.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = if (qiblaData.isAligned) 3.dp else 1.dp,
                        color = alignedBorderColor,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Background compass dial that rotates with device heading
                val goldColor = IslamicTheme.colors.goldAccent
                val needleDefault = if (IslamicTheme.isDark) Color(0xFF90A4AE) else Color(0xFF1E3A5F)
                Canvas(
                    modifier = Modifier
                        .size(280.dp)
                        .rotate(animatedRotation)
                ) {
                    drawCompassDial(
                        qiblaAngle = qiblaData.qiblaAngle,
                        isAligned = qiblaData.isAligned,
                        pulseScale = if (qiblaData.isAligned) pulseGlow else 1f,
                        goldColor = goldColor,
                        needleDefaultColor = needleDefault
                    )
                }

                // Center Kaaba Emblem & Status
                Surface(
                    shape = CircleShape,
                    color = if (qiblaData.isAligned) IslamicTheme.colors.badgeContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(2.dp, if (qiblaData.isAligned) IslamicTheme.colors.goldAccent else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "🕋",
                            fontSize = 32.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Degree Information Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CompassInfoCard(
                    title = "زاوية القبلة",
                    value = "${HijriCalendarHelper.toArabicDigits(qiblaData.qiblaAngle.toInt())}°",
                    subtitle = "من الشمال الحقيقي"
                )

                CompassInfoCard(
                    title = "اتجاه الجهاز",
                    value = "${HijriCalendarHelper.toArabicDigits(qiblaData.userHeading.toInt())}°",
                    subtitle = "البوصلة الحالية"
                )

                CompassInfoCard(
                    title = "الفارق",
                    value = "${HijriCalendarHelper.toArabicDigits(kotlin.math.abs(qiblaData.relativeAngle.toInt()))}°",
                    subtitle = if (qiblaData.isAligned) "محاذاة تامة" else "للوصول للقبلة"
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Calibration Guidance Tip
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CompassCalibration,
                        contentDescription = null,
                        tint = IslamicTheme.colors.goldAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "دقة البوصلة والمعايرة",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "للحصول على أعلى دقة، ضع الهاتف بشكل أفقي مستوٍ وابتعد عن المعادن والأجهزة الإلكترونية. في حال عدم الثبات حرّك الهاتف على شكل رقم (8) لمعايرة المستشعر.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompassInfoCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(105.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun DrawScope.drawCompassDial(
    qiblaAngle: Float,
    isAligned: Boolean,
    pulseScale: Float,
    goldColor: Color,
    needleDefaultColor: Color
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = size.width / 2f

    // Draw degree tick marks
    for (i in 0 until 360 step 5) {
        val angleRad = Math.toRadians(i.toDouble())
        val isMajor = i % 30 == 0
        val isCardinal = i % 90 == 0

        val tickLength = when {
            isCardinal -> 16f
            isMajor -> 10f
            else -> 6f
        }

        val strokeWidth = when {
            isCardinal -> 3f
            isMajor -> 2f
            else -> 1f
        }

        val tickColor = when {
            i == 0 -> Color(0xFFEF5350) // North in Red
            isCardinal -> goldColor
            isMajor -> Color.Gray
            else -> Color.Gray.copy(alpha = 0.4f)
        }

        val startX = center.x + (radius - tickLength) * sin(angleRad).toFloat()
        val startY = center.y - (radius - tickLength) * cos(angleRad).toFloat()
        val endX = center.x + radius * sin(angleRad).toFloat()
        val endY = center.y - radius * cos(angleRad).toFloat()

        drawLine(
            color = tickColor,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }

    // Draw Qibla Pointer Pointer Line & Needle towards Kaaba angle
    rotate(qiblaAngle, pivot = center) {
        val needleColor = if (isAligned) goldColor else needleDefaultColor

        // Golden / Navy needle pointing towards Kaaba
        val path = Path().apply {
            moveTo(center.x, center.y - radius + 20f)
            lineTo(center.x - 14f, center.y - 45f)
            lineTo(center.x + 14f, center.y - 45f)
            close()
        }

        drawPath(
            path = path,
            color = needleColor,
            style = Fill
        )

        // Draw Kaaba pointer icon circle on top edge
        drawCircle(
            color = needleColor,
            radius = 12f * pulseScale,
            center = Offset(center.x, center.y - radius + 22f)
        )
    }
}

private fun triggerAlignmentHaptic(context: Context) {
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(80)
        }
    } catch (e: Exception) {
        // Ignore
    }
}
