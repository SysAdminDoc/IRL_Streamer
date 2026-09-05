package com.irlstreamer.reconstruction.engine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.util.Size
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.irlstreamer.reconstruction.model.ReplicaSettings
import com.irlstreamer.reconstruction.model.redactStreamKey
import io.github.thibaultbee.streampack.core.elements.encoders.AudioCodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig
import io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord.MicrophoneSourceFactory
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.backCameras
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.cameraManager
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.frontCameras
import io.github.thibaultbee.streampack.core.interfaces.IWithVideoSource
import io.github.thibaultbee.streampack.core.interfaces.setCameraId
import io.github.thibaultbee.streampack.core.interfaces.releaseBlocking
import io.github.thibaultbee.streampack.core.interfaces.startStream
import io.github.thibaultbee.streampack.core.streamers.single.IVideoSingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.cameraSingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.cameraVideoOnlySingleStreamer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "StreamPackEngine"

/**
 * Watches a streamer's own flows and reports a stream that ended without being
 * asked to.
 *
 * The engine used to command StreamPack and never listen to it, so a link that
 * died mid-broadcast left the console showing LIVE indefinitely. Both signals
 * matter: `throwableFlow` carries a socket or muxer failure, while
 * `isStreamingFlow` going false covers a clean close by the far end that throws
 * nothing.
 *
 * [isLive] is what makes a deliberate stop quiet - callers move the state out
 * of LIVE before they ask the streamer to stop, so the flip they cause is
 * ignored here.
 */
internal fun CoroutineScope.watchOutgoingStream(
    throwableFlow: Flow<Throwable?>,
    isStreamingFlow: Flow<Boolean>,
    isLive: () -> Boolean,
    onLost: (BroadcastFailure) -> Unit,
): Job = launch {
    launch {
        throwableFlow.filterNotNull().collect { throwable ->
            if (isLive()) {
                onLost(
                    BroadcastFailure.TransportUnavailable(
                        throwable.message ?: throwable.javaClass.simpleName,
                    ),
                )
            }
        }
    }
    launch {
        // The first value is the streamer's current state at subscription time,
        // which is false on a streamer that has not started yet.
        isStreamingFlow.drop(1).collect { streaming ->
            if (!streaming && isLive()) {
                onLost(BroadcastFailure.TransportUnavailable("The destination closed the connection."))
            }
        }
    }
}

/** First retry waits this long; each later one doubles. */
internal const val RECONNECT_BASE_DELAY_MS = 1_000L

/** Ceiling on the wait, so a long outage still retries about twice a minute. */
internal const val RECONNECT_MAX_DELAY_MS = 30_000L

/** Bounded, so a destination that is simply wrong stops rather than retrying forever. */
internal const val RECONNECT_MAX_ATTEMPTS = 10

/**
 * How long to wait before attempt [attempt], counting from 1.
 *
 * Exponential so a brief cellular blip recovers in about a second while a real
 * outage backs off instead of hammering the receiver.
 */
internal fun reconnectDelayMillis(attempt: Int): Long {
    if (attempt <= 1) return RECONNECT_BASE_DELAY_MS
    val shift = (attempt - 1).coerceAtMost(20)
    val scaled = RECONNECT_BASE_DELAY_MS shl shift
    return if (scaled <= 0L) RECONNECT_MAX_DELAY_MS else scaled.coerceAtMost(RECONNECT_MAX_DELAY_MS)
}

/**
 * Retries [connect] with bounded exponential backoff until it succeeds.
 *
 * Uses `delay`, so a test drives it on virtual time rather than waiting. An
 * explicit stop cancels the coroutine this runs in, and `delay` is
 * cancellable, so no extra stop flag is needed.
 *
 * @return true when a retry connected, false when [maxAttempts] were used up.
 */
internal suspend fun reconnectWithBackoff(
    maxAttempts: Int = RECONNECT_MAX_ATTEMPTS,
    onAttempt: (Int) -> Unit = {},
    connect: suspend () -> Boolean,
): Boolean {
    for (attempt in 1..maxAttempts) {
        delay(reconnectDelayMillis(attempt))
        onAttempt(attempt)
        if (connect()) return true
    }
    return false
}

/**
 * Camera capture, H.264 encode and RTMP publish.
 *
 * This engine owns the camera: it drives both the console preview and the
 * outgoing stream, so nothing else may bind the device while it exists. Sound
 * is captured too when RECORD_AUDIO has been granted; without it the stream is
 * video only rather than refusing to start.
 */
class StreamPackBroadcastEngine(private val context: Context) : BroadcastEngine {
    private val _state = MutableStateFlow(BroadcastState.IDLE)
    override val state: StateFlow<BroadcastState> = _state.asStateFlow()

    private val _statistics = MutableStateFlow(BroadcastStatistics())
    override val statistics: StateFlow<BroadcastStatistics> = _statistics.asStateFlow()

    private val _failure = MutableStateFlow<BroadcastFailure?>(null)
    override val failure: StateFlow<BroadcastFailure?> = _failure.asStateFlow()

