package com.irlstreamer.reconstruction.engine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.util.Size
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.irlstreamer.reconstruction.model.ReplicaSettings
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.backCameras
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.cameraManager
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.frontCameras
import io.github.thibaultbee.streampack.core.interfaces.IWithVideoSource
import io.github.thibaultbee.streampack.core.interfaces.setCameraId
import io.github.thibaultbee.streampack.core.interfaces.releaseBlocking
import io.github.thibaultbee.streampack.core.interfaces.startStream
import io.github.thibaultbee.streampack.core.streamers.single.VideoOnlySingleStreamer
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
 * outgoing stream, so nothing else may bind the device while it exists. Audio
 * is not captured yet - the microphone source needs the RECORD_AUDIO flow that
 * ROADMAP IS-05 covers - so the published stream is video only.
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
    private var streamer: VideoOnlySingleStreamer? = null
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
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    suspend fun openCamera(): Result<VideoOnlySingleStreamer> = streamerLock.withLock {
        streamer?.let { return@withLock Result.success(it) }
        runCatching {
            // Video only: the microphone needs RECORD_AUDIO, which the app does not
            // request yet, and a streamer with an audio input refuses to start
            // without an audio config.
            cameraVideoOnlySingleStreamer(context = context).also { opened ->
                opened.setVideoConfig(videoConfig())
                streamer = opened
                _videoSource.value = opened
            }
        }.onFailure { Log.e(TAG, "camera open failed", it) }
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
            opened.setVideoConfig(videoConfig())
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
                Log.e(TAG, "publish to $url failed", failure)
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
