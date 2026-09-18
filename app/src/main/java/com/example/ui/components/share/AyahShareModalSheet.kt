package com.example.ui.components.share

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.PhotoSizeSelectActual
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Ayah
import com.example.data.model.SurahText
import com.example.data.provider.QuranTextProvider
import com.example.ui.theme.Gold400
import com.example.ui.theme.Gold500
import com.example.ui.theme.Gold600
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AyahShareModalSheet(
    surahNumber: Int,
    surahName: String,
    initialAyahNumber: Int,
    surahText: SurahText?,
    reciterName: String,
    onDismiss: () -> Unit,
    onNavigateAyah: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedAyahNumber by remember(initialAyahNumber) {
        mutableStateOf(initialAyahNumber.coerceAtLeast(1))
    }
    var selectedTemplate by remember { mutableStateOf(AyahCardTemplate.EMERALD_GOLD) }
    var selectedAspectRatio by remember { mutableStateOf(AyahCardAspectRatio.SQUARE) }
    var includeReciter by remember { mutableStateOf(true) }
    var includeWatermark by remember { mutableStateOf(true) }

    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGenerating by remember { mutableStateOf(false) }
    var isSharingAction by remember { mutableStateOf(false) }

    // Resolve active Ayah text
    val totalAyahs = surahText?.numberOfAyahs ?: 286
    val currentAyahObj: Ayah? = surahText?.ayahs?.find { it.numberInSurah == selectedAyahNumber }
        ?: surahText?.ayahs?.getOrNull(selectedAyahNumber - 1)

    val currentRawText = currentAyahObj?.text
        ?: (if (surahNumber == 1 && selectedAyahNumber == 1) "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
            else "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ")

    val cleanAyahText = QuranTextProvider.cleanBismillahFromVerse(
        currentRawText,
        surahNumber,
        selectedAyahNumber
    ).trim()

    // Regenerate bitmap whenever configuration changes
    LaunchedEffect(
        surahNumber,
        surahName,
        selectedAyahNumber,
        cleanAyahText,
        reciterName,
        selectedTemplate,
        selectedAspectRatio,
        includeReciter,
        includeWatermark
    ) {
        isGenerating = true
        val cardData = AyahCardData(
            surahNumber = surahNumber,
            surahName = surahName,
            ayahNumber = selectedAyahNumber,
            rawAyahText = cleanAyahText,
            reciterName = reciterName,
            template = selectedTemplate,
            aspectRatio = selectedAspectRatio,
            includeReciter = includeReciter,
            includeWatermark = includeWatermark
        )
        val bmp = AyahCardGenerator.generateCardBitmap(cardData)
        generatedBitmap = bmp
        isGenerating = false
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("btn_close_share_sheet")) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "مشاركة اللحظة القرآنية 🌟",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "بطاقة مصممة للنشر الفوري على وسائل التواصل",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText(
                            "Quran Ayah",
                            "﴿ $cleanAyahText ﴾\n[سورة $surahName - الآية $selectedAyahNumber]\nبصوت القارئ: $reciterName\n✦ تطبيق إذاعات وتلاوات القرآن الكريم ✦"
                        )
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "تم نسخ نص الآية للذاكرة 📋", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("btn_copy_ayah_text")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "نسخ نص الآية",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Ayah Quick Selector Row (Prev / Current / Next)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (selectedAyahNumber > 1) {
                                selectedAyahNumber--
                                onNavigateAyah?.invoke(selectedAyahNumber)
                            }
                        },
                        enabled = selectedAyahNumber > 1,
                        modifier = Modifier.size(36.dp).testTag("btn_prev_ayah_share")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NavigateNext, // In RTL Next points right/previous
                            contentDescription = "الآية السابقة",
                            tint = if (selectedAyahNumber > 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "سورة $surahName",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "الآية $selectedAyahNumber من $totalAyahs",
                            style = MaterialTheme.typography.labelSmall,
                            color = Gold600
                        )
                    }

                    IconButton(
                        onClick = {
                            if (selectedAyahNumber < totalAyahs) {
                                selectedAyahNumber++
                                onNavigateAyah?.invoke(selectedAyahNumber)
                            }
                        },
                        enabled = selectedAyahNumber < totalAyahs,
                        modifier = Modifier.size(36.dp).testTag("btn_next_ayah_share")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NavigateBefore, // In RTL Before points left/next
                            contentDescription = "الآية التالية",
                            tint = if (selectedAyahNumber < totalAyahs) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Card Live Preview Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (selectedAspectRatio == AyahCardAspectRatio.STORY) 340.dp else 260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .border(1.5.dp, Gold500.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isGenerating && generatedBitmap == null) {
                    CircularProgressIndicator(
                        color = Gold500,
                        modifier = Modifier.size(36.dp)
                    )
                } else if (generatedBitmap != null) {
                    Image(
                        bitmap = generatedBitmap!!.asImageBitmap(),
                        contentDescription = "معاينة بطاقة الآية",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Aspect Ratio Selector (Square 1:1 vs Story 9:16)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AyahCardAspectRatio.values().forEach { aspect ->
                    val isSelected = selectedAspectRatio == aspect
                    Surface(
                        onClick = { selectedAspectRatio = aspect },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoSizeSelectActual,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (aspect == AyahCardAspectRatio.SQUARE) "مربع (1:1)" else "ستوري (9:16)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Template Theme Options
            Text(
                text = "اختر نمط التصميم والخلفية:",
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
                items(AyahCardTemplate.values()) { template ->
                    val isSelected = selectedTemplate == template
                    Surface(
                        onClick = { selectedTemplate = template },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(template.bgColors.first()),
                        border = BorderStroke(
                            if (isSelected) 2.5.dp else 1.dp,
                            if (isSelected) Color(template.primaryColorHex) else Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.shadow(if (isSelected) 4.dp else 0.dp, RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(template.primaryColorHex),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = template.titleAr,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (template == AyahCardTemplate.WARM_PARCHMENT) Color(0xFF2A1C0E) else Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Customization Switches (Reciter Name & App Watermark)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = includeReciter,
                        onCheckedChange = { includeReciter = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Gold500,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إظهار اسم القارئ", style = MaterialTheme.typography.bodySmall)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = includeWatermark,
                        onCheckedChange = { includeWatermark = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Gold500,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("شعار التطبيق", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bottom Action Buttons: Share & Save To Gallery
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Save to Gallery Button
                OutlinedButton(
                    onClick = {
                        val bmp = generatedBitmap
                        if (bmp != null) {
                            scope.launch {
                                AyahCardGenerator.saveCardToGallery(
                                    context = context,
                                    bitmap = bmp,
                                    surahName = surahName,
                                    ayahNumber = selectedAyahNumber
                                )
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).height(50.dp).testTag("btn_save_card_to_gallery"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حفظ بالمعرض", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                // Primary Share Image Button
                Button(
                    onClick = {
                        val bmp = generatedBitmap
                        if (bmp != null && !isSharingAction) {
                            isSharingAction = true
                            scope.launch {
                                val playStoreUrl = "https://play.google.com/store/apps/details?id=com.aistudio.quranaudio.mskdra"
                                val caption = "﴿ $cleanAyahText ﴾\n[سورة $surahName: $selectedAyahNumber] - بصوت القارئ: $reciterName\n\n✦ تم الإنشاء والمشاركة عبر تطبيق القرآن الكريم ✦\n$playStoreUrl"
                                AyahCardGenerator.shareAyahCardImage(
                                    context = context,
                                    bitmap = bmp,
                                    caption = caption
                                )
                                isSharingAction = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1.3f).height(50.dp).testTag("btn_share_card_direct"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    if (isSharingAction) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "مشاركة البطاقة 🔗",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
