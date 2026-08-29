package com.irlstreamer.reconstruction

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.irlstreamer.reconstruction.data.ReplicaSettingsRepository
import com.irlstreamer.reconstruction.model.AppUiState
import com.irlstreamer.reconstruction.model.DialogRequest
import com.irlstreamer.reconstruction.model.DialogType
import com.irlstreamer.reconstruction.model.ReplicaSettings
import com.irlstreamer.reconstruction.model.RuntimeUiState
import com.irlstreamer.reconstruction.ui.settings.withCurrentValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Persisted values must survive a relaunch and must not leak. The generic
 * toggle/choice maps replaced a fifteen-key whitelist that left roughly twenty
 * audited switches and every choice value in memory only.
 */
class SettingsPersistenceTest {

    @Test
    fun aPersistedChoiceSeedsItsDialogAfterRelaunch() {
        // No runtime state at all - this is what a cold start looks like.
        val state = AppUiState(
            settings = ReplicaSettings(choiceValues = mapOf("resolution" to "1280x720 (16:9)")),
        )
        val request = DialogRequest(
            "resolution",
            "Resolution",
            DialogType.CHOICE_SINGLE,
            options = listOf("1920x1080 (16:9)", "1280x720 (16:9)"),
            selectedOptions = setOf("1920x1080 (16:9)"),
        )

        assertEquals(setOf("1280x720 (16:9)"), withCurrentValue(request, state).selectedOptions)
    }

    @Test
    fun aSessionValueStillWinsOverThePersistedOne() {
        val state = AppUiState(
            runtime = RuntimeUiState(transientValues = mapOf("resolution" to "640x480")),
            settings = ReplicaSettings(choiceValues = mapOf("resolution" to "1280x720 (16:9)")),
        )
        val request = DialogRequest("resolution", "Resolution", DialogType.CHOICE_SINGLE, selectedOptions = setOf("1920x1080 (16:9)"))

        assertEquals(setOf("640x480"), withCurrentValue(request, state).selectedOptions)
    }

    @Test
    fun resetClearsEverythingAndUndoPutsItAllBack() = runBlocking {
        val repository = ReplicaSettingsRepository(InMemoryPreferencesDataStore())
        repository.setBoolean("grid_visible", true)
        repository.setInt("h264_bitrate_kbps", 4200)
        repository.setStringSet("safe_margin_ratios", setOf("21:9 (2.33)"))
        repository.setExtraToggle("prefer_bluetooth", true)
        repository.setChoiceValue("resolution", "1280x720 (16:9)")
        val populated = repository.settings.first()
        assertNotEquals(ReplicaSettings(), populated)

        val snapshot = repository.reset()
        val cleared = repository.settings.first()
        assertEquals("reset must fall back to defaults", ReplicaSettings(), cleared)
        assertTrue(cleared.extraToggles.isEmpty())
        assertTrue(cleared.choiceValues.isEmpty())

        repository.restore(snapshot)
        assertEquals("undo must put every key back", populated, repository.settings.first())
    }

    /**
     * Storage stands in for the file-backed store: the JVM DataStore cannot write
     * under a Windows unit test (its .tmp rename fails), and the behaviour under
     * test is the repository's snapshot-and-restore, not persistence to disk.
     * `edit` and the transform below are the library's own code either way.
     */
    private class InMemoryPreferencesDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> = state
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }

    @Test
    fun extraTogglesRoundTripThroughTheGenericMap() {
        val settings = ReplicaSettings(extraToggles = mapOf("prefer_bluetooth" to true, "aec" to false))

        assertEquals(true, settings.extraToggles["prefer_bluetooth"])
        assertEquals(false, settings.extraToggles["aec"])
        assertEquals(null, settings.extraToggles["never_set"])
    }
}
