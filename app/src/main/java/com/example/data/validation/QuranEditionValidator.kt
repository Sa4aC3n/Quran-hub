package com.example.data.validation

import com.example.data.model.AudioQualityLevel
import com.example.data.model.ReviewItem
import java.util.Locale

object QuranEditionValidator {

    data class SurahMeta(
        val number: Int,
        val arabicName: String,
        val englishName: String,
        val aliases: List<String>
    )

    val SURAHS_LIST: List<SurahMeta> = listOf(
        SurahMeta(1, "الفاتحة", "Al-Fatiha", listOf("الفاتحة", "فاتحة", "fatiha", "fatihah")),
        SurahMeta(2, "البقرة", "Al-Baqarah", listOf("البقرة", "بقرة", "baqarah", "baqara", "baqra")),
        SurahMeta(3, "آل عمران", "Aal-Imran", listOf("آل عمران", "ال عمران", "عمران", "imran", "ali-imran")),
        SurahMeta(4, "النساء", "An-Nisa", listOf("النساء", "نساء", "nisa", "nisaa")),
        SurahMeta(5, "المائدة", "Al-Ma'idah", listOf("المائدة", "مائدة", "maidah", "maida")),
        SurahMeta(6, "الأنعام", "Al-An'am", listOf("الأنعام", "الانعام", "انعام", "anam", "an'am")),
        SurahMeta(7, "الأعراف", "Al-A'raf", listOf("الأعراف", "الاعراف", "اعراف", "araf", "a'raf")),
        SurahMeta(8, "الأنفال", "Al-Anfal", listOf("الأنفال", "الانفال", "انفال", "anfal")),
        SurahMeta(9, "التوبة", "At-Tawbah", listOf("التوبة", "توبة", "tawbah", "tawba", "bara'ah", "براءة")),
        SurahMeta(10, "يونس", "Yunus", listOf("يونس", "yunus", "younus", "younes")),
        SurahMeta(11, "هود", "Hud", listOf("هود", "hud", "houd")),
        SurahMeta(12, "يوسف", "Yusuf", listOf("يوسف", "yusuf", "youssef", "yousuf")),
        SurahMeta(13, "الرعد", "Ar-Ra'd", listOf("الرعد", "رعد", "rad", "ra'd")),
        SurahMeta(14, "إبراهيم", "Ibrahim", listOf("إبراهيم", "ابراهيم", "ibrahim")),
        SurahMeta(15, "الحجر", "Al-Hijr", listOf("الحجر", "حجر", "hijr")),
        SurahMeta(16, "النحل", "An-Nahl", listOf("النحل", "نحل", "nahl")),
        SurahMeta(17, "الإسراء", "Al-Isra", listOf("الإسراء", "الاسراء", "إسراء", "اسراء", "isra", "isra'")),
        SurahMeta(18, "الكهف", "Al-Kahf", listOf("الكهف", "كهف", "kahf", "kahaf")),
        SurahMeta(19, "مريم", "Maryam", listOf("مريم", "maryam", "mariam")),
        SurahMeta(20, "طه", "Ta-Ha", listOf("طه", "taha", "ta-ha")),
        SurahMeta(21, "الأنبياء", "Al-Anbiya", listOf("الأنبياء", "الانبياء", "انبياء", "anbiya", "anbiyaa")),
        SurahMeta(22, "الحج", "Al-Hajj", listOf("الحج", "حج", "hajj", "haj")),
        SurahMeta(23, "المؤمنون", "Al-Mu'minun", listOf("المؤمنون", "المؤمنين", "مؤمنون", "muminun", "mu'minun")),
        SurahMeta(24, "النور", "An-Nur", listOf("النور", "نور", "nur", "noor")),
        SurahMeta(25, "الفرقان", "Al-Furqan", listOf("الفرقان", "فرقان", "furqan")),
        SurahMeta(26, "الشعراء", "Ash-Shu'ara", listOf("الشعراء", "شعراء", "shuara", "shu'ara")),
        SurahMeta(27, "النمل", "An-Naml", listOf("النمل", "نمل", "naml")),
        SurahMeta(28, "القصص", "Al-Qasas", listOf("القصص", "قصص", "qasas")),
        SurahMeta(29, "العنكبوت", "Al-Ankabut", listOf("العنكبوت", "عنكبوت", "ankabut", "ankaboot")),
        SurahMeta(30, "الروم", "Ar-Rum", listOf("الروم", "روم", "rum", "room")),
        SurahMeta(31, "لقمان", "Luqman", listOf("لقمان", "luqman", "lokman")),
        SurahMeta(32, "السجدة", "As-Sajdah", listOf("السجدة", "سجدة", "sajdah", "sajda")),
        SurahMeta(33, "الأحزاب", "Al-Ahzab", listOf("الأحزاب", "الاحزاب", "احزاب", "ahzab")),
        SurahMeta(34, "سبأ", "Saba", listOf("سبأ", "سبا", "saba", "saba'")),
        SurahMeta(35, "فاطر", "Fatir", listOf("فاطر", "fatir")),
        SurahMeta(36, "يس", "Ya-Sin", listOf("يس", "ياسين", "yasin", "ya-sin", "yaseen")),
        SurahMeta(37, "الصافات", "As-Saffat", listOf("الصافات", "صافات", "saffat", "as-saffat")),
        SurahMeta(38, "ص", "Sad", listOf("ص", "صاد", "sad")),
        SurahMeta(39, "الزمر", "Az-Zumar", listOf("الزمر", "زمر", "zumar")),
        SurahMeta(40, "غافر", "Ghafir", listOf("غافر", "المؤمن", "ghafir", "mumin")),
        SurahMeta(41, "فصلت", "Fussilat", listOf("فصلت", "fussilat")),
        SurahMeta(42, "الشورى", "Ash-Shura", listOf("الشورى", "شورى", "shura")),
        SurahMeta(43, "الزخرف", "Az-Zukhruf", listOf("الزخرف", "زخرف", "zukhruf")),
        SurahMeta(44, "الدخان", "Ad-Dukhan", listOf("الدخان", "دخان", "dukhan")),
        SurahMeta(45, "الجاثية", "Al-Jathiyah", listOf("الجاثية", "جاثية", "jathiyah", "jathiya")),
        SurahMeta(46, "الأحقاف", "Al-Ahqaf", listOf("الأحقاف", "الاحقاف", "احقاف", "ahqaf")),
        SurahMeta(47, "محمد", "Muhammad", listOf("محمد", "muhammad", "mohamed")),
        SurahMeta(48, "الفتح", "Al-Fath", listOf("الفتح", "فتح", "fath")),
        SurahMeta(49, "الحجرات", "Al-Hujurat", listOf("الحجرات", "حجرات", "hujurat")),
        SurahMeta(50, "ق", "Qaf", listOf("ق", "قاف", "qaf")),
        SurahMeta(51, "الذاريات", "Adh-Dhariyat", listOf("الذاريات", "ذاريات", "dhariyat")),
        SurahMeta(52, "الطور", "At-Tur", listOf("الطور", "طور", "tur", "toor")),
        SurahMeta(53, "النجم", "An-Najm", listOf("النجم", "نجم", "najm")),
        SurahMeta(54, "القمر", "Al-Qamar", listOf("القمر", "قمر", "qamar")),
        SurahMeta(55, "الرحمن", "Ar-Rahman", listOf("الرحمن", "رحمن", "rahman", "al-rahman")),
        SurahMeta(56, "الواقعة", "Al-Waqi'ah", listOf("الواقعة", "واقعة", "waqiah", "waqia")),
        SurahMeta(57, "الحديد", "Al-Hadid", listOf("الحديد", "حديد", "hadid")),
        SurahMeta(58, "المجادلة", "Al-Mujadila", listOf("المجادلة", "مجادلة", "mujadila")),
        SurahMeta(59, "الحشر", "Al-Hashr", listOf("الحشر", "حشر", "hashr")),
        SurahMeta(60, "الممتحنة", "Al-Mumtahanah", listOf("الممتحنة", "ممتحنة", "mumtahanah")),
        SurahMeta(61, "الصف", "As-Saff", listOf("الصف", "صف", "saff")),
        SurahMeta(62, "الجمعة", "Al-Jumu'ah", listOf("الجمعة", "جمعة", "jumuah", "juma")),
        SurahMeta(63, "المنافقون", "Al-Munafiqun", listOf("المنافقون", "منافقون", "munafiqun")),
        SurahMeta(64, "التغابن", "At-Taghabun", listOf("التغابن", "تغابن", "taghabun")),
        SurahMeta(65, "الطلاق", "At-Talaq", listOf("الطلاق", "طلاق", "talaq")),
        SurahMeta(66, "التحريم", "At-Tahrim", listOf("التحريم", "تحريم", "tahrim")),
        SurahMeta(67, "الملك", "Al-Mulk", listOf("الملك", "ملك", "تبارك", "mulk", "tabarak")),
        SurahMeta(68, "القلم", "Al-Qalam", listOf("القلم", "قلم", "qalam")),
        SurahMeta(69, "الحاقة", "Al-Haqqah", listOf("الحاقة", "حاقة", "haqqah")),
        SurahMeta(70, "المعارج", "Al-Ma'arij", listOf("المعارج", "معارج", "maarij")),
        SurahMeta(71, "نوح", "Nuh", listOf("نوح", "nuh", "nooh")),
        SurahMeta(72, "الجن", "Al-Jinn", listOf("الجن", "جن", "jinn")),
        SurahMeta(73, "المزمل", "Al-Muzzammil", listOf("المزمل", "مزمل", "muzzammil")),
        SurahMeta(74, "المدثر", "Al-Muddaththir", listOf("المدثر", "مدثر", "muddathir")),
        SurahMeta(75, "القيامة", "Al-Qiyamah", listOf("القيامة", "قيامة", "qiyamah")),
        SurahMeta(76, "الإنسان", "Al-Insan", listOf("الإنسان", "الانسان", "انسان", "الدهر", "insan")),
        SurahMeta(77, "المرسلات", "Al-Mursalat", listOf("المرسلات", "مرسلات", "mursalat")),
        SurahMeta(78, "النبأ", "An-Naba", listOf("النبأ", "النباء", "نبا", "عم", "naba")),
        SurahMeta(79, "النازعات", "An-Nazi'at", listOf("النازعات", "نازعات", "naziat")),
        SurahMeta(80, "عبس", "Abasa", listOf("عبس", "abasa")),
        SurahMeta(81, "التكوير", "At-Takwir", listOf("التكوير", "تكوير", "takwir")),
        SurahMeta(82, "الانفطار", "Al-Infitar", listOf("الانفطار", "انفطار", "infitar")),
        SurahMeta(83, "المطففين", "Al-Mutaffifin", listOf("المطففين", "مطففين", "mutaffifin")),
        SurahMeta(84, "الانشقاق", "Al-Inshiqaq", listOf("الانشقاق", "انشقاق", "inshiqaq")),
        SurahMeta(85, "البروج", "Al-Buruj", listOf("البروج", "بروج", "buruj")),
        SurahMeta(86, "الطارق", "At-Tariq", listOf("الطارق", "طارق", "tariq")),
        SurahMeta(87, "الأعلى", "Al-A'la", listOf("الأعلى", "الاعلى", "اعلى", "ala", "a'la")),
        SurahMeta(88, "الغاشية", "Al-Ghashiyah", listOf("الغاشية", "غاشية", "ghashiyah")),
        SurahMeta(89, "الفجر", "Al-Fajr", listOf("الفجر", "فجر", "fajr")),
        SurahMeta(90, "البلد", "Al-Balad", listOf("البلد", "بلد", "balad")),
        SurahMeta(91, "الشمس", "Ash-Shams", listOf("الشمس", "شمس", "shams")),
        SurahMeta(92, "الليل", "Al-Layl", listOf("الليل", "ليل", "layl")),
        SurahMeta(93, "الضحى", "Ad-Duha", listOf("الضحى", "ضحى", "duha")),
        SurahMeta(94, "الشرح", "Ash-Sharh", listOf("الشرح", "شرح", "الانشراح", "sharh", "inshirah")),
        SurahMeta(95, "التين", "At-Tin", listOf("التين", "تين", "tin")),
        SurahMeta(96, "العلق", "Al-Alaq", listOf("العلق", "علق", "iqra", "اقرأ", "alaq")),
        SurahMeta(97, "القدر", "Al-Qadr", listOf("القدر", "قدر", "qadr")),
        SurahMeta(98, "البينة", "Al-Bayyinah", listOf("البينة", "بينة", "bayyinah")),
        SurahMeta(99, "الزلزلة", "Az-Zalzalah", listOf("الزلزلة", "زلزلة", "zalzalah")),
        SurahMeta(100, "العاديات", "Al-Adiyat", listOf("العاديات", "عاديات", "adiyat")),
        SurahMeta(101, "القارعة", "Al-Qari'ah", listOf("القارعة", "قارعة", "qariah")),
        SurahMeta(102, "التكاثر", "At-Takathur", listOf("التكاثر", "تكاثر", "takathur")),
        SurahMeta(103, "العصر", "Al-Asr", listOf("العصر", "عصر", "asr")),
        SurahMeta(104, "الهمزة", "Al-Humazah", listOf("الهمزة", "همزة", "humazah")),
        SurahMeta(105, "الفيل", "Al-Fil", listOf("الفيل", "فيل", "fil")),
        SurahMeta(106, "قريش", "Quraysh", listOf("قريش", "quraysh", "quraish")),
        SurahMeta(107, "الماعون", "Al-Ma'un", listOf("الماعون", "ماعون", "maun")),
        SurahMeta(108, "الكوثر", "Al-Kawthar", listOf("الكوثر", "كوثر", "kawthar")),
        SurahMeta(109, "الكافرون", "Al-Kafirun", listOf("الكافرون", "كافرون", "kafirun")),
        SurahMeta(110, "النصر", "An-Nasr", listOf("النصر", "نصر", "nasr")),
        SurahMeta(111, "المسد", "Al-Masad", listOf("المسد", "مسد", "تبت", "masad")),
        SurahMeta(112, "الإخلاص", "Al-Ikhlas", listOf("الإخلاص", "الاخلاص", "اخلاص", "توحيد", "ikhlas")),
        SurahMeta(113, "الفلق", "Al-Falaq", listOf("الفلق", "فلق", "falaq")),
        SurahMeta(114, "الناس", "An-Nas", listOf("الناس", "ناس", "nas"))
    )

