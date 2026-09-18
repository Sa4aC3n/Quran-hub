package com.example.haram

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HaramNotificationManager(context: Context) {

    private val appContext = context.applicationContext

    companion object {
        const val CHANNEL_ID = "haram_live_recitations"
        const val CHANNEL_NAME = "الآن يُقرأ في الحرم الشريف"
        const val CHANNEL_DESC = "إشعارات لطيفة عند بدء تلاوة سور القرآن الكريم في بث الحرمين المباشر"
        const val NOTIFICATION_ID_BASE = 88440
        const val EXTRA_PLAY_HARAM = "extra_play_haram"
        const val EXTRA_HARAM_ID = "extra_haram_id"
        const val EXTRA_SURAH_NUM = "extra_surah_num"
        const val EXTRA_SURAH_NAME = "extra_surah_name"
    }

    private val prefs: SharedPreferences = appContext.getSharedPreferences("haram_notification_prefs", Context.MODE_PRIVATE)

    private val _preferencesFlow = MutableStateFlow(loadPreferences())
    val preferencesFlow: StateFlow<HaramNotificationPreferences> = _preferencesFlow.asStateFlow()

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT // Normal priority -> respects system Do Not Disturb
            ).apply {
                description = CHANNEL_DESC
                enableLights(true)
                setShowBadge(true)
            }
            nm.createNotificationChannel(channel)
        }
    }

    fun loadPreferences(): HaramNotificationPreferences {
        val isEnabled = prefs.getBoolean("is_enabled", false) // Opt-in: default false
        val targetSource = try {
            HaramTargetSource.valueOf(prefs.getString("target_source", HaramTargetSource.BOTH.name) ?: HaramTargetSource.BOTH.name)
        } catch (_: Exception) {
            HaramTargetSource.BOTH
        }
        val mode = try {
            HaramNotificationMode.valueOf(prefs.getString("notification_mode", HaramNotificationMode.ALL_NEW_SURAHS.name) ?: HaramNotificationMode.ALL_NEW_SURAHS.name)
        } catch (_: Exception) {
            HaramNotificationMode.ALL_NEW_SURAHS
        }
        val preferredSurahsSet = prefs.getStringSet("preferred_surahs", setOf("1", "2", "18", "36", "55", "56", "67")) ?: emptySet()
        val preferredSurahs = preferredSurahsSet.mapNotNull { it.toIntOrNull() }.toSet()
        val maxDaily = prefs.getInt("max_daily_notifs", 5)

        val todayStr = getTodayDateString()
        val lastDate = prefs.getString("last_notif_date", "") ?: ""
        val todayCount = if (lastDate == todayStr) prefs.getInt("today_notif_count", 0) else 0

        val lastMakkah = if (prefs.contains("last_surah_makkah")) prefs.getInt("last_surah_makkah", -1).takeIf { it > 0 } else null
        val lastMadinah = if (prefs.contains("last_surah_madinah")) prefs.getInt("last_surah_madinah", -1).takeIf { it > 0 } else null

        return HaramNotificationPreferences(
            isEnabled = isEnabled,
            targetSource = targetSource,
            notificationMode = mode,
            preferredSurahNumbers = preferredSurahs,
            maxDailyNotifications = maxDaily,
            todayNotificationsCount = todayCount,
            lastNotificationDate = lastDate,
            lastNotifiedSurahNumberMakkah = lastMakkah,
            lastNotifiedSurahNumberMadinah = lastMadinah
        )
    }

    fun updatePreferences(
        isEnabled: Boolean? = null,
        targetSource: HaramTargetSource? = null,
        notificationMode: HaramNotificationMode? = null,
        preferredSurahs: Set<Int>? = null,
        maxDailyNotifications: Int? = null
    ) {
        val editor = prefs.edit()
        isEnabled?.let { editor.putBoolean("is_enabled", it) }
        targetSource?.let { editor.putString("target_source", it.name) }
        notificationMode?.let { editor.putString("notification_mode", it.name) }
        preferredSurahs?.let { editor.putStringSet("preferred_surahs", it.map { num -> num.toString() }.toSet()) }
        maxDailyNotifications?.let { editor.putInt("max_daily_notifs", it) }
        editor.apply()

        _preferencesFlow.value = loadPreferences()
    }

    fun canSendNotification(nowPlaying: HaramNowPlaying): Boolean {
        val currentPrefs = loadPreferences()

        // 1. Must be strictly enabled (Opt-in)
        if (!currentPrefs.isEnabled) return false

        // 2. Source check (Makkah vs Madinah)
        when (currentPrefs.targetSource) {
            HaramTargetSource.MAKKAH_ONLY -> if (nowPlaying.location != HaramLocation.MAKKAH) return false
            HaramTargetSource.MADINAH_ONLY -> if (nowPlaying.location != HaramLocation.MADINAH) return false
            HaramTargetSource.BOTH -> { /* allow both */ }
        }

        // 3. Prevent duplicate notification for the same Surah in a row
        val lastSurah = if (nowPlaying.location == HaramLocation.MAKKAH) {
            currentPrefs.lastNotifiedSurahNumberMakkah
        } else {
            currentPrefs.lastNotifiedSurahNumberMadinah
        }
        if (lastSurah != null && lastSurah == nowPlaying.surahNumber) {
            return false
        }

        // 4. Rate Limiting Check (Max daily limit)
        val todayStr = getTodayDateString()
        val todayCount = if (currentPrefs.lastNotificationDate == todayStr) currentPrefs.todayNotificationsCount else 0
        if (todayCount >= currentPrefs.maxDailyNotifications) {
            return false
        }

        // 5. Mode filter check
        when (currentPrefs.notificationMode) {
            HaramNotificationMode.ALL_NEW_SURAHS -> {
                return true
            }
            HaramNotificationMode.ONLY_PREFERRED_SURAHS -> {
                return currentPrefs.preferredSurahNumbers.contains(nowPlaying.surahNumber)
            }
            HaramNotificationMode.SPECIAL_TIMES_ONLY -> {
                return isSpecialTimeNow() || currentPrefs.preferredSurahNumbers.contains(nowPlaying.surahNumber)
            }
        }
    }

    private fun isSpecialTimeNow(): Boolean {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

        // Friday prayer time (11 AM to 2 PM)
        if (dayOfWeek == Calendar.FRIDAY && hour in 11..14) return true

        // Fajr time (3:30 AM to 6:00 AM)
        if (hour in 3..6) return true

        // Isha & Taraweeh / Evening time (8 PM to 11 PM)
        if (hour in 20..23) return true

        return false
    }

    fun showHaramNowPlayingNotification(nowPlaying: HaramNowPlaying, isTest: Boolean = false) {
        try {
            val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            // Record notification for rate limiting if not a test
            if (!isTest) {
                val todayStr = getTodayDateString()
                val currentPrefs = loadPreferences()
                val newCount = if (currentPrefs.lastNotificationDate == todayStr) currentPrefs.todayNotificationsCount + 1 else 1

                val editor = prefs.edit()
                editor.putString("last_notif_date", todayStr)
                editor.putInt("today_notif_count", newCount)
                if (nowPlaying.location == HaramLocation.MAKKAH) {
                    editor.putInt("last_surah_makkah", nowPlaying.surahNumber)
                } else {
                    editor.putInt("last_surah_madinah", nowPlaying.surahNumber)
                }
                editor.apply()
                _preferencesFlow.value = loadPreferences()
            }

            val notifId = if (nowPlaying.location == HaramLocation.MAKKAH) NOTIFICATION_ID_BASE else NOTIFICATION_ID_BASE + 1

            // 1. Primary Action: "استمع الآن" -> Deep Links into MainActivity with Auto-Play
            val playIntent = Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_PLAY_HARAM, true)
                putExtra(EXTRA_HARAM_ID, nowPlaying.location.id)
                putExtra(EXTRA_SURAH_NUM, nowPlaying.surahNumber)
                putExtra(EXTRA_SURAH_NAME, nowPlaying.surahName)
            }
            val playPendingIntent = PendingIntent.getActivity(
                appContext,
                notifId,
                playIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 2. Opt-Out Action: "إيقاف هذه التنبيهات" -> Direct one-tap disable
            val disableIntent = Intent(appContext, HaramNotificationActionReceiver::class.java).apply {
                action = HaramNotificationActionReceiver.ACTION_DISABLE_HARAM_NOTIFICATIONS
                putExtra(HaramNotificationActionReceiver.EXTRA_NOTIFICATION_ID, notifId)
            }
            val disablePendingIntent = PendingIntent.getBroadcast(
                appContext,
                notifId + 100,
                disableIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = if (nowPlaying.location == HaramLocation.MAKKAH) {
                "الآن يُقرأ في الحرم المكي الشريف 🕋"
            } else {
                "الآن يُقرأ في الحرم النبوي الشريف 🕌"
            }

            val contentText = "سورة ${nowPlaying.surahName}"
            val subText = if (isTest) "تجربة إشعار الحرم" else "بث حي ومباشر"

            val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(contentText)
                .setSubText(subText)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .setBigContentTitle(title)
                        .bigText("سورة ${nowPlaying.surahName} • تلاوة خاشعة مباركة من ${nowPlaying.location.arabicName}")
                )
                .setContentIntent(playPendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Respects DND
                .addAction(
                    android.R.drawable.ic_media_play,
                    "استمع الآن 🎧",
                    playPendingIntent
                )
                .addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "إيقاف هذه التنبيهات",
                    disablePendingIntent
                )

            nm.notify(notifId, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
}
