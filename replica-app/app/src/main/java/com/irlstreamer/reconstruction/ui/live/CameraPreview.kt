package com.irlstreamer.reconstruction.ui.live

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.irlstreamer.reconstruction.ui.theme.AuditColors

private const val TAG = "CameraPreview"

/** The audited lens fixture exposes ids 1 and 3 as front lenses (D008). */
internal fun isFrontCamera(cameraId: Int): Boolean = cameraId == 1 || cameraId == 3

/**
 * Live camera preview for the console.
 *
 * Until v0.3.0 the console showed a static JPEG, so the app looked frozen the
 * moment it opened. This binds a CameraX [Preview] to the composition's
 * lifecycle and follows the console's selected lens: the fixture ids 1 and 3
 * map to the front facing, everything else to the back.
 *
 * Permission is requested on first composition. A refusal leaves the console
 * usable on a black surface with a plain explanation and a retry; once Android
 * stops showing the system prompt the retry opens the app's settings page.
 */
@Composable
fun CameraPreview(
    cameraId: Int,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var askedOnce by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        askedOnce = true
        granted = result
    }

    LaunchedEffect(Unit) {
        if (!granted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (!granted) {
        PermissionSurface(
            modifier = modifier,
            onRequest = {
                val activity = context as? Activity
                val canPrompt = activity == null || !askedOnce ||
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
                if (canPrompt) {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
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

    val previewView = remember {
        PreviewView(context).apply {
            // TextureView keeps the Compose overlays composited above the
            // camera frames on every device; SurfaceView punches a hole.
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    LaunchedEffect(cameraId) {
        val cameraProvider = provider ?: runCatching { ProcessCameraProvider.awaitInstance(context) }
            .onFailure {
                Log.e(TAG, "CameraX provider unavailable", it)
                onError("Camera unavailable: ${it.message ?: it.javaClass.simpleName}")
            }
            .getOrNull() ?: return@LaunchedEffect
        provider = cameraProvider

        val wanted = if (isFrontCamera(cameraId)) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        val selector = when {
            cameraProvider.hasCamera(wanted) -> wanted
            cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
            else -> {
                onError("This device reports no camera")
                return@LaunchedEffect
            }
        }
        if (selector != wanted) onError("Selected lens is not available; using the other camera")

        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
        runCatching {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
        }.onFailure {
            Log.e(TAG, "Camera bind failed", it)
            onError("Camera failed to start: ${it.message ?: it.javaClass.simpleName}")
        }
    }

    DisposableEffect(Unit) {
        onDispose { provider?.unbindAll() }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier.testTag("camera_preview"),
    )
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
