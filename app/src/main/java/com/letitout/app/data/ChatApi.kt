package com.letitout.app.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.letitout.app.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit
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
class ChatRepository @Inject constructor(
    private val api: ChatApi,
    @ApplicationContext private val context: Context
) : ChatGateway {
    override suspend fun reply(mood: Mood, conversation: List<ChatMessage>): ChatReply {
        if (FirebaseApp.getApps(context).isEmpty()) {
            throw FriendlyException("Chat isn't set up in this build yet.")
        }
        val user = FirebaseAuth.getInstance().currentUser
            ?: throw FriendlyException("Please sign in or continue as a guest first.")
        val idToken = try {
            user.getIdToken(false).await().token
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (e: FirebaseNetworkException) {
            throw FriendlyException("Couldn't reach the server. Check your connection and try again.", e)
        } catch (e: Exception) {
            throw FriendlyException("Your sign-in needs a refresh. Please sign out and sign in again.", e)
        } ?: throw FriendlyException("Your sign-in needs a refresh. Please sign out and sign in again.")
        val messages = conversation
            .map { it.copy(text = it.text.trim().take(2000)) }
            .filter { it.text.isNotBlank() }
            .takeLast(12)
            .map { ApiMessage(if (it.fromUser) "user" else "assistant", it.text) }
        val response = api.chat("Bearer $idToken", ChatRequest(mood.name, messages))
        val text = response.reply.trim()
        if (text.isEmpty()) throw FriendlyException("I couldn't put a reply together just now. Mind trying again?")
        return ChatReply(text, response.safetyState == "SUPPORT_SUGGESTED")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .build()

    @Provides @Singleton
    fun provideApi(client: OkHttpClient): ChatApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().add(KotlinJsonAdapterFactory()).build()))
        .build()
        .create(ChatApi::class.java)
}
