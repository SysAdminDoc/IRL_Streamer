package com.irlstreamer.reconstruction

import com.irlstreamer.reconstruction.engine.BroadcastStatistics
import com.irlstreamer.reconstruction.engine.withEndpointMetrics
import io.github.thibaultbee.streampack.core.elements.metrics.BasicEndpointMetrics
import io.github.thibaultbee.streampack.core.elements.metrics.TrackedMetrics
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Uptime and the dropped counts used to be reset to zero and the bitrate echoed
 * back from settings, so the console's numbers could not tell a healthy stream
 * from a failing one. These pin the mapping from what the endpoint measured.
 */
class EndpointMetricsTest {

    private class Sample(
        override val uptime: Duration = Duration.ZERO,
        override val packetsWritten: Long = 0,
        override val packetsWriteDropped: Long = 0,
        override val packetsWriteLost: Long = 0,
        override val bytesWritten: Long = 0,
        override val bytesWriteDropped: Long = 0,
    ) : BasicEndpointMetrics

    @Test
    fun uptimeAndBytesComeFromTheEndpointRatherThanBeingReset() {
        val stats = BroadcastStatistics().withEndpointMetrics(
            TrackedMetrics(
                instant = Sample(uptime = 1.seconds, bytesWritten = 750_000),
                cumulative = Sample(uptime = 42.seconds, bytesWritten = 31_000_000),
            ),
        )

        assertEquals(42L, stats.uptimeSeconds)
        assertEquals(31_000_000L, stats.bytesSent)
    }

    @Test
    fun theBitrateIsMeasuredOverTheLastIntervalNotTheWholeBroadcast() {
        // 750000 bytes in one second is 6000 kbps. Using the cumulative total
        // here would report an average that hides a stream that just collapsed.
        val stats = BroadcastStatistics().withEndpointMetrics(
            TrackedMetrics(
                instant = Sample(uptime = 1.seconds, bytesWritten = 750_000),
                cumulative = Sample(uptime = 600.seconds, bytesWritten = 450_000_000),
            ),
        )

        assertEquals(6_000, stats.currentBitrateKbps)
    }

    @Test
    fun droppedAndLostPacketsBothCountAsLoss() {
        // Both mean payload that never reached the destination.
        val stats = BroadcastStatistics().withEndpointMetrics(
            TrackedMetrics(
                instant = Sample(uptime = 1.seconds),
                cumulative = Sample(uptime = 10.seconds, packetsWriteDropped = 12, packetsWriteLost = 30),
            ),
        )

        assertEquals(42, stats.droppedFrames)
    }

    @Test
    fun anIdleEndpointReportsZeroRatherThanANegativeReading() {
        val stats = BroadcastStatistics(currentBitrateKbps = 6_000).withEndpointMetrics(
            TrackedMetrics(instant = Sample(), cumulative = Sample()),
        )

        assertEquals(0, stats.currentBitrateKbps)
        assertEquals(0L, stats.uptimeSeconds)
        assertEquals(0, stats.droppedFrames)
        assertEquals(0L, stats.bytesSent)
    }

    @Test
    fun configuredValuesSurviveTheMetricsUpdate() {
        // The target and the link list come from settings, not from the wire.
        val stats = BroadcastStatistics(targetBitrateKbps = 6_000, fps = 30)
            .withEndpointMetrics(
                TrackedMetrics(
                    instant = Sample(uptime = 1.seconds, bytesWritten = 125_000),
                    cumulative = Sample(uptime = 5.seconds),
                ),
            )

        assertEquals(6_000, stats.targetBitrateKbps)
        assertEquals(30, stats.fps)
        assertEquals(1_000, stats.currentBitrateKbps)
    }
}
