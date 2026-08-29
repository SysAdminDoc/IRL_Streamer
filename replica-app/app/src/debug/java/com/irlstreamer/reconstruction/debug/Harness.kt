package com.irlstreamer.reconstruction.debug

import android.content.Context
import com.irlstreamer.reconstruction.model.RuntimeUiState

/**
 * Debug builds resolve the 145 audited capture states. The release source set
 * provides the same object wired to [NoHarness].
 */
object Harness {
    val overrides: HarnessOverrides = AuditHarness
}

private object AuditHarness : HarnessOverrides {
    override fun screenState(screenId: String): RuntimeUiState = DebugStateCatalog.resolve(screenId)

    override fun namedState(name: String): RuntimeUiState = DebugStateCatalog.resolveNamedState(name)

    override fun scrollAnchorLabel(context: Context, screenId: String?): String? =
        AuditScrollAnchors.labelFor(context, screenId)
}
