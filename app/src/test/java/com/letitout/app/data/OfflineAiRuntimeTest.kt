package com.letitout.app.data

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineAiRuntimeTest {
    private lateinit var runtime: OfflineAiRuntime

    @Before fun setUp() {
        runtime = OfflineAiRuntime(java.io.File(System.getProperty("java.io.tmpdir"), "offline-ai-test").apply { mkdirs() })
    }

    @Test fun localFallbackDoesNotNeedNetwork() = runBlocking {
        val response = runtime.reply(Mood.TALK, listOf(ChatMessage(1, "I need to talk", true)))

        assertEquals(OfflineModelStatus.FALLBACK_READY, runtime.status())
        assertEquals("NORMAL", response.safetyState)
        assertTrue(response.reply.contains("listen", ignoreCase = true))
    }

    @Test fun obviousRiskRoutesToSupportWithoutModelCall() = runBlocking {
        val response = runtime.reply(Mood.SAD, listOf(ChatMessage(1, "I want to die", true)))

        assertEquals("CRISIS", response.safetyState)
        assertTrue(response.reply.contains("14416"))
    }
}
