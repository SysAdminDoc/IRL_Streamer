package com.irlstreamer.reconstruction.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.irlstreamer.reconstruction.MainViewModel
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
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { }

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
            AuditedDialogHost(
                request = request,
                onDismiss = viewModel::dismissDialog,
                onConfirm = viewModel::confirmDialog,
            )
        }
    }
}
