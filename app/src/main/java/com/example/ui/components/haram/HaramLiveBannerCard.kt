package com.example.ui.components.haram

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlayerState
import com.example.haram.HaramLocation
import com.example.haram.HaramNowPlaying
import com.example.ui.theme.Gold400
import com.example.ui.theme.Gold500

@Composable
fun HaramLiveBannerCard(
    makkahNowPlaying: HaramNowPlaying,
    madinahNowPlaying: HaramNowPlaying,
    playerState: PlayerState,
    isHaramNotificationsEnabled: Boolean,
    onPlayLiveHaram: (HaramLocation) -> Unit,
    onTogglePlayPause: () -> Unit,
    onOpenHaramSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Makkah, 1 = Madinah
    val currentNowPlaying = if (selectedTab == 0) makkahNowPlaying else madinahNowPlaying

    val isCurrentHaramPlaying = playerState.isPlaying &&
            playerState.currentItem?.reciterId == "haram_${currentNowPlaying.location.id}_live"

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("card_haram_live_recitation"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.2.dp, Gold500.copy(alpha = 0.55f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF09291D),
                            Color(0xFF061E15),
                            Color(0xFF04140E)
                        )
                    )
                )
        ) {
            // Subtle Islamic ornate corner decorations
            IslamicCornerOrnaments(
                color = Gold400.copy(alpha = 0.35f),
                modifier = Modifier.matchParentSize()
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Top Row: Live Indicator & Notification Customization Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Right in RTL: Live Pulsing Badge (الآن يُقرأ في الحرم)
                    Surface(
                        color = Color(0xFF3B1515).copy(alpha = 0.7f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF3B47).copy(alpha = pulseAlpha))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "الآن يُقرأ في الحرم",
                                color = Color(0xFFFFCDD2),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Left in RTL: Customize Notification Pill (تخصيص الإشعار)
                    Surface(
                        onClick = onOpenHaramSettings,
                        color = Color(0xFF103628).copy(alpha = 0.6f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isHaramNotificationsEnabled) Gold400.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "تخصيص الإشعار",
                                tint = if (isHaramNotificationsEnabled) Gold400 else Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "تخصيص الإشعار",
                                color = if (isHaramNotificationsEnabled) Gold400 else Color.White.copy(alpha = 0.9f),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Haram Switcher Chips: (الحرم المكي 🕋 vs الحرم النبوي 🕌)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val locations = listOf(
                        Triple(0, "الحرم المكي 🕋", HaramLocation.MAKKAH),
                        Triple(1, "الحرم النبوي 🕌", HaramLocation.MADINAH)
                    )

                    locations.forEach { (index, title, _) ->
                        val isSelected = selectedTab == index
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = index },
                            color = if (isSelected) Color.Transparent else Color(0xFF09291D).copy(alpha = 0.5f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                1.2.dp,
                                if (isSelected) Gold500 else Color(0xFF1E503E).copy(alpha = 0.5f)
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelected) {
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color(0xFFE4C379),
                                                    Color(0xFFD3A852),
                                                    Color(0xFFC79B42)
                                                )
                                            )
                                        } else {
                                            Brush.horizontalGradient(
                                                listOf(Color.Transparent, Color.Transparent)
                                            )
                                        }
                                    )
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    color = if (isSelected) Color(0xFF1B1605) else Color.White.copy(alpha = 0.85f),
                                    fontSize = 13.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Main Details & Play Control Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Right Column (Surah, Reciter, City badge)
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "سورة ${currentNowPlaying.surahName}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Color(0xFF0B4632),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.8.dp, Gold400.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = currentNowPlaying.location.shortName,
                                    color = Gold400,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = currentNowPlaying.reciterName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                            color = Color.White.copy(alpha = 0.88f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Left Column: Golden Circular Play/Pause Button with metallic ring
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    listOf(
                                        Color(0xFFF7E2B5),
                                        Color(0xFFC99700),
                                        Color(0xFFECC276),
                                        Color(0xFF9E7500),
                                        Color(0xFFF7E2B5)
                                    )
                                )
                            )
                            .padding(2.5.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFFFCE9BE),
                                        Color(0xFFE4BE6B),
                                        Color(0xFFB88C33)
                                    )
                                )
                            )
                            .clickable {
                                if (isCurrentHaramPlaying) {
                                    onTogglePlayPause()
                                } else {
                                    onPlayLiveHaram(currentNowPlaying.location)
                                }
                            }
                            .testTag("btn_play_haram_stream"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCurrentHaramPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isCurrentHaramPlaying) "إيقاف مؤقت" else "استمع الآن",
                            tint = Color(0xFF141208),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IslamicCornerOrnaments(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val strokeWidth = 1.2.dp.toPx()
        val cornerSize = 18.dp.toPx()
        val margin = 6.dp.toPx()

        // Top-Right Corner
        val pathTR = Path().apply {
            moveTo(size.width - margin, margin + cornerSize)
            lineTo(size.width - margin, margin)
            lineTo(size.width - margin - cornerSize, margin)
        }
        drawPath(pathTR, color = color, style = Stroke(width = strokeWidth))

        // Top-Left Corner
        val pathTL = Path().apply {
            moveTo(margin + cornerSize, margin)
            lineTo(margin, margin)
            lineTo(margin, margin + cornerSize)
        }
        drawPath(pathTL, color = color, style = Stroke(width = strokeWidth))

        // Bottom-Right Corner
        val pathBR = Path().apply {
            moveTo(size.width - margin, size.height - margin - cornerSize)
            lineTo(size.width - margin, size.height - margin)
            lineTo(size.width - margin - cornerSize, size.height - margin)
        }
        drawPath(pathBR, color = color, style = Stroke(width = strokeWidth))

        // Bottom-Left Corner
        val pathBL = Path().apply {
            moveTo(margin + cornerSize, size.height - margin)
            lineTo(margin, size.height - margin)
            lineTo(margin, size.height - margin - cornerSize)
        }
        drawPath(pathBL, color = color, style = Stroke(width = strokeWidth))
    }
}
