package com.irlstreamer.reconstruction

import com.irlstreamer.reconstruction.debug.DebugStateCatalog
import com.irlstreamer.reconstruction.model.AppRoute
import com.irlstreamer.reconstruction.model.AppUiState
import com.irlstreamer.reconstruction.model.DialogType
import com.irlstreamer.reconstruction.model.QuickTab
import com.irlstreamer.reconstruction.model.ReplicaSettings
import com.irlstreamer.reconstruction.model.RuntimeUiState
import com.irlstreamer.reconstruction.model.ScreenOverrides
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugStateCatalogTest {
    @Test
    fun everyCapturedAuditIdHasADeterministicState() {
        (1..145).forEach { number ->
            val id = "%03d_test_state".format(number)
            val state = DebugStateCatalog.resolve(id)
            assertEquals(id, state.debugScreenId)
            assertNotNull(state.route)
        }
    }

    @Test
    fun namedNonDefaultStatesAreReproducible() {
        assertTrue(DebugStateCatalog.resolveNamedState("loading").launchInitializing)
        assertTrue(DebugStateCatalog.resolveNamedState("empty").route is AppRoute.Settings)
        assertEquals(QuickTab.NETWORK, DebugStateCatalog.resolveNamedState("network_error").quickTab)
        assertEquals(
            "Please input non-empty text/html code",
            DebugStateCatalog.resolveNamedState("validation_error").validationError,
        )
    }

    @Test
    fun representativeDialogAndQuickPanelStatesMatchCatalogContract() {
        assertEquals(DialogType.CHOICE_SINGLE, DebugStateCatalog.resolve("041").dialog?.type)
        assertEquals(DialogType.CHOICE_MULTIPLE, DebugStateCatalog.resolve("088").dialog?.type)
        assertTrue(DebugStateCatalog.resolve("130").quickPanelOpen)
        assertEquals(QuickTab.LOG, DebugStateCatalog.resolve("138").quickTab)
        assertTrue(DebugStateCatalog.resolve("139").microphoneMuted)
        assertEquals(2, DebugStateCatalog.resolve("141").currentCameraId)
    }

    @Test
    fun runtimeOverridesTakePrecedenceWithoutMutatingPersistedSettings() {
        val settings = ReplicaSettings(gridVisible = false, wifiWeight = 100)
        val state = AppUiState(
            runtime = RuntimeUiState(overrides = ScreenOverrides(gridVisible = true, wifiWeight = 50)),
            settings = settings,
        )

        assertTrue(state.effectiveGrid)
        assertEquals(50, state.effectiveWifiWeight)
        assertFalse(state.settings.gridVisible)
        assertEquals(100, state.settings.wifiWeight)
    }
}
