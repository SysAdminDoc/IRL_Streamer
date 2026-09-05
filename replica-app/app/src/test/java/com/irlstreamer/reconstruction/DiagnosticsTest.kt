package com.irlstreamer.reconstruction

import com.irlstreamer.reconstruction.diagnostics.DiagnosticEntry
import com.irlstreamer.reconstruction.diagnostics.DiagnosticsLog
import com.irlstreamer.reconstruction.diagnostics.buildDiagnosticsReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Log tab was a hardcoded string and "send debug details" was a toast, so a
 * user whose stream failed in the field had nothing to send and the developer
 * had nothing to read.
 */
class DiagnosticsTest {

    private val secret = "sk_live_do_not_share_this"

    @Test
    fun recordedLinesComeBackOldestFirst() {
        val log = DiagnosticsLog()

        log.record("state", "CONNECTING", 1_000)
        log.record("state", "LIVE", 2_000)

        assertEquals(listOf("CONNECTING", "LIVE"), log.entries().map { it.message })
    }

    @Test
    fun theLogIsBoundedSoALongBroadcastCannotGrowIt() {
        val log = DiagnosticsLog(capacity = 3)

        repeat(10) { index -> log.record("tick", "entry $index", index.toLong()) }

        val entries = log.entries()
        assertEquals(3, entries.size)
        // The newest are kept: the oldest are the least useful after a failure.
        assertEquals(listOf("entry 7", "entry 8", "entry 9"), entries.map { it.message })
    }

    @Test
    fun aStreamKeyCannotEnterTheLog() {
        val log = DiagnosticsLog()

        log.record("broadcast", "publish to rtmp://ingest.example.com/live/$secret failed", 1_000)

        val recorded = log.entries().single().message
        assertFalse("the key reached the log: $recorded", recorded.contains(secret))
    }

    @Test
    fun theSharedReportCarriesNoKeyEvenIfOneReachesItLate() {
        // Redaction happens on the way in and again on the way out, so a report
        // assembled from anywhere else still cannot leak.
        val report = buildDiagnosticsReport(
            versionName = "0.4.0",
            device = "Samsung SM-S938B",
            androidRelease = "16",
            entries = listOf(
                DiagnosticEntry(1_000, "broadcast", "publish to rtmp://ingest.example.com/live/$secret failed"),
            ),
            formatTimestamp = { "00:00:01" },
        )

        assertFalse("the key reached the report: $report", report.contains(secret))
        assertTrue(report, report.contains("0.4.0"))
        assertTrue(report, report.contains("Samsung SM-S938B"))
        assertTrue(report, report.contains("broadcast"))
    }

    @Test
    fun anEmptyReportStillIdentifiesTheBuild() {
        // A report sent before anything happened is still worth reading.
        val report = buildDiagnosticsReport("0.4.0", "Pixel", "16", emptyList()) { "" }

        assertTrue(report, report.contains("IRL Streamer 0.4.0"))
        assertTrue(report, report.contains("Entries: 0"))
    }
}
