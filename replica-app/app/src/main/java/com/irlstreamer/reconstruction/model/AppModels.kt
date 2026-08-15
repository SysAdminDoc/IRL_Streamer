package com.irlstreamer.reconstruction.model

enum class SettingsPage {
    ROOT,
    STREAMER,
    BONDING,
    CONNECTIONS,
    MANAGE_CONNECTIONS,
    CONNECTION_FORM,
    VIDEO,
    AUDIO,
    RECORDING,
    DISPLAY,
    OVERLAYS_ROOT,
    TEXT_OVERLAYS,
    MANAGE_LAYERS,
    PICTURE_LAYER_FORM,
    TEXT_LAYER_FORM,
    TIMESTAMP_FORM,
    WEB_OVERLAYS,
    MANAGE_WEB_OVERLAYS,
    WEB_OVERLAY_FORM,
    IMPORT_EXPORT,
    ADVANCED,
    HELP,
}

sealed interface AppRoute {
    data object LiveConsole : AppRoute
    data class Settings(val page: SettingsPage) : AppRoute
}

enum class QuickTab { CAMERA, NETWORK, DISPLAY, OVERLAYS, AUDIO, LOG }

enum class ChoiceMode { SINGLE, MULTIPLE }

data class DialogRequest(
    val id: String,
    val title: String,
    val type: DialogType,
    val options: List<String> = emptyList(),
    val selectedOptions: Set<String> = emptySet(),
    val initialValue: String = "",
    val message: String = "",
    val positiveLabel: String = "OK",
    val negativeLabel: String = "CANCEL",
    val dismissOnChoice: Boolean = true,
)

enum class DialogType { CHOICE_SINGLE, CHOICE_MULTIPLE, TEXT, NUMBER, ALERT, ABOUT }

data class ReplicaSettings(
    val showPlatformIcons: Boolean = false,
    val showBots: Boolean = false,
    val showCommands: Boolean = false,
    val showUserCount: Boolean = true,
    val chatFontScale: Int = 220,
    val alertDashboardScale: Int = 280,
    val cellularEnabled: Boolean = true,
    val wifiEnabled: Boolean = true,
    val ethernetEnabled: Boolean = true,
    val cellularWeight: Int = 100,
    val wifiWeight: Int = 100,
    val ethernetWeight: Int = 100,
    val bitrateMatchesResolution: Boolean = true,
    val h264BitrateKbps: Int = 6000,
    val audioInputGainDb: Float = 0f,
    val recordStream: Boolean = false,
    val splitSections: Boolean = true,
    val audioMeterVisible: Boolean = true,
    val gridVisible: Boolean = false,
    val safeMarginsVisible: Boolean = false,
    val safeMarginIndentPercent: Int = 5,
    val safeMarginRatios: Set<String> = setOf("16:9 (1.78)"),
    val timestampActive: Boolean = false,
    val webOverlayMaster: Boolean = true,
)

data class ScreenOverrides(
    val showPlatformIcons: Boolean? = null,
    val wifiWeight: Int? = null,
    val bitrateMatchesResolution: Boolean? = null,
    val audioInputGainDb: Float? = null,
    val audioMeterVisible: Boolean? = null,
    val gridVisible: Boolean? = null,
    val safeMarginsVisible: Boolean? = null,
    val safeMarginIndentPercent: Int? = null,
    val timestampActive: Boolean? = null,
)

data class RuntimeUiState(
    val route: AppRoute = AppRoute.LiveConsole,
    val backStack: List<AppRoute> = emptyList(),
    val dialog: DialogRequest? = null,
    val quickPanelOpen: Boolean = false,
    val quickTab: QuickTab = QuickTab.CAMERA,
    val quickPanelLower: Boolean = false,
    val currentCameraId: Int = 0,
    val microphoneMuted: Boolean = false,
    val settingsScrollIndex: Int = 0,
    val debugScreenId: String? = null,
    val overrides: ScreenOverrides = ScreenOverrides(),
    val toastMessage: String? = null,
    val launchInitializing: Boolean = false,
    val requestFolderPicker: Boolean = false,
    val formVariant: String? = null,
    val validationError: String? = null,
    val transientBooleans: Map<String, Boolean> = emptyMap(),
    val transientValues: Map<String, String> = emptyMap(),
)

data class AppUiState(
    val runtime: RuntimeUiState = RuntimeUiState(),
    val settings: ReplicaSettings = ReplicaSettings(),
) {
    val effectivePlatformIcons: Boolean
        get() = runtime.overrides.showPlatformIcons ?: settings.showPlatformIcons
    val effectiveWifiWeight: Int
        get() = runtime.overrides.wifiWeight ?: settings.wifiWeight
    val effectiveBitrateMatch: Boolean
        get() = runtime.overrides.bitrateMatchesResolution ?: settings.bitrateMatchesResolution
    val effectiveInputGain: Float
        get() = runtime.overrides.audioInputGainDb ?: settings.audioInputGainDb
    val effectiveAudioMeter: Boolean
        get() = runtime.overrides.audioMeterVisible ?: settings.audioMeterVisible
    val effectiveGrid: Boolean
        get() = runtime.overrides.gridVisible ?: settings.gridVisible
    val effectiveSafeMargins: Boolean
        get() = runtime.overrides.safeMarginsVisible ?: settings.safeMarginsVisible
    val effectiveSafeMarginIndent: Int
        get() = runtime.overrides.safeMarginIndentPercent ?: settings.safeMarginIndentPercent
    val effectiveTimestamp: Boolean
        get() = runtime.overrides.timestampActive ?: settings.timestampActive
}
