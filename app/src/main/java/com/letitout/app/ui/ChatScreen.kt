package com.letitout.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.media.AudioFormat
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.letitout.app.data.ChatMessage
import com.letitout.app.data.COMPANION_NAME
import com.letitout.app.data.Mood
import com.letitout.app.data.MoodPalette
import com.letitout.app.data.palette
import com.letitout.app.BuildConfig
import com.letitout.app.R
import java.io.File
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.launch

@Composable
internal fun ChatScreen(
    vm: AppViewModel,
    speechRuntime: AriSpeechRuntime,
    liveController: LiveAvatarController,
    openLiveOnEntry: Boolean,
    onHome: () -> Unit,
    onHumanSupport: () -> Unit,
    onSignIn: () -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val online by rememberIsOnline()
    var menuOpen by remember { mutableStateOf(false) }
    var speechError by remember { mutableStateOf<String?>(null) }
    var isListening by remember { mutableStateOf(false) }
    val speechEnabled = speechRuntime.ready
    var speakingId by remember { mutableStateOf<Long?>(null) }
    var liveRequested by remember { mutableStateOf(false) }
    var liveToken by remember { mutableStateOf<String?>(null) }
    var liveReady by remember { mutableStateOf(false) }
    var liveError by remember { mutableStateOf<String?>(null) }
    var liveStartId by remember { mutableStateOf(0L) }
    var autoStartAttempted by rememberSaveable { mutableStateOf(false) }
    var liveOpenedAfter by remember { mutableStateOf(0L) }
    var lastAutoSpokenId by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()
    val main = remember { Handler(Looper.getMainLooper()) }
    val liveUtterance = remember { AtomicReference<LiveUtterance?>(null) }
    val currentSpeechId = remember { AtomicReference<String?>(null) }
    val speechSerial = remember { AtomicLong(0L) }
    val tts = remember(speechRuntime) { speechRuntime.prepare() }
    DisposableEffect(tts) {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                if (liveUtterance.get()?.id != utteranceId) main.post {
                    if (currentSpeechId.get() == utteranceId) speakingId = utteranceId?.substringBefore(':')?.toLongOrNull()
                }
            }
            override fun onBeginSynthesis(utteranceId: String?, sampleRateInHz: Int, audioFormat: Int, channelCount: Int) {
                liveUtterance.get()?.takeIf { it.id == utteranceId }?.let { utterance ->
                    utterance.resampler = if (audioFormat == AudioFormat.ENCODING_PCM_16BIT) {
                        runCatching { Pcm16Resampler(sampleRateInHz, channelCount) }.getOrNull()
                    } else null
                }
            }
            override fun onAudioAvailable(utteranceId: String?, audio: ByteArray?) {
                val utterance = liveUtterance.get()?.takeIf { it.id == utteranceId } ?: return
                val converter = utterance.resampler ?: return
                if (audio != null) synchronized(converter) {
                    val pcm = converter.convert(audio)
                    if (pcm.isNotEmpty()) {
                        utterance.sentAudio = true
                        liveController.sendAudio(pcm)
                    }
                }
            }
            override fun onDone(utteranceId: String?) {
                val live = liveUtterance.get()?.takeIf { it.id == utteranceId }
                live?.file?.delete()
                main.post {
                    if (live != null) {
                        if ((live.resampler == null || !live.sentAudio) && liveUtterance.compareAndSet(live, null)) {
                            tts.language = Locale.getDefault()
                            tts.speak(live.message.text, TextToSpeech.QUEUE_FLUSH, null, live.id)
                        } else if (liveUtterance.get() === live) liveController.finishAudio()
                    } else if (currentSpeechId.compareAndSet(utteranceId, null)) speakingId = null
                }
            }
            @Suppress("OVERRIDE_DEPRECATION")
            override fun onError(utteranceId: String?) {
                main.post {
                    liveUtterance.get()?.takeIf { it.id == utteranceId }?.let {
                        if (liveUtterance.compareAndSet(it, null)) it.file.delete()
                    }
                    if (currentSpeechId.compareAndSet(utteranceId, null)) {
                        speakingId = null
                        speechError = "Voice playback is unavailable right now."
                    }
                }
            }
            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                main.post { if (currentSpeechId.compareAndSet(utteranceId, null)) speakingId = null }
            }
        })
        onDispose {
            liveUtterance.getAndSet(null)?.file?.delete()
            tts.stop()
            tts.setOnUtteranceProgressListener(null)
        }
    }
    val stopSpeaking: () -> Unit = {
        currentSpeechId.set(null)
        liveUtterance.getAndSet(null)?.file?.delete()
        liveController.clear()
        tts.stop()
        speakingId = null
    }
    val stopLive: () -> Unit = {
        liveStartId += 1
        stopSpeaking()
        liveRequested = false
        liveReady = false
        liveToken = null
    }
    val endConversation: () -> Unit = {
        menuOpen = false
        stopLive()
        vm.newConversation()
        onHome()
    }
    val startLive: () -> Unit = {
        val requestId = ++liveStartId
        liveOpenedAfter = state.messages.maxOfOrNull { it.id } ?: 0L
        liveRequested = true
        liveError = null
        scope.launch {
            try {
                val token = vm.avatarSessionToken(if (state.settings.followMoodTheme) state.mood else Mood.TALK)
                if (liveRequested && liveStartId == requestId) liveToken = token
            } catch (_: Exception) {
                if (liveRequested && liveStartId == requestId) {
                    stopLive()
                    liveError = "Talking avatar is unavailable. The portrait and device voice still work."
                }
            }
        }
    }
    val speakMessage: (ChatMessage) -> Unit = { message ->
        if (speechEnabled) {
            stopSpeaking()
            tts.language = Locale.getDefault()
            val utteranceId = "${message.id}:${speechSerial.incrementAndGet()}"
            currentSpeechId.set(utteranceId)
            if (liveReady && !message.supportResources) {
                val file = runCatching { File.createTempFile("avatar-reply-", ".wav", context.cacheDir) }.getOrNull()
                if (file == null) tts.speak(message.text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                else {
                    val utterance = LiveUtterance(message, file, utteranceId)
                    liveUtterance.set(utterance)
                    speakingId = message.id
                    if (tts.synthesizeToFile(message.text, null, file, utterance.id) != TextToSpeech.SUCCESS) {
                        liveUtterance.set(null)
                        file.delete()
                        tts.speak(message.text, TextToSpeech.QUEUE_FLUSH, null, utterance.id)
                    }
                }
            } else tts.speak(message.text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } else {
            speechError = "Read aloud is unavailable on this device."
        }
    }
    val transcriptReceived = androidx.compose.runtime.rememberUpdatedState<(String) -> Unit> { transcript ->
        if (liveRequested && transcript.isNotBlank() && state.signedIn && !state.loading) {
            stopSpeaking()
            vm.send(transcript)
            input = ""
        } else input = transcript
    }
    val recognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null
    }
    DisposableEffect(recognizer) {
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { isListening = true }
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) { isListening = false; speechError = "Speech couldn't be recognized. You can type instead." }
            override fun onResults(results: Bundle?) {
                isListening = false
                val transcript = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                main.post { transcriptReceived.value(transcript) }
            }
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        onDispose { recognizer?.destroy() }
    }
    val startListening: () -> Unit = {
        speechError = null
        if (recognizer == null) speechError = "Speech recognition is unavailable on this device."
        else recognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
        })
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startListening() else speechError = "Microphone permission denied. You can still type."
    }
    val toggleMic: () -> Unit = {
        if (isListening) { recognizer?.stopListening(); isListening = false }
        else {
            if (liveRequested) stopSpeaking()
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startListening()
            else micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    val canSend = state.signedIn && input.isNotBlank() && !state.loading
    val submitMessage: () -> Unit = {
        if (canSend) {
            stopSpeaking()
            vm.send(input)
            input = ""
            focusManager.clearFocus()
            keyboard?.hide()
        }
    }

    // Read new replies aloud, but never ones that were already on screen when this page opened.
    val seenUpTo = remember { state.messages.maxOfOrNull { it.id } ?: 0L }
    val latest = state.messages.lastOrNull()
    LaunchedEffect(latest?.id, speechEnabled, liveRequested) {
        if (latest != null && !latest.fromUser && latest.id > seenUpTo && latest.id != lastAutoSpokenId &&
            (state.settings.readAloud || (liveRequested && latest.id > liveOpenedAfter)) && speechEnabled) {
            lastAutoSpokenId = latest.id
            speakMessage(latest)
        }
    }
    val itemCount = 2 + state.messages.size + (if (state.loading) 1 else 0) + (if (state.error != null) 1 else 0)
    LaunchedEffect(itemCount) { listState.animateScrollToItem(itemCount - 1) }
    BackHandler { if (liveRequested) stopLive() else onHome() }

    val supportMode = state.supportMode
    LaunchedEffect(supportMode) { if (supportMode && liveRequested) stopLive() }
    LaunchedEffect(state.signedIn, supportMode) {
        if (openLiveOnEntry && BuildConfig.LIVE_AVATAR_PILOT && state.signedIn && !supportMode && !autoStartAttempted) {
            autoStartAttempted = true
            startLive()
        }
    }
    val palette = if (supportMode || !state.settings.followMoodTheme) Mood.TALK.palette() else state.mood.palette()
    Box(Modifier.fillMaxSize().background(palette.surface)) {
        if (liveRequested) {
            Box(Modifier.fillMaxSize().background(palette.surface)) {
                Image(
                    painter = painterResource(R.drawable.ari_onboarding_fullscreen),
                    contentDescription = "Ari, your AI companion",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                liveToken?.let { token ->
                    LiveAvatarView(
                        sessionToken = token,
                        controller = liveController,
                        backgroundColor = palette.surface,
                        modifier = Modifier.fillMaxSize().zIndex(1f),
                        onReady = { if (liveRequested && liveToken == token) liveReady = true },
                        onFailure = {
                            if (liveRequested && liveToken == token) {
                                val interrupted = liveUtterance.getAndSet(null)
                                interrupted?.file?.delete()
                                if (interrupted != null && !interrupted.videoStarted) {
                                    tts.stop()
                                    val fallbackId = "${interrupted.message.id}:${speechSerial.incrementAndGet()}"
                                    currentSpeechId.set(fallbackId)
                                    tts.speak(interrupted.message.text, TextToSpeech.QUEUE_FLUSH, null, fallbackId)
                                } else if (interrupted != null) stopSpeaking()
                                liveRequested = false
                                liveReady = false
                                liveToken = null
                                liveError = "Talking avatar disconnected. The portrait and device voice still work."
                            }
                        },
                        onSpeaking = { liveUtterance.get()?.videoStarted = true },
                        onSilent = {
                            liveUtterance.get()?.takeIf { it.videoStarted }?.let {
                                if (liveUtterance.compareAndSet(it, null)) {
                                    it.file.delete()
                                    if (currentSpeechId.compareAndSet(it.id, null)) speakingId = null
                                }
                            }
                        }
                    )
                }
                Column(
                    Modifier.fillMaxSize().zIndex(3f).statusBarsPadding().navigationBarsPadding()
                        .imePadding().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SquareIconButton(AppIcons.Back, "View chat summary", stopLive, shadowColor = palette.accent)
                        ChipButton("End chat", endConversation, AppColors.Surface, AppColors.Danger)
                    }
                    Spacer(Modifier.weight(1f))
                    if (!liveReady) StatusPill("Connecting avatar…", palette.accent)
                    if (!online) StatusPill("No internet connection", AppColors.Warning)
                    speechError?.let { StatusPill(it, AppColors.Danger) }
                    state.error?.let { StatusPill(it, AppColors.Danger) }
                    if (isListening) StatusPill("Listening…", palette.accent)
                    if (!state.signedIn) {
                        Row(
                            Modifier.fillMaxWidth().background(AppColors.Surface, RoundedCornerShape(18.dp))
                                .padding(start = 16.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                        Text("Sign in to talk to $COMPANION_NAME.", Modifier.weight(1f), color = AppColors.Muted, fontSize = 13.sp)
                            LinkButton("Sign in", AppColors.Primary, onSignIn)
                        }
                    }
                    Composer(
                        input = input, onInputChange = { input = it }, palette = palette,
                        placeholder = "Type or speak to $COMPANION_NAME…",
                        signedIn = state.signedIn, canSend = canSend, showMic = true, isListening = isListening,
                        onMic = toggleMic, onSend = submitMessage,
                        onDone = { focusManager.clearFocus(); keyboard?.hide() }
                    )
                }
            }
        } else Column(Modifier.fillMaxSize().navigationBarsPadding().imePadding()) {
            Column(
                Modifier.fillMaxWidth().background(
                    Brush.verticalGradient(listOf(palette.tile, palette.surface))
                ).statusBarsPadding().padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SquareIconButton(AppIcons.Back, "Back to home", onHome, shadowColor = palette.accent)
                    Surface(
                        shape = CircleShape,
                        color = palette.bubble,
                        modifier = Modifier.size(48.dp).semantics { contentDescription = "Ari, your companion" }
                    ) {
                        CompanionPortrait(
                            modifier = Modifier.padding(3.dp).clip(CircleShape)
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(COMPANION_NAME, style = DisplayStyle, fontSize = 18.sp, maxLines = 1)
                        Text(
                            if (supportMode) "Support options" else "Listening with care",
                            color = AppColors.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                    Box {
                        SquareIconButton(AppIcons.More, "More options", { menuOpen = true }, shadowColor = palette.accent)
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, shape = RoundedCornerShape(16.dp)) {
                            if (BuildConfig.LIVE_AVATAR_PILOT && state.signedIn && !supportMode) {
                                DropdownMenuItem(
                                    text = { Text("Return to live avatar") },
                                    onClick = { menuOpen = false; startLive() }
                                )
                            }
                            HorizontalDivider(color = palette.accent.copy(alpha = .12f))
                            DropdownMenuItem(
                                text = { Text("Human support") },
                                leadingIcon = { Icon(AppIcons.Shield, contentDescription = null) },
                                onClick = { menuOpen = false; onHumanSupport() }
                            )
                            HorizontalDivider(color = palette.accent.copy(alpha = .12f))
                            DropdownMenuItem(text = { Text("New conversation") }, onClick = { menuOpen = false; stopLive(); vm.newConversation() })
                            DropdownMenuItem(text = { Text("End conversation") }, onClick = endConversation)
                        }
                    }
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, top = 15.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom)
            ) {
                item {
                    Text(
                        "Today · Chat isn't saved on this device", Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        color = AppColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                    )
                }
                item { AiBubble(palette.accent) { BubbleText(state.mood.prompt) } }
                items(state.messages, key = { it.id }) { message ->
                    val speakingFooter: (@Composable () -> Unit)? =
                        if (speakingId == message.id) { { SpeakingFooter(palette.accent, stopSpeaking) } } else null
                    when {
                        message.fromUser -> UserBubble(message.text, palette)
                        message.supportResources -> SupportBubble(message.text, footer = speakingFooter) { dialNumber(context, it) }
                        else -> AiBubble(
                            palette.accent,
                            footer = speakingFooter ?: { ListenFooter(palette.bubble, palette.accent) { speakMessage(message) } }
                        ) { BubbleText(message.text) }
                    }
                }
                if (state.loading) item { AiBubble(palette.accent) { TypingDots(palette.accent) } }
                state.error?.let { error ->
                    item {
                        ErrorBubble(
                            error, palette, canEdit = state.messages.lastOrNull()?.fromUser == true,
                            onRetry = vm::retry, onEdit = { vm.editLastMessage()?.let { input = it } }
                        )
                    }
                }
            }
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!online) StatusPill("No internet connection", AppColors.Warning)
                speechError?.let { StatusPill(it, AppColors.Danger) }
                liveError?.let { StatusPill(it, AppColors.Danger) }
                if (!state.signedIn) {
                    Row(
                        Modifier.fillMaxWidth().background(AppColors.Surface, RoundedCornerShape(18.dp)).padding(start = 16.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Preview mode · sign in to send messages.", Modifier.weight(1f), color = AppColors.Muted, fontSize = 13.sp, lineHeight = 18.sp)
                        LinkButton("Sign in", AppColors.Primary, onSignIn)
                    }
                }
                Composer(
                    input = input, onInputChange = { input = it }, palette = palette,
                    placeholder = if (supportMode) "Keep talking, if you'd like…" else "Say what's on your mind…",
                    signedIn = state.signedIn, canSend = canSend, showMic = state.settings.voiceInput, isListening = isListening,
                    onMic = toggleMic, onSend = submitMessage,
                    onDone = { focusManager.clearFocus(); keyboard?.hide() }
                )
            }
        }
    }
}

private class LiveUtterance(val message: ChatMessage, val file: File, val id: String) {
    @Volatile var resampler: Pcm16Resampler? = null
    @Volatile var videoStarted: Boolean = false
    @Volatile var sentAudio: Boolean = false
}

@Composable
private fun rememberIsOnline(): State<Boolean> {
    val context = LocalContext.current
    val online = remember { mutableStateOf(true) }
    DisposableEffect(context) {
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        online.value = connectivity.activeNetwork != null
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { online.value = true }
            override fun onLost(network: Network) { online.value = false }
        }
        connectivity.registerDefaultNetworkCallback(callback)
        onDispose { connectivity.unregisterNetworkCallback(callback) }
    }
    return online
}

@Composable
private fun BubbleText(text: String) {
    Text(text, color = AppColors.Ink, fontSize = 16.sp, lineHeight = 23.sp)
}

@Composable
private fun UserBubble(text: String, palette: MoodPalette) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Text(
            text, Modifier.widthIn(max = 280.dp).background(palette.bubble, RoundedCornerShape(22.dp, 22.dp, 6.dp, 22.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            color = AppColors.Ink, fontSize = 16.sp, lineHeight = 23.sp
        )
    }
}

@Composable
private fun AiAvatar() {
    CompanionPortrait(
        modifier = Modifier.size(46.dp).clip(CircleShape),
        contentDescription = null
    )
}

@Composable
private fun AiBubble(
    accent: Color,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(22.dp, 22.dp, 22.dp, 6.dp)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AiAvatar()
        Column(
            Modifier.widthIn(max = 300.dp).softShadow(accent, shape, 2.dp).background(AppColors.Surface, shape).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            content()
            footer?.let { HorizontalDivider(color = accent.copy(alpha = .15f)); it() }
        }
    }
}

