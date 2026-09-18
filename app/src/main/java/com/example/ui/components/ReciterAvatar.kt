package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Reciter
import com.example.ui.theme.Gold200
import com.example.ui.theme.Gold400
import com.example.ui.theme.Gold500
import com.example.ui.theme.Gold600

/**
 * Standard Reciter Avatar displaying the initial letters of the reciter's name
 * encased in a gold border and emerald gradient medallion.
 */
@Composable
fun ReciterAvatar(
    reciterId: String?,
    reciterName: String,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    size: Dp = 56.dp,
    borderWidth: Dp = 2.dp,
    isPlaying: Boolean = false,
    showPlayingBadge: Boolean = true,
    elevation: Dp = 3.dp
) {
    // Extract initials (first 2 letters or initials of the reciter's name)
    val initials = remember(reciterName) {
        extractReciterInitials(reciterName)
    }

    // Gold Gradient for the luxurious frame
    val goldGradient = remember {
        Brush.sweepGradient(
            colors = listOf(
                Gold200,
                Gold400,
                Gold600,
                Gold500,
                Gold200,
                Gold400,
                Gold600,
                Gold200
            )
        )
    }

    val pulseScale = if (isPlaying) {
        val activeHaloTransition = rememberInfiniteTransition(label = "avatar_pulse")
        val anim by activeHaloTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
        anim
    } else {
        1f
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing ring when playing
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .size(size)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .border(
                        BorderStroke(1.5.dp, Gold400.copy(alpha = 0.6f)),
                        CircleShape
                    )
            )
        }

        // Main Circular Gold Frame Container
        Box(
            modifier = Modifier
                .size(size - 2.dp)
                .shadow(if (isPlaying) elevation + 2.dp else elevation, CircleShape)
                .clip(CircleShape)
                .background(goldGradient)
                .padding(borderWidth),
            contentAlignment = Alignment.Center
        ) {
            // Inner Circular Initials Badge
            Box(
                modifier = Modifier
                    .size(size - (borderWidth * 2) - 2.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                if (isPlaying) Color(0xFF16593F) else Color(0xFF114C37),
                                if (isPlaying) Color(0xFF08261A) else Color(0xFF072219)
                            )
                        )
                    )
                    .border(0.5.dp, Gold400.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val fontSize = (size.value * 0.32f).sp
                Text(
                    text = initials,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = Gold400,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Active Playing Equalizer Badge at bottom-end corner
        if (isPlaying && showPlayingBadge && size >= 40.dp) {
            val badgeSize = (size * 0.38f).coerceIn(16.dp, 24.dp)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 1.dp, y = 1.dp)
                    .size(badgeSize)
                    .shadow(3.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Gold400, Gold600)
                        )
                    )
                    .border(1.dp, Color(0xFF041910), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "قيد التشغيل",
                    tint = Color(0xFF072118),
                    modifier = Modifier.size(badgeSize * 0.65f)
                )
            }
        }
    }
}

/**
 * Overload taking a [Reciter] object.
 */
@Composable
fun ReciterAvatar(
    reciter: Reciter,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    borderWidth: Dp = 2.dp,
    isPlaying: Boolean = false,
    showPlayingBadge: Boolean = true,
    elevation: Dp = 3.dp
) {
    ReciterAvatar(
        reciterId = reciter.id,
        reciterName = reciter.name,
        modifier = modifier,
        imageUrl = null,
        size = size,
        borderWidth = borderWidth,
        isPlaying = isPlaying,
        showPlayingBadge = showPlayingBadge,
        elevation = elevation
    )
}

/**
 * Fallback badge function for any external caller.
 */
@Composable
fun ReciterFallbackInitialsBadge(
    reciterName: String,
    isPlaying: Boolean,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val initials = remember(reciterName) {
        extractReciterInitials(reciterName)
    }
    val fontSize = (size.value * 0.32f).sp

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(
                        if (isPlaying) Color(0xFF16593F) else Color(0xFF114C37),
                        if (isPlaying) Color(0xFF08261A) else Color(0xFF072219)
                    )
                )
            )
            .border(0.5.dp, Gold400.copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = Gold400,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * Helper to extract initials (first two letters) from a reciter's name.
 */
private fun extractReciterInitials(reciterName: String): String {
    val cleaned = reciterName.trim()
    if (cleaned.isEmpty()) return "ق"

    val words = cleaned.split("\\s+".toRegex())
        .map { it.trim() }
        .filter { word ->
            word.isNotEmpty() &&
            !word.equals("الشيخ", ignoreCase = true) &&
            !word.equals("الدكتور", ignoreCase = true) &&
            !word.equals("القارئ", ignoreCase = true) &&
            !word.equals("أ.", ignoreCase = true) &&
            !word.equals("د.", ignoreCase = true)
        }

    return when {
        words.size >= 2 -> {
            val firstLetter = words[0].firstOrNull() ?: ' '
            val secondLetter = words[1].firstOrNull() ?: ' '
            "$firstLetter $secondLetter".trim()
        }
        words.size == 1 -> {
            words[0].take(2)
        }
        else -> {
            cleaned.take(2)
        }
    }
}
