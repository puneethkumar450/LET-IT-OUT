package com.letitout.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.letitout.app.R
import com.letitout.app.data.Feeling
import com.letitout.app.data.Mood
import com.letitout.app.data.palette
import com.letitout.app.ui.AppColors
import com.letitout.app.ui.AppViewModel
import com.letitout.app.ui.MoodSymbol
import com.letitout.app.ui.components.PageBackButton
import com.letitout.app.ui.components.TELE_MANAS_NUMBER
import com.letitout.app.ui.components.dialTeleManas
import java.util.Locale

private const val SPEECH_MISSED = "Couldn't catch that. Try again, or type it out if that's easier."
private const val MIC_DENIED = "Microphone access is off. Typing works just as well."
private const val VOICE_UNAVAILABLE = "Voice input isn't available on this phone, but typing works just as well."

private fun speechIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
}

@Composable
internal fun ChatScreen(vm: AppViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    var speechError by remember { mutableStateOf<String?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var speechEnabled by remember { mutableStateOf(false) }
    var askFeeling by remember { mutableStateOf(false) }
    val tts = remember { TextToSpeech(context) { result -> speechEnabled = result == TextToSpeech.SUCCESS } }
    DisposableEffect(tts) { onDispose { tts.stop(); tts.shutdown() } }
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
            override fun onError(error: Int) { isListening = false; speechError = SPEECH_MISSED }
            override fun onResults(results: Bundle?) {
                isListening = false
                val heard = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (heard.isNullOrBlank()) speechError = SPEECH_MISSED else input = heard
            }
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        onDispose { recognizer?.destroy() }
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) speechError = MIC_DENIED
        else if (recognizer == null) speechError = VOICE_UNAVAILABLE
        else recognizer.startListening(speechIntent())
    }
    val submitMessage: () -> Unit = {
        if (state.signedIn && input.isNotBlank() && !state.loading) {
            vm.send(input)
            input = ""
            speechError = null
            focusManager.clearFocus()
            keyboard?.hide()
        }
    }
    // Leaving after a real conversation asks one gentle question first; otherwise just go back.
    val leaveChat: () -> Unit = { if (state.feelingCheckDue) askFeeling = true else onBack() }
    BackHandler(enabled = state.feelingCheckDue) { askFeeling = true }
    LaunchedEffect(state.messages.size, state.loading) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size + if (state.loading) 1 else 0)
    }
    val palette = state.mood.palette()
    Column(Modifier.fillMaxSize()) {
        Surface(color = AppColors.Card) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                PageBackButton(onClick = leaveChat)
                Spacer(Modifier.width(10.dp))
                Box(Modifier.size(42.dp).background(palette.background, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    MoodSymbol(state.mood, Modifier.size(25.dp), palette.primary)
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text("${state.mood.label} space", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Ink)
                    Text(if (state.loading) "typing…" else "Take your time", fontSize = 12.sp, color = if (state.loading) palette.primary else AppColors.Muted)
                }
                IconButton(onClick = { vm.clearConversation(); tts.stop(); speechError = null }, modifier = Modifier.background(AppColors.PrimarySoft, CircleShape)) {
                    Icon(Icons.Default.Delete, "Clear this conversation", tint = AppColors.Primary, modifier = Modifier.size(20.dp))
                }
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().background(palette.background.copy(alpha = .55f)),
            contentPadding = PaddingValues(horizontal = 17.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            item {
                Surface(color = AppColors.Card, shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, AppColors.Line)) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.Top) {
                        MoodSymbol(state.mood, Modifier.size(24.dp), palette.primary)
                        Spacer(Modifier.width(13.dp))
                        Text(state.mood.prompt, color = AppColors.Ink, fontSize = 16.sp, lineHeight = 23.sp)
                    }
                }
            }
            if (state.messages.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(top = 19.dp)) {
                    Text("A STARTING POINT", color = palette.primary, fontSize = 11.sp, letterSpacing = 1.7.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(7.dp))
                    Text("It's okay if you don't know what to say yet.", color = AppColors.Muted, fontSize = 14.sp, lineHeight = 20.sp)
                    Spacer(Modifier.height(16.dp))
                    listOf("I want to talk about today", "I'm not sure where to start").forEach { suggestion ->
                        Surface(
                            onClick = { input = suggestion },
                            color = AppColors.Card,
                            shape = RoundedCornerShape(17.dp),
                            border = BorderStroke(1.dp, AppColors.Line),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp)
                        ) {
                            Row(Modifier.padding(horizontal = 17.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(suggestion, color = AppColors.Ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = AppColors.Muted, modifier = Modifier.size(19.dp))
                            }
                        }
                    }
                }
            }
            items(state.messages, key = { it.id }) { message ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start) {
                    Column(horizontalAlignment = if (message.fromUser) Alignment.End else Alignment.Start) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (message.fromUser) AppColors.Primary else AppColors.Card,
                            border = if (message.fromUser) null else BorderStroke(1.dp, AppColors.Line),
                            modifier = Modifier.widthIn(max = 300.dp)
                        ) {
                            Text(message.text, modifier = Modifier.padding(horizontal = 17.dp, vertical = 14.dp), color = if (message.fromUser) Color.White else AppColors.Ink, fontSize = 15.sp, lineHeight = 21.sp)
                        }
                        if (!message.fromUser) TextButton(onClick = { if (speechEnabled) { tts.language = Locale.getDefault(); tts.speak(message.text, TextToSpeech.QUEUE_FLUSH, null, "letitout-reply") } }) {
                            Icon(Icons.Default.PlayArrow, "Read this aloud", modifier = Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text("Listen", fontSize = 12.sp)
                        }
                    }
                }
            }
            if (state.loading) item {
                Surface(shape = RoundedCornerShape(20.dp), color = AppColors.Card, border = BorderStroke(1.dp, AppColors.Line)) {
                    Text("typing…", modifier = Modifier.padding(horizontal = 17.dp, vertical = 12.dp), color = AppColors.Muted, fontSize = 14.sp)
                }
            }
        }
        if (state.supportSuggested) SupportCard(onCall = { dialTeleManas(context) }, onDismiss = vm::dismissSupport)
        state.error?.let { error ->
            Surface(color = AppColors.Warm, shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(error, color = AppColors.Error, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = vm::retry) { Text("Retry") }
                }
            }
        }
        speechError?.let { Text(it, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = AppColors.Error, fontSize = 12.sp) }
        if (!state.signedIn) {
            Surface(color = AppColors.PrimarySoft, shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = AppColors.Primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("You're exploring in preview. Sign in whenever you're ready to start talking.", color = AppColors.Ink, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }
        Surface(color = AppColors.Card, shadowElevation = 4.dp) {
            Row(Modifier.fillMaxWidth().imePadding().padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(enabled = state.signedIn, onClick = {
                    if (isListening) { recognizer?.stopListening(); isListening = false }
                    else {
                        speechError = null
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            if (recognizer == null) speechError = VOICE_UNAVAILABLE
                            else recognizer.startListening(speechIntent())
                        } else micPermission.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }, modifier = Modifier.size(46.dp).background(AppColors.PrimarySoft, CircleShape)) {
                    Icon(painterResource(if (isListening) R.drawable.ic_stop else R.drawable.ic_mic), if (isListening) "Stop listening" else "Speak", tint = if (state.signedIn) AppColors.Primary else AppColors.Muted, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = input, onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("What's on your mind?") },
                    maxLines = 4, shape = RoundedCornerShape(20.dp),
                    keyboardOptions = KeyboardOptions(imeAction = if (state.signedIn) ImeAction.Send else ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onSend = { submitMessage() },
                        onDone = { focusManager.clearFocus(); keyboard?.hide() }
                    )
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(onClick = submitMessage, enabled = state.signedIn && input.isNotBlank() && !state.loading, modifier = Modifier.size(46.dp), shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send", modifier = Modifier.size(21.dp))
                }
            }
        }
    }
    if (askFeeling) {
        val finish: (Feeling?) -> Unit = { feeling ->
            askFeeling = false
            if (feeling == null) vm.skipFeelingCheck() else vm.recordFeeling(feeling)
            onBack()
        }
        AlertDialog(
            onDismissRequest = { askFeeling = false },
            containerColor = AppColors.Card,
            shape = RoundedCornerShape(26.dp),
            title = { Text("How are you feeling now?", color = AppColors.Ink, fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    Text("Compared to when you started. Your answer stays on this phone.", color = AppColors.Muted, fontSize = 14.sp, lineHeight = 20.sp)
                    Spacer(Modifier.height(16.dp))
                    FeelingOption("Lighter", Mood.HAPPY) { finish(Feeling.LIGHTER) }
                    FeelingOption("About the same", Mood.TALK) { finish(Feeling.SAME) }
                    FeelingOption("Heavier", Mood.SAD) { finish(Feeling.HEAVIER) }
                }
            },
            confirmButton = { TextButton(onClick = { finish(null) }) { Text("Skip") } }
        )
    }
}

@Composable
private fun FeelingOption(label: String, mood: Mood, onClick: () -> Unit) {
    val palette = mood.palette()
    Surface(
        onClick = onClick,
        color = AppColors.Card,
        shape = RoundedCornerShape(17.dp),
        border = BorderStroke(1.dp, AppColors.Line),
        modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp)
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).background(palette.background, RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                MoodSymbol(mood, Modifier.size(20.dp), palette.primary)
            }
            Spacer(Modifier.width(13.dp))
            Text(label, color = AppColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SupportCard(onCall: () -> Unit, onDismiss: () -> Unit) {
    Surface(color = AppColors.Warm, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).background(AppColors.Card, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text("You don't have to carry this alone", color = AppColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Talking to a doctor is the best next step, or someone you trust. Tele-MANAS ($TELE_MANAS_NUMBER) is free and open 24x7, with trained counsellors who'll listen.",
                color = AppColors.Ink, fontSize = 13.sp, lineHeight = 19.sp
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onCall, shape = RoundedCornerShape(15.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Call $TELE_MANAS_NUMBER", fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onDismiss) { Text("Not now") }
            }
        }
    }
}
