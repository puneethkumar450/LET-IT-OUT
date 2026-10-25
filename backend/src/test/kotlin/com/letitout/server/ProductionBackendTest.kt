package com.letitout.server

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProductionBackendTest {
    @Test fun pilotFaceIdOverridesEarlierCompanionFaceSetting() {
        val config = ServerConfig.fromEnvironment(mapOf(
            "SIMLI_FACE_ID_PILOT" to "template/generation",
            "SIMLI_FACE_ID_WOMAN_ADULT" to "old-face"
        ))
        assertEquals("template/generation", config.simliFaceId)
    }

    @Test fun productionDefaultsToChatDisabledAndRequiresProject() {
        val config = ServerConfig.fromEnvironment(mapOf("APP_ENV" to "production", "FIREBASE_PROJECT_ID" to "let-it-out-d661a"))
        assertFalse(config.chatEnabled)
        assertFailsWith<IllegalArgumentException> { ServerConfig.fromEnvironment(mapOf("APP_ENV" to "production")) }
        assertFailsWith<IllegalArgumentException> { ServerConfig.fromEnvironment(mapOf("APP_ENV" to "prod")) }
        assertFailsWith<IllegalArgumentException> { ServerConfig.fromEnvironment(mapOf("PORT" to "invalid")) }
        assertFailsWith<IllegalArgumentException> {
            ServerConfig.fromEnvironment(mapOf("APP_ENV" to "production", "CHAT_ENABLED" to "true", "FIREBASE_PROJECT_ID" to "let-it-out-d661a"))
        }
    }

    @Test fun rejectsInvalidConversationShape() {
        assertTrue(validChatRequest(ChatRequest("HAPPY", listOf(Message("user", "Hello")), 5)))
        assertFalse(validChatRequest(ChatRequest("HAPPY", listOf(Message("user", "  ")))))
        assertFalse(validChatRequest(ChatRequest("HAPPY", listOf(Message("user", "Hello")), 11)))
        assertFalse(validChatRequest(ChatRequest("UNKNOWN", listOf(Message("user", "Hello")))))
    }

    @Test fun localLimitsCountUsersAndGlobalTraffic() = runBlocking {
        val limiter = LocalRateLimiter(perUserLimit = 2, globalLimit = 3)
        assertTrue(limiter.allow("a"))
        assertTrue(limiter.allow("a"))
        assertFalse(limiter.allow("a"))
        assertTrue(limiter.allow("b"))
        assertFalse(limiter.allow("c"))
    }

    @Test fun disabledChatStillExposesLivenessButNotReadiness() = testApplication {
        application { module(ServerConfig.fromEnvironment(mapOf("APP_ENV" to "production", "FIREBASE_PROJECT_ID" to "let-it-out-d661a"))) }
        assertEquals(HttpStatusCode.OK, client.get("/health").status)
        assertEquals(HttpStatusCode.ServiceUnavailable, client.get("/ready").status)
        assertEquals(HttpStatusCode.ServiceUnavailable, client.post("/v1/chat").status)
    }
}
