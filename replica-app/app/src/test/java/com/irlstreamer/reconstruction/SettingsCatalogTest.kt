package com.irlstreamer.reconstruction

import com.irlstreamer.reconstruction.model.SettingsPage
import com.irlstreamer.reconstruction.ui.settings.SettingItem
import com.irlstreamer.reconstruction.ui.settings.SettingsCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsCatalogTest {
    @Test
    fun everyCatalogBackedSettingsPageHasContent() {
        val formPages = setOf(
            SettingsPage.CONNECTION_FORM,
            SettingsPage.PICTURE_LAYER_FORM,
            SettingsPage.TEXT_LAYER_FORM,
            SettingsPage.TIMESTAMP_FORM,
            SettingsPage.WEB_OVERLAY_FORM,
        )

        SettingsPage.entries.filterNot { it in formPages }.forEach { page ->
            val spec = SettingsCatalog.page(page)
            assertTrue("$page must have a title", spec.title.isNotBlank())
            assertTrue("$page must expose audited content", spec.items.isNotEmpty())
        }
    }

    @Test
    fun rootContainsTheAuditedPrimaryDestinations() {
        val rows = SettingsCatalog.page(SettingsPage.ROOT).items.filterIsInstance<SettingItem.Row>()
        assertEquals(12, rows.size)
        assertTrue(rows.any { it.title == "Streamer" })
        assertTrue(rows.any { it.title == "Connections" })
        assertTrue(rows.any { it.title == "Advanced options" })
        assertTrue(rows.any { it.title == "Help & support" })
    }
}
