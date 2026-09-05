package com.irlstreamer.reconstruction

import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.irlstreamer.reconstruction.data.ReplicaSettingsRepository
import com.irlstreamer.reconstruction.debug.Harness
import com.irlstreamer.reconstruction.engine.BroadcastEngine
import com.irlstreamer.reconstruction.engine.BroadcastFailure
import com.irlstreamer.reconstruction.engine.BroadcastRequest
import com.irlstreamer.reconstruction.engine.BroadcastResult
import com.irlstreamer.reconstruction.engine.BroadcastState
import com.irlstreamer.reconstruction.engine.SimulatedBroadcastEngine
import com.irlstreamer.reconstruction.engine.StreamPackBroadcastEngine
import io.github.thibaultbee.streampack.core.interfaces.IWithVideoSource
import com.irlstreamer.reconstruction.model.AppRoute
import com.irlstreamer.reconstruction.model.AppUiState
import com.irlstreamer.reconstruction.model.DialogRequest
import com.irlstreamer.reconstruction.model.DialogType
import com.irlstreamer.reconstruction.model.QuickTab
import com.irlstreamer.reconstruction.model.ReplicaSettings
import com.irlstreamer.reconstruction.model.REVEAL_STREAM_KEY
import com.irlstreamer.reconstruction.model.RuntimeUiState
import com.irlstreamer.reconstruction.model.SettingsPage
import com.irlstreamer.reconstruction.model.noConnectionDialog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: ReplicaSettingsRepository,
    private val engine: BroadcastEngine = SimulatedBroadcastEngine(),
) : ViewModel() {
    private val runtime = MutableStateFlow(RuntimeUiState())
    private var lastRearCameraId = 0
    private var lastFrontCameraId = 1

    private val capture = engine as? StreamPackBroadcastEngine

    /** The camera the console previews. Null while the engine owns no camera. */
    val videoSource: StateFlow<IWithVideoSource?> =
        capture?.videoSource ?: MutableStateFlow(null)

    /** True from the moment Start is pressed until the stream is closed. */
    val broadcasting: StateFlow<Boolean> = engine.state
        .map { it == BroadcastState.CONNECTING || it == BroadcastState.LIVE || it == BroadcastState.DEGRADED }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * Which reconnect attempt is in flight, or 0 when nothing is reconnecting.
     *
     * The console renders this only while it is non-zero, so the audited
     * captures - which run the simulation - are unchanged.
     */
    val reconnectAttempt: StateFlow<Int> = engine.statistics
        .map { it.reconnectAttempt }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _cameraFailure = MutableStateFlow<String?>(null)

    /** Why the preview is not showing, when it is not. */
    val cameraFailure: StateFlow<String?> = _cameraFailure

    /** What the last reset cleared, held until the user undoes it or the offer expires. */
    private var resetSnapshot: Preferences? = null
    private var broadcastRequestInFlight = false
    private var undoOfferId = 0L

    val uiState: StateFlow<AppUiState> = combine(runtime, repository.settings) { runtimeState, settings ->
        AppUiState(runtimeState, settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState(settings = ReplicaSettings()))

    init {
        // Keep the engine's view of the durable settings current, so bonding
        // weights and encoder targets cannot drift from the settings tree.
        viewModelScope.launch { repository.settings.collect(engine::configure) }
        // A broadcast that ends on its own has no start request to reject, so
        // the engine reports it here instead.
        viewModelScope.launch {
            engine.failure.filterNotNull().collect { failure ->
                runtime.value = runtime.value.copy(
                    toastMessage = when (failure) {
                        BroadcastFailure.NoActiveConnection -> "The broadcast stopped: no active connection."
                        is BroadcastFailure.TransportUnavailable -> "The broadcast stopped: ${failure.reason}"
                    },
                )
            }
        }
    }

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
            val choice = selected.first()
            runtime.value = runtime.value.copy(
                transientValues = runtime.value.transientValues + (dialog.id to choice),
            )
            viewModelScope.launch { repository.setChoiceValue(dialog.id, choice) }
        }

        // A number dialog that cannot parse its input must say so rather than
        // closing as though it worked.
        if (dialog.type == DialogType.NUMBER && value.isNotBlank() && value.toIntOrNull() == null) {
            runtime.value = runtime.value.copy(
                validationError = "Enter a whole number for ${dialog.title.lowercase()}",
            )
            return
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
            "reset_settings" -> {
                viewModelScope.launch {
                    val cleared = repository.reset()
                    // Resetting twice inside the offer window must not replace the
                    // snapshot with the empty store the first reset just left behind.
                    if (resetSnapshot == null) resetSnapshot = cleared
                    runtime.value = runtime.value.copy(undoResetOffer = ++undoOfferId)
                }
                // Clearing only the DataStore left every in-session value on
                // screen, so a reset appeared not to have happened.
                runtime.value = runtime.value.copy(
                    transientValues = emptyMap(),
                    transientBooleans = emptyMap(),
                    overrides = com.irlstreamer.reconstruction.model.ScreenOverrides(),
                )
            }
            else ->
                // Everything else keeps its value for the session so the row it
                // was opened from updates. Silently discarding the input taught
                // the user that editing settings does nothing.
                if (dialog.type == DialogType.TEXT || dialog.type == DialogType.NUMBER) {
                    runtime.value = runtime.value.copy(
                        transientValues = runtime.value.transientValues + (dialog.id to value),
                    )
                    viewModelScope.launch { repository.setChoiceValue(dialog.id, value) }
                }
        }
        runtime.value = runtime.value.copy(dialog = null)
    }

    fun applyDebugScreen(screenId: String) {
        Harness.overrides.screenState(screenId)?.let { runtime.value = it }
    }

    fun applyNamedState(name: String) {
        Harness.overrides.namedState(name)?.let { runtime.value = it }
    }

    /** Persists the destination entered on the connection form. */
    fun saveConnection(name: String, url: String) {
        viewModelScope.launch { repository.setConnection(name.trim(), url.trim()) }
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

    /**
     * Opens the camera for the console preview. Safe to call repeatedly: the
     * engine keeps one camera open and hands the same source back.
     */
    fun openCamera() {
        val capture = capture ?: return
        viewModelScope.launch {
            capture.openCamera()
                .onSuccess { _cameraFailure.value = null }
                .onFailure { _cameraFailure.value = "Camera failed to start: ${it.message ?: it.javaClass.simpleName}" }
        }
    }

    /** Hands the camera back, so a backgrounded app is not holding the device. */
    fun releaseCamera() {
        val capture = capture ?: return
        viewModelScope.launch { capture.releaseCamera() }
    }

    /**
     * Retry after a failure: the cached streamer may be the thing that is broken,
     * so it is discarded before opening again.
     */
    fun retryCamera() {
        val capture = capture ?: return
        viewModelScope.launch {
            capture.releaseCamera()
            _cameraFailure.value = null
            capture.openCamera()
                .onFailure { _cameraFailure.value = "Camera failed to start: ${it.message ?: it.javaClass.simpleName}" }
        }
    }

    fun setCurrentCamera(cameraId: Int) {
        runtime.value = runtime.value.copy(currentCameraId = cameraId)
        applyFacing(cameraId)
    }

    /** Points the real camera the way the selected lens asks. */
    private fun applyFacing(cameraId: Int) {
        val capture = capture ?: return
        viewModelScope.launch {
            capture.selectFacing(cameraId in FRONT_CAMERA_IDS)
                .onFailure { runtime.value = runtime.value.copy(toastMessage = "That lens is not available on this device") }
        }
    }

    /**
     * Flip between facings, not between two fixed ids.
     *
     * The lens fixture exposes 0 and 2 as rear and 1 and 3 as front (deviation
     * D008). Toggling against id 1 alone meant flipping from camera 3 landed on
     * camera 1 - front to front - and the console's flip control only lit up for
     * one of the two front lenses.
     */
    fun flipCamera() {
        val current = runtime.value.currentCameraId
        val target = if (current in FRONT_CAMERA_IDS) lastRearCameraId else lastFrontCameraId
        if (current in FRONT_CAMERA_IDS) lastFrontCameraId = current else lastRearCameraId = current
        runtime.value = runtime.value.copy(currentCameraId = target)
        applyFacing(target)
    }

    fun toggleMicrophone() {
        runtime.value = runtime.value.copy(microphoneMuted = !runtime.value.microphoneMuted)
    }

    /**
     * Start the broadcast through the engine seam.
     *
     * No connection is configurable yet, so the simulation always refuses with
     * [BroadcastFailure.NoActiveConnection] and the audited guard dialog is
     * raised (screen 142). The refusal is the engine's decision, not the UI's,
     * so a real engine changes this behaviour without touching Compose.
     */
    fun startBroadcast() {
        if (broadcastRequestInFlight) return
        broadcastRequestInFlight = true
        viewModelScope.launch {
            try {
                start()
            } finally {
                broadcastRequestInFlight = false
            }
        }
    }

    fun stopBroadcast() {
        viewModelScope.launch { engine.stop() }
    }

    /** One control, as the audited console has: it starts, then it stops. */
    fun toggleBroadcast() {
        // Without this, two quick taps both read `broadcasting == false` while the
        // first request is still suspended, and both reach startStream.
        if (broadcastRequestInFlight) return
        broadcastRequestInFlight = true
        viewModelScope.launch {
            try {
                if (broadcasting.value) engine.stop() else start()
            } finally {
                broadcastRequestInFlight = false
            }
        }
    }

    private suspend fun start() {
        val settings = repository.settings.first()
        val request = BroadcastRequest(
            connectionName = settings.connectionUrl.takeIf { it.isNotBlank() },
            recordLocally = settings.recordStream,
        )
        when (val result = engine.start(request)) {
            is BroadcastResult.Started -> Unit
            is BroadcastResult.Rejected -> when (val failure = result.failure) {
                BroadcastFailure.NoActiveConnection ->
                    runtime.value = runtime.value.copy(dialog = noConnectionDialog)
                is BroadcastFailure.TransportUnavailable ->
                    runtime.value = runtime.value.copy(toastMessage = failure.reason)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        engine.release()
    }

    fun reload() {
        runtime.value = runtime.value.copy(toastMessage = "Reloading chat and all web overlays, please wait a few seconds…")
    }

    fun showToast(message: String) {
        runtime.value = runtime.value.copy(toastMessage = message)
    }

    /** Puts back everything the last reset cleared. */
    fun undoReset() {
        val snapshot = resetSnapshot ?: return
        resetSnapshot = null
        runtime.value = runtime.value.copy(undoResetOffer = null)
        viewModelScope.launch { repository.restore(snapshot) }
    }

    /** The undo offer expired or was dismissed; the reset stands. */
    fun consumeUndoReset() {
        resetSnapshot = null
        runtime.value = runtime.value.copy(undoResetOffer = null)
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

    /**
     * Toggles persist. Keys with a named field in [ReplicaSettings] use it;
     * everything else is stored generically. Previously only fifteen keys were
     * durable and the rest reset on relaunch, which made roughly twenty audited
     * switches look functional and silently forget.
     */
    fun toggleBoolean(key: String, current: Boolean) {
        if (key in persistedBooleanKeys) {
            setBoolean(key, !current)
        } else {
            viewModelScope.launch { repository.setExtraToggle(key, !current) }
        }
    }

    /**
     * Show or hide the saved destination's stream key.
     *
     * Deliberately transient: `transientBooleans` is cleared on navigation, so
     * the key cannot be left on screen by a setting the user forgot about, and
     * it never reaches storage or a backup.
     */
    fun toggleStreamKeyReveal() {
        val current = runtime.value.transientBooleans[REVEAL_STREAM_KEY] == true
        runtime.value = runtime.value.copy(
            transientBooleans = runtime.value.transientBooleans + (REVEAL_STREAM_KEY to !current),
        )
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

    class Factory(
        private val repository: ReplicaSettingsRepository,
        private val engine: BroadcastEngine,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(repository, engine) as T
    }

    private companion object {
        /** Audit evidence: the console lens pills expose 1 and 3 as front lenses (D008). */
        val FRONT_CAMERA_IDS = setOf(1, 3)

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
