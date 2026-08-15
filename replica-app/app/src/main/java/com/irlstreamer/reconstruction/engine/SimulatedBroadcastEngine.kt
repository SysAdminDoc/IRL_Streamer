package com.irlstreamer.reconstruction.engine

import com.irlstreamer.reconstruction.model.ReplicaSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Deterministic local simulation - the reconstruction's only engine.
 *
 * Nothing is captured and nothing is transmitted (deviation D005). Every
 * transition is synchronous and every reading is a fixed audited value, because
 * the debug-state harness captures screenshots from a warm process: a timer or
 * a random walk here would make the 145 states unreproducible.
 *
 * Audit evidence for the fixture readings: screen 001 shows "30 fps"; the
 * Network quick tab (screens 132-134) shows "Current Adaptive Bitrate 0bps"
 * against a 6.0 Mbps target.
 */
class SimulatedBroadcastEngine : BroadcastEngine {
    private val _state = MutableStateFlow(BroadcastState.IDLE)
    override val state: StateFlow<BroadcastState> = _state.asStateFlow()

    private val _statistics = MutableStateFlow(IdleStatistics)
    override val statistics: StateFlow<BroadcastStatistics> = _statistics.asStateFlow()

    /**
     * Bonding link readings are derived from the persisted weights so the
     * Network quick tab and the Bonding settings page cannot disagree.
     */
    override fun configure(settings: ReplicaSettings) {
        _statistics.value = _statistics.value.copy(
            targetBitrateKbps = settings.h264BitrateKbps,
            links = linksFrom(settings),
        )
    }

    override suspend fun start(request: BroadcastRequest): BroadcastResult {
        if (request.connectionName.isNullOrBlank()) {
            // The audited guard: the console refuses to go live with no
            // configured destination rather than failing later.
            return BroadcastResult.Rejected(BroadcastFailure.NoActiveConnection)
        }
        _state.value = BroadcastState.CONNECTING
        _state.value = BroadcastState.LIVE
        _statistics.value = _statistics.value.copy(
            currentBitrateKbps = _statistics.value.targetBitrateKbps,
            fps = SIMULATED_FPS,
        )
        return BroadcastResult.Started
    }

    override suspend fun stop() {
        _state.value = BroadcastState.IDLE
        _statistics.value = _statistics.value.copy(
            currentBitrateKbps = 0,
            uptimeSeconds = 0,
            droppedFrames = 0,
        )
    }

    override fun release() {
        _state.value = BroadcastState.IDLE
    }

    private fun linksFrom(settings: ReplicaSettings): List<LinkStatistics> = listOf(
        LinkStatistics("cellular", "Cellular", settings.cellularEnabled, settings.cellularWeight, 0),
        LinkStatistics("wifi", "WiFi", settings.wifiEnabled, settings.wifiWeight, 0),
        LinkStatistics("ethernet", "Ethernet", settings.ethernetEnabled, settings.ethernetWeight, 0),
    )

    private companion object {
        /** Audit evidence: the console FPS pill reads "30 fps" on every live state. */
        const val SIMULATED_FPS = 30

        val IdleStatistics = BroadcastStatistics(
            targetBitrateKbps = 6000,
            currentBitrateKbps = 0,
            fps = SIMULATED_FPS,
        )
    }
}
