package com.irlstreamer.reconstruction

import com.irlstreamer.reconstruction.engine.BroadcastEngine
import com.irlstreamer.reconstruction.engine.BroadcastFailure
import com.irlstreamer.reconstruction.engine.BroadcastRequest
import com.irlstreamer.reconstruction.engine.BroadcastResult
import com.irlstreamer.reconstruction.engine.BroadcastState
import com.irlstreamer.reconstruction.engine.BroadcastStatistics
import com.irlstreamer.reconstruction.engine.SimulatedBroadcastEngine
import com.irlstreamer.reconstruction.engine.RECONNECT_BASE_DELAY_MS
import com.irlstreamer.reconstruction.engine.RECONNECT_MAX_ATTEMPTS
import com.irlstreamer.reconstruction.engine.RECONNECT_MAX_DELAY_MS
import com.irlstreamer.reconstruction.engine.reconnectDelayMillis
import com.irlstreamer.reconstruction.engine.reconnectWithBackoff
import com.irlstreamer.reconstruction.engine.watchOutgoingStream
import com.irlstreamer.reconstruction.model.ReplicaSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The engine seam is the only place a real transport will land, so its contract
 * is asserted here rather than through the screenshot harness - which cannot
 * observe a running pipeline at all.
 */
class BroadcastEngineTest {

    /** Minimal in-memory engine proving the interface is implementable without Android. */
    private class FakeBroadcastEngine(
        private val rejectWith: BroadcastFailure? = null,
    ) : BroadcastEngine {
        val startedRequests = mutableListOf<BroadcastRequest>()
        var released = false
        private val _state = MutableStateFlow(BroadcastState.IDLE)
        override val state: StateFlow<BroadcastState> = _state
        private val _statistics = MutableStateFlow(BroadcastStatistics())
        override val statistics: StateFlow<BroadcastStatistics> = _statistics
        override val failure: StateFlow<BroadcastFailure?> = MutableStateFlow(null)

        override suspend fun start(request: BroadcastRequest): BroadcastResult {
            startedRequests += request
            rejectWith?.let { return BroadcastResult.Rejected(it) }
            _state.value = BroadcastState.LIVE
            return BroadcastResult.Started
        }

        override suspend fun stop() {
            _state.value = BroadcastState.IDLE
        }

        override fun release() {
            released = true
        }
    }

    @Test
    fun simulationRefusesToStartWithoutAnActiveConnection() = runBlocking {
        val engine = SimulatedBroadcastEngine()

        val result = engine.start(BroadcastRequest(connectionName = null))

        assertEquals(BroadcastResult.Rejected(BroadcastFailure.NoActiveConnection), result)
        assertEquals(BroadcastState.IDLE, engine.state.value)
    }

    @Test
    fun simulationGoesLiveAndBackToIdleWithAConnection() = runBlocking {
        val engine = SimulatedBroadcastEngine()

        val result = engine.start(BroadcastRequest(connectionName = "Local fixture"))
        assertEquals(BroadcastResult.Started, result)
        assertEquals(BroadcastState.LIVE, engine.state.value)
        assertEquals(30, engine.statistics.value.fps)

        engine.stop()
        assertEquals(BroadcastState.IDLE, engine.state.value)
        assertEquals(0, engine.statistics.value.currentBitrateKbps)
    }

    @Test
    fun simulationDerivesLinkStatisticsFromPersistedBondingSettings() {
        val engine = SimulatedBroadcastEngine()

        engine.configure(
            ReplicaSettings(
                cellularEnabled = true,
                cellularWeight = 80,
                wifiEnabled = false,
                wifiWeight = 25,
                h264BitrateKbps = 4500,
            ),
        )

        val stats = engine.statistics.value
        assertEquals(4500, stats.targetBitrateKbps)
        assertEquals(3, stats.links.size)
        val cellular = stats.links.single { it.id == "cellular" }
        assertTrue(cellular.enabled)
        assertEquals(80, cellular.weight)
        assertTrue(!stats.links.single { it.id == "wifi" }.enabled)
    }

    @Test
    fun simulationTransitionsAreDeterministicAcrossRepeatedRuns() = runBlocking {
        // The debug harness screenshots a warm process; a timer or random walk
        // in the engine would make the 145 captured states unreproducible.
        val first = SimulatedBroadcastEngine()
        val second = SimulatedBroadcastEngine()

        first.start(BroadcastRequest(connectionName = "a"))
        second.start(BroadcastRequest(connectionName = "a"))

        assertEquals(first.state.value, second.state.value)
        assertEquals(first.statistics.value, second.statistics.value)
    }

