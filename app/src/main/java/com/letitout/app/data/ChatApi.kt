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
import retrofit2.http.POST
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

interface ChatApi {
    @POST("v1/chat")
    suspend fun chat(
        @Header("Authorization") authorization: String,
        @Body request: ChatRequest
    ): ChatResponse
}

@Singleton
class ChatRepository @Inject constructor(private val api: ChatApi, @ApplicationContext private val context: Context) {
    suspend fun reply(mood: Mood, conversation: List<ChatMessage>, intensity: Int? = null): ChatResponse {
        if (FirebaseApp.getApps(context).isEmpty()) {
            throw IllegalStateException("Firebase setup is required before AI chat can connect.")
        }
        val user = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("Please sign in or continue as a guest first.")
        val idToken = user.getIdToken(false).await().token
            ?: throw IllegalStateException("Could not retrieve sign-in token.")
        return api.chat(
            "Bearer $idToken",
            ChatRequest(mood.name, conversation.takeLast(12).map {
                ApiMessage(if (it.fromUser) "user" else "assistant", it.text)
            }, intensity)
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton
    fun provideApi(): ChatApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().add(KotlinJsonAdapterFactory()).build()))
        .build()
        .create(ChatApi::class.java)
}
