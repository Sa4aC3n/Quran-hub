package com.example.data.provider

import com.example.data.model.Ayah
import com.example.data.model.SurahText

/**
 * Authoritative Canonical Manifest of the Holy Quran (Hafs recitation).
 * Contains the immutable 114 surahs with exact canonical ayah counts, names, and revelation types.
 * Used to strictly validate downloaded or cached text against corruption or truncated payloads.
 */
object QuranManifest {

    const val TOTAL_SURAHS = 114
    const val TOTAL_AYAHS = 6236

    val CANONICAL_AYAH_COUNTS = intArrayOf(
        0,   // index 0 unused
        7, 286, 200, 176, 120, 165, 206, 75, 129, 109, // 1..10
        123, 111, 43, 52, 99, 128, 111, 110, 98, 135, // 11..20
        112, 78, 118, 64, 77, 227, 93, 88, 69, 60,    // 21..30
        34, 30, 73, 54, 45, 83, 182, 88, 75, 85,      // 31..40
        54, 53, 89, 59, 37, 35, 38, 29, 18, 45,       // 41..50
        60, 49, 62, 55, 78, 96, 29, 22, 24, 13,       // 51..60
        14, 11, 11, 18, 12, 12, 30, 52, 52, 44,       // 61..70
        28, 28, 20, 56, 40, 31, 50, 40, 46, 42,       // 71..80
        29, 19, 36, 25, 22, 17, 19, 26, 30, 20,       // 81..90
        15, 21, 11, 8, 8, 19, 5, 8, 8, 11,            // 91..100
        11, 8, 3, 9, 5, 4, 7, 3, 6, 3,                // 101..110
        5, 4, 5, 6                                    // 111..114
    )

    val SURAH_NAMES_ARABIC = arrayOf(
        "",
        "الفاتحة", "البقرة", "آل عمران", "النساء", "المائدة", "الأنعام", "الأعراف", "الأنفال", "التوبة", "يونس",
        "هود", "يوسف", "الرعد", "إبراهيم", "الحجر", "النحل", "الإسراء", "الكهف", "مريم", "طه",
        "الأنبياء", "الحج", "المؤمنون", "النور", "الفرقان", "الشعراء", "النمل", "القصص", "العنكبوت", "الروم",
        "لقمان", "السجدة", "الأحزاب", "سبأ", "فاطر", "يس", "الصافات", "ص", "الزمر", "غافر",
        "فصلت", "الشورى", "الزخرف", "الدخان", "الجاثية", "الأحقاف", "محمد", "الفتح", "الحجرات", "ق",
        "الذاريات", "الطور", "النجم", "القمر", "الرحمن", "الواقعة", "الحديد", "المجادلة", "الحشر", "الممتحنة",
        "الصف", "الجمعة", "المنافقون", "التغابن", "الطلاق", "التحريم", "الملك", "القلم", "الحاقة", "المعارج",
        "نوح", "الجن", "المزمل", "المدثر", "القيامة", "الإنسان", "المرسلات", "النبأ", "النازعات", "عبس",
        "التكوير", "الانفطار", "المطففين", "الانشقاق", "البروج", "الطارق", "الأعلى", "الغاشية", "الفجر", "البلد",
        "الشمس", "الليل", "الضحى", "الشرح", "التين", "العلق", "القدر", "البينة", "الزلزلة", "العاديات",
        "القارعة", "التكاثر", "العصر", "الهمزة", "الفيل", "قريش", "الماعون", "الكوثر", "الكافرون", "النصر",
        "المسد", "الإخلاص", "الفلق", "الناس"
    )

    fun getCanonicalAyahCount(surahNumber: Int): Int {
        return if (surahNumber in 1..TOTAL_SURAHS) CANONICAL_AYAH_COUNTS[surahNumber] else 0
    }

    fun getSurahNameArabic(surahNumber: Int): String {
        return if (surahNumber in 1..TOTAL_SURAHS) SURAH_NAMES_ARABIC[surahNumber] else "سورة $surahNumber"
    }

    /**
     * Validates whether a SurahText is complete, authentic, and non-corrupt.
     * Rejects placeholder generation, empty text, truncated verse lists, and broken sequencing.
     */
    fun validateSurah(surah: SurahText): ValidationResult {
        if (surah.number !in 1..TOTAL_SURAHS) {
            return ValidationResult.Invalid("رقم السورة ${surah.number} خارج النطاق الصحيح (1-114)")
        }
        val expectedAyahs = getCanonicalAyahCount(surah.number)
        if (surah.ayahs.size != expectedAyahs) {
            return ValidationResult.Invalid(
                "عدد آيات السورة (${surah.ayahs.size}) لا يطابق العدد المعتمد ($expectedAyahs)"
            )
        }
        if (surah.numberOfAyahs != expectedAyahs) {
            return ValidationResult.Invalid(
                "حقل numberOfAyahs (${surah.numberOfAyahs}) لا يطابق العدد المعتمد ($expectedAyahs)"
            )
        }

        // Verify strictly sequential order and non-empty authentic text
        for (i in surah.ayahs.indices) {
            val ayah = surah.ayahs[i]
            val expectedNumberInSurah = i + 1
            if (ayah.numberInSurah != expectedNumberInSurah) {
                return ValidationResult.Invalid(
                    "تسلسل الآيات غير متسق: متوقع $expectedNumberInSurah ووجد ${ayah.numberInSurah}"
                )
            }
            if (ayah.text.isBlank()) {
                return ValidationResult.Invalid("الآية $expectedNumberInSurah تحتوي على نص فارغ")
            }
            // Reject any legacy synthetic placeholder text
            if (ayah.text.contains("الآية الكريمة رقم") || ayah.text.contains("سورة") && ayah.text.contains("- الآية")) {
                return ValidationResult.Invalid("الآية $expectedNumberInSurah تحتوي على نص مؤقت مصطنع مرفوض")
            }
        }

        return ValidationResult.Valid
    }

    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }
}
