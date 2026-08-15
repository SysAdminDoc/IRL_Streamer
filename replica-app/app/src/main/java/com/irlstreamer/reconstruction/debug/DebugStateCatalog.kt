package com.irlstreamer.reconstruction.debug

import com.irlstreamer.reconstruction.model.AppRoute
import com.irlstreamer.reconstruction.model.DialogRequest
import com.irlstreamer.reconstruction.model.DialogType
import com.irlstreamer.reconstruction.model.QuickTab
import com.irlstreamer.reconstruction.model.RuntimeUiState
import com.irlstreamer.reconstruction.model.ScreenOverrides
import com.irlstreamer.reconstruction.model.SettingsPage

/**
 * Deterministic ADB state mapping for every captured audit ID.
 * Audit evidence: app-audit/screens/screen-catalog.csv and all 145 screen specs.
 */
object DebugStateCatalog {
    fun resolve(rawScreenId: String): RuntimeUiState {
        val number = rawScreenId.take(3).toIntOrNull() ?: 1
        val canonicalId = canonicalId(number, rawScreenId)
        val base = RuntimeUiState(debugScreenId = canonicalId)
        return when (number) {
            1 -> base
            in 2..5 -> base.settings(SettingsPage.ROOT, intArrayOf(0, 0, 3, 6, 6)[number - 1])
            in 6..11 -> base.settings(SettingsPage.STREAMER, intArrayOf(0, 3, 7, 11, 13, 13)[number - 6])
            12 -> base.settings(SettingsPage.STREAMER, 13).withDialog(textDialog("custom_page", "Custom Page", ""))
            13 -> base.settings(SettingsPage.STREAMER, 13).withDialog(numberDialog("alert_dashboard_scale", "Alert dashboard scale", "280"))
            14 -> base.settings(SettingsPage.STREAMER, 3).withDialog(numberDialog("chat_font_scale", "Chat font scale", "220"))
            15 -> base.settings(SettingsPage.STREAMER, 1).copy(overrides = ScreenOverrides(showPlatformIcons = true))
            in 16..19 -> base.settings(SettingsPage.BONDING, intArrayOf(0, 3, 7, 7)[number - 16])
            20 -> base.settings(SettingsPage.BONDING, 7).copy(overrides = ScreenOverrides(wifiWeight = 50))
            21 -> base.settings(SettingsPage.CONNECTIONS)
            22 -> base.settings(SettingsPage.CONNECTION_FORM).copy(formVariant = "empty")
            23 -> base.settings(SettingsPage.CONNECTION_FORM, 3).copy(formVariant = "empty")
            24 -> base.settings(SettingsPage.CONNECTION_FORM, 3).copy(formVariant = "invalid")
            25 -> base.settings(SettingsPage.CONNECTION_FORM).copy(formVariant = "rtmp")
            26 -> base.settings(SettingsPage.CONNECTION_FORM, 3).copy(formVariant = "rtmp")
            27 -> base.settings(SettingsPage.CONNECTION_FORM, 3).copy(formVariant = "rtmp", dialog = choiceDialog(
                id = "rtmp_target",
                title = "Target type",
                selected = "Default (no authorization)",
                options = listOf("Default (no authorization)", "RTMP authorization", "Akamai/Dacast", "Limelight Networks", "Periscope Producer"),
            ))
            28 -> base.settings(SettingsPage.CONNECTION_FORM, 3).copy(formVariant = "rtmp_auth")
            29 -> base.settings(SettingsPage.MANAGE_CONNECTIONS)
            30 -> base.settings(SettingsPage.CONNECTION_FORM).withDialog(textDialog("platform_a", "Platform A", "", listOf("Username", "Stream key")))
            31 -> base.settings(SettingsPage.CONNECTION_FORM).withDialog(textDialog("platform_b", "Platform B", "", listOf("Username", "Stream URL", "Stream key")))
            in 32..40 -> base.settings(SettingsPage.VIDEO, intArrayOf(0, 3, 6, 9, 12, 15, 18, 20, 20)[number - 32])
            in 41..64 -> base.settings(SettingsPage.VIDEO, videoAnchor(number)).withDialog(videoDialog(number))
            65 -> base.settings(SettingsPage.AUDIO)
            in 66..70 -> base.settings(SettingsPage.AUDIO).withDialog(audioDialog(number))
            71 -> base.settings(SettingsPage.AUDIO, 5)
            in 72..73 -> base.settings(SettingsPage.AUDIO, 5).withDialog(audioDialog(number))
            74 -> base.settings(SettingsPage.AUDIO, 6).copy(overrides = ScreenOverrides(audioInputGainDb = 10f))
            75 -> base.settings(SettingsPage.AUDIO, 8)
            76 -> base.settings(SettingsPage.RECORDING)
            77 -> base.settings(SettingsPage.RECORDING).withDialog(numberDialog("section_minutes", "Sections duration (minutes)", "30"))
            78 -> base.settings(SettingsPage.RECORDING, 4)
            79 -> base.settings(SettingsPage.RECORDING, 4).withDialog(choiceDialog("snapshot_format", "Snapshot format", "JPEG", listOf("JPEG", "PNG", "WebP")))
            in 80..81 -> base.settings(SettingsPage.RECORDING, 4).withDialog(choiceDialog("snapshot_quality", "Snapshot quality", "90", listOf("100", "95", "90", "85")))
            82 -> base.settings(SettingsPage.RECORDING, 4).copy(requestFolderPicker = true)
            83 -> base.settings(SettingsPage.DISPLAY)
            84 -> base.copy(route = AppRoute.LiveConsole, overrides = ScreenOverrides(gridVisible = true))
            85 -> base.copy(route = AppRoute.LiveConsole, overrides = ScreenOverrides(safeMarginsVisible = true))
            86 -> base.copy(route = AppRoute.LiveConsole, overrides = ScreenOverrides(audioMeterVisible = false))
            87 -> base.settings(SettingsPage.DISPLAY).copy(overrides = ScreenOverrides(safeMarginsVisible = true))
            in 88..89 -> base.settings(SettingsPage.DISPLAY).copy(
                overrides = ScreenOverrides(safeMarginsVisible = true),
                dialog = marginDialog(),
            )
            90 -> base.settings(SettingsPage.DISPLAY).copy(overrides = ScreenOverrides(safeMarginsVisible = true, safeMarginIndentPercent = 20))
            91 -> base.settings(SettingsPage.OVERLAYS_ROOT)
            92 -> base.settings(SettingsPage.TEXT_OVERLAYS)
            93 -> base.settings(SettingsPage.TEXT_OVERLAYS, 4)
            94 -> base.settings(SettingsPage.TEXT_OVERLAYS, 4).copy(overrides = ScreenOverrides(timestampActive = true))
            95 -> base.copy(route = AppRoute.LiveConsole, overrides = ScreenOverrides(timestampActive = true))
            96 -> base.settings(SettingsPage.PICTURE_LAYER_FORM).copy(formVariant = "picture")
            97 -> base.settings(SettingsPage.PICTURE_LAYER_FORM).withDialog(choiceDialog("layer_type", "Type", "Picture", listOf("Picture", "Text")))
            98 -> base.settings(SettingsPage.TEXT_LAYER_FORM).copy(formVariant = "text")
            99 -> base.settings(SettingsPage.TEXT_LAYER_FORM, 5).copy(formVariant = "text")
            100 -> base.settings(SettingsPage.TEXT_LAYER_FORM, 8).copy(formVariant = "text")
            101 -> base.settings(SettingsPage.TEXT_LAYER_FORM, 8).copy(formVariant = "text", validationError = "Please input non-empty text/html code")
            102 -> base.settings(SettingsPage.MANAGE_LAYERS)
            103 -> base.settings(SettingsPage.TIMESTAMP_FORM)
            104 -> base.settings(SettingsPage.TIMESTAMP_FORM, 4)
            105 -> base.settings(SettingsPage.TEXT_OVERLAYS).withDialog(multiDialog("standby_overlays", "Standby overlays", emptySet(), listOf("Timestamp")))
            106 -> base.settings(SettingsPage.TEXT_OVERLAYS).withDialog(multiDialog("pause_overlays", "Pause overlays", emptySet(), listOf("Timestamp")))
            107 -> base.settings(SettingsPage.WEB_OVERLAYS)
            108 -> base.settings(SettingsPage.WEB_OVERLAY_FORM).copy(formVariant = "default")
            109 -> base.settings(SettingsPage.WEB_OVERLAY_FORM, 5).copy(formVariant = "default")
            110 -> base.settings(SettingsPage.WEB_OVERLAY_FORM, 9).copy(formVariant = "default")
            111 -> base.settings(SettingsPage.WEB_OVERLAY_FORM).withDialog(choiceDialog("view_mode", "View mode", "Preview + stream", listOf("Preview + stream", "Preview", "Stream")))
            112 -> base.settings(SettingsPage.WEB_OVERLAY_FORM).withDialog(choiceDialog("position", "Position", "Center", listOf("Top left", "Top right", "Center", "Bottom left", "Bottom right", "Custom")))
            113 -> base.settings(SettingsPage.WEB_OVERLAY_FORM).copy(formVariant = "custom")
            114 -> base.settings(SettingsPage.WEB_OVERLAY_FORM, 4).copy(formVariant = "custom")
            115 -> base.settings(SettingsPage.MANAGE_WEB_OVERLAYS)
            116 -> base.settings(SettingsPage.IMPORT_EXPORT)
            117 -> base.settings(SettingsPage.IMPORT_EXPORT).withDialog(textDialog("import_settings", "Import settings", "irlstreamer://"))
            118 -> base.settings(SettingsPage.IMPORT_EXPORT).withDialog(alertDialog(
                id = "scanner_prompt",
                title = "Install QR Scanner?",
                message = "This reconstruction can use an installed QR scanner. Would you like to open the app store?",
                positive = "YES",
                negative = "NO",
            ))
            119 -> base.settings(SettingsPage.ROOT, 6)
            in 120..126 -> base.settings(SettingsPage.ADVANCED, intArrayOf(0, 5, 9, 13, 16, 16, 5)[number - 120])
            127 -> base.settings(SettingsPage.ADVANCED, 9).withDialog(choiceDialog("volume_keys", "Volume keys", "Do nothing", listOf("Start/stop broadcast", "Zoom", "Flip camera", "Do nothing")))
            128 -> base.settings(SettingsPage.HELP)
            129 -> base.settings(SettingsPage.HELP).withDialog(DialogRequest(
                id = "about",
                title = "IRL Streamer",
                type = DialogType.ABOUT,
                message = "Independent build - Version 0.1.0\nLocal simulation; no licensed transport code",
                positiveLabel = "OK",
            ))
            in 130..138 -> liveQuickPreset(base, number)
            139 -> base.copy(route = AppRoute.LiveConsole, microphoneMuted = true)
            140 -> base.copy(route = AppRoute.LiveConsole, currentCameraId = 1)
            141 -> base.copy(route = AppRoute.LiveConsole, currentCameraId = 2)
            142 -> base.copy(route = AppRoute.LiveConsole, dialog = noConnectionDialog())
            143 -> base.copy(route = AppRoute.LiveConsole, toastMessage = "Reloading chat and all web overlays, please wait a few seconds…")
            in 144..145 -> base.copy(route = AppRoute.LiveConsole)
            else -> base
        }
    }

