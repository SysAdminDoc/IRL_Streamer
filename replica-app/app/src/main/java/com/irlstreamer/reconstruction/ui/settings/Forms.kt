package com.irlstreamer.reconstruction.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irlstreamer.reconstruction.MainViewModel
import com.irlstreamer.reconstruction.model.AppUiState
import com.irlstreamer.reconstruction.model.DialogRequest
import com.irlstreamer.reconstruction.model.DialogType
import com.irlstreamer.reconstruction.model.SettingsPage
import com.irlstreamer.reconstruction.ui.components.AuditedAppBar
import com.irlstreamer.reconstruction.ui.components.ConnectionFormField
import com.irlstreamer.reconstruction.ui.components.FormTextField
import com.irlstreamer.reconstruction.ui.components.FormHint
import com.irlstreamer.reconstruction.ui.components.FormSectionHeader
import com.irlstreamer.reconstruction.ui.components.InfoRow
import com.irlstreamer.reconstruction.ui.components.PreferenceRow
import com.irlstreamer.reconstruction.ui.components.SectionHeader
import com.irlstreamer.reconstruction.ui.components.SliderPreferenceRow
import com.irlstreamer.reconstruction.ui.components.TogglePreferenceRow
import com.irlstreamer.reconstruction.ui.theme.AuditColors

@Composable
fun ConnectionFormScreen(state: AppUiState, viewModel: MainViewModel) {
    val variant = state.runtime.formVariant ?: "empty"
    var name by remember(variant) { mutableStateOf(if (variant == "invalid") "AuditTest3not_a_url" else if (variant.startsWith("rtmp")) "Local fixture" else "") }
    var url by remember(variant) { mutableStateOf(if (variant.startsWith("rtmp")) "rtmp://198.51.100.1/live" else "") }
    var login by remember(variant) { mutableStateOf("") }
    var password by remember(variant) { mutableStateOf("") }
    val rtmp = url.startsWith("rtmp://", ignoreCase = true) || variant.startsWith("rtmp")
    val authorization = variant == "rtmp_auth" || state.runtime.transientValues["rtmp_target"] == "RTMP authorization"
    val listState = key(state.runtime.debugScreenId) {
        rememberLazyListState(initialFirstVisibleItemIndex = state.runtime.settingsScrollIndex.coerceAtMost(3))
    }

    FormScaffold("New outgoing connection", viewModel) {
        LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
            // Audit evidence: screen 025 gives the exact form structure.
            //   "Quick Site Setup"  [75,309]-[2181,377]
            //   two buttons         [75,377]-[777,512] and [777,377]-[1479,512], 702 px each
            //   one hint slot       [98,580]-[2158,633] whose text changes with the URL
            //   Name  EditText      [75,664]-[2181,790]
            //   URL   EditText      [75,821]-[2181,947]   (field pitch 157 px)
            // The form previously rendered two separate hint rows and stretched the
            // buttons across the full width.
            item { Spacer(Modifier.height(32.dp)) }
            item { FormSectionHeader("Quick Site Setup") }
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    FormButton("PLATFORM A", Modifier.width(249.6.dp)) {
                        viewModel.showDialog(DialogRequest(
                            id = "platform_a",
                            title = "Platform A",
                            type = DialogType.TEXT,
                            options = listOf("Username", "Stream key"),
                        ))
                    }
                    FormButton("PLATFORM B", Modifier.width(249.6.dp)) {
                        viewModel.showDialog(DialogRequest(
                            id = "platform_b",
                            title = "Platform B",
                            type = DialogType.TEXT,
                            options = listOf("Username", "Stream URL", "Stream key"),
                        ))
                    }
                }
            }
            // One hint slot, not two: the audited TextView keeps its position and
            // swaps its text once the URL identifies a protocol.
            item { Spacer(Modifier.height(24.18.dp)) }
            item {
                FormHint(
                    if (rtmp) {
                        "RTMP URL schema is rtmp://server/application/streamkey."
                    } else {
                        "Start typing or paste URL to view protocol-specific fields for authentication etc."
                    },
                )
            }
            item { Spacer(Modifier.height(2.49.dp)) }
            item { ConnectionFormField("connection_name", "Name", name, { name = it }) }
            item { Spacer(Modifier.height(10.67.dp)) }
            item { ConnectionFormField("connection_url", "URL", url, { url = it }, KeyboardType.Uri) }
            if (rtmp) {
                item { Spacer(Modifier.height(43.02.dp)) }
                item {
                    PreferenceRow(
                        id = "rtmp_target",
                        title = "Target type - Leave Default for most cases",
                        summary = state.runtime.transientValues["rtmp_target"] ?: if (authorization) "RTMP authorization" else "Default (no authorization)",
                        enabled = true,
                        onClick = {
                            viewModel.showDialog(DialogRequest(
                                id = "rtmp_target",
                                title = "Target type",
                                type = DialogType.CHOICE_SINGLE,
                                options = listOf("Default (no authorization)", "RTMP authorization", "Akamai/Dacast", "Limelight Networks", "Periscope Producer"),
                                selectedOptions = setOf(if (authorization) "RTMP authorization" else "Default (no authorization)"),
                            ))
                        },
                    )
                }
            }
            if (authorization) {
                item { ConnectionFormField("connection_login", "Login", login, { login = it }) }
                item { ConnectionFormField("connection_password", "Password", password, { password = it }, visualTransformation = PasswordVisualTransformation()) }
            }
            item {
                FormFooter(
                    saveEnabled = rtmp,
                    onCancel = { viewModel.back() },
                    onSave = {
                        viewModel.saveConnection(name, url)
                        viewModel.showToast("Connection saved")
                        viewModel.back()
                    },
                )
            }
        }
    }
}

