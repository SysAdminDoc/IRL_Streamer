package com.irlstreamer.reconstruction

import com.irlstreamer.reconstruction.data.ReplicaSettingsRepository
import com.irlstreamer.reconstruction.engine.BroadcastEngine
import com.irlstreamer.reconstruction.engine.BroadcastFailure
import com.irlstreamer.reconstruction.engine.BroadcastRequest
import com.irlstreamer.reconstruction.engine.BroadcastResult
import com.irlstreamer.reconstruction.engine.BroadcastState
import com.irlstreamer.reconstruction.engine.BroadcastStatistics
import com.irlstreamer.reconstruction.model.ReplicaSettings
import com.irlstreamer.reconstruction.model.SettingsPage
import com.irlstreamer.reconstruction.ui.settings.SettingItem
import com.irlstreamer.reconstruction.ui.settings.SettingsCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Start used to refuse unconditionally because nothing could be configured.
 * The saved destination is what it now hands the engine.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionDestinationTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun useTestDispatcher() = Dispatchers.setMain(dispatcher)

    @After
    fun releaseDispatcher() = Dispatchers.resetMain()

    /** Records what the console asked for, and always accepts. */
    private class RecordingEngine : BroadcastEngine {
        var request: BroadcastRequest? = null
        private val _state = MutableStateFlow(BroadcastState.IDLE)
        override val state: StateFlow<BroadcastState> = _state.asStateFlow()
        private val _statistics = MutableStateFlow(BroadcastStatistics())
        override val statistics: StateFlow<BroadcastStatistics> = _statistics.asStateFlow()
        override val failure: StateFlow<BroadcastFailure?> = MutableStateFlow(null)

        override suspend fun start(request: BroadcastRequest): BroadcastResult {
            this.request = request
            _state.value = BroadcastState.LIVE
            return BroadcastResult.Started
        }

        override suspend fun stop() {
            _state.value = BroadcastState.IDLE
        }

        override fun release() = Unit
    }

    @Test
    fun theSavedConnectionUrlIsWhatTheEngineIsAskedToPublishTo() = runTest(dispatcher) {
        val repository = ReplicaSettingsRepository(InMemoryPreferencesDataStore())
        val engine = RecordingEngine()
        val viewModel = MainViewModel(repository, engine)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.saveConnection("  Local fixture  ", "  rtmp://10.0.2.2/live/key  ")
        advanceUntilIdle()

        val settings = repository.settings.first()
        assertEquals("Local fixture", settings.connectionName)
        assertEquals("rtmp://10.0.2.2/live/key", settings.connectionUrl)

        viewModel.startBroadcast()
        advanceUntilIdle()
        assertEquals("rtmp://10.0.2.2/live/key", engine.request?.connectionName)
    }

    @Test
    fun withNothingSavedStartStillRaisesTheAuditedGuard() = runTest(dispatcher) {
        val repository = ReplicaSettingsRepository(InMemoryPreferencesDataStore())
        val viewModel = MainViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.startBroadcast()
        advanceUntilIdle()

        assertEquals("no_connection", viewModel.uiState.value.runtime.dialog?.id)
    }

    @Test
    fun theConnectionsPageListsTheSavedDestination() {
        val empty = SettingsCatalog.connectionsPage(ReplicaSettings())
        assertNull(
            empty.items.filterIsInstance<SettingItem.Row>().firstOrNull { it.id == "saved_connection" },
        )
        assertEquals(3, empty.items.size)

        val saved = SettingsCatalog.connectionsPage(
            ReplicaSettings(connectionName = "Local fixture", connectionUrl = "rtmp://10.0.2.2/live/key"),
        )
        val row = saved.items.filterIsInstance<SettingItem.Row>().single { it.id == "saved_connection" }
        assertEquals("Local fixture", row.title)
        assertTrue(row.summary.contains("rtmp://10.0.2.2/live/key"))
    }
}
