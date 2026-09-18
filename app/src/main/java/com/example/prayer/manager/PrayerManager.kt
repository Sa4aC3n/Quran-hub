package com.example.prayer.manager

import android.content.Context
import android.content.SharedPreferences
import androidx.media3.common.AudioAttributes as Media3AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.prayer.calculator.HijriCalendarHelper
import com.example.prayer.calculator.PrayerTimesCalculator
import com.example.prayer.calculator.QiblaCalculator
import com.example.prayer.location.PrayerLocationManager
import com.example.prayer.model.AsrJuristic
import com.example.prayer.model.CalculationMethod
import com.example.prayer.model.DayPrayerTimes
import com.example.prayer.model.IqamahSettings
import com.example.prayer.model.LocationInfo
import com.example.prayer.model.PrayerNotificationSettings
import com.example.prayer.model.PrayerTimeItem
import com.example.prayer.model.PrayerTimeOffsets
import com.example.prayer.model.PrayerType
import com.example.prayer.model.QiblaData
import com.example.prayer.notification.AdhanAlarmScheduler
import com.example.prayer.notification.AdhanVoicesCatalog
import com.example.prayer.sensor.CompassSensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Main coordinator for Prayer Times, Hijri Calendar, Qibla Compass, and Adhan scheduling.
 */
