package com.irlstreamer.reconstruction.diagnostics

import android.content.Context
import android.util.Log
import com.irlstreamer.reconstruction.model.redactStreamKeysIn
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "CrashReporter"

/** Where crash files live. Inside the app's own storage, never on shared media. */
internal const val CRASH_DIRECTORY = "crashes"

/** Keep a handful, so a crash loop cannot fill the device. */
internal const val MAX_CRASH_FILES = 5

/**
 * Writes an uncaught exception to a file before the process dies.
 *
 * Without this a crash in the field leaves nothing behind: there is no store to
 * collect reports and no logcat once the phone has been unplugged. The previous
 * handler is always called, so the system still does whatever it would have.
 */
fun installCrashReporter(context: Context, log: DiagnosticsLog) {
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        runCatching { writeCrashFile(context, log, thread.name, throwable) }
            .onFailure { Log.e(TAG, "could not write the crash file: ${it.javaClass.simpleName}") }
        previous?.uncaughtException(thread, throwable)
    }
}

private fun writeCrashFile(context: Context, log: DiagnosticsLog, threadName: String, throwable: Throwable) {
    val directory = File(context.filesDir, CRASH_DIRECTORY).apply { mkdirs() }
    val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
    val stackTrace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()

    // A stack trace can carry the destination URL through an exception message,
    // so the whole report goes through redaction before it touches the disk.
    val report = redactStreamKeysIn(
        buildString {
            appendLine("thread: $threadName")
            appendLine()
            append(stackTrace)
            appendLine()
            appendLine("--- recent activity ---")
            log.entries().forEach { appendLine("${it.tag}: ${it.message}") }
        },
    )

    File(directory, "crash-$stamp.txt").writeText(report)
    pruneOldCrashes(directory)
}

/** Oldest first, so a crash loop leaves the most recent evidence rather than the first. */
private fun pruneOldCrashes(directory: File) {
    val files = directory.listFiles()?.sortedBy { it.name } ?: return
    files.dropLast(MAX_CRASH_FILES).forEach { it.delete() }
}

/** Every crash report on disk, newest first. */
fun readCrashReports(context: Context): List<String> {
    val directory = File(context.filesDir, CRASH_DIRECTORY)
    val files = directory.listFiles()?.sortedByDescending { it.name } ?: return emptyList()
    return files.mapNotNull { file -> runCatching { file.readText() }.getOrNull() }
}
