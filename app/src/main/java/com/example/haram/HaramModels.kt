package com.example.haram

import com.squareup.moshi.JsonClass

enum class HaramLocation(
    val id: String,
    val arabicName: String,
    val shortName: String,
    val emoji: String,
    val defaultReciter: String,
    val primaryStreamUrl: String,
    val backupStreamUrls: List<String>
) {
    MAKKAH(
        id = "makkah",
        arabicName = "الحرم المكي الشريف",
        shortName = "مكة المكرمة",
        emoji = "🕋",
        defaultReciter = "أئمة الحرم المكي الشريف",
        primaryStreamUrl = "https://qurango.net/radio/tarteel",
        backupStreamUrls = listOf(
            "https://backup.qurango.net/radio/tarteel",
            "https://stream.radiojar.com/4wqre23fytzuv",
            "https://live.mp3quran.net:9702/"
        )
    ),
    MADINAH(
        id = "madinah",
        arabicName = "الحرم النبوي الشريف",
        shortName = "المدينة المنورة",
        emoji = "🕌",
        defaultReciter = "أئمة الحرم النبوي الشريف",
        primaryStreamUrl = "https://backup.qurango.net/radio/madinah_live",
        backupStreamUrls = listOf(
            "https://qurango.net/radio/madinah_live",
            "https://stream.radiojar.com/0t5t1g29ytzuv",
            "https://live.mp3quran.net:9718/"
        )
    );

    companion object {
        fun fromId(id: String?): HaramLocation {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: MAKKAH
        }
    }
}

enum class HaramTargetSource(val labelArabic: String) {
    MAKKAH_ONLY("الحرم المكي فقط 🕋"),
    MADINAH_ONLY("الحرم النبوي فقط 🕌"),
    BOTH("كلا الحرمين (مكة والمدينة) 🕋🕌")
}

enum class HaramNotificationMode(val labelArabic: String, val descriptionArabic: String) {
    ALL_NEW_SURAHS("عند بداية كل سورة جديدة", "إشعار عند انتقال التلاوة في البث لسورة جديدة"),
    ONLY_PREFERRED_SURAHS("السور المفضلة فقط", "إشعار فقط عندما يقرأ في الحرم إحدى السور التي حددتها مسبقاً"),
    SPECIAL_TIMES_ONLY("في أوقات مباركة ومحددة", "وقت الفجر، صلاة الجمعة، والعشاء والتراويح")
}

@JsonClass(generateAdapter = true)
data class HaramNowPlaying(
    val location: HaramLocation = HaramLocation.MAKKAH,
    val surahNumber: Int = 1,
    val surahName: String = "الفاتحة",
    val ayahRange: String = "كاملة",
    val reciterName: String = "أئمة الحرم المكي",
    val streamUrl: String = HaramLocation.MAKKAH.primaryStreamUrl,
    val timestamp: Long = System.currentTimeMillis(),
    val isLive: Boolean = true,
    val confidenceScore: Float = 0.95f,
    val nextScheduledSurah: String? = null
)

data class HaramNotificationPreferences(
    val isEnabled: Boolean = false, // Strictly Opt-In (Off by default)
    val targetSource: HaramTargetSource = HaramTargetSource.BOTH,
    val notificationMode: HaramNotificationMode = HaramNotificationMode.ALL_NEW_SURAHS,
    val preferredSurahNumbers: Set<Int> = setOf(1, 2, 18, 36, 55, 56, 67), // Default popular surahs
    val maxDailyNotifications: Int = 5,
    val todayNotificationsCount: Int = 0,
    val lastNotificationDate: String = "",
    val lastNotifiedSurahNumberMakkah: Int? = null,
    val lastNotifiedSurahNumberMadinah: Int? = null
)
