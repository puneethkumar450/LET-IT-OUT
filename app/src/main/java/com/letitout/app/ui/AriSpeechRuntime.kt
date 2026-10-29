package com.letitout.app.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

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

/** Ari speaks a little slower than the engine default, for a calmer pace. */
internal const val ARI_SPEECH_RATE = 0.95f
internal const val ARI_PREVIEW_TEXT = "Hi, I'm Ari. I'm here to listen, whenever you're ready."

/** An installed on-device voice Ari can use. [label] is what Settings shows. */
internal data class VoiceOption(val name: String, val locale: Locale, val quality: Int, val label: String)

/**
 * Installed, offline voices in the phone's language, best first: the phone's own country, then
 * higher quality, then name. Voices that need the network are left out so reply text stays on
 * the device.
 */
internal fun ariVoiceOptions(voices: Collection<RawVoice>, phoneLocale: Locale): List<VoiceOption> =
    voices.asSequence()
        .filter { it.installed && !it.needsNetwork && it.locale.language == phoneLocale.language }
        .sortedWith(
            compareByDescending<RawVoice> { it.locale.country == phoneLocale.country }
                .thenByDescending { it.quality }
                .thenBy { it.name }
        )
        .toList()
        .mapIndexed { index, voice ->
            VoiceOption(voice.name, voice.locale, voice.quality, "Voice ${index + 1} · ${voice.locale.getDisplayName(phoneLocale)}")
        }

/** The saved voice if it is still installed, otherwise Ari's default: the first option. */
internal fun resolveAriVoice(options: List<VoiceOption>, saved: String?): VoiceOption? =
    options.firstOrNull { it.name == saved } ?: options.firstOrNull()

/** The parts of an Android [Voice] the choice depends on, so the rules can be unit tested. */
internal data class RawVoice(val name: String, val locale: Locale, val quality: Int, val needsNetwork: Boolean, val installed: Boolean)

internal fun Voice.toRaw() = RawVoice(
    name, locale, quality, isNetworkConnectionRequired,
    installed = TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in features.orEmpty()
)

/** Ari's voice options on this phone; empty while the engine is starting or has no voices. */
internal fun TextToSpeech.ariVoiceOptions(phoneLocale: Locale = Locale.getDefault()): List<VoiceOption> =
    ariVoiceOptions(runCatching { voices.orEmpty().map { it.toRaw() } }.getOrDefault(emptyList()), phoneLocale)

/** Sets the language, Ari's voice ([saved] or the default) and pace before speaking. */
internal fun TextToSpeech.useAriVoice(saved: String?) {
    language = Locale.getDefault()
    val chosen = resolveAriVoice(ariVoiceOptions(), saved)
    runCatching { voices.orEmpty().firstOrNull { it.name == chosen?.name } }.getOrNull()?.let { voice = it }
    setSpeechRate(ARI_SPEECH_RATE)
}
