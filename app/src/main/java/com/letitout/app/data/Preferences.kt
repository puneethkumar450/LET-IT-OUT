package com.letitout.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("preferences")

@Singleton
class MoodPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private val moodKey = stringPreferencesKey("selected_mood")
    val mood: Flow<Mood> = context.dataStore.data.map { values ->
        Mood.entries.firstOrNull { it.name == values[moodKey] } ?: Mood.TALK
    }
    suspend fun setMood(mood: Mood) {
        context.dataStore.edit { it[moodKey] = mood.name }
    }
}
