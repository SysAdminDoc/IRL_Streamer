package com.irlstreamer.reconstruction.debug

import android.content.Context
import com.irlstreamer.reconstruction.model.RuntimeUiState

/**
 * The seam the audit capture harness plugs into.
 *
 * Production surfaces used to reach straight into the capture catalog, so every
 * new screen had to remember the harness and the release build carried catalog
 * code it could never reach. Only the debug variant supplies a real
 * implementation; release gets [NoHarness] and the catalog is not compiled in.
 */
interface HarnessOverrides {
    /** The audited state for a capture id, or null when this build has no catalog. */
    fun screenState(screenId: String): RuntimeUiState?

    /** The audited state for a named fixture (`loading`, `empty`, ...). */
    fun namedState(name: String): RuntimeUiState?

    /** The audited first-visible-row label for a capture, used to restore its scroll. */
    fun scrollAnchorLabel(context: Context, screenId: String?): String?
}

/** What a release build gets: the app behaves as though no capture is running. */
object NoHarness : HarnessOverrides {
    override fun screenState(screenId: String): RuntimeUiState? = null

    override fun namedState(name: String): RuntimeUiState? = null

    override fun scrollAnchorLabel(context: Context, screenId: String?): String? = null
}
