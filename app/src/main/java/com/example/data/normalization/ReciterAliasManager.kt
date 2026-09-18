package com.example.data.normalization

data class CanonicalReciterProfile(
    val canonicalId: String,
    val arabicName: String,
    val englishName: String,
    val country: String,
    val aliases: List<String>
)

object ReciterAliasManager {

    val KNOWN_PROFILES: List<CanonicalReciterProfile> = listOf(
        CanonicalReciterProfile(
            canonicalId = "mohamed_refaat",
            arabicName = "محمد رفعت",
            englishName = "Mohamed Refaat",
            country = "مصر",
            aliases = listOf(
                "الشيخ محمد رفعت",
                "محمد رفعت",
                "Mohamed Refaat",
                "Mohammad Refaat",
                "Muhammad Rifat",
                "Sheikh Mohamed Refaat",
                "قثارة السماء محمد رفعت"
            )
        ),
        CanonicalReciterProfile(
            canonicalId = "abdulbasit_abdulsamad",
            arabicName = "عبد الباسط عبد الصمد",
            englishName = "Abdul Basit Abdul Samad",
            country = "مصر",
            aliases = listOf(
                "الشيخ عبد الباسط عبد الصمد",
                "عبد الباسط عبد الصمد",
                "عبدالباسط عبدالصمد",
                "Abdulbasit Abdulsamad",
                "Abdul Basit",
                "Abdelbasset Abdessamad",
                "Sheikh Abdul Basit"
            )
        ),
        CanonicalReciterProfile(
            canonicalId = "mahmoud_al_hussary",
            arabicName = "محمود خليل الحصري",
            englishName = "Mahmoud Khalil Al-Hussary",
            country = "مصر",
            aliases = listOf(
                "الشيخ محمود خليل الحصري",
                "محمود خليل الحصري",
                "الشيخ الحصري",
                "Mahmoud Khalil Al-Hussary",
                "Mahmood Khaleel Al-Husaree",
                "Al-Hussary",
                "Al-Husary",
                "Sheikh Al-Hussary"
            )
        ),
        CanonicalReciterProfile(
            canonicalId = "mohammad_siddiq_al_minshawi",
            arabicName = "محمد صديق المنشاوي",
            englishName = "Mohamed Siddiq Al-Minshawi",
            country = "مصر",
            aliases = listOf(
                "الشيخ محمد صديق المنشاوي",
                "محمد صديق المنشاوي",
                "الشيخ المنشاوي",
                "Mohamed Siddiq El-Minshawi",
                "Muhammad Siddeeq Al-Minshawi",
                "Al-Minshawi",
                "Sheikh Minshawi"
            )
        ),
        CanonicalReciterProfile(
            canonicalId = "mustafa_ismail",
            arabicName = "مصطفى إسماعيل",
            englishName = "Mustafa Ismail",
            country = "مصر",
            aliases = listOf(
                "الشيخ مصطفى إسماعيل",
                "مصطفى اسماعيل",
                "Mustafa Ismail",
                "Mustapha Ismail",
                "Sheikh Mustafa Ismail"
            )
        ),
        CanonicalReciterProfile(
            canonicalId = "mahmoud_ali_al_banna",
            arabicName = "محمود علي البنا",
            englishName = "Mahmoud Ali Al-Banna",
            country = "مصر",
            aliases = listOf(
                "الشيخ محمود علي البنا",
                "محمود علي البنا",
                "Mahmoud Ali Al-Banna",
                "Al-Banna",
                "Sheikh Al-Banna"
            )
        ),
        CanonicalReciterProfile(
            canonicalId = "mishari_alafasy",
            arabicName = "مشاري راشد العفاسي",
            englishName = "Mishari Rashid Alafasy",
            country = "الكويت",
            aliases = listOf(
                "الشيخ مشاري راشد العفاسي",
                "مشاري العفاسي",
                "مشاري راشد",
                "Mishari Rashid Alafasy",
                "Mishaari Raashid Al-Afasy",
                "Alafasy",
                "Al-Afasy"
            )
        ),
        CanonicalReciterProfile(
            canonicalId = "saad_al_ghamdi",
            arabicName = "سعد الغامدي",
            englishName = "Saad Al-Ghamdi",
            country = "السعودية",
            aliases = listOf(
                "الشيخ سعد الغامدي",
                "سعد الغامدي",
                "Saad Al-Ghamdi",
                "Sa3d Al-Ghaamidee",
                "Saad Ghamidi"
            )
        ),
        CanonicalReciterProfile(
            canonicalId = "abdul_rahman_al_sudais",
            arabicName = "عبد الرحمن السديس",
            englishName = "Abdul Rahman Al-Sudais",
            country = "السعودية - الحرم المكي",
            aliases = listOf(
                "الشيخ عبد الرحمن السديس",
                "عبدالرحمن السديس",
                "Abdul Rahman Al-Sudais",
                "Abdurrahmaan As-Sudays",
                "Al-Sudais"
            )
        ),
        CanonicalReciterProfile(
            canonicalId = "saud_al_shuraim",
            arabicName = "سعود الشريم",
            englishName = "Saud Al-Shuraim",
            country = "السعودية - الحرم المكي",
            aliases = listOf(
                "الشيخ سعود الشريم",
                "سعود بن إبراهيم الشريم",
                "Saud Al-Shuraim",
                "Saood Ash-Shuraym",
                "Al-Shuraim"
            )
        ),
        CanonicalReciterProfile(
            canonicalId = "abu_bakr_al_shatri",
            arabicName = "أبو بكر الشاطري",
            englishName = "Abu Bakr Al-Shatri",
            country = "السعودية",
            aliases = listOf(
                "الشيخ أبو بكر الشاطري",
                "أبوبكر الشاطري",
                "Abu Bakr Al-Shatri",
                "Abu Bakr Ash-Shaatree"
            )
        ),
        CanonicalReciterProfile(
            canonicalId = "yasser_al_dosari",
            arabicName = "ياسر الدوسري",
            englishName = "Yasser Al-Dosari",
            country = "السعودية - الحرم المكي",
            aliases = listOf(
                "الشيخ ياسر الدوسري",
                "ياسر بن راشد الدوسري",
                "Yasser Al-Dosari",
                "Yasser Ad-Dussary"
            )
        ),
        CanonicalReciterProfile(
            canonicalId = "ali_jaber",
            arabicName = "علي عبد الله جابر",
            englishName = "Ali Jaber",
            country = "السعودية - الحرم المكي",
            aliases = listOf(
                "الشيخ علي جابر",
                "علي عبد الله جابر",
                "علي جابر",
                "Ali Jaber",
                "Ali Abdullah Jaber"
            )
        ),
        CanonicalReciterProfile(
            canonicalId = "maher_al_muaiqly",
            arabicName = "ماهر المعيقلي",
            englishName = "Maher Al-Muaiqly",
            country = "السعودية - مكة",
            aliases = listOf(
                "الشيخ ماهر المعيقلي",
                "ماهر المعيقلي",
                "ماهر حمد المعيقلي",
                "Maher Al-Muaiqly",
                "Maher Almuaiqly"
            )
        ),
        CanonicalReciterProfile(
            canonicalId = "ahmed_al_ajmi",
            arabicName = "أحمد بن علي العجمي",
            englishName = "Ahmed Al-Ajmi",
            country = "السعودية",
            aliases = listOf(
                "الشيخ أحمد العجمي",
                "أحمد بن علي العجمي",
                "Ahmed Al-Ajmi",
                "Ahmed Ibn Ali Al-Ajamy"
            )
        ),
        CanonicalReciterProfile(
            canonicalId = "mohamed_al_tablawi",
            arabicName = "محمد محمود الطبلاوي",
            englishName = "Mohamed Al-Tablawi",
            country = "مصر",
            aliases = listOf(
                "الشيخ محمد محمود الطبلاوي",
                "محمد الطبلاوي",
                "Mohammad Al-Tablaway",
                "Al-Tablawi"
            )
        ),
        CanonicalReciterProfile(
            canonicalId = "abdulrashid_sufi",
            arabicName = "عبد الرشيد صوفي",
            englishName = "Abdul Rashid Sufi",
            country = "الصومال / قطر",
            aliases = listOf(
                "الشيخ عبد الرشيد صوفي",
                "عبدالرشيد صوفي",
                "Abdul Rashid Sufi",
                "Abdul Rasheed Soufi"
            )
        ),
        CanonicalReciterProfile(
            canonicalId = "yassin_al_jazairi",
            arabicName = "ياسين الجزائري",
            englishName = "Yasin Al-Jazairi",
            country = "الجزائر",
            aliases = listOf(
                "الشيخ ياسين الجزائري",
                "ياسين الجزائري",
                "Yasin Al-Jazairi",
                "Yasin Al Jaza-iree"
            )
        )
    )

