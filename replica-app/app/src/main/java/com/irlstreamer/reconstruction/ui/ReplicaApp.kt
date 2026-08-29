package com.irlstreamer.reconstruction.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.irlstreamer.reconstruction.MainViewModel
import com.irlstreamer.reconstruction.debug.Harness
import com.irlstreamer.reconstruction.model.AppRoute
import com.irlstreamer.reconstruction.model.AppUiState
import com.irlstreamer.reconstruction.ui.components.AuditedDialogHost
import com.irlstreamer.reconstruction.ui.live.LiveConsoleScreen
import com.irlstreamer.reconstruction.ui.settings.SettingsScreen

@Composable
fun ReplicaApp(
    state: AppUiState,
    viewModel: MainViewModel,
    moveTaskToBackground: () -> Unit,
) {
    val context = LocalContext.current
    // Acknowledge the chosen folder. Dropping the result made the audited
    // "Save to" row launch the system picker and then behave as though the user
    // had cancelled, whatever they chose.
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            viewModel.showToast("Recording destination set to ${uri.lastPathSegment ?: uri}")
        }
    }

    LaunchedEffect(state.runtime.requestFolderPicker) {
        if (state.runtime.requestFolderPicker) {
            viewModel.consumeFolderRequest()
            folderPicker.launch(null)
        }
    }

    val toastText = state.runtime.validationError ?: state.runtime.toastMessage
    LaunchedEffect(toastText) {
        if (!toastText.isNullOrBlank()) {
            Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
            viewModel.consumeToast()
        }
    }

    // A reset is real and irreversible once the offer lapses, so it gets an
    // action affordance rather than the plain toast every other event uses.
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.runtime.undoResetOffer) {
        if (state.runtime.undoResetOffer != null) {
            val result = snackbarHostState.showSnackbar(
                message = "Settings reset",
                actionLabel = "UNDO",
                withDismissAction = false,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoReset() else viewModel.consumeUndoReset()
        }
    }

    BackHandler {
        if (!viewModel.back()) moveTaskToBackground()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Audit evidence: all app surfaces begin after a 75 px / 26.67 dp left safe strip.
                .padding(
                    start = 26.67.dp,
                    // Target SDK 36 enforces edge-to-edge on the validation AVD. The audited
                    // settings surfaces retain a measured 75 px status-bar band (screen 002).
                    top = if (state.runtime.route is AppRoute.Settings) 26.67.dp else 0.dp,
                ),
        ) {
            when (state.runtime.route) {
                AppRoute.LiveConsole -> LiveConsoleScreen(state, viewModel)
                is AppRoute.Settings -> SettingsScreen(state, viewModel)
            }
        }

        state.runtime.dialog?.let { request ->
            // Restore the audited choice-list scroll offset for the *_menu_middle and
            // *_menu_lower captures, which show the same menu scrolled to a later option.
            val anchorLabel = Harness.overrides.scrollAnchorLabel(context, state.runtime.debugScreenId)
            val anchoredRequest = anchorLabel
                ?.let { label -> request.options.indexOf(label) }
                ?.takeIf { it > 0 }
                ?.let { request.copy(listAnchorIndex = it) }
                ?: request
            AuditedDialogHost(
                request = anchoredRequest,
                onDismiss = viewModel::dismissDialog,
                onConfirm = viewModel::confirmDialog,
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .testTag("undo_host"),
        )
    }
}
