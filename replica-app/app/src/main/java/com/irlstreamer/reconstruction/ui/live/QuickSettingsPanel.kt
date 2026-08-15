package com.irlstreamer.reconstruction.ui.live

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irlstreamer.reconstruction.MainViewModel
import com.irlstreamer.reconstruction.model.AppUiState
import com.irlstreamer.reconstruction.model.QuickTab
import com.irlstreamer.reconstruction.ui.components.AuditedSlider
import com.irlstreamer.reconstruction.ui.components.AuditedSwitch
import com.irlstreamer.reconstruction.ui.theme.AuditColors

@Composable
fun QuickSettingsPanel(
    state: AppUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val tabScroll = rememberScrollState()
    val bodyScroll = rememberScrollState(initial = if (state.runtime.quickPanelLower) 220 else 0)
    LaunchedEffect(state.runtime.quickTab) {
        val target = when (state.runtime.quickTab) {
            QuickTab.CAMERA, QuickTab.NETWORK -> 0
            QuickTab.DISPLAY -> 60
            QuickTab.OVERLAYS -> 150
            QuickTab.AUDIO -> 230
            QuickTab.LOG -> 300
        }
        tabScroll.animateScrollTo(target.coerceAtMost(tabScroll.maxValue))
    }
    LaunchedEffect(state.runtime.quickPanelLower) {
        bodyScroll.scrollTo(if (state.runtime.quickPanelLower) bodyScroll.maxValue else 0)
    }

    Column(
        modifier = modifier
            .background(AuditColors.QuickPanel)
            .testTag("quick_settings_panel"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .horizontalScroll(tabScroll)
                .background(Color.Black),
        ) {
            QuickTab.entries.forEach { tab ->
                val selected = tab == state.runtime.quickTab
                Box(
                    modifier = Modifier
                        .width(
                            when (tab) {
                                QuickTab.CAMERA -> 80.36.dp
                                QuickTab.NETWORK -> 90.31.dp
                                QuickTab.DISPLAY -> 79.29.dp
                                QuickTab.OVERLAYS -> 88.dp
                                QuickTab.AUDIO -> 64.dp
                                QuickTab.LOG -> 52.dp
                            },
                        )
                        .height(32.dp)
                        .clickable { viewModel.selectQuickTab(tab) }
                        .drawBehind {
                            if (selected) {
                                drawRect(
                                    AuditColors.Accent,
                                    topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - 2.dp.toPx()),
                                    size = androidx.compose.ui.geometry.Size(size.width, 2.dp.toPx()),
                                )
                            }
                        }
                        .testTag("quick_tab_${tab.name.lowercase()}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        tab.name,
                        color = if (selected) Color.White else Color(0xFF999999),
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(bodyScroll)
                .padding(start = 8.18.dp, top = 8.18.dp, end = 8.18.dp),
        ) {
            when (state.runtime.quickTab) {
                QuickTab.CAMERA -> CameraQuickSettings(state, viewModel)
                QuickTab.NETWORK -> NetworkQuickSettings(state, viewModel)
                QuickTab.DISPLAY -> DisplayQuickSettings(state, viewModel)
                QuickTab.OVERLAYS -> OverlayQuickSettings(state, viewModel)
                QuickTab.AUDIO -> AudioQuickSettings(state, viewModel)
                QuickTab.LOG -> LogQuickSettings()
            }
        }
    }
}

@Composable
private fun CameraQuickSettings(state: AppUiState, viewModel: MainViewModel) {
    var exposure by remember { mutableFloatStateOf(0f) }
    var zoom by remember { mutableFloatStateOf(1f) }
    QuickHeading("Active camera")
    val cameras = listOf(
        0 to "id:0 [73° FoV, rear]",
        1 to "id:1 [67° FoV, front]",
        2 to "id:2 [103° FoV, rear]",
        3 to "id:3 [61° FoV, front]",
    )
    cameras.forEach { (id, label) ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clickable { viewModel.setCurrentCamera(id) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = state.runtime.currentCameraId == id,
                onClick = { viewModel.setCurrentCamera(id) },
                colors = RadioButtonDefaults.colors(selectedColor = AuditColors.Accent, unselectedColor = Color(0xFFAAAAAA)),
            )
            Text(label, color = AuditColors.SecondaryText, fontSize = 14.sp)
        }
    }
    Spacer(Modifier.height(8.18.dp))
    QuickToggle("Torch", state.runtime.transientBooleans["torch"] ?: false) {
        viewModel.toggleBoolean("torch", state.runtime.transientBooleans["torch"] ?: false)
    }
    Spacer(Modifier.height(8.18.dp))
    QuickSlider("Exposure compensation", exposure, -2f..2f, if (exposure > 0) "+%.1f".format(exposure) else "%.1f".format(exposure)) { exposure = it }
    QuickSlider("Zoom", zoom, 1f..8f, "%.2fx".format(zoom)) { zoom = it }
    QuickToggle("Focus mode", state.runtime.transientBooleans["quick_focus_mode"] ?: false) { viewModel.toggleBoolean("quick_focus_mode", state.runtime.transientBooleans["quick_focus_mode"] ?: false) }
    QuickToggle("White balance", state.runtime.transientBooleans["quick_white_balance"] ?: false) { viewModel.toggleBoolean("quick_white_balance", state.runtime.transientBooleans["quick_white_balance"] ?: false) }
    QuickToggle("Anti-flicker", state.runtime.transientBooleans["quick_antiflicker"] ?: false) { viewModel.toggleBoolean("quick_antiflicker", state.runtime.transientBooleans["quick_antiflicker"] ?: false) }
    QuickHeading("Tap settings")
    QuickToggle("Focus", state.runtime.transientBooleans["tap_focus"] ?: false) { viewModel.toggleBoolean("tap_focus", state.runtime.transientBooleans["tap_focus"] ?: false) }
    QuickToggle("Exposure", state.runtime.transientBooleans["tap_exposure"] ?: false) { viewModel.toggleBoolean("tap_exposure", state.runtime.transientBooleans["tap_exposure"] ?: false) }
    QuickToggle("White balance", state.runtime.transientBooleans["tap_wb"] ?: false) { viewModel.toggleBoolean("tap_wb", state.runtime.transientBooleans["tap_wb"] ?: false) }
}

@Composable
private fun NetworkQuickSettings(state: AppUiState, viewModel: MainViewModel) {
    var bitrate by remember { mutableFloatStateOf(6f) }
    NetworkLine("Conditioner")
    NetworkLine("Off", alignEnd = true)
    Spacer(Modifier.height(8.18.dp))
    NetworkLine("Bitrate Mode")
    Spacer(Modifier.height(27.02.dp))
    NetworkLine("Current Adaptive Bitrate")
    NetworkLine("0bps", alignEnd = true)
    Spacer(Modifier.height(8.18.dp))
    QuickSlider("Target Video Bitrate", bitrate, 0.5f..20f, "%.1fMbps".format(bitrate)) { bitrate = it }
    Text(
        "Double tap \"Target Bitrate\" to reset bitrate to start value",
        color = Color(0xFF999999),
        fontSize = 14.sp,
        lineHeight = 18.sp,
        modifier = Modifier.fillMaxWidth().height(35.2.dp),
    )
    Spacer(Modifier.height(12.09.dp))
    QuickHeading("Bonding")
    QuickToggle("Enable Cellular", state.settings.cellularEnabled) { viewModel.toggleBoolean("cellular_enabled", state.settings.cellularEnabled) }
    QuickSlider("Cellular Weight", state.settings.cellularWeight.toFloat(), 0f..100f, state.settings.cellularWeight.toString()) { viewModel.setInt("cellular_weight", it.toInt()) }
    QuickToggle("Enable WiFi", state.settings.wifiEnabled) { viewModel.toggleBoolean("wifi_enabled", state.settings.wifiEnabled) }
    QuickSlider("WiFi Weight", state.effectiveWifiWeight.toFloat(), 0f..100f, state.effectiveWifiWeight.toString()) { viewModel.setInt("wifi_weight", it.toInt()) }
    QuickToggle("Enable Ethernet/USB Connection", state.settings.ethernetEnabled) { viewModel.toggleBoolean("ethernet_enabled", state.settings.ethernetEnabled) }
    QuickSlider("Ethernet Weight", state.settings.ethernetWeight.toFloat(), 0f..100f, state.settings.ethernetWeight.toString()) { viewModel.setInt("ethernet_weight", it.toInt()) }
    QuickHeading("Stats")
    Spacer(Modifier.height(30.dp))
}

@Composable
private fun DisplayQuickSettings(state: AppUiState, viewModel: MainViewModel) {
    QuickToggle("Grid", state.effectiveGrid) { viewModel.toggleBoolean("grid_visible", state.effectiveGrid) }
    QuickToggle("Safe margins", state.effectiveSafeMargins) { viewModel.toggleBoolean("safe_margins_visible", state.effectiveSafeMargins) }
    QuickToggle("Lock Screen", false) { viewModel.showToast("Lock Screen is disabled in this safe reconstruction") }
    Text("Block all screen touches, useful in water or rain situations.", color = Color(0xFF999999), fontSize = 12.sp, lineHeight = 15.sp)
}

@Composable
private fun OverlayQuickSettings(state: AppUiState, viewModel: MainViewModel) {
    QuickToggle("Overlays", state.settings.webOverlayMaster) { viewModel.toggleBoolean("web_overlay_master", state.settings.webOverlayMaster) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clickable { viewModel.toggleBoolean("timestamp_active", state.effectiveTimestamp) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = state.effectiveTimestamp,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(checkedColor = AuditColors.Accent),
        )
        Text("Text - Timestamp", color = AuditColors.PrimaryText, fontSize = 14.sp)
    }
    Text("Long Press to edit text overlays", color = Color(0xFF999999), fontSize = 12.sp)
}

@Composable
private fun AudioQuickSettings(state: AppUiState, viewModel: MainViewModel) {
    QuickSlider("Input gain", state.effectiveInputGain, -40f..10f, "%.1fdB".format(state.effectiveInputGain)) { viewModel.setFloat("audio_input_gain_db", it) }
    Text("Double tap \"Input gain\" to reset to 0dB", color = Color(0xFF999999), fontSize = 11.sp)
}

@Composable
private fun LogQuickSettings() {
    Text(
        text = "Not writing to file\nGo to Record settings to toggle writing\n" +
            "21:25:52 {type=MIC, source=local, channels=2, sampleRate=44100, bitRate=96000}\n" +
            "21:25:52 {mime=video/avc, bitRate=6000000, fps=30, keyFrameInterval=2, videoSize=1920x1080}\n" +
            "21:25:52 local camera fixture opened\n" +
            "21:25:52 H.264, 1920x1080\n" +
            "21:25:53 onAudioCaptureStateChanged, state=SIMULATED\n" +
            "21:25:53 onVideoCaptureStateChanged, state=SIMULATED",
        color = Color(0xFFBDBDBD),
        fontSize = 9.sp,
        lineHeight = 11.sp,
        modifier = Modifier.testTag("quick_log"),
    )
}

@Composable
private fun QuickHeading(text: String) {
    Row(modifier = Modifier.fillMaxWidth().height(18.84.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Check, null, tint = Color(0xFF148D28), modifier = Modifier.size(18.dp))
        Text(text, color = AuditColors.SecondaryText, fontSize = 14.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun NetworkLine(text: String, alignEnd: Boolean = false) {
    Box(
        modifier = Modifier.fillMaxWidth().height(18.84.dp),
        contentAlignment = if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Text(text, color = AuditColors.SecondaryText, fontSize = 14.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun QuickToggle(title: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(27.02.dp)
            .clickable(onClick = onToggle)
            .testTag("quick_${title.lowercase().replace(' ', '_').replace('/', '_')}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = AuditColors.PrimaryText, fontSize = 14.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f))
        AuditedSwitch(
            checked = checked,
            enabled = true,
        )
    }
}

@Composable
private fun QuickSlider(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    label: String,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().height(45.87.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(18.84.dp), contentAlignment = Alignment.CenterStart) {
            Text(title, color = AuditColors.SecondaryText, fontSize = 14.sp, lineHeight = 18.sp)
        }
        Row(modifier = Modifier.fillMaxWidth().height(18.84.dp), verticalAlignment = Alignment.CenterVertically) {
            AuditedSlider(
                value = value.coerceIn(range),
                onValueChange = onValueChange,
                valueRange = range,
                steps = 0,
                modifier = Modifier.weight(1f).height(18.84.dp),
                touchHeight = 18.84.dp,
                trackHeight = 1.2.dp,
                thumbDiameter = 11.dp,
            )
            Box(Modifier.width(54.04.dp).height(18.84.dp), contentAlignment = Alignment.CenterEnd) {
                Text(label, color = AuditColors.SecondaryText, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
        Spacer(Modifier.height(8.18.dp))
    }
}
