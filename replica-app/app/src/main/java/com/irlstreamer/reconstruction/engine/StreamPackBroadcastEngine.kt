package com.irlstreamer.reconstruction.engine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.util.Size
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.irlstreamer.reconstruction.model.ReplicaSettings
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
                _state.value = BroadcastState.LIVE
                _statistics.value = _statistics.value.copy(
                    currentBitrateKbps = settings.h264BitrateKbps,
                    fps = VIDEO_FPS,
                )
                BroadcastResult.Started
            },
            onFailure = { failure ->
                // The URL carries the stream key; only the host is safe to log.
                Log.e(TAG, "publish to ${url.substringBefore("://")}://${url.substringAfter("://").substringBefore('/')} failed", failure)
                _state.value = BroadcastState.ERROR
                BroadcastResult.Rejected(
                    BroadcastFailure.TransportUnavailable(
                        failure.message ?: "Could not publish to $url",
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
        runCatching {
            current.stopStream()
            current.close()
        }.onFailure { Log.e(TAG, "stop failed", it) }
        _statistics.value = _statistics.value.copy(currentBitrateKbps = 0, uptimeSeconds = 0, droppedFrames = 0)
    }

    override fun release() {
        watchJob?.cancel()
        watchJob = null
        engineScope.cancel()
        val current = streamer ?: return
        streamer = null
        _videoSource.value = null
        _state.value = BroadcastState.IDLE
        current.releaseBlocking()
    }

    /**
     * The outgoing stream ended without the app asking. Records why and leaves
     * LIVE, so the console stops claiming to be broadcasting.
     */
    private fun onStreamLost(failure: BroadcastFailure) {
        Log.e(TAG, "outgoing stream lost: $failure")
        _failure.value = failure
        _state.value = BroadcastState.ERROR
        _statistics.value = _statistics.value.copy(currentBitrateKbps = 0)
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
