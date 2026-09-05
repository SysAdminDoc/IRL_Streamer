package com.irlstreamer.reconstruction

import com.irlstreamer.reconstruction.model.ReplicaSettings
import com.irlstreamer.reconstruction.model.hasStreamKey
import com.irlstreamer.reconstruction.model.redactStreamKey
import com.irlstreamer.reconstruction.ui.settings.SettingItem
import com.irlstreamer.reconstruction.ui.settings.SettingsCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A stream key on screen is the channel-hijack path that key rotation exists to
 * undo, and the phone showing it is the one being streamed with. The connections
 * page used to render the whole URL as a row summary, and a publish failure
 * raised a toast containing it.
 */
class StreamKeyRedactionTest {

    private val secret = "sk_live_do_not_show_this"

    @Test
    fun theKeySegmentIsHiddenButTheDestinationStaysRecognisable() {
        val masked = redactStreamKey("rtmp://ingest.example.com/live/$secret")

        assertFalse("the key survived redaction: $masked", masked.contains(secret))
        // Scheme, host and application path are what identify the destination.
        assertTrue(masked, masked.startsWith("rtmp://ingest.example.com/live/"))
    }

    @Test
    fun aKeyCarriedInTheQueryStringIsHiddenToo() {
        val masked = redactStreamKey("rtmps://ingest.example.com/app/stream?key=$secret")

        assertFalse("the key survived redaction: $masked", masked.contains(secret))
        assertTrue(masked, masked.startsWith("rtmps://ingest.example.com/app/"))
    }

    @Test
    fun urlsWithNothingToHideAreLeftAlone() {
        // Decorating these with a mask would hide nothing and only confuse.
        assertEquals("rtmp://ingest.example.com", redactStreamKey("rtmp://ingest.example.com"))
        assertEquals("rtmp://ingest.example.com/", redactStreamKey("rtmp://ingest.example.com/"))
        assertEquals("", redactStreamKey("   "))
        assertFalse(hasStreamKey("rtmp://ingest.example.com"))
        assertTrue(hasStreamKey("rtmp://ingest.example.com/live/$secret"))
    }

    @Test
    fun theConnectionsPageHidesTheKeyUntilItIsDeliberatelyRevealed() {
        val settings = ReplicaSettings(
            connectionName = "Rig",
            connectionUrl = "rtmp://ingest.example.com/live/$secret",
        )

        val hidden = SettingsCatalog.connectionsPage(settings)
        val summaries = hidden.items.filterIsInstance<SettingItem.Row>().map { it.summary }
        assertFalse(
            "the key reached a settings row: $summaries",
            summaries.any { it.contains(secret) },
        )
        assertTrue(
            "there must be a way to ask for the key",
            hidden.items.filterIsInstance<SettingItem.Row>().any { it.id == "reveal_stream_key" },
        )

        // Revealing is a deliberate act, and only then is the key shown.
        val revealed = SettingsCatalog.connectionsPage(settings, revealStreamKey = true)
        val saved = revealed.items.filterIsInstance<SettingItem.Row>().single { it.id == "saved_connection" }
        assertEquals(settings.connectionUrl, saved.summary)
    }

    @Test
    fun aDestinationWithoutAKeyOffersNoRevealRow() {
        val settings = ReplicaSettings(connectionName = "Rig", connectionUrl = "rtmp://ingest.example.com")

        val page = SettingsCatalog.connectionsPage(settings)

        assertFalse(
            page.items.filterIsInstance<SettingItem.Row>().any { it.id == "reveal_stream_key" },
        )
    }
}
