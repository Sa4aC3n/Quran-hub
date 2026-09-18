package com.example.prayer.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes as Media3AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * High-priority Foreground Service that plays the Adhan audio at exact prayer time
 * and shows an interactive notification on the lockscreen with an instant "إيقاف الأذان" (Stop Adhan) button.
 */
class AdhanForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "adhan_prayer_channel_v1"
        const val NOTIFICATION_ID = 99110

        const val ACTION_START_ADHAN = "com.example.prayer.ACTION_START_ADHAN"
        const val ACTION_STOP_ADHAN = "com.example.prayer.ACTION_STOP_ADHAN"
        const val ACTION_SNOOZE = "com.example.prayer.ACTION_SNOOZE"

        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_CITY_NAME = "extra_city_name"
        const val EXTRA_VOICE_ID = "extra_voice_id"
        const val EXTRA_VIBRATE_ONLY = "extra_vibrate_only"
    }

    private var exoPlayer: ExoPlayer? = null
    private val serviceJob = Job()
    private val scope = CoroutineScope(Dispatchers.Main + serviceJob)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_ADHAN

        when (action) {
            ACTION_STOP_ADHAN -> {
                stopAdhanAudio()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_SNOOZE -> {
                stopAdhanAudio()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_START_ADHAN -> {
                val prayerName = intent?.getStringExtra(EXTRA_PRAYER_NAME) ?: "الصلاة"
                val cityName = intent?.getStringExtra(EXTRA_CITY_NAME) ?: "موقعك الحالي"
                val voiceId = intent?.getStringExtra(EXTRA_VOICE_ID) ?: "makkah"
                val vibrateOnly = intent?.getBooleanExtra(EXTRA_VIBRATE_ONLY, false) ?: false

                val notification = buildAdhanNotification(prayerName, cityName)
                startForeground(NOTIFICATION_ID, notification)

                playAdhan(voiceId, vibrateOnly)
            }
        }

        return START_NOT_STICKY
    }

    private fun playAdhan(voiceId: String, vibrateOnly: Boolean) {
        stopAdhanAudio()

        // Trigger haptic vibration pattern
        triggerAdhanVibration()

        if (vibrateOnly) {
            // Auto finish after 15 seconds of vibration
            scope.launch {
                delay(15000)
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            }
            return
        }

        val voice = AdhanVoicesCatalog.getVoiceById(voiceId)

        try {
            exoPlayer = ExoPlayer.Builder(applicationContext)
                .setAudioAttributes(
                    Media3AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(C.USAGE_ALARM)
                        .build(),
                    true
                )
                .build().apply {
                    val mediaItem = MediaItem.fromUri(voice.audioUrl)
                    setMediaItem(mediaItem)
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_ENDED) {
                                stopForeground(STOP_FOREGROUND_DETACH)
                                stopSelf()
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            // Fallback to default alarm sound if streaming fails or network is offline
                            playFallbackAlarmSound()
                        }
                    })
                    prepare()
                    playWhenReady = true
                }
        } catch (e: Exception) {
            playFallbackAlarmSound()
        }
    }

    private fun playFallbackAlarmSound() {
        try {
            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            exoPlayer?.release()
            exoPlayer = ExoPlayer.Builder(applicationContext)
                .setAudioAttributes(
                    Media3AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(C.USAGE_ALARM)
                        .build(),
                    true
                )
                .build().apply {
                    val mediaItem = MediaItem.fromUri(alertUri)
                    setMediaItem(mediaItem)
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_ENDED) {
                                stopForeground(STOP_FOREGROUND_DETACH)
                                stopSelf()
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            stopForeground(STOP_FOREGROUND_DETACH)
                            stopSelf()
                        }
                    })
                    prepare()
                    playWhenReady = true
                }
        } catch (e: Exception) {
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }
    }

    private fun triggerAdhanVibration() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 500, 300, 500, 300, 800)
                val amplitudes = intArrayOf(0, 200, 0, 200, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 500, 300, 500, 300, 800), -1)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun stopAdhanAudio() {
        try {
            exoPlayer?.stop()
            exoPlayer?.release()
            exoPlayer = null
        } catch (e: Exception) {
            exoPlayer = null
        }
    }

    private fun buildAdhanNotification(prayerName: String, cityName: String): Notification {
        // App open pending intent
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Instant STOP action pending intent (Dispatched to StopAdhanReceiver directly without opening UI)
        val stopPendingIntent = PendingIntent.getBroadcast(
            this,
            1,
            Intent(this, StopAdhanReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🕌 الله أكبر • حان الآن موعد أذان $prayerName")
            .setContentText("الموقع: $cityName • حي على الصلاة، حي على الفلاح")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("حان الآن موعد أذان صلاة $prayerName في $cityName.\nاللهم رب هذه الدعوة التامة والصلاة القائمة آت سيدنا محمداً الوسيلة والفضيلة.")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                " إيقاف الأذان",
                stopPendingIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تنبيهات مواقيت الصلاة والأذان",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات الأذان المسموع وتنبيهات الإقامة ومواقيت الصلاة"
                enableVibration(true)
                enableLights(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopAdhanAudio()
        serviceJob.cancel()
        super.onDestroy()
    }
}
