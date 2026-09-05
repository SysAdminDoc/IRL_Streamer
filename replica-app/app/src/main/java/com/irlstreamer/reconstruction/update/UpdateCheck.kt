package com.irlstreamer.reconstruction.update

/** Where releases are published. Distribution is signed APKs here and nowhere else. */
const val RELEASES_URL = "https://github.com/SysAdminDoc/IRL_Streamer/releases/latest"

/** The API the check reads. */
internal const val LATEST_RELEASE_API = "https://api.github.com/repos/SysAdminDoc/IRL_Streamer/releases/latest"

/** At most one check a day: a new release is not worth a request per launch. */
internal const val UPDATE_CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000

private val TAG_NAME = Regex("\"tag_name\"\\s*:\\s*\"([^\"]+)\"")
private val VERSION_NUMBERS = Regex("\\d+")

/**
 * Whether to ask GitHub for the latest release now.
 *
 * There is no store to push updates here, so the app has to look. Throttling
 * keeps that to once a day, and a user who does not want it can turn it off.
 */
fun shouldCheckForUpdate(
    enabled: Boolean,
    lastCheckMillis: Long,
    nowMillis: Long,
): Boolean {
    if (!enabled) return false
    // A clock that moved backwards would otherwise suppress the check forever.
    if (nowMillis < lastCheckMillis) return true
    return nowMillis - lastCheckMillis >= UPDATE_CHECK_INTERVAL_MS
}

/** Pulls the release tag out of the API response, or null if it is not there. */
fun parseLatestTag(json: String): String? =
    TAG_NAME.find(json)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }

/**
 * True when [latest] is a later release than [current].
 *
 * Tags carry a leading `v` and versions have a varying number of parts, so both
 * are reduced to their numbers and compared piecewise. A version that cannot be
 * read as numbers reports "not newer" rather than nagging about an update that
 * may not exist.
 */
fun isNewerVersion(latest: String, current: String): Boolean {
    val latestParts = versionNumbers(latest)
    val currentParts = versionNumbers(current)
    if (latestParts.isEmpty() || currentParts.isEmpty()) return false

    for (index in 0 until maxOf(latestParts.size, currentParts.size)) {
        val l = latestParts.getOrElse(index) { 0 }
        val c = currentParts.getOrElse(index) { 0 }
        if (l != c) return l > c
    }
    return false
}

private fun versionNumbers(value: String): List<Int> =
    VERSION_NUMBERS.findAll(value).mapNotNull { it.value.toIntOrNull() }.toList()
