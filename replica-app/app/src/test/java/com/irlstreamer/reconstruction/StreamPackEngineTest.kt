package com.irlstreamer.reconstruction

import com.irlstreamer.reconstruction.engine.BroadcastFailure
import com.irlstreamer.reconstruction.engine.BroadcastRequest
import com.irlstreamer.reconstruction.engine.BroadcastResult
import com.irlstreamer.reconstruction.engine.BroadcastState
import com.irlstreamer.reconstruction.engine.OutgoingStreamer
import com.irlstreamer.reconstruction.engine.StreamPackBroadcastEngine
import com.irlstreamer.reconstruction.model.ReplicaSettings
import io.github.thibaultbee.streampack.core.elements.metrics.TrackedMetrics
import io.github.thibaultbee.streampack.core.interfaces.IWithVideoSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.emptyFlow
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

    /** A capture session that records what the engine asked of it. */
    private class FakeStreamer(
        private val failOnConfigure: Boolean = false,
        failOnStart: Boolean = false,
    ) : OutgoingStreamer {
        /** Flipped mid-test to fail a republish the first publish accepted. */
        var failStart = failOnStart

        override val throwableFlow = MutableStateFlow<Throwable?>(null)
        override val isStreamingFlow = MutableStateFlow(false)
        override val videoSource: IWithVideoSource? = null

        var released = false
        var closed = 0
        var stopped = 0
        val started = mutableListOf<String>()
        var configuredWidth = 0
        var configuredHeight = 0
        var configuredFps = 0
        var configuredBitrateBps = 0

        override fun metrics(): Flow<TrackedMetrics> = emptyFlow()

        override suspend fun setVideoConfig(width: Int, height: Int, fps: Int, bitrateBps: Int) {
            if (failOnConfigure) throw IllegalStateException("configure refused")
            configuredWidth = width
            configuredHeight = height
            configuredFps = fps
            configuredBitrateBps = bitrateBps
        }

        override suspend fun startStream(url: String) {
            if (failStart) throw IllegalStateException("publish refused")
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
    fun aFailedSessionDoesNotBlockTheNextOpen() = runTest(UnconfinedTestDispatcher()) {
        // The engine used to keep a broken session, so every retry short-circuited
        // on it. Opening again must actually build a new one.
        val opened = mutableListOf<Boolean>()
        val engine = engine(FakeStreamer(failOnConfigure = true), opened = opened)

        engine.openCamera()
        engine.openCamera()

        assertEquals(2, opened.size)
    }

    @Test
    fun startingWalksConnectingToLiveAndRemembersTheDestination() = runTest(UnconfinedTestDispatcher()) {
        val streamer = FakeStreamer()
        val engine = engine(streamer)

        val result = engine.start(BroadcastRequest(connectionName = "rtmp://host/live/key"))

        assertEquals(BroadcastResult.Started, result)
        assertEquals(BroadcastState.LIVE, engine.state.value)
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

        engine.stop()

        assertEquals(BroadcastState.IDLE, engine.state.value)
        assertEquals(1, streamer.stopped)
        assertEquals(1, streamer.closed)
        assertEquals(0, engine.statistics.value.currentBitrateKbps)
        assertEquals(0L, engine.statistics.value.bytesSent)
    }

    @Test
    fun releasingHandsTheCameraBack() = runTest(UnconfinedTestDispatcher()) {
        val streamer = FakeStreamer()
        val engine = engine(streamer)
        engine.openCamera()

        engine.release()

        assertTrue(streamer.released)
        assertEquals(BroadcastState.IDLE, engine.state.value)
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
