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
    /**
     * The live console hides the status bar, so a modal raised over it centres on the
     * full 1080 px height (audit screen 142, centre y = 539.5 px) instead of the
     * settings region that begins 83 px down (centre y = 581.5 px).
     */
    val overLiveConsole: Boolean = false,
    /**
     * Audit screens 055, 057 and 077 were captured with the field focused and the
     * soft keyboard raised. On the reference device the Samsung IME takes over the
     * whole landscape window, which is why those screenshots show a keyboard rather
     * than the modal that the matching UI hierarchy proves is still present.
     */
    val focusFieldOnShow: Boolean = false,
    /**
     * First visible option index for a scrolled choice list. Audit states such as
     * 045_video_fps_menu_middle and 046_video_fps_menu_lower captured the same menu
     * at different scroll offsets, so the option list must start where the audit
     * hierarchy shows it starting.
     */
    val listAnchorIndex: Int = 0,
    /**
     * Absolute placement for an anchored spinner popup. Audit screens 027 and 112 are
     * not AlertDialogs: they are `ListView` popups anchored to their spinner row, with
     * no title and no button bar (027 at [98,157]-[657,832]; 112 at [278,270]-[575,1080]).
     */
    val popupBounds: PopupBounds? = null,
)

/** Absolute popup placement in dp, converted from the audited pixel bounds at 450 dpi. */
data class PopupBounds(
    val leftDp: Float,
    val topDp: Float,
    val widthDp: Float,
)

enum class DialogType { CHOICE_SINGLE, CHOICE_MULTIPLE, TEXT, NUMBER, ALERT, ABOUT, POPUP_MENU }

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
    /**
     * Every other audited toggle, persisted generically.
     *
     * The named fields above are the ones the audit pins to a specific default
     * or that other surfaces read. The rest used to live in memory only, so
     * around twenty switches flipped convincingly and reset on relaunch.
     */
    val extraToggles: Map<String, Boolean> = emptyMap(),
    /** Persisted single-choice and text values, keyed by dialog id. */
    val choiceValues: Map<String, String> = emptyMap(),
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
    /**
     * Audited console telemetry for this state (deviation D009 fixtures).
     * Each live capture recorded its own reading - screen 132 shows -512 mA /
     * -2106 mW / 33.3 C where screen 001 shows -283 mA - so pinning one state's
     * values across all of them mismatched both the text and its glyph geometry.
     */
    val telemetry: ConsoleTelemetry? = null,
)

/**
 * The audited guard raised when Start is pressed with nothing to broadcast to
 * (screen 142). Production behaviour, so it lives here rather than in the
 * capture catalog the release build does not carry.
 */
val noConnectionDialog = DialogRequest(
    id = "no_connection",
    title = "Start Broadcast?",
    type = DialogType.ALERT,
    message = "You don't have any active connections, please add one.",
    positiveLabel = "CREATE CONNECTION",
    negativeLabel = "CANCEL",
    overLiveConsole = true,
)

/** One live-console telemetry sample, exactly as the audit recorded it. */
data class ConsoleTelemetry(
    val currentMilliamps: Int,
    val powerMilliwatts: Int,
    val temperatureCelsius: String,
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
    /**
     * Identifies the live "settings were reset" offer. A second reset raises a
     * new id so the offer is shown again with a fresh countdown; a plain flag
     * could not, because true -> true is not a change.
     */
    val undoResetOffer: Long? = null,
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

    /** Audit evidence: screen 001 reads -283 mA / -1144 mW / 31.6 C. */
    val effectiveTelemetry: ConsoleTelemetry
        get() = runtime.overrides.telemetry ?: ConsoleTelemetry(-283, -1144, "31.6")
}
