package com.irlstreamer.reconstruction.diagnostics

import com.irlstreamer.reconstruction.model.redactStreamKeysIn
import java.util.ArrayDeque

/** One recorded line. [timestamp] is epoch millis so formatting stays at the edge. */
data class DiagnosticEntry(
    val timestamp: Long,
    val tag: String,
    val message: String,
)

/**
 * The last few hundred things the app did, kept in memory.
 *
 * The console's Log tab used to be a hardcoded string ending `state=SIMULATED`,
 * so a user whose stream failed in the field had nothing to send and the
 * developer had nothing to read. Bounded because a long broadcast would
 * otherwise grow this without limit.
 *
 * Every message is redacted on the way in: a transport error quotes the URL it
 * failed on, and that URL carries the stream key.
 */
class DiagnosticsLog(private val capacity: Int = DEFAULT_CAPACITY) {
    private val entries = ArrayDeque<DiagnosticEntry>()

    @Synchronized
    fun record(tag: String, message: String, timestamp: Long) {
        if (entries.size >= capacity) entries.removeFirst()
        entries.addLast(DiagnosticEntry(timestamp, tag, redactStreamKeysIn(message)))
    }

    /** Oldest first, which is the order a reader wants. */
    @Synchronized
    fun entries(): List<DiagnosticEntry> = entries.toList()

    @Synchronized
    fun clear() = entries.clear()

    companion object {
        const val DEFAULT_CAPACITY = 300
    }
}

/**
 * The text the "send debug details" action shares.
 *
 * Redaction happens on the way into the log as well; doing it again here means
 * a caller that builds a report from anywhere else still cannot leak a key.
 */
fun buildDiagnosticsReport(
    versionName: String,
    device: String,
    androidRelease: String,
    entries: List<DiagnosticEntry>,
    formatTimestamp: (Long) -> String,
): String = buildString {
    appendLine("IRL Streamer $versionName")
    appendLine("Device: $device")
    appendLine("Android: $androidRelease")
    appendLine("Entries: ${entries.size}")
    appendLine()
    entries.forEach { entry ->
        appendLine("${formatTimestamp(entry.timestamp)} ${entry.tag}: ${entry.message}")
    }
}.let(::redactStreamKeysIn)
