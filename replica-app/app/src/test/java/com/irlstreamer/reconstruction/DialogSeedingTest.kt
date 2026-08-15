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
 * A dialog must edit the value it is opened over. Seeding it from the catalog
 * default made reopening a dialog silently revert the saved setting on confirm.
 */
class DialogSeedingTest {

    @Test
    fun numberDialogReopensWithThePersistedValueNotTheCatalogDefault() {
        val state = AppUiState(settings = ReplicaSettings(chatFontScale = 300))
        val catalogRequest = DialogRequest("chat_font_scale", "Chat font scale", DialogType.NUMBER, initialValue = "220")

        assertEquals("300", withCurrentValue(catalogRequest, state).initialValue)
    }

    @Test
    fun multiSelectReopensWithEveryPersistedSelection() {
        // The regression this guards: reopening showed only the catalog default
        // and confirming dropped every other ratio the user had saved.
        val saved = setOf("16:9 (1.78)", "21:9 (2.33)")
        val state = AppUiState(settings = ReplicaSettings(safeMarginRatios = saved))
        val catalogRequest = DialogRequest(
            "safe_margin_ratios",
            "Safe margins ratios",
            DialogType.CHOICE_MULTIPLE,
            options = listOf("16:9 (1.78)", "21:9 (2.33)"),
            selectedOptions = setOf("16:9 (1.78)"),
        )

        assertEquals(saved, withCurrentValue(catalogRequest, state).selectedOptions)
    }

    @Test
    fun singleChoiceReopensOnTheSessionSelection() {
        val state = AppUiState(
            runtime = RuntimeUiState(transientValues = mapOf("resolution" to "1280x720 (16:9)")),
        )
        val catalogRequest = DialogRequest(
            "resolution",
            "Resolution",
            DialogType.CHOICE_SINGLE,
            options = listOf("1920x1080 (16:9)", "1280x720 (16:9)"),
            selectedOptions = setOf("1920x1080 (16:9)"),
        )

        assertEquals(setOf("1280x720 (16:9)"), withCurrentValue(catalogRequest, state).selectedOptions)
    }

    @Test
    fun aValueWithNoStoredCounterpartKeepsTheAuditedDefault() {
        val request = DialogRequest("keyframe", "Keyframe frequency", DialogType.NUMBER, initialValue = "2")

        assertEquals(request, withCurrentValue(request, AppUiState()))
    }
}
