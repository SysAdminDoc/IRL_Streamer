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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "StreamPackEngine"

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
        runCatching {
            current.stopStream()
            current.close()
        }.onFailure { Log.e(TAG, "stop failed", it) }
        _state.value = BroadcastState.IDLE
        _statistics.value = _statistics.value.copy(currentBitrateKbps = 0, uptimeSeconds = 0, droppedFrames = 0)
    }

    override fun release() {
        val current = streamer ?: return
        streamer = null
        _videoSource.value = null
        _state.value = BroadcastState.IDLE
        current.releaseBlocking()
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