    /** Outlives any single streamer: the watch is re-established on every open. */
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var watchJob: Job? = null

    /** Where a dropped broadcast should reconnect to. Cleared by an explicit stop. */
    private var activeUrl: String? = null
    private var reconnectJob: Job? = null

    /** The console previews this once the camera is open. */
    private val _videoSource = MutableStateFlow<IWithVideoSource?>(null)
    val videoSource: StateFlow<IWithVideoSource?> = _videoSource.asStateFlow()

    private val streamerLock = Mutex()
    private var streamer: IVideoSingleStreamer? = null

    /** True when the open streamer captures sound as well as picture. */
    var hasAudio: Boolean = false
        private set
    private var settings = ReplicaSettings()

    override fun configure(settings: ReplicaSettings) {
        this.settings = settings
        _statistics.value = _statistics.value.copy(
            targetBitrateKbps = settings.h264BitrateKbps,
            links = linksFrom(settings),
        )
    }

    /**
     * Opens the camera if it is not already open. Called by the console before it
     * can show a preview, and again by [start].
     *
     * The stream carries sound only when RECORD_AUDIO was granted before the
     * camera opened: a streamer built with an audio input refuses to start
     * without one, and the input cannot be added afterwards.
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    suspend fun openCamera(): Result<IVideoSingleStreamer> = streamerLock.withLock {
        streamer?.let { return@withLock Result.success(it) }
        val withAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        var opened: IVideoSingleStreamer? = null
        runCatching {
            opened = if (withAudio) {
                cameraSingleStreamer(
                    context = context,
                    audioSourceFactory = MicrophoneSourceFactory(),
                ).also { it.setAudioConfig(AudioCodecConfig()) }
            } else {
                cameraVideoOnlySingleStreamer(context = context)
            }
            val streamerToUse = requireNotNull(opened)
            streamerToUse.setVideoConfig(videoConfig())
            hasAudio = withAudio
            streamer = streamerToUse
            _videoSource.value = streamerToUse
            watchJob?.cancel()
            watchJob = engineScope.watchOutgoingStream(
                throwableFlow = streamerToUse.throwableFlow,
                isStreamingFlow = streamerToUse.isStreamingFlow,
                isLive = { _state.value == BroadcastState.LIVE || _state.value == BroadcastState.CONNECTING },
                onLost = ::onStreamLost,
            )
            streamerToUse
        }.onFailure { failure ->
            Log.e(TAG, "camera open failed", failure)
            // Configuring can throw after the camera is already held. Dropping
            // that streamer untracked would leave the device open and every
            // retry would then fail with "camera in use".
            opened?.let { runCatching { it.releaseBlocking() } }
        }
    }

    /**
     * Closes the camera and forgets the streamer.
     *
     * Android takes the camera away from a backgrounded app, and a streamer that
     * lost its device still reports itself as open, so the console would show a
     * dead surface with no way back. The console releases on stop and opens again
     * on start.
     */
    suspend fun releaseCamera() = streamerLock.withLock {
        val current = streamer ?: return@withLock
        streamer = null
        _videoSource.value = null
        hasAudio = false
        _state.value = BroadcastState.IDLE
        watchJob?.cancel()
        watchJob = null
        // The camera is gone, so there is nothing left to reconnect with.
        activeUrl = null
        reconnectJob?.cancel()
        reconnectJob = null
        runCatching {
            current.stopStream()
            current.close()
            current.releaseBlocking()
        }.onFailure { Log.e(TAG, "camera release failed", it) }
    }

    /**
     * Points the camera the way the console's lens pills ask for.
     *
     * The audited fixture exposes ids 1 and 3 as front lenses (deviation D008);
     * the real device may have any number, so the facing is what carries over.
     */
    suspend fun selectFacing(front: Boolean): Result<Unit> {
        val current = streamerLock.withLock { streamer } ?: return Result.success(Unit)
        // The permission can be revoked while the app is running, and switching
        // reopens the device.
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return Result.failure(SecurityException("camera permission is not granted"))
        }
        val manager = context.cameraManager
        val target = (if (front) manager.frontCameras else manager.backCameras).firstOrNull()
            ?: return Result.failure(IllegalStateException("no ${if (front) "front" else "back"} camera"))
        return runCatching { current.setCameraId(target) }
            .onFailure { Log.e(TAG, "camera switch failed", it) }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    override suspend fun start(request: BroadcastRequest): BroadcastResult {
        val url = request.connectionName?.takeIf { it.isNotBlank() }
            ?: return BroadcastResult.Rejected(BroadcastFailure.NoActiveConnection)

        // A new attempt clears whatever ended the last one, so the console never
        // shows a stale reason over a running stream.
        _failure.value = null

        val opened = openCamera().getOrElse {
            return BroadcastResult.Rejected(
                BroadcastFailure.TransportUnavailable("Camera unavailable: ${it.message ?: it.javaClass.simpleName}"),
            )
        }

        _state.value = BroadcastState.CONNECTING
        return runCatching {
            // The configs are applied once, when the camera opens. Re-applying the
            // video config here left the RTMP endpoint advertising the audio track
            // only, and the receiver rejected the video packets that followed.
            opened.startStream(url)
        }.fold(
            onSuccess = {
                // Remembered so a drop can reconnect without the console asking
                // the user to start again.
                activeUrl = url
                _state.value = BroadcastState.LIVE
                _statistics.value = _statistics.value.copy(
                    currentBitrateKbps = settings.h264BitrateKbps,
                    fps = VIDEO_FPS,
                    reconnectAttempt = 0,
                )
                BroadcastResult.Started
            },
            onFailure = { failure ->
                // The URL carries the stream key, so neither the log nor the
                // message the console shows may contain it.
                val safeUrl = redactStreamKey(url)
                Log.e(TAG, "publish to $safeUrl failed", failure)
                _state.value = BroadcastState.ERROR
                BroadcastResult.Rejected(
                    BroadcastFailure.TransportUnavailable(
                        failure.message ?: "Could not publish to $safeUrl",
                    ),
                )
            },
        )
    }

