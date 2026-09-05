package com.irlstreamer.reconstruction.ui.settings

import com.irlstreamer.reconstruction.BuildConfig
import com.irlstreamer.reconstruction.model.DialogRequest
import com.irlstreamer.reconstruction.model.DialogType
import com.irlstreamer.reconstruction.model.ReplicaSettings
import com.irlstreamer.reconstruction.model.SettingsPage
import com.irlstreamer.reconstruction.model.effectiveConnections
import com.irlstreamer.reconstruction.model.hasStreamKey
import com.irlstreamer.reconstruction.model.redactStreamKey
import com.irlstreamer.reconstruction.update.isNewerVersion

sealed interface SettingItem {
    data class Section(val title: String) : SettingItem
    data class Row(
        val id: String,
        val title: String,
        val summary: String = "",
        val enabled: Boolean = true,
        val accentTitle: Boolean = false,
        val valueKey: String? = null,
        val action: SettingAction = SettingAction.None,
    ) : SettingItem
    data class Toggle(
        val key: String,
        val title: String,
        val summary: String = "",
        val enabled: Boolean = true,
        /**
         * Summary shown while the toggle is off, when the audit records a
         * different string for each state. Audit evidence: screen 054 shows
         * "Bitrate is now defined manually." where screens 032-037 show the
         * recommended-bitrate text. Null keeps [summary] for both states.
         */
        val summaryOff: String? = null,
    ) : SettingItem
    data class Slider(
        val key: String,
        val title: String,
        val summary: String = "",
        val min: Float,
        val max: Float,
        val steps: Int,
        val formatter: (Float) -> String,
    ) : SettingItem
    data class Info(val id: String, val text: String) : SettingItem
}

sealed interface SettingAction {
    data object None : SettingAction
    data class Navigate(val page: SettingsPage) : SettingAction
    data class Dialog(val request: DialogRequest) : SettingAction
    data object OpenFolder : SettingAction
    data class Toast(val message: String) : SettingAction

    /** Show or hide the saved destination's stream key, for this visit only. */
    data object ToggleStreamKeyReveal : SettingAction

    /** Make this saved destination the one the console broadcasts to. */
    data class SelectConnection(val name: String) : SettingAction

    /** Share the recent-activity report, with secrets removed. */
    data object ShareDiagnostics : SettingAction

    /** Open the GitHub releases page, where the newer build is. */
    data object OpenReleasesPage : SettingAction

    /** Ask before forgetting a saved destination. */
    data class DeleteConnection(val name: String) : SettingAction
}

data class SettingsPageSpec(
    val title: String,
    val items: List<SettingItem>,
)

object SettingsCatalog {
    fun page(page: SettingsPage): SettingsPageSpec = when (page) {
        SettingsPage.ROOT -> root
        SettingsPage.STREAMER -> streamer
        SettingsPage.BONDING -> bonding
        SettingsPage.CONNECTIONS -> connections
        SettingsPage.MANAGE_CONNECTIONS -> manageConnections
        SettingsPage.VIDEO -> video
        SettingsPage.AUDIO -> audio
        SettingsPage.RECORDING -> recording
        SettingsPage.DISPLAY -> display
        SettingsPage.OVERLAYS_ROOT -> overlaysRoot
        SettingsPage.TEXT_OVERLAYS -> textOverlays
        SettingsPage.MANAGE_LAYERS -> manageLayers
        SettingsPage.WEB_OVERLAYS -> webOverlays
        SettingsPage.MANAGE_WEB_OVERLAYS -> manageWebOverlays
        SettingsPage.IMPORT_EXPORT -> importExport
        SettingsPage.ADVANCED -> advanced
        SettingsPage.HELP -> help
        else -> SettingsPageSpec("Layer", emptyList())
    }

