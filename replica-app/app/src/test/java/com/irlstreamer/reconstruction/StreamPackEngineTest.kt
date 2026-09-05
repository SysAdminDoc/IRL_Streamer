package com.irlstreamer.reconstruction

import com.irlstreamer.reconstruction.engine.BroadcastFailure
import com.irlstreamer.reconstruction.engine.BroadcastRequest
import com.irlstreamer.reconstruction.engine.BroadcastResult
import com.irlstreamer.reconstruction.engine.BroadcastState
import com.irlstreamer.reconstruction.engine.OutgoingStreamer
import com.irlstreamer.reconstruction.engine.StreamPackBroadcastEngine
import com.irlstreamer.reconstruction.model.ReplicaSettings
import io.github.thibaultbee.streampack.core.elements.metrics.BasicEndpointMetrics
import io.github.thibaultbee.streampack.core.elements.metrics.TrackedMetrics
import io.github.thibaultbee.streampack.core.interfaces.IWithVideoSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `StreamPackBroadcastEngine` owns the camera, the microphone, the encoder and
 * the network, and had no tests at all: it needed an Android `Context` and a
 * real device to construct anything. The seam it now takes its capture session
 * through is what makes its own decisions testable off-device.
 */
class StreamPackEngineTest {

    /** One endpoint reading. Only the fields the console maps are set. */
    private class Sample(
        override val uptime: Duration = Duration.ZERO,
        override val packetsWritten: Long = 0,
        override val packetsWriteDropped: Long = 0,
        override val packetsWriteLost: Long = 0,
        override val bytesWritten: Long = 0,
        override val bytesWriteDropped: Long = 0,
    ) : BasicEndpointMetrics

    /** A capture session that records what the engine asked of it. */
    private class FakeStreamer(
        private val failOnConfigure: Boolean = false,
        failOnStart: Boolean = false,
        /** Fails after the engine has already taken the session, unlike configure. */
        private val failOnVideoSource: Boolean = false,
    ) : OutgoingStreamer {
        /** Flipped mid-test to fail a republish the first publish accepted. */
        var failStart = failOnStart

        override val throwableFlow = MutableStateFlow<Throwable?>(null)
        override val isStreamingFlow = MutableStateFlow(false)
        override val videoSource: IWithVideoSource?
            get() = if (failOnVideoSource) throw IllegalStateException("no preview surface") else null

        /** What the endpoint reports it has written; empty unless a test sets it. */
        val samples = MutableStateFlow<TrackedMetrics?>(null)

        var released = false
        var closed = 0
        var stopped = 0

        /** Publishes that succeeded. */
        val started = mutableListOf<String>()

        /** Every publish the engine tried, refused ones included. */
        val attempted = mutableListOf<String>()
        var configuredWidth = 0
        var configuredHeight = 0
        var configuredFps = 0
        var configuredBitrateBps = 0

        override fun metrics(): Flow<TrackedMetrics> = samples.filterNotNull()

        override suspend fun setVideoConfig(width: Int, height: Int, fps: Int, bitrateBps: Int) {
            if (failOnConfigure) throw IllegalStateException("configure refused")
            configuredWidth = width
            configuredHeight = height
            configuredFps = fps
            configuredBitrateBps = bitrateBps
        }

        override suspend fun startStream(url: String) {
            attempted += url
            if (failStart) {
                // A refused connect surfaces on both channels: the streamer
                // publishes it to throwableFlow and the call throws. The order
                // matters - the flow fires while the engine is still CONNECTING.
                throwableFlow.value = java.io.IOException("connect to $url refused")
                // Transport errors quote the URL they failed on, which is the
                // whole reason the engine has to redact before it reports.
                throw IllegalStateException("connect to $url refused")
            }
            started += url
            isStreamingFlow.value = true
        }

        override suspend fun stopStream() {
            stopped++
            isStreamingFlow.value = false
        }

        override suspend fun close() {
            closed++
        }

        override suspend fun selectFacing(front: Boolean) = Result.success(Unit)

        override fun release() {
            released = true
        }
    }