    fun getSurahName(number: Int): String {
        return SURAHS_LIST.find { it.number == number }?.arabicName ?: "سورة $number"
    }

    /**
     * Strictly extract Surah Number (1..114) from filename, title, track, metadata.
     * Returns Pair(surahNumber, reasonOrConfidence). If ambiguous or out of range, returns null.
     */
    fun extractSurahNumber(
        filename: String,
        title: String?,
        track: String?,
        metadata: Map<String, Any?>? = null
    ): Pair<Int, String>? {
        val cleanName = filename.lowercase(Locale.ROOT)

        // 1. Direct 3-digit regex at start of filename: e.g. "001.flac", "001 - Fatiha.flac", "001_Al-Baqarah.flac"
        val startThreeDigits = Regex("^0*([1-9][0-9]?|10[0-9]|11[0-4])[\\s\\-_.]").find(cleanName)
        if (startThreeDigits != null) {
            val num = startThreeDigits.groupValues[1].toIntOrNull()
            if (num in 1..114) {
                return num!! to "Filename 3-digit Prefix"
            }
        }

        // 2. Exact number standalone before extension: e.g. "001.flac", "114.flac"
        val exactNum = Regex("^0*([1-9][0-9]?|10[0-9]|11[0-4])\\.[a-z0-9]+$").find(cleanName)
        if (exactNum != null) {
            val num = exactNum.groupValues[1].toIntOrNull()
            if (num in 1..114) {
                return num!! to "Filename Exact Standalone Number"
            }
        }

        // 3. Track number if valid
        val trackNum = track?.trim()?.split("/")?.firstOrNull()?.toIntOrNull()
        if (trackNum != null && trackNum in 1..114) {
            // Verify with title or filename to prevent track mismatch
            for (surah in SURAHS_LIST) {
                if (surah.number == trackNum) {
                    val matches = surah.aliases.any { cleanName.contains(it) || (title != null && title.lowercase().contains(it)) }
                    if (matches) {
                        return trackNum to "Track Number + Surah Name Match"
                    }
                }
            }
        }

        // 4. Surah name match in filename or title
        val combined = "$cleanName ${title?.lowercase(Locale.ROOT) ?: ""}"
        for (surah in SURAHS_LIST) {
            for (alias in surah.aliases) {
                if (alias.length >= 3 && combined.contains(alias)) {
                    return surah.number to "Surah Alias Match ($alias)"
                }
            }
        }

        // 5. Embedded number pattern like "surah_001", "quran_114"
        val embeddedPattern = Regex("(?:surah|sura|soorah|سورة)[\\s_\\-]*0*([1-9][0-9]?|10[0-9]|11[0-4])").find(combined)
        if (embeddedPattern != null) {
            val num = embeddedPattern.groupValues[1].toIntOrNull()
            if (num in 1..114) {
                return num!! to "Embedded Surah Pattern Match"
            }
        }

        return null
    }

