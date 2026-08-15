package com.irlstreamer.reconstruction

import com.irlstreamer.reconstruction.model.AppUiState
import com.irlstreamer.reconstruction.model.DialogRequest
import com.irlstreamer.reconstruction.model.DialogType
import com.irlstreamer.reconstruction.model.ReplicaSettings
import com.irlstreamer.reconstruction.model.RuntimeUiState
import com.irlstreamer.reconstruction.ui.settings.withCurrentValue
import org.junit.Assert.assertEquals
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
    fun extraTogglesRoundTripThroughTheGenericMap() {
        val settings = ReplicaSettings(extraToggles = mapOf("prefer_bluetooth" to true, "aec" to false))

        assertEquals(true, settings.extraToggles["prefer_bluetooth"])
        assertEquals(false, settings.extraToggles["aec"])
        assertEquals(null, settings.extraToggles["never_set"])
    }
}
