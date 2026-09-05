package com.irlstreamer.reconstruction.engine

import kotlinx.coroutines.flow.StateFlow

/**
 * Lifecycle of the outgoing broadcast.
 *
 * The audit records only the idle console and the "no active connection" guard
 * (screen 142); every other state here is the contract a real transport will
 * need, declared now so the UI can be written against it once rather than
 * rewritten when the simulation is replaced.
 */
enum class BroadcastState {
    IDLE,
    CONNECTING,
    LIVE,
    DEGRADED,
    RECONNECTING,
    ERROR,
}

/** Per-link contribution, one entry per enabled bonding link. */
data class LinkStatistics(
    val id: String,
    val label: String,
    val enabled: Boolean,
    val weight: Int,
    val bitrateKbps: Int,
)

/**
 * Sampled broadcast statistics.
 *
 * Values are whatever the active engine reports. The simulation reports the
 * audited fixture readings so the 145 captured states are unchanged.
 */
data class BroadcastStatistics(
    val targetBitrateKbps: Int = 0,
    val currentBitrateKbps: Int = 0,
    val fps: Int = 0,
    val uptimeSeconds: Long = 0,
    val droppedFrames: Int = 0,
    val links: List<LinkStatistics> = emptyList(),
)

/** Why a start request was refused. */
sealed interface BroadcastFailure {
    /** Audited: "You don't have any active connections, please add one." (screen 142) */
    data object NoActiveConnection : BroadcastFailure

    /** The engine cannot reach a transport - permissions, hardware, or network. */
    data class TransportUnavailable(val reason: String) : BroadcastFailure
}

/** Outcome of a start request. */
sealed interface BroadcastResult {
    data object Started : BroadcastResult
    data class Rejected(val failure: BroadcastFailure) : BroadcastResult
}

/** What to broadcast to. `connectionName` is null while no connection is configured. */
data class BroadcastRequest(
    val connectionName: String? = null,
    val recordLocally: Boolean = false,
)

/**
 * The seam between the UI and whatever produces the outgoing stream.
 *
 * `SimulatedBroadcastEngine` transmits nothing (deviation D005);
 * `StreamPackBroadcastEngine` captures and publishes for real. A transport
 * lands behind this interface without touching Compose or the debug-state
 * harness.
 */
interface BroadcastEngine {
    val state: StateFlow<BroadcastState>
    val statistics: StateFlow<BroadcastStatistics>

    /**
     * Why the broadcast ended when the user did not end it.
     *
     * A start request reports its own refusal through [BroadcastResult]. This
     * carries the other case: the stream was running and then stopped on its
     * own. Null whenever there is nothing to report.
     */
    val failure: StateFlow<BroadcastFailure?>

    /**
     * Push the durable settings into the engine. Called whenever persisted
     * settings change so bonding weights and encoder targets cannot drift out
     * of step with the settings tree.
     */
    fun configure(settings: com.irlstreamer.reconstruction.model.ReplicaSettings) = Unit

    suspend fun start(request: BroadcastRequest): BroadcastResult

    suspend fun stop()

    /** Releases any held resources. Safe to call when already idle. */
    fun release()
}
