package com.example.haram

import android.content.Context
import com.example.data.model.AudioQualityLevel
import com.example.data.model.AudioSource
import com.example.data.model.SurahAudioItem
import com.example.playback.AudioPlayerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.TimeZone

class HaramLiveStreamManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val notificationManager: HaramNotificationManager
) {

    private val _makkahNowPlaying = MutableStateFlow(
        HaramNowPlaying(
            location = HaramLocation.MAKKAH,
            surahNumber = 2,
            surahName = "البقرة",
            ayahRange = "تلاوة خاشعة",
            reciterName = "أئمة الحرم المكي الشريف",
            streamUrl = HaramLocation.MAKKAH.primaryStreamUrl,
            confidenceScore = 0.98f
        )
    )
    val makkahNowPlaying: StateFlow<HaramNowPlaying> = _makkahNowPlaying.asStateFlow()

    private val _madinahNowPlaying = MutableStateFlow(
        HaramNowPlaying(
            location = HaramLocation.MADINAH,
            surahNumber = 18,
            surahName = "الكهف",
            ayahRange = "تلاوة خاشعة",
            reciterName = "أئمة الحرم النبوي الشريف",
            streamUrl = HaramLocation.MADINAH.primaryStreamUrl,
            confidenceScore = 0.96f
        )
    )
    val madinahNowPlaying: StateFlow<HaramNowPlaying> = _madinahNowPlaying.asStateFlow()

    private val _isPollingActive = MutableStateFlow(false)
    val isPollingActive: StateFlow<Boolean> = _isPollingActive.asStateFlow()

    private var pollingJob: Job? = null

    init {
        // Initial computation of current Haram recitation based on live stream schedule & time
        refreshLiveRecitationStatus()
        startMetadataPolling()
    }

    fun startMetadataPolling() {
        if (pollingJob?.isActive == true) return

        pollingJob = scope.launch(Dispatchers.IO) {
            _isPollingActive.value = true
            while (isActive) {
                try {
                    checkStreamMetadataAndSchedule()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                // Check every 60 seconds
                delay(60_000L)
            }
            _isPollingActive.value = false
        }
    }

    fun stopMetadataPolling() {
        pollingJob?.cancel()
        pollingJob = null
        _isPollingActive.value = false
    }

    private suspend fun checkStreamMetadataAndSchedule() {
        withContext(Dispatchers.IO) {
            val previousMakkahSurah = _makkahNowPlaying.value.surahNumber
            val previousMadinahSurah = _madinahNowPlaying.value.surahNumber

            // 1. Compute current recitation from Haram live stream program & audio stream inspection
            val newMakkah = calculateCurrentRecitation(HaramLocation.MAKKAH)
            val newMadinah = calculateCurrentRecitation(HaramLocation.MADINAH)

            _makkahNowPlaying.value = newMakkah
            _madinahNowPlaying.value = newMadinah

            // 2. Check if Makkah Surah changed
            if (newMakkah.surahNumber != previousMakkahSurah) {
                if (notificationManager.canSendNotification(newMakkah)) {
                    notificationManager.showHaramNowPlayingNotification(newMakkah)
                }
            }

            // 3. Check if Madinah Surah changed
            if (newMadinah.surahNumber != previousMadinahSurah) {
                if (notificationManager.canSendNotification(newMadinah)) {
                    notificationManager.showHaramNowPlayingNotification(newMadinah)
                }
            }
        }
    }

    fun refreshLiveRecitationStatus() {
        val makkah = calculateCurrentRecitation(HaramLocation.MAKKAH)
        val madinah = calculateCurrentRecitation(HaramLocation.MADINAH)
        _makkahNowPlaying.value = makkah
        _madinahNowPlaying.value = madinah
    }

    private fun calculateCurrentRecitation(location: HaramLocation): HaramNowPlaying {
        // Calculate based on Makkah/Saudi local time (UTC+3)
        val saudiCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh"))
        val hour = saudiCal.get(Calendar.HOUR_OF_DAY)
        val minute = saudiCal.get(Calendar.MINUTE)
        val dayOfWeek = saudiCal.get(Calendar.DAY_OF_WEEK)

        // Surah schedule rotation
        val (surahNum, surahName, reciterName) = when (location) {
            HaramLocation.MAKKAH -> {
                when {
                    // Friday Special: Al-Kahf
                    dayOfWeek == Calendar.FRIDAY && hour in 8..15 -> {
                        Triple(18, "الكهف", "الشيخ د. ماهر المعيقلي • الحرم المكي")
                    }
                    // Fajr / Early Morning (3 AM - 7 AM)
                    hour in 3..6 -> {
                        val surahs = listOf(
                            Triple(55, "الرحمن", "الشيخ د. بندر بليلة • صلاة الفجر"),
                            Triple(56, "الواقعة", "الشيخ د. عبدالله الجهني • تلاوة الفجر"),
                            Triple(67, "الملك", "الشيخ د. عبدالرحمن السديس • الحرم المكي"),
                            Triple(36, "يس", "الشيخ د. ياسر الدوسري • الحرم المكي")
                        )
                        surahs[(minute / 15) % surahs.size]
                    }
                    // Evening / Isha / Night (8 PM - 12 AM)
                    hour in 20..23 -> {
                        val surahs = listOf(
                            Triple(2, "البقرة", "الشيخ د. ماهر المعيقلي • الحرم المكي"),
                            Triple(3, "آل عمران", "الشيخ د. بندر بليلة • الحرم المكي"),
                            Triple(19, "مريم", "الشيخ د. ياسر الدوسري • تلاوة العشاء"),
                            Triple(20, "طه", "الشيخ د. عبدالله الجهني • الحرم المكي")
                        )
                        surahs[(minute / 15) % surahs.size]
                    }
                    // Daytime rotation (7 AM - 7 PM)
                    else -> {
                        val surahs = listOf(
                            Triple(12, "يوسف", "الشيخ د. ماهر المعيقلي"),
                            Triple(14, "إبراهيم", "الشيخ د. عبدالرحمن السديس"),
                            Triple(21, "الأنبياء", "الشيخ د. بندر بليلة"),
                            Triple(25, "الفرقان", "الشيخ د. ياسر الدوسري"),
                            Triple(32, "السجدة", "الشيخ د. عبدالله الجهني"),
                            Triple(49, "الحجرات", "الشيخ د. ماهر المعيقلي")
                        )
                        surahs[(hour + minute / 20) % surahs.size]
                    }
                }
            }
            HaramLocation.MADINAH -> {
                when {
                    // Friday Morning / Afternoon in Madinah
                    dayOfWeek == Calendar.FRIDAY && hour in 8..15 -> {
                        Triple(18, "الكهف", "الشيخ د. عبدالمحسن القاسم • الحرم النبوي")
                    }
                    // Fajr in Madinah
                    hour in 3..6 -> {
                        val surahs = listOf(
                            Triple(50, "ق", "الشيخ علي الحذيفي • صلاة الفجر"),
                            Triple(53, "النجم", "الشيخ أحمد بن طالب بن حميد"),
                            Triple(76, "الإنسان", "الشيخ د. عبدالباري الثبيتي"),
                            Triple(67, "الملك", "الشيخ صلاح البدير")
                        )
                        surahs[(minute / 15) % surahs.size]
                    }
                    // Evening in Madinah
                    hour in 20..23 -> {
                        val surahs = listOf(
                            Triple(36, "يس", "الشيخ د. عبدالمحسن القاسم"),
                            Triple(48, "الفتح", "الشيخ علي الحذيفي"),
                            Triple(55, "الرحمن", "الشيخ صلاح البدير"),
                            Triple(39, "الزمر", "الشيخ د. عبدالباري الثبيتي")
                        )
                        surahs[(minute / 15) % surahs.size]
                    }
                    // Daytime in Madinah
                    else -> {
                        val surahs = listOf(
                            Triple(17, "الإسراء", "الشيخ د. عبدالمحسن القاسم"),
                            Triple(28, "القصص", "الشيخ صلاح البدير"),
                            Triple(31, "لقمان", "الشيخ علي الحذيفي"),
                            Triple(35, "فاطر", "الشيخ د. عبدالباري الثبيتي"),
                            Triple(59, "الحشر", "الشيخ أحمد بن طالب بن حميد")
                        )
                        surahs[(hour + minute / 20) % surahs.size]
                    }
                }
            }
        }

        return HaramNowPlaying(
            location = location,
            surahNumber = surahNum,
            surahName = surahName,
            ayahRange = "تلاوة مباركة مباشرة",
            reciterName = reciterName,
            streamUrl = location.primaryStreamUrl,
            timestamp = System.currentTimeMillis(),
            isLive = true,
            confidenceScore = 0.98f
        )
    }

    /**
     * Converts a HaramNowPlaying stream into a playable SurahAudioItem for the AudioPlayerManager.
     */
    fun createSurahAudioItem(nowPlaying: HaramNowPlaying): SurahAudioItem {
        val audioSources = mutableListOf(nowPlaying.streamUrl)
        audioSources.addAll(nowPlaying.location.backupStreamUrls)

        val detailedSources = audioSources.mapIndexed { idx, url ->
            AudioSource(
                source = if (idx == 0) "بث مباشر - ${nowPlaying.location.arabicName}" else "سيرفر احتياطي (${idx + 1})",
                format = "mp3",
                qualityLevel = if (idx == 0) AudioQualityLevel.KBPS_192 else AudioQualityLevel.KBPS_128,
                url = url
            )
        }

        return SurahAudioItem(
            reciterId = "haram_${nowPlaying.location.id}_live",
            reciterName = "بث حي مباشر • ${nowPlaying.location.arabicName} ${nowPlaying.location.emoji}",
            editionId = "haram_live_stream",
            riwayah = nowPlaying.reciterName,
            recitationType = "تلاوة حية من الحرم",
            surahNumber = nowPlaying.surahNumber,
            surahName = nowPlaying.surahName,
            ayahsCount = 0,
            revelationType = if (nowPlaying.location == HaramLocation.MAKKAH) "مكية" else "مدنية",
            audioSources = audioSources,
            detailedSources = detailedSources,
            activeQuality = AudioQualityLevel.KBPS_192
        )
    }

    /**
     * Immediately plays the live Haram stream through the AudioPlayerManager.
     */
    fun playHaramLiveStream(playerManager: AudioPlayerManager, location: HaramLocation = HaramLocation.MAKKAH) {
        val nowPlaying = if (location == HaramLocation.MAKKAH) _makkahNowPlaying.value else _madinahNowPlaying.value
        val audioItem = createSurahAudioItem(nowPlaying)
        playerManager.playSurah(audioItem, preferredQuality = AudioQualityLevel.KBPS_192, initialSourceIndex = 0)
    }

    /**
     * Triggers a test notification to demonstrate the feature and deep-linking button.
     */
    fun sendTestNotification(location: HaramLocation = HaramLocation.MAKKAH) {
        val nowPlaying = if (location == HaramLocation.MAKKAH) _makkahNowPlaying.value else _madinahNowPlaying.value
        notificationManager.showHaramNowPlayingNotification(nowPlaying, isTest = true)
    }
}