    private fun TestScope.engine(
        streamer: FakeStreamer = FakeStreamer(),
        audioGranted: Boolean = true,
        cameraGranted: Boolean = true,
        opened: MutableList<Boolean> = mutableListOf(),
    ) = StreamPackBroadcastEngine(
        hasAudioPermission = { audioGranted },
        hasCameraPermission = { cameraGranted },
        openStreamer = { withAudio ->
            opened += withAudio
            streamer
        },
        // Its own job, because the engine cancels this scope on release, and
        // the test scheduler, so the reconnect backoff runs in virtual time.
        engineScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler)),
    )

    @Test
    fun startingWithNoDestinationIsRefusedBeforeTheCameraOpens() = runTest(UnconfinedTestDispatcher()) {
        val opened = mutableListOf<Boolean>()
        val engine = engine(opened = opened)

        val result = engine.start(BroadcastRequest(connectionName = null))

        assertEquals(BroadcastResult.Rejected(BroadcastFailure.NoActiveConnection), result)
        assertTrue("the camera must not open with nowhere to send to", opened.isEmpty())
    }

    @Test
    fun aGrantedMicrophoneOpensAnAudioSession() = runTest(UnconfinedTestDispatcher()) {
        val opened = mutableListOf<Boolean>()
        val engine = engine(audioGranted = true, opened = opened)

        engine.openCamera()

        assertEquals(listOf(true), opened)
        assertTrue(engine.hasAudio)
    }

    @Test
    fun aRefusedMicrophoneStillStreamsVideo() = runTest(UnconfinedTestDispatcher()) {
        // A streamer built with an audio input refuses to start without one, so
        // the fork happens at open time and the broadcast goes out silent.
        val opened = mutableListOf<Boolean>()
        val engine = engine(audioGranted = false, opened = opened)

        engine.openCamera()

        assertEquals(listOf(false), opened)
        assertFalse(engine.hasAudio)
    }

    @Test
    fun theChosenFormatIsPushedToTheSessionWhenItOpens() = runTest(UnconfinedTestDispatcher()) {
        val streamer = FakeStreamer()
        val engine = engine(streamer)
        engine.configure(
            ReplicaSettings(
                h264BitrateKbps = 4_500,
                choiceValues = mapOf("resolution" to "1280x720 (16:9)", "fps" to "60 fixed rate"),
            ),
        )

        engine.openCamera()

        assertEquals(1280, streamer.configuredWidth)
        assertEquals(720, streamer.configuredHeight)
        assertEquals(60, streamer.configuredFps)
        assertEquals(4_500_000, streamer.configuredBitrateBps)
    }

    @Test
    fun aSessionThatFailsToConfigureIsReleasedRatherThanLeaked() = runTest(UnconfinedTestDispatcher()) {
        // Configuring can throw after the camera is already held. Dropping that
        // session untracked would leave the device open and every retry would
        // then fail with "camera in use".
        val streamer = FakeStreamer(failOnConfigure = true)
        val engine = engine(streamer)

        val result = engine.openCamera()

        assertTrue(result.isFailure)
        assertTrue("the held camera was leaked", streamer.released)
    }

    @Test
    fun aSessionThatFailsAfterBeingTakenIsNotKept() = runTest(UnconfinedTestDispatcher()) {
        // Opening does several things after the engine has already stored the
        // session. A failure there used to leave a broken session in the field,
        // and every later open short-circuited on it, so the preview never came
        // back. Failing on the preview surface is the case that reaches past the
        // point where the engine takes ownership.
        val opened = mutableListOf<Boolean>()
        val engine = engine(FakeStreamer(failOnVideoSource = true), opened = opened)

        engine.openCamera()
        engine.openCamera()

        assertEquals("the second open reused the broken session", 2, opened.size)
    }

    @Test
    fun startingWalksConnectingToLive() = runTest(UnconfinedTestDispatcher()) {
        // CONNECTING is what the console shows while the handshake is in flight.
        // Going straight to LIVE would claim a stream that has not connected.
        val streamer = FakeStreamer()
        val engine = engine(streamer)
        val seen = mutableListOf<BroadcastState>()
        backgroundScope.launch { engine.state.collect { seen += it } }

        val result = engine.start(BroadcastRequest(connectionName = "rtmp://host/live/key"))

        assertEquals(BroadcastResult.Started, result)
        assertEquals(listOf(BroadcastState.IDLE, BroadcastState.CONNECTING, BroadcastState.LIVE), seen)
        assertEquals(listOf("rtmp://host/live/key"), streamer.started)
    }

    @Test
    fun aRefusedPublishReportsWithoutTheStreamKey() = runTest(UnconfinedTestDispatcher()) {
        val engine = engine(FakeStreamer(failOnStart = true))

        val result = engine.start(BroadcastRequest(connectionName = "rtmp://host/live/sk_secret"))

        val rejected = result as BroadcastResult.Rejected
        val reason = (rejected.failure as BroadcastFailure.TransportUnavailable).reason
        assertEquals(BroadcastState.ERROR, engine.state.value)
        assertFalse("the key reached the failure reason: $reason", reason.contains("sk_secret"))
    }

    @Test
    fun aDropWhileLivePublishesAgainToTheSameDestination() = runTest(UnconfinedTestDispatcher()) {
        // A drop is the normal case on a phone. The engine reconnects rather
        // than ending the broadcast, and the console has to see it leave LIVE
        // on the way so it is not claiming to be up while the link is down.
        val streamer = FakeStreamer()
        val engine = engine(streamer)
        val seen = mutableListOf<BroadcastState>()
        backgroundScope.launch { engine.state.collect { seen += it } }
        engine.start(BroadcastRequest(connectionName = "rtmp://host/live/key"))
        assertEquals(BroadcastState.LIVE, engine.state.value)

        streamer.throwableFlow.value = java.net.SocketException("Connection reset")
        advanceUntilIdle()

        assertTrue("no reconnect was reported: $seen", seen.contains(BroadcastState.RECONNECTING))
        assertEquals(
            listOf("rtmp://host/live/key", "rtmp://host/live/key"),
            streamer.started,
        )
        assertEquals("the dead session was reused", 1, streamer.closed)
        assertEquals(BroadcastState.LIVE, engine.state.value)
    }

    @Test
    fun aDropWithNothingLeftToRetryEndsInError() = runTest(UnconfinedTestDispatcher()) {
        // Every retry reopens the session, so a session that cannot publish any
        // more must not retry forever: the engine gives up and reports.
        val streamer = FakeStreamer()
        val engine = engine(streamer)
        engine.start(BroadcastRequest(connectionName = "rtmp://host/live/key"))
        streamer.failStart = true

        streamer.throwableFlow.value = java.net.SocketException("Connection reset")
        advanceUntilIdle()

        assertEquals(BroadcastState.ERROR, engine.state.value)
        assertEquals(0, engine.statistics.value.reconnectAttempt)
    }

    @Test
    fun anExplicitStopClosesTheSessionAndClearsTheState() = runTest(UnconfinedTestDispatcher()) {
        val streamer = FakeStreamer()
        val engine = engine(streamer)
        engine.start(BroadcastRequest(connectionName = "rtmp://host/live/key"))
        // Real numbers first: the counters used to carry over, so the next
        // broadcast opened showing the last one's bitrate and total.
        streamer.samples.value = TrackedMetrics(
            instant = Sample(uptime = 1.seconds, bytesWritten = 750_000),
            cumulative = Sample(uptime = 42.seconds, bytesWritten = 31_000_000),
        )
        advanceUntilIdle()
        assertEquals(6_000, engine.statistics.value.currentBitrateKbps)
        assertEquals(31_000_000L, engine.statistics.value.bytesSent)

        engine.stop()

        assertEquals(BroadcastState.IDLE, engine.state.value)
        assertEquals(1, streamer.stopped)
        assertEquals(1, streamer.closed)
        assertEquals(0, engine.statistics.value.currentBitrateKbps)
        assertEquals(0L, engine.statistics.value.bytesSent)
    }

    @Test
    fun releasingHandsTheCameraBackFromALiveBroadcast() = runTest(UnconfinedTestDispatcher()) {
        // Release runs when the ViewModel is cleared, which can happen with a
        // broadcast still up. Holding the camera then would deny it to whatever
        // opens the app next.
        val streamer = FakeStreamer()
        val engine = engine(streamer)
        engine.start(BroadcastRequest(connectionName = "rtmp://host/live/key"))
        assertEquals(BroadcastState.LIVE, engine.state.value)

        engine.release()

        assertTrue(streamer.released)
        assertEquals(BroadcastState.IDLE, engine.state.value)
    }

    @Test
    fun aStartThatFailsDoesNotReconnectToThePreviousDestination() = runTest(UnconfinedTestDispatcher()) {
        // A broadcast that ran out of retries leaves the engine in ERROR. If it
        // is still holding that destination, the next start reaches the watch -
        // which counts CONNECTING as live - and the reconnect republishes to the
        // old URL, not the one the user just entered.
        val streamer = FakeStreamer()
        val engine = engine(streamer)
        val old = "rtmp://old.example.com/live/key"
        val fresh = "rtmp://new.example.com/live/key"
        engine.start(BroadcastRequest(connectionName = old))
        streamer.failStart = true
        streamer.throwableFlow.value = java.io.IOException("dropped")
        advanceUntilIdle()
        assertEquals(BroadcastState.ERROR, engine.state.value)
        val beforeRestart = streamer.attempted.size

        engine.start(BroadcastRequest(connectionName = fresh))
        advanceUntilIdle()

        val afterRestart = streamer.attempted.drop(beforeRestart)
        assertTrue(
            "the engine went back to the destination the user left: $afterRestart",
            afterRestart.none { it == old },
        )
    }

    @Test
    fun switchingLensesNeedsTheCameraPermission() = runTest(UnconfinedTestDispatcher()) {
        // The permission can be revoked while the app is running.
        val engine = engine(cameraGranted = false)
        engine.openCamera()

        val result = engine.selectFacing(front = true)

        assertTrue(result.isFailure)
    }
}