    private val root = SettingsPageSpec(
        title = "Settings",
        items = listOf(
            SettingItem.Row("identity", "IRL Streamer", "Version ${BuildConfig.VERSION_NAME} · clean-room reconstruction", accentTitle = true),
            SettingItem.Row("streamer", "Streamer", "Chatbox and activity feed settings", action = SettingAction.Navigate(SettingsPage.STREAMER)),
            SettingItem.Row("bonding", "Bonding", "Bonding settings", action = SettingAction.Navigate(SettingsPage.BONDING)),
            SettingItem.Row("connections", "Connections", action = SettingAction.Navigate(SettingsPage.CONNECTIONS)),
            SettingItem.Row("video", "Video", action = SettingAction.Navigate(SettingsPage.VIDEO)),
            SettingItem.Row("audio", "Audio", action = SettingAction.Navigate(SettingsPage.AUDIO)),
            SettingItem.Row("recording", "Recording", action = SettingAction.Navigate(SettingsPage.RECORDING)),
            SettingItem.Row("display", "Display", action = SettingAction.Navigate(SettingsPage.DISPLAY)),
            SettingItem.Row("overlays", "Overlays", action = SettingAction.Navigate(SettingsPage.OVERLAYS_ROOT)),
            SettingItem.Row("import_export", "Import/Export Settings", action = SettingAction.Navigate(SettingsPage.IMPORT_EXPORT)),
            SettingItem.Row("advanced", "Advanced options", action = SettingAction.Navigate(SettingsPage.ADVANCED)),
            SettingItem.Row("help", "Help & support", action = SettingAction.Navigate(SettingsPage.HELP)),
        ),
    )

    private val streamer = SettingsPageSpec(
        title = "Streamer options",
        items = listOf(
            SettingItem.Section("Chat"),
            SettingItem.Row("platform_a_username", "Platform A Username", "This is for the chat overlay and view count/title, set to blank to disable", action = SettingAction.Dialog(text("platform_a_username", "Platform A Username"))),
            SettingItem.Row("platform_b_username", "Platform B Username", "Local compatibility fixture; blank disables associated use", action = SettingAction.Dialog(text("platform_b_username", "Platform B Username"))),
            SettingItem.Toggle("show_platform_icons", "Show platform icons", "Show the chat platform icon per chat message (Platform A, Platform B, etc.)"),
            SettingItem.Toggle("show_bots", "Show bots in chat"),
            SettingItem.Toggle("show_commands", "Show commands in chat"),
            SettingItem.Row("chat_font_scale", "Chat font scale", valueKey = "chat_font_scale", action = SettingAction.Dialog(number("chat_font_scale", "Chat font scale", "220"))),
            SettingItem.Toggle("smaller_chat_box", "Smaller chat box", "Half the height of the chat box"),
            SettingItem.Toggle("increase_chat_left_margin", "Increase chat left margin", "Shift over chat from the left, useful for clip on lenses"),
            SettingItem.Toggle("right_chat_box", "Right chat box", "Move chat box to the right"),
            SettingItem.Section("Other"),
            SettingItem.Toggle("show_user_count", "Show user count"),
            SettingItem.Row("custom_chatbox_url", "Custom chatbox URL", "Custom chatbox url, setting this ignores the username settings above", action = SettingAction.Dialog(text("custom_chatbox_url", "Custom chatbox URL"))),
            SettingItem.Section("Alert Dashboard"),
            SettingItem.Info("alert_info", "Activity/replay dashboards are local fixtures. On-stream alerts belong in a web overlay."),
            SettingItem.Row("dashboard_a_key", "Dashboard A API Key", "Blank; locally simulated and never transmitted", action = SettingAction.Dialog(text("dashboard_a_key", "Dashboard A API Key"))),
            SettingItem.Toggle("dashboard_b_enabled", "Enable Dashboard B", "Enables the local activity/replay dashboard fixture"),
            SettingItem.Row("dashboard_c_key", "Dashboard C API Key", "Blank; locally simulated and never transmitted", action = SettingAction.Dialog(text("dashboard_c_key", "Dashboard C API Key"))),
            SettingItem.Section("Custom Page"),
            SettingItem.Row("custom_page", "Custom Page", "Enter a url for a custom page in the popup", action = SettingAction.Dialog(text("custom_page", "Custom Page"))),
            SettingItem.Row("alert_dashboard_scale", "Alert dashboard scale", valueKey = "alert_dashboard_scale", action = SettingAction.Dialog(number("alert_dashboard_scale", "Alert dashboard scale", "280"))),
        ),
    )

