package com.irlstreamer.reconstruction.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.irlstreamer.reconstruction.model.OutgoingConnection
import com.irlstreamer.reconstruction.model.ReplicaSettings
import com.irlstreamer.reconstruction.model.activeOrFirst
import com.irlstreamer.reconstruction.model.decodeConnections
import com.irlstreamer.reconstruction.model.encodeConnections
import com.irlstreamer.reconstruction.model.isSecretChoiceId
import com.irlstreamer.reconstruction.model.removeNamed
import com.irlstreamer.reconstruction.model.upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Name of the settings store.
 *
 * DataStore writes it to `filesDir/datastore/$SETTINGS_DATASTORE_NAME.preferences_pb`,
 * and the connection URL it holds carries the stream key. The backup rules
 * exclude that exact path, so the name is shared rather than repeated as a
 * literal - renaming the store must break the test that ties the two together.
 */
internal const val SETTINGS_DATASTORE_NAME = "irl_streamer_settings"

/**
 * The directory Auto Backup must exclude, relative to the `file` domain.
 *
 * The whole directory rather than the one file: DataStore writes an update to a
 * scratch file beside the real one and renames it, so a rule naming only the
 * final filename leaves a window where a second copy of the key is not covered.
 */
internal const val SETTINGS_DATASTORE_DIRECTORY = "datastore"

/** The file itself, for messages that need to name it. */
internal const val SETTINGS_DATASTORE_BACKUP_PATH = "$SETTINGS_DATASTORE_DIRECTORY/$SETTINGS_DATASTORE_NAME.preferences_pb"

private val Context.replicaDataStore by preferencesDataStore(name = SETTINGS_DATASTORE_NAME)

/** Namespaces for generically persisted values, kept out of the named key space. */
private const val TOGGLE_PREFIX = "toggle."
private const val CHOICE_PREFIX = "choice."

