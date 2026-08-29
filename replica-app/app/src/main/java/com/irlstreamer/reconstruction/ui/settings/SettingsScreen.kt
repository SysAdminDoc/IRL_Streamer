package com.irlstreamer.reconstruction.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import com.irlstreamer.reconstruction.BuildConfig
import com.irlstreamer.reconstruction.MainViewModel
import com.irlstreamer.reconstruction.debug.Harness
import com.irlstreamer.reconstruction.model.AppRoute
import com.irlstreamer.reconstruction.model.AppUiState
import com.irlstreamer.reconstruction.model.DialogRequest
import com.irlstreamer.reconstruction.model.DialogType
import com.irlstreamer.reconstruction.model.SettingsPage
import com.irlstreamer.reconstruction.ui.components.AuditedAppBar
import com.irlstreamer.reconstruction.ui.components.InfoRow
import com.irlstreamer.reconstruction.ui.components.isSecret
import com.irlstreamer.reconstruction.ui.components.PreferenceRow
import com.irlstreamer.reconstruction.ui.components.SectionHeader
import com.irlstreamer.reconstruction.ui.components.SliderPreferenceRow
import com.irlstreamer.reconstruction.ui.components.TogglePreferenceRow
import com.irlstreamer.reconstruction.ui.theme.AuditColors

@Composable
fun SettingsScreen(state: AppUiState, viewModel: MainViewModel) {
    val route = state.runtime.route as? AppRoute.Settings ?: return
    when (route.page) {
        SettingsPage.CONNECTION_FORM -> ConnectionFormScreen(state, viewModel)
        SettingsPage.PICTURE_LAYER_FORM,
        SettingsPage.TEXT_LAYER_FORM,
        SettingsPage.TIMESTAMP_FORM,
        -> LayerFormScreen(route.page, state, viewModel)
        SettingsPage.WEB_OVERLAY_FORM -> WebOverlayFormScreen(state, viewModel)
        else -> GenericSettingsScreen(route.page, state, viewModel)
    }
}

@Composable
private fun GenericSettingsScreen(page: SettingsPage, state: AppUiState, viewModel: MainViewModel) {
    val spec = SettingsCatalog.page(page)
    val visibleItems = remember(spec.items, state.effectiveSafeMargins) {
        if (page == SettingsPage.DISPLAY && !state.effectiveSafeMargins) {
            spec.items.filterNot {
                (it is SettingItem.Row && it.id == "safe_margin_ratios") ||
                    (it is SettingItem.Slider && it.key == "safe_margin_indent_percent")
            }
        } else {
            spec.items
        }
    }
    // Prefer the audited scroll position: the anchor names the row that the audit
    // hierarchy shows flush against the top of the list viewport. Fall back to the
    // catalog index only when a state has no recorded anchor.
    val anchorLabel = Harness.overrides.scrollAnchorLabel(LocalContext.current, state.runtime.debugScreenId)
    val anchorIndex = anchorLabel
        ?.let { label -> visibleItems.indexOfFirst { itemTitle(it) == label } }
        ?.takeIf { it >= 0 }
    if (anchorLabel != null && anchorIndex == null && BuildConfig.ENABLE_DEBUG_STATE_SELECTOR) {
        // Falling back to the catalog index is the behaviour the anchors exist to
        // replace, so it must never happen quietly: an anchor that resolves to
        // nothing looks identical to a state that never had one.
        Log.w(
            "CaptureAnchors",
            "Anchor \"$anchorLabel\" for ${state.runtime.debugScreenId} matched no row on $page; " +
                "falling back to index ${state.runtime.settingsScrollIndex}",
        )
    }
    val initialIndex = anchorIndex
        ?: state.runtime.settingsScrollIndex.coerceIn(0, (visibleItems.size - 1).coerceAtLeast(0))
    // Keyed on the page as well: every catalog page renders through this one
    // composable, so without it a positionally-remembered list state is shared
    // across pages and Root opens at the scroll offset Video was left at.
    val listState = key(page, state.runtime.debugScreenId, initialIndex) {
        rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuditColors.SettingsBackground),
    ) {
        AuditedAppBar(spec.title) {
            if (!viewModel.back()) viewModel.navigateToLive()
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(AuditColors.SettingsBackground),
            state = listState,
        ) {
            items(
                count = visibleItems.size,
                key = { index -> itemKey(visibleItems[index], index) },
            ) { index ->
                when (val item = visibleItems[index]) {
                    is SettingItem.Section -> SectionHeader(item.title)
                    is SettingItem.Info -> InfoRow(item.id, item.text)
                    is SettingItem.Row -> {
                        val derivedEnabled = item.enabled && !(item.id == "h264_bitrate" && state.effectiveBitrateMatch)
                        PreferenceRow(
                            id = item.id,
                            title = item.title,
                            summary = resolveSummary(item, state),
                            enabled = derivedEnabled,
                            accentTitle = item.accentTitle,
                            onClick = if (derivedEnabled) {
                                { handleAction(item.action, state, viewModel) }
                            } else null,
                        )
                    }
                    is SettingItem.Toggle -> {
                        val value = booleanValue(item.key, state)
                        TogglePreferenceRow(
                            key = item.key,
                            title = item.title,
                            summary = if (!value && item.summaryOff != null) item.summaryOff else item.summary,
                            checked = value,
                            enabled = item.enabled,
                            onToggle = { viewModel.toggleBoolean(item.key, value) },
                        )
                    }
                    is SettingItem.Slider -> {
                        val value = sliderValue(item.key, state)
                        SliderPreferenceRow(
                            key = item.key,
                            title = item.title,
                            summary = item.summary,
                            value = value,
                            valueRange = item.min..item.max,
                            steps = item.steps,
                            valueLabel = item.formatter(value),
                            onValueChange = { updateSlider(item.key, it, viewModel) },
                        )
                    }
                }
            }
        }
    }
}

