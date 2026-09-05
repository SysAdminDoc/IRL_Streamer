package com.irlstreamer.reconstruction

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The instrumented suite used to launch without a capture extra, and any
 * non-capture launch picked the real engine, so the device tests opened the
 * camera and microphone and could reach the network. That made their results
 * depend on the device and on whatever else held the camera at the time.
 */
class EngineSelectionTest {

    @Test
    fun aCaptureRunSimulates() {
        // The 145-state sweep must not open the camera: its screenshots have to
        // be reproducible.
        assertTrue(shouldSimulateBroadcast(isCaptureLaunch = true, isUnderInstrumentation = false))
    }

    @Test
    fun anInstrumentedRunSimulatesEvenWithoutACaptureExtra() {
        // This is the case that shipped broken.
        assertTrue(shouldSimulateBroadcast(isCaptureLaunch = false, isUnderInstrumentation = true))
    }

    @Test
    fun anOrdinaryLaunchUsesTheRealEngine() {
        // A user starting the app gets real capture; simulating here would make
        // the app useless.
        assertFalse(shouldSimulateBroadcast(isCaptureLaunch = false, isUnderInstrumentation = false))
    }
}