    private val bonding = SettingsPageSpec(
        title = "Bonding and NoDrop options",
        items = listOf(
            SettingItem.Section("Bonding"),
            SettingItem.Info("bonding_intro", "Bonding uses multiple internet connections, such as cellular + Wi-Fi, to improve bandwidth and reliability. This reconstruction provides a safe local simulation."),
            SettingItem.Row("own_server", "1. Own Bonding Server (SRTLA)", "Use an independently licensed, owner-controlled server implementation."),
            SettingItem.Row("service", "2. Connection Bonding Service", "Built-in behavior is simulated locally and transmits no media."),
            SettingItem.Toggle("bonding_only", "Bonding Only", "Only bond the connections, video is sent straight to the destination"),
            SettingItem.Section("Connections"),
            SettingItem.Info("connections_info", "Enable Bonding for the following connections:"),
            SettingItem.Section("Advanced Bonding Settings"),
            SettingItem.Info("weights_info", "Tune which links are enabled and their relative weights. The same values appear in Network quick settings."),
            SettingItem.Toggle("cellular_enabled", "Enable Cellular"),
            SettingItem.Slider("cellular_weight", "Cellular Weight", min = 0f, max = 100f, steps = 99, formatter = { it.toInt().toString() }),
            SettingItem.Toggle("wifi_enabled", "Enable WiFi"),
            SettingItem.Slider("wifi_weight", "WiFi Weight", min = 0f, max = 100f, steps = 99, formatter = { it.toInt().toString() }),
            SettingItem.Toggle("ethernet_enabled", "Enable Ethernet/USB Connection"),
            SettingItem.Slider("ethernet_weight", "Ethernet Weight", min = 0f, max = 100f, steps = 99, formatter = { it.toInt().toString() }),
        ),
    )

    /**
     * The audited page, plus the saved destination when there is one.
     *
     * The URL is masked by default: it carries the stream key, and this screen
     * is on the phone a user is streaming with. [revealStreamKey] is a
     * transient, deliberate choice - it is never persisted and is dropped on
     * navigation.
     */
    fun connectionsPage(settings: ReplicaSettings, revealStreamKey: Boolean = false): SettingsPageSpec {
        val saved = settings.effectiveConnections
        if (saved.isEmpty()) return connections
        val items = mutableListOf<SettingItem>()
        items += connections.items
        // Every saved destination, so a second one no longer replaces the first
        // without saying so. Tapping one makes it the destination to broadcast to.
        saved.forEach { connection ->
            val active = connection.name.equals(settings.connectionName, ignoreCase = true)
            items += SettingItem.Row(
                id = if (active) "saved_connection" else "saved_connection_${connection.name}",
                title = connection.name,
                // The summary is the destination and nothing else; the active
                // one is marked by the accent, which keeps the row readable and
                // leaves the URL the only thing in the summary.
                summary = if (revealStreamKey && active) connection.url else redactStreamKey(connection.url),
                accentTitle = active,
                action = SettingAction.SelectConnection(connection.name),
            )
        }
        if (hasStreamKey(settings.connectionUrl)) {
            items += SettingItem.Row(
                id = "reveal_stream_key",
                title = if (revealStreamKey) "Hide stream key" else "Show stream key",
                summary = if (revealStreamKey) "The key is on screen now." else "The key stays hidden until you ask for it.",
                action = SettingAction.ToggleStreamKeyReveal,
            )
        }
        return connections.copy(items = items)
    }

    /**
     * The audited page, plus what the release check found.
     *
     * There is no store to push an update here, so the app has to say when a
     * newer build exists. The rows only appear once a check has run, which the
     * capture harness never does, so the audited page is unchanged.
     */
    fun helpPage(settings: ReplicaSettings): SettingsPageSpec {
        val items = mutableListOf<SettingItem>()
        items += help.items
        items += SettingItem.Toggle(
            key = "update_check_enabled",
            title = "Check for updates",
            summary = "Ask GitHub once a day whether a newer release exists.",
            summaryOff = "The app will not look for updates.",
        )
        val latest = settings.updateLatestTag
        if (latest.isNotBlank() && isNewerVersion(latest, BuildConfig.VERSION_NAME)) {
            items += SettingItem.Row(
                id = "update_available",
                title = "Update available: $latest",
                summary = "You are on ${BuildConfig.VERSION_NAME}. Open the releases page to download it.",
                accentTitle = true,
                action = SettingAction.OpenReleasesPage,
            )
        }
        return help.copy(items = items)
    }