@Composable
fun LayerFormScreen(page: SettingsPage, state: AppUiState, viewModel: MainViewModel) {
    val timestamp = page == SettingsPage.TIMESTAMP_FORM
    val textLayer = page == SettingsPage.TEXT_LAYER_FORM || timestamp
    var name by remember(page) { mutableStateOf(if (timestamp) "Timestamp" else "") }
    var source by remember(page) {
        mutableStateOf(
            if (timestamp) "Date/time · MMM dd, HH:mm:ss · en-US" else "",
        )
    }
    var periodic by remember(page) { mutableStateOf(timestamp) }
    var active by remember(page) { mutableStateOf(false) }
    var scale by remember(page) { mutableFloatStateOf(if (timestamp) 20f else 0f) }
    val title = "Layer"
    val listState = key(state.runtime.debugScreenId) {
        rememberLazyListState(initialFirstVisibleItemIndex = state.runtime.settingsScrollIndex.coerceAtMost(8))
    }

    FormScaffold(title, viewModel) {
        LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
            item { FormTextField("layer_name", "Name", name, { name = it }) }
            item {
                PreferenceRow(
                    id = "layer_type",
                    title = "Type",
                    summary = if (textLayer) "Text" else "Picture",
                    enabled = !timestamp,
                    onClick = if (timestamp) null else {
                        {
                            viewModel.showDialog(DialogRequest(
                                id = "layer_type",
                                title = "Type",
                                type = DialogType.CHOICE_SINGLE,
                                options = listOf("Picture", "Text"),
                                selectedOptions = setOf(if (textLayer) "Text" else "Picture"),
                            ))
                        }
                    },
                )
            }
            if (textLayer) {
                item { FormTextField("layer_html", "HTML code", source, { source = it }) }
            } else {
                item { FormTextField("layer_url", "URL", source, { source = it }, KeyboardType.Uri) }
                item {
                    PreferenceRow("select_file", "Select file", "You may either specify a remote file URL or select a local file", true, onClick = {
                        viewModel.showToast("File selection is a local fixture")
                    })
                }
            }
            item { TogglePreferenceRow("layer_periodic", "Periodic refresh", "", periodic, true) { periodic = !periodic } }
            if (periodic && timestamp) {
                item { PreferenceRow("refresh_interval", "Refresh interval (sec)", "1", true, onClick = { viewModel.showDialog(DialogRequest("refresh_interval", "Refresh interval (sec)", DialogType.NUMBER, initialValue = "1")) }) }
            }
            if (textLayer) {
                item { TogglePreferenceRow("layer_active", "Active", "", active, true) { active = !active } }
                item {
                    SliderPreferenceRow("layer_scale", "Scale", "Scale is relative to video resolution; Off uses original size.", scale, 0f..100f, 99, if (scale == 0f) "Off" else "${scale.toInt()} %") { scale = it }
                }
                item {
                    PreferenceRow("layer_position", "Position", "Center", true, onClick = {
                        viewModel.showDialog(DialogRequest("layer_position", "Position", DialogType.CHOICE_SINGLE, options = listOf("Top left", "Top right", "Center", "Bottom left", "Bottom right"), selectedOptions = setOf("Center")))
                    })
                }
                item { PreferenceRow("layer_z", "Z Order", "5", true, onClick = { viewModel.showDialog(DialogRequest("layer_z", "Z Order", DialogType.NUMBER, initialValue = "5")) }) }
                item { InfoRow("z_help", "Layers with greater Z Order will appear at front.", emphasized = true) }
            }
            item {
                FormFooter(
                    saveEnabled = true,
                    onCancel = { viewModel.back() },
                    onSave = {
                        if (textLayer && source.isBlank()) {
                            viewModel.showToast("Please input non-empty text/html code")
                        } else {
                            viewModel.showToast("Layer saved to the local fixture")
                            viewModel.back()
                        }
                    },
                )
            }
        }
    }
}

