package com.letitout.server

import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatRouteTest {
    private val config = ServerConfig.fromEnvironment(emptyMap())

    @Test fun chatRequiresAuthAndRejectsInvalidInputBeforeCallingProvider() = testApplication {
        var providerCalls = 0
        application {
            module(config, ChatDependencies(
                ready = { true },
                verifyUid = { token, _ -> if (token == "valid") "test-user" else error("Invalid token") },
                reply = { _, _ -> providerCalls++; "A test reply" },
                rateLimiter = { allowAll() }
            ))
        }

        val missingAuth = client.post("/v1/chat") { contentType(ContentType.Application.Json); setBody(message("Hello")) }
        assertEquals(HttpStatusCode.Unauthorized, missingAuth.status)
        assertEquals("no-store", missingAuth.headers[HttpHeaders.CacheControl])

        assertEquals(HttpStatusCode.Unauthorized, client.post("/v1/chat") {
            header(HttpHeaders.Authorization, "Bearer invalid")
            contentType(ContentType.Application.Json)
            setBody(message("Hello"))
        }.status)

        assertEquals(HttpStatusCode.BadRequest, client.post("/v1/chat") {
            header(HttpHeaders.Authorization, "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody("""{"mood":"SAD","messages":[{"role":"assistant","content":"Hello"}]}""")
        }.status)

        assertEquals(HttpStatusCode.PayloadTooLarge, client.post("/v1/chat") {
            header(HttpHeaders.Authorization, "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody("x".repeat(20_001))
        }.status)
        assertEquals(0, providerCalls)
    }

    @Test fun authenticatedChatReturnsReplyAndDivertsExplicitCrisisWithoutCallingProvider() = testApplication {
        var providerCalls = 0
        application {
            module(config, ChatDependencies(
                ready = { true },
                verifyUid = { _, _ -> "test-user" },
                reply = { _, _ -> providerCalls++; "What happened today?" },
                rateLimiter = { allowAll() }
            ))
        }

        val normal = client.post("/v1/chat") {
            header(HttpHeaders.Authorization, "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(message("My day was difficult"))
        }
        assertEquals(HttpStatusCode.OK, normal.status)
        assertTrue(normal.bodyAsText().contains("What happened today?"))
        assertTrue(normal.bodyAsText().contains("NORMAL"))

        val crisis = client.post("/v1/chat") {
            header(HttpHeaders.Authorization, "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(message("I want to die"))
        }
        assertEquals(HttpStatusCode.OK, crisis.status)
        assertTrue(crisis.bodyAsText().contains("CRISIS_SUPPORT"))
        assertEquals(1, providerCalls)
    }

    @Test fun rateLimitRejectsChatBeforeCallingProvider() = testApplication {
        var providerCalls = 0
        application {
            module(config, ChatDependencies(
                ready = { true },
                verifyUid = { _, _ -> "test-user" },
                reply = { _, _ -> providerCalls++; "Should not run" },
                rateLimiter = { object : RateLimiter { override suspend fun allow(uid: String) = false } }
            ))
        }

        val response = client.post("/v1/chat") {
            header(HttpHeaders.Authorization, "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(message("Hello"))
        }
        assertEquals(HttpStatusCode.TooManyRequests, response.status)
        assertEquals(0, providerCalls)
    }

    @Test fun providerFailureReturnsGenericErrorWithoutLeakingDetails() = testApplication {
        application {
            module(config, ChatDependencies(
                ready = { true },
                verifyUid = { _, _ -> "test-user" },
                reply = { _, _ -> error("provider diagnostic with private content") },
                rateLimiter = { allowAll() }
            ))
        }

        val response = client.post("/v1/chat") {
            header(HttpHeaders.Authorization, "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(message("Hello"))
        }
        assertEquals(HttpStatusCode.BadGateway, response.status)
        assertTrue(response.bodyAsText().contains("AI is temporarily unavailable."))
        assertFalse(response.bodyAsText().contains("private content"))
    }

    private fun message(text: String): String =
        """{"mood":"SAD","messages":[{"role":"user","content":"$text"}]}"""

    private fun allowAll(): RateLimiter = object : RateLimiter {
        override suspend fun allow(uid: String) = true
    }
}