    /** The audited page, plus a delete row per saved destination. */
    fun manageConnectionsPage(settings: ReplicaSettings): SettingsPageSpec {
        val saved = settings.effectiveConnections
        if (saved.isEmpty()) return manageConnections
        val items = mutableListOf<SettingItem>()
        items += manageConnections.items
        saved.forEach { connection ->
            items += SettingItem.Row(
                id = "delete_connection_${connection.name}",
                title = "Delete ${connection.name}",
                summary = redactStreamKey(connection.url),
                action = SettingAction.DeleteConnection(connection.name),
            )
        }
        return manageConnections.copy(items = items)
    }

    private val connections = SettingsPageSpec(
        title = "Outgoing connections",
        items = listOf(
            SettingItem.Info("connection_help", "Long hold to edit a connection, only one active at a time."),
            SettingItem.Row("new_connection", "New connection", action = SettingAction.Navigate(SettingsPage.CONNECTION_FORM)),
            SettingItem.Row("manage_connections", "Manage connections", action = SettingAction.Navigate(SettingsPage.MANAGE_CONNECTIONS)),
        ),
    )

    private val manageConnections = SettingsPageSpec(
        title = "Manage outgoing connections",
        items = listOf(
            SettingItem.Row("new_connection", "New connection", action = SettingAction.Navigate(SettingsPage.CONNECTION_FORM)),
            SettingItem.Row("delete_multiple", "Delete multiple", enabled = false),
        ),
    )

