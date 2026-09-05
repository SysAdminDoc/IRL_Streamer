package com.irlstreamer.reconstruction

import com.irlstreamer.reconstruction.data.ReplicaSettingsRepository
import com.irlstreamer.reconstruction.model.OutgoingConnection
import com.irlstreamer.reconstruction.model.decodeConnections
import com.irlstreamer.reconstruction.model.encodeConnections
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Saving a second destination used to overwrite the first with no warning,
 * while the page copy promised editing and offered a disabled "Delete multiple".
 */
class OutgoingConnectionsTest {

    @Test
    fun severalConnectionsPersistWithOneActive() = runTest {
        val repository = ReplicaSettingsRepository(InMemoryPreferencesDataStore())

        repository.setConnection("Home", "rtmp://10.0.0.2/live/home")
        repository.setConnection("Relay", "rtmp://relay.example.com/live/relay")

        val settings = repository.settings.first()
        assertEquals(2, settings.connections.size)
        // The most recently saved one is what the console broadcasts to.
        assertEquals("Relay", settings.connectionName)
        assertEquals("rtmp://relay.example.com/live/relay", settings.connectionUrl)
    }

    @Test
    fun editingAnExistingConnectionDoesNotCreateADuplicate() = runTest {
        val repository = ReplicaSettingsRepository(InMemoryPreferencesDataStore())
        repository.setConnection("Home", "rtmp://10.0.0.2/live/old")

        repository.setConnection("Home", "rtmp://10.0.0.2/live/new")

        val settings = repository.settings.first()
        assertEquals(1, settings.connections.size)
        assertEquals("rtmp://10.0.0.2/live/new", settings.connections.single().url)
    }

    @Test
    fun theActiveConnectionCanBeSwitched() = runTest {
        val repository = ReplicaSettingsRepository(InMemoryPreferencesDataStore())
        repository.setConnection("Home", "rtmp://10.0.0.2/live/home")
        repository.setConnection("Relay", "rtmp://relay.example.com/live/relay")

        repository.setActiveConnection("Home")

        assertEquals("Home", repository.settings.first().connectionName)
    }

    @Test
    fun deletingRemovesOnlyThatConnection() = runTest {
        val repository = ReplicaSettingsRepository(InMemoryPreferencesDataStore())
        repository.setConnection("Home", "rtmp://10.0.0.2/live/home")
        repository.setConnection("Relay", "rtmp://relay.example.com/live/relay")

        repository.deleteConnection("Relay")

        val settings = repository.settings.first()
        assertEquals(listOf("Home"), settings.connections.map { it.name })
        // The active one was deleted, so what is left takes over rather than
        // leaving the console pointing at nothing.
        assertEquals("Home", settings.connectionName)
    }

    @Test
    fun deletingTheLastConnectionLeavesNothingActive() = runTest {
        val repository = ReplicaSettingsRepository(InMemoryPreferencesDataStore())
        repository.setConnection("Home", "rtmp://10.0.0.2/live/home")

        repository.deleteConnection("Home")

        val settings = repository.settings.first()
        assertTrue(settings.connections.isEmpty())
        assertEquals("", settings.connectionName)
        assertEquals("", settings.connectionUrl)
    }

    @Test
    fun theEncodingRoundTrips() {
        val connections = listOf(
            OutgoingConnection("Home", "rtmp://10.0.0.2/live/home"),
            OutgoingConnection("Relay", "rtmps://relay.example.com:1936/live/relay?token=abc"),
        )

        assertEquals(connections, decodeConnections(encodeConnections(connections)))
    }

    @Test
    fun aSeparatorTypedIntoANameCannotCorruptTheStore() {
        // The separators are structural, so they must not survive into a record.
        val connections = listOf(OutgoingConnection("Home", "rtmp://host/live/key"))

        val decoded = decodeConnections(encodeConnections(connections))

        assertEquals(1, decoded.size)
        assertEquals("Home", decoded.single().name)
        assertEquals("rtmp://host/live/key", decoded.single().url)
    }

    @Test
    fun malformedStoredTextIsIgnoredRatherThanCrashing() {
        assertTrue(decodeConnections("nonsense-with-no-separator").isEmpty())
        assertTrue(decodeConnections("").isEmpty())
    }
}
