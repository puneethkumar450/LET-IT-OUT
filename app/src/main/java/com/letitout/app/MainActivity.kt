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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.letitout.app.ui.AppViewModel
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

private val Ink = Color(0xFF29263E)
private val Lavender = Color(0xFF7866CF)

@Composable
private fun LetItOutApp(vm: AppViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf("welcome") }
    val palette = state.mood.palette()
    val background by animateColorAsState(palette.background, label = "Mood background")
    val dynamicScheme = lightColorScheme(
        primary = palette.primary,
        secondary = palette.accent,
        background = background,
        surface = Color.White,
        onPrimary = Color.White,
        onSurface = Ink,
        onBackground = Ink
    )
    MaterialTheme(colorScheme = dynamicScheme) {
        Surface(color = background, modifier = Modifier.fillMaxSize()) {
            when (page) {
                "welcome" -> WelcomePage { page = "auth" }
                "auth" -> AuthPage(vm, onContinue = { page = "home" })
                "home" -> HomePage(
                    onChoose = { mood -> vm.chooseMood(mood); page = "chat" },
                    onSettings = { page = "settings" }
                )
                "chat" -> ChatPage(vm, onBack = { page = "home" })
                "settings" -> SettingsPage(vm, onBack = { page = "home" }, onPrivacy = { page = "privacy" }, onAuth = { page = "auth" })
                "privacy" -> PrivacyPage { page = "settings" }
            }
        }
    }
}

@Composable
private fun WelcomePage(onStart: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(26.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(142.dp).background(
                    Brush.linearGradient(listOf(Color(0xFFDCCFFF), Color(0xFFA9E9DB))),
                    RoundedCornerShape(42.dp)
                ), contentAlignment = Alignment.Center
            ) { Text("💙", fontSize = 78.sp) }
            Spacer(Modifier.height(30.dp))
            Text("LET IT OUT", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Ink)
            Spacer(Modifier.height(12.dp))
            Text("Your feelings matter.\nWe're here to listen.", color = Color(0xFF6D6687), fontSize = 20.sp, textAlign = TextAlign.Center, lineHeight = 28.sp)
            Spacer(Modifier.height(46.dp))
            listOf("🎙️   Talk freely, at your own pace", "🫶   Feel heard without judgment", "🌿   Take small steps toward calm").forEach { label ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                ) { Text(label, modifier = Modifier.padding(18.dp), fontSize = 15.sp, color = Ink) }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(19.dp)) {
                Text("Get started", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Text("AI listening companion · 18+ · Not emergency care", color = Color(0xFF777189), fontSize = 11.sp)
        }
    }
}

