package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Gold200
import com.example.ui.theme.Gold400
import com.example.ui.theme.Gold500
import com.example.ui.theme.Gold600
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Islamic Geometric Background Pattern (خلفية زخارف إسلامية هندسية دقيقة).
 * Uses hardware-accelerated drawWithCache to construct the Girih lattice
 * and 8-point stars (Khatam / Rub el Hizb) once per layout size, ensuring
 * zero GC allocations during scrolling, 120 FPS performance, and an exquisite
 * royal manuscript aesthetic across all screens.
 */
@Composable
fun IslamicGeometricBackground(
    modifier: Modifier = Modifier,
    patternColor: Color? = null,
    spacing: Dp = 52.dp,
    showCornerAccents: Boolean = true
) {
    val isDark = isSystemInDarkTheme()
    val resolvedColor = patternColor ?: if (isDark) {
        Gold400.copy(alpha = 0.048f)
    } else {
        Gold600.copy(alpha = 0.058f)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val step = spacing.toPx()
                val starRadius = step * 0.32f
                val innerRadius = starRadius * 0.52f
                val strokeWidth = 0.85.dp.toPx()

                // Pre-build the entire geometric lattice into one single Path once
                val latticePath = Path()

                var x = 0f
                while (x < size.width + step) {
                    var y = 0f
                    var rowIndex = 0
                    while (y < size.height + step) {
                        val offsetX = if (rowIndex % 2 == 1) step / 2f else 0f
                        val cx = x + offsetX
                        val cy = y

                        // 8-Point Islamic Star (Khatam)
                        for (i in 0 until 16) {
                            val r = if (i % 2 == 0) starRadius else innerRadius
                            val angle = (i * PI / 8.0).toFloat() - (PI / 2.0).toFloat()
                            val px = cx + r * cos(angle)
                            val py = cy + r * sin(angle)
                            if (i == 0) {
                                latticePath.moveTo(px, py)
                            } else {
                                latticePath.lineTo(px, py)
                            }
                        }
                        latticePath.close()

                        // Interlaced Girih straps connecting stars
                        // Horizontal straps
                        latticePath.moveTo(cx - step / 2f, cy)
                        latticePath.lineTo(cx - starRadius, cy)
                        latticePath.moveTo(cx + starRadius, cy)
                        latticePath.lineTo(cx + step / 2f, cy)

                        // Vertical straps
                        latticePath.moveTo(cx, cy - step * 0.433f)
                        latticePath.lineTo(cx, cy - starRadius)
                        latticePath.moveTo(cx, cy + starRadius)
                        latticePath.lineTo(cx, cy + step * 0.433f)

                        // Diagonal decorative interlock accents
                        val diagOffset = step * 0.22f
                        latticePath.moveTo(cx - diagOffset, cy - diagOffset)
                        latticePath.lineTo(cx + diagOffset, cy + diagOffset)
                        latticePath.moveTo(cx + diagOffset, cy - diagOffset)
                        latticePath.lineTo(cx - diagOffset, cy + diagOffset)

                        y += step * 0.866f
                        rowIndex++
                    }
                    x += step
                }

                val strokeStyle = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )

                onDrawBehind {
                    // Single instant GPU draw call for the entire screen background
                    drawPath(
                        path = latticePath,
                        color = resolvedColor,
                        style = strokeStyle
                    )
                }
            }
    ) {
        if (showCornerAccents) {
            IslamicGeometricCornerDecorations(
                modifier = Modifier.matchParentSize(),
                color = resolvedColor.copy(alpha = (resolvedColor.alpha * 2.8f).coerceAtMost(0.22f)),
                cornerLength = 18.dp,
                margin = 6.dp
            )
        }
    }
}

/**
 * Helper to draw a single 8-pointed star in Canvas (for badges, dividers, medallions).
 */
fun DrawScope.drawIslamicEightStar(
    center: Offset,
    radius: Float,
    color: Color,
    strokeWidth: Float
) {
    val path = Path()
    val points = 8
    val innerRadius = radius * 0.54f

    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) radius else innerRadius
        val angle = (i * PI / points).toFloat() - (PI / 2f).toFloat()
        val px = center.x + r * cos(angle)
        val py = center.y + r * sin(angle)

        if (i == 0) {
            path.moveTo(px, py)
        } else {
            path.lineTo(px, py)
        }
    }
    path.close()

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

/**
 * Delicate Islamic Corner Ornaments with Interlocking Geometric Knots.
 * Cached with drawWithCache for zero allocations and maximum fluidity.
 */
