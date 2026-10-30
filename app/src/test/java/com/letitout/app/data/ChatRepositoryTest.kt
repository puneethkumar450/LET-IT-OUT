package com.letitout.app.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class ChatRepositoryTest {
    private lateinit var server: MockWebServer
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @Before fun startServer() {
        server = MockWebServer()
        server.start()
    }

    @After fun stopServer() {
        server.shutdown()
    }

    private fun repository(token: String = "test-token"): ChatRepository {
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ChatApi::class.java)
        return ChatRepository(api, object : SessionTokenProvider {
            override suspend fun idToken() = token
        })
    }

    @Test fun sendsOnlyRecentConversationWithMoodIntensityAndAuth() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"reply":"I'm listening.","safetyState":"NORMAL"}"""))
        val conversation = (1..15).map { number ->
            ChatMessage(number.toLong(), "message $number", fromUser = number % 2 == 1)
        }

        val response = repository().reply(Mood.ANXIOUS, conversation, intensity = 8, sessionId = "chat-0001")
        val request = server.takeRequest()
        val body = request.body.readUtf8()
        val sent = moshi.adapter(ChatRequest::class.java).fromJson(body)!!

        assertEquals("I'm listening.", response.reply)
        assertEquals("POST", request.method)
        assertEquals("/v1/chat", request.path)
        assertEquals("Bearer test-token", request.getHeader("Authorization"))
        assertEquals("ANXIOUS", sent.mood)
        assertEquals(8, sent.intensity)
        assertEquals("chat-0001", sent.sessionId)
        assertFalse(sent.closing)
        assertEquals(12, sent.messages.size)
        assertEquals("message 4", sent.messages.first().content)
        assertEquals("assistant", sent.messages.first().role)
        assertEquals("message 15", sent.messages.last().content)
        assertEquals("user", sent.messages.last().role)
        assertFalse(body.contains("test-token"))
    }

    @Test fun serverFailureIsAvailableToRetryFlow() {
        server.enqueue(MockResponse().setResponseCode(503).setBody("""{"error":"unavailable"}"""))

        val error = assertThrows(HttpException::class.java) {
            runBlocking { repository().reply(Mood.TALK, listOf(ChatMessage(1, "Hello", true))) }
        }

        assertEquals(503, error.code())
        assertEquals(1, server.requestCount)
    }

    @Test fun avatarSessionUsesFirebaseAuthorizationAndReceivesOnlyShortLivedToken() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"sessionToken":"short-lived","seconds":170}"""))
        val session = repository().avatarSession(Mood.ANGRY, "chat-0001")
        assertEquals("short-lived", session.sessionToken)
        assertEquals(170L, session.seconds)
        val request = server.takeRequest()
        assertEquals("/v1/avatar/session?mood=ANGRY&sessionId=chat-0001", request.path)
        server.enqueue(MockResponse().setResponseCode(204))
        repository().avatarStarted("chat-0001")
        assertEquals("/v1/avatar/started?sessionId=chat-0001", server.takeRequest().path)
        assertEquals("Bearer test-token", request.getHeader("Authorization"))
        assertEquals(0, request.body.size)
    }

    @Test fun allowanceReadsTodaysFreeTime() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"chatsPerDay":3,"chatsLeft":2,"chatMinutes":15,"resetsAt":1793300000000,"liveSeconds":180,"liveAvailable":false,"nextLiveAt":1793900000000,"liveEnabled":true}"""
        ))
        val allowance = repository().allowance()
        assertEquals(2, allowance.chatsLeft)
        assertEquals(1793900000000L, allowance.nextLiveAt)
        assertTrue(allowance.liveEnabled)
        assertEquals("/v1/allowance", server.takeRequest().path)
    }

    @Test fun prewarmUsesHealthWithoutStartingAvatarSession() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

        repository().prewarm()

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/health", request.path)
        assertEquals(1, server.requestCount)
    }

    @Test fun missingSessionNeverSendsPrivateConversation() {
        val api = object : ChatApi {
            override suspend fun health() = throw AssertionError("Health request must not run without a session")
            override suspend fun chat(authorization: String, request: ChatRequest): ChatResponse {
                throw AssertionError("Chat request must not run without a session")
            }
            override suspend fun avatarSession(authorization: String, mood: String, sessionId: String): AvatarSessionResponse {
                throw AssertionError("Avatar request must not run without a session")
            }
            override suspend fun avatarStarted(authorization: String, sessionId: String): retrofit2.Response<Unit> {
                throw AssertionError("Avatar request must not run without a session")
            }
            override suspend fun allowance(authorization: String): Allowance {
                throw AssertionError("Allowance request must not run without a session")
            }
        }
        val repository = ChatRepository(api, object : SessionTokenProvider {
            override suspend fun idToken(): String = throw IllegalStateException("Please sign in first")
        })

        val error = assertThrows(IllegalStateException::class.java) {
            runBlocking { repository.reply(Mood.SAD, listOf(ChatMessage(1, "Private message", true))) }
        }

        assertTrue(error.message!!.contains("sign in"))
        assertEquals(0, server.requestCount)
    }
}