    fun resolveNamedState(name: String): RuntimeUiState = when (name.lowercase()) {
        "loading" -> RuntimeUiState(route = AppRoute.LiveConsole, launchInitializing = true, toastMessage = "H.264, 1920x1080")
        "empty" -> RuntimeUiState(route = AppRoute.Settings(SettingsPage.CONNECTIONS))
        "network_error" -> RuntimeUiState(route = AppRoute.LiveConsole, quickPanelOpen = true, quickTab = QuickTab.NETWORK, toastMessage = "Local network fixture unavailable")
        "validation_error" -> resolve("101_overlay_blank_save_validation")
        else -> RuntimeUiState(route = AppRoute.LiveConsole)
    }

    fun noConnectionDialog() = alertDialog(
        id = "no_connection",
        title = "Start Broadcast?",
        message = "You don't have any active connections, please add one.",
        positive = "CREATE CONNECTION",
        negative = "CANCEL",
    )

    private fun RuntimeUiState.settings(page: SettingsPage, index: Int = 0) = copy(
        route = AppRoute.Settings(page),
        backStack = parentStack(page),
        settingsScrollIndex = index,
    )

    private fun RuntimeUiState.withDialog(request: DialogRequest) = copy(dialog = request)

    private fun parentStack(page: SettingsPage): List<AppRoute> = when (page) {
        SettingsPage.ROOT -> listOf(AppRoute.LiveConsole)
        SettingsPage.CONNECTION_FORM, SettingsPage.MANAGE_CONNECTIONS -> listOf(
            AppRoute.LiveConsole,
            AppRoute.Settings(SettingsPage.ROOT),
            AppRoute.Settings(SettingsPage.CONNECTIONS),
        )
        SettingsPage.TEXT_OVERLAYS, SettingsPage.WEB_OVERLAYS -> listOf(
            AppRoute.LiveConsole,
            AppRoute.Settings(SettingsPage.ROOT),
            AppRoute.Settings(SettingsPage.OVERLAYS_ROOT),
        )
        SettingsPage.MANAGE_LAYERS,
        SettingsPage.PICTURE_LAYER_FORM,
        SettingsPage.TEXT_LAYER_FORM,
        SettingsPage.TIMESTAMP_FORM,
        -> listOf(
            AppRoute.LiveConsole,
            AppRoute.Settings(SettingsPage.ROOT),
            AppRoute.Settings(SettingsPage.OVERLAYS_ROOT),
            AppRoute.Settings(SettingsPage.TEXT_OVERLAYS),
        )
        SettingsPage.MANAGE_WEB_OVERLAYS, SettingsPage.WEB_OVERLAY_FORM -> listOf(
            AppRoute.LiveConsole,
            AppRoute.Settings(SettingsPage.ROOT),
            AppRoute.Settings(SettingsPage.OVERLAYS_ROOT),
            AppRoute.Settings(SettingsPage.WEB_OVERLAYS),
        )
        else -> listOf(AppRoute.LiveConsole, AppRoute.Settings(SettingsPage.ROOT))
    }

