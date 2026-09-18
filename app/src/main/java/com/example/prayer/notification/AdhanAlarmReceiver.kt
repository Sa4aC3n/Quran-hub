package com.example.prayer.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.prayer.model.PrayerType

/**
 * Receiver invoked by AlarmManager when an exact prayer time arrives.
 */
class AdhanAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_PRAYER_ALARM = "com.example.prayer.ACTION_PRAYER_ALARM"
        const val ACTION_IQAMAH_ALARM = "com.example.prayer.ACTION_IQAMAH_ALARM"

        const val EXTRA_PRAYER_TYPE = "extra_prayer_type"
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_CITY_NAME = "extra_city_name"
        const val EXTRA_VOICE_ID = "extra_voice_id"
        const val EXTRA_VIBRATE_ONLY = "extra_vibrate_only"
        const val EXTRA_IQAMAH_MINUTES = "extra_iqamah_minutes"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: ACTION_PRAYER_ALARM

        if (action == ACTION_IQAMAH_ALARM) {
            // Handle Iqamah reminder notification
            val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "الصلاة"
            showIqamahNotification(context, prayerName)
            return
        }

        val prayerTypeId = intent.getStringExtra(EXTRA_PRAYER_TYPE) ?: "fajr"
        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "الفجر"
        val cityName = intent.getStringExtra(EXTRA_CITY_NAME) ?: "موقعك الحالي"
        val voiceId = intent.getStringExtra(EXTRA_VOICE_ID) ?: "makkah"
        val vibrateOnly = intent.getBooleanExtra(EXTRA_VIBRATE_ONLY, false)
        val iqamahMinutes = intent.getIntExtra(EXTRA_IQAMAH_MINUTES, 0)

        // Start Foreground Service to play Adhan & show interactive lockscreen notification
        val serviceIntent = Intent(context, AdhanForegroundService::class.java).apply {
            this.action = AdhanForegroundService.ACTION_START_ADHAN
            putExtra(AdhanForegroundService.EXTRA_PRAYER_NAME, prayerName)
            putExtra(AdhanForegroundService.EXTRA_CITY_NAME, cityName)
            putExtra(AdhanForegroundService.EXTRA_VOICE_ID, voiceId)
            putExtra(AdhanForegroundService.EXTRA_VIBRATE_ONLY, vibrateOnly)
        }

        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            // Fallback if background service start is restricted
        }

        // If Iqamah reminder is set, schedule Iqamah alarm
        if (iqamahMinutes > 0) {
            scheduleIqamahAlarm(context, prayerName, iqamahMinutes)
        }
    }

    private fun scheduleIqamahAlarm(context: Context, prayerName: String, delayMinutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val iqamahIntent = Intent(context, AdhanAlarmReceiver::class.java).apply {
            action = ACTION_IQAMAH_ALARM
            putExtra(EXTRA_PRAYER_NAME, prayerName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            prayerName.hashCode(),
            iqamahIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = System.currentTimeMillis() + (delayMinutes * 60 * 1000L)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun showIqamahNotification(context: Context, prayerName: String) {
        val channelId = "iqamah_channel"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "تنبيهات إقامة الصلاة",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيه خفيف باقتراب إقامة الصلاة في المسجد"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(" قد قامت الصلاة • صلاة $prayerName")
            .setContentText("حان الآن وقت إقامة صلاة $prayerName في المسجد • استعد للصلاة")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        manager.notify(prayerName.hashCode() + 100, notif)
    }
}
