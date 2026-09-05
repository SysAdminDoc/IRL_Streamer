package com.irlstreamer.reconstruction.model

/** What the encoder should produce. Plain numbers, so it is testable off-device. */
data class VideoFormat(
    val width: Int,
    val height: Int,
    val fps: Int,
)

/**
 * The audited default, and what the Video parameters page shows before anything
 * is chosen: "1920x1080 (16:9)" at "System default".
 */
val DefaultVideoFormat = VideoFormat(width = 1920, height = 1080, fps = 30)

/** Settings-catalog ids for the two rows that describe the encoded picture. */
private const val RESOLUTION_CHOICE = "resolution"
private const val FPS_CHOICE = "fps"

private val RESOLUTION_PATTERN = Regex("""^\s*(\d+)\s*x\s*(\d+)""")
private val FIXED_RATE_PATTERN = Regex("""^\s*(\d+)\s*fixed rate""", RegexOption.IGNORE_CASE)
private val VARIABLE_RATE_PATTERN = Regex("""^\s*(\d+)\s*-\s*(\d+)\s*variable rate""", RegexOption.IGNORE_CASE)

/**
 * The encoder settings the user actually chose.
 *
 * The Video parameters page persists its rows generically, by catalog id, into
 * [ReplicaSettings.choiceValues]. The encoder used to ignore them and hardcode
 * 1920x1080 at 30 fps, so choosing 720p to survive a weak link still sent 1080p.
 *
 * Anything unrecognised falls back to [DefaultVideoFormat] rather than throwing:
 * a label this build does not know about must not stop a broadcast.
 */
fun videoFormatFrom(choiceValues: Map<String, String>): VideoFormat {
    val resolution = choiceValues[RESOLUTION_CHOICE]?.let(::parseResolution)
    val fps = choiceValues[FPS_CHOICE]?.let(::parseFps)
    return VideoFormat(
        width = resolution?.first ?: DefaultVideoFormat.width,
        height = resolution?.second ?: DefaultVideoFormat.height,
        fps = fps ?: DefaultVideoFormat.fps,
    )
}

/** "1280x720 (16:9)" and "1088x1088" both carry the size before any aspect note. */
private fun parseResolution(label: String): Pair<Int, Int>? {
    val match = RESOLUTION_PATTERN.find(label) ?: return null
    val width = match.groupValues[1].toIntOrNull() ?: return null
    val height = match.groupValues[2].toIntOrNull() ?: return null
    if (width <= 0 || height <= 0) return null
    return width to height
}

/**
 * The audited menu offers three shapes.
 *
 * "30 fixed rate" is the rate itself. "7-30 variable rate" is a range, and the
 * encoder takes one number, so the ceiling is used - the range describes how far
 * the camera may drop in poor light, not a lower target. "System default" has no
 * number at all and resolves to [DefaultVideoFormat].fps, which is what the
 * audited console reports.
 */
private fun parseFps(label: String): Int? {
    FIXED_RATE_PATTERN.find(label)?.let { return it.groupValues[1].toIntOrNull()?.takeIf { rate -> rate > 0 } }
    VARIABLE_RATE_PATTERN.find(label)?.let { return it.groupValues[2].toIntOrNull()?.takeIf { rate -> rate > 0 } }
    return null
}