    private fun liveQuickPreset(base: RuntimeUiState, number: Int): RuntimeUiState {
        val tab = when (number) {
            130, 131 -> QuickTab.CAMERA
            132, 133, 134 -> QuickTab.NETWORK
            135 -> QuickTab.DISPLAY
            136 -> QuickTab.OVERLAYS
            137 -> QuickTab.AUDIO
            else -> QuickTab.LOG
        }
        return base.copy(
            route = AppRoute.LiveConsole,
            quickPanelOpen = true,
            quickTab = tab,
            quickPanelLower = number in setOf(131, 133, 134),
        )
    }

    private fun videoAnchor(number: Int) = when (number) {
        in 41..53 -> if (number <= 46) 1 else 7
        in 54..60 -> 12
        else -> 17
    }

    private fun videoDialog(number: Int): DialogRequest = when (number) {
        41 -> choiceDialog("start_camera", "Start app with", "Rear camera (0)", listOf("Rear camera (0)", "Front camera (1)", "Rear camera (2)", "Front camera (3)"))
        42, 43 -> choiceDialog("resolution", "Resolution", "1920x1080 (16:9)", listOf("1920x1080 (16:9)", "1920x824", "1440x1080", "1280x720 (16:9)", "1088x1088", "960x720", "720x480", "640x480", "640x360 (16:9)"))
        44, 45, 46 -> choiceDialog("fps", "FPS", "System default", listOf("System default", "7-10 variable rate", "10 fixed rate", "7-15 variable rate", "15 fixed rate", "7-24 variable rate", "24 fixed rate", "7-30 variable rate", "30 fixed rate", "60 fixed rate", "120 fixed rate"))
        47 -> choiceDialog("focus_mode", "Focus mode", "Continuous auto focus", listOf("Continuous auto focus", "Infinity"))
        48 -> choiceDialog("white_balance", "White balance", "Auto", listOf("Auto", "Cloudy daylight", "Daylight", "Fluorescent", "Incandescent"))
        49 -> choiceDialog("anti_flicker", "Anti-flicker", "Auto", listOf("Off", "50Hz", "60Hz", "Auto"))
        in 50..53 -> choiceDialog("exposure", "Exposure compensation", "0", (-20..20).map { if (it > 0) "+${it / 10.0}" else "${it / 10.0}" }.map { it.removeSuffix(".0") })
        55 -> numberDialog("h264_bitrate_kbps", "Bitrate", "6000")
        56 -> choiceDialog("bitrate_mode", "Bitrate mode", "System default", listOf("System default", "Constant quality", "Variable bitrate", "Constant bitrate", "Constant bitrate w/frame drops (OFTEN UNSUPPORTED)"))
        57 -> numberDialog("keyframe", "Keyframe frequency", "2")
        58 -> choiceDialog("format", "Format", "Auto", listOf("Auto", "H.264", "HEVC"))
        59 -> choiceDialog("h264_profile", "H.264 profile", "System default", listOf("System default", "Baseline", "Constrained Baseline", "Main", "High"))
        60 -> choiceDialog("hevc_profile", "HEVC profile", "System default", listOf("System default", "Main", "Main 10", "Main 10 HDR 10", "Main 10 HDR 10 Plus"))
        61 -> choiceDialog("eis", "Electronic image stabilization", "On", listOf("System default", "Off", "On"))
        62 -> choiceDialog("ois", "Optical image stabilization", "On", listOf("System default", "Off", "On"))
        63 -> choiceDialog("noise_reduction", "Noise reduction", "Fast", listOf("System default", "Off", "Fast", "High quality", "Minimal"))
        else -> choiceDialog("adaptive_mode", "Mode", "Automatically Select", listOf("Automatically Select", "Local bonding simulation", "Legacy - Logarithmic descend", "Legacy - Ladder ascend", "Legacy - Hybrid"))
    }

