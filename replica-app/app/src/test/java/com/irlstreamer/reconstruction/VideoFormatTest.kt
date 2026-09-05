package com.irlstreamer.reconstruction

import com.irlstreamer.reconstruction.model.DefaultVideoFormat
import com.irlstreamer.reconstruction.model.VideoFormat
import com.irlstreamer.reconstruction.model.videoFormatFrom
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The Video parameters page offered nine resolutions and eleven frame rates and
 * the encoder ignored all of them, so choosing 720p to survive a weak link still
 * sent 1080p. These pin every label the audited menus actually offer.
 */
class VideoFormatTest {

    @Test
    fun theChosenResolutionReachesTheEncoder() {
        assertEquals(
            VideoFormat(1280, 720, 30),
            videoFormatFrom(mapOf("resolution" to "1280x720 (16:9)", "fps" to "30 fixed rate")),
        )
    }

    @Test
    fun everyAuditedResolutionLabelParses() {
        val labels = listOf(
            "1920x1080 (16:9)" to (1920 to 1080),
            "1920x824" to (1920 to 824),
            "1440x1080" to (1440 to 1080),
            "1280x720 (16:9)" to (1280 to 720),
            "1088x1088" to (1088 to 1088),
            "960x720" to (960 to 720),
            "720x480" to (720 to 480),
            "640x480" to (640 to 480),
            "640x360 (16:9)" to (640 to 360),
        )
        for ((label, expected) in labels) {
            val format = videoFormatFrom(mapOf("resolution" to label))
            assertEquals(label, expected.first, format.width)
            assertEquals(label, expected.second, format.height)
        }
    }

    @Test
    fun everyAuditedFrameRateLabelParses() {
        // A fixed rate is the rate. A variable range hands the encoder its
        // ceiling, because the range describes how far the camera may drop in
        // poor light rather than a lower target.
        val labels = mapOf(
            "System default" to DefaultVideoFormat.fps,
            "7-10 variable rate" to 10,
            "10 fixed rate" to 10,
            "7-15 variable rate" to 15,
            "15 fixed rate" to 15,
            "7-24 variable rate" to 24,
            "24 fixed rate" to 24,
            "7-30 variable rate" to 30,
            "30 fixed rate" to 30,
            "60 fixed rate" to 60,
            "120 fixed rate" to 120,
        )
        for ((label, expected) in labels) {
            assertEquals(label, expected, videoFormatFrom(mapOf("fps" to label)).fps)
        }
    }

    @Test
    fun nothingChosenKeepsTheAuditedDefault() {
        assertEquals(DefaultVideoFormat, videoFormatFrom(emptyMap()))
    }

    @Test
    fun anUnknownLabelFallsBackRatherThanBreakingABroadcast() {
        // A label a future build adds must not stop the stream starting.
        val format = videoFormatFrom(mapOf("resolution" to "Cinematic", "fps" to "as fast as possible"))

        assertEquals(DefaultVideoFormat, format)
    }

    @Test
    fun aDegenerateLabelIsRejectedRatherThanUsedAsZero() {
        // 0x0 or 0 fps would configure an encoder that cannot start.
        assertEquals(DefaultVideoFormat, videoFormatFrom(mapOf("resolution" to "0x0", "fps" to "0 fixed rate")))
    }
}