    /**
     * Detects Recitation Type (Mujawwad, Murattal, Concerts, Teacher)
     */
    fun detectRecitationType(
        title: String?,
        description: String?,
        identifier: String?,
        subject: String?
    ): String {
        val text = "${title ?: ""} ${description ?: ""} ${identifier ?: ""} ${subject ?: ""}".lowercase()
        return when {
            text.contains("مجود") || text.contains("المجود") || text.contains("mujawwad") || text.contains("mojawwad") -> "مجوّد"
            text.contains("حفلات") || text.contains("حفلة") || text.contains("نوادر") || text.contains("concert") || text.contains("rare") -> "حفلات وتلاوات نادرة"
            text.contains("معلم") || text.contains("المعلم") || text.contains("teacher") || text.contains("muallim") -> "المصحف المعلم"
            text.contains("خاشع") || text.contains("خاشعة") -> "تلاوات خاشعة"
            else -> "مرتّل"
        }
    }

    /**
     * Detects Riwayah (Hafs, Warsh, Qalun, Al-Duri, As-Sousi, etc.)
     */
    fun detectRiwayah(
        title: String?,
        description: String?,
        identifier: String?,
        subject: String?
    ): String {
        val text = "${title ?: ""} ${description ?: ""} ${identifier ?: ""} ${subject ?: ""}".lowercase()
        return when {
            text.contains("ورش") || text.contains("warsh") -> "ورش عن نافع"
            text.contains("قالون") || text.contains("qaloon") || text.contains("qalun") -> "قالون عن نافع"
            text.contains("السوسي") || text.contains("sousi") || text.contains("soosi") -> "السوسي عن أبي عمرو"
            text.contains("الدوري") || text.contains("douri") || text.contains("duri") -> "الدوري عن أبي عمرو"
            text.contains("خلف") || text.contains("khalaf") -> "خلف عن حمزة"
            text.contains("شعبة") || text.contains("shuba") || text.contains("shu'bah") -> "شعبة عن عاصم"
            else -> "حفص عن عاصم"
        }
    }

    fun isCompleteQuran(surahNumbers: Set<Int>): Boolean {
        return surahNumbers.size >= 114 || (surahNumbers.containsAll((1..114).toList()))
    }
}
