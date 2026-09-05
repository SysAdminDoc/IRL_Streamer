package com.irlstreamer.reconstruction

import com.irlstreamer.reconstruction.data.ReplicaSettingsRepository
import com.irlstreamer.reconstruction.update.UPDATE_CHECK_INTERVAL_MS
import com.irlstreamer.reconstruction.update.isNewerVersion
import com.irlstreamer.reconstruction.update.parseLatestTag
import com.irlstreamer.reconstruction.update.shouldCheckForUpdate
import kotlinx.coroutines.flow.first
import org.junit.Rule
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
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

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

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
    fun aNumberedPreReleaseTagIsNotMistakenForANewerRelease() {
        // Regression: taking every digit made the "1" in rc1 a fourth part, so
        // an rc that precedes 0.4.0 reported as newer than it.
        assertFalse(isNewerVersion("v0.4.0-rc1", "0.4.0"))
        assertFalse(isNewerVersion("v0.4.0", "0.4.0-rc1"))
        // A real later release is still recognised across a suffix.
        assertTrue(isNewerVersion("v0.5.0-rc1", "0.4.0"))
    }

    @Test
    fun aDebugBuildIsNotToldItIsBehindItsOwnVersion() {
        // Debug builds append -debug to versionName.
        assertFalse(isNewerVersion("v0.4.0", "0.4.0-debug"))
        assertTrue(isNewerVersion("v0.5.0", "0.4.0-debug"))
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

    @Test
    fun theConsoleAsksForTheLatestReleaseOnceAndRecordsWhatItFound() = runTest {
        // The check runs from init, so nothing in the app triggers it and
        // nothing else would notice if it stopped happening.
        val repository = ReplicaSettingsRepository(InMemoryPreferencesDataStore(), FakeSecretCipher())
        var calls = 0

        MainViewModel(repository, latestReleaseTag = { calls++; "v9.9.9" })
        advanceUntilIdle()

        assertEquals(1, calls)
        assertEquals("v9.9.9", repository.settings.first().updateLatestTag)
    }

    @Test
    fun turningTheCheckOffActuallyStopsIt() = runTest {
        // The switch used to be stored as a generic toggle, under a key the
        // check does not read, so turning it off changed nothing.
        val repository = ReplicaSettingsRepository(InMemoryPreferencesDataStore(), FakeSecretCipher())
        var calls = 0
        // Returns nothing, so no check is recorded and the once-a-day guard
        // cannot be what stops the second console from looking.
        val fetch: suspend () -> String? = { calls++; null }

        val console = MainViewModel(repository, latestReleaseTag = fetch)
        advanceUntilIdle()
        assertEquals(1, calls)

        console.toggleBoolean("update_check_enabled", current = true)
        advanceUntilIdle()
        MainViewModel(repository, latestReleaseTag = fetch)
        advanceUntilIdle()

        assertEquals("the switch did not reach the setting the check reads", 1, calls)
    }
}
