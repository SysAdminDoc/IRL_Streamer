package com.irlstreamer.reconstruction

import androidx.compose.ui.unit.dp
import com.irlstreamer.reconstruction.ui.theme.AuditMetrics
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks the measured layout tokens to the pixel values recorded in the immutable
 * audit evidence (see `docs/measured-tokens.md`).
 *
 * These are observations, not preferences. A token drifting back towards a round
 * Material default is a regression, and the first reconstruction pass produced
 * exactly that class of error, so each token is asserted against the pixel value
 * it was derived from at the audited 450 dpi density.
 */
class AuditMetricsTest {
    private fun px(value: Float) = value / AuditMetrics.DENSITY_PX_PER_DP

    private fun assertPx(expectedPx: Float, actualDp: androidx.compose.ui.unit.Dp, label: String) {
        // Tokens are stored to 2 dp decimals, so allow half a pixel of rounding.
        assertEquals(label, expectedPx, actualDp.value * AuditMetrics.DENSITY_PX_PER_DP, 0.5f)
    }

    @Test
    fun displayInsetsMatchTheAuditedWindowContract() {
        assertPx(75f, AuditMetrics.LeftSafeInset, "left cutout safe inset")
        assertPx(135f, AuditMetrics.NavigationInset, "right navigation inset")
        assertPx(75f, AuditMetrics.SettingsStatusInset, "settings status band")
    }

    @Test
    fun dialogSurfaceMatchesTheDrawnPixels() {
        // The audit hierarchy reports a 1369 px window, but the drawn #424242
        // surface measures 1279 px: the AlertDialog carries a 45 px decor inset
        // per side. Taking the window width was the first pass's largest error.
        assertPx(1279f, AuditMetrics.Dialog.Width, "visible modal width")
        assertPx(907f, AuditMetrics.Dialog.MaxHeight, "max visible modal height")
        assertPx(83f, AuditMetrics.Dialog.SettingsTopRegionInset, "settings vertical region top")
    }

    @Test
    fun dialogInternalPaddingMatchesTheVisibleSurface() {
        assertPx(68f, AuditMetrics.Dialog.TitleHorizontalPadding, "title side inset")
        assertPx(51f, AuditMetrics.Dialog.TitleTopPadding, "title top offset")
        assertPx(76f, AuditMetrics.Dialog.TitleHeight, "title line box")
        assertPx(56f, AuditMetrics.Dialog.FieldStartInset, "field side inset")
        assertPx(127f, AuditMetrics.Dialog.FieldTopOffset, "field top offset")
        assertPx(135f, AuditMetrics.Dialog.FieldHeight, "field height")
        assertPx(150f, AuditMetrics.Dialog.ListTopOffset, "choice list top offset")
        assertPx(0f, AuditMetrics.Dialog.ListStartInset, "choice row side inset")
        assertPx(135f, AuditMetrics.Dialog.ListRowHeight, "choice row height")
        assertPx(135f, AuditMetrics.Dialog.ButtonHeight, "button row height")
        assertPx(34f, AuditMetrics.Dialog.ButtonEndGap, "button end gap")
        assertPx(11f, AuditMetrics.Dialog.ButtonBottomGap, "button bottom gap")
    }

    @Test
    fun settingsDialogCentresOnTheAuditedPoint() {
        // Modal centre x = 1127.5 px, the centre of the content area [75, 2181].
        val contentLeft = AuditMetrics.LeftSafeInset.value * AuditMetrics.DENSITY_PX_PER_DP
        val contentRight = 2316f - AuditMetrics.NavigationInset.value * AuditMetrics.DENSITY_PX_PER_DP
        assertEquals("modal centre x", 1127.5f, (contentLeft + contentRight) / 2f, 1.0f)

        // Modal centre y = 581.5 px, the centre of [83, 1080] on settings surfaces.
        val regionTop = AuditMetrics.Dialog.SettingsTopRegionInset.value * AuditMetrics.DENSITY_PX_PER_DP
        assertEquals("modal centre y", 581.5f, (regionTop + 1080f) / 2f, 1.0f)
    }

    @Test
    fun densityConversionMatchesTheAuditDevice() {
        assertEquals(2.8125f, AuditMetrics.DENSITY_PX_PER_DP, 0.0001f)
        assertEquals(384.0f, px(1080f), 0.01f)
        assertEquals(823.47f, px(2316f), 0.01f)
    }
}
