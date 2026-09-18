package com.example.prayer.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receiver invoked when the user clicks the "إيقاف الأذان" (Stop Adhan) button
 * directly on the lockscreen/notification banner.
 * Stops playback immediately without opening the application.
 */
class StopAdhanReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val stopIntent = Intent(context, AdhanForegroundService::class.java).apply {
            action = AdhanForegroundService.ACTION_STOP_ADHAN
        }
        context.startService(stopIntent)
    }
}
