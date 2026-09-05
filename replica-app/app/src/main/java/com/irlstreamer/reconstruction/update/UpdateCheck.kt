package com.irlstreamer.reconstruction.update

/** Where releases are published. Distribution is signed APKs here and nowhere else. */
const val RELEASES_URL = "https://github.com/SysAdminDoc/IRL_Streamer/releases/latest"

/** The API the check reads. */
internal const val LATEST_RELEASE_API = "https://api.github.com/repos/SysAdminDoc/IRL_Streamer/releases/latest"

/** At most one check a day: a new release is not worth a request per launch. */
internal const val UPDATE_CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000

private val TAG_NAME = Regex("\"tag_name\"\\s*:\\s*\"([^\"]+)\"")
/** `v0.4.0-rc1` and `0.4.0` both yield `0.4.0`; the suffix is deliberately dropped. */
private val RELEASE_CORE = Regex("""^[^0-9]*(\d+(?:\.\d+)*)""")

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
 * Only the dotted release number is compared. A tag carries a leading `v`, and
 * either side can carry a suffix - `-rc1` on a pre-release, `-debug` on a debug
 * build - so taking every digit in the string made `v0.4.0-rc1` look newer than
 * `0.4.0` because the `1` became a fourth part.
 *
 * Equal release numbers report "not newer" whatever the suffixes say. Ordering
 * pre-releases is not something this needs to do, and a false "update
 * available" is worse than a missed one: it nags about a build that is not
 * actually ahead.
 */
fun isNewerVersion(latest: String, current: String): Boolean {
    val latestParts = releaseNumbers(latest)
    val currentParts = releaseNumbers(current)
    if (latestParts.isEmpty() || currentParts.isEmpty()) return false

    for (index in 0 until maxOf(latestParts.size, currentParts.size)) {
        val l = latestParts.getOrElse(index) { 0 }
        val c = currentParts.getOrElse(index) { 0 }
        if (l != c) return l > c
    }
    return false
}

/** The leading dotted number, ignoring a `v` prefix and any suffix after it. */
private fun releaseNumbers(value: String): List<Int> {
    val core = RELEASE_CORE.find(value)?.groupValues?.get(1) ?: return emptyList()
    return core.split('.').mapNotNull { it.toIntOrNull() }
}
