package com.example.prayer.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.MainActivity
import com.example.prayer.calculator.PrayerTimesCalculator
import com.example.prayer.model.AsrJuristic
import com.example.prayer.model.CalculationMethod
import com.example.prayer.model.LocationInfo
import com.example.prayer.model.PrayerNotificationSettings
import com.example.prayer.model.PrayerTimeOffsets
import com.example.prayer.model.PrayerType
import java.util.Calendar

/**
 * Schedules exact alarms for upcoming prayer times using AlarmManager.
 * Ensures notifications and Adhan trigger reliably even in Doze / Standby mode.
 */
class AdhanAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedules alarms for all enabled prayers for the next 7 days.
     */
    fun scheduleAllPrayers(
        location: LocationInfo,
        method: CalculationMethod,
        asrJuristic: AsrJuristic,
        offsets: PrayerTimeOffsets,
        settings: PrayerNotificationSettings
    ) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        // Schedule for next 7 days
        for (dayOffset in 0..6) {
            val dayCal = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }

            val times = PrayerTimesCalculator.calculate(
                calendar = dayCal,
                latitude = location.latitude,
                longitude = location.longitude,
                method = method,
                asrJuristic = asrJuristic,
                offsets = offsets
            )

            scheduleSinglePrayerIfFuture(PrayerType.FAJR, times.fajr, location, settings, now)
            scheduleSinglePrayerIfFuture(PrayerType.DHUHR, times.dhuhr, location, settings, now)
            scheduleSinglePrayerIfFuture(PrayerType.ASR, times.asr, location, settings, now)
            scheduleSinglePrayerIfFuture(PrayerType.MAGHRIB, times.maghrib, location, settings, now)
            scheduleSinglePrayerIfFuture(PrayerType.ISHA, times.isha, location, settings, now)
        }
    }

    private fun scheduleSinglePrayerIfFuture(
        type: PrayerType,
        timeMillis: Long,
        location: LocationInfo,
        settings: PrayerNotificationSettings,
        now: Long
    ) {
        if (timeMillis <= now) return
        val isEnabled = settings.enabledPrayers[type] ?: true
        if (!isEnabled) return

        val intent = Intent(context, AdhanAlarmReceiver::class.java).apply {
            action = AdhanAlarmReceiver.ACTION_PRAYER_ALARM
            putExtra(AdhanAlarmReceiver.EXTRA_PRAYER_TYPE, type.id)
            putExtra(AdhanAlarmReceiver.EXTRA_PRAYER_NAME, type.arabicName)
            putExtra(AdhanAlarmReceiver.EXTRA_CITY_NAME, location.cityName)
            putExtra(AdhanAlarmReceiver.EXTRA_VOICE_ID, settings.selectedVoiceId)
            putExtra(AdhanAlarmReceiver.EXTRA_VIBRATE_ONLY, settings.vibrateOnly)
            val iqamahMinutes = if (settings.iqamahSettings.enabled) settings.iqamahSettings.getMinutesForPrayer(type) else 0
            putExtra(AdhanAlarmReceiver.EXTRA_IQAMAH_MINUTES, iqamahMinutes)
        }

        val requestCode = generateRequestCode(type, timeMillis)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        timeMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        timeMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    timeMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            // Fallback gracefully without crash
            try {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    timeMillis,
                    pendingIntent
                )
            } catch (_: Exception) {}
        }
    }

    fun cancelAllPrayers() {
        // Can cancel known request codes or re-schedule with updated filters
    }

    private fun generateRequestCode(type: PrayerType, timeMillis: Long): Int {
        val cal = Calendar.getInstance().apply { this.timeInMillis = timeMillis }
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val typeOrdinal = type.ordinal
        return (dayOfYear * 10) + typeOrdinal
    }
}
