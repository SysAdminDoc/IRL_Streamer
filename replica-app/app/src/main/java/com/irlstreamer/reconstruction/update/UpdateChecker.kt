package com.irlstreamer.reconstruction.update

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "UpdateCheck"

/**
 * Asks GitHub for the newest published release tag.
 *
 * Uses `HttpURLConnection` rather than adding an HTTP client: this is one
 * request, once a day, and every dependency here has to be pinned by checksum.
 * Every failure returns null - being offline is the normal case for a phone
 * that streams outdoors, and it must never surface as an error.
 */
suspend fun fetchLatestReleaseTag(): String? = withContext(Dispatchers.IO) {
    var connection: HttpURLConnection? = null
    try {
        connection = (URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            Log.w(TAG, "release check returned ${connection.responseCode}")
            return@withContext null
        }
        parseLatestTag(connection.inputStream.bufferedReader().use { it.readText() })
    } catch (failure: Exception) {
        // Offline, DNS down, rate limited: none of these are worth telling the
        // user about, and none should stop the app doing its job.
        Log.w(TAG, "release check failed: ${failure.javaClass.simpleName}")
        null
    } finally {
        connection?.disconnect()
    }
}
