package com.letitout.app.ui

import android.app.Activity
import com.letitout.app.data.AuthGateway
import com.letitout.app.data.ChatGateway
import com.letitout.app.data.ChatMessage
import com.letitout.app.data.ChatReply
import com.letitout.app.data.CheckInStats
import com.letitout.app.data.Feeling
import com.letitout.app.data.Mood
import com.letitout.app.data.ReminderSettings
import com.letitout.app.data.UserPreferences
import com.letitout.app.reminder.ReminderScheduler
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    private class FakeChat : ChatGateway {
        val calls = mutableListOf<Pair<Mood, List<ChatMessage>>>()
        private val pending = ArrayDeque<CompletableDeferred<ChatReply>>()
        var ignoresCancellation = false

        override suspend fun reply(mood: Mood, conversation: List<ChatMessage>): ChatReply {
            calls += mood to conversation
            val answer = CompletableDeferred<ChatReply>().also { pending.addLast(it) }
            return if (ignoresCancellation) withContext(NonCancellable) { answer.await() } else answer.await()
        }

        fun answer(text: String, support: Boolean = false) = pending.removeFirst().complete(ChatReply(text, support))
        fun fail(t: Throwable) = pending.removeFirst().completeExceptionally(t)
    }

    private class FakePreferences : UserPreferences {
        override val mood = MutableStateFlow(Mood.TALK)
        override val checkInStats = MutableStateFlow(CheckInStats())
        override val reminder = MutableStateFlow(ReminderSettings())
        val checkIns = mutableListOf<Pair<Mood, Feeling>>()
        var clears = 0

        override suspend fun setMood(mood: Mood) { this.mood.value = mood }
        override suspend fun recordCheckIn(mood: Mood, feeling: Feeling) { checkIns += mood to feeling }
        override suspend fun clearCheckIns() { clears++; checkInStats.value = CheckInStats() }
        override suspend fun setReminder(settings: ReminderSettings) { reminder.value = settings }
    }

    private class FakeAuth : AuthGateway {
        var signedIn = true
        var name: String? = "Asha"
        override fun isConfigured() = true
        override fun isSignedIn() = signedIn
        override fun userName() = name
        override suspend fun signInGuest() { signedIn = true }
        override suspend fun signInGoogle(activity: Activity): Boolean = true
        override suspend fun signOut() { signedIn = false }
        override suspend fun deleteAccount() { signedIn = false }
    }

    private class FakeReminders : ReminderScheduler {
        val events = mutableListOf<String>()
        override fun schedule(hour: Int, minute: Int) { events += "schedule $hour:$minute" }
        override fun cancel() { events += "cancel" }
    }

    private val chat = FakeChat()
    private val preferences = FakePreferences()
    private val auth = FakeAuth()
    private val reminders = FakeReminders()
    private lateinit var vm: AppViewModel
    private val state get() = vm.state.value

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        vm = AppViewModel(chat, preferences, auth, reminders)
    }

    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun startsWithAuthAndSavedPreferences() {
        preferences.mood.value = Mood.SAD
        preferences.checkInStats.value = CheckInStats(total = 5, lighter = 3)
        preferences.reminder.value = ReminderSettings(true, 9, 0)
        assertEquals(Mood.SAD, state.mood)
        assertEquals(CheckInStats(5, 3), state.checkInStats)
        assertEquals(ReminderSettings(true, 9, 0), state.reminder)
        assertTrue(state.authConfigured)
        assertTrue(state.signedIn)
        assertEquals("Asha", state.userName)

        auth.signedIn = false
        auth.name = null
        vm.refreshAuth()
        assertFalse(state.signedIn)
        assertNull(state.userName)
    }

    @Test fun sendTrimsAndAddsTheReply() {
        vm.chooseMood(Mood.ANXIOUS)
        vm.send("   my chest feels tight before exams   ")
        assertTrue(state.loading)
        assertEquals(Mood.ANXIOUS, chat.calls.single().first)
        assertEquals(listOf("my chest feels tight before exams"), chat.calls.single().second.map { it.text })

        vm.send("ignored while a reply is loading")
        assertEquals(1, chat.calls.size)

        chat.answer("Exams can make your whole body tense up. What's the one coming up first?")
        assertFalse(state.loading)
        assertEquals(listOf(true, false), state.messages.map { it.fromUser })
        assertEquals(state.messages.size, state.messages.map { it.id }.toSet().size)
    }

    @Test fun lateReplyAfterChoosingANewMoodIsDropped() {
        vm.send("hello")
        vm.chooseMood(Mood.SAD)
        assertFalse(state.loading)
        assertTrue(state.messages.isEmpty())
        assertEquals(Mood.SAD, preferences.mood.value)
    }

    @Test fun lateReplyThatIgnoresCancellationNeverLandsInTheNewConversation() {
        chat.ignoresCancellation = true
        vm.send("first conversation")
        vm.chooseMood(Mood.LONELY)
        vm.send("second conversation")
        assertTrue(state.loading)

        chat.answer("reply meant for the first conversation")
        assertEquals(listOf("second conversation"), state.messages.map { it.text })
        assertTrue(state.loading)

        chat.answer("reply for the second conversation")
        assertEquals(listOf("second conversation", "reply for the second conversation"), state.messages.map { it.text })
        assertFalse(state.loading)
    }

    @Test fun lateFailureAfterClearingIsDropped() {
        chat.ignoresCancellation = true
        vm.send("hello")
        vm.clearConversation()
        chat.fail(IOException("offline"))
        assertNull(state.error)
        assertFalse(state.loading)
        assertTrue(state.messages.isEmpty())
    }

    @Test fun retryResendsWithoutDuplicatingTheUserMessage() {
        vm.send("nobody at home listens to me")
        chat.fail(IOException("offline"))
        assertEquals("Couldn't reach the server. Check your connection and try again.", state.error)
        assertFalse(state.loading)

        vm.retry()
        assertNull(state.error)
        assertTrue(state.loading)
        assertEquals(chat.calls[0].second, chat.calls[1].second)
        assertEquals(1, state.messages.count { it.fromUser })

        vm.retry()
        assertEquals(2, chat.calls.size)

        chat.answer("That sounds really lonely, even with people around. What happened today?")
        assertEquals(listOf(true, false), state.messages.map { it.fromUser })

        vm.retry()
        assertEquals(2, chat.calls.size)
    }

    @Test fun errorsBecomeFriendlyTextAndCanBeDismissed() {
        vm.send("hi")
        chat.fail(HttpException(Response.error<Any>(429, "{}".toResponseBody("application/json".toMediaType()))))
        val error = state.error.orEmpty()
        assertTrue(error.contains("pause"))
        assertFalse(error.contains("429"))
        vm.dismissError()
        assertNull(state.error)
    }

    @Test fun supportCardShowsAndStaysDismissedForThisConversation() {
        vm.send("i can't sleep and nothing feels worth it lately")
        chat.answer("That sounds really heavy.", support = true)
        assertTrue(state.supportSuggested)

        vm.dismissSupport()
        assertFalse(state.supportSuggested)
        vm.send("still feel the same")
        chat.answer("I'm here with you.", support = true)
        assertFalse(state.supportSuggested)

        vm.chooseMood(Mood.SAD)
        vm.send("it's back again")
        chat.answer("I'm really glad you told me.", support = true)
        assertTrue(state.supportSuggested)

        vm.clearConversation()
        assertFalse(state.supportSuggested)
    }

    @Test fun feelingCheckIsDueAfterTwoRepliesAndResetsPerConversation() {
        vm.chooseMood(Mood.STRESSED)
        vm.send("work is too much")
        chat.answer("What's piling up the most?")
        assertFalse(state.feelingCheckDue)
        vm.send("deadlines")
        chat.answer("Which one is closest?")
        assertTrue(state.feelingCheckDue)

        vm.recordFeeling(Feeling.LIGHTER)
        assertFalse(state.feelingCheckDue)
        assertEquals(listOf(Mood.STRESSED to Feeling.LIGHTER), preferences.checkIns)
        vm.recordFeeling(Feeling.HEAVIER)
        assertEquals(1, preferences.checkIns.size)

        vm.send("thanks")
        chat.answer("Anytime. Come back whenever you like.")
        assertFalse(state.feelingCheckDue)

        vm.chooseMood(Mood.HAPPY)
        assertFalse(state.feelingCheckDue)
        vm.send("got the job!")
        chat.answer("No way, congratulations! How did you find out?")
        vm.send("they called me")
        chat.answer("I love that. How are you celebrating?")
        assertTrue(state.feelingCheckDue)

        vm.skipFeelingCheck()
        assertFalse(state.feelingCheckDue)
        assertEquals(1, preferences.checkIns.size)
    }

    @Test fun setReminderSchedulesCancelsAndPersists() {
        vm.setReminder(true, 21, 30)
        assertEquals(listOf("schedule 21:30"), reminders.events)
        assertEquals(ReminderSettings(true, 21, 30), preferences.reminder.value)
        assertEquals(ReminderSettings(true, 21, 30), state.reminder)

        vm.setReminder(false, 21, 30)
        assertEquals(listOf("schedule 21:30", "cancel"), reminders.events)
        assertEquals(ReminderSettings(false, 21, 30), preferences.reminder.value)

        vm.setReminder(true, 30, 75)
        assertEquals("schedule 23:59", reminders.events.last())
    }

    @Test fun clearCheckInsResetsStats() {
        preferences.checkInStats.value = CheckInStats(4, 2)
        vm.clearCheckIns()
        assertEquals(1, preferences.clears)
        assertEquals(CheckInStats(), state.checkInStats)
    }
}