    private val video = SettingsPageSpec(
        title = "Video parameters",
        items = listOf(
            SettingItem.Row("multi_camera", "Multi-camera capture", "Off", enabled = false),
            SettingItem.Section("Camera"),
            SettingItem.Row("start_camera", "Start app with", "Rear camera (0)", action = SettingAction.Dialog(choice("start_camera", "Start app with", "Rear camera (0)", listOf("Rear camera (0)", "Front camera (1)", "Rear camera (2)", "Front camera (3)")))),
            SettingItem.Toggle("flip_sub_cameras", "Flip between physical sub-cameras"),
            SettingItem.Row("resolution", "Resolution", "1920x1080 (16:9)", action = SettingAction.Dialog(choice("resolution", "Resolution", "1920x1080 (16:9)", listOf("1920x1080 (16:9)", "1920x824", "1440x1080", "1280x720 (16:9)", "1088x1088", "960x720", "720x480", "640x480", "640x360 (16:9)")))),
            SettingItem.Row("fps", "FPS", "System default", action = SettingAction.Dialog(choice("fps", "FPS", "System default", listOf("System default", "7-10 variable rate", "10 fixed rate", "7-15 variable rate", "15 fixed rate", "7-24 variable rate", "24 fixed rate", "7-30 variable rate", "30 fixed rate", "60 fixed rate", "120 fixed rate")))),
            SettingItem.Row("orientation", "Orientation", "Landscape", enabled = false),
            SettingItem.Toggle("live_rotation", "Live rotation", enabled = false),
            SettingItem.Toggle("reverse_orientation", "Reverse Orientation"),
            SettingItem.Toggle("double_tap_flip", "Double Tap to Flip Camera"),
            SettingItem.Section("Background streaming"),
            SettingItem.Toggle("keep_streaming", "Keep streaming when not in focus.", "This is always enabled now.", enabled = false),
            SettingItem.Section("Start new lens with"),
            SettingItem.Row("focus_mode", "Focus mode", "Continuous auto focus", action = SettingAction.Dialog(choice("focus_mode", "Focus mode", "Continuous auto focus", listOf("Continuous auto focus", "Infinity")))),
            SettingItem.Row("white_balance", "White balance", "Auto", action = SettingAction.Dialog(choice("white_balance", "White balance", "Auto", listOf("Auto", "Cloudy daylight", "Daylight", "Fluorescent", "Incandescent")))),
            SettingItem.Row("anti_flicker", "Anti-flicker", "Auto", action = SettingAction.Dialog(choice("anti_flicker", "Anti-flicker", "Auto", listOf("Off", "50Hz", "60Hz", "Auto")))),
            SettingItem.Row("exposure", "Exposure compensation", "0", action = SettingAction.Dialog(choice("exposure", "Exposure compensation", "0", (-20..20).map { value -> if (value == 0) "0" else if (value > 0) "+${value / 10.0}".removeSuffix(".0") else "${value / 10.0}".removeSuffix(".0") }))),
            SettingItem.Section("Encoder parameters"),
            SettingItem.Toggle(
                "bitrate_matches_resolution",
                "Bitrate matches resolution",
                "Bitrate matches the value recommended based on resolution and frame rate.",
                summaryOff = "Bitrate is now defined manually.",
            ),
            SettingItem.Row("h264_bitrate", "Bitrate H264 - HEVC is auto 66%", valueKey = "h264_bitrate", action = SettingAction.Dialog(number("h264_bitrate_kbps", "Bitrate", "6000"))),
            SettingItem.Row("bitrate_mode", "Bitrate mode", "System default", action = SettingAction.Dialog(choice("bitrate_mode", "Bitrate mode", "System default", listOf("System default", "Constant quality", "Variable bitrate", "Constant bitrate", "Constant bitrate w/frame drops (OFTEN UNSUPPORTED)")))),
            SettingItem.Row("keyframe", "Keyframe frequency", "2 sec", action = SettingAction.Dialog(number("keyframe", "Keyframe frequency", "2"))),
            SettingItem.Row("format", "Format", "Auto", action = SettingAction.Dialog(choice("format", "Format", "Auto", listOf("Auto", "H.264", "HEVC")))),
            SettingItem.Toggle("auto_adjust_format", "Auto adjust bitrate based on format", "Bitrate adjusts based on the format being used."),
            SettingItem.Row("h264_profile", "H.264 profile", "System default", action = SettingAction.Dialog(choice("h264_profile", "H.264 profile", "System default", listOf("System default", "Baseline", "Constrained Baseline", "Main", "High")))),
            SettingItem.Row("hevc_profile", "HEVC profile", "System default", action = SettingAction.Dialog(choice("hevc_profile", "HEVC profile", "System default", listOf("System default", "Main", "Main 10", "Main 10 HDR 10", "Main 10 HDR 10 Plus")))),
            SettingItem.Section("Vendor-specific video enhancements"),
            SettingItem.Row("eis", "Electronic image stabilization", "On", action = SettingAction.Dialog(choice("eis", "Electronic image stabilization", "On", listOf("System default", "Off", "On")))),
            SettingItem.Row("ois", "Optical image stabilization", "On", action = SettingAction.Dialog(choice("ois", "Optical image stabilization", "On", listOf("System default", "Off", "On")))),
            SettingItem.Row("noise_reduction", "Noise reduction", "Fast", action = SettingAction.Dialog(choice("noise_reduction", "Noise reduction", "Fast", listOf("System default", "Off", "Fast", "High quality", "Minimal")))),
            SettingItem.Section("Adaptive bitrate streaming"),
            SettingItem.Row("adaptive_mode", "Mode", "Recommended - Automatically choose the best option for the current connection type.", action = SettingAction.Dialog(choice("adaptive_mode", "Mode", "Automatically Select", listOf("Automatically Select", "Local bonding simulation", "Legacy - Logarithmic descend", "Legacy - Ladder ascend", "Legacy - Hybrid")))),
            SettingItem.Toggle("adaptive_frame_rate", "Adaptive frame rate", "Reduce frame rate related to bitrate drop in adaptive bitrate mode. However this can cause issues when it changes fps. YMMV"),
        ),
    )

