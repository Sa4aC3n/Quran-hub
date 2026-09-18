package com.example.i18n

/**
 * Represents a supported language in the Quran app with localization metadata.
 */
data class Language(
    val code: String,
    val nativeName: String,
    val arabicName: String,
    val flagEmoji: String,
    val isRtl: Boolean,
    val shortLabel: String
) {
    companion object {
        val ARABIC = Language("ar", "العربية", "العربية", "🇸🇦", isRtl = true, shortLabel = "AR")
        val ENGLISH = Language("en", "English", "الإنجليزية", "🇬🇧", isRtl = false, shortLabel = "EN")
        val FRENCH = Language("fr", "Français", "الفرنسية", "🇫🇷", isRtl = false, shortLabel = "FR")
        val ITALIAN = Language("it", "Italiano", "الإيطالية", "🇮🇹", isRtl = false, shortLabel = "IT")
        val JAPANESE = Language("ja", "日本語", "اليابانية", "🇯🇵", isRtl = false, shortLabel = "JA")
        val GERMAN = Language("de", "Deutsch", "الألمانية", "🇩🇪", isRtl = false, shortLabel = "DE")
        val RUSSIAN = Language("ru", "Русский", "الروسية", "🇷🇺", isRtl = false, shortLabel = "RU")
        val CHINESE = Language("zh", "简体中文", "الصينية", "🇨🇳", isRtl = false, shortLabel = "ZH")
        val PERSIAN = Language("fa", "فارسی", "الفارسية", "🇮🇷", isRtl = true, shortLabel = "FA")
        val URDU = Language("ur", "اردو", "الأردية", "🇵🇰", isRtl = true, shortLabel = "UR")
        val HAUSA = Language("ha", "Hausa", "الهوساوية", "🇳🇬", isRtl = false, shortLabel = "HA")
        val KYRGYZ = Language("ky", "Кыргызча", "القيرغيزية", "🇰🇬", isRtl = false, shortLabel = "KY")

        val ALL: List<Language> = listOf(
            ARABIC,
            ENGLISH,
            FRENCH,
            ITALIAN,
            JAPANESE,
            GERMAN,
            RUSSIAN,
            CHINESE,
            PERSIAN,
            URDU,
            HAUSA,
            KYRGYZ
        )

        fun findByCode(code: String): Language {
            val normalized = code.trim().lowercase().take(2)
            return ALL.find { it.code == normalized } ?: ARABIC
        }
    }
}
