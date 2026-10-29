package com.letitout.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class SpeechIdTest {
    @Test fun clearsAnEqualIdThatIsADifferentObject() {
        val current = AtomicReference<String?>("42:1")
        // The TTS engine hands IDs back as new String objects.
        val reported = StringBuilder("42:").append(1).toString()
        assertTrue(current.clearIfCurrent(reported))
        assertNull(current.get())
    }

    @Test fun keepsANewerUtterance() {
        val current = AtomicReference<String?>("42:2")
        assertFalse(current.clearIfCurrent("42:1"))
        assertFalse(AtomicReference<String?>(null).clearIfCurrent("42:1"))
    }
}