    private val audio = SettingsPageSpec(
        title = "Audio parameters",
        items = listOf(
            SettingItem.Toggle("prefer_bluetooth", "Prefer Bluetooth Mic/Headset"),
            SettingItem.Row("audio_source", "Audio source", "Camcorder · behavior depends on device implementation", action = SettingAction.Dialog(choice("audio_source", "Audio source", "Camcorder", listOf("Camcorder", "External mic (if present)", "Default audio source", "Optimised for voice calls", "Optimised for voice recognition")))),
            SettingItem.Row("audio_channels", "Audio channels", "Stereo", action = SettingAction.Dialog(choice("audio_channels", "Audio channels", "Stereo", listOf("Mono", "Stereo")))),
            SettingItem.Row("audio_bitrate", "Bitrate", "Auto", action = SettingAction.Dialog(choice("audio_bitrate", "Bitrate", "Auto", listOf("Auto", "16 Kbps", "24 Kbps", "32 Kbps", "64 Kbps", "96 Kbps", "128 Kbps", "160 Kbps", "256 Kbps", "320 Kbps")))),
            SettingItem.Row("sample_rate", "Sample rate", "44100", action = SettingAction.Dialog(choice("sample_rate", "Sample rate", "44100", listOf("8000", "11025", "12000", "16000", "22050", "24000", "32000", "44100", "48000")))),
            SettingItem.Toggle("keep_speakers_awake", "Keep Speakers Awake", "Periodically play a silent sound to keep bluetooth speaker awake"),
            SettingItem.Section("Processing"),
            SettingItem.Slider("audio_input_gain_db", "Input gain", "Tap to reset to 0dB", -40f, 10f, 49, formatter = { value -> if (value > 0) "+%.1f".format(value) else "%.1fdB".format(value) }),
            SettingItem.Toggle("aec", "Acoustic Echo Canceler (AEC)", "Acoustic Echo Canceler [local device capability]"),
            SettingItem.Toggle("noise_suppressor", "Noise Suppressor (NS)", "Noise Suppression [local device capability]"),
        ),
    )

    private val recording = SettingsPageSpec(
        title = "Recording options",
        items = listOf(
            SettingItem.Toggle("record_stream", "Record stream"),
            SettingItem.Info("battery_warning", "WARNING: Recording will stop when the battery level is critically low, usually ~7%."),
            SettingItem.Toggle("split_sections", "Split video into sections", "Video recording is split into sections by default for reliability purposes. You may disable this if you need."),
            SettingItem.Row("section_minutes", "Sections duration (minutes)", "30", action = SettingAction.Dialog(number("section_minutes", "Sections duration (minutes)", "30"))),
            SettingItem.Row("snapshot_format", "Snapshot format", "JPEG", action = SettingAction.Dialog(choice("snapshot_format", "Snapshot format", "JPEG", listOf("JPEG", "PNG", "WebP")))),
            SettingItem.Row("snapshot_quality", "Snapshot quality", "90", action = SettingAction.Dialog(choice("snapshot_quality", "Snapshot quality", "90", listOf("100", "95", "90", "85")))),
            SettingItem.Toggle("use_saf", "Use Storage Access Framework", "Enable to select a user-owned destination through Android's folder picker."),
            SettingItem.Row("save_to", "Save to", "DCIM/IRLStreamer, Podcasts/IRLStreamer", action = SettingAction.OpenFolder),
        ),
    )

    private val display = SettingsPageSpec(
        title = "Display",
        items = listOf(
            SettingItem.Toggle("audio_meter_visible", "Show audio level meter"),
            SettingItem.Toggle("grid_visible", "Grid 3x3"),
            SettingItem.Toggle("safe_margins_visible", "Show safe margins"),
            SettingItem.Row("safe_margin_ratios", "Safe margins ratios", valueKey = "safe_margin_ratios", action = SettingAction.Dialog(multi("safe_margin_ratios", "Safe margins ratios", setOf("16:9 (1.78)"), listOf("1:1 (1.0)", "5:4 (1.25)", "4:3 (1.33)", "3:2 (1.5)", "14:9 (1.56)", "16:9 (1.78)", "2:1 (2.0)", "19.5:9 (2.17)", "21:9 (2.33)")))),
            SettingItem.Slider("safe_margin_indent_percent", "Safe margin indent %", min = 0f, max = 20f, steps = 19, formatter = { it.toInt().toString() }),
        ),
    )

    private val overlaysRoot = SettingsPageSpec(
        title = "Settings",
        items = listOf(
            SettingItem.Row("text_picture", "Text/Picture Overlays", action = SettingAction.Navigate(SettingsPage.TEXT_OVERLAYS)),
            SettingItem.Row("web", "Web Overlays", action = SettingAction.Navigate(SettingsPage.WEB_OVERLAYS)),
        ),
    )