@Composable
fun IslamicGeometricCornerDecorations(
    modifier: Modifier = Modifier,
    color: Color = Gold400.copy(alpha = 0.35f),
    cornerLength: Dp = 18.dp,
    margin: Dp = 6.dp,
    strokeWidth: Dp = 1.dp
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val sWidth = strokeWidth.toPx()
                val cLen = cornerLength.toPx()
                val m = margin.toPx()
                val innerLen = cLen * 0.52f
                val gap = 3.dp.toPx()

                val cornersPath = Path()

                // Top-Right Corner (RTL Primary)
                cornersPath.moveTo(size.width - m, m + cLen)
                cornersPath.lineTo(size.width - m, m)
                cornersPath.lineTo(size.width - m - cLen, m)

                cornersPath.moveTo(size.width - m - gap, m + innerLen)
                cornersPath.lineTo(size.width - m - gap, m + gap)
                cornersPath.lineTo(size.width - m - innerLen, m + gap)

                // Top-Left Corner
                cornersPath.moveTo(m + cLen, m)
                cornersPath.lineTo(m, m)
                cornersPath.lineTo(m, m + cLen)

                cornersPath.moveTo(m + innerLen, m + gap)
                cornersPath.lineTo(m + gap, m + gap)
                cornersPath.lineTo(m + gap, m + innerLen)

                // Bottom-Right Corner
                cornersPath.moveTo(size.width - m, size.height - m - cLen)
                cornersPath.lineTo(size.width - m, size.height - m)
                cornersPath.lineTo(size.width - m - cLen, size.height - m)

                cornersPath.moveTo(size.width - m - gap, size.height - m - innerLen)
                cornersPath.lineTo(size.width - m - gap, size.height - m - gap)
                cornersPath.lineTo(size.width - m - innerLen, size.height - m - gap)

                // Bottom-Left Corner
                cornersPath.moveTo(m + cLen, size.height - m)
                cornersPath.lineTo(m, size.height - m)
                cornersPath.lineTo(m, size.height - m - cLen)

                cornersPath.moveTo(m + innerLen, size.height - m - gap)
                cornersPath.lineTo(m + gap, size.height - m - gap)
                cornersPath.lineTo(m + gap, size.height - m - innerLen)

                val stroke = Stroke(width = sWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

                onDrawBehind {
                    drawPath(cornersPath, color = color, style = stroke)
                }
            }
    )
}

/**
 * Islamic Ornamental Divider / Separator (فاصل إسلامي هندسي أنيق).
 * Features a miniature 8-pointed star medallion with tapered geometric gold wings.
 */
@Composable
fun IslamicOrnamentalDivider(
    modifier: Modifier = Modifier,
    color: Color = Gold400,
    starSize: Dp = 12.dp,
    alpha: Float = 0.5f
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Right wing tapering line
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            color.copy(alpha = alpha)
                        )
                    )
                )
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Central Mini 8-Point Star
        Canvas(modifier = Modifier.size(starSize)) {
            drawIslamicEightStar(
                center = Offset(size.width / 2f, size.height / 2f),
                radius = size.width / 2f,
                color = color.copy(alpha = alpha + 0.25f),
                strokeWidth = 1.dp.toPx()
            )
            drawCircle(
                color = color.copy(alpha = alpha + 0.4f),
                radius = 1.5.dp.toPx(),
                center = Offset(size.width / 2f, size.height / 2f)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Left wing tapering line
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            color.copy(alpha = alpha),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

/**
 * Islamic Ornamental Card (بطاقة إسلامية مذهبة بزخارف هندسية).
 * Encapsulates content with subtle background patterns and fine gold framing.
 */
@Composable
fun IslamicOrnamentCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(18.dp),
    backgroundGradient: Brush = Brush.verticalGradient(
        listOf(
            Color(0xFF09291D),
            Color(0xFF061E15)
        )
    ),
    borderStroke: Color = Gold500.copy(alpha = 0.35f),
    elevation: Dp = 2.dp,
    showCornerDecorations: Boolean = true,
    showBackgroundPattern: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier
            .clip(shape)
            .clickable { onClick() }
    } else {
        modifier.clip(shape)
    }

    Card(
        modifier = cardModifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundGradient)
                .border(1.dp, borderStroke, shape)
        ) {
            // Subtle Islamic Pattern Watermark
            if (showBackgroundPattern) {
                IslamicGeometricBackground(
                    modifier = Modifier.matchParentSize(),
                    patternColor = Gold400.copy(alpha = 0.028f),
                    spacing = 38.dp
                )
            }

            // Fine Corner Ornaments
            if (showCornerDecorations) {
                IslamicGeometricCornerDecorations(
                    modifier = Modifier.matchParentSize(),
                    color = Gold400.copy(alpha = 0.28f),
                    cornerLength = 14.dp,
                    margin = 5.dp
                )
            }

            // Actual Card Content
            content()
        }
    }
}

/**
 * Islamic Styled Action Button (زر إسلامي حديث بحواف مذهبة وزخارف دقيقة).
 */
@Composable
fun IslamicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    isPrimaryGold: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
) {
    val containerBrush = if (isPrimaryGold) {
        Brush.horizontalGradient(
            listOf(Gold200, Gold400, Gold600)
        )
    } else {
        Brush.horizontalGradient(
            listOf(Color(0xFF0F4432), Color(0xFF08271C))
        )
    }

    val contentColor = if (isPrimaryGold) Color(0xFF141208) else Color(0xFFF7E2B5)
    val borderColor = if (isPrimaryGold) Gold600.copy(alpha = 0.6f) else Gold400.copy(alpha = 0.45f)

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(containerBrush)
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Text(
                    text = text,
                    color = contentColor,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )

                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    trailingIcon()
                }
            }
        }
    }
}

/**
 * Centered Islamic 8-Point Star Medallion (نجمة ثمانية إسلامية زخرفية).
 */
@Composable
fun IslamicStarMedallion(
    modifier: Modifier = Modifier,
    color: Color = Gold400
) {
    Canvas(modifier = modifier) {
        drawIslamicEightStar(
            center = Offset(size.width / 2f, size.height / 2f),
            radius = size.width / 2f,
            color = color,
            strokeWidth = 1.25.dp.toPx()
        )
        drawCircle(
            color = color,
            radius = 2.dp.toPx(),
            center = Offset(size.width / 2f, size.height / 2f)
        )
    }
}
