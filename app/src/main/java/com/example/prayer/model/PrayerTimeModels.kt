package com.example.prayer.model

import java.util.Calendar

enum class PrayerType(
    val id: String,
    val arabicName: String,
    val isActualPrayer: Boolean = true
) {
    FAJR("fajr", "الفجر", true),
    SUNRISE("sunrise", "الشروق", false),
    DHUHR("dhuhr", "الظهر", true),
    ASR("asr", "العصر", true),
    MAGHRIB("maghrib", "المغرب", true),
    ISHA("isha", "العشاء", true)
}

enum class CalculationMethod(
    val id: String,
    val arabicName: String,
    val description: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val ishaIntervalMinutes: Int? = null
) {
    EGYPTIAN(
        id = "egypt",
        arabicName = "الهيئة العامة المصرية للمساحة",
        description = "مصر، إفريقيا، الشرق الأوسط (فجر: 19.5°، عشاء: 17.5°)",
        fajrAngle = 19.5,
        ishaAngle = 17.5
    ),
    UMM_AL_QURA(
        id = "makkah",
        arabicName = "أم القرى - مكة المكرمة",
        description = "المملكة العربية السعودية والخليج (فجر: 18.5°، عشاء: +90 دقيقة)",
        fajrAngle = 18.5,
        ishaAngle = 0.0,
        ishaIntervalMinutes = 90
    ),
    MUSLIM_WORLD_LEAGUE(
        id = "mwl",
        arabicName = "رابطة العالم الإسلامي",
        description = "أوروبا، الشرق الأقصى (فجر: 18°، عشاء: 17°)",
        fajrAngle = 18.0,
        ishaAngle = 17.0
    ),
    KARACHI(
        id = "karachi",
        arabicName = "جامعة العلوم الإسلامية بكراتشي",
        description = "باكستان، الهند، بنغلاديش، أفغانستان (فجر: 18°، عشاء: 18°)",
        fajrAngle = 18.0,
        ishaAngle = 18.0
    ),
    ISNA(
        id = "isna",
        arabicName = "الجمعية الإسلامية لأمريكا الشمالية (ISNA)",
        description = "الولايات المتحدة، كندا، أمريكا الشمالية (فجر: 15°، عشاء: 15°)",
        fajrAngle = 15.0,
        ishaAngle = 15.0
    ),
    DIYANET(
        id = "diyanet",
        arabicName = "رئاسة الشؤون الدينية بتركيا (Diyanet)",
        description = "تركيا والمناطق المحيطة (فجر: 18°، عشاء: 17°)",
        fajrAngle = 18.0,
        ishaAngle = 17.0
    ),
    DUBAI(
        id = "dubai",
        arabicName = "دائرة الشؤون الإسلامية بدبي",
        description = "الإمارات العربية المتحدة (فجر: 18.2°، عشاء: 18.2°)",
        fajrAngle = 18.2,
        ishaAngle = 18.2
    ),
    KUWAIT(
        id = "kuwait",
        arabicName = "وزارة الأوقاف الكويتية",
        description = "دولة الكويت (فجر: 18°، عشاء: 17.5°)",
        fajrAngle = 18.0,
        ishaAngle = 17.5
    ),
    QATAR(
        id = "qatar",
        arabicName = "وزارة الأوقاف والشؤون الإسلامية بقطر",
        description = "دولة قطر (فجر: 18°، عشاء: +90 دقيقة)",
        fajrAngle = 18.0,
        ishaAngle = 0.0,
        ishaIntervalMinutes = 90
    )
}

enum class AsrJuristic(val id: String, val arabicName: String, val factor: Int) {
    STANDARD("standard", "الجمهور (الشافعي، المالكي، الحنبلي)", 1),
    HANAFI("hanafi", "المذهب الحنفي", 2)
}

data class AdhanVoice(
    val id: String,
    val name: String,
    val muezzin: String,
    val origin: String,
    val audioUrl: String
)

data class PrayerTimeItem(
    val type: PrayerType,
    val timeFormatted: String,
    val timeMillis: Long,
    val isPassed: Boolean,
    val isCurrent: Boolean,
    val isNext: Boolean,
    val isNotificationEnabled: Boolean,
    val minutesOffset: Int = 0
)

data class DayPrayerTimes(
    val dateGregorian: String,
    val dateHijri: String,
    val prayers: List<PrayerTimeItem>,
    val nextPrayer: PrayerTimeItem?,
    val currentPrayer: PrayerTimeItem?,
    val timeUntilNextMillis: Long,
    val progressPercent: Float,
    val location: LocationInfo
)

data class LocationInfo(
    val latitude: Double = 30.0444, // Default Cairo
    val longitude: Double = 31.2357,
    val cityName: String = "القاهرة",
    val countryName: String = "مصر",
    val isAutoGps: Boolean = true
)

data class QiblaData(
    val qiblaAngle: Float = 136.0f,
    val userHeading: Float = 0.0f,
    val relativeAngle: Float = 136.0f,
    val distanceKm: Double = 1250.0,
    val isAligned: Boolean = false,
    val accuracy: Int = 3,
    val isSensorAvailable: Boolean = true
)

data class IqamahSettings(
    val enabled: Boolean = true,
    val fajrMinutes: Int = 20,
    val dhuhrMinutes: Int = 15,
    val asrMinutes: Int = 15,
    val maghribMinutes: Int = 10,
    val ishaMinutes: Int = 15
) {
    fun getMinutesForPrayer(type: PrayerType): Int {
        return when (type) {
            PrayerType.FAJR -> fajrMinutes
            PrayerType.DHUHR -> dhuhrMinutes
            PrayerType.ASR -> asrMinutes
            PrayerType.MAGHRIB -> maghribMinutes
            PrayerType.ISHA -> ishaMinutes
            PrayerType.SUNRISE -> 0
        }
    }
}

data class PrayerNotificationSettings(
    val enabledPrayers: Map<PrayerType, Boolean> = mapOf(
        PrayerType.FAJR to true,
        PrayerType.SUNRISE to false,
        PrayerType.DHUHR to true,
        PrayerType.ASR to true,
        PrayerType.MAGHRIB to true,
        PrayerType.ISHA to true
    ),
    val selectedVoiceId: String = "makkah",
    val playFullAdhan: Boolean = true,
    val vibrateOnly: Boolean = false,
    val iqamahSettings: IqamahSettings = IqamahSettings()
)

data class PrayerTimeOffsets(
    val fajr: Int = 0,
    val sunrise: Int = 0,
    val dhuhr: Int = 0,
    val asr: Int = 0,
    val maghrib: Int = 0,
    val isha: Int = 0
) {
    fun getOffset(type: PrayerType): Int {
        return when (type) {
            PrayerType.FAJR -> fajr
            PrayerType.SUNRISE -> sunrise
            PrayerType.DHUHR -> dhuhr
            PrayerType.ASR -> asr
            PrayerType.MAGHRIB -> maghrib
            PrayerType.ISHA -> isha
        }
    }
}
