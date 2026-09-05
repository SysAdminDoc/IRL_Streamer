package com.irlstreamer.reconstruction

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.irlstreamer.reconstruction.data.ReplicaSettingsRepository
import com.irlstreamer.reconstruction.diagnostics.DiagnosticsLog
import com.irlstreamer.reconstruction.diagnostics.installCrashReporter
import com.irlstreamer.reconstruction.engine.SimulatedBroadcastEngine
import com.irlstreamer.reconstruction.engine.StreamPackBroadcastEngine
import com.irlstreamer.reconstruction.model.AppRoute
import com.irlstreamer.reconstruction.ui.ReplicaApp
import com.irlstreamer.reconstruction.ui.theme.IrlStreamerTheme

class MainActivity : ComponentActivity() {
    private lateinit var mainViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        // A capture run must not open the camera or reach the network: the
        // harness drives audited states, and its screenshots have to be
        // reproducible. An instrumented run must not either - it would grab the
        // camera from whatever else holds it and make results device-dependent.
        val engine = {
            if (shouldSimulateBroadcast(isCaptureLaunch(intent), isUnderInstrumentation())) {
                SimulatedBroadcastEngine()
            } else {
                StreamPackBroadcastEngine(applicationContext)
            }
        }
        val log = DiagnosticsLog()
        // A crash in the field leaves nothing behind otherwise: no store collects
        // reports and logcat is gone once the phone is unplugged.
        installCrashReporter(applicationContext, log)
        val factory = MainViewModel.Factory(ReplicaSettingsRepository(applicationContext), engine, log)
        mainViewModel = androidx.lifecycle.ViewModelProvider(this, factory)[MainViewModel::class.java]
        handleDebugIntent(intent)

        setContent {
            val vm: MainViewModel = viewModel(factory = factory)
            val state by vm.uiState.collectAsStateWithLifecycle()
            val liveConsole = state.runtime.route is AppRoute.LiveConsole

            LaunchedEffect(liveConsole) {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
                controller.show(WindowInsetsCompat.Type.navigationBars())
                if (liveConsole) {
                    controller.hide(WindowInsetsCompat.Type.statusBars())
                } else {
                    controller.show(WindowInsetsCompat.Type.statusBars())
                }
            }

            IrlStreamerTheme {
                ReplicaApp(
                    state = state,
                    viewModel = vm,
                    moveTaskToBackground = { moveTaskToBack(true) },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDebugIntent(intent)
    }

    /**
     * True when this process is running under an instrumentation test.
     *
     * The runner class only exists on an androidTest classpath, so its presence
     * is the signal. Nothing else in the app depends on the test libraries.
     */
    private fun isUnderInstrumentation(): Boolean =
        runCatching { Class.forName("androidx.test.platform.app.InstrumentationRegistry") }.isSuccess

    /** True when the debug harness launched us to render one audited state. */
    private fun isCaptureLaunch(intent: Intent?): Boolean {
        if (!BuildConfig.ENABLE_DEBUG_STATE_SELECTOR || intent == null) return false
        return intent.hasExtra("screen_id") || intent.hasExtra("replica_state") ||
            intent.data?.getQueryParameter("screen_id") != null ||
            intent.data?.getQueryParameter("replica_state") != null
    }

    private fun handleDebugIntent(intent: Intent?) {
        if (!BuildConfig.ENABLE_DEBUG_STATE_SELECTOR || intent == null) return
        val stateName = intent.getStringExtra("replica_state")
            ?: intent.data?.getQueryParameter("replica_state")
        val screenId = intent.getStringExtra("screen_id")
            ?: intent.data?.getQueryParameter("screen_id")
            ?: intent.data?.lastPathSegment?.takeIf { Regex("^\\d{3}").containsMatchIn(it) }
        when {
            !screenId.isNullOrBlank() -> mainViewModel.applyDebugScreen(screenId)
            !stateName.isNullOrBlank() -> mainViewModel.applyNamedState(stateName)
        }
    }
}

/**
 * Whether the console should drive the simulation rather than real capture.
 *
 * Two runs must never touch the camera or the network: the 145-state capture
 * sweep, whose screenshots have to be reproducible, and an instrumented test,
 * which would otherwise take the camera from whatever else holds it and make
 * its results depend on the device it ran on.
 */
internal fun shouldSimulateBroadcast(isCaptureLaunch: Boolean, isUnderInstrumentation: Boolean): Boolean =
    isCaptureLaunch || isUnderInstrumentation
