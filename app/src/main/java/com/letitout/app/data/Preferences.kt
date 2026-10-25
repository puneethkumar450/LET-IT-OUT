package com.letitout.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("preferences")

data class AppSettings(
    val readAloud: Boolean = true,
    val voiceInput: Boolean = true,
    val followMoodTheme: Boolean = true,
    val onboarded: Boolean = false
)

/** A visual preference; every age option is an adult portrait style. */
data class CompanionChoice(val gender: String = "woman", val age: String = "adult") {
    companion object {
        val genders = setOf("woman", "man")
        val ages = setOf("young", "adult", "mature")
    }
}

@Singleton
class MoodPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private val moodKey = stringPreferencesKey("selected_mood")
    private val companionGenderKey = stringPreferencesKey("companion_gender")
    private val companionAgeKey = stringPreferencesKey("companion_age")
    private val readAloudKey = booleanPreferencesKey("read_aloud")
    private val voiceInputKey = booleanPreferencesKey("voice_input")
    private val followMoodKey = booleanPreferencesKey("follow_mood_theme")
    private val onboardedKey = booleanPreferencesKey("onboarding_done")

    val mood: Flow<Mood> = context.dataStore.data.map { values ->
        Mood.entries.firstOrNull { it.name == values[moodKey] } ?: Mood.TALK
    }
    val companion: Flow<CompanionChoice> = context.dataStore.data.map { values ->
        CompanionChoice(
            gender = values[companionGenderKey]?.takeIf { it in CompanionChoice.genders } ?: "woman",
            age = values[companionAgeKey]?.takeIf { it in CompanionChoice.ages } ?: "adult"
        )
    }
    val settings: Flow<AppSettings> = context.dataStore.data.map { values ->
        AppSettings(
            readAloud = values[readAloudKey] ?: true,
            voiceInput = values[voiceInputKey] ?: true,
            followMoodTheme = values[followMoodKey] ?: true,
            onboarded = values[onboardedKey] ?: false
        )
    }

    suspend fun setMood(mood: Mood) {
        context.dataStore.edit { it[moodKey] = mood.name }
    }
    suspend fun setCompanion(gender: String, age: String) {
        require(gender in CompanionChoice.genders && age in CompanionChoice.ages)
        context.dataStore.edit { values ->
            values[companionGenderKey] = gender
            values[companionAgeKey] = age
        }
    }
    suspend fun clearMood() {
        context.dataStore.edit { it.remove(moodKey) }
    }
    /** Remove every preference associated with this installation after account deletion. */
    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
    suspend fun setReadAloud(enabled: Boolean) = set(readAloudKey, enabled)
    suspend fun setVoiceInput(enabled: Boolean) = set(voiceInputKey, enabled)
    suspend fun setFollowMoodTheme(enabled: Boolean) = set(followMoodKey, enabled)
    suspend fun setOnboarded() = set(onboardedKey, true)

    private suspend fun set(key: Preferences.Key<Boolean>, value: Boolean) {
        context.dataStore.edit { it[key] = value }
    }
}
