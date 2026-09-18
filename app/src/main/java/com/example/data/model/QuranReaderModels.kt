package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Ayah(
    @Json(name = "number") val number: Int = 1,
    @Json(name = "numberInSurah") val numberInSurah: Int = 1,
    @Json(name = "text") val text: String = "",
    @Json(name = "juz") val juz: Int = 1,
    @Json(name = "manzil") val manzil: Int = 1,
    @Json(name = "page") val page: Int = 1,
    @Json(name = "ruku") val ruku: Int = 1,
    @Json(name = "hizbQuarter") val hizbQuarter: Int = 1,
    @Json(name = "sajda") val sajda: Boolean = false,
    val tafsir: String? = null
)

@JsonClass(generateAdapter = true)
data class SurahText(
    @Json(name = "number") val number: Int = 1,
    @Json(name = "name") val name: String = "",
    @Json(name = "englishName") val englishName: String = "",
    @Json(name = "englishNameTranslation") val englishNameTranslation: String = "",
    @Json(name = "revelationType") val revelationType: String = "Meccan",
    @Json(name = "numberOfAyahs") val numberOfAyahs: Int = 7,
    @Json(name = "ayahs") val ayahs: List<Ayah> = emptyList()
)

data class QuranBookmark(
    val id: String = java.util.UUID.randomUUID().toString(),
    val surahNumber: Int,
    val surahName: String,
    val ayahNumber: Int,
    val pageNumber: Int = 1,
    val juzNumber: Int = 1,
    val ayahText: String = "",
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class ReadingProgress(
    val surahNumber: Int = 1,
    val surahName: String = "الفاتحة",
    val ayahNumber: Int = 1,
    val pageNumber: Int = 1,
    val juzNumber: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ReaderTheme(
    val id: String,
    val labelArabic: String,
    val bgHex: Long,
    val textHex: Long,
    val cardBgHex: Long,
    val accentHex: Long,
    val isDark: Boolean
) {
    WARM_PAPER("sepia", "ورق كلاسيكي", 0xFFFBF7EE, 0xFF2D241E, 0xFFF3ECE0, 0xFFB37D28, false),
    DARK_NIGHT("night", "الوضع الليلي", 0xFF121516, 0xFFE6ECEE, 0xFF1C2224, 0xFF38B58D, true),
    PURE_WHITE("white", "أبيض نقي", 0xFFFFFFFF, 0xFF1A1A1A, 0xFFF5F7F8, 0xFF0D6E5A, false),
    EMERALD_GREEN("emerald", "أخضر إسلامي", 0xFF0F1E19, 0xFFE0EFE8, 0xFF172B24, 0xFF51CF9D, true)
}

enum class QuranDisplayMode(val label: String) {
    AYAH_BY_AYAH("آية بآية مع التفسير"),
    MUSHAF_PAGE("صفحة المصحف المتصلة")
}
