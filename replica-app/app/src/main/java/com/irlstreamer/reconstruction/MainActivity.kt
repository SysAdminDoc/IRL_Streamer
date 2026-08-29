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

        val factory = MainViewModel.Factory(
            ReplicaSettingsRepository(applicationContext),
            StreamPackBroadcastEngine(applicationContext),
        )
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
