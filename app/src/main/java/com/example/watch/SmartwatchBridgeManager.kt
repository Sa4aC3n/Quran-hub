package com.example.watch

import android.content.Context
import com.example.data.model.PlayerState
import com.example.data.model.Reciter
import com.example.data.model.Surah
import com.example.data.model.WatchSyncSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SmartwatchBridgeManager(
    private val context: Context,
    private val meetingModeManager: MeetingModeManager
) {
    private val _watchSnapshot = MutableStateFlow(WatchSyncSnapshot())
    val watchSnapshot: StateFlow<WatchSyncSnapshot> = _watchSnapshot.asStateFlow()

    // Command listener attached to main playback engine
    var onWatchTogglePlayPause: (() -> Unit)? = null
    var onWatchNextSurah: (() -> Unit)? = null
    var onWatchPreviousSurah: (() -> Unit)? = null
    var onWatchSelectSurah: ((Int) -> Unit)? = null
    var onWatchSelectReciter: ((Reciter) -> Unit)? = null
    var onWatchResumeLastSaved: ((surahNumber: Int, ayahNumber: Int) -> Unit)? = null

    /**
     * Updates snapshot sent to Smartwatch tiles/complications and Now Playing interface
     */
    fun updatePlaybackState(
        playerState: PlayerState,
        favoriteRecitersCount: Int,
        currentAyahNumber: Int? = null
    ) {
        val currentItem = playerState.currentItem
        val meetingState = meetingModeManager.meetingState.value

        _watchSnapshot.update {
            WatchSyncSnapshot(
                isPlaying = playerState.isPlaying,
                currentSurahNumber = currentItem?.surahNumber ?: 1,
                currentSurahName = currentItem?.surahName ?: "الفاتحة",
                currentAyahNumber = currentAyahNumber ?: 1,
                reciterName = currentItem?.reciterName ?: "مشاري العفاسي",
                progressPercent = if (playerState.durationMs > 0) {
                    ((playerState.currentPositionMs.toFloat() / playerState.durationMs) * 100).toInt().coerceIn(0, 100)
                } else 0,
                isMeetingModeActive = meetingState.isEnabled,
                meetingRemainingMinutes = meetingState.remainingDurationMinutes,
                lastSavedSurahNumber = meetingState.lastStoppedSurahNumber ?: currentItem?.surahNumber ?: 1,
                lastSavedSurahName = meetingState.lastStoppedSurahName ?: currentItem?.surahName ?: "الفاتحة",
                lastSavedAyahNumber = meetingState.lastStoppedAyahNumber ?: 1,
                lastSavedReciterName = meetingState.lastStoppedReciterName ?: currentItem?.reciterName ?: "مشاري العفاسي",
                favoriteRecitersCount = favoriteRecitersCount
            )
        }
    }

    /**
     * Handle incoming command from Smartwatch or simulated companion interface
     */
    fun dispatchWatchCommand(command: WatchCommand, param: Any? = null) {
        when (command) {
            WatchCommand.TOGGLE_PLAY_PAUSE -> onWatchTogglePlayPause?.invoke()
            WatchCommand.NEXT_SURAH -> onWatchNextSurah?.invoke()
            WatchCommand.PREVIOUS_SURAH -> onWatchPreviousSurah?.invoke()
            WatchCommand.SELECT_SURAH -> {
                val surahNum = (param as? Int) ?: 1
                onWatchSelectSurah?.invoke(surahNum)
            }
            WatchCommand.SELECT_RECITER -> {
                (param as? Reciter)?.let { onWatchSelectReciter?.invoke(it) }
            }
            WatchCommand.RESUME_LAST_SAVED -> {
                val surah = _watchSnapshot.value.lastSavedSurahNumber
                val ayah = _watchSnapshot.value.lastSavedAyahNumber
                onWatchResumeLastSaved?.invoke(surah, ayah)
            }
            WatchCommand.TOGGLE_MEETING_MODE -> {
                meetingModeManager.toggleManualMeetingMode()
            }
            WatchCommand.SET_MEETING_TIMER -> {
                val minutes = (param as? Int) ?: 30
                meetingModeManager.startMeetingTimer(minutes)
            }
        }
    }
}

enum class WatchCommand {
    TOGGLE_PLAY_PAUSE,
    NEXT_SURAH,
    PREVIOUS_SURAH,
    SELECT_SURAH,
    SELECT_RECITER,
    RESUME_LAST_SAVED,
    TOGGLE_MEETING_MODE,
    SET_MEETING_TIMER
}