@Composable
fun WebOverlayFormScreen(state: AppUiState, viewModel: MainViewModel) {
    val custom = state.runtime.formVariant == "custom" || state.runtime.transientValues["position"] == "Custom"
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var css by remember { mutableStateOf("body { background-color: rgba(0, 0, 0, 0); margin: 0px auto; overflow: hidden; }") }
    var horizontal by remember(custom) { mutableFloatStateOf(if (custom) 100f else 50f) }
    var vertical by remember(custom) { mutableFloatStateOf(if (custom) 100f else 50f) }
    val listState = key(state.runtime.debugScreenId) {
        rememberLazyListState(initialFirstVisibleItemIndex = state.runtime.settingsScrollIndex.coerceAtMost(9))
    }

    FormScaffold("Layer", viewModel) {
        LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
            item { FormTextField("web_name", "Name", name, { name = it }) }
            item { FormTextField("web_url", "URL", url, { url = it }, KeyboardType.Uri) }
            item { FormTextField("web_css", "Custom CSS", css, { css = it }, auditedHeight = 72.53.dp) }
            item {
                PreferenceRow("view_mode", "View mode", state.runtime.transientValues["view_mode"] ?: "Preview + stream", true, onClick = {
                    viewModel.showDialog(DialogRequest("view_mode", "View mode", DialogType.CHOICE_SINGLE, options = listOf("Preview + stream", "Preview", "Stream"), selectedOptions = setOf("Preview + stream")))
                })
            }
            item {
                PreferenceRow("position", "Position", if (custom) "Custom" else state.runtime.transientValues["position"] ?: "Center", true, onClick = {
                    viewModel.showDialog(DialogRequest("position", "Position", DialogType.CHOICE_SINGLE, options = listOf("Top left", "Top right", "Center", "Bottom left", "Bottom right", "Custom"), selectedOptions = setOf(if (custom) "Custom" else "Center")))
                })
            }
            if (custom) {
                item { SliderPreferenceRow("h_position", "H Position", "", horizontal, 0f..100f, 99, "Right ${horizontal.toInt()} %") { horizontal = it } }
                item { SliderPreferenceRow("v_position", "V Position", "", vertical, 0f..100f, 99, "Bottom ${vertical.toInt()} %") { vertical = it } }
            }
            item { PreferenceRow("web_z", "Z Order", "1", true, onClick = { viewModel.showDialog(DialogRequest("web_z", "Z Order", DialogType.NUMBER, initialValue = "1")) }) }
            item { InfoRow("web_z_help", "Layers with greater Z Order will appear at front.", emphasized = true) }
            item { SectionHeader("WebView options") }
            item { InfoRow("canvas_help", "Width/Height scaled to 1080p canvas", emphasized = true) }
            item { PreferenceRow("web_width", "Width", "1280", true, onClick = { viewModel.showDialog(DialogRequest("web_width", "Width", DialogType.NUMBER, initialValue = "1280")) }) }
            item { PreferenceRow("web_height", "Height", "720", true, onClick = { viewModel.showDialog(DialogRequest("web_height", "Height", DialogType.NUMBER, initialValue = "720")) }) }
            item { PreferenceRow("web_scale", "Scale in percents", "100", true, onClick = { viewModel.showDialog(DialogRequest("web_scale", "Scale in percents", DialogType.NUMBER, initialValue = "100")) }) }
            item {
                FormFooter(true, { viewModel.back() }) {
                    viewModel.showToast("Web overlay saved to the local fixture; remote content remains disabled")
                    viewModel.back()
                }
            }
        }
    }
}

@Composable
private fun FormScaffold(title: String, viewModel: MainViewModel, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuditColors.SettingsBackground),
    ) {
        AuditedAppBar(title) { viewModel.back() }
        content()
    }
}

@Composable
private fun FormFooter(saveEnabled: Boolean, onCancel: () -> Unit, onSave: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        FormButton("CANCEL", Modifier.weight(1f), onClick = onCancel)
        FormButton("SAVE", Modifier.weight(1f), enabled = saveEnabled, onClick = onSave)
    }
}

@Composable
private fun FormButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(48.dp)
            .testTag("form_${label.lowercase().replace(' ', '_')}"),
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF4A4A4A),
            contentColor = AuditColors.PrimaryText,
            disabledContainerColor = Color(0xFF3A3A3A),
            disabledContentColor = AuditColors.DisabledText,
        ),
    ) {
        Text(label, fontSize = 14.sp)
    }
}