    /**
     * A scriptable engine that walks the full lifecycle.
     *
     * None of the three validation gates can observe a running pipeline - they
     * compare screenshots and hierarchy dumps - and real streaming cannot run in
     * CI, so the transitions a transport must honour are pinned here instead.
     */
    private class ScriptedBroadcastEngine : BroadcastEngine {
        private val _state = MutableStateFlow(BroadcastState.IDLE)
        override val state: StateFlow<BroadcastState> = _state
        private val _statistics = MutableStateFlow(BroadcastStatistics())
        override val statistics: StateFlow<BroadcastStatistics> = _statistics
        override val failure: StateFlow<BroadcastFailure?> = MutableStateFlow(null)
        val transitions = mutableListOf<BroadcastState>()

        private fun move(next: BroadcastState) {
            _state.value = next
            transitions += next
        }

        override suspend fun start(request: BroadcastRequest): BroadcastResult {
            move(BroadcastState.CONNECTING)
            move(BroadcastState.LIVE)
            _statistics.value = BroadcastStatistics(targetBitrateKbps = 6000, currentBitrateKbps = 6000, fps = 30)
            return BroadcastResult.Started
        }

        /** Sustained congestion: the link is up but the encoder cannot keep the target. */
        fun degrade(toKbps: Int) {
            move(BroadcastState.DEGRADED)
            _statistics.value = _statistics.value.copy(currentBitrateKbps = toKbps)
        }

        /** The link dropped and the engine is retrying rather than ending the broadcast. */
        fun loseLink() = move(BroadcastState.RECONNECTING)

        fun recover() {
            move(BroadcastState.LIVE)
            _statistics.value = _statistics.value.copy(currentBitrateKbps = _statistics.value.targetBitrateKbps)
        }

        fun fail() = move(BroadcastState.ERROR)

        override suspend fun stop() = move(BroadcastState.IDLE)

        override fun release() = Unit
    }

    @Test
    fun aDegradedLinkRecoversWithoutEndingTheBroadcast() = runBlocking {
        val engine = ScriptedBroadcastEngine()

        engine.start(BroadcastRequest("rig"))
        engine.degrade(toKbps = 900)
        assertEquals(BroadcastState.DEGRADED, engine.state.value)
        assertEquals(900, engine.statistics.value.currentBitrateKbps)

        engine.loseLink()
        assertEquals(BroadcastState.RECONNECTING, engine.state.value)

        engine.recover()
        assertEquals(BroadcastState.LIVE, engine.state.value)
        assertEquals(6000, engine.statistics.value.currentBitrateKbps)

        engine.stop()
        assertEquals(
            listOf(
                BroadcastState.CONNECTING,
                BroadcastState.LIVE,
                BroadcastState.DEGRADED,
                BroadcastState.RECONNECTING,
                BroadcastState.LIVE,
                BroadcastState.IDLE,
            ),
            engine.transitions,
        )
    }

    @Test
    fun anErroredBroadcastStopsCleanly() = runBlocking {
        val engine = ScriptedBroadcastEngine()
        engine.start(BroadcastRequest("rig"))

        engine.fail()
        assertEquals(BroadcastState.ERROR, engine.state.value)

        // Stopping from ERROR must reach IDLE rather than sticking, or the
        // console would offer no way back to a startable state.
        engine.stop()
        assertEquals(BroadcastState.IDLE, engine.state.value)
    }

    @Test
    fun engineContractSupportsAlternativeImplementations() = runBlocking {
        val fake = FakeBroadcastEngine()
        assertEquals(BroadcastResult.Started, fake.start(BroadcastRequest("rig")))
        assertEquals(BroadcastState.LIVE, fake.state.value)
        assertEquals(1, fake.startedRequests.size)
        fake.release()
        assertTrue(fake.released)

        val refusing = FakeBroadcastEngine(BroadcastFailure.TransportUnavailable("no camera"))
        val rejected = refusing.start(BroadcastRequest("rig")) as BroadcastResult.Rejected
        assertTrue(rejected.failure is BroadcastFailure.TransportUnavailable)
    }

    /**
     * The streamer's two failure signals, as `StreamPackBroadcastEngine` sees
     * them. Driving the flows directly is what the real engine's watch consumes,
     * and it needs no Android context to exercise.
     */
    private class FakeStreamerSignals {
        val throwableFlow = MutableStateFlow<Throwable?>(null)
        val isStreamingFlow = MutableStateFlow(false)
    }

