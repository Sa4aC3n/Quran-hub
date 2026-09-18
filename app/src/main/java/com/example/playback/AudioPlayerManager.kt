package com.example.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.session.MediaSession
import com.example.MainActivity
import com.example.data.model.ABLoopState
import com.example.data.model.AudioQualityLevel
import com.example.data.model.AudioSource
import com.example.data.model.PlaylistItem
import com.example.data.model.PlayerEvent
import com.example.data.model.PlayerRepeatMode
import com.example.data.model.PlayerState
import com.example.data.model.RepeatSettings
import com.example.data.model.SleepTimerMode
import com.example.data.model.SurahAudioItem
import com.example.data.provider.ReciterImageProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class AudioPlayerManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val httpDataSourceFactory by lazy {
        DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36 QuranAudio/1.0")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)
            .setKeepPostFor302Redirects(true)
    }

    private val dataSourceFactory by lazy {
        DefaultDataSource.Factory(context, httpDataSourceFactory)
    }

    private val mediaSourceFactory by lazy {
        DefaultMediaSourceFactory(dataSourceFactory)
            .setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(1))
    }

    val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true // handle audio focus
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NONE)
            .build().apply {
                addListener(playerListener)
            }
    }

    var onTrackEndedCallback: (() -> Unit)? = null
    var onNextTrackCallback: (() -> Unit)? = null
    var onPreviousTrackCallback: (() -> Unit)? = null

    val forwardingPlayer: ForwardingPlayer by lazy {
        object : ForwardingPlayer(exoPlayer) {
            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_BACK)
                    .add(Player.COMMAND_SEEK_FORWARD)
                    .build()
            }

            override fun isCommandAvailable(command: Int): Boolean {
                return when (command) {
                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                    Player.COMMAND_SEEK_BACK,
                    Player.COMMAND_SEEK_FORWARD -> true
                    else -> super.isCommandAvailable(command)
                }
            }

            override fun seekToNext() {
                onNextTrackCallback?.invoke()
            }

            override fun seekToNextMediaItem() {
                onNextTrackCallback?.invoke()
            }

            override fun seekToPrevious() {
                onPreviousTrackCallback?.invoke()
            }

            override fun seekToPreviousMediaItem() {
                onPreviousTrackCallback?.invoke()
            }

            override fun seekBack() {
                seekRewind10s()
            }

            override fun seekForward() {
                seekForward10s()
            }
        }
    }

    val mediaSession: MediaSession by lazy {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        MediaSession.Builder(context, forwardingPlayer)
            .setSessionActivity(pendingIntent)
            .setId("QuranAudioSession")
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controllerInfo: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                        .build()
                    val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                        .add(Player.COMMAND_PLAY_PAUSE)
                        .add(Player.COMMAND_SEEK_TO_NEXT)
                        .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                        .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                        .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                        .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                        .add(Player.COMMAND_SEEK_BACK)
                        .add(Player.COMMAND_SEEK_FORWARD)
                        .add(Player.COMMAND_STOP)
                        .build()
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailableSessionCommands(sessionCommands)
                        .setAvailablePlayerCommands(playerCommands)
                        .build()
                }
            })
            .build().also { session ->
                QuranMediaPlaybackService.currentMediaSession = session
            }
    }

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _playerEvents = MutableSharedFlow<PlayerEvent>(extraBufferCapacity = 10)
    val playerEvents: SharedFlow<PlayerEvent> = _playerEvents.asSharedFlow()

    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerState.value = _playerState.value.copy(
                isPlaying = isPlaying,
                isBuffering = exoPlayer.playbackState == Player.STATE_BUFFERING
            )
            if (isPlaying) {
                startProgressUpdates()
            } else {
                stopProgressUpdates()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val isBuffering = playbackState == Player.STATE_BUFFERING
            val duration = if (exoPlayer.duration > 0) exoPlayer.duration else 0L

            _playerState.value = _playerState.value.copy(
                isBuffering = isBuffering,
                durationMs = duration
            )

            if (playbackState == Player.STATE_READY) {
                _playerState.value = _playerState.value.copy(
                    isBuffering = false,
                    durationMs = exoPlayer.duration.coerceAtLeast(0L),
                    fallbackMessage = null,
                    isFallbackActive = _playerState.value.currentSourceIndex > 0
                )
            } else if (playbackState == Player.STATE_ENDED) {
                val state = _playerState.value

                // 1. Check End-of-Surah Sleep Timer
                if (state.isSleepTimerActive && state.sleepTimerMode == SleepTimerMode.END_OF_SURAH) {
                    cancelSleepTimer()
                    exoPlayer.pause()
                    _playerEvents.tryEmit(PlayerEvent.ToastMessage("انتهت السورة وتم إيقاف المشغل حسب مؤقت النوم 🌙"))
                    return
                }

                // 2. Check Repeat Target Count (e.g. 3, 5, 10 times)
                val repeatSettings = state.repeatSettings
                if (repeatSettings.targetCount > 0) {
                    if (repeatSettings.currentIteration < repeatSettings.targetCount) {
                        val nextIteration = repeatSettings.currentIteration + 1
                        _playerState.value = state.copy(
                            repeatSettings = repeatSettings.copy(currentIteration = nextIteration)
                        )
                        _playerEvents.tryEmit(PlayerEvent.ToastMessage("تكرار السورة: تلاوة $nextIteration من أصل ${repeatSettings.targetCount}"))
                        exoPlayer.seekTo(0)
                        exoPlayer.play()
                        return
                    } else {
                        // Finished all repetitions
                        _playerState.value = state.copy(
                            repeatSettings = repeatSettings.copy(currentIteration = 1)
                        )
                        _playerEvents.tryEmit(PlayerEvent.ToastMessage("اكتمل تكرار السورة ${repeatSettings.targetCount} مرات بنجاح"))
                    }
                }

                // 3. Normal Repeat Mode
                when (state.repeatMode) {
                    PlayerRepeatMode.REPEAT_ONE -> {
                        exoPlayer.seekTo(0)
                        exoPlayer.play()
                    }
                    PlayerRepeatMode.REPEAT_ALL, PlayerRepeatMode.OFF -> {
                        if (state.autoPlayNext) {
                            onTrackEndedCallback?.invoke()
                        }
                    }
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            handlePlaybackError(error)
        }
    }

    fun playSurah(
        item: SurahAudioItem,
        preferredQuality: AudioQualityLevel = AudioQualityLevel.KBPS_320,
        initialSourceIndex: Int = 0
    ) {
        val hasLocalFile = item.localFilePath != null

        val (targetUri, resolvedSource, sourceIndex, isOffline) = if (hasLocalFile) {
            val dummySource = AudioSource(
                url = item.localFilePath!!,
                format = "mp3",
                qualityLevel = AudioQualityLevel.KBPS_192
            )
            Quadruple(item.localFilePath!!, dummySource, 0, true)
        } else {
            // Find quality matched source
            if (item.detailedSources.isNotEmpty()) {
                val idx = initialSourceIndex.coerceIn(0, (item.detailedSources.size - 1).coerceAtLeast(0))
                val src = if (initialSourceIndex > 0) {
                    item.detailedSources[idx]
                } else {
                    val pair = item.getSourceForQuality(preferredQuality)
                    pair.second ?: item.detailedSources.first()
                }
                val chosenIndex = item.detailedSources.indexOf(src).coerceAtLeast(0)
                Quadruple(src.url, src, chosenIndex, false)
            } else {
                val sources = item.audioSources
                val idx = initialSourceIndex.coerceIn(0, (sources.size - 1).coerceAtLeast(0))
                val url = if (sources.isNotEmpty()) sources[idx] else ""
                val dummySource = AudioSource(url = url, format = "mp3", qualityLevel = preferredQuality)
                Quadruple(url, dummySource, idx, false)
            }
        }

        if (targetUri.isEmpty()) {
            _playerEvents.tryEmit(PlayerEvent.PlaybackError("لا توجد روابط تشغيل متاحة لهذه السورة"))
            return
        }

        _playerState.value = _playerState.value.copy(
            currentItem = item,
            currentSourceIndex = sourceIndex,
            totalSourcesCount = if (item.detailedSources.isNotEmpty()) item.detailedSources.size else item.audioSources.size,
            currentSourceUrl = targetUri,
            currentAudioSource = resolvedSource,
            activeQuality = resolvedSource?.qualityLevel ?: preferredQuality,
            isOfflineMode = isOffline,
            isBuffering = true,
            fallbackMessage = if (sourceIndex > 0) "جاري تجربة مصدر بديل (${sourceIndex + 1})..." else null,
            isFallbackActive = sourceIndex > 0
        )

        val formatDesc = "${resolvedSource?.qualityLevel?.approximateBitrateKbps ?: 192} kbps"
        val artworkUri = ReciterImageProvider.getArtworkUri(context, item.reciterId, item.reciterName)

        val metadata = MediaMetadata.Builder()
            .setTitle("سورة ${item.surahName}")
            .setArtist(item.reciterName)
            .setSubtitle(item.reciterName)
            .setAlbumTitle("${item.riwayah} • ${item.recitationType}")
            .setDisplayTitle("سورة ${item.surahName}")
            .setArtworkUri(artworkUri)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(targetUri)
            .setMediaMetadata(metadata)
            .build()

        try {
            // Ensure mediaSession is initialized
            val session = mediaSession
            
            // Start QuranMediaPlaybackService to connect MediaSession with system resources
            try {
                val serviceIntent = Intent(context, QuranMediaPlaybackService::class.java)
                context.startService(serviceIntent)
            } catch (_: Exception) {}

            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        } catch (e: Exception) {
            handlePlaybackError(PlaybackException(e.message, e, PlaybackException.ERROR_CODE_IO_UNSPECIFIED))
        }
    }

    private fun handlePlaybackError(error: PlaybackException) {
        val state = _playerState.value
        val item = state.currentItem ?: return

        // If local file failed, fall back to online streaming sources
        if (state.isOfflineMode && (item.detailedSources.isNotEmpty() || item.audioSources.isNotEmpty())) {
            _playerEvents.tryEmit(
                PlayerEvent.FallbackSourceSwitched(
                    sourceIndex = 1,
                    totalSources = if (item.detailedSources.isNotEmpty()) item.detailedSources.size else item.audioSources.size,
                    surahName = item.surahName,
                    reciterName = item.reciterName,
                    quality = "البث المباشر (Online)"
                )
            )
            scope.launch(Dispatchers.Main) {
                playSurah(item.copy(localFilePath = null), preferredQuality = state.activeQuality, initialSourceIndex = 0)
            }
            return
        }

        val totalSources = if (item.detailedSources.isNotEmpty()) item.detailedSources.size else item.audioSources.size
        val nextIndex = state.currentSourceIndex + 1
        if (nextIndex < totalSources) {
            val fallbackQuality = if (item.detailedSources.isNotEmpty()) {
                item.detailedSources[nextIndex].qualityLevel.labelArabic
            } else "خادم احتياطي (${nextIndex + 1})"

            _playerEvents.tryEmit(
                PlayerEvent.FallbackSourceSwitched(
                    sourceIndex = nextIndex + 1,
                    totalSources = totalSources,
                    surahName = item.surahName,
                    reciterName = item.reciterName,
                    quality = fallbackQuality
                )
            )

            scope.launch(Dispatchers.Main) {
                playSurah(item, preferredQuality = state.activeQuality, initialSourceIndex = nextIndex)
            }
        } else {
            _playerState.value = _playerState.value.copy(
                isPlaying = false,
                isBuffering = false,
                fallbackMessage = "تعذر التشغيل من كافة المصادر المتاحة"
            )
            _playerEvents.tryEmit(
                PlayerEvent.PlaybackError("تعذر تشغيل الصوت من كافة المصادر المتاحة ($totalSources روابط). يرجى التحقق من اتصال الإنترنت.")
            )
        }
    }

    fun switchQuality(quality: AudioQualityLevel) {
        val item = _playerState.value.currentItem ?: return
        val currentPos = exoPlayer.currentPosition
        playSurah(item.copy(localFilePath = null), preferredQuality = quality, initialSourceIndex = 0)
        exoPlayer.seekTo(currentPos)
    }

    fun switchSourceManually(sourceIndex: Int) {
        val item = _playerState.value.currentItem ?: return
        val total = if (item.detailedSources.isNotEmpty()) item.detailedSources.size else item.audioSources.size
        if (sourceIndex in 0 until total) {
            playSurah(item.copy(localFilePath = null), initialSourceIndex = sourceIndex)
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_IDLE && _playerState.value.currentItem != null) {
                playSurah(_playerState.value.currentItem!!)
            } else {
                exoPlayer.play()
            }
        }
    }

    fun pause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        }
    }

    fun play() {
        if (!exoPlayer.isPlaying) {
            exoPlayer.play()
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs.coerceIn(0L, exoPlayer.duration.coerceAtLeast(0L)))
        _playerState.value = _playerState.value.copy(currentPositionMs = positionMs)
    }

    fun seekForward10s() {
        val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
        seekTo(newPos)
    }

    fun seekRewind10s() {
        val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
        seekTo(newPos)
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer.playbackParameters = PlaybackParameters(speed)
        _playerState.value = _playerState.value.copy(playbackSpeed = speed)
    }

    fun setRepeatMode(mode: PlayerRepeatMode) {
        _playerState.value = _playerState.value.copy(
            repeatMode = mode,
            repeatSettings = _playerState.value.repeatSettings.copy(mode = mode)
        )
    }

    fun setRepeatCount(targetCount: Int) {
        _playerState.value = _playerState.value.copy(
            repeatSettings = _playerState.value.repeatSettings.copy(
                targetCount = targetCount,
                currentIteration = 1
            )
        )
        if (targetCount > 0) {
            _playerEvents.tryEmit(PlayerEvent.ToastMessage("تم ضبط تكرار السورة للحفظ: $targetCount مرات"))
        } else {
            _playerEvents.tryEmit(PlayerEvent.ToastMessage("تم إلغاء عداد التكرار المخصص"))
        }
    }

    // A-B Looping Interval
    fun setABLoopStart(startMs: Long) {
        val current = _playerState.value.repeatSettings.abLoop
        val newAb = current.copy(
            startPositionMs = startMs,
            isActive = current.endPositionMs != null && (current.endPositionMs ?: 0L) > startMs,
            loopCount = 0
        )
        _playerState.value = _playerState.value.copy(
            repeatSettings = _playerState.value.repeatSettings.copy(abLoop = newAb)
        )
        _playerEvents.tryEmit(PlayerEvent.ToastMessage("تم تحديد نقطة البداية (أ)"))
    }

    fun setABLoopEnd(endMs: Long) {
        val current = _playerState.value.repeatSettings.abLoop
        val start = current.startPositionMs
        if (start != null && endMs > start) {
            val newAb = current.copy(
                endPositionMs = endMs,
                isActive = true,
                loopCount = 0
            )
            _playerState.value = _playerState.value.copy(
                repeatSettings = _playerState.value.repeatSettings.copy(abLoop = newAb)
            )
            _playerEvents.tryEmit(PlayerEvent.ToastMessage("تم تفعيل تكرار المقطع (أ - ب) بنجاح 🔁"))
        } else {
            _playerEvents.tryEmit(PlayerEvent.ToastMessage("يجب أن تكون نقطة النهاية بعد نقطة البداية"))
        }
    }

    fun toggleABLoopActive(active: Boolean) {
        val current = _playerState.value.repeatSettings.abLoop
        if (current.isReady) {
            _playerState.value = _playerState.value.copy(
                repeatSettings = _playerState.value.repeatSettings.copy(
                    abLoop = current.copy(isActive = active)
                )
            )
            if (active) {
                current.startPositionMs?.let { seekTo(it) }
                _playerEvents.tryEmit(PlayerEvent.ToastMessage("تم تفعيل تكرار المقطع"))
            } else {
                _playerEvents.tryEmit(PlayerEvent.ToastMessage("تم إيقاف تكرار المقطع"))
            }
        }
    }

    fun clearABLoop() {
        _playerState.value = _playerState.value.copy(
            repeatSettings = _playerState.value.repeatSettings.copy(
                abLoop = ABLoopState()
            )
        )
        _playerEvents.tryEmit(PlayerEvent.ToastMessage("تمت إزالة نقاط التكرار المقطعي"))
    }

    // Sleep Timer
    fun startSleepTimer(minutes: Int, fadeOut: Boolean = true, mode: SleepTimerMode = SleepTimerMode.CUSTOM) {
        cancelSleepTimer()
        val totalSeconds = minutes * 60L
        _playerState.value = _playerState.value.copy(
            isSleepTimerActive = true,
            sleepTimerRemainingSeconds = totalSeconds,
            sleepTimerMode = mode,
            sleepTimerFadeOutEnabled = fadeOut
        )
        _playerEvents.tryEmit(PlayerEvent.ToastMessage("تم ضبط مؤقت النوم: $minutes دقيقة 🌙"))

        sleepTimerJob = scope.launch(Dispatchers.Main) {
            var remaining = totalSeconds
            while (isActive && remaining > 0) {
                delay(1000)
                remaining--
                _playerState.value = _playerState.value.copy(sleepTimerRemainingSeconds = remaining)

                // Gradual Fade Out in last 30 seconds
                if (fadeOut && remaining <= 30) {
                    val fadeFactor = (remaining.toFloat() / 30f).coerceIn(0.05f, 1f)
                    exoPlayer.volume = fadeFactor
                }
            }

            if (isActive && remaining <= 0) {
                exoPlayer.pause()
                exoPlayer.volume = 1f
                _playerState.value = _playerState.value.copy(
                    isSleepTimerActive = false,
                    sleepTimerRemainingSeconds = 0L,
                    sleepTimerMode = SleepTimerMode.OFF
                )
                _playerEvents.tryEmit(PlayerEvent.ToastMessage("انتهى مؤقت النوم وتم إيقاف المشغل بهدوء 🌙"))
            }
        }
    }

    fun startSleepTimerEndOfSurah(fadeOut: Boolean = true) {
        cancelSleepTimer()
        _playerState.value = _playerState.value.copy(
            isSleepTimerActive = true,
            sleepTimerRemainingSeconds = 0L,
            sleepTimerMode = SleepTimerMode.END_OF_SURAH,
            sleepTimerFadeOutEnabled = fadeOut
        )
        _playerEvents.tryEmit(PlayerEvent.ToastMessage("سيتم إيقاف التلاوة تلقائياً عند نهاية السورة الحالية 🌙"))
    }

    fun addSleepTimerMinutes(extraMinutes: Int) {
        if (!_playerState.value.isSleepTimerActive) {
            startSleepTimer(extraMinutes)
            return
        }
        val currentRemaining = _playerState.value.sleepTimerRemainingSeconds
        val newRemaining = currentRemaining + (extraMinutes * 60L)
        _playerState.value = _playerState.value.copy(
            sleepTimerRemainingSeconds = newRemaining,
            sleepTimerMode = SleepTimerMode.CUSTOM
        )
        _playerEvents.tryEmit(PlayerEvent.ToastMessage("تم تمديد مؤقت النوم +$extraMinutes دقائق"))
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        exoPlayer.volume = 1f
        _playerState.value = _playerState.value.copy(
            isSleepTimerActive = false,
            sleepTimerRemainingSeconds = 0L,
            sleepTimerMode = SleepTimerMode.OFF
        )
    }

    // Custom Playlist Queue
    fun setPlaylistQueue(
        playlistId: String?,
        playlistName: String?,
        queue: List<PlaylistItem>,
        startIndex: Int = 0,
        isShuffle: Boolean = false
    ) {
        _playerState.value = _playerState.value.copy(
            activePlaylistId = playlistId,
            activePlaylistName = playlistName,
            playlistQueue = queue,
            currentPlaylistIndex = startIndex,
            isShuffleEnabled = isShuffle
        )
    }

    fun toggleShuffle(): Boolean {
        val newShuffle = !_playerState.value.isShuffleEnabled
        _playerState.value = _playerState.value.copy(isShuffleEnabled = newShuffle)
        return newShuffle
    }

    fun setAutoPlayNext(enabled: Boolean) {
        _playerState.value = _playerState.value.copy(autoPlayNext = enabled)
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    val currentPos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val dur = exoPlayer.duration.coerceAtLeast(0L)
                    _playerState.value = _playerState.value.copy(
                        currentPositionMs = currentPos,
                        durationMs = dur
                    )

                    // Check A-B Looping
                    val ab = _playerState.value.repeatSettings.abLoop
                    if (ab.isActive && ab.isReady) {
                        val endMs = ab.endPositionMs ?: Long.MAX_VALUE
                        val startMs = ab.startPositionMs ?: 0L
                        if (currentPos >= endMs) {
                            exoPlayer.seekTo(startMs)
                            _playerState.value = _playerState.value.copy(
                                repeatSettings = _playerState.value.repeatSettings.copy(
                                    abLoop = ab.copy(loopCount = ab.loopCount + 1)
                                )
                            )
                        }
                    }
                }
                delay(60)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopProgressUpdates()
        cancelSleepTimer()
        exoPlayer.removeListener(playerListener)
        try {
            mediaSession.release()
        } catch (_: Exception) {}
        QuranMediaPlaybackService.currentMediaSession = null
        exoPlayer.release()
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
