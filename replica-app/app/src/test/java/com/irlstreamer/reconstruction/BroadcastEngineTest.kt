package com.irlstreamer.reconstruction

import com.irlstreamer.reconstruction.engine.BroadcastEngine
import com.irlstreamer.reconstruction.engine.BroadcastFailure
import com.irlstreamer.reconstruction.engine.BroadcastRequest
import com.irlstreamer.reconstruction.engine.BroadcastResult
import com.irlstreamer.reconstruction.engine.BroadcastState
import com.irlstreamer.reconstruction.engine.BroadcastStatistics
import com.irlstreamer.reconstruction.engine.SimulatedBroadcastEngine
import com.irlstreamer.reconstruction.model.ReplicaSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
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
        private val failure: BroadcastFailure? = null,
    ) : BroadcastEngine {
        val startedRequests = mutableListOf<BroadcastRequest>()
        var released = false
        private val _state = MutableStateFlow(BroadcastState.IDLE)
        override val state: StateFlow<BroadcastState> = _state
        private val _statistics = MutableStateFlow(BroadcastStatistics())
        override val statistics: StateFlow<BroadcastStatistics> = _statistics

        override suspend fun start(request: BroadcastRequest): BroadcastResult {
            startedRequests += request
            failure?.let { return BroadcastResult.Rejected(it) }
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
}
