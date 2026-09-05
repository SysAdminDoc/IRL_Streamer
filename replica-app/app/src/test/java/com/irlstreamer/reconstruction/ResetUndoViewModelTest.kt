package com.irlstreamer.reconstruction

import com.irlstreamer.reconstruction.data.ReplicaSettingsRepository
import com.irlstreamer.reconstruction.model.DialogRequest
import com.irlstreamer.reconstruction.model.DialogType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * The undo offer as the user meets it, through the view model rather than the
 * repository: the reset dialog, the offer, and Undo.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ResetUndoViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun useTestDispatcher() = Dispatchers.setMain(dispatcher)

    @After
    fun releaseDispatcher() = Dispatchers.resetMain()

    private val resetDialog = DialogRequest(
        id = "reset_settings",
        title = "Reset app settings",
        type = DialogType.ALERT,
    )

    @Test
    fun undoAfterAResetRestoresTheSettings() = runTest(dispatcher) {
        val repository = ReplicaSettingsRepository(InMemoryPreferencesDataStore(), FakeSecretCipher())
        repository.setInt("h264_bitrate_kbps", 4200)
        val viewModel = MainViewModel(repository)
        // uiState is WhileSubscribed: without a collector it never leaves its initial value.
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.showDialog(resetDialog)
        viewModel.confirmDialog()
        advanceUntilIdle()
        assertEquals(6000, repository.settings.first().h264BitrateKbps)
        assertNotNull("a reset must offer undo", viewModel.uiState.value.runtime.undoResetOffer)

        viewModel.undoReset()
        advanceUntilIdle()
        assertEquals(4200, repository.settings.first().h264BitrateKbps)
        assertNull(viewModel.uiState.value.runtime.undoResetOffer)
    }

    @Test
    fun aSecondResetKeepsTheOriginalSnapshotAndReoffersUndo() = runTest(dispatcher) {
        val repository = ReplicaSettingsRepository(InMemoryPreferencesDataStore(), FakeSecretCipher())
        repository.setInt("h264_bitrate_kbps", 4200)
        val viewModel = MainViewModel(repository)
        // uiState is WhileSubscribed: without a collector it never leaves its initial value.
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.showDialog(resetDialog)
        viewModel.confirmDialog()
        advanceUntilIdle()
        val firstOffer = viewModel.uiState.value.runtime.undoResetOffer

        // The second reset clears an already-empty store. Taking that as the
        // snapshot would make Undo restore nothing at all.
        viewModel.showDialog(resetDialog)
        viewModel.confirmDialog()
        advanceUntilIdle()
        val secondOffer = viewModel.uiState.value.runtime.undoResetOffer
        assertEquals(
            "a second reset must raise a new offer so the countdown restarts",
            true,
            secondOffer != null && secondOffer != firstOffer,
        )

        viewModel.undoReset()
        advanceUntilIdle()
        assertEquals(4200, repository.settings.first().h264BitrateKbps)
    }

    @Test
    fun lettingTheOfferLapseKeepsTheReset() = runTest(dispatcher) {
        val repository = ReplicaSettingsRepository(InMemoryPreferencesDataStore(), FakeSecretCipher())
        repository.setInt("h264_bitrate_kbps", 4200)
        val viewModel = MainViewModel(repository)
        // uiState is WhileSubscribed: without a collector it never leaves its initial value.
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.showDialog(resetDialog)
        viewModel.confirmDialog()
        advanceUntilIdle()

        viewModel.consumeUndoReset()
        viewModel.undoReset()
        advanceUntilIdle()

        assertEquals(6000, repository.settings.first().h264BitrateKbps)
        assertNull(viewModel.uiState.value.runtime.undoResetOffer)
    }
}
