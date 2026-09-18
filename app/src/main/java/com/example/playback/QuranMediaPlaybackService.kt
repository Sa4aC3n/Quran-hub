package com.example.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.R

@OptIn(UnstableApi::class)
class QuranMediaPlaybackService : MediaSessionService() {

    companion object {
        const val CHANNEL_ID = "quran_playback_channel"
        const val NOTIFICATION_ID = 114

        @Volatile
        var currentMediaSession: MediaSession? = null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        try {
            val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(CHANNEL_ID)
                .setNotificationId(NOTIFICATION_ID)
                .build()
            setMediaNotificationProvider(notificationProvider)
        } catch (_: Exception) {}
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تشغيل القرآن الكريم",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعارات التحكم في تشغيل تلاوات القرآن الكريم في الخلفية وشاشة القفل"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return currentMediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = currentMediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        currentMediaSession = null
        super.onDestroy()
    }
}
