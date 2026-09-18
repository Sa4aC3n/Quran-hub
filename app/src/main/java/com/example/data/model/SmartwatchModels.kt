package com.example.data.model

/**
 * Supported Haptic Vibration Patterns for smartwatch and phone
 */
enum class HapticVibrationPattern(
    val titleAr: String,
    val descriptionAr: String,
    val timingsMs: LongArray,
    val amplitudes: IntArray
) {
    /**
     * Paused playback automatically when meeting starts:
     * Double gentle tap (70ms pulse, 80ms gap, 70ms pulse)
     */
    PLAYBACK_PAUSED_ON_MEETING(
        titleAr = "إيقاف التلاوة في الاجتماع",
        descriptionAr = "نبضتان خفيفتان ناعمتان لتأكيد كتم الصوت فوراً",
        timingsMs = longArrayOf(0, 70, 80, 70),
        amplitudes = intArrayOf(0, 140, 0, 140)
    ),

    /**
     * Daily Wird Quran Reminder:
     * Triple ascending subtle pulses
     */
    DAILY_WIRD_REMINDER(
        titleAr = "تذكير الورد اليومي",
        descriptionAr = "ثلاث نبضات ناعمة متدرجة للتذكير بالورد",
        timingsMs = longArrayOf(0, 50, 70, 70, 70, 100),
        amplitudes = intArrayOf(0, 100, 0, 160, 0, 220)
    ),

    /**
     * Khatma Milestone & Achievement:
     * Rhythmic harmonic celebration vibration
     */
    KHATMA_MILESTONE(
        titleAr = "تنبيه الختمة والإنجاز",
        descriptionAr = "نمط إيقاعي لطيف للاحتفاء بإتمام مرحلة في الختمة",
        timingsMs = longArrayOf(0, 90, 60, 90, 60, 140),
        amplitudes = intArrayOf(0, 180, 0, 180, 0, 255)
    ),

    /**
     * Meeting ended / Resume prompt:
     * Single gentle tap
     */
    MEETING_ENDED_RESUME(
        titleAr = "انتهاء الاجتماع واستئناف التلاوة",
        descriptionAr = "نبضة مفردة هادئة تسألك عن متابعة الاستماع",
        timingsMs = longArrayOf(0, 80),
        amplitudes = intArrayOf(0, 150)
    )
}

/**
 * App Notification categories for silent filtering in Meeting Mode
 */
enum class NotificationCategory(val titleAr: String) {
    DAILY_WIRD("تذكير الورد القرآني"),
    KHATMA_REMINDER("تذكير تقدم الختمة"),
    NEW_RECITATION("التلاوات والمحتوى الجديد"),
    GENERAL("التنبيهات العامة")
}

/**
 * State representing Meeting Mode configuration and active session
 */
data class MeetingModeState(
    val isEnabled: Boolean = false,
    val isAutoCalendarDetectEnabled: Boolean = false,
    val isDndSyncEnabled: Boolean = false,
    val pausePlaybackOnMeeting: Boolean = true,
    val vibrateOnlyNotifications: Boolean = true,
    val activeMeetingTitle: String? = null,
    val remainingDurationMinutes: Int? = null,
    val meetingEndTimeEpochMs: Long? = null,
    val allowedNotificationTypes: Set<NotificationCategory> = setOf(NotificationCategory.DAILY_WIRD),
    val autoResumePromptAfterMeeting: Boolean = true,
    val lastStoppedSurahNumber: Int? = null,
    val lastStoppedSurahName: String? = null,
    val lastStoppedAyahNumber: Int? = null,
    val lastStoppedReciterName: String? = null,
    val lastStoppedPositionMs: Long? = null
) {
    val isMeetingModeActive: Boolean get() = isEnabled
}

/**
 * Snapshot sent to Smartwatch (Wear OS Tile / Complication / watchOS)
 */
data class WatchSyncSnapshot(
    val isPlaying: Boolean = false,
    val currentSurahNumber: Int = 1,
    val currentSurahName: String = "الفاتحة",
    val currentAyahNumber: Int? = 1,
    val reciterName: String = "مشاري العفاسي",
    val progressPercent: Int = 0,
    val isMeetingModeActive: Boolean = false,
    val meetingRemainingMinutes: Int? = null,
    val lastSavedSurahNumber: Int = 1,
    val lastSavedSurahName: String = "الفاتحة",
    val lastSavedAyahNumber: Int = 1,
    val lastSavedReciterName: String = "مشاري العفاسي",
    val favoriteRecitersCount: Int = 0
)
