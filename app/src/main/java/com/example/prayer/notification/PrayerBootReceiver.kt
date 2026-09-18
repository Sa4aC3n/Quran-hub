package com.example.prayer.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.prayer.manager.PrayerManager

/**
 * Reschedules all prayer alarms when device restarts or time settings change.
 */
class PrayerBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            try {
                val prayerManager = PrayerManager(context)
                prayerManager.rescheduleAlarms()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
