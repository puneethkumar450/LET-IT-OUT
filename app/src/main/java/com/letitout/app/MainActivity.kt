package com.letitout.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.hilt.navigation.compose.hiltViewModel
import com.letitout.app.ui.AppColors
import com.letitout.app.ui.AppViewModel
import com.letitout.app.ui.screens.AuthScreen
import com.letitout.app.ui.screens.ChatScreen
import com.letitout.app.ui.screens.HomeScreen
import com.letitout.app.ui.screens.PrivacyScreen
import com.letitout.app.ui.screens.SettingsScreen
import com.letitout.app.ui.screens.WelcomeScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // targetSdk 35+ always draws behind the system bars, so do it on every version and pad the content instead.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AppColors.Canvas.toArgb(), AppColors.Canvas.toArgb())
        )
        super.onCreate(savedInstanceState)
        setContent { LetItOutApp() }
    }
}

@Composable
private fun LetItOutApp(vm: AppViewModel = hiltViewModel()) {
    var backStack by rememberSaveable { mutableStateOf(listOf(if (vm.auth.isSignedIn()) "home" else "welcome")) }
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
            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                when (page) {
                    "welcome" -> WelcomeScreen { navigateTo("auth") }
                    "auth" -> AuthScreen(vm, onBack = navigateBack, onContinue = enterHome)
                    "home" -> HomeScreen(
                        vm,
                        onChoose = { mood -> vm.chooseMood(mood); navigateTo("chat") },
                        onSettings = { navigateTo("settings") }
                    )
                    "chat" -> ChatScreen(vm, onBack = navigateBack)
                    "settings" -> SettingsScreen(vm, onBack = navigateBack, onPrivacy = { navigateTo("privacy") }, onAuth = { navigateTo("auth") })
                    "privacy" -> PrivacyScreen(onBack = navigateBack)
                }
            }
        }
    }
}
