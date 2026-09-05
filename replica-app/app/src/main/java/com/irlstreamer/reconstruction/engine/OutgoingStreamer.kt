package com.irlstreamer.reconstruction.engine

import android.util.Size
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig
import io.github.thibaultbee.streampack.core.elements.metrics.TrackedMetrics
import io.github.thibaultbee.streampack.core.elements.metrics.WithEndpointMetrics
import io.github.thibaultbee.streampack.core.elements.metrics.metricsFlow
import io.github.thibaultbee.streampack.core.interfaces.IWithVideoSource
import io.github.thibaultbee.streampack.core.interfaces.releaseBlocking
import io.github.thibaultbee.streampack.core.interfaces.setCameraId
import io.github.thibaultbee.streampack.core.interfaces.startStream
import io.github.thibaultbee.streampack.core.streamers.single.IVideoSingleStreamer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Everything the engine asks of a capture session.
 *
 * `StreamPackBroadcastEngine` owns the camera, the microphone, the encoder and
 * the network, and until this seam existed none of it could be tested: the class
 * needs an Android `Context` and a real device to construct anything. Nine
 * members is the whole surface it actually uses, so a test can implement them.
 */
internal interface OutgoingStreamer {
    /** Carries a socket or muxer failure. */
    val throwableFlow: StateFlow<Throwable?>

    /** False when the outgoing stream is not running. */
    val isStreamingFlow: StateFlow<Boolean>

    /** What the console previews, when there is something to preview. */
    val videoSource: IWithVideoSource?

    /** Emits a sample per interval while the endpoint is open. */
    fun metrics(): Flow<TrackedMetrics>

    suspend fun setVideoConfig(width: Int, height: Int, fps: Int, bitrateBps: Int)

    suspend fun startStream(url: String)

    suspend fun stopStream()

    suspend fun close()

    /** Points the camera at the given facing. */
    suspend fun selectFacing(front: Boolean): Result<Unit>

    fun release()
}

/** Opens a capture session. [withAudio] is false when RECORD_AUDIO was refused. */
internal fun interface OutgoingStreamerFactory {
    suspend fun open(withAudio: Boolean): OutgoingStreamer
}

/**
 * The real session, over StreamPack.
 *
 * Kept deliberately thin: it translates types and forwards. Everything that
 * decides anything lives in the engine, where it can be tested.
 */
internal class StreamPackStreamer(
    private val streamer: IVideoSingleStreamer,
    private val cameraIdFor: (front: Boolean) -> String?,
) : OutgoingStreamer {
    override val throwableFlow: StateFlow<Throwable?> get() = streamer.throwableFlow
    override val isStreamingFlow: StateFlow<Boolean> get() = streamer.isStreamingFlow
    override val videoSource: IWithVideoSource? get() = streamer

    override fun metrics(): Flow<TrackedMetrics> =
        (streamer.endpoint as? WithEndpointMetrics<*>)?.metricsFlow() ?: emptyFlow()

    override suspend fun setVideoConfig(width: Int, height: Int, fps: Int, bitrateBps: Int) {
        streamer.setVideoConfig(
            VideoCodecConfig(startBitrate = bitrateBps, resolution = Size(width, height), fps = fps),
        )
    }

    override suspend fun startStream(url: String) = streamer.startStream(url)

    override suspend fun stopStream() = streamer.stopStream()

    override suspend fun close() = streamer.close()

    override suspend fun selectFacing(front: Boolean): Result<Unit> {
        val target = cameraIdFor(front)
            ?: return Result.failure(IllegalStateException("no ${if (front) "front" else "back"} camera"))
        return runCatching { streamer.setCameraId(target) }
    }

    override fun release() {
        streamer.releaseBlocking()
    }
}
