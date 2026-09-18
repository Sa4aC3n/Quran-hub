package com.example.haram

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Handles instant one-tap disable action directly from the notification.
 * Allows users to turn off Haram notifications immediately without needing to open the app settings.
 */
class HaramNotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DISABLE_HARAM_NOTIFICATIONS = "com.example.haram.ACTION_DISABLE_HARAM_NOTIFICATIONS"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_DISABLE_HARAM_NOTIFICATIONS) {
            try {
                // 1. Disable Haram notifications in preferences
                val prefs = context.getSharedPreferences("haram_notification_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("is_enabled", false).apply()

                // 2. Cancel the active notification
                val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, HaramNotificationManager.NOTIFICATION_ID_BASE)
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.cancel(notifId)

                // 3. Provide immediate gentle feedback
                Toast.makeText(
                    context,
                    "تم إيقاف تنبيهات (الآن يُقرأ في الحرم) بنجاح 🕋. يمكنك إعادة تفعيلها دائماً من الإعدادات.",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
