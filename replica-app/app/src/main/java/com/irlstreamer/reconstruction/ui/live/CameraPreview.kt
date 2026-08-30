package com.irlstreamer.reconstruction.ui.live

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.irlstreamer.reconstruction.MainViewModel
import com.irlstreamer.reconstruction.ui.theme.AuditColors
import io.github.thibaultbee.streampack.compose.SourcePreview

/**
 * Live camera preview for the console.
 *
 * The broadcast engine owns the camera so preview and stream share one capture
 * session; this composable shows what the engine opened. Permission is requested
 * on first composition, and a refusal leaves the console usable on a black
 * surface with a plain explanation and a retry.
 */
@Composable
fun CameraPreview(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var askedOnce by remember { mutableStateOf(false) }
    // Both at once: the console is a broadcaster, and a streamer that opens
    // without a microphone cannot gain one later without reopening the camera.
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        askedOnce = true
        granted = result[Manifest.permission.CAMERA] ?: granted
        if (result[Manifest.permission.RECORD_AUDIO] == false) {
            viewModel.showToast("Microphone refused, so the broadcast will be silent")
        }
    }

    LaunchedEffect(Unit) {
        val wanted = buildList {
            if (!granted) add(Manifest.permission.CAMERA)
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.RECORD_AUDIO)
            }
        }
        if (wanted.isNotEmpty()) permissionLauncher.launch(wanted.toTypedArray())
    }

    if (!granted) {
        PermissionSurface(
            modifier = modifier,
            onRequest = {
                val activity = context as? Activity
                val canPrompt = activity == null || !askedOnce ||
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
                if (canPrompt) {
                    permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
                } else {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            },
        )
        return
    }

    // The camera follows the console's lifecycle. Android revokes it from a
    // backgrounded app anyway, and holding a dead streamer showed a black
    // preview that Retry could not recover.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, granted) {
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            viewModel.openCamera()
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.openCamera()
                Lifecycle.Event.ON_STOP -> viewModel.releaseCamera()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val source by viewModel.videoSource.collectAsStateWithLifecycle()
    val failure by viewModel.cameraFailure.collectAsStateWithLifecycle()

    // A failure is a state the console can show and retry, not a toast that
    // scrolls away leaving a black rectangle (the complaint IRL Pro reviews make).
    failure?.let { message ->
        FailureSurface(message = message, modifier = modifier, onRetry = viewModel::retryCamera)
        return
    }

    source?.let { videoSource ->
        SourcePreview(
            videoSource = videoSource,
            modifier = modifier.testTag("camera_preview"),
        )
    }
}

/** What the console shows when the camera will not start, with a way out. */
@Composable
private fun FailureSurface(message: String, modifier: Modifier, onRetry: () -> Unit) {
    Column(
        modifier = modifier
            .background(Color.Black)
            .padding(horizontal = 48.dp)
            .testTag("camera_failure_surface"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            color = Color(0xFFBBBBBB),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onRetry, modifier = Modifier.testTag("camera_retry")) {
            Text("RETRY", color = AuditColors.Accent, fontSize = 14.sp)
        }
    }
}

@Composable
private fun PermissionSurface(modifier: Modifier, onRequest: () -> Unit) {
    Column(
        modifier = modifier
            .background(Color.Black)
            .padding(horizontal = 48.dp)
            .testTag("camera_permission_surface"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Camera access is needed to show the preview.",
            color = Color(0xFFBBBBBB),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onRequest) {
            Text("ALLOW CAMERA", color = AuditColors.Accent, fontSize = 14.sp)
        }
    }
}
