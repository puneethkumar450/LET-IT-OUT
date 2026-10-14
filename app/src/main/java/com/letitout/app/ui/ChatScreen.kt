package com.letitout.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.letitout.app.data.Mood
import com.letitout.app.data.MoodPalette
import com.letitout.app.data.palette
import java.util.Locale

@Composable
internal fun ChatScreen(
    vm: AppViewModel,
    onHome: () -> Unit,
    onChangeMood: () -> Unit,
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
    var speechEnabled by remember { mutableStateOf(false) }
    var speakingId by remember { mutableStateOf<Long?>(null) }
    val tts = remember { TextToSpeech(context) { result -> speechEnabled = result == TextToSpeech.SUCCESS } }
    DisposableEffect(tts) {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { speakingId = utteranceId?.toLongOrNull() }
            override fun onDone(utteranceId: String?) { if (speakingId == utteranceId?.toLongOrNull()) speakingId = null }
            @Suppress("OVERRIDE_DEPRECATION")
            override fun onError(utteranceId: String?) { speakingId = null }
            override fun onStop(utteranceId: String?, interrupted: Boolean) { speakingId = null }
        })
        onDispose { tts.stop(); tts.shutdown() }
    }
    val stopSpeaking: () -> Unit = { tts.stop(); speakingId = null }
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
                input = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
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
        else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startListening()
        else micPermission.launch(Manifest.permission.RECORD_AUDIO)
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
    LaunchedEffect(latest?.id, speechEnabled) {
        if (latest != null && !latest.fromUser && latest.id > seenUpTo && state.settings.readAloud && speechEnabled) {
            tts.language = Locale.getDefault()
            tts.speak(latest.text, TextToSpeech.QUEUE_FLUSH, null, latest.id.toString())
        }
    }
    val itemCount = 2 + state.messages.size + (if (state.loading) 1 else 0) + (if (state.error != null) 1 else 0)
    LaunchedEffect(itemCount) { listState.animateScrollToItem(itemCount - 1) }
    BackHandler(onBack = onHome)

    val supportMode = state.supportMode
    val palette = if (supportMode || !state.settings.followMoodTheme) Mood.TALK.palette() else state.mood.palette()
    Column(Modifier.fillMaxSize().background(palette.surface).statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            SquareIconButton(AppIcons.Back, "Back to home", onHome, shadowColor = palette.accent)
            if (supportMode) {
                ChipButton("Get human support", onHumanSupport, container = AppColors.Surface, content = AppColors.Primary, icon = AppIcons.Shield)
                LinkButton("Leave", AppColors.Muted, onHome)
            } else {
                ChipButton(state.mood.label, onChangeMood, container = palette.bubble, content = AppColors.Ink, icon = state.mood.icon, iconTint = palette.accent)
                Box {
                    SquareIconButton(AppIcons.More, "More options", { menuOpen = true }, shadowColor = palette.accent)
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, shape = RoundedCornerShape(16.dp)) {
                        DropdownMenuItem(text = { Text("New conversation") }, onClick = { menuOpen = false; stopSpeaking(); vm.newConversation() })
                        DropdownMenuItem(text = { Text("End conversation") }, onClick = { menuOpen = false; stopSpeaking(); vm.newConversation(); onHome() })
                    }
                }
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom)
        ) {
            item {
                Text(
                    "Today · Nothing here is saved", Modifier.fillMaxWidth().padding(bottom = 6.dp),
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
                    else -> AiBubble(palette.accent, footer = speakingFooter) { BubbleText(message.text) }
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
private fun AiAvatar(tint: Color) {
    Box(Modifier.size(28.dp).background(AppColors.Surface, CircleShape), contentAlignment = Alignment.Center) {
        Icon(AppIcons.Heart, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun AiBubble(accent: Color, footer: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(22.dp, 22.dp, 22.dp, 6.dp)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AiAvatar(accent)
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
        AiAvatar(palette.accent)
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
        AiAvatar(AppColors.Primary)
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
                "Let It Out can't call anyone or alert a person for you. These numbers go to real people. I'm still here if you want to keep talking.",
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