    @Test
    fun aThrowWhileLiveIsReportedAsALostStream() = runTest(UnconfinedTestDispatcher()) {
        val signals = FakeStreamerSignals()
        var live = true
        val reported = mutableListOf<BroadcastFailure>()
        backgroundScope.watchOutgoingStream(
            throwableFlow = signals.throwableFlow,
            isStreamingFlow = signals.isStreamingFlow,
            isLive = { live },
            onLost = { reported += it },
        )
        advanceUntilIdle()

        signals.throwableFlow.value = java.net.SocketException("Connection reset")
        advanceUntilIdle()

        assertEquals(1, reported.size)
        assertEquals(
            BroadcastFailure.TransportUnavailable("Connection reset"),
            reported.single(),
        )
        live = false
    }

    @Test
    fun theFarEndClosingTheConnectionIsReportedEvenWithoutAThrow() = runTest(UnconfinedTestDispatcher()) {
        val signals = FakeStreamerSignals()
        val reported = mutableListOf<BroadcastFailure>()
        backgroundScope.watchOutgoingStream(
            throwableFlow = signals.throwableFlow,
            isStreamingFlow = signals.isStreamingFlow,
            isLive = { true },
            onLost = { reported += it },
        )
        advanceUntilIdle()

        signals.isStreamingFlow.value = true
        advanceUntilIdle()
        assertTrue("streaming must not itself report a loss", reported.isEmpty())

        signals.isStreamingFlow.value = false
        advanceUntilIdle()

        assertEquals(1, reported.size)
        assertTrue(reported.single() is BroadcastFailure.TransportUnavailable)
    }

    @Test
    fun backoffGrowsThenStopsGrowing() {
        // A brief blip recovers in about a second; a long outage still retries
        // about twice a minute rather than hammering the receiver.
        assertEquals(RECONNECT_BASE_DELAY_MS, reconnectDelayMillis(1))
        assertEquals(2_000L, reconnectDelayMillis(2))
        assertEquals(4_000L, reconnectDelayMillis(3))
        assertEquals(RECONNECT_MAX_DELAY_MS, reconnectDelayMillis(RECONNECT_MAX_ATTEMPTS))
        // No overflow into a negative or zero wait at absurd attempt numbers.
        assertEquals(RECONNECT_MAX_DELAY_MS, reconnectDelayMillis(64))
        assertTrue(reconnectDelayMillis(1000) > 0)
    }

    @Test
    fun aDroppedStreamRetriesUntilTheReceiverComesBack() = runTest {
        val attempts = mutableListOf<Int>()
        var connected = false

        // The receiver refuses twice, then accepts.
        val recovered = reconnectWithBackoff(
            onAttempt = { attempts += it },
            connect = {
                if (attempts.size >= 3) {
                    connected = true
                    true
                } else {
                    false
                }
            },
        )

        assertTrue("the retry loop gave up on a receiver that came back", recovered)
        assertTrue(connected)
        assertEquals(listOf(1, 2, 3), attempts)
    }

    @Test
    fun retriesAreBoundedSoAWrongDestinationEventuallyStops() = runTest {
        val attempts = mutableListOf<Int>()

        val recovered = reconnectWithBackoff(
            maxAttempts = 4,
            onAttempt = { attempts += it },
            connect = { false },
        )

        assertTrue("a destination that never accepts must not retry forever", !recovered)
        assertEquals(listOf(1, 2, 3, 4), attempts)
    }

    @Test
    fun anExplicitStopCancelsTheRetryLoop() = runTest {
        val attempts = mutableListOf<Int>()
        val job = backgroundScope.launch {
            reconnectWithBackoff(onAttempt = { attempts += it }, connect = { false })
        }

        // Let the first two retries happen, then stop as the user would.
        advanceTimeBy(RECONNECT_BASE_DELAY_MS + 2_000L + 1)
        val seenBeforeStop = attempts.size
        job.cancel()
        advanceTimeBy(60_000)

        assertTrue("no retry ran before the stop", seenBeforeStop > 0)
        assertEquals("retries continued after an explicit stop", seenBeforeStop, attempts.size)
    }

    @Test
    fun aDeliberateStopReportsNothing() = runTest(UnconfinedTestDispatcher()) {
        val signals = FakeStreamerSignals()
        val reported = mutableListOf<BroadcastFailure>()
        // Callers leave LIVE before asking the streamer to stop, so the flip
        // they cause must stay quiet.
        backgroundScope.watchOutgoingStream(
            throwableFlow = signals.throwableFlow,
            isStreamingFlow = signals.isStreamingFlow,
            isLive = { false },
            onLost = { reported += it },
        )
        advanceUntilIdle()

        signals.isStreamingFlow.value = true
        signals.isStreamingFlow.value = false
        signals.throwableFlow.value = IllegalStateException("closed by us")
        advanceUntilIdle()

        assertTrue(reported.isEmpty())
    }
}
