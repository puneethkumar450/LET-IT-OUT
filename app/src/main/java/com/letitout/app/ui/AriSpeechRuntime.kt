package com.letitout.app.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Keeps the device voice initialized across the welcome and chat screens. */
internal class AriSpeechRuntime(context: Context) {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var closed = false

    var ready by mutableStateOf(false)
        private set

    fun prepare(): TextToSpeech {
        check(!closed)
        engine?.let { return it }
        return TextToSpeech(appContext) { status ->
            main.post { if (!closed) ready = status == TextToSpeech.SUCCESS }
        }.also { engine = it }
    }

    fun close() {
        closed = true
        ready = false
        engine?.stop()
        engine?.shutdown()
        engine = null
    }
}
