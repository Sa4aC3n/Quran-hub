package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Gold400
import com.example.ui.theme.Gold600

@Composable
fun SurahNumberBadge(
    number: Int,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    isSelected: Boolean = false
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Outer rotated square to form an 8-pointed star effect
        Box(
            modifier = Modifier
                .size(size * 0.78f)
                .rotate(45f)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                )
                .border(
                    1.dp,
                    if (isSelected) Gold400 else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(4.dp)
                )
        )
        // Inner square
        Box(
            modifier = Modifier
                .size(size * 0.78f)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                )
                .border(
                    1.dp,
                    if (isSelected) Gold400 else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(4.dp)
                )
        )

        Text(
            text = "$number",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = if (number >= 100) 11.sp else 13.sp
            ),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun IslamicGradientCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val brush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.surface
        )
    )
    Box(
        modifier = modifier
            .background(brush, shape = RoundedCornerShape(16.dp))
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(Gold600.copy(alpha = 0.3f), Color.Transparent)),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        content()
    }
}
