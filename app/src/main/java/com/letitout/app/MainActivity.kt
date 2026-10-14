package com.letitout.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.letitout.app.data.Mood
import com.letitout.app.data.palette
import com.letitout.app.ui.AppColors
import com.letitout.app.ui.AppViewModel
import com.letitout.app.ui.BrandMark
import com.letitout.app.ui.MoodSymbol
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LetItOutApp() }
    }
}

@Composable
private fun LetItOutApp(vm: AppViewModel = hiltViewModel()) {
    var backStack by rememberSaveable { mutableStateOf(listOf("welcome")) }
    val page = backStack.last()
    val navigateTo: (String) -> Unit = { destination -> backStack = backStack + destination }
    val enterHome: () -> Unit = { backStack = listOf("home") }
    val navigateBack: () -> Unit = {
        if (backStack.size > 1) backStack = backStack.dropLast(1)
    }
    BackHandler(enabled = backStack.size > 1, onBack = navigateBack)
    val scheme = lightColorScheme(
        primary = AppColors.Primary,
        secondary = Color(0xFF8DAF9C),
        secondaryContainer = AppColors.PrimarySoft,
        onSecondaryContainer = AppColors.Primary,
        background = AppColors.Canvas,
        surface = AppColors.Card,
        surfaceVariant = AppColors.PrimarySoft,
        onSurfaceVariant = AppColors.Muted,
        onPrimary = Color.White,
        onSurface = AppColors.Ink,
        onBackground = AppColors.Ink,
        outline = AppColors.Line,
        error = AppColors.Error
    )
    MaterialTheme(colorScheme = scheme) {
        Surface(color = AppColors.Canvas, modifier = Modifier.fillMaxSize()) {
            when (page) {
                "welcome" -> WelcomePage { navigateTo("auth") }
                "auth" -> AuthPage(vm, onBack = navigateBack, onContinue = enterHome)
                "home" -> HomePage(
                    onChoose = { mood -> vm.chooseMood(mood); navigateTo("chat") },
                    onSettings = { navigateTo("settings") }
                )
                "chat" -> ChatPage(vm, onBack = navigateBack)
                "settings" -> SettingsPage(vm, onBack = navigateBack, onPrivacy = { navigateTo("privacy") }, onAuth = { navigateTo("auth") })
                "privacy" -> PrivacyPage(onBack = navigateBack)
            }
        }
    }
}

@Composable
private fun WelcomePage(onStart: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 26.dp, vertical = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandMark(size = 42.dp)
            Spacer(Modifier.width(12.dp))
            Text("LET IT OUT", color = AppColors.Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 2.sp)
        }
        Spacer(Modifier.height(54.dp))
        Text("A little room\nto be yourself.", color = AppColors.Ink, fontSize = 42.sp, lineHeight = 47.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(14.dp))
        Text("Whatever today feels like, you can start here.", color = AppColors.Muted, fontSize = 17.sp, lineHeight = 24.sp)
        Spacer(Modifier.height(34.dp))
        Surface(
            color = AppColors.PrimarySoft,
            shape = RoundedCornerShape(30.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(24.dp)) {
                Box(Modifier.size(68.dp).background(AppColors.Card, RoundedCornerShape(22.dp)), contentAlignment = Alignment.Center) {
                    MoodSymbol(Mood.TALK, Modifier.size(38.dp), AppColors.Primary)
                }
                Spacer(Modifier.height(28.dp))
                Text("No perfect words needed.", color = AppColors.Ink, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("Talk about what's on your mind, or simply explore the space.", color = AppColors.Muted, fontSize = 14.sp, lineHeight = 21.sp)
            }
        }
        Spacer(Modifier.height(30.dp))
        listOf(
            "A place to check in with yourself",
            "Listen or write, at your own pace",
            "Explore first, sign in when ready"
        ).forEach { line ->
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).background(AppColors.Primary, CircleShape))
                Spacer(Modifier.width(13.dp))
                Text(line, color = AppColors.Ink, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(32.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(19.dp)) {
            Text("Get started", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
        }
        Spacer(Modifier.height(15.dp))
        Text("For adults 18+ · Not emergency care", modifier = Modifier.fillMaxWidth(), color = AppColors.Muted, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun AuthPage(vm: AppViewModel, onBack: () -> Unit, onContinue: () -> Unit) {
    val activity = LocalContext.current as Activity
    val scope = rememberCoroutineScope()
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val configured = vm.auth.isConfigured()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 26.dp, vertical = 24.dp)) {
        PageBackButton(onBack)
        Spacer(Modifier.height(43.dp))
        BrandMark(size = 76.dp)
        Spacer(Modifier.height(27.dp))
        Text("Come as you are.", fontSize = 36.sp, lineHeight = 41.sp, color = AppColors.Ink, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        Text("Choose how you'd like to enter your space. You can always explore before signing in.", color = AppColors.Muted, fontSize = 16.sp, lineHeight = 23.sp)
        Spacer(Modifier.height(36.dp))
        Button(
            onClick = {
                scope.launch {
                    working = true; error = null
                    try { vm.auth.signInGoogle(activity); vm.refreshAuth(); onContinue() }
                    catch (t: Exception) { error = t.message ?: "Google sign-in didn't complete" }
                    finally { working = false }
                }
            }, enabled = !working && configured, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp)
        ) { Icon(Icons.Default.AccountCircle, null); Spacer(Modifier.width(10.dp)); Text("Continue with Google", fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = {
                scope.launch {
                    working = true; error = null
                    try { vm.auth.signInGuest(); vm.refreshAuth(); onContinue() }
                    catch (t: Exception) { error = t.message ?: "Guest sign-in failed" }
                    finally { working = false }
                }
            }, enabled = !working && configured, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp)
        ) { Text("Continue as guest", fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(12.dp))
        FilledTonalButton(onClick = onContinue, enabled = !working, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp)) {
            Text("Explore in preview", fontWeight = FontWeight.SemiBold)
        }
        if (working) { Spacer(Modifier.height(20.dp)); CircularProgressIndicator() }
        if (!configured) {
            Spacer(Modifier.height(25.dp))
            Surface(color = AppColors.Warm, shape = RoundedCornerShape(20.dp)) {
                Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Info, null, tint = AppColors.Error, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Sign-in is unavailable until Firebase is configured. Preview mode is ready to explore.", color = AppColors.Ink, fontSize = 13.sp, lineHeight = 19.sp)
                }
            }
        }
        error?.let { Spacer(Modifier.height(14.dp)); Text(it, color = AppColors.Error, fontSize = 13.sp) }
        Spacer(Modifier.height(31.dp))
        Text("AI can make mistakes and is not a replacement for professional care.", fontSize = 12.sp, lineHeight = 18.sp, color = AppColors.Muted)
    }
}