private fun handleAction(action: SettingAction, state: AppUiState, viewModel: MainViewModel) {
    when (action) {
        SettingAction.None -> Unit
        is SettingAction.Navigate -> viewModel.navigateTo(action.page)
        is SettingAction.Dialog -> viewModel.showDialog(withCurrentValue(action.request, state))
        SettingAction.OpenFolder -> viewModel.requestFolderPicker()
        is SettingAction.Toast -> viewModel.showToast(action.message)
    }
}

/**
 * Seed a dialog with the value it is editing.
 *
 * The catalog's `DialogRequest` carries the audited *default*, which is correct
 * for a first open and wrong for every one after it: a reopened dialog showed
 * the default instead of the saved value, and confirming it wrote that default
 * back over the user's setting. A multi-select was worse - reopening dropped
 * every selection the catalog default did not contain.
 *
 * Debug states are unaffected: the capture harness injects its dialogs into
 * runtime state directly and never routes through here, so the audited captures
 * keep exactly the selection they were captured with.
 */
internal fun withCurrentValue(request: DialogRequest, state: AppUiState): DialogRequest {
    val transient = state.runtime.transientValues[request.id] ?: state.settings.choiceValues[request.id]
    return when (request.type) {
        DialogType.TEXT, DialogType.NUMBER -> {
            val current = transient ?: persistedValue(request.id, state)
            if (current != null) request.copy(initialValue = current) else request
        }
        DialogType.CHOICE_SINGLE ->
            if (transient != null) request.copy(selectedOptions = setOf(transient)) else request
        DialogType.CHOICE_MULTIPLE -> {
            val current = persistedSelection(request.id, state)
            if (current != null) request.copy(selectedOptions = current) else request
        }
        else -> request
    }
}

private fun persistedValue(id: String, state: AppUiState): String? = when (id) {
    "chat_font_scale" -> state.settings.chatFontScale.toString()
    "alert_dashboard_scale" -> state.settings.alertDashboardScale.toString()
    "h264_bitrate_kbps" -> state.settings.h264BitrateKbps.toString()
    else -> null
}

private fun persistedSelection(id: String, state: AppUiState): Set<String>? = when (id) {
    "safe_margin_ratios" -> state.settings.safeMarginRatios
    else -> null
}

