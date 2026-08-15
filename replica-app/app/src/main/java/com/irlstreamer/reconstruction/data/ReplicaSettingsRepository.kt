package com.irlstreamer.reconstruction.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.irlstreamer.reconstruction.model.ReplicaSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.replicaDataStore by preferencesDataStore(name = "irl_streamer_settings")

/** Namespaces for generically persisted values, kept out of the named key space. */
private const val TOGGLE_PREFIX = "toggle."
private const val CHOICE_PREFIX = "choice."

class ReplicaSettingsRepository(private val context: Context) {
    private object Keys {
        val showPlatformIcons = booleanPreferencesKey("show_platform_icons")
        val showBots = booleanPreferencesKey("show_bots")
        val showCommands = booleanPreferencesKey("show_commands")
        val showUserCount = booleanPreferencesKey("show_user_count")
        val chatFontScale = intPreferencesKey("chat_font_scale")
        val alertDashboardScale = intPreferencesKey("alert_dashboard_scale")
        val cellularEnabled = booleanPreferencesKey("cellular_enabled")
        val wifiEnabled = booleanPreferencesKey("wifi_enabled")
        val ethernetEnabled = booleanPreferencesKey("ethernet_enabled")
        val cellularWeight = intPreferencesKey("cellular_weight")
        val wifiWeight = intPreferencesKey("wifi_weight")
        val ethernetWeight = intPreferencesKey("ethernet_weight")
        val bitrateMatchesResolution = booleanPreferencesKey("bitrate_matches_resolution")
        val h264BitrateKbps = intPreferencesKey("h264_bitrate_kbps")
        val audioInputGainDb = floatPreferencesKey("audio_input_gain_db")
        val recordStream = booleanPreferencesKey("record_stream")
        val splitSections = booleanPreferencesKey("split_sections")
        val audioMeterVisible = booleanPreferencesKey("audio_meter_visible")
        val gridVisible = booleanPreferencesKey("grid_visible")
        val safeMarginsVisible = booleanPreferencesKey("safe_margins_visible")
        val safeMarginIndentPercent = intPreferencesKey("safe_margin_indent_percent")
        val safeMarginRatios = stringSetPreferencesKey("safe_margin_ratios")
        val timestampActive = booleanPreferencesKey("timestamp_active")
        val webOverlayMaster = booleanPreferencesKey("web_overlay_master")
    }

    val settings: Flow<ReplicaSettings> = context.replicaDataStore.data.map { preferences ->
        ReplicaSettings(
            showPlatformIcons = preferences[Keys.showPlatformIcons] ?: false,
            showBots = preferences[Keys.showBots] ?: false,
            showCommands = preferences[Keys.showCommands] ?: false,
            showUserCount = preferences[Keys.showUserCount] ?: true,
            chatFontScale = preferences[Keys.chatFontScale] ?: 220,
            alertDashboardScale = preferences[Keys.alertDashboardScale] ?: 280,
            cellularEnabled = preferences[Keys.cellularEnabled] ?: true,
            wifiEnabled = preferences[Keys.wifiEnabled] ?: true,
            ethernetEnabled = preferences[Keys.ethernetEnabled] ?: true,
            cellularWeight = preferences[Keys.cellularWeight] ?: 100,
            wifiWeight = preferences[Keys.wifiWeight] ?: 100,
            ethernetWeight = preferences[Keys.ethernetWeight] ?: 100,
            bitrateMatchesResolution = preferences[Keys.bitrateMatchesResolution] ?: true,
            h264BitrateKbps = preferences[Keys.h264BitrateKbps] ?: 6000,
            audioInputGainDb = preferences[Keys.audioInputGainDb] ?: 0f,
            recordStream = preferences[Keys.recordStream] ?: false,
            splitSections = preferences[Keys.splitSections] ?: true,
            audioMeterVisible = preferences[Keys.audioMeterVisible] ?: true,
            gridVisible = preferences[Keys.gridVisible] ?: false,
            safeMarginsVisible = preferences[Keys.safeMarginsVisible] ?: false,
            safeMarginIndentPercent = preferences[Keys.safeMarginIndentPercent] ?: 5,
            safeMarginRatios = preferences[Keys.safeMarginRatios] ?: setOf("16:9 (1.78)"),
            timestampActive = preferences[Keys.timestampActive] ?: false,
            webOverlayMaster = preferences[Keys.webOverlayMaster] ?: true,
            extraToggles = preferences.asMap()
                .filterKeys { it.name.startsWith(TOGGLE_PREFIX) }
                .entries
                .mapNotNull { (key, value) ->
                    (value as? Boolean)?.let { key.name.removePrefix(TOGGLE_PREFIX) to it }
                }
                .toMap(),
            choiceValues = preferences.asMap()
                .filterKeys { it.name.startsWith(CHOICE_PREFIX) }
                .entries
                .mapNotNull { (key, value) ->
                    (value as? String)?.let { key.name.removePrefix(CHOICE_PREFIX) to it }
                }
                .toMap(),
        )
    }

