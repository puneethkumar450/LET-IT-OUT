package com.letitout.app.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.letitout.app.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import okhttp3.ResponseBody
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

interface ChatApi {
    @GET("health")
    suspend fun health(): ResponseBody

    @POST("v1/chat")
    suspend fun chat(
        @Header("Authorization") authorization: String,
        @Body request: ChatRequest
    ): ChatResponse

    @POST("v1/avatar/session")
    suspend fun avatarSession(
        @Header("Authorization") authorization: String,
        @Query("mood") mood: String,
        @Query("sessionId") sessionId: String
    ): AvatarSessionResponse

    /** Tells the server Ari's video appeared, so the week's live preview counts. */
    @POST("v1/avatar/started")
    suspend fun avatarStarted(@Header("Authorization") authorization: String, @Query("sessionId") sessionId: String): retrofit2.Response<Unit>

    @GET("v1/allowance")
    suspend fun allowance(@Header("Authorization") authorization: String): Allowance
}

@Singleton
class ChatRepository @Inject constructor(private val api: ChatApi, private val tokenProvider: SessionTokenProvider) {
    /** Warms Firebase auth and the existing backend connection without starting a Simli session. */
    suspend fun prewarm(): Unit = coroutineScope {
        val auth = async { tokenProvider.idToken() }
        val backend = async { api.health().close() }
        auth.await()
        backend.await()
    }

    suspend fun avatarSession(mood: Mood, sessionId: String): AvatarSessionResponse =
        api.avatarSession("Bearer ${tokenProvider.idToken()}", mood.name, sessionId)
    suspend fun avatarStarted(sessionId: String) {
        api.avatarStarted("Bearer ${tokenProvider.idToken()}", sessionId)
    }
    suspend fun allowance(): Allowance = api.allowance("Bearer ${tokenProvider.idToken()}")
    suspend fun reply(
        mood: Mood, conversation: List<ChatMessage>, intensity: Int? = null,
        sessionId: String? = null, closing: Boolean = false
    ): ChatResponse {
        val idToken = tokenProvider.idToken()
        return api.chat(
            "Bearer $idToken",
            ChatRequest(mood.name, conversation.takeLast(12).map {
                ApiMessage(if (it.fromUser) "user" else "assistant", it.text)
            }, intensity, sessionId, closing)
        )
    }
}

interface SessionTokenProvider {
    suspend fun idToken(): String
}

@Singleton
class FirebaseSessionTokenProvider @Inject constructor(@ApplicationContext private val context: Context) : SessionTokenProvider {
    override suspend fun idToken(): String {
        if (FirebaseApp.getApps(context).isEmpty()) {
            throw IllegalStateException("Firebase setup is required before AI chat can connect.")
        }
        val user = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("Please sign in or continue as a guest first.")
        return user.getIdToken(false).await().token
            ?: throw IllegalStateException("Could not retrieve sign-in token.")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton
    fun provideSessionTokenProvider(provider: FirebaseSessionTokenProvider): SessionTokenProvider = provider

    @Provides @Singleton
    fun provideApi(): ChatApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().add(KotlinJsonAdapterFactory()).build()))
        .build()
        .create(ChatApi::class.java)
}