/** Visible label of a row, used to resolve an audited scroll anchor. */
internal fun itemTitle(item: SettingItem): String = when (item) {
    is SettingItem.Section -> item.title
    is SettingItem.Row -> item.title
    is SettingItem.Toggle -> item.title
    is SettingItem.Slider -> item.title
    is SettingItem.Info -> item.text
}

private fun itemKey(item: SettingItem, index: Int): String = when (item) {
    is SettingItem.Section -> "section_${item.title}_$index"
    is SettingItem.Row -> "row_${item.id}"
    is SettingItem.Toggle -> "toggle_${item.key}"
    is SettingItem.Slider -> "slider_${item.key}"
    is SettingItem.Info -> "info_${item.id}"
}

private fun resolveSummary(item: SettingItem.Row, state: AppUiState): String {
    val entered = state.runtime.transientValues[item.id] ?: state.settings.choiceValues[item.id]
    if (entered != null) {
        if (entered.isBlank()) return item.summary
        // Never print a secret into a settings row. The value is held so the row
        // can show that something is set, not so it can show what.
        return if (isSecret(item.title)) "*".repeat(entered.length.coerceAtMost(12)) else entered
    }
    return when (item.valueKey) {
        "chat_font_scale" -> state.settings.chatFontScale.toString()
        "alert_dashboard_scale" -> state.settings.alertDashboardScale.toString()
        "h264_bitrate" -> "${state.settings.h264BitrateKbps} Kbps"
        "safe_margin_ratios" -> state.settings.safeMarginRatios.joinToString()
        else -> item.summary
    }
}

private fun booleanValue(key: String, state: AppUiState): Boolean {
    // Debug overrides win, then the persisted value, then the audited default.
    state.runtime.transientBooleans[key]?.let { return it }
    state.settings.extraToggles[key]?.let { persisted ->
        if (key !in namedToggleKeys) return persisted
    }
    return when (key) {
        "show_platform_icons" -> state.effectivePlatformIcons
        "show_bots" -> state.settings.showBots
        "show_commands" -> state.settings.showCommands
        "show_user_count" -> state.settings.showUserCount
        "cellular_enabled" -> state.settings.cellularEnabled
        "wifi_enabled" -> state.settings.wifiEnabled
        "ethernet_enabled" -> state.settings.ethernetEnabled
        "bitrate_matches_resolution" -> state.effectiveBitrateMatch
        "record_stream" -> state.settings.recordStream
        "split_sections" -> state.settings.splitSections
        "audio_meter_visible" -> state.effectiveAudioMeter
        "grid_visible" -> state.effectiveGrid
        "safe_margins_visible" -> state.effectiveSafeMargins
        "timestamp_active" -> state.effectiveTimestamp
        "web_overlay_master" -> state.settings.webOverlayMaster
        "show_layers_preview", "standby_mode", "keep_streaming", "auto_adjust_format" -> true
        else -> false
    }
}

/** Keys with a named field in `ReplicaSettings`; those never read the generic map. */
private val namedToggleKeys = setOf(
    "show_platform_icons", "show_bots", "show_commands", "show_user_count",
    "cellular_enabled", "wifi_enabled", "ethernet_enabled",
    "bitrate_matches_resolution", "record_stream", "split_sections",
    "audio_meter_visible", "grid_visible", "safe_margins_visible",
    "timestamp_active", "web_overlay_master",
)

private fun sliderValue(key: String, state: AppUiState): Float = when (key) {
    "cellular_weight" -> state.settings.cellularWeight.toFloat()
    "wifi_weight" -> state.effectiveWifiWeight.toFloat()
    "ethernet_weight" -> state.settings.ethernetWeight.toFloat()
    "audio_input_gain_db" -> state.effectiveInputGain
    "safe_margin_indent_percent" -> state.effectiveSafeMarginIndent.toFloat()
    else -> 0f
}

private fun updateSlider(key: String, value: Float, viewModel: MainViewModel) {
    when (key) {
        "audio_input_gain_db" -> viewModel.setFloat(key, value)
        else -> viewModel.setInt(key, value.toInt())
    }
}
