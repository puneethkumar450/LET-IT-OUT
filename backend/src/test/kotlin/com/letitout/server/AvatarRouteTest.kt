package com.letitout.server

import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AvatarRouteTest {
    private val config = ServerConfig.fromEnvironment(emptyMap())

    @Test fun sessionRequiresAuthAndNeverExposesProviderKey() = testApplication {
        var providerCalls = 0
        application {
            module(config, avatarDependencies = AvatarDependencies(
                ready = { true },
                verifyUid = { token, _ -> if (token == "valid") "user-1" else error("bad token") },
                createSession = { _, mood ->
                    assertEquals("ANGRY", mood)
                    providerCalls++
                    "temporary-session-token"
                },
                rateLimiter = { allowAll() }
            ))
        }
        assertEquals(HttpStatusCode.Unauthorized, client.post("/v1/avatar/session").status)
        assertEquals(HttpStatusCode.Unauthorized, client.post("/v1/avatar/session") {
            header(HttpHeaders.Authorization, "Bearer invalid")
        }.status)
        assertEquals(0, providerCalls)

        val response = client.post("/v1/avatar/session?mood=ANGRY") { header(HttpHeaders.Authorization, "Bearer valid") }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("no-store", response.headers[HttpHeaders.CacheControl])
        assertTrue(response.bodyAsText().contains("temporary-session-token"))
        assertFalse(response.bodyAsText().contains("faceId"))
        assertEquals(1, providerCalls)
    }

    @Test fun rateLimitAndProviderFailureDoNotIssueTokens() = testApplication {
        application {
            module(config, avatarDependencies = AvatarDependencies(
                ready = { true },
                verifyUid = { _, _ -> "user-1" },
                createSession = { _, _ -> error("secret provider diagnostic") },
                rateLimiter = { allowAll() }
            ))
        }
        val response = client.post("/v1/avatar/session") { header(HttpHeaders.Authorization, "Bearer valid") }
        assertEquals(HttpStatusCode.BadGateway, response.status)
        assertFalse(response.bodyAsText().contains("secret"))
    }

    @Test fun rateLimitRejectsSessionBeforeProviderCall() = testApplication {
        var providerCalls = 0
        application {
            module(config, avatarDependencies = AvatarDependencies(
                ready = { true },
                verifyUid = { _, _ -> "user-1" },
                createSession = { _, _ -> providerCalls++; "unused" },
                rateLimiter = { object : RateLimiter { override suspend fun allow(uid: String) = false } }
            ))
        }
        assertEquals(HttpStatusCode.TooManyRequests, client.post("/v1/avatar/session") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }.status)
        assertEquals(0, providerCalls)
    }

    @Test fun disabledPilotRejectsSessionBeforeAuth() = testApplication {
        application { module(config) }
        assertEquals(HttpStatusCode.ServiceUnavailable, client.post("/v1/avatar/session").status)
    }

    @Test fun webSessionRequiresAllowedOriginAndLimitsByAddress() = testApplication {
        val webConfig = config.copy(webAvatarOrigins = setOf("https://let-it-go.in"))
        var providerCalls = 0
        val limitedKeys = mutableListOf<String>()
        application {
            module(webConfig, avatarDependencies = AvatarDependencies(
                webReady = { true },
                createWebSession = { providerCalls++; "web-session-token" },
                webRateLimiter = {
                    object : RateLimiter {
                        override suspend fun allow(uid: String): Boolean { limitedKeys += uid; return limitedKeys.size == 1 }
                    }
                }
            ))
        }
        assertEquals(HttpStatusCode.Forbidden, client.post("/v1/avatar/web-session").status)
        assertEquals(HttpStatusCode.Forbidden, client.post("/v1/avatar/web-session") {
            header(HttpHeaders.Origin, "https://example.com")
        }.status)
        assertEquals(0, providerCalls)

        val response = client.post("/v1/avatar/web-session") {
            header(HttpHeaders.Origin, "https://let-it-go.in")
            header("X-Forwarded-For", "1.1.1.1, 203.0.113.9")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("https://let-it-go.in", response.headers[HttpHeaders.AccessControlAllowOrigin])
        assertEquals("no-store", response.headers[HttpHeaders.CacheControl])
        assertTrue(response.bodyAsText().contains("web-session-token"))
        assertEquals("ip:203.0.113.9", limitedKeys.single())

        assertEquals(HttpStatusCode.TooManyRequests, client.post("/v1/avatar/web-session") {
            header(HttpHeaders.Origin, "https://let-it-go.in")
        }.status)
        assertEquals(1, providerCalls)
    }

    @Test fun webSessionIsOffWithoutConfiguredOrigins() = testApplication {
        application { module(config.copy(webAvatarOrigins = emptySet())) }
        assertEquals(HttpStatusCode.Forbidden, client.post("/v1/avatar/web-session") {
            header(HttpHeaders.Origin, "http://localhost:4173")
        }.status)
    }

    private fun allowAll(): RateLimiter = object : RateLimiter {
        override suspend fun allow(uid: String) = true
    }
}