    private val textOverlays = SettingsPageSpec(
        title = "Overlays",
        items = listOf(
            SettingItem.Toggle("show_layers_preview", "Show layers on preview"),
            SettingItem.Toggle("standby_mode", "Enable standby mode"),
            SettingItem.Row("standby_overlays", "Standby overlays", action = SettingAction.Dialog(multi("standby_overlays", "Standby overlays", emptySet(), listOf("Timestamp")))),
            SettingItem.Row("pause_overlays", "Pause overlays", action = SettingAction.Dialog(multi("pause_overlays", "Pause overlays", emptySet(), listOf("Timestamp")))),
            SettingItem.Section("Layers"),
            SettingItem.Toggle("timestamp_active", "Timestamp", "Local green date/time layer"),
            SettingItem.Row("new_layer", "New layer", action = SettingAction.Navigate(SettingsPage.PICTURE_LAYER_FORM)),
            SettingItem.Row("manage_layers", "Manage layers", action = SettingAction.Navigate(SettingsPage.MANAGE_LAYERS)),
            SettingItem.Row("check_images", "Check layers images availability", action = SettingAction.Toast("Local layer fixtures are available")),
        ),
    )

    private val manageLayers = SettingsPageSpec(
        title = "Manage layers",
        items = listOf(
            SettingItem.Row("timestamp", "Timestamp", "Local date/time layer", action = SettingAction.Navigate(SettingsPage.TIMESTAMP_FORM)),
            SettingItem.Row("new_layer", "New layer", action = SettingAction.Navigate(SettingsPage.PICTURE_LAYER_FORM)),
            SettingItem.Row("delete_multiple", "Delete multiple", enabled = false),
        ),
    )

    private val webOverlays = SettingsPageSpec(
        title = "Overlays",
        items = listOf(
            SettingItem.Section("Web overlays"),
            SettingItem.Info("web_perf", "For perf, no more than 3–4 web overlays are recommended"),
            SettingItem.Row("new_web", "New web overlay", action = SettingAction.Navigate(SettingsPage.WEB_OVERLAY_FORM)),
            SettingItem.Row("manage_web", "Manage web overlays", action = SettingAction.Navigate(SettingsPage.MANAGE_WEB_OVERLAYS)),
        ),
    )

    private val manageWebOverlays = SettingsPageSpec(
        title = "Manage web overlays",
        items = listOf(
            SettingItem.Row("new_web", "New web overlay", action = SettingAction.Navigate(SettingsPage.WEB_OVERLAY_FORM)),
            SettingItem.Row("delete_multiple", "Delete multiple", enabled = false),
        ),
    )

    private val importExport = SettingsPageSpec(
        title = "Import/Export Settings",
        items = listOf(
            SettingItem.Row("import", "Import settings", action = SettingAction.Dialog(textWithInitial("import_settings", "Import settings", "irlstreamer://"))),
            SettingItem.Row("scan", "Scan QR code", "You can take a picture of QR code with your camera app which recognizes QR.", action = SettingAction.Dialog(alert("scanner_prompt", "Install QR Scanner?", "This reconstruction can use an installed QR scanner. Would you like to open the app store?", "YES", "NO"))),
            SettingItem.Row("export", "Export settings", action = SettingAction.Toast("Export is a local sanitized fixture; no secrets are included")),
        ),
    )

