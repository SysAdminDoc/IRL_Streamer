package com.irlstreamer.reconstruction.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Measured layout tokens taken from the immutable audit UI-hierarchy evidence.
 *
 * Every value below was derived from the `app-audit/evidence/ui-xml` dumps by
 * `scripts/geometry_diff.py` and the dialog-metric sweep recorded in
 * `docs/measured-tokens.md`. The dialog values were identical (min == max)
 * across all 50 audited AlertDialog states, so they are exact observations
 * rather than approximations. Raw pixel values are retained beside the dp
 * conversion at the audited 450 dpi / 2.8125 px-per-dp density.
 */
object AuditMetrics {
    /** Audited reference viewport: 2316 x 1080 px landscape at 450 dpi. */
    const val DENSITY_PX_PER_DP = 2.8125f

    /** Display cutout / rounded-corner safe strip on the left in landscape (75 px). */
    val LeftSafeInset = 26.67.dp

    /** Samsung three-button navigation occupies the right edge (135 px). */
    val NavigationInset = 48.0.dp

    /** Status band retained on settings surfaces (75 px); hidden on the live console. */
    val SettingsStatusInset = 26.67.dp

    /**
     * AlertDialog geometry. Audit evidence: 50 dialog states, e.g. screen 055
     * modal_surface [443,327]-[1812,836]. All observations agreed exactly.
     */
    object Dialog {
        /**
         * Visible modal width 1279 px. The audit UI-hierarchy window node is 1369 px
         * wide because it includes a 45 px transparent AlertDialog decor inset on each
         * side; the drawn #424242 surface measures 488..1767 px in the screenshots.
         */
        val Width = 454.76.dp

        /**
         * Dialogs centre inside [83 px, 1080 px] on settings surfaces, giving the
         * audited centre y = 581.5 px. Verified against variable-height modals:
         * screen 031 (h=845 -> top 159), 042 (h=996 -> top 84), 118 (h=532 -> top 316).
         */
        val SettingsTopRegionInset = 29.51.dp

        /** Maximum visible modal height observed, 907 px (screen 042 drew 906 px). */
        val MaxHeight = 322.49.dp

        /** Title inset from the visible modal edges, 68 px on both sides. */
        val TitleHorizontalPadding = 24.18.dp

        /** Title top offset inside the visible modal, 51 px. */
        val TitleTopPadding = 18.13.dp

        /** Title line box height, 76 px. */
        val TitleHeight = 27.02.dp

        /** Button row height, 135 px. */
        val ButtonHeight = 48.0.dp

        /** Gap between the button row and the visible modal bottom edge, 11 px. */
        val ButtonBottomGap = 3.91.dp

        /** Gap between the trailing button and the visible modal right edge, 34 px. */
        val ButtonEndGap = 12.09.dp

        /** Editable field inset from both visible modal edges, 56 px. */
        val FieldStartInset = 19.91.dp

        /** Editable field height, 135 px. */
        val FieldHeight = 48.0.dp

        /** Choice rows span the full visible modal width, 0 px inset. */
        val ListStartInset = 0.0.dp

        /** Choice-list top offset inside the visible modal, 150 px. */
        val ListTopOffset = 53.33.dp

        /** Editable field top offset inside the visible modal, 127 px. */
        val FieldTopOffset = 45.16.dp

        /** Choice row height, measured from the audited selection lists. */
        val ListRowHeight = 48.0.dp
    }
}
