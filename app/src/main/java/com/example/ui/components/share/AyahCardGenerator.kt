package com.example.ui.components.share

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.provider.QuranTextProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Visual styles for the Ayah Card
 */
enum class AyahCardTemplate(
    val titleAr: String,
    val primaryColorHex: Long,
    val accentColorHex: Long,
    val bgColors: List<Long>
) {
    EMERALD_GOLD(
        titleAr = "الأخضر والذهبي الملكي",
        primaryColorHex = 0xFFD4AF37,
        accentColorHex = 0xFFFFDF73,
        bgColors = listOf(0xFF042018, 0xFF0A3D2E, 0xFF021610)
    ),
    MIDNIGHT_NAVY(
        titleAr = "الليل والتهجد",
        primaryColorHex = 0xFFE2C974,
        accentColorHex = 0xFF90CAF9,
        bgColors = listOf(0xFF050C14, 0xFF0E1E32, 0xFF02070D)
    ),
    WARM_PARCHMENT(
        titleAr = "رق المصحف العتيق",
        primaryColorHex = 0xFF8C531B,
        accentColorHex = 0xFFC68A4C,
        bgColors = listOf(0xFFF9F5EC, 0xFFEFE8D8, 0xFFE5DCB8)
    ),
    GEOMETRIC_ARABESQUE(
        titleAr = "الزخرفة الإسلامية",
        primaryColorHex = 0xFFE5B869,
        accentColorHex = 0xFF26A69A,
        bgColors = listOf(0xFF0C2B33, 0xFF144754, 0xFF081C22)
    ),
    SOFT_DAWN(
        titleAr = "الفجر والسكينة",
        primaryColorHex = 0xFFFFD54F,
        accentColorHex = 0xFFCE93D8,
        bgColors = listOf(0xFF2C1654, 0xFF4A2574, 0xFF1F0E3D)
    )
}

/**
 * Aspect Ratios supported for social sharing
 */
enum class AyahCardAspectRatio(val titleAr: String, val width: Int, val height: Int, val isStory: Boolean) {
    SQUARE(titleAr = "مربع (1:1) • واتساب وفيسبوك", width = 1080, height = 1080, isStory = false),
    STORY(titleAr = "طولي (9:16) • ستوري وحالات", width = 1080, height = 1920, isStory = true)
}

/**
 * Data needed to render the card
 */
data class AyahCardData(
    val surahNumber: Int,
    val surahName: String,
    val ayahNumber: Int,
    val rawAyahText: String,
    val reciterName: String = "",
    val template: AyahCardTemplate = AyahCardTemplate.EMERALD_GOLD,
    val aspectRatio: AyahCardAspectRatio = AyahCardAspectRatio.SQUARE,
    val includeReciter: Boolean = true,
    val includeWatermark: Boolean = true,
    val watermarkText: String = "تطبيق إذاعات وتلاوات القرآن الكريم"
)

object AyahCardGenerator {

    /**
     * Generate high resolution bitmap on background thread
     */
    suspend fun generateCardBitmap(data: AyahCardData): Bitmap = withContext(Dispatchers.Default) {
        val width = data.aspectRatio.width
        val height = data.aspectRatio.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        renderCanvas(canvas, width, height, data)
        bitmap
    }