    fun normalizeReciter(
        creator: String?,
        title: String?,
        description: String?,
        identifier: String?
    ): Pair<String, String> { // Pair(canonicalId, arabicName)
        val combined = "${creator ?: ""} ${title ?: ""} ${description ?: ""} ${identifier ?: ""}"
        val cleaned = cleanText(combined)

        for (profile in KNOWN_PROFILES) {
            for (alias in profile.aliases) {
                if (cleaned.contains(cleanText(alias), ignoreCase = true)) {
                    return profile.canonicalId to profile.arabicName
                }
            }
        }

        // If not in known profiles, derive cleanly from creator or title
        val fallbackName = extractDynamicReciterName(creator, title)
        val slug = generateIdSlug(fallbackName)
        return slug to fallbackName
    }

    private fun extractDynamicReciterName(creator: String?, title: String?): String {
        var raw = creator?.takeIf { it.isNotBlank() } ?: title ?: "قارئ غير معروف"
        // Strip out common noise words
        val noiseWords = listOf(
            "الشيخ", "فضيلة الشيخ", "القارئ", "المقرئ", "تلاوة", "المصحف", "المرتل", "المجود",
            "بصوت", "رواية", "حفص", "ورش", "قراءة", "كامل", "مصحف", "تلاوات", "mp3", "quran",
            "audio", "archive", "القرآن", "الكريم"
        )
        for (noise in noiseWords) {
            raw = raw.replace(noise, "", ignoreCase = true)
        }
        val trimmed = raw.trim().replace(Regex("[\\-_()0-9\\[\\]]+"), " ").trim()
        return if (trimmed.length >= 3) trimmed else (creator?.trim() ?: "قارئ غير معروف")
    }

    private fun generateIdSlug(name: String): String {
        return name.lowercase()
            .replace(Regex("[^a-z0-9\\u0600-\\u06FF]+"), "_")
            .trim('_')
            .ifEmpty { "reciter_${System.currentTimeMillis() % 10000}" }
    }

    private fun cleanText(text: String): String {
        return text
            .replace("أ", "ا")
            .replace("إ", "ا")
            .replace("آ", "ا")
            .replace("ة", "ه")
            .replace("ى", "ي")
            .replace(Regex("[\\p{Mn}\\s\\-_,]+"), " ")
            .trim()
            .lowercase()
    }
}