    private fun audioDialog(number: Int): DialogRequest = when (number) {
        66 -> choiceDialog("audio_source", "Audio source", "Camcorder", listOf("Camcorder", "External mic (if present)", "Default audio source", "Optimised for voice calls", "Optimised for voice recognition"))
        67 -> choiceDialog("audio_channels", "Audio channels", "Stereo", listOf("Mono", "Stereo"))
        68, 69, 70 -> choiceDialog("audio_bitrate", "Bitrate", "Auto", listOf("Auto", "16 Kbps", "24 Kbps", "32 Kbps", "64 Kbps", "96 Kbps", "128 Kbps", "160 Kbps", "256 Kbps", "320 Kbps"))
        else -> choiceDialog("sample_rate", "Sample rate", "44100", listOf("8000", "11025", "12000", "16000", "22050", "24000", "32000", "44100", "48000"))
    }

    private fun marginDialog() = multiDialog(
        id = "safe_margin_ratios",
        title = "Safe margins ratios",
        selected = setOf("16:9 (1.78)"),
        options = listOf("1:1 (1.0)", "5:4 (1.25)", "4:3 (1.33)", "3:2 (1.5)", "14:9 (1.56)", "16:9 (1.78)", "2:1 (2.0)", "19.5:9 (2.17)", "21:9 (2.33)"),
    )