@Composable
private fun SpeakingFooter(accent: Color, onStop: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                listOf(8.dp, 16.dp, 11.dp, 18.dp, 7.dp).forEach { height ->
                    Box(Modifier.size(3.dp, height).background(accent, RoundedCornerShape(2.dp)))
                }
            }
            Text("Speaking…", color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Surface(onClick = onStop, shape = CircleShape, color = AppColors.Surface, contentColor = accent, border = BorderStroke(1.5.dp, accent)) {
            Row(Modifier.heightIn(min = 36.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(AppIcons.Stop, contentDescription = null, tint = accent, modifier = Modifier.size(12.dp))
                Text("Stop", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ListenFooter(background: Color, accent: Color, onSpeak: () -> Unit) {
    Surface(onClick = onSpeak, shape = CircleShape, color = background, contentColor = accent) {
        Row(
            Modifier.heightIn(min = 40.dp).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(AppIcons.Speaker, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Text("Listen", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TypingDots(color: Color) {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(Modifier.padding(vertical = 6.dp).semantics { contentDescription = "Thinking" }, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = .25f, targetValue = 1f, label = "dot$index",
                animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse, StartOffset(index * 160))
            )
            Box(Modifier.size(8.dp).alpha(alpha).background(color, CircleShape))
        }
    }
}

private fun Modifier.dashedBorder(color: Color, shape: Shape, width: Dp = 1.5.dp) = drawWithContent {
    drawContent()
    drawOutline(
        shape.createOutline(size, layoutDirection, this), color,
        style = Stroke(width.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())))
    )
}

@Composable
private fun ErrorBubble(message: String, palette: MoodPalette, canEdit: Boolean, onRetry: () -> Unit, onEdit: () -> Unit) {
    val shape = RoundedCornerShape(22.dp, 22.dp, 22.dp, 6.dp)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AiAvatar()
        Column(
            Modifier.widthIn(max = 300.dp).background(AppColors.Surface, shape).dashedBorder(palette.accent.copy(alpha = .45f), shape)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(AppIcons.Retry, contentDescription = null, tint = palette.accent, modifier = Modifier.padding(top = 2.dp).size(20.dp))
                Text(message, color = AppColors.Ink, fontSize = 15.sp, lineHeight = 22.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallPillButton("Try again", onRetry, container = palette.accent, content = Color.White)
                if (canEdit) SmallPillButton("Edit message", onEdit, container = palette.surface, content = palette.accent)
            }
        }
    }
}

@Composable
private fun SupportBubble(text: String, footer: (@Composable () -> Unit)?, onDial: (String) -> Unit) {
    val shape = RoundedCornerShape(24.dp, 24.dp, 24.dp, 6.dp)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AiAvatar()
        Column(
            Modifier.weight(1f).softShadow(AppColors.Primary, shape, 4.dp).background(AppColors.Surface, shape).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BubbleText(text)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HelplineRow("Tele-MANAS · 14416", "Free, 24/7 mental health support (India)", filled = true) { onDial("14416") }
                HelplineRow("Emergency · 112", "If you're in immediate danger", filled = false) { onDial("112") }
            }
            Text(
                "Let It Go can't call anyone or alert a person for you. These numbers go to real people. I'm still here if you want to keep talking.",
                color = AppColors.Muted, fontSize = 13.sp, lineHeight = 19.sp
            )
            footer?.let { HorizontalDivider(color = AppColors.Lavender); it() }
        }
    }
}

@Composable
private fun HelplineRow(title: String, detail: String, filled: Boolean, onCall: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Mood.TALK.palette().surface, RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(detail, color = AppColors.Muted, fontSize = 13.sp, lineHeight = 18.sp)
        }
        val content = if (filled) Color.White else AppColors.Primary
        Surface(
            onClick = onCall, shape = CircleShape, color = if (filled) AppColors.Primary else AppColors.Surface, contentColor = content,
            border = if (filled) null else BorderStroke(1.5.dp, AppColors.Primary)
        ) {
            Row(Modifier.heightIn(min = 44.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(AppIcons.Phone, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
                Text("Call", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, dot: Color) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            Modifier.background(AppColors.Surface, CircleShape).heightIn(min = 32.dp).padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(Modifier.size(8.dp).background(dot, CircleShape))
            Text(text, color = AppColors.Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Composer(
    input: String, onInputChange: (String) -> Unit, palette: MoodPalette, placeholder: String,
    signedIn: Boolean, canSend: Boolean, showMic: Boolean, isListening: Boolean,
    onMic: () -> Unit, onSend: () -> Unit, onDone: () -> Unit
) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        Modifier.fillMaxWidth().softShadow(palette.accent, shape, 6.dp).background(AppColors.Surface, shape)
            .padding(start = 18.dp, top = 6.dp, end = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BasicTextField(
            value = input, onValueChange = onInputChange,
            modifier = Modifier.weight(1f).semantics { contentDescription = "Message" },
            textStyle = TextStyle(fontFamily = Nunito, fontSize = 16.sp, lineHeight = 22.sp, color = AppColors.Ink),
            cursorBrush = SolidColor(palette.accent), maxLines = 4,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = if (signedIn) ImeAction.Send else ImeAction.Done),
            keyboardActions = KeyboardActions(onSend = { onSend() }, onDone = { onDone() }),
            decorationBox = { field ->
                Box(Modifier.padding(vertical = 11.dp), contentAlignment = Alignment.CenterStart) {
                    if (input.isEmpty()) Text(placeholder, color = AppColors.Muted, fontSize = 16.sp)
                    field()
                }
            }
        )
        if (showMic) RoundIconButton(
            if (isListening) AppIcons.Stop else AppIcons.Mic,
            if (isListening) "Stop listening" else "Speak instead of typing",
            container = if (isListening) palette.accent else palette.surface,
            tint = if (isListening) Color.White else AppColors.Ink,
            enabled = signedIn, onClick = onMic
        )
        RoundIconButton(AppIcons.Send, "Send", container = palette.accent, tint = Color.White, enabled = canSend, onClick = onSend)
    }
}

@Composable
private fun RoundIconButton(icon: ImageVector, description: String, container: Color, tint: Color, enabled: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, enabled = enabled, shape = CircleShape, color = container, modifier = Modifier.size(44.dp).alpha(if (enabled) 1f else .45f)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(20.dp)) }
    }
}
