package com.letitout.app.ui

import android.app.Activity
import com.letitout.app.data.AppSettings
import com.letitout.app.data.AuthGateway
import com.letitout.app.data.ChatGateway
import com.letitout.app.data.ChatMessage
import com.letitout.app.data.ChatReply
import com.letitout.app.data.CheckInStats
import com.letitout.app.data.Feeling
import com.letitout.app.data.FriendlyException
import com.letitout.app.data.Mood
import com.letitout.app.data.ReminderSettings
import com.letitout.app.data.ReportReason
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
    private data class Call(val mood: Mood, val conversation: List<ChatMessage>, val intensity: Int?)

    private class FakeChat : ChatGateway {
        val calls = mutableListOf<Call>()
        private val pending = ArrayDeque<CompletableDeferred<ChatReply>>()
        var ignoresCancellation = false
        val reports = mutableListOf<Pair<ReportReason, String>>()
        var reportFailure: Throwable? = null

        override suspend fun report(reason: ReportReason, replyText: String) {
            reportFailure?.let { throw it }
            reports += reason to replyText
        }

        override suspend fun reply(mood: Mood, conversation: List<ChatMessage>, intensity: Int?): ChatReply {
            calls += Call(mood, conversation, intensity)
            val answer = CompletableDeferred<ChatReply>().also { pending.addLast(it) }
            return if (ignoresCancellation) withContext(NonCancellable) { answer.await() } else answer.await()
        }

        fun answer(text: String, support: Boolean = false) = pending.removeFirst().complete(ChatReply(text, support))
        fun fail(t: Throwable) = pending.removeFirst().completeExceptionally(t)
    }

    private class FakePreferences : UserPreferences {
        override val mood = MutableStateFlow(Mood.TALK)
        override val settings = MutableStateFlow(AppSettings())
        override val checkInStats = MutableStateFlow(CheckInStats())
        override val reminder = MutableStateFlow(ReminderSettings())
        val checkIns = mutableListOf<Pair<Mood, Feeling>>()
        var clears = 0
        var moodClears = 0

        override suspend fun setMood(mood: Mood) { this.mood.value = mood }
        override suspend fun clearMood() { moodClears++; mood.value = Mood.TALK }
        override suspend fun setReadAloud(enabled: Boolean) { settings.value = settings.value.copy(readAloud = enabled) }
        override suspend fun setVoiceInput(enabled: Boolean) { settings.value = settings.value.copy(voiceInput = enabled) }
        override suspend fun setFollowMoodTheme(enabled: Boolean) { settings.value = settings.value.copy(followMoodTheme = enabled) }
        override suspend fun setOnboarded() { settings.value = settings.value.copy(onboarded = true) }
        override suspend fun recordCheckIn(mood: Mood, feeling: Feeling) { checkIns += mood to feeling }
        override suspend fun clearCheckIns() { clears++; checkInStats.value = CheckInStats() }
        override suspend fun setReminder(settings: ReminderSettings) { reminder.value = settings }
    }

    private class FakeAuth : AuthGateway {
        var signedIn = true
        var guest = false
        var name: String? = "Asha"
        var email: String? = "asha@example.com"
        override fun isConfigured() = true
        override fun isGoogleConfigured() = true
        override fun isSignedIn() = signedIn
        override fun isGuest() = signedIn && guest
        override fun userName() = name
        override fun userEmail() = email
        override suspend fun signInGuest() { signedIn = true; guest = true }
        override suspend fun signInGoogle(activity: Activity): Boolean = true
        var signOutFailure: Exception? = null
        var deleteFailure: Exception? = null
        // Like Firebase: signed out first, then the Google credential clean-up can still fail.
        override suspend fun signOut() { signedIn = false; signOutFailure?.let { throw it } }
        override suspend fun deleteAccount() { deleteFailure?.let { throw it }; signedIn = false }
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
        preferences.settings.value = AppSettings(readAloud = false, voiceInput = true, followMoodTheme = false, onboarded = true)
        preferences.checkInStats.value = CheckInStats(total = 5, lighter = 3)
        preferences.reminder.value = ReminderSettings(true, 9, 0)
        assertEquals(Mood.SAD, state.mood)
        assertEquals(AppSettings(readAloud = false, voiceInput = true, followMoodTheme = false, onboarded = true), state.settings)
        assertEquals(CheckInStats(5, 3), state.checkInStats)
        assertEquals(ReminderSettings(true, 9, 0), state.reminder)
        assertTrue(state.authConfigured)
        assertTrue(state.signedIn)
        assertFalse(state.guest)
        assertEquals("Asha", state.userName)
        assertEquals("asha@example.com", state.userEmail)

        auth.signedIn = false
        auth.name = null
        auth.email = null
        vm.refreshAuth()
        assertFalse(state.signedIn)
        assertNull(state.userName)
        assertNull(state.userEmail)

        auth.signedIn = true
        auth.guest = true
        vm.refreshAuth()
        assertTrue(state.guest)
    }

    @Test fun settingsSettersSaveToPreferences() {
        vm.setReadAloud(false)
        vm.setVoiceInput(false)
        vm.setFollowMoodTheme(false)
        assertFalse(state.settings.onboarded)
        vm.finishOnboarding()
        assertEquals(AppSettings(readAloud = false, voiceInput = false, followMoodTheme = false, onboarded = true), preferences.settings.value)
        assertEquals(preferences.settings.value, state.settings)
    }

    @Test fun sendTrimsAndAddsTheReply() {
        vm.chooseMood(Mood.ANXIOUS)
        vm.send("   my chest feels tight before exams   ")
        assertTrue(state.loading)
        assertEquals(Mood.ANXIOUS, chat.calls.single().mood)
        assertEquals(listOf("my chest feels tight before exams"), chat.calls.single().conversation.map { it.text })

        vm.send("ignored while a reply is loading")
        assertEquals(1, chat.calls.size)

        chat.answer("Exams can make your whole body tense up. What's the one coming up first?")
        assertFalse(state.loading)
        assertEquals(listOf(true, false), state.messages.map { it.fromUser })
        assertEquals(state.messages.size, state.messages.map { it.id }.toSet().size)
    }

    @Test fun intensityIsPassedThroughAndKeptForANewConversation() {
        vm.chooseMood(Mood.ANGRY, 8)
        assertEquals(8, state.intensity)
        assertEquals(Mood.ANGRY, preferences.mood.value)
        vm.send("my manager yelled at me in front of everyone")
        assertEquals(Call(Mood.ANGRY, state.messages, 8), chat.calls.last())
        chat.answer("That sounds humiliating. What did he say?")

        vm.send("that I'm useless")
        assertEquals(8, chat.calls.last().intensity)
        chat.answer("Ouch. That would sting anyone.")

        vm.newConversation()
        assertTrue(state.messages.isEmpty())
        assertEquals(Mood.ANGRY, state.mood)
        assertEquals(8, state.intensity)
        vm.send("still thinking about it")
        assertEquals(Call(Mood.ANGRY, state.messages, 8), chat.calls.last())
        chat.answer("Of course you are.")

        vm.chooseMood(Mood.TALK)
        assertNull(state.intensity)
        vm.send("hey")
        assertEquals(Call(Mood.TALK, state.messages, null), chat.calls.last())
    }

    @Test fun retryKeepsTheIntensity() {
        vm.chooseMood(Mood.STRESSED, 9)
        vm.send("three deadlines tomorrow")
        chat.fail(IOException("offline"))
        vm.retry()
        assertEquals(listOf(9, 9), chat.calls.map { it.intensity })
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

    @Test fun lateFailureAfterStartingOverIsDropped() {
        chat.ignoresCancellation = true
        vm.send("hello")
        vm.newConversation()
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
        assertEquals(chat.calls[0].conversation, chat.calls[1].conversation)
        assertEquals(1, state.messages.count { it.fromUser })

        vm.retry()
        assertEquals(2, chat.calls.size)

        chat.answer("That sounds really lonely, even with people around. What happened today?")
        assertEquals(listOf(true, false), state.messages.map { it.fromUser })

        vm.retry()
        assertEquals(2, chat.calls.size)
    }

    @Test fun editLastMessageTakesTheUnsentMessageBack() {
        vm.send("i dont know how to say this")
        assertNull("can't edit while a reply is on its way", vm.editLastMessage())
        assertEquals(1, state.messages.size)

        chat.fail(IOException("offline"))
        assertEquals("i dont know how to say this", vm.editLastMessage())
        assertTrue(state.messages.isEmpty())
        assertNull(state.error)
        assertNull(vm.editLastMessage())

        vm.send("let me try again")
        chat.answer("Take your time.")
        assertNull("a message that was answered stays", vm.editLastMessage())
        assertEquals(2, state.messages.size)
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

    @Test fun supportFlagShowsAtMostOnceEveryFourReplies() {
        vm.send("i can't sleep and nothing feels worth it lately")
        chat.answer("That sounds really heavy.", support = true)
        assertTrue(state.messages.last().supportResources)
        assertTrue(state.supportMode)

        repeat(3) { turn ->
            vm.send("message $turn")
            chat.answer("reply $turn", support = true)
            assertFalse("reply ${turn + 2} repeats the helpline", state.messages.last().supportResources)
        }
        vm.send("i still feel like giving up")
        chat.answer("I'm really glad you told me.", support = true)
        assertTrue(state.messages.last().supportResources)
        assertEquals(listOf(true, false, false, false, true), state.messages.filter { !it.fromUser }.map { it.supportResources })
        assertTrue(state.messages.filter { it.fromUser }.none { it.supportResources })
    }

    @Test fun supportFlagOnlyLooksAtThePreviousThreeReplies() {
        vm.send("weeks of bad sleep")
        chat.answer("That's a long stretch.", support = true)
        vm.send("work too")
        chat.answer("That's a lot at once.")
        vm.send("and home")
        chat.answer("No wonder you're tired.")
        vm.send("i feel like i'm drowning")
        chat.answer("I hear you.", support = true)
        assertFalse("the flagged reply is still among the previous three", state.messages.last().supportResources)
        vm.send("it's the same")
        chat.answer("Still here with you.", support = true)
        assertTrue("the last flagged reply was four replies ago", state.messages.last().supportResources)
        assertEquals(listOf(true, false, false, false, true), state.messages.filter { !it.fromUser }.map { it.supportResources })
    }

    @Test fun ordinaryRepliesAreNeverFlaggedAndANewConversationStartsFresh() {
        repeat(3) { turn ->
            vm.send("message $turn")
            chat.answer("reply $turn")
        }
        assertFalse(state.supportMode)

        vm.send("it's all too much")
        chat.answer("That sounds so heavy.", support = true)
        assertTrue(state.supportMode)

        vm.newConversation()
        assertFalse(state.supportMode)
        vm.send("it's back again")
        chat.answer("I'm really glad you told me.", support = true)
        assertTrue(state.messages.last().supportResources)

        vm.chooseMood(Mood.SAD)
        assertFalse(state.supportMode)
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

        vm.newConversation()
        vm.send("one more thing")
        chat.answer("Go on.")
        vm.send("never mind")
        chat.answer("That's okay.")
        assertTrue(state.feelingCheckDue)
    }

    @Test fun feelingCheckIsDueWhileTheSecondReplyLoadsAndIsNeverAskedTwice() {
        vm.send("work is too much")
        assertFalse("nothing to ask about before the first reply", state.feelingCheckDue)
        chat.answer("What's piling up the most?")
        vm.send("deadlines")
        assertTrue(state.loading)
        assertTrue("leaving now would otherwise skip the question", state.feelingCheckDue)

        vm.recordFeeling(Feeling.LIGHTER)
        assertFalse(state.feelingCheckDue)
        chat.answer("Which one is closest?")
        assertFalse(state.feelingCheckDue)
        assertEquals(1, preferences.checkIns.size)

        vm.send("the report")
        chat.fail(IOException("offline"))
        assertFalse(state.feelingCheckDue)
        vm.retry()
        assertTrue(state.loading)
        assertFalse("already answered in this conversation", state.feelingCheckDue)
    }

    @Test fun failedSecondReplyWithNoAnswerIsNotDueYet() {
        vm.send("work is too much")
        chat.answer("What's piling up the most?")
        vm.send("deadlines")
        assertTrue(state.feelingCheckDue)
        chat.fail(IOException("offline"))
        assertFalse(state.feelingCheckDue)

        vm.retry()
        assertTrue(state.feelingCheckDue)
        chat.answer("Which one is closest?")
        assertTrue(state.feelingCheckDue)
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

    @Test fun clearDeviceDataForgetsConversationMoodAndCheckInsButKeepsSettingsAndReminder() {
        vm.setReminder(true, 9, 0)
        vm.setReadAloud(false)
        preferences.checkInStats.value = CheckInStats(4, 2)
        chat.ignoresCancellation = true
        vm.chooseMood(Mood.SAD, 6)
        vm.send("i miss her")
        chat.answer("I'm so sorry. Tell me about her?")
        vm.send("she was my best friend")
        assertTrue(state.loading)

        vm.clearDeviceData()
        assertTrue(state.messages.isEmpty())
        assertFalse(state.loading)
        assertFalse(state.feelingCheckDue)
        assertEquals(Mood.TALK, state.mood)
        assertNull(state.intensity)
        assertEquals(1, preferences.moodClears)
        assertEquals(Mood.TALK, preferences.mood.value)
        assertEquals(1, preferences.clears)
        assertEquals(CheckInStats(), state.checkInStats)
        assertEquals(listOf("schedule 9:0"), reminders.events)
        assertEquals(ReminderSettings(true, 9, 0), state.reminder)
        assertEquals(ReminderSettings(true, 9, 0), preferences.reminder.value)
        assertFalse(state.settings.readAloud)

        chat.answer("reply that arrived after clearing")
        assertTrue(state.messages.isEmpty())
    }

    @Test fun clearLocalDataTurnsOffTheReminderAndClearsCheckIns() {
        vm.setReminder(true, 22, 0)
        preferences.checkInStats.value = CheckInStats(6, 1)
        vm.clearLocalData()
        assertEquals("cancel", reminders.events.last())
        assertEquals(ReminderSettings(false, 22, 0), preferences.reminder.value)
        assertEquals(ReminderSettings(false, 22, 0), state.reminder)
        assertEquals(1, preferences.clears)
        assertEquals(CheckInStats(), state.checkInStats)
    }

    @Test fun reportingAReplySendsItAndSaysThanks() {
        vm.send("hi")
        chat.answer("Some reply that missed the mark.")
        vm.reportReply("Some reply that missed the mark.", ReportReason.HURTFUL)
        assertEquals(listOf(ReportReason.HURTFUL to "Some reply that missed the mark."), chat.reports)
        assertTrue(state.reportNotice.orEmpty().startsWith("Thanks for telling us"))
        assertNull(state.error)

        vm.dismissReportNotice()
        assertNull(state.reportNotice)
    }

    @Test fun failedReportSaysSoAndNoticeClearsWithANewConversation() {
        chat.reportFailure = IOException("offline")
        vm.reportReply("a reply", ReportReason.WRONG)
        assertTrue(chat.reports.isEmpty())
        assertTrue(state.reportNotice.orEmpty().startsWith("Couldn't send"))
        assertNull(state.error)

        vm.chooseMood(Mood.TALK)
        assertNull(state.reportNotice)
    }

    @Test fun reportLimitShowsTheServersOwnSentence() {
        val limit = "Thanks, we already have your recent reports. Please try again a little later."
        chat.reportFailure = HttpException(Response.error<Any>(429, """{"error":"$limit"}""".toResponseBody("application/json".toMediaType())))
        vm.reportReply("a reply", ReportReason.OTHER)
        assertEquals(limit, state.reportNotice)
    }

    @Test fun signOutAlwaysClearsThisPhonesDataAndRefreshesAuth() {
        vm.setReminder(true, 9, 0)
        preferences.checkInStats.value = CheckInStats(3, 2)
        vm.send("hi")
        auth.signOutFailure = IllegalStateException("credential clean-up failed")
        var done = 0
        vm.signOut { done++ }
        assertEquals(1, done)
        assertFalse(state.signedIn)
        assertEquals("cancel", reminders.events.last())
        assertEquals(ReminderSettings(false, 9, 0), preferences.reminder.value)
        assertEquals(1, preferences.clears)
        assertTrue(state.messages.isEmpty())
        assertFalse(state.loading)
    }

    @Test fun deleteAccountKeepsLocalDataUntilTheAccountIsGone() {
        vm.setReminder(true, 9, 0)
        vm.chooseMood(Mood.LONELY, 4)
        val recentLogin = "For your security, please sign out, sign in again, and then delete your account."
        auth.deleteFailure = FriendlyException(recentLogin)
        var error: String? = null
        var done = 0
        vm.deleteAccount(onDone = { done++ }, onError = { error = it })
        assertEquals(recentLogin, error)
        assertEquals(0, done)
        assertTrue(state.signedIn)
        assertEquals(ReminderSettings(true, 9, 0), state.reminder)
        assertEquals(0, preferences.clears)
        assertEquals(0, preferences.moodClears)
        assertEquals(Mood.LONELY, state.mood)

        auth.deleteFailure = null
        vm.deleteAccount(onDone = { done++ }, onError = { error = it })
        assertEquals(1, done)
        assertFalse(state.signedIn)
        assertEquals(ReminderSettings(false, 9, 0), state.reminder)
        assertEquals(1, preferences.clears)
        assertEquals(1, preferences.moodClears)
        assertEquals(Mood.TALK, state.mood)
        assertNull(state.intensity)
    }

    @Test fun clearCheckInsResetsStats() {
        preferences.checkInStats.value = CheckInStats(4, 2)
        vm.clearCheckIns()
        assertEquals(1, preferences.clears)
        assertEquals(CheckInStats(), state.checkInStats)
    }
}