    private val advanced = SettingsPageSpec(
        title = "Advanced options",
        items = listOf(
            SettingItem.Section("Misc Settings"),
            SettingItem.Toggle("show_status_broadcast", "Show status bar on broadcast page"),
            SettingItem.Toggle("show_bonding_debug", "Show SRTLA Debug"),
            SettingItem.Toggle("allow_rtmp_hevc", "Allow RTMP HEVC"),
            SettingItem.Toggle("experimental_bonding", "Experimental SRTLA Tweaks", "Enables local experimental fixtures that aren't ready yet"),
            SettingItem.Toggle("keep_streaming", "Keep streaming when not in focus.", "This is always enabled now.", enabled = false),
            SettingItem.Toggle("quit_inactive", "Quit if inactive in background", "If IRL Streamer sits in background and has no active connections, it will quit after a timeout."),
            // Audit evidence: screen 126 shows a preference category titled
            // "Advanced options" at [278,276]-[2136,329], the same shape as the
            // "Misc Settings" header in screen 120, immediately above the
            // preferred-camera row.
            SettingItem.Section("Advanced options"),
            SettingItem.Row("preferred_camera", "Preferred camera API", "Disable Camera2 API only if you have some issues with camera like some resolutions or framerates are not available.", enabled = false),
            SettingItem.Toggle("mirror_front", "Mirror front camera", "Stream and record front camera as appears in preview (mirrored)."),
            SettingItem.Toggle("horizon", "Horizon stream demo", "Say no to vertical videos.", enabled = false),
            SettingItem.Section("Camera controls"),
            SettingItem.Row("volume_keys", "Volume keys", "Do nothing", action = SettingAction.Dialog(choice("volume_keys", "Volume keys", "Do nothing", listOf("Start/stop broadcast", "Zoom", "Flip camera", "Do nothing")))),
            SettingItem.Section("Platform-specific tweaks"),
            SettingItem.Toggle("skip_network_check", "Don't check network connection", "Enable only for unusual local connectivity such as USB tethering."),
            SettingItem.Section("Encoder buffer size"),
            SettingItem.Toggle("custom_buffer", "Enable custom encoder buffer size", "Use recommended encoder buffer size."),
            SettingItem.Toggle("usb_camera", "USB Camera", "Take picture from USB UVC camera.", enabled = false),
            SettingItem.Info("usb_help", "USB OTG depends on hardware capabilities. This local reconstruction does not open an external support site."),
            SettingItem.Section("WebViews"),
            SettingItem.Toggle("web_debug", "Web contents debugging", "Debug WebViews using Chrome Developer Tools"),
            SettingItem.Toggle("web_geo", "Web overlay geo enabled", "Allow web overlays to access location"),
            SettingItem.Row("clear_cookies", "Clear all cookies from web overlays", action = SettingAction.Dialog(alert("clear_cookies", "Clear cookies?", "No remote WebView data exists in the local fixture.", "OK", "CANCEL"))),
        ),
    )

    private val help = SettingsPageSpec(
        title = "Help & support",
        items = listOf(
            SettingItem.Row("community", "Community", "External support links are intentionally omitted", action = SettingAction.Toast("No authorized external support destination was supplied")),
            SettingItem.Row("debug_details", "Send debug details", "Shares recent activity with the stream key removed. Nothing is sent automatically.", action = SettingAction.ShareDiagnostics),
            SettingItem.Row("reset", "Reset app settings", action = SettingAction.Dialog(alert("reset_settings", "Reset app settings", "This resets only the reconstruction's local preferences.", "RESET", "CANCEL"))),
            SettingItem.Row("about", "About IRL Streamer", action = SettingAction.Dialog(DialogRequest(
                id = "about",
                title = "IRL Streamer",
                type = DialogType.ABOUT,
                message = "Independent build - Version ${BuildConfig.VERSION_NAME}\nLocal simulation; no licensed transport code",
                positiveLabel = "OK",
            ))),
        ),
    )

    private fun text(id: String, title: String) = DialogRequest(id, title, DialogType.TEXT)
    private fun textWithInitial(id: String, title: String, value: String) = DialogRequest(id, title, DialogType.TEXT, initialValue = value)
    private fun number(id: String, title: String, value: String) = DialogRequest(id, title, DialogType.NUMBER, initialValue = value)
    private fun choice(id: String, title: String, selected: String, options: List<String>) = DialogRequest(id, title, DialogType.CHOICE_SINGLE, options = options, selectedOptions = setOf(selected))
    private fun multi(id: String, title: String, selected: Set<String>, options: List<String>) = DialogRequest(id, title, DialogType.CHOICE_MULTIPLE, options = options, selectedOptions = selected, dismissOnChoice = false)
    private fun alert(id: String, title: String, message: String, positive: String, negative: String) = DialogRequest(id, title, DialogType.ALERT, message = message, positiveLabel = positive, negativeLabel = negative)
}