    private fun renderCanvas(canvas: Canvas, width: Int, height: Int, data: AyahCardData) {
        val isLightBg = data.template == AyahCardTemplate.WARM_PARCHMENT
        val goldColor = data.template.primaryColorHex.toInt()
        val accentColor = data.template.accentColorHex.toInt()
        val textPrimaryColor = if (isLightBg) 0xFF2A1C0E.toInt() else 0xFFFFFFFF.toInt()
        val textSecondaryColor = if (isLightBg) 0xFF6E5642.toInt() else 0xFFDFE2E6.toInt()

        // 1. Draw Background Gradient
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                data.template.bgColors.map { it.toInt() }.toIntArray(),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 1.1 Subtle Central Radial Glow
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                width / 2f, height / 2f,
                (width * 0.7f),
                if (isLightBg) 0x33FFFDF5.toInt() else 0x22D4AF37.toInt(),
                0x00000000,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), glowPaint)

        // 2. Outer & Inner Islamic Ornate Borders
        val safeMarginHoriz = if (data.aspectRatio.isStory) 70f else 60f
        val safeMarginVert = if (data.aspectRatio.isStory) 180f else 60f // Leave top/bottom room for IG story safe zones

        val outerRect = RectF(
            safeMarginHoriz,
            safeMarginVert,
            width - safeMarginHoriz,
            height - safeMarginVert
        )

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = goldColor
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRoundRect(outerRect, 24f, 24f, borderPaint)

        val innerRect = RectF(
            outerRect.left + 16f,
            outerRect.top + 16f,
            outerRect.right - 16f,
            outerRect.bottom - 16f
        )
        val innerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = goldColor
            alpha = 140
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRoundRect(innerRect, 16f, 16f, innerBorderPaint)

        // 3. Draw Corner Islamic Floral Ornaments
        drawCornerOrnaments(canvas, innerRect, goldColor)

        // 4. Header Ornament & Basmala
        val headerY = innerRect.top + (if (data.aspectRatio.isStory) 90f else 60f)
        drawIslamicHeader(
            canvas = canvas,
            centerX = width / 2f,
            centerY = headerY,
            goldColor = goldColor,
            accentColor = accentColor,
            isLightBg = isLightBg,
            surahNumber = data.surahNumber,
            ayahNumber = data.ayahNumber
        )

        // 5. Footer Information (Reciter + Watermark)
        val footerBaseY = innerRect.bottom - (if (data.aspectRatio.isStory) 80f else 48f)

        if (data.includeWatermark) {
            val wmPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isLightBg) 0xFF8A7460.toInt() else 0xCCFFFFFF.toInt()
                textSize = 25f
                typeface = Typeface.DEFAULT
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("✦ ${data.watermarkText} ✦", width / 2f, footerBaseY, wmPaint)
        }

        if (data.includeReciter && data.reciterName.isNotBlank()) {
            val reciterY = if (data.includeWatermark) footerBaseY - 44f else footerBaseY
            val reciterPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = goldColor
                textSize = 29f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("بصوت القارئ: ${data.reciterName}", width / 2f, reciterY, reciterPaint)
        }

        // 6. Middle Quranic Text Area & Closing Reference Line
        val topContentLimit = headerY + (if (data.aspectRatio.isStory) 55f else 45f)
        val bottomContentLimit = when {
            data.includeReciter && data.reciterName.isNotBlank() -> footerBaseY - 85f
            data.includeWatermark -> footerBaseY - 55f
            else -> footerBaseY - 30f
        }
        val availableContentHeight = (bottomContentLimit - topContentLimit).coerceAtLeast(150f)
        val availableContentWidth = (innerRect.width() - (if (data.aspectRatio.isStory) 80f else 70f)).toInt()

        // Clean text and strip any duplicate brackets or braces
        val cleanAyah = QuranTextProvider.cleanBismillahFromVerse(
            data.rawAyahText,
            data.surahNumber,
            data.ayahNumber
        ).trim()
        val strippedAyah = cleanAyah
            .removePrefix("﴿").removeSuffix("﴾")
            .removePrefix("{").removeSuffix("}")
            .removePrefix("«").removeSuffix("»")
            .trim()
        val fullVerseText = "﴿ $strippedAyah ﴾"

        val pillHeight = 46f
        val pillSpacing = 32f
        val maxAyahHeight = (availableContentHeight - pillHeight - pillSpacing - 10f).coerceAtLeast(80f)

        // Dynamic Text Sizing: Adaptively calculate font size so long ayahs fit comfortably
        val optimalTextSize = calculateOptimalFontSize(
            text = fullVerseText,
            targetWidth = availableContentWidth,
            maxHeight = maxAyahHeight,
            minSize = 26f,
            maxSize = if (data.aspectRatio.isStory) 60f else 54f
        )

        val versePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPrimaryColor
            textSize = optimalTextSize
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.LEFT // CRITICAL: StaticLayout handles ALIGN_CENTER; paint must be ALIGN_LEFT
        }

        val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(
                fullVerseText,
                0,
                fullVerseText.length,
                versePaint,
                availableContentWidth
            )
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(20f, 1.35f)
                .setIncludePad(false)
                .setBreakStrategy(Layout.BREAK_STRATEGY_BALANCED)
                .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(
                fullVerseText,
                versePaint,
                availableContentWidth,
                Layout.Alignment.ALIGN_CENTER,
                1.35f,
                20f,
                false
            )
        }

        // Calculate combined height of Ayah text + final reference line (اسم السورة ورقم الآية)
        val textTotalHeight = staticLayout.height
        val totalBlockHeight = textTotalHeight + pillSpacing + pillHeight

        // Perfectly center the combined block in the middle vertical area
        val startY = topContentLimit + ((availableContentHeight - totalBlockHeight) / 2f).coerceAtLeast(0f)

        // Draw Ayah text (centered horizontally and vertically)
        canvas.save()
        canvas.translate((width - availableContentWidth) / 2f, startY)
        staticLayout.draw(canvas)
        canvas.restore()

        // Draw Final Line: Mention Surah Name and Ayah Number (ذكر اسم السورة ورقم الآية في سطر أخير)
        val pillCenterY = startY + textTotalHeight + pillSpacing + (pillHeight / 2f)
        val surahBadgeText = "سورة ${data.surahName} • الآية ${data.ayahNumber}"
        drawBadgePill(canvas, width / 2f, pillCenterY, surahBadgeText, goldColor, isLightBg)
    }

    private fun calculateOptimalFontSize(
        text: String,
        targetWidth: Int,
        maxHeight: Float,
        minSize: Float = 26f,
        maxSize: Float = 54f
    ): Float {
        var low = minSize
        var high = maxSize
        var bestSize = minSize

        val tf = Typeface.create(Typeface.SERIF, Typeface.BOLD)

        while (high - low > 1f) {
            val mid = (low + high) / 2f
            val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = mid
                typeface = tf
                textAlign = Paint.Align.LEFT
            }
            val sl = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StaticLayout.Builder.obtain(text, 0, text.length, tp, targetWidth)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setLineSpacing(20f, 1.35f)
                    .setIncludePad(false)
                    .setBreakStrategy(Layout.BREAK_STRATEGY_BALANCED)
                    .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                StaticLayout(text, tp, targetWidth, Layout.Alignment.ALIGN_CENTER, 1.35f, 20f, false)
            }

            if (sl.height <= maxHeight) {
                bestSize = mid
                low = mid
            } else {
                high = mid
            }
        }
        return bestSize
    }

    private fun drawCornerOrnaments(canvas: Canvas, rect: RectF, color: Int) {
        val ornamentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        val size = 28f
        // Top Left
        canvas.drawLine(rect.left + 8f, rect.top + 8f, rect.left + 8f + size, rect.top + 8f, ornamentPaint)
        canvas.drawLine(rect.left + 8f, rect.top + 8f, rect.left + 8f, rect.top + 8f + size, ornamentPaint)
        canvas.drawCircle(rect.left + 8f + size / 2, rect.top + 8f + size / 2, 4f, ornamentPaint)

        // Top Right
        canvas.drawLine(rect.right - 8f, rect.top + 8f, rect.right - 8f - size, rect.top + 8f, ornamentPaint)
        canvas.drawLine(rect.right - 8f, rect.top + 8f, rect.right - 8f, rect.top + 8f + size, ornamentPaint)
        canvas.drawCircle(rect.right - 8f - size / 2, rect.top + 8f + size / 2, 4f, ornamentPaint)

        // Bottom Left
        canvas.drawLine(rect.left + 8f, rect.bottom - 8f, rect.left + 8f + size, rect.bottom - 8f, ornamentPaint)
        canvas.drawLine(rect.left + 8f, rect.bottom - 8f, rect.left + 8f, rect.bottom - 8f - size, ornamentPaint)
        canvas.drawCircle(rect.left + 8f + size / 2, rect.bottom - 8f - size / 2, 4f, ornamentPaint)

        // Bottom Right
        canvas.drawLine(rect.right - 8f, rect.bottom - 8f, rect.right - 8f - size, rect.bottom - 8f, ornamentPaint)
        canvas.drawLine(rect.right - 8f, rect.bottom - 8f, rect.right - 8f, rect.bottom - 8f - size, ornamentPaint)
        canvas.drawCircle(rect.right - 8f - size / 2, rect.bottom - 8f - size / 2, 4f, ornamentPaint)
    }

    private fun drawIslamicHeader(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        goldColor: Int,
        accentColor: Int,
        isLightBg: Boolean,
        surahNumber: Int = 1,
        ayahNumber: Int = 1
    ) {
        val basmalaText = if (surahNumber == 9) {
            "أَعُوذُ بِٱللَّهِ مِنَ ٱلشَّيْطَانِ ٱلرَّجِيمِ"
        } else if (surahNumber == 1 && ayahNumber == 1) {
            "✦  آية كريمة  ✦"
        } else {
            "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
        }

        val basmalaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = goldColor
            textSize = 33f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val textBaseline = centerY - (basmalaPaint.descent() + basmalaPaint.ascent()) / 2f
        canvas.drawText(basmalaText, centerX, textBaseline, basmalaPaint)

        // Decorative Wings
        val wingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = goldColor
            alpha = 180
            strokeWidth = 2f
        }
        val textWidth = basmalaPaint.measureText(basmalaText)
        val wingStartLeft = centerX - (textWidth / 2f) - 22f
        val wingStartRight = centerX + (textWidth / 2f) + 22f

        canvas.drawLine(wingStartLeft, centerY, wingStartLeft - 60f, centerY, wingPaint)
        canvas.drawCircle(wingStartLeft - 66f, centerY, 3.5f, wingPaint)

        canvas.drawLine(wingStartRight, centerY, wingStartRight + 60f, centerY, wingPaint)
        canvas.drawCircle(wingStartRight + 66f, centerY, 3.5f, wingPaint)
    }

    private fun drawBadgePill(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        text: String,
        goldColor: Int,
        isLightBg: Boolean
    ) {
        val badgePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLightBg) 0xFF4A341E.toInt() else 0xFFFFF8E7.toInt()
            textSize = 27f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val textWidth = badgePaint.measureText(text)
        val pillWidth = textWidth + 64f
        val pillHeight = 46f
        val pillRect = RectF(
            centerX - (pillWidth / 2f),
            centerY - (pillHeight / 2f),
            centerX + (pillWidth / 2f),
            centerY + (pillHeight / 2f)
        )

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = goldColor
            alpha = if (isLightBg) 40 else 55
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(pillRect, 23f, 23f, fillPaint)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = goldColor
            alpha = if (isLightBg) 180 else 220
            style = Paint.Style.STROKE
            strokeWidth = 1.8f
        }
        canvas.drawRoundRect(pillRect, 23f, 23f, strokePaint)

        // Subtle diamond accents on both sides of the pill
        val diamondPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = goldColor
            alpha = if (isLightBg) 200 else 240
            style = Paint.Style.FILL
        }
        drawSmallDiamond(canvas, pillRect.left + 16f, centerY, 4f, diamondPaint)
        drawSmallDiamond(canvas, pillRect.right - 16f, centerY, 4f, diamondPaint)

        val textBaseline = centerY - (badgePaint.descent() + badgePaint.ascent()) / 2f
        canvas.drawText(text, centerX, textBaseline, badgePaint)
    }

    private fun drawSmallDiamond(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
        val path = Path().apply {
            moveTo(cx, cy - size)
            lineTo(cx + size, cy)
            lineTo(cx, cy + size)
            lineTo(cx - size, cy)
            close()
        }
        canvas.drawPath(path, paint)
    }

    /**
     * Saves the image to disk and launches the native Android Share Sheet (Intent.ACTION_SEND)
     */
    suspend fun shareAyahCardImage(
        context: Context,
        bitmap: Bitmap,
        caption: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val imagesFolder = File(context.cacheDir, "images")
            if (!imagesFolder.exists()) imagesFolder.mkdirs()

            val file = File(imagesFolder, "ayah_moment_${System.currentTimeMillis()}.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, caption)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooserIntent = Intent.createChooser(shareIntent, "مشاركة بطاقة الآية الكريمة").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooserIntent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "تعذر مشاركة الصورة: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
            false
        }
    }

    /**
     * Saves the bitmap to the device MediaStore / Pictures directory
     */
    suspend fun saveCardToGallery(
        context: Context,
        bitmap: Bitmap,
        surahName: String,
        ayahNumber: Int
    ): Boolean = withContext(Dispatchers.IO) {
        val fileName = "Quran_${surahName}_Ayah_${ayahNumber}_${System.currentTimeMillis()}.png"
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/QuranRadio")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    val stream = resolver.openOutputStream(uri)
                    if (stream != null) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                        stream.close()
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "تم حفظ بطاقة الآية في معرض الصور بنجاح 🖼️", Toast.LENGTH_SHORT).show()
                    }
                    return@withContext true
                }
            } else {
                @Suppress("DEPRECATION")
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val appDir = File(picturesDir, "QuranRadio")
                if (!appDir.exists()) appDir.mkdirs()
                val imageFile = File(appDir, fileName)
                val out: OutputStream = FileOutputStream(imageFile)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
                out.close()

                @Suppress("DEPRECATION")
                MediaStore.Images.Media.insertImage(
                    context.contentResolver,
                    imageFile.absolutePath,
                    fileName,
                    "Quran Ayah Card"
                )

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "تم حفظ بطاقة الآية في معرض الصور 🖼️", Toast.LENGTH_SHORT).show()
                }
                return@withContext true
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "فشل حفظ الصورة: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
            false
        }
    }
}
