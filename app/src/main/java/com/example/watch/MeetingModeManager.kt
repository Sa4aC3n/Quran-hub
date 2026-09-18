package com.example.watch

import android.content.Context
import com.example.data.model.HapticVibrationPattern
import com.example.data.model.MeetingModeState
import com.example.data.model.NotificationCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MeetingModeManager(
    private val context: Context,
    private val scope: CoroutineScope,
    val hapticManager: HapticVibrationManager = HapticVibrationManager(context),
    val calendarDetector: CalendarMeetingDetector = CalendarMeetingDetector(context)
) {
    private val _meetingState = MutableStateFlow(MeetingModeState())
    val meetingState: StateFlow<MeetingModeState> = _meetingState.asStateFlow()

    private var meetingTimerJob: Job? = null
    private var autoDetectJob: Job? = null

    var onMeetingActivatedAction: ((isManual: Boolean) -> Unit)? = null
    var onMeetingEndedAction: ((lastSurah: Int?, lastAyah: Int?, reciter: String?) -> Unit)? = null

    init {
        startAutoDetectPeriodicChecker()
    }

    /**
     * Toggles meeting mode manually (Indefinite or until stopped)
     */
    fun toggleManualMeetingMode() {
        if (_meetingState.value.isEnabled) {
            stopMeetingMode(triggeredByUser = true)
        } else {
            startMeetingMode(durationMinutes = null, meetingTitle = "اجتماع يدوي")
        }
    }

    /**
     * Start meeting mode with a specific timer (e.g. 15, 30, 45, 60 minutes)
     */
    fun startMeetingTimer(durationMinutes: Int, meetingTitle: String = "اجتماع محدد المدة") {
        startMeetingMode(durationMinutes = durationMinutes, meetingTitle = meetingTitle)
    }

    /**
     * Internal activation of meeting mode
     */
    fun startMeetingMode(
        durationMinutes: Int?,
        meetingTitle: String = "اجتماع جاري"
    ) {
        val endTimeEpoch = if (durationMinutes != null && durationMinutes > 0) {
            System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        } else {
            null
        }

        _meetingState.update { current ->
            current.copy(
                isEnabled = true,
                activeMeetingTitle = meetingTitle,
                remainingDurationMinutes = durationMinutes,
                meetingEndTimeEpochMs = endTimeEpoch
            )
        }

        // Trigger double gentle haptic confirming silence
        hapticManager.triggerHaptic(HapticVibrationPattern.PLAYBACK_PAUSED_ON_MEETING)

        // Invoke playback pause callback
        onMeetingActivatedAction?.invoke(true)

        // Cancel previous countdown
        meetingTimerJob?.cancel()
        if (durationMinutes != null && durationMinutes > 0) {
            meetingTimerJob = scope.launch(Dispatchers.Default) {
                while (isActive) {
                    val remainingMs = (endTimeEpoch ?: 0L) - System.currentTimeMillis()
                    if (remainingMs <= 0) {
                        // Meeting ended
                        stopMeetingMode(triggeredByUser = false)
                        break
                    }
                    val remainingMins = (remainingMs / 60000L).toInt() + 1
                    _meetingState.update { it.copy(remainingDurationMinutes = remainingMins) }
                    delay(15000L) // Update every 15s
                }
            }
        }
    }

    /**
     * Ends meeting mode and prompts to resume if configured
     */
    fun stopMeetingMode(triggeredByUser: Boolean = false) {
        meetingTimerJob?.cancel()
        meetingTimerJob = null

        val lastSurah = _meetingState.value.lastStoppedSurahNumber
        val lastAyah = _meetingState.value.lastStoppedAyahNumber
        val lastReciter = _meetingState.value.lastStoppedReciterName

        _meetingState.update { current ->
            current.copy(
                isEnabled = false,
                activeMeetingTitle = null,
                remainingDurationMinutes = null,
                meetingEndTimeEpochMs = null
            )
        }

        // Trigger gentle resume haptic
        hapticManager.triggerHaptic(HapticVibrationPattern.MEETING_ENDED_RESUME)

        if (_meetingState.value.autoResumePromptAfterMeeting) {
            onMeetingEndedAction?.invoke(lastSurah, lastAyah, lastReciter)
        }
    }

    /**
     * Stores playback snapshot so it can be resumed after meeting ends
     */
    fun savePlaybackSnapshot(
        surahNumber: Int?,
        surahName: String?,
        ayahNumber: Int?,
        reciterName: String?,
        positionMs: Long?
    ) {
        _meetingState.update { current ->
            current.copy(
                lastStoppedSurahNumber = surahNumber ?: current.lastStoppedSurahNumber,
                lastStoppedSurahName = surahName ?: current.lastStoppedSurahName,
                lastStoppedAyahNumber = ayahNumber ?: current.lastStoppedAyahNumber,
                lastStoppedReciterName = reciterName ?: current.lastStoppedReciterName,
                lastStoppedPositionMs = positionMs ?: current.lastStoppedPositionMs
            )
        }
    }

    fun setAutoCalendarDetect(enabled: Boolean) {
        _meetingState.update { it.copy(isAutoCalendarDetectEnabled = enabled) }
        if (enabled) {
            triggerInstantCalendarCheck()
        }
    }

    fun setDndSync(enabled: Boolean) {
        _meetingState.update { it.copy(isDndSyncEnabled = enabled) }
        if (enabled) {
            triggerInstantDndCheck()
        }
    }

    fun setPausePlaybackOnMeeting(enabled: Boolean) {
        _meetingState.update { it.copy(pausePlaybackOnMeeting = enabled) }
    }

    fun setVibrateOnlyNotifications(enabled: Boolean) {
        _meetingState.update { it.copy(vibrateOnlyNotifications = enabled) }
    }

    fun setAutoResumePrompt(enabled: Boolean) {
        _meetingState.update { it.copy(autoResumePromptAfterMeeting = enabled) }
    }

    fun toggleNotificationCategory(category: NotificationCategory) {
        _meetingState.update { current ->
            val set = current.allowedNotificationTypes.toMutableSet()
            if (set.contains(category)) {
                set.remove(category)
            } else {
                set.add(category)
            }
            current.copy(allowedNotificationTypes = set)
        }
    }

    /**
     * Periodic background check for Calendar & DND events
     */
    private fun startAutoDetectPeriodicChecker() {
        autoDetectJob?.cancel()
        autoDetectJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(60000L) // Check every minute
                checkAutoDetectConditions()
            }
        }
    }

    private suspend fun checkAutoDetectConditions() {
        val state = _meetingState.value

        // Check DND
        if (state.isDndSyncEnabled) {
            val isDnd = calendarDetector.isDoNotDisturbActive()
            if (isDnd && !state.isEnabled) {
                startMeetingMode(durationMinutes = null, meetingTitle = "وضع عدم الإزعاج (DND) 🌙")
                return
            } else if (!isDnd && state.isEnabled && state.activeMeetingTitle?.contains("DND") == true) {
                stopMeetingMode()
                return
            }
        }

        // Check Calendar
        if (state.isAutoCalendarDetectEnabled && calendarDetector.hasCalendarPermission()) {
            val meeting = calendarDetector.findCurrentOngoingMeeting()
            if (meeting != null && !state.isEnabled) {
                val durationMins = ((meeting.endTimeEpochMs - System.currentTimeMillis()) / 60000L).toInt().coerceAtLeast(5)
                startMeetingMode(durationMinutes = durationMins, meetingTitle = meeting.title)
            } else if (meeting == null && state.isEnabled && state.activeMeetingTitle != "اجتماع يدوي" && state.activeMeetingTitle?.contains("DND") != true) {
                // Calendar meeting expired
                stopMeetingMode()
            }
        }
    }

    fun triggerInstantCalendarCheck() {
        scope.launch(Dispatchers.Default) {
            val meeting = calendarDetector.findCurrentOngoingMeeting()
            if (meeting != null) {
                val durationMins = ((meeting.endTimeEpochMs - System.currentTimeMillis()) / 60000L).toInt().coerceAtLeast(5)
                startMeetingMode(durationMinutes = durationMins, meetingTitle = meeting.title)
            }
        }
    }

    fun triggerInstantDndCheck() {
        val isDnd = calendarDetector.isDoNotDisturbActive()
        if (isDnd) {
            startMeetingMode(durationMinutes = null, meetingTitle = "وضع عدم الإزعاج (DND) 🌙")
        }
    }
}