class ReplicaSettingsRepository internal constructor(
    private val dataStore: DataStore<Preferences>,
    /**
     * Protects the values that would let someone else broadcast to the user's
     * channel. Injected because the AndroidKeyStore does not exist off-device,
     * and a default that quietly degraded to plaintext there would be a default
     * that could ship.
     */
    private val secrets: SecretCipher,
) {
    constructor(context: Context) : this(context.replicaDataStore, KeystoreSecretCipher())

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
        val connectionName = stringPreferencesKey("connection_name")
        val connectionUrl = stringPreferencesKey("connection_url")
        val connections = stringPreferencesKey("connections")
        val activeConnection = stringPreferencesKey("active_connection")
        val updateCheckEnabled = booleanPreferencesKey("update_check_enabled")
        val updateLastCheck = longPreferencesKey("update_last_check_millis")
        val updateLatestTag = stringPreferencesKey("update_latest_tag")
    }

    val settings: Flow<ReplicaSettings> = dataStore.data.map { preferences ->
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
            connectionName = activeConnectionIn(preferences)?.name ?: "",
            connectionUrl = activeConnectionIn(preferences)?.url ?: "",
            connections = savedConnectionsIn(preferences),
            updateCheckEnabled = preferences[Keys.updateCheckEnabled] ?: true,
            updateLastCheckMillis = preferences[Keys.updateLastCheck] ?: 0L,
            updateLatestTag = preferences[Keys.updateLatestTag] ?: "",
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
                    (value as? String)?.let { stored ->
                        val id = key.name.removePrefix(CHOICE_PREFIX)
                        id to if (isSecretChoiceId(id)) reveal(stored, secrets) else stored
                    }
                }
                .toMap(),
        )
    }

    suspend fun setBoolean(key: String, value: Boolean) = dataStore.edit { preferences ->
        preferences[booleanKey(key)] = value
    }

    suspend fun setInt(key: String, value: Int) = dataStore.edit { preferences ->
        preferences[intKey(key)] = value
    }

    suspend fun setFloat(key: String, value: Float) = dataStore.edit { preferences ->
        preferences[floatKey(key)] = value
    }

    suspend fun setStringSet(key: String, value: Set<String>) = dataStore.edit { preferences ->
        preferences[stringSetKey(key)] = value
    }

    /** Persist a toggle the settings model has no named field for. */
    suspend fun setExtraToggle(key: String, value: Boolean) = dataStore.edit { preferences ->
        preferences[booleanPreferencesKey("$TOGGLE_PREFIX$key")] = value
    }

    /**
     * Persist a single-choice or text value, keyed by dialog id.
     *
     * @return false when the value is a credential the device would not encrypt.
     * Nothing is written in that case: a dashboard key belongs on disk protected
     * or not at all.
     */
    suspend fun setChoiceValue(id: String, value: String): Boolean {
        val stored = if (isSecretChoiceId(id)) protect(value, secrets) ?: return false else value
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("$CHOICE_PREFIX$id")] = stored
        }
        return true
    }

    /**
     * Saves a destination and makes it the active one.
     *
     * Saving the same name edits that entry rather than adding a second, which
     * is what stops the form's edit path from leaving duplicates. A second
     * destination used to overwrite the first with no warning.
     */
    suspend fun setConnection(name: String, url: String): Boolean {
        // Before the transaction: a destination whose key cannot be protected is
        // not saved at all, and finding that out mid-edit would leave the list
        // half written.
        val storedUrl = protect(url.trim(), secrets) ?: return false
        dataStore.edit { preferences ->
            val saved = storedConnectionsIn(preferences)
                .upsert(OutgoingConnection(name.trim(), storedUrl))
            preferences[Keys.connections] = encodeConnections(saved)
            preferences[Keys.activeConnection] = name.trim()
            // The legacy single-destination keys are no longer read; clearing them
            // stops a stale pair from reappearing if the list is ever emptied.
            preferences.remove(Keys.connectionName)
            preferences.remove(Keys.connectionUrl)
        }
        return true
    }

    /** Records what the last release check found, and when it ran. */
    suspend fun recordUpdateCheck(latestTag: String, checkedAtMillis: Long) = dataStore.edit { preferences ->
        preferences[Keys.updateLastCheck] = checkedAtMillis
        preferences[Keys.updateLatestTag] = latestTag
    }

    /** Marks an already-saved destination as the one to broadcast to. */
    suspend fun setActiveConnection(name: String) = dataStore.edit { preferences ->
        preferences[Keys.activeConnection] = name.trim()
    }

    /** Forgets a destination. The active one falls back to whatever is left. */
    suspend fun deleteConnection(name: String) = dataStore.edit { preferences ->
        // The stored form, because this writes the list straight back: decrypting
        // first would put every remaining key on disk in the clear.
        val remaining = storedConnectionsIn(preferences).removeNamed(name)
        preferences[Keys.connections] = encodeConnections(remaining)
        if (preferences[Keys.activeConnection].orEmpty().equals(name, ignoreCase = true)) {
            preferences[Keys.activeConnection] = remaining.firstOrNull()?.name.orEmpty()
        }
        preferences.remove(Keys.connectionName)
        preferences.remove(Keys.connectionUrl)
    }

    /**
     * The saved list, migrating a destination stored under the old
     * single-connection keys so an existing install does not lose it.
     */
    private fun savedConnectionsIn(preferences: Preferences): List<OutgoingConnection> =
        storedConnectionsIn(preferences).map { it.copy(url = reveal(it.url, secrets)) }

    /**
     * The list exactly as it sits on disk, URLs still encrypted.
     *
     * Anything that writes the list back works from this. A destination saved
     * before this build, or by a build that could not protect it, is plaintext
     * here and [reveal] passes it through; it becomes ciphertext the next time
     * the user saves it.
     */
    private fun storedConnectionsIn(preferences: Preferences): List<OutgoingConnection> {
        preferences[Keys.connections]?.let { return decodeConnections(it) }
        val legacyName = preferences[Keys.connectionName].orEmpty()
        val legacyUrl = preferences[Keys.connectionUrl].orEmpty()
        return if (legacyName.isNotBlank() && legacyUrl.isNotBlank()) {
            listOf(OutgoingConnection(legacyName, legacyUrl))
        } else {
            emptyList()
        }
    }

    private fun activeConnectionIn(preferences: Preferences): OutgoingConnection? =
        savedConnectionsIn(preferences).activeOrFirst(preferences[Keys.activeConnection].orEmpty())

    /**
     * Clears every stored value and returns what was cleared.
     *
     * The caller keeps the snapshot so the user can undo: a reset used to be
     * silent and irreversible, with the confirm dialog as its only guard.
     */
    suspend fun reset(): Preferences {
        // One transaction: reading the snapshot separately let a concurrent write
        // land between the read and the clear, where undo could not recover it.
        var snapshot: Preferences = emptyPreferences()
        dataStore.edit { preferences ->
            val copy = mutablePreferencesOf()
            preferences.asMap().forEach { (key, value) ->
                @Suppress("UNCHECKED_CAST")
                copy[key as Preferences.Key<Any>] = value
            }
            snapshot = copy
            preferences.clear()
        }
        return snapshot
    }

    /** Puts back a snapshot taken by [reset], discarding anything written since. */
    suspend fun restore(snapshot: Preferences) = dataStore.edit { preferences ->
        preferences.clear()
        snapshot.asMap().forEach { (key, value) ->
            @Suppress("UNCHECKED_CAST")
            preferences[key as Preferences.Key<Any>] = value
        }
    }

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