class PrayerManager(
    context: Context,
    private val externalScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {

    private val appContext: Context = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences("prayer_settings_prefs", Context.MODE_PRIVATE)

    val locationManager = PrayerLocationManager(appContext)
    val compassSensorManager = CompassSensorManager(appContext)
    private val alarmScheduler = AdhanAlarmScheduler(appContext)

    // Configuration Flows
    private val _calculationMethod = MutableStateFlow(loadCalculationMethod())
    val calculationMethod: StateFlow<CalculationMethod> = _calculationMethod.asStateFlow()

    private val _asrJuristic = MutableStateFlow(loadAsrJuristic())
    val asrJuristic: StateFlow<AsrJuristic> = _asrJuristic.asStateFlow()

    private val _prayerOffsets = MutableStateFlow(loadPrayerOffsets())
    val prayerOffsets: StateFlow<PrayerTimeOffsets> = _prayerOffsets.asStateFlow()

    private val _notificationSettings = MutableStateFlow(loadNotificationSettings())
    val notificationSettings: StateFlow<PrayerNotificationSettings> = _notificationSettings.asStateFlow()

    // Calculated Prayer Times State
    private val _dayPrayerTimes = MutableStateFlow(calculateCurrentDayPrayerTimes())
    val dayPrayerTimes: StateFlow<DayPrayerTimes> = _dayPrayerTimes.asStateFlow()

    // Real-time Qibla state
    private val _qiblaData = MutableStateFlow(computeQiblaData(0f))
    val qiblaData: StateFlow<QiblaData> = _qiblaData.asStateFlow()

    private var previewPlayer: ExoPlayer? = null
    private val _isPlayingPreview = MutableStateFlow<String?>(null)
    val isPlayingPreview: StateFlow<String?> = _isPlayingPreview.asStateFlow()

    private var tickerJob: Job? = null

    init {
        // Start second/minute ticker for real-time prayer countdown updates
        startPrayerTicker()

        // Combine compass sensor heading with Qibla calculations
        externalScope.launch {
            compassSensorManager.azimuthFlow.collect { heading ->
                val qibla = computeQiblaData(heading)
                _qiblaData.value = qibla
            }
        }

        // Listen for location changes to recompute
        externalScope.launch {
            locationManager.currentLocation.collect { loc ->
                compassSensorManager.setLocation(loc.latitude, loc.longitude)
                recalculate()
            }
        }

        // Reschedule alarms on init
        rescheduleAlarms()
    }

    private fun startPrayerTicker() {
        tickerJob?.cancel()
        tickerJob = externalScope.launch {
            while (isActive) {
                _dayPrayerTimes.value = calculateCurrentDayPrayerTimes()
                delay(1000) // 1 sec tick
            }
        }
    }

    fun recalculate() {
        _dayPrayerTimes.value = calculateCurrentDayPrayerTimes()
        _qiblaData.value = computeQiblaData(compassSensorManager.azimuthFlow.value)
        rescheduleAlarms()
    }

    fun rescheduleAlarms() {
        val loc = locationManager.currentLocation.value
        alarmScheduler.scheduleAllPrayers(
            location = loc,
            method = _calculationMethod.value,
            asrJuristic = _asrJuristic.value,
            offsets = _prayerOffsets.value,
            settings = _notificationSettings.value
        )
    }

    fun calculateCurrentDayPrayerTimes(): DayPrayerTimes {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        val location = locationManager.currentLocation.value

        val todayTimes = PrayerTimesCalculator.calculate(
            calendar = calendar,
            latitude = location.latitude,
            longitude = location.longitude,
            method = _calculationMethod.value,
            asrJuristic = _asrJuristic.value,
            offsets = _prayerOffsets.value
        )

        // Tomorrow's Fajr in case today's Isha has passed
        val tomorrowCal = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val tomorrowTimes = PrayerTimesCalculator.calculate(
            calendar = tomorrowCal,
            latitude = location.latitude,
            longitude = location.longitude,
            method = _calculationMethod.value,
            asrJuristic = _asrJuristic.value,
            offsets = _prayerOffsets.value
        )

        val prayerEntries = listOf(
            PrayerType.FAJR to todayTimes.fajr,
            PrayerType.SUNRISE to todayTimes.sunrise,
            PrayerType.DHUHR to todayTimes.dhuhr,
            PrayerType.ASR to todayTimes.asr,
            PrayerType.MAGHRIB to todayTimes.maghrib,
            PrayerType.ISHA to todayTimes.isha
        )

        var nextItem: PrayerTimeItem? = null
        var currentItem: PrayerTimeItem? = null
        var timeUntilNext = 0L

        // Find next prayer today
        val nextPair = prayerEntries.find { it.second > now }
        val (nextType, nextMillis) = if (nextPair != null) {
            nextPair
        } else {
            // All today prayers passed -> next is Tomorrow's Fajr
            PrayerType.FAJR to tomorrowTimes.fajr
        }

        // Find current (last passed) prayer
        val passedEntries = prayerEntries.filter { it.second <= now }
        val currentPair = passedEntries.lastOrNull()

        val items = prayerEntries.map { (type, timeMillis) ->
            val isPassed = timeMillis < now
            val isNext = type == nextType && (nextPair != null || (type == PrayerType.FAJR && passedEntries.size == 6))
            val isCurrent = currentPair?.first == type
            val isEnabled = _notificationSettings.value.enabledPrayers[type] ?: (type != PrayerType.SUNRISE)

            val item = PrayerTimeItem(
                type = type,
                timeFormatted = HijriCalendarHelper.formatTimeArabic(timeMillis),
                timeMillis = timeMillis,
                isPassed = isPassed,
                isCurrent = isCurrent,
                isNext = isNext,
                isNotificationEnabled = isEnabled,
                minutesOffset = _prayerOffsets.value.getOffset(type)
            )

            if (isNext) nextItem = item
            if (isCurrent) currentItem = item

            item
        }

        if (nextItem == null) {
            nextItem = PrayerTimeItem(
                type = PrayerType.FAJR,
                timeFormatted = HijriCalendarHelper.formatTimeArabic(tomorrowTimes.fajr),
                timeMillis = tomorrowTimes.fajr,
                isPassed = false,
                isCurrent = false,
                isNext = true,
                isNotificationEnabled = _notificationSettings.value.enabledPrayers[PrayerType.FAJR] ?: true,
                minutesOffset = _prayerOffsets.value.fajr
            )
        }

        timeUntilNext = (nextMillis - now).coerceAtLeast(0L)

        // Calculate progress percentage between current and next prayer
        val currentMillis = currentItem?.timeMillis ?: (todayTimes.fajr - 6 * 3600 * 1000L)
        val totalInterval = (nextMillis - currentMillis).coerceAtLeast(1000L)
        val elapsed = (now - currentMillis).coerceAtLeast(0L)
        val progress = (elapsed.toFloat() / totalInterval.toFloat()).coerceIn(0f, 1f)

        val hijriDate = HijriCalendarHelper.getHijriDate(calendar)
        val gregorianDate = HijriCalendarHelper.formatGregorianArabic(calendar)

        return DayPrayerTimes(
            dateGregorian = gregorianDate,
            dateHijri = hijriDate.formatFormattedArabic(),
            prayers = items,
            nextPrayer = nextItem,
            currentPrayer = currentItem,
            timeUntilNextMillis = timeUntilNext,
            progressPercent = progress,
            location = location
        )
    }

    private fun computeQiblaData(userHeading: Float): QiblaData {
        val loc = locationManager.currentLocation.value
        val qiblaAngle = QiblaCalculator.calculateQiblaBearing(loc.latitude, loc.longitude)
        val relativeAngle = QiblaCalculator.calculateRelativeAngle(userHeading, qiblaAngle)
        val distanceKm = QiblaCalculator.calculateDistanceToMecca(loc.latitude, loc.longitude)
        val isAligned = QiblaCalculator.isPointingTowardsQibla(userHeading, qiblaAngle, 3.5f)

        return QiblaData(
            qiblaAngle = qiblaAngle,
            userHeading = userHeading,
            relativeAngle = relativeAngle,
            distanceKm = distanceKm,
            isAligned = isAligned,
            accuracy = compassSensorManager.sensorAccuracyFlow.value,
            isSensorAvailable = compassSensorManager.isSensorAvailableFlow.value
        )
    }

    // Actions
    fun setCalculationMethod(method: CalculationMethod) {
        _calculationMethod.value = method
        prefs.edit().putString("calc_method", method.id).apply()
        recalculate()
    }

    fun setAsrJuristic(juristic: AsrJuristic) {
        _asrJuristic.value = juristic
        prefs.edit().putString("asr_juristic", juristic.id).apply()
        recalculate()
    }

    fun togglePrayerNotification(type: PrayerType) {
        val current = _notificationSettings.value
        val enabledMap = current.enabledPrayers.toMutableMap()
        val currentVal = enabledMap[type] ?: true
        enabledMap[type] = !currentVal
        val updated = current.copy(enabledPrayers = enabledMap)
        _notificationSettings.value = updated
        saveNotificationSettings(updated)
        recalculate()
    }

    fun setAdhanVoice(voiceId: String) {
        val current = _notificationSettings.value
        val updated = current.copy(selectedVoiceId = voiceId)
        _notificationSettings.value = updated
        saveNotificationSettings(updated)
        recalculate()
    }

    fun setVibrateOnly(vibrateOnly: Boolean) {
        val current = _notificationSettings.value
        val updated = current.copy(vibrateOnly = vibrateOnly)
        _notificationSettings.value = updated
        saveNotificationSettings(updated)
        recalculate()
    }

    fun setIqamahSettings(settings: IqamahSettings) {
        val current = _notificationSettings.value
        val updated = current.copy(iqamahSettings = settings)
        _notificationSettings.value = updated
        saveNotificationSettings(updated)
        recalculate()
    }

    fun setPrayerOffsets(offsets: PrayerTimeOffsets) {
        _prayerOffsets.value = offsets
        prefs.edit()
            .putInt("offset_fajr", offsets.fajr)
            .putInt("offset_sunrise", offsets.sunrise)
            .putInt("offset_dhuhr", offsets.dhuhr)
            .putInt("offset_asr", offsets.asr)
            .putInt("offset_maghrib", offsets.maghrib)
            .putInt("offset_isha", offsets.isha)
            .apply()
        recalculate()
    }

    suspend fun refreshLocationGps(): LocationInfo {
        val loc = locationManager.refreshCurrentLocationGps()
        val defaultMethod = PrayerTimesCalculator.getDefaultMethodForCountry(loc.countryName)
        // If user hasn't explicitly customized, set appropriate calculation method
        if (!prefs.contains("calc_method")) {
            _calculationMethod.value = defaultMethod
        }
        recalculate()
        return loc
    }

    fun selectPresetCity(preset: PrayerLocationManager.CityPreset) {
        locationManager.setManualCity(preset)
        val defaultMethod = CalculationMethod.entries.find { it.id == preset.defaultMethodId }
            ?: PrayerTimesCalculator.getDefaultMethodForCountry(preset.countryAr)
        _calculationMethod.value = defaultMethod
        prefs.edit().putString("calc_method", defaultMethod.id).apply()
        recalculate()
    }

    fun startCompass() {
        val loc = locationManager.currentLocation.value
        compassSensorManager.setLocation(loc.latitude, loc.longitude)
        compassSensorManager.startListening()
    }

    fun stopCompass() {
        compassSensorManager.stopListening()
    }

    fun playVoicePreview(voiceId: String) {
        if (_isPlayingPreview.value == voiceId) {
            stopVoicePreview()
            return
        }

        stopVoicePreview()
        val voice = AdhanVoicesCatalog.getVoiceById(voiceId)
        try {
            previewPlayer = ExoPlayer.Builder(appContext)
                .setAudioAttributes(
                    Media3AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    true
                )
                .build().apply {
                    val mediaItem = MediaItem.fromUri(voice.audioUrl)
                    setMediaItem(mediaItem)
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_ENDED) {
                                stopVoicePreview()
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            stopVoicePreview()
                        }
                    })
                    prepare()
                    playWhenReady = true
                    _isPlayingPreview.value = voiceId
                }
        } catch (e: Exception) {
            _isPlayingPreview.value = null
        }
    }

    fun stopVoicePreview() {
        try {
            previewPlayer?.stop()
            previewPlayer?.release()
            previewPlayer = null
        } catch (e: Exception) {
            previewPlayer = null
        }
        _isPlayingPreview.value = null
    }

    // Prefs persistence loaders
    private fun loadCalculationMethod(): CalculationMethod {
        val id = prefs.getString("calc_method", null)
        if (id != null) {
            return CalculationMethod.entries.find { it.id == id } ?: CalculationMethod.EGYPTIAN
        }
        val loc = locationManager.currentLocation.value
        return PrayerTimesCalculator.getDefaultMethodForCountry(loc.countryName)
    }

    private fun loadAsrJuristic(): AsrJuristic {
        val id = prefs.getString("asr_juristic", "standard")
        return AsrJuristic.entries.find { it.id == id } ?: AsrJuristic.STANDARD
    }

    private fun loadPrayerOffsets(): PrayerTimeOffsets {
        return PrayerTimeOffsets(
            fajr = prefs.getInt("offset_fajr", 0),
            sunrise = prefs.getInt("offset_sunrise", 0),
            dhuhr = prefs.getInt("offset_dhuhr", 0),
            asr = prefs.getInt("offset_asr", 0),
            maghrib = prefs.getInt("offset_maghrib", 0),
            isha = prefs.getInt("offset_isha", 0)
        )
    }

    private fun loadNotificationSettings(): PrayerNotificationSettings {
        val voiceId = prefs.getString("voice_id", "makkah") ?: "makkah"
        val vibrateOnly = prefs.getBoolean("vibrate_only", false)
        val iqamahEnabled = prefs.getBoolean("iqamah_enabled", true)
        val fajrIqamah = prefs.getInt("iqamah_fajr", 20)
        val dhuhrIqamah = prefs.getInt("iqamah_dhuhr", 15)
        val asrIqamah = prefs.getInt("iqamah_asr", 15)
        val maghribIqamah = prefs.getInt("iqamah_maghrib", 10)
        val ishaIqamah = prefs.getInt("iqamah_isha", 15)

        val enabledMap = mutableMapOf<PrayerType, Boolean>()
        for (type in PrayerType.entries) {
            val def = type != PrayerType.SUNRISE
            enabledMap[type] = prefs.getBoolean("notif_${type.id}", def)
        }

        return PrayerNotificationSettings(
            enabledPrayers = enabledMap,
            selectedVoiceId = voiceId,
            vibrateOnly = vibrateOnly,
            iqamahSettings = IqamahSettings(
                enabled = iqamahEnabled,
                fajrMinutes = fajrIqamah,
                dhuhrMinutes = dhuhrIqamah,
                asrMinutes = asrIqamah,
                maghribMinutes = maghribIqamah,
                ishaMinutes = ishaIqamah
            )
        )
    }

    private fun saveNotificationSettings(settings: PrayerNotificationSettings) {
        val editor = prefs.edit()
            .putString("voice_id", settings.selectedVoiceId)
            .putBoolean("vibrate_only", settings.vibrateOnly)
            .putBoolean("iqamah_enabled", settings.iqamahSettings.enabled)
            .putInt("iqamah_fajr", settings.iqamahSettings.fajrMinutes)
            .putInt("iqamah_dhuhr", settings.iqamahSettings.dhuhrMinutes)
            .putInt("iqamah_asr", settings.iqamahSettings.asrMinutes)
            .putInt("iqamah_maghrib", settings.iqamahSettings.maghribMinutes)
            .putInt("iqamah_isha", settings.iqamahSettings.ishaMinutes)

        for ((type, enabled) in settings.enabledPrayers) {
            editor.putBoolean("notif_${type.id}", enabled)
        }
        editor.apply()
    }
}