@Composable
private fun AuthPage(vm: AppViewModel, onContinue: () -> Unit) {
    val activity = LocalContext.current as Activity
    val scope = rememberCoroutineScope()
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🫶", fontSize = 68.sp)
        Spacer(Modifier.height(20.dp))
        Text("A little space for you", fontSize = 28.sp, color = Ink, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("You can explore first. Sign in to start AI conversations.", color = Color.Gray, textAlign = TextAlign.Center)
        Spacer(Modifier.height(34.dp))
        Button(
            onClick = {
                scope.launch {
                    working = true; error = null
                    try { vm.auth.signInGoogle(activity); vm.refreshAuth(); onContinue() }
                    catch (t: Exception) { error = t.message ?: "Google sign-in didn't complete" }
                    finally { working = false }
                }
            }, enabled = !working, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp)
        ) { Text("Continue with Google") }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = {
                scope.launch {
                    working = true; error = null
                    try { vm.auth.signInGuest(); vm.refreshAuth(); onContinue() }
                    catch (t: Exception) { error = t.message ?: "Guest sign-in failed" }
                    finally { working = false }
                }
            }, enabled = !working, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp)
        ) { Text("Continue as guest") }
        TextButton(onClick = onContinue, enabled = !working) { Text("Explore the app (preview only)") }
        if (working) CircularProgressIndicator(Modifier.padding(16.dp))
        if (!vm.auth.isConfigured()) Text("Firebase not configured. Preview works; AI requires setup.", color = Color(0xFF9C5826), textAlign = TextAlign.Center, fontSize = 12.sp)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, fontSize = 13.sp) }
        Spacer(Modifier.height(28.dp))
        Text("AI can make mistakes. Not a replacement for professional care.", textAlign = TextAlign.Center, fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
private fun HomePage(onChoose: (Mood) -> Unit, onSettings: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 23.dp)) {
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("LET IT OUT", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Lavender)
                Text("Hello there 💙", fontSize = 31.sp, fontWeight = FontWeight.Bold, color = Ink)
            }
            IconButton(onClick = onSettings, modifier = Modifier.background(Color.White, CircleShape)) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Ink)
            }
        }
        Spacer(Modifier.height(29.dp))
        Text("How are you feeling today?", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(7.dp))
        Text("Every feeling deserves a little space.", fontSize = 15.sp, color = Color(0xFF766F88))
        Spacer(Modifier.height(26.dp))
        Mood.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                row.forEach { mood ->
                    Card(
                        onClick = { onChoose(mood) },
                        modifier = Modifier.weight(1f).height(143.dp),
                        colors = CardDefaults.cardColors(containerColor = mood.palette().background),
                        shape = RoundedCornerShape(27.dp),
                        border = BorderStroke(1.dp, mood.palette().accent)
                    ) {
                        Column(Modifier.fillMaxSize().padding(19.dp), verticalArrangement = Arrangement.SpaceBetween) {
                            Text(mood.icon, fontSize = 36.sp)
                            Text(mood.label, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(10.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(24.dp)) {
            Row(Modifier.fillMaxWidth().padding(19.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🌿", fontSize = 30.sp)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Your pace. Your space.", fontWeight = FontWeight.Bold, color = Ink)
                    Text("You don't have to have it all figured out.", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun ChatPage(vm: AppViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as Activity
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
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
    LaunchedEffect(state.messages.size, state.loading) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size + if (state.loading) 1 else 0)
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 12.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { tts.stop(); onBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Text(state.mood.icon, fontSize = 26.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Let's talk", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("${state.mood.label} mood · AI companion", fontSize = 12.sp, color = Color.Gray)
            }
            IconButton(onClick = { vm.clearConversation(); tts.stop() }) {
                Icon(Icons.Default.Delete, "Clear this conversation")
            }
        }
        LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
            item {
                Text("💙  ${state.mood.prompt}", modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(22.dp)).padding(19.dp), color = Ink, fontSize = 16.sp)
            }
            items(state.messages, key = { it.id }) { message ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start) {
                    Column(horizontalAlignment = if (message.fromUser) Alignment.End else Alignment.Start) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (message.fromUser) MaterialTheme.colorScheme.primary else Color.White,
                            modifier = Modifier.widthIn(max = 300.dp)
                        ) {
                            Text(message.text, modifier = Modifier.padding(16.dp), color = if (message.fromUser) Color.White else Ink, fontSize = 15.sp)
                        }
                        if (!message.fromUser) TextButton(onClick = { if (speechEnabled) { tts.language = Locale.getDefault(); tts.speak(message.text, TextToSpeech.QUEUE_FLUSH, null, "letitout-ai") } }) {
                            Icon(Icons.Default.PlayArrow, "Speak response", modifier = Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text("Listen", fontSize = 12.sp)
                        }
                    }
                }
            }
            if (state.loading) item { Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)); Text("Listening and thinking…", color = Color.Gray) } }
        }
        state.error?.let { error ->
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E9)), modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(error, color = Color(0xFF7D452E), fontSize = 12.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = vm::retry) { Text("Retry") }
                }
            }
        }
        speechError?.let { Text(it, modifier = Modifier.padding(10.dp), color = Color(0xFF984D3D), fontSize = 12.sp) }
        Surface(color = Color.White, shadowElevation = 8.dp) {
            Row(Modifier.fillMaxWidth().imePadding().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (isListening) { recognizer?.stopListening(); isListening = false }
                    else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        if (recognizer == null) speechError = "Speech recognition is unavailable on this device."
                        else recognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                        })
                    } else micPermission.launch(Manifest.permission.RECORD_AUDIO)
                }) { Icon(painterResource(if (isListening) R.drawable.ic_stop else R.drawable.ic_mic), "Speak") }
                OutlinedTextField(
                    value = input, onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("What's on your mind?") },
                    maxLines = 4, shape = RoundedCornerShape(22.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (input.isNotBlank()) { vm.send(input); input = "" } })
                )
                Spacer(Modifier.width(6.dp))
                IconButton(onClick = { vm.send(input); input = "" }, enabled = input.isNotBlank() && !state.loading) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = MaterialTheme.colorScheme.primary)
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
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp)) {
        TextButton(onClick = onBack) { Text("← Back") }
        Spacer(Modifier.height(16.dp))
        Text("Your space ⚙️", fontSize = 29.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(20.dp))
        Text("Account", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(if (vm.auth.isSignedIn()) "Signed in" else "Preview mode", color = Color.Gray)
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onPrivacy, modifier = Modifier.fillMaxWidth()) { Text("Privacy, help & support") }
        OutlinedButton(onClick = {
            scope.launch {
                try { vm.auth.signOut(); vm.refreshAuth(); onAuth() }
                catch (t: Exception) { status = t.message ?: "Sign-out problem" }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }
        OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) { Text("Delete my account") }
        if (status.isNotBlank()) Text(status, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(28.dp))
        Text("LET IT OUT · v0.1.0 preview", color = Color.Gray, fontSize = 12.sp)
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
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        TextButton(onClick = onBack) { Text("← Back") }
        Spacer(Modifier.height(14.dp))
        Text("Here for you 💙", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        Text("LET IT OUT is an AI listening companion, not a therapist or emergency service.", fontSize = 16.sp)
        Spacer(Modifier.height(16.dp))
        Text("Your messages are sent to our backend and AI provider when you use AI chat. Do not share information you aren't comfortable sending to those services.")
        Spacer(Modifier.height(20.dp))
        Text("Need urgent support?", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(8.dp))
        Text("In India: call 112 for emergencies. Tele-MANAS: 14416 for mental health support. If you are elsewhere, contact local emergency services.")
        Spacer(Modifier.height(20.dp))
        Text("We do not currently save chat history on our server. This is a prototype; final privacy, terms and deletion policies must be published before release.")
    }
}
