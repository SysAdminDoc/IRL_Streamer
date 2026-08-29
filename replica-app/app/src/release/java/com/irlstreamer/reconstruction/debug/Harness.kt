package com.irlstreamer.reconstruction.debug

/**
 * Release builds ship no capture catalog. The debug source set provides the
 * same object backed by `DebugStateCatalog`.
 */
object Harness {
    val overrides: HarnessOverrides = NoHarness
}
