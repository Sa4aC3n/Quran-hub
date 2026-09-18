package com.example.watch

import android.Manifest
import android.app.NotificationManager
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DetectedCalendarMeeting(
    val title: String,
    val startTimeEpochMs: Long,
    val endTimeEpochMs: Long,
    val isOngoing: Boolean
)

class CalendarMeetingDetector(context: Context) {

    private val appContext = context.applicationContext

    /**
     * Checks if the device is currently in Do Not Disturb (DND) / Zen Mode
     */
    fun isDoNotDisturbActive(): Boolean {
        return try {
            val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager != null) {
                val currentFilter = notificationManager.currentInterruptionFilter
                currentFilter == NotificationManager.INTERRUPTION_FILTER_NONE ||
                currentFilter == NotificationManager.INTERRUPTION_FILTER_ALARMS ||
                currentFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Checks if the user has granted calendar permission
     */
    fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Queries the user's Android Calendar for ongoing or upcoming meetings in the current hour window
     */
    suspend fun findCurrentOngoingMeeting(): DetectedCalendarMeeting? = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) return@withContext null

        var cursor: Cursor? = null
        try {
            val now = System.currentTimeMillis()
            val windowEnd = now + (60 * 1000) // current window

            val builder: Uri.Builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, now - 60000)
            ContentUris.appendId(builder, windowEnd)

            val projection = arrayOf(
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY
            )

            cursor = appContext.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val title = cursor.getString(0) ?: "اجتماع عمل"
                    val start = cursor.getLong(1)
                    val end = cursor.getLong(2)
                    val isAllDay = cursor.getInt(3) == 1

                    if (!isAllDay && now in start..end) {
                        return@withContext DetectedCalendarMeeting(
                            title = title,
                            startTimeEpochMs = start,
                            endTimeEpochMs = end,
                            isOngoing = true
                        )
                    }
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        null
    }
}
