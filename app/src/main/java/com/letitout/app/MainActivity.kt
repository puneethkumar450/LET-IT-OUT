package com.letitout.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.letitout.app.data.Mood
import com.letitout.app.data.palette
import com.letitout.app.ui.AppColors
import com.letitout.app.ui.AppTypography
import com.letitout.app.ui.AppViewModel
import com.letitout.app.ui.ChatScreen
import com.letitout.app.ui.CompanionPickerScreen
import com.letitout.app.ui.HomeScreen
import com.letitout.app.ui.MoodCheckInScreen
import com.letitout.app.ui.OnboardingScreen
import com.letitout.app.ui.PrivacyHelpScreen
import com.letitout.app.ui.SettingsScreen
import com.letitout.app.ui.SignInScreen
import com.letitout.app.ui.SplashScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val transparent = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(SystemBarStyle.light(transparent, transparent), SystemBarStyle.light(transparent, transparent))
        super.onCreate(savedInstanceState)
        setContent { LetItOutApp() }
    }
}

private val appColorScheme = lightColorScheme(
    primary = AppColors.Primary,
    onPrimary = Color.White,
    secondaryContainer = AppColors.Lavender,
    onSecondaryContainer = AppColors.Primary,
    background = AppColors.Background,
    onBackground = AppColors.Ink,
    surface = AppColors.Surface,
    onSurface = AppColors.Ink,
    surfaceVariant = AppColors.Lavender,
    onSurfaceVariant = AppColors.Muted,
    surfaceContainer = AppColors.Surface,
    surfaceContainerHigh = AppColors.Surface,
    outline = AppColors.Track,
    outlineVariant = AppColors.Divider,
    error = AppColors.Danger
)

@Composable
private fun LetItOutApp(vm: AppViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var backStack by rememberSaveable { mutableStateOf(listOf("splash")) }
    var checkInMood by rememberSaveable { mutableStateOf(Mood.ANGRY) }
    val page = backStack.last()
    val navigateTo: (String) -> Unit = { destination -> backStack = backStack + destination }
    val resetTo: (String) -> Unit = { destination -> backStack = listOf(destination) }
    val navigateBack: () -> Unit = {
        if (backStack.size > 1) backStack = backStack.dropLast(1)
    }
    val popTo: (String) -> Unit = { destination ->
        val index = backStack.lastIndexOf(destination)
        backStack = if (index >= 0) backStack.take(index + 1) else listOf(destination)
    }
    // After sign-in, return to where it was opened from (Settings, a chat), or start at Home.
    val finishSignIn: () -> Unit = {
        val previous = backStack.getOrNull(backStack.size - 2)
        if (previous == null || previous == "onboarding" || previous == "companion") resetTo("home") else navigateBack()
    }
    BackHandler(enabled = backStack.size > 1, onBack = navigateBack)
    MaterialTheme(colorScheme = appColorScheme, typography = AppTypography) {
        Surface(color = AppColors.Background, modifier = Modifier.fillMaxSize()) {
            when (page) {
                "splash" -> SplashScreen(state.companion.gender, state.companion.age) {
                    resetTo(if (state.signedIn || state.settings.onboarded) "home" else "onboarding")
                }
                "onboarding" -> OnboardingScreen(
                    onSkip = { vm.finishOnboarding(); resetTo("home") },
                    onGetStarted = { navigateTo("companion") }
                )
                "companion" -> CompanionPickerScreen(
                    selectedGender = state.companion.gender,
                    selectedAge = state.companion.age,
                    onSelect = { gender, age ->
                        vm.chooseCompanion(gender, age)
                        vm.finishOnboarding()
                        navigateTo("signin")
                    },
                    onBack = navigateBack
                )
                "signin" -> SignInScreen(vm, onSignedIn = finishSignIn, onPreview = finishSignIn, onPrivacy = { navigateTo("privacy") })
                "home" -> HomeScreen(
                    onMood = { mood -> checkInMood = mood; navigateTo("mood") },
                    onJustTalk = { vm.chooseMood(Mood.TALK); navigateTo("chat") },
                    onLetsTalk = { navigateTo("chat") },
                    onNewConversation = { vm.newConversation(); navigateTo("chat") },
                    onSettings = { navigateTo("settings") }
                )
                "mood" -> MoodCheckInScreen(
                    mood = checkInMood,
                    palette = if (state.settings.followMoodTheme) checkInMood.palette() else Mood.TALK.palette(),
                    onBack = navigateBack,
                    onChangeMood = navigateBack,
                    onStart = { intensity -> vm.chooseMood(checkInMood, intensity); navigateTo("chat") }
                )
                "chat" -> ChatScreen(
                    vm,
                    onHome = { popTo("home") },
                    onChangeMood = { popTo("home") },
                    onHumanSupport = { navigateTo("privacy") },
                    onSignIn = { navigateTo("signin") }
                )
                "settings" -> SettingsScreen(
                    vm,
                    onBack = navigateBack,
                    onPrivacy = { navigateTo("privacy") },
                    onSignIn = { navigateTo("signin") },
                    onSignedOut = { resetTo("signin") }
                )
                "privacy" -> PrivacyHelpScreen(onBack = navigateBack)
            }
        }
    }
}
