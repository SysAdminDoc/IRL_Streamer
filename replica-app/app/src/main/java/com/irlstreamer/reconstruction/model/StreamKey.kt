package com.irlstreamer.reconstruction.model

/** Stands in for the hidden part of a destination URL. */
private const val MASK = "••••••"

/** Transient runtime key for "the user asked to see the stream key". */
const val REVEAL_STREAM_KEY = "reveal_stream_key"

/** Any destination URL embedded in free text, so it can be redacted in place. */
private val URL_IN_TEXT = Regex("""\b[A-Za-z][A-Za-z0-9+.\-]*://\S+""")

/**
 * A destination URL with its stream key hidden.
 *
 * `rtmp://server/application/streamkey` is the shape every RTMP destination
 * uses, so the key is the last path segment and anything in the query string.
 * Scheme, host and the application path stay readable, because those are what
 * a user needs to recognise which destination a row refers to. A password in
 * `user:password@host` is hidden too - it is the same kind of secret sitting in
 * the same string.
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

    // Split the tail off first: a key can hide in any of the three parts, and
    // each is masked on its own so the rest stays readable.
    val fragmentStart = trimmed.indexOf('#', afterScheme)
    val beforeFragment = if (fragmentStart >= 0) trimmed.substring(0, fragmentStart) else trimmed
    val maskedFragment = if (fragmentStart >= 0) "#$MASK" else ""

    val queryStart = beforeFragment.indexOf('?', afterScheme)
    val beforeQuery = if (queryStart >= 0) beforeFragment.substring(0, queryStart) else beforeFragment
    val maskedQuery = if (queryStart >= 0) "?$MASK" else ""

    val withoutCredentials = maskCredentials(beforeQuery, afterScheme)
    return maskLastPathSegment(withoutCredentials, afterScheme) + maskedQuery + maskedFragment
}

/** True when [redactStreamKey] would actually hide something. */
fun hasStreamKey(url: String): Boolean = redactStreamKey(url) != url.trim()

/**
 * Redacts every destination URL inside a longer message.
 *
 * Transport errors from the streaming library routinely quote the URL they
 * failed on, and those messages are shown to the user and written to the log,
 * so passing one through unchanged puts the key back on screen.
 */
fun redactStreamKeysIn(text: String): String =
    URL_IN_TEXT.replace(text) { match ->
        // A URL at the end of a sentence picks up its punctuation, and masking
        // that as part of the key would eat the full stop or close-paren.
        val trailing = match.value.takeLastWhile { it in TRAILING_PUNCTUATION }
        redactStreamKey(match.value.dropLast(trailing.length)) + trailing
    }

/** Characters that end a sentence rather than a URL. */
private const val TRAILING_PUNCTUATION = ".,;:!?)]}\"'"

/** `rtmp://user:password@host/...` keeps the user and hides the password. */
private fun maskCredentials(url: String, afterScheme: Int): String {
    val authorityEnd = url.indexOf('/', afterScheme).let { if (it < 0) url.length else it }
    val at = url.lastIndexOf('@', authorityEnd - 1)
    if (at < afterScheme) return url
    val colon = url.indexOf(':', afterScheme)
    if (colon !in afterScheme until at) return url
    return url.substring(0, colon + 1) + MASK + url.substring(at)
}

/**
 * Hides the last non-empty path segment.
 *
 * The segment is found past any trailing slashes: `rtmp://host/app/key/` still
 * carries the key, and treating a trailing slash as "no path" left it fully
 * exposed and reported nothing to hide.
 */
private fun maskLastPathSegment(url: String, afterScheme: Int): String {
    val pathStart = url.indexOf('/', afterScheme)
    if (pathStart < 0) return url

    var end = url.length
    while (end > pathStart && url[end - 1] == '/') end--
    if (end <= pathStart) return url

    val lastSlash = url.lastIndexOf('/', end - 1)
    if (lastSlash < pathStart) return url
    return url.substring(0, lastSlash + 1) + MASK + url.substring(end)
}