    override suspend fun stop() {
        val current = streamerLock.withLock { streamer } ?: return
        if (_state.value == BroadcastState.IDLE) return
        // Leave LIVE before asking the streamer to stop: the watch treats a flip
        // to not-streaming as a lost link unless the state already says we asked.
        _state.value = BroadcastState.IDLE
        _failure.value = null
        // An explicit stop ends the broadcast for good; nothing should reconnect
        // behind the user's back.
        activeUrl = null
        reconnectJob?.cancel()
        reconnectJob = null
        runCatching {
            current.stopStream()
            current.close()
        }.onFailure { Log.e(TAG, "stop failed", it) }
        _statistics.value = _statistics.value.copy(
            currentBitrateKbps = 0,
            uptimeSeconds = 0,
            droppedFrames = 0,
            reconnectAttempt = 0,
        )
    }

    override fun release() {
        watchJob?.cancel()
        watchJob = null
        reconnectJob?.cancel()
        reconnectJob = null
        activeUrl = null
        engineScope.cancel()
        val current = streamer ?: return
        streamer = null
        _videoSource.value = null
        _state.value = BroadcastState.IDLE
        current.releaseBlocking()
    }

    /**
     * The outgoing stream ended without the app asking.
     *
     * A drop is the normal case on a phone, so this reconnects rather than
     * ending the broadcast. It reports the failure and gives up only once the
     * retries are exhausted.
     */
    private fun onStreamLost(failure: BroadcastFailure) {
        Log.e(TAG, "outgoing stream lost: $failure")
        _statistics.value = _statistics.value.copy(currentBitrateKbps = 0)
        val url = activeUrl
        if (url == null) {
            _failure.value = failure
            _state.value = BroadcastState.ERROR
            return
        }
        _state.value = BroadcastState.RECONNECTING
        reconnectJob?.cancel()
        reconnectJob = engineScope.launch {
            val recovered = reconnectWithBackoff(
                onAttempt = { attempt ->
                    _statistics.value = _statistics.value.copy(reconnectAttempt = attempt)
                },
                connect = { attemptReconnect(url) },
            )
            if (recovered) {
                _state.value = BroadcastState.LIVE
                _statistics.value = _statistics.value.copy(
                    reconnectAttempt = 0,
                    currentBitrateKbps = settings.h264BitrateKbps,
                    fps = VIDEO_FPS,
                )
            } else {
                _failure.value = failure
                _state.value = BroadcastState.ERROR
                _statistics.value = _statistics.value.copy(reconnectAttempt = 0)
            }
        }
    }

    /**
     * One reconnect attempt.
     *
     * The streamer has to be closed before it can publish again, and the camera
     * may have been taken away while the link was down, so this reopens rather
     * than assuming the old session is still usable.
     */
    private suspend fun attemptReconnect(url: String): Boolean {
        val current = streamerLock.withLock { streamer } ?: return false
        return runCatching {
            runCatching { current.stopStream() }
            runCatching { current.close() }
            current.startStream(url)
        }.fold(
            onSuccess = { true },
            onFailure = { failure ->
                Log.e(TAG, "reconnect to ${redactStreamKey(url)} failed", failure)
                false
            },
        )
    }

    private fun videoConfig() = VideoCodecConfig(
        startBitrate = settings.h264BitrateKbps * 1_000,
        resolution = Size(VIDEO_WIDTH, VIDEO_HEIGHT),
        fps = VIDEO_FPS,
    )

    private fun linksFrom(settings: ReplicaSettings) = listOf(
        LinkStatistics("cellular", "Cellular", settings.cellularEnabled, settings.cellularWeight, 0),
        LinkStatistics("wifi", "WiFi", settings.wifiEnabled, settings.wifiWeight, 0),
        LinkStatistics("ethernet", "Ethernet", settings.ethernetEnabled, settings.ethernetWeight, 0),
    )

    private companion object {
        const val VIDEO_WIDTH = 1920
        const val VIDEO_HEIGHT = 1080
        const val VIDEO_FPS = 30
    }
}
