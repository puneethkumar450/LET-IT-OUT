package com.letitout.app.data

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// A corrupt file is reset rather than failing every read and write.
private val Context.dataStore by preferencesDataStore("preferences", corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() })

/** Everything here stays on the phone. Check-ins are kept as the last 20 "MOOD:FEELING" tokens. */
@Singleton
class DataStoreUserPreferences @Inject constructor(@ApplicationContext private val context: Context) : UserPreferences {
    private val moodKey = stringPreferencesKey("selected_mood")
    private val readAloudKey = booleanPreferencesKey("read_aloud")
    private val voiceInputKey = booleanPreferencesKey("voice_input")
    private val followMoodKey = booleanPreferencesKey("follow_mood_theme")
    private val onboardedKey = booleanPreferencesKey("onboarding_done")
    private val checkInsKey = stringPreferencesKey("check_ins")
    private val reminderEnabledKey = booleanPreferencesKey("reminder_enabled")
    private val reminderHourKey = intPreferencesKey("reminder_hour")
    private val reminderMinuteKey = intPreferencesKey("reminder_minute")

    private val data = context.dataStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }

    override val mood: Flow<Mood> = data.map { values ->
        Mood.entries.firstOrNull { it.name == values[moodKey] } ?: Mood.TALK
    }.distinctUntilChanged()
    override val settings: Flow<AppSettings> = data.map { values ->
        val defaults = AppSettings()
        AppSettings(
            readAloud = values[readAloudKey] ?: defaults.readAloud,
            voiceInput = values[voiceInputKey] ?: defaults.voiceInput,
            followMoodTheme = values[followMoodKey] ?: defaults.followMoodTheme,
            onboarded = values[onboardedKey] ?: defaults.onboarded
        )
    }.distinctUntilChanged()

    override suspend fun setMood(mood: Mood) = save { it[moodKey] = mood.name }
    override suspend fun clearMood() = save { it.remove(moodKey) }
    override suspend fun setReadAloud(enabled: Boolean) = set(readAloudKey, enabled)
    override suspend fun setVoiceInput(enabled: Boolean) = set(voiceInputKey, enabled)
    override suspend fun setFollowMoodTheme(enabled: Boolean) = set(followMoodKey, enabled)
    override suspend fun setOnboarded() = set(onboardedKey, true)

    override val checkInStats: Flow<CheckInStats> = data.map { values ->
        val recent = checkInTokens(values[checkInsKey]).takeLast(10)
        CheckInStats(total = recent.size, lighter = recent.count { it.endsWith(":${Feeling.LIGHTER.name}") })
    }.distinctUntilChanged()

    override suspend fun recordCheckIn(mood: Mood, feeling: Feeling) = save { values ->
        val tokens = checkInTokens(values[checkInsKey]) + "${mood.name}:${feeling.name}"
        values[checkInsKey] = tokens.takeLast(20).joinToString(",")
    }

    override suspend fun clearCheckIns() = save { it.remove(checkInsKey) }

    override val reminder: Flow<ReminderSettings> = data.map { values ->
        val defaults = ReminderSettings()
        ReminderSettings(
            enabled = values[reminderEnabledKey] ?: defaults.enabled,
            hour = values[reminderHourKey] ?: defaults.hour,
            minute = values[reminderMinuteKey] ?: defaults.minute
        )
    }.distinctUntilChanged()

    override suspend fun setReminder(settings: ReminderSettings) = save {
        it[reminderEnabledKey] = settings.enabled
        it[reminderHourKey] = settings.hour.coerceIn(0, 23)
        it[reminderMinuteKey] = settings.minute.coerceIn(0, 59)
    }

    private suspend fun set(key: Preferences.Key<Boolean>, value: Boolean) = save { it[key] = value }

    // Writes are best-effort: a full or failing disk must not crash the app over an on-device preference.
    private suspend fun save(transform: suspend (MutablePreferences) -> Unit) {
        try {
            context.dataStore.edit(transform)
        } catch (ignored: IOException) {
        }
    }

    private fun checkInTokens(raw: String?): List<String> =
        raw.orEmpty().split(',').filter { token -> Feeling.entries.any { token.endsWith(":${it.name}") } }
}
