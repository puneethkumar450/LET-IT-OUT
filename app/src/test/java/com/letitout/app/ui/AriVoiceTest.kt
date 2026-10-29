package com.letitout.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class AriVoiceTest {
    private val india = Locale("en", "IN")
    private val voices = listOf(
        RawVoice("en-us-x-low", Locale.US, quality = 300, needsNetwork = false, installed = true),
        RawVoice("en-in-x-b", india, quality = 300, needsNetwork = false, installed = true),
        RawVoice("en-in-x-a", india, quality = 400, needsNetwork = false, installed = true),
        RawVoice("en-in-x-online", india, quality = 500, needsNetwork = true, installed = true),
        RawVoice("en-in-x-missing", india, quality = 500, needsNetwork = false, installed = false),
        RawVoice("hi-in-x-a", Locale("hi", "IN"), quality = 400, needsNetwork = false, installed = true)
    )

    @Test fun offersInstalledOfflineVoicesInThePhoneLanguageBestFirst() {
        val options = ariVoiceOptions(voices, india)
        assertEquals(listOf("en-in-x-a", "en-in-x-b", "en-us-x-low"), options.map { it.name })
        assertEquals("Voice 1 · English (India)", options.first().label)
    }

    @Test fun keepsASavedVoiceAndFallsBackWhenItIsGone() {
        val options = ariVoiceOptions(voices, india)
        assertEquals("en-in-x-b", resolveAriVoice(options, "en-in-x-b")?.name)
        assertEquals("en-in-x-a", resolveAriVoice(options, "uninstalled-voice")?.name)
        assertEquals("en-in-x-a", resolveAriVoice(options, null)?.name)
        assertNull(resolveAriVoice(emptyList(), null))
    }
}
