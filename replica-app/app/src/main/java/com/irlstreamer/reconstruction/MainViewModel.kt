package com.irlstreamer.reconstruction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.irlstreamer.reconstruction.data.ReplicaSettingsRepository
import com.irlstreamer.reconstruction.debug.DebugStateCatalog
import com.irlstreamer.reconstruction.model.AppRoute
import com.irlstreamer.reconstruction.model.AppUiState
import com.irlstreamer.reconstruction.model.DialogRequest
import com.irlstreamer.reconstruction.model.DialogType
import com.irlstreamer.reconstruction.model.QuickTab
import com.irlstreamer.reconstruction.model.ReplicaSettings
import com.irlstreamer.reconstruction.model.RuntimeUiState
import com.irlstreamer.reconstruction.model.SettingsPage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val repository: ReplicaSettingsRepository) : ViewModel() {
    private val runtime = MutableStateFlow(RuntimeUiState())

    val uiState: StateFlow<AppUiState> = combine(runtime, repository.settings) { runtimeState, settings ->
        AppUiState(runtimeState, settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState(settings = ReplicaSettings()))

    fun navigateTo(page: SettingsPage) {
        runtime.value = runtime.value.let { current ->
            current.copy(
                backStack = current.backStack + current.route,
                route = AppRoute.Settings(page),
                dialog = null,
                settingsScrollIndex = 0,
                debugScreenId = null,
                overrides = com.irlstreamer.reconstruction.model.ScreenOverrides(),
                formVariant = null,
                validationError = null,
            )
        }
    }

    fun navigateToLive() {
        runtime.value = runtime.value.copy(
            route = AppRoute.LiveConsole,
            backStack = emptyList(),
            dialog = null,
            quickPanelOpen = false,
            settingsScrollIndex = 0,
        )
    }

    /** Returns true when the app consumed Back; false means the activity should background itself. */
    fun back(): Boolean {
        val state = runtime.value
        when {
            state.dialog != null -> runtime.value = state.copy(dialog = null)
            state.quickPanelOpen -> runtime.value = state.copy(quickPanelOpen = false)
            state.route is AppRoute.Settings -> {
                val previous = state.backStack.lastOrNull() ?: AppRoute.LiveConsole
                runtime.value = state.copy(
                    route = previous,
                    backStack = state.backStack.dropLast(1),
                    settingsScrollIndex = 0,
                    formVariant = null,
                    validationError = null,
                )
            }
            else -> return false
        }
        return true
    }

    fun showDialog(dialog: DialogRequest) {
        runtime.value = runtime.value.copy(dialog = dialog)
    }

    fun dismissDialog() {
        runtime.value = runtime.value.copy(dialog = null)
    }

    fun confirmDialog(value: String = "", selected: Set<String> = emptySet()) {
        val dialog = runtime.value.dialog ?: return
        if (dialog.type == DialogType.CHOICE_SINGLE && selected.isNotEmpty()) {
            runtime.value = runtime.value.copy(
                transientValues = runtime.value.transientValues + (dialog.id to selected.first()),
            )
        }
        when (dialog.id) {
            "chat_font_scale" -> value.toIntOrNull()?.let { setInt("chat_font_scale", it) }
            "alert_dashboard_scale" -> value.toIntOrNull()?.let { setInt("alert_dashboard_scale", it) }
            "h264_bitrate_kbps" -> value.toIntOrNull()?.let { setInt("h264_bitrate_kbps", it) }
            "safe_margin_ratios" -> setStringSet("safe_margin_ratios", selected)
            "no_connection" -> navigateTo(SettingsPage.CONNECTION_FORM)
            "layer_type" -> when (selected.firstOrNull()) {
                "Text" -> navigateTo(SettingsPage.TEXT_LAYER_FORM)
                "Picture" -> navigateTo(SettingsPage.PICTURE_LAYER_FORM)
            }
            "reset_settings" -> viewModelScope.launch { repository.reset() }
        }
        runtime.value = runtime.value.copy(dialog = null)
    }

    fun applyDebugScreen(screenId: String) {
        runtime.value = DebugStateCatalog.resolve(screenId)
    }

    fun applyNamedState(name: String) {
        runtime.value = DebugStateCatalog.resolveNamedState(name)
    }

    fun toggleQuickPanel() {
        runtime.value = runtime.value.copy(quickPanelOpen = !runtime.value.quickPanelOpen)
    }

    fun selectQuickTab(tab: QuickTab) {
        runtime.value = runtime.value.copy(quickTab = tab, quickPanelLower = false)
    }

    fun setQuickPanelLower(lower: Boolean) {
        runtime.value = runtime.value.copy(quickPanelLower = lower)
    }

    fun setCurrentCamera(cameraId: Int) {
        runtime.value = runtime.value.copy(currentCameraId = cameraId)
    }

    fun flipCamera() {
        runtime.value = runtime.value.copy(currentCameraId = if (runtime.value.currentCameraId == 1) 0 else 1)
    }

    fun toggleMicrophone() {
        runtime.value = runtime.value.copy(microphoneMuted = !runtime.value.microphoneMuted)
    }

    fun showNoConnectionGuard() {
        runtime.value = runtime.value.copy(dialog = DebugStateCatalog.noConnectionDialog())
    }

    fun reload() {
        runtime.value = runtime.value.copy(toastMessage = "Reloading chat and all web overlays, please wait a few seconds…")
    }

    fun showToast(message: String) {
        runtime.value = runtime.value.copy(toastMessage = message)
    }

    fun consumeToast() {
        runtime.value = runtime.value.copy(toastMessage = null, validationError = null)
    }

    fun consumeFolderRequest() {
        runtime.value = runtime.value.copy(requestFolderPicker = false)
    }

    fun requestFolderPicker() {
        runtime.value = runtime.value.copy(requestFolderPicker = true)
    }

    fun toggleBoolean(key: String, current: Boolean) {
        if (key in persistedBooleanKeys) {
            setBoolean(key, !current)
        } else {
            runtime.value = runtime.value.copy(
                transientBooleans = runtime.value.transientBooleans + (key to !current),
            )
        }
    }

    fun setBoolean(key: String, value: Boolean) {
        viewModelScope.launch { repository.setBoolean(key, value) }
    }

    fun setInt(key: String, value: Int) {
        viewModelScope.launch { repository.setInt(key, value) }
    }

    fun setFloat(key: String, value: Float) {
        viewModelScope.launch { repository.setFloat(key, value) }
    }

    fun setStringSet(key: String, value: Set<String>) {
        viewModelScope.launch { repository.setStringSet(key, value) }
    }

    class Factory(private val repository: ReplicaSettingsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(repository) as T
    }

    private companion object {
        val persistedBooleanKeys = setOf(
            "show_platform_icons",
            "show_bots",
            "show_commands",
            "show_user_count",
            "cellular_enabled",
            "wifi_enabled",
            "ethernet_enabled",
            "bitrate_matches_resolution",
            "record_stream",
            "split_sections",
            "audio_meter_visible",
            "grid_visible",
            "safe_margins_visible",
            "timestamp_active",
            "web_overlay_master",
        )
    }
}
