package com.irlstreamer.reconstruction.model

/** Stands in for the hidden part of a destination URL. */
private const val MASK = "••••••"

/** Transient runtime key for "the user asked to see the stream key". */
const val REVEAL_STREAM_KEY = "reveal_stream_key"

/**
 * A destination URL with its stream key hidden.
 *
 * `rtmp://server/application/streamkey` is the shape every RTMP destination
 * uses, so the key is the last path segment and anything in the query string.
 * Scheme, host and the application path stay readable, because those are what
 * a user needs to recognise which destination a row refers to.
 *
 * This exists because a stream key on screen is the channel-hijack path that
 * key rotation exists to undo, and a phone streaming itself shows its own
 * screen. A blank or key-less URL is returned unchanged rather than decorated
 * with a mask that hides nothing.
 */
fun redactStreamKey(url: String): String {
    val trimmed = url.trim()
    if (trimmed.isEmpty()) return trimmed

    val schemeEnd = trimmed.indexOf("://")
    if (schemeEnd < 0) return trimmed
    val afterScheme = schemeEnd + 3

    // Query first: some ingests carry the key there instead of in the path.
    val queryStart = trimmed.indexOfFirst(afterScheme) { it == '?' }
    val withoutQuery = if (queryStart >= 0) trimmed.substring(0, queryStart) else trimmed
    val maskedQuery = if (queryStart >= 0) "?$MASK" else ""

    val lastSlash = withoutQuery.lastIndexOf('/')
    // No path at all, or a bare host with a trailing slash: nothing to hide.
    if (lastSlash < afterScheme || lastSlash == withoutQuery.lastIndex) {
        return withoutQuery + maskedQuery
    }
    return withoutQuery.substring(0, lastSlash + 1) + MASK + maskedQuery
}

/** True when [redactStreamKey] would actually hide something. */
fun hasStreamKey(url: String): Boolean = redactStreamKey(url) != url.trim()

private inline fun String.indexOfFirst(startIndex: Int, predicate: (Char) -> Boolean): Int {
    for (index in startIndex until length) {
        if (predicate(this[index])) return index
    }
    return -1
}
