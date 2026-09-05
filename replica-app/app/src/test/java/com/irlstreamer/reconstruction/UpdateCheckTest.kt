package com.irlstreamer.reconstruction

import com.irlstreamer.reconstruction.update.UPDATE_CHECK_INTERVAL_MS
import com.irlstreamer.reconstruction.update.isNewerVersion
import com.irlstreamer.reconstruction.update.parseLatestTag
import com.irlstreamer.reconstruction.update.shouldCheckForUpdate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Distribution is signed APKs on GitHub Releases, so nothing pushes an update.
 * Without a check, a user on an old build has no signal that a fix shipped, and
 * the same bug gets reported against a version whose fix already exists.
 */
class UpdateCheckTest {

    @Test
    fun aLaterReleaseIsRecognisedThroughItsTag() {
        // Tags carry a leading v; the installed version does not.
        assertTrue(isNewerVersion("v0.5.0", "0.4.0"))
        assertTrue(isNewerVersion("v0.4.1", "0.4.0"))
        assertTrue(isNewerVersion("v1.0.0", "0.9.9"))
        // Ten is later than nine, which string comparison would get wrong.
        assertTrue(isNewerVersion("v0.10.0", "0.9.0"))
    }

    @Test
    fun theSameOrAnOlderReleaseIsNotAnUpdate() {
        assertFalse(isNewerVersion("v0.4.0", "0.4.0"))
        assertFalse(isNewerVersion("v0.3.9", "0.4.0"))
        // A shorter tag is not automatically older.
        assertFalse(isNewerVersion("v0.4", "0.4.0"))
    }

    @Test
    fun anUnreadableVersionNeverNags() {
        // Better to say nothing than to claim an update that may not exist.
        assertFalse(isNewerVersion("nightly", "0.4.0"))
        assertFalse(isNewerVersion("v0.5.0", "unknown"))
    }

    @Test
    fun theTagIsReadOutOfTheApiResponse() {
        val json = """{"url":"https://api.github.com/x","tag_name":"v0.5.0","name":"IRL Streamer v0.5.0"}"""

        assertEquals("v0.5.0", parseLatestTag(json))
    }

    @Test
    fun aResponseWithoutATagReportsNothing() {
        assertNull(parseLatestTag("""{"message":"Not Found"}"""))
        assertNull(parseLatestTag(""))
    }

    @Test
    fun theCheckIsThrottledToOncePerDay() {
        val now = 1_000_000_000L

        assertFalse(
            "a check ten minutes ago must not run again",
            shouldCheckForUpdate(enabled = true, lastCheckMillis = now - 600_000, nowMillis = now),
        )
        assertTrue(
            "a check a day ago must run",
            shouldCheckForUpdate(enabled = true, lastCheckMillis = now - UPDATE_CHECK_INTERVAL_MS, nowMillis = now),
        )
        // Never checked before.
        assertTrue(shouldCheckForUpdate(enabled = true, lastCheckMillis = 0, nowMillis = now))
    }

    @Test
    fun turningTheCheckOffStopsIt() {
        assertFalse(shouldCheckForUpdate(enabled = false, lastCheckMillis = 0, nowMillis = 1_000_000_000L))
    }

    @Test
    fun aClockThatWentBackwardsDoesNotWedgeTheCheck() {
        // Otherwise a future timestamp would suppress every future check.
        assertTrue(shouldCheckForUpdate(enabled = true, lastCheckMillis = 2_000, nowMillis = 1_000))
    }
}
