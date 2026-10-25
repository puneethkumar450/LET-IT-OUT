package com.letitout.app.data

import android.content.Context
import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class MoodPreferencesInstrumentedTest {
    @Test fun choicesPersistAcrossInstancesAndAccountResetClearsAllLocalPreferences() = runBlocking {
        // Instrumentation runs under the target app UID, so use a separate writable cache directory.
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val isolatedFiles = File(targetContext.cacheDir, "instrumented-preferences").apply { mkdirs() }
        val context = object : ContextWrapper(targetContext) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = isolatedFiles
        }
        val preferences = MoodPreferences(context)
        preferences.clearAll()

        try {
            preferences.setMood(Mood.ANGRY)
            preferences.setCompanion("man", "mature")
            preferences.setReadAloud(false)
            preferences.setVoiceInput(false)
            preferences.setFollowMoodTheme(false)
            preferences.setOnboarded()

            val reopened = MoodPreferences(context)
            assertEquals(Mood.ANGRY, reopened.mood.first())
            assertEquals(CompanionChoice("man", "mature"), reopened.companion.first())
            assertFalse(reopened.settings.first().readAloud)
            assertFalse(reopened.settings.first().voiceInput)
            assertFalse(reopened.settings.first().followMoodTheme)
            assertTrue(reopened.settings.first().onboarded)

            reopened.clearAll()
            assertEquals(Mood.TALK, preferences.mood.first())
            assertEquals(CompanionChoice(), preferences.companion.first())
            assertTrue(preferences.settings.first().readAloud)
            assertTrue(preferences.settings.first().voiceInput)
            assertTrue(preferences.settings.first().followMoodTheme)
            assertFalse(preferences.settings.first().onboarded)
        } finally {
            preferences.clearAll()
        }
    }
}