@Composable
private fun HomePage(onChoose: (Mood) -> Unit, onSettings: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BrandMark(size = 44.dp)
            Spacer(Modifier.width(11.dp))
            Text("LET IT OUT", modifier = Modifier.weight(1f), color = AppColors.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
            IconButton(onClick = onSettings, modifier = Modifier.background(AppColors.Card, CircleShape)) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = AppColors.Primary)
            }
        }
        Spacer(Modifier.height(39.dp))
        Text("How are you\nfeeling today?", fontSize = 36.sp, lineHeight = 41.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Ink)
        Spacer(Modifier.height(11.dp))
        Text("Choose what feels closest. You can change your mind anytime.", color = AppColors.Muted, fontSize = 15.sp, lineHeight = 22.sp)
        Spacer(Modifier.height(34.dp))
        Text("CHECK IN", color = AppColors.Primary, fontSize = 11.sp, letterSpacing = 1.9.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Mood.entries.dropLast(1).chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { mood ->
                    MoodCard(mood, modifier = Modifier.weight(1f).height(157.dp), onClick = { onChoose(mood) })
                }
            }
        }
        MoodCard(Mood.TALK, modifier = Modifier.fillMaxWidth().height(104.dp), wide = true, onClick = { onChoose(Mood.TALK) })
        Spacer(Modifier.height(24.dp))
        Surface(color = AppColors.PrimarySoft, shape = RoundedCornerShape(24.dp)) {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                MoodSymbol(Mood.TALK, Modifier.size(29.dp), AppColors.Primary)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Your pace. Your space.", color = AppColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("No need to have it all figured out.", color = AppColors.Muted, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun MoodCard(mood: Mood, modifier: Modifier, wide: Boolean = false, onClick: () -> Unit) {
    val palette = mood.palette()
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = AppColors.Card),
        shape = RoundedCornerShape(23.dp),
        border = BorderStroke(1.dp, AppColors.Line)
    ) {
        if (wide) {
            Row(Modifier.fillMaxSize().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(60.dp).background(palette.background, RoundedCornerShape(19.dp)), contentAlignment = Alignment.Center) {
                    MoodSymbol(mood, Modifier.size(31.dp), palette.primary)
                }
                Spacer(Modifier.width(17.dp))
                Column(Modifier.weight(1f)) {
                    Text(mood.label, color = AppColors.Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text(mood.detail, color = AppColors.Muted, fontSize = 12.sp)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = AppColors.Muted)
            }
        } else {
            Column(Modifier.fillMaxSize().padding(17.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Box(Modifier.size(57.dp).background(palette.background, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                    MoodSymbol(mood, Modifier.size(32.dp), palette.primary)
                }
                Column {
                    Text(mood.label, color = AppColors.Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(mood.detail, color = AppColors.Muted, fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun ChatPage(vm: AppViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    var speechError by remember { mutableStateOf<String?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var speechEnabled by remember { mutableStateOf(false) }
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
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) speechError = "Microphone permission denied. You can still type."
        else if (recognizer != null) {
            recognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            })
        }
    }
    val submitMessage: () -> Unit = {
        if (state.signedIn && input.isNotBlank() && !state.loading) {
            vm.send(input)
            input = ""
            focusManager.clearFocus()
            keyboard?.hide()
        }
    }
    LaunchedEffect(state.messages.size, state.loading) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size + if (state.loading) 1 else 0)
    }
    val palette = state.mood.palette()
    Column(Modifier.fillMaxSize()) {
        Surface(color = AppColors.Card) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                PageBackButton(onClick = onBack)
                Spacer(Modifier.width(10.dp))
                Box(Modifier.size(42.dp).background(palette.background, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    MoodSymbol(state.mood, Modifier.size(25.dp), palette.primary)
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text("${state.mood.label} space", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Ink)
                    Text("Take your time", fontSize = 12.sp, color = AppColors.Muted)
                }
                IconButton(onClick = { vm.clearConversation(); tts.stop() }, modifier = Modifier.background(AppColors.PrimarySoft, CircleShape)) {
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
                        if (!message.fromUser) TextButton(onClick = { if (speechEnabled) { tts.language = Locale.getDefault(); tts.speak(message.text, TextToSpeech.QUEUE_FLUSH, null, "letitout-ai") } }) {
                            Icon(Icons.Default.PlayArrow, "Speak response", modifier = Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text("Listen", fontSize = 12.sp)
                        }
                    }
                }
            }
            if (state.loading) item {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Listening and thinking…", color = AppColors.Muted, fontSize = 13.sp)
                }
            }
        }
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
                    Text("Preview mode · You can explore this space. Sending messages requires sign-in.", color = AppColors.Ink, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }
        Surface(color = AppColors.Card, shadowElevation = 4.dp) {
            Row(Modifier.fillMaxWidth().imePadding().padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(enabled = state.signedIn, onClick = {
                    if (isListening) { recognizer?.stopListening(); isListening = false }
                    else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        if (recognizer == null) speechError = "Speech recognition is unavailable on this device."
                        else recognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                        })
                    } else micPermission.launch(Manifest.permission.RECORD_AUDIO)
                }, modifier = Modifier.size(46.dp).background(AppColors.PrimarySoft, CircleShape)) {
                    Icon(painterResource(if (isListening) R.drawable.ic_stop else R.drawable.ic_mic), "Speak", tint = if (state.signedIn) AppColors.Primary else AppColors.Muted, modifier = Modifier.size(22.dp))
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
}

@Composable
private fun SettingsPage(vm: AppViewModel, onBack: () -> Unit, onPrivacy: () -> Unit, onAuth: () -> Unit) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }
    val signedIn = vm.auth.isSignedIn()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp)) {
        PageBackButton(onBack)
        Spacer(Modifier.height(34.dp))
        Text("YOUR SPACE", color = AppColors.Primary, fontSize = 11.sp, letterSpacing = 1.9.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(9.dp))
        Text("Settings", fontSize = 36.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Ink)
        Spacer(Modifier.height(9.dp))
        Text("Keep your space comfortable and in your control.", color = AppColors.Muted, fontSize = 15.sp, lineHeight = 22.sp)
        Spacer(Modifier.height(30.dp))
        Surface(color = AppColors.Card, shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, AppColors.Line)) {
            Row(Modifier.fillMaxWidth().padding(19.dp), verticalAlignment = Alignment.CenterVertically) {
                BrandMark(size = 52.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (signedIn) vm.auth.userName() ?: "Your account" else "Exploring in preview", color = AppColors.Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(3.dp))
                    Text(if (signedIn) "Signed in" else "No account connected", color = AppColors.Muted, fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(33.dp))
        Text("ACCOUNT & PRIVACY", color = AppColors.Primary, fontSize = 11.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(13.dp))
        SettingsItem(Icons.Default.Lock, "Privacy and support", "How this space works", onClick = onPrivacy)
        Spacer(Modifier.height(11.dp))
        if (signedIn) {
            SettingsItem(Icons.AutoMirrored.Filled.ExitToApp, "Sign out", "Return to sign-in options", onClick = {
                scope.launch {
                    try { vm.auth.signOut(); vm.refreshAuth(); onAuth() }
                    catch (t: Exception) { status = t.message ?: "Sign-out problem" }
                }
            })
            Spacer(Modifier.height(11.dp))
            SettingsItem(Icons.Default.Delete, "Delete my account", "Remove your sign-in account", destructive = true, onClick = { confirmDelete = true })
        } else {
            SettingsItem(Icons.Default.AccountCircle, "Sign in or join as guest", "Enable conversations when configured", onClick = onAuth)
            Spacer(Modifier.height(20.dp))
            Surface(color = AppColors.PrimarySoft, shape = RoundedCornerShape(20.dp)) {
                Text("Preview mode lets you explore the app. There is no account to sign out of or delete.", modifier = Modifier.fillMaxWidth().padding(17.dp), color = AppColors.Muted, fontSize = 13.sp, lineHeight = 19.sp)
            }
        }
        if (status.isNotBlank()) { Spacer(Modifier.height(15.dp)); Text(status, color = AppColors.Error, fontSize = 13.sp) }
        Spacer(Modifier.height(41.dp))
        Text("LET IT OUT  ·  VERSION 0.1.0 PREVIEW", color = AppColors.Muted, fontSize = 11.sp, letterSpacing = .6.sp)
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete your account?") },
        text = { Text("This removes your Firebase sign-in account. Conversations aren't stored on our backend in this preview.") },
        confirmButton = { TextButton(onClick = {
            confirmDelete = false
            scope.launch {
                try { vm.auth.deleteAccount(); vm.clearConversation(); vm.refreshAuth(); onAuth() }
                catch (t: Exception) { status = t.message ?: "Deletion could not be completed. Please reauthenticate." }
            }
        }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
    )
}

@Composable
private fun PrivacyPage(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp)) {
        PageBackButton(onBack)
        Spacer(Modifier.height(34.dp))
        Text("GOOD TO KNOW", color = AppColors.Primary, fontSize = 11.sp, letterSpacing = 1.9.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(9.dp))
        Text("Privacy & support", fontSize = 34.sp, lineHeight = 39.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Ink)
        Spacer(Modifier.height(10.dp))
        Text("Clear information for a more comfortable conversation.", color = AppColors.Muted, fontSize = 15.sp, lineHeight = 22.sp)
        Spacer(Modifier.height(30.dp))
        InformationCard(Icons.Default.Info, "What this app is", "LET IT OUT is an AI listening companion, not a therapist or emergency service.")
        Spacer(Modifier.height(12.dp))
        InformationCard(Icons.Default.Lock, "When you use AI chat", "Your messages are sent to our backend and AI provider. Do not share information you aren't comfortable sending to those services.")
        Spacer(Modifier.height(12.dp))
        InformationCard(Icons.Default.Phone, "Need urgent support?", "In India: call 112 for emergencies. Tele-MANAS: 14416 for mental health support. If you are elsewhere, contact local emergency services.", warm = true)
        Spacer(Modifier.height(25.dp))
        Text("ABOUT THIS PREVIEW", color = AppColors.Primary, fontSize = 11.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text("We do not currently save chat history on our server. This is a prototype; final privacy, terms and deletion policies must be published before release.", color = AppColors.Muted, fontSize = 13.sp, lineHeight = 20.sp)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PageBackButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(44.dp).background(AppColors.PrimarySoft, CircleShape)) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppColors.Primary, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun SettingsItem(icon: ImageVector, title: String, detail: String, destructive: Boolean = false, onClick: () -> Unit) {
    val tint = if (destructive) AppColors.Error else AppColors.Primary
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = AppColors.Card), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, AppColors.Line)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).background(if (destructive) AppColors.Warm else AppColors.PrimarySoft, RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(15.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = if (destructive) AppColors.Error else AppColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(detail, color = AppColors.Muted, fontSize = 12.sp, lineHeight = 17.sp)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = AppColors.Muted)
        }
    }
}

@Composable
private fun InformationCard(icon: ImageVector, title: String, body: String, warm: Boolean = false) {
    Surface(
        color = if (warm) AppColors.Warm else AppColors.Card,
        shape = RoundedCornerShape(22.dp),
        border = if (warm) null else BorderStroke(1.dp, AppColors.Line)
    ) {
        Column(Modifier.fillMaxWidth().padding(19.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).background(if (warm) AppColors.Card else AppColors.PrimarySoft, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = if (warm) AppColors.Error else AppColors.Primary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(13.dp))
                Text(title, color = AppColors.Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(13.dp))
            Text(body, color = AppColors.Muted, fontSize = 14.sp, lineHeight = 21.sp)
        }
    }
}
