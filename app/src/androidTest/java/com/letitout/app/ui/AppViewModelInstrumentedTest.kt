package com.letitout.app.ui

import android.content.Context
import android.content.ContextWrapper
import android.os.SystemClock
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.letitout.app.auth.AuthManager
import com.letitout.app.data.AppSettings
import com.letitout.app.data.ChatApi
import com.letitout.app.data.ChatRepository
import com.letitout.app.data.ChatRequest
import com.letitout.app.data.ChatResponse
import com.letitout.app.data.Mood
import com.letitout.app.data.MoodPreferences
import com.letitout.app.data.SessionTokenProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/** Exercises the real ViewModel with in-process replies; no Firebase sign-in or AI request is made. */
@RunWith(AndroidJUnit4::class)
class AppViewModelInstrumentedTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Test fun oldReplyCannotChangeNewConversation() = withViewModel { viewModel, api ->
        onMain { viewModel.send(" First message ") }
        await { api.requests.size == 1 }

        // Reset and begin B in the same UI turn, so A's cancellation may be delivered afterwards.
        onMain {
            viewModel.newConversation()
            viewModel.send("Second message")
        }
        await { api.requests.size == 2 }
        assertTrue(viewModel.state.value.loading)
        assertEquals(listOf("Second message"), viewModel.state.value.messages.map { it.text })

        // This fake call can still complete after cancellation, as some callbacks do in practice.
        api.reply(0, "Stale reply")
        instrumentation.waitForIdleSync()
        assertTrue("The old request must not stop B's loading state", viewModel.state.value.loading)
        assertEquals(listOf("Second message"), viewModel.state.value.messages.map { it.text })

        api.reply(1, "Current reply")
        await { viewModel.state.value.messages.size == 2 && !viewModel.state.value.loading }
        assertEquals(listOf("Second message", "Current reply"), viewModel.state.value.messages.map { it.text })
        assertNull(viewModel.state.value.error)
    }

    @Test fun retryAndEditPreserveOneUserDraft() = withViewModel { viewModel, api ->
        onMain { viewModel.send("  A difficult day  ") }
        await { api.requests.size == 1 }
        api.fail(0, IOException("offline"))
        await { viewModel.state.value.error != null && !viewModel.state.value.loading }
        assertEquals(listOf("A difficult day"), viewModel.state.value.messages.map { it.text })

        onMain { viewModel.retry() }
        await { api.requests.size == 2 }
        assertEquals(1, viewModel.state.value.messages.size)
        api.fail(1, IOException("offline again"))
        await { viewModel.state.value.error != null && !viewModel.state.value.loading }

        var draft: String? = null
        onMain { draft = viewModel.editLastMessage() }
        assertEquals("A difficult day", draft)
        assertTrue(viewModel.state.value.messages.isEmpty())
        assertNull(viewModel.state.value.error)

        onMain { viewModel.send("An edited difficult day") }
        await { api.requests.size == 3 }
        api.reply(2, "I'm listening")
        await { viewModel.state.value.messages.size == 2 && !viewModel.state.value.loading }
        assertEquals(listOf("An edited difficult day", "I'm listening"), viewModel.state.value.messages.map { it.text })
        assertFalse(viewModel.state.value.supportMode)
    }

    private inline fun withViewModel(block: (AppViewModel, HeldChatApi) -> Unit) {
        val targetContext = instrumentation.targetContext
        // Match the existing preference test's isolated path; neither test touches the app's store.
        val isolatedFiles = File(targetContext.cacheDir, "instrumented-preferences").apply { mkdirs() }
        val context = object : ContextWrapper(targetContext) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = isolatedFiles
        }
        val preferences = MoodPreferences(context)
        runBlocking { preferences.clearAll() }
        val api = HeldChatApi()
        val repository = ChatRepository(api, object : SessionTokenProvider {
            override suspend fun idToken() = "local-test-token"
        })
        val viewModel = AppViewModel(repository, preferences, AuthManager(targetContext))
        val store = ViewModelStore().apply { put("test", viewModel) }
        try {
            await { viewModel.state.value.settings == AppSettings() && viewModel.state.value.mood == Mood.TALK }
            block(viewModel, api)
        } finally {
            onMain { store.clear() }
            runBlocking { preferences.clearAll() }
        }
    }

    private fun onMain(action: () -> Unit) = instrumentation.runOnMainSync(action)

    private fun await(condition: () -> Boolean) {
        repeat(50) {
            instrumentation.waitForIdleSync()
            if (condition()) return
            SystemClock.sleep(50)
        }
        assertTrue("Timed out waiting for ViewModel state", condition())
    }

    private class HeldChatApi : ChatApi {
        val requests = CopyOnWriteArrayList<ChatRequest>()
        private val continuations = CopyOnWriteArrayList<Continuation<ChatResponse>>()

        override suspend fun chat(authorization: String, request: ChatRequest): ChatResponse = suspendCoroutine { continuation ->
            requests += request
            continuations += continuation
        }

        fun reply(index: Int, text: String) = continuations[index].resume(ChatResponse(text))
        fun fail(index: Int, error: Exception) = continuations[index].resumeWithException(error)
    }
}
