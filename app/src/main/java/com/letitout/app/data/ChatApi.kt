package com.letitout.app.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.letitout.app.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

interface ChatApi {
    @POST("v1/chat")
    suspend fun chat(
        @Header("Authorization") authorization: String,
        @Body request: ChatRequest
    ): ChatResponse

    @POST("v1/report")
    suspend fun report(
        @Header("Authorization") authorization: String,
        @Body request: ReportRequest
    )
}

@Singleton
class ChatRepository @Inject constructor(
    private val api: ChatApi,
    @ApplicationContext private val context: Context
) : ChatGateway {
    override suspend fun reply(mood: Mood, conversation: List<ChatMessage>, intensity: Int?): ChatReply =
        api.chat("Bearer ${idToken()}", chatRequestFor(mood, conversation, intensity)).toChatReply()

    override suspend fun report(reason: ReportReason, replyText: String) {
        api.report("Bearer ${idToken()}", ReportRequest(reason.slug, replyText.trim().take(2000)))
    }

    private suspend fun idToken(): String {
        if (FirebaseApp.getApps(context).isEmpty()) {
            throw FriendlyException("Chat isn't set up in this build yet.")
        }
        val user = FirebaseAuth.getInstance().currentUser
            ?: throw FriendlyException("Please sign in or continue as a guest first.")
        return try {
            user.getIdToken(false).await().token
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (e: FirebaseNetworkException) {
            throw FriendlyException("Couldn't reach the server. Check your connection and try again.", e)
        } catch (e: Exception) {
            throw FriendlyException("Your sign-in needs a refresh. Please sign out and sign in again.", e)
        } ?: throw FriendlyException("Your sign-in needs a refresh. Please sign out and sign in again.")
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