    suspend fun setBoolean(key: String, value: Boolean) = context.replicaDataStore.edit { preferences ->
        preferences[booleanKey(key)] = value
    }

    suspend fun setInt(key: String, value: Int) = context.replicaDataStore.edit { preferences ->
        preferences[intKey(key)] = value
    }

    suspend fun setFloat(key: String, value: Float) = context.replicaDataStore.edit { preferences ->
        preferences[floatKey(key)] = value
    }

    suspend fun setStringSet(key: String, value: Set<String>) = context.replicaDataStore.edit { preferences ->
        preferences[stringSetKey(key)] = value
    }

    /** Persist a toggle the settings model has no named field for. */
    suspend fun setExtraToggle(key: String, value: Boolean) = context.replicaDataStore.edit { preferences ->
        preferences[booleanPreferencesKey("$TOGGLE_PREFIX$key")] = value
    }

    /** Persist a single-choice or text value, keyed by dialog id. */
    suspend fun setChoiceValue(id: String, value: String) = context.replicaDataStore.edit { preferences ->
        preferences[stringPreferencesKey("$CHOICE_PREFIX$id")] = value
    }

    suspend fun reset() = context.replicaDataStore.edit { it.clear() }

    private fun booleanKey(key: String): Preferences.Key<Boolean> = when (key) {
        "show_platform_icons" -> Keys.showPlatformIcons
        "show_bots" -> Keys.showBots
        "show_commands" -> Keys.showCommands
        "show_user_count" -> Keys.showUserCount
        "cellular_enabled" -> Keys.cellularEnabled
        "wifi_enabled" -> Keys.wifiEnabled
        "ethernet_enabled" -> Keys.ethernetEnabled
        "bitrate_matches_resolution" -> Keys.bitrateMatchesResolution
        "record_stream" -> Keys.recordStream
        "split_sections" -> Keys.splitSections
        "audio_meter_visible" -> Keys.audioMeterVisible
        "grid_visible" -> Keys.gridVisible
        "safe_margins_visible" -> Keys.safeMarginsVisible
        "timestamp_active" -> Keys.timestampActive
        "web_overlay_master" -> Keys.webOverlayMaster
        else -> error("Unknown boolean preference: $key")
    }

    private fun intKey(key: String): Preferences.Key<Int> = when (key) {
        "chat_font_scale" -> Keys.chatFontScale
        "alert_dashboard_scale" -> Keys.alertDashboardScale
        "cellular_weight" -> Keys.cellularWeight
        "wifi_weight" -> Keys.wifiWeight
        "ethernet_weight" -> Keys.ethernetWeight
        "h264_bitrate_kbps" -> Keys.h264BitrateKbps
        "safe_margin_indent_percent" -> Keys.safeMarginIndentPercent
        else -> error("Unknown integer preference: $key")
    }

    private fun floatKey(key: String): Preferences.Key<Float> = when (key) {
        "audio_input_gain_db" -> Keys.audioInputGainDb
        else -> error("Unknown float preference: $key")
    }

    private fun stringSetKey(key: String): Preferences.Key<Set<String>> = when (key) {
        "safe_margin_ratios" -> Keys.safeMarginRatios
        else -> error("Unknown string-set preference: $key")
    }
}