    private fun choiceDialog(id: String, title: String, selected: String, options: List<String>) = DialogRequest(
        id = id,
        title = title,
        type = DialogType.CHOICE_SINGLE,
        options = options,
        selectedOptions = setOf(selected),
    )

    private fun multiDialog(id: String, title: String, selected: Set<String>, options: List<String>) = DialogRequest(
        id = id,
        title = title,
        type = DialogType.CHOICE_MULTIPLE,
        options = options,
        selectedOptions = selected,
        dismissOnChoice = false,
    )

    private fun numberDialog(id: String, title: String, value: String) = DialogRequest(
        id = id,
        title = title,
        type = DialogType.NUMBER,
        initialValue = value,
    )

    private fun textDialog(id: String, title: String, value: String, labels: List<String> = emptyList()) = DialogRequest(
        id = id,
        title = title,
        type = DialogType.TEXT,
        initialValue = value,
        options = labels,
    )

    private fun alertDialog(id: String, title: String, message: String, positive: String, negative: String) = DialogRequest(
        id = id,
        title = title,
        type = DialogType.ALERT,
        message = message,
        positiveLabel = positive,
        negativeLabel = negative,
    )

    private fun canonicalId(number: Int, raw: String) = if (raw.length > 3) raw else "%03d".format(number)
}
