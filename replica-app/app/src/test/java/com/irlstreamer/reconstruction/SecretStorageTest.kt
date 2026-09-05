package com.irlstreamer.reconstruction

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.irlstreamer.reconstruction.data.ReplicaSettingsRepository
import com.irlstreamer.reconstruction.data.isProtected
import com.irlstreamer.reconstruction.data.protect
import com.irlstreamer.reconstruction.model.DialogType
import com.irlstreamer.reconstruction.model.OutgoingConnection
import com.irlstreamer.reconstruction.model.SECRET_CHOICE_IDS
import com.irlstreamer.reconstruction.model.SettingsPage
import com.irlstreamer.reconstruction.model.encodeConnections
import com.irlstreamer.reconstruction.ui.components.isSecret
import com.irlstreamer.reconstruction.ui.settings.SettingAction
import com.irlstreamer.reconstruction.ui.settings.SettingItem
import com.irlstreamer.reconstruction.ui.settings.SettingsCatalog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * An RTMP or SRT URL carries the stream key, and the key is the whole
 * credential: anyone holding it can broadcast to the channel. It used to sit in
 * the settings file as typed.
 */
class SecretStorageTest {

    private val streamKey = "sk_live_9f2c4a7be1"
    private val url = "rtmp://ingest.example.com/live/$streamKey"

    /** Everything the store holds, as strings, which is where a leak would show. */
    private suspend fun storedText(store: InMemoryPreferencesDataStore): List<String> =
        store.data.first().asMap().values.map { it.toString() }

    @Test
    fun aSavedStreamKeyIsNotInTheStoreInTheClear() = runTest {
        val store = InMemoryPreferencesDataStore()
        val repository = ReplicaSettingsRepository(store, FakeSecretCipher())

        repository.setConnection("Twitch", url)

        val stored = storedText(store)
        assertFalse("the key is on disk as typed: $stored", stored.any { it.contains(streamKey) })
        assertFalse("the URL is on disk as typed: $stored", stored.any { it.contains(url) })
    }

    @Test
    fun aSavedDestinationStillReadsBackAsTheUserTypedIt() = runTest {
        val store = InMemoryPreferencesDataStore()
        val repository = ReplicaSettingsRepository(store, FakeSecretCipher())

        repository.setConnection("Twitch", url)

        val settings = repository.settings.first()
        assertEquals(url, settings.connectionUrl)
        assertEquals(listOf(OutgoingConnection("Twitch", url)), settings.connections)
    }

    @Test
    fun deletingOneDestinationDoesNotRewriteTheOthersInTheClear() = runTest {
        // The delete path reads the list, drops one and writes the rest back.
        // Reading the decrypted form there would put every remaining key on disk.
        val store = InMemoryPreferencesDataStore()
        val repository = ReplicaSettingsRepository(store, FakeSecretCipher())
        repository.setConnection("Twitch", url)
        repository.setConnection("Backup", "rtmp://backup.example.com/live/sk_backup_1")

        repository.deleteConnection("Backup")

        val stored = storedText(store)
        assertFalse("a surviving key was rewritten in the clear: $stored", stored.any { it.contains(streamKey) })
        assertEquals(url, repository.settings.first().connectionUrl)
    }

    @Test
    fun aDashboardApiKeyIsStoredEncryptedAndReadsBack() = runTest {
        val store = InMemoryPreferencesDataStore()
        val repository = ReplicaSettingsRepository(store, FakeSecretCipher())

        repository.setChoiceValue("dashboard_a_key", "sl_token_4471")

        assertFalse(
            "the API key is on disk as typed",
            storedText(store).any { it.contains("sl_token_4471") },
        )
        assertEquals("sl_token_4471", repository.settings.first().choiceValues["dashboard_a_key"])
    }

    @Test
    fun anOrdinarySettingIsNotEncrypted() = runTest {
        // Encrypting everything would cost nothing on disk and a great deal in
        // debuggability, and it would hide the settings a support request needs.
        val store = InMemoryPreferencesDataStore()
        val cipher = FakeSecretCipher()
        val repository = ReplicaSettingsRepository(store, cipher)

        repository.setChoiceValue("resolution", "1280x720 (16:9)")

        assertEquals(0, cipher.encryptCalls)
        assertTrue(storedText(store).any { it == "1280x720 (16:9)" })
    }

    @Test
    fun aDeviceThatWillNotEncryptSavesNothing() = runTest {
        // Falling back to plaintext would mean the one device that most needs
        // the protection is the one that silently does not get it.
        val store = InMemoryPreferencesDataStore()
        val repository = ReplicaSettingsRepository(store, FakeSecretCipher(refuse = true))

        val saved = repository.setConnection("Twitch", url)

        assertFalse("the save reported success", saved)
        assertEquals("", repository.settings.first().connectionUrl)
        assertTrue(storedText(store).none { it.contains(streamKey) })
    }

    @Test
    fun aDestinationSavedBeforeThisBuildStillOpens() = runTest {
        // Upgrade path: the stored list is plaintext, written by a build with no
        // cipher. Refusing it would lose the user's destination on update.
        val store = InMemoryPreferencesDataStore()
        store.edit { preferences ->
            preferences[stringPreferencesKey("connections")] =
                encodeConnections(listOf(OutgoingConnection("Twitch", url)))
            preferences[stringPreferencesKey("active_connection")] = "Twitch"
        }
        val repository = ReplicaSettingsRepository(store, FakeSecretCipher())

        assertEquals(url, repository.settings.first().connectionUrl)
    }

    @Test
    fun savingAgainProtectsADestinationThatWasStoredInTheClear() = runTest {
        val store = InMemoryPreferencesDataStore()
        store.edit { preferences ->
            preferences[stringPreferencesKey("connections")] =
                encodeConnections(listOf(OutgoingConnection("Twitch", url)))
            preferences[stringPreferencesKey("active_connection")] = "Twitch"
        }
        val repository = ReplicaSettingsRepository(store, FakeSecretCipher())

        repository.setConnection("Twitch", url)

        assertFalse(storedText(store).any { it.contains(streamKey) })
    }

    @Test
    fun anAlreadyProtectedValueIsNotEncryptedTwice() {
        // Double-wrapping would still decrypt to something, just not to the URL,
        // and the failure would only appear when the broadcast could not connect.
        val cipher = FakeSecretCipher()
        val once = protect(url, cipher)!!

        assertTrue(isProtected(once))
        assertEquals(once, protect(once, cipher))
    }

    @Test
    fun everySecretTextFieldInTheCatalogIsOnTheEncryptedList() {
        // The list is written by hand, so this is what stops a new API key field
        // from being stored in the clear because nobody remembered to add it.
        val secretTextIds = SettingsPage.entries
            .flatMap { page -> runCatching { SettingsCatalog.page(page).items }.getOrDefault(emptyList()) }
            .filterIsInstance<SettingItem.Row>()
            .mapNotNull { (it.action as? SettingAction.Dialog)?.request }
            .filter { it.type == DialogType.TEXT && isSecret(it.title) }
            .map { it.id }
            .toSet()

        assertEquals(
            "the encrypted-id list and the catalog's secret text fields disagree",
            secretTextIds,
            SECRET_CHOICE_IDS,
        )
    }
}
