package com.letitout.app.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.letitout.app.data.Mood
import com.letitout.app.data.palette
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@Composable
internal fun SplashScreen(companionGender: String, companionAge: String, onContinue: () -> Unit) {
    val latestOnContinue by rememberUpdatedState(onContinue)
    var continued by remember { mutableStateOf(false) }
    val continueOnce: () -> Unit = {
        if (!continued) {
            continued = true
            latestOnContinue()
        }
    }
    LaunchedEffect(Unit) { delay(1400); continueOnce() }
    Box(Modifier.fillMaxSize().background(AppColors.Background).clickable(onClickLabel = "Continue", onClick = continueOnce)) {
        Blob(342.dp, AppColors.Lavender, Modifier.align(Alignment.TopStart).offset((-94).dp, (-114).dp).alpha(.9f))
        Blob(365.dp, AppColors.Mint, Modifier.align(Alignment.BottomEnd).offset(105.dp, 153.dp).alpha(.9f))
        Blob(164.dp, AppColors.Peach, Modifier.align(Alignment.TopEnd).offset(89.dp, 310.dp).alpha(.8f))
        Column(Modifier.align(Alignment.Center).offset(y = (-5).dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(27.dp)) {
            Box(Modifier.size(184.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(174.dp).background(AppColors.Lavender, CircleShape))
                CompanionPortrait(
                    companionGender, companionAge, "calm",
                    modifier = Modifier.size(152.dp).clip(CircleShape),
                    contentDescription = "AI companion portrait"
                )
                BrandMark(Modifier.align(Alignment.BottomEnd), size = 52.dp, corner = 16.dp, glyph = 29.dp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("LET IT GO", style = DisplayStyle, fontSize = 39.sp, lineHeight = 44.sp, letterSpacing = 6.sp)
                Text("Your Feelings Matter.", color = AppColors.Muted, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Text(
            "An AI listening companion — not a therapist or a substitute for professional care.",
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 32.dp, vertical = 44.dp),
            color = AppColors.Muted, fontSize = 13.sp, lineHeight = 19.sp, textAlign = TextAlign.Center
        )
    }
}

private class OnboardingPage(val title: String, val body: String)

private val onboardingPages = listOf(
    OnboardingPage("A Space to Express Yourself", "Talk about whatever is on your mind."),
    OnboardingPage("Name How You Feel", "Pick a mood, or just start talking. No label needed."),
    OnboardingPage("Speak or Type", "Say it out loud or write it down, at your own pace."),
    OnboardingPage("Meet Your Companion", "Choose an adult portrait you feel comfortable talking to. You can change it anytime.")
)

@Composable
internal fun OnboardingScreen(onSkip: () -> Unit, onGetStarted: () -> Unit) {
    val pager = rememberPagerState { onboardingPages.size }
    Column(
        Modifier.fillMaxSize().background(AppColors.Background).systemBarsPadding().padding(top = 8.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.End) { LinkButton("Skip", AppColors.Muted, onSkip) }
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { index ->
            val page = onboardingPages[index]
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Box(Modifier.weight(1f).fillMaxWidth().clipToBounds(), contentAlignment = Alignment.Center) { OnboardingArt(index) }
                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(page.title, style = DisplayStyle, fontSize = 29.sp, lineHeight = 33.sp, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
                    Text(page.body, Modifier.widthIn(max = 322.dp), color = AppColors.Muted, fontSize = 16.sp, lineHeight = 22.sp, textAlign = TextAlign.Center)
                }
            }
        }
        PageDots(current = pager.currentPage, count = onboardingPages.size)
        Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            PillButton("Get Started", onGetStarted)
            Text(
                "You'll be talking with an AI, not a person. Messages are processed to generate replies and aren't saved on our server. For adults 18+.",
                Modifier.fillMaxWidth(), color = AppColors.Muted, fontSize = 12.sp, lineHeight = 18.sp, textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun OnboardingArt(page: Int) {
    Blob(300.dp, AppColors.Lavender)
    Blob(150.dp, AppColors.Peach, Modifier.offset(110.dp, (-90).dp))
    Blob(110.dp, AppColors.Mint, Modifier.offset((-120).dp, 100.dp))
    when (page) {
        0 -> Column(Modifier.width(236.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ArtBubble("Today was a lot. I don't even know where to start.", fromUser = false, Modifier.align(Alignment.Start))
            ArtBubble("That's okay. Start anywhere — I'm listening.", fromUser = true, Modifier.align(Alignment.End))
        }
        1 -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(Mood.ANGRY to Mood.SAD, Mood.STRESSED to Mood.HAPPY).forEach { (left, right) ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { ArtMoodTile(left); ArtMoodTile(right) }
            }
        }
        2 -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(120.dp).softShadow(AppColors.Primary, CircleShape, 10.dp).background(AppColors.Surface, CircleShape), contentAlignment = Alignment.Center) {
                Icon(AppIcons.Mic, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(52.dp))
            }
            ArtBubble("Speak or type — whatever feels easier.", fromUser = false)
        }
        else -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf("woman" to "young", "woman" to "adult", "man" to "mature").forEachIndexed { index, (gender, age) ->
                Box(
                    Modifier.size(if (index == 1) 100.dp else 84.dp)
                        .softShadow(AppColors.Primary, CircleShape, 5.dp)
                        .background(AppColors.Surface, CircleShape)
                        .padding(4.dp).clip(CircleShape)
                ) {
                    CompanionPortrait(gender, age, "calm", Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun ArtBubble(text: String, fromUser: Boolean, modifier: Modifier = Modifier) {
    val shape = if (fromUser) RoundedCornerShape(24.dp, 24.dp, 6.dp, 24.dp) else RoundedCornerShape(24.dp, 24.dp, 24.dp, 6.dp)
    val color = if (fromUser) AppColors.Primary else AppColors.Surface
    Text(
        text, modifier.width(234.dp).softShadow(if (fromUser) AppColors.Primary else AppColors.Ink, shape, 8.dp)
            .background(color, shape).padding(horizontal = 16.dp, vertical = 18.dp),
        color = if (fromUser) Color.White else AppColors.Ink, fontSize = 14.sp, lineHeight = 19.sp
    )
}

@Composable
private fun ArtMoodTile(mood: Mood) {
    val palette = mood.palette()
    Column(
        Modifier.width(112.dp).background(palette.tile, RoundedCornerShape(22.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(36.dp).background(AppColors.Surface, CircleShape), contentAlignment = Alignment.Center) {
            Icon(mood.icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(20.dp))
        }
        Text(mood.label, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PageDots(current: Int, count: Int) {
    Row(
        Modifier.fillMaxWidth().semantics { contentDescription = "Page ${current + 1} of $count" },
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        repeat(count) { index ->
            val width by animateDpAsState(if (index == current) 24.dp else 8.dp, label = "dot")
            Box(Modifier.size(width, 8.dp).background(if (index == current) AppColors.Primary else AppColors.Track, CircleShape))
        }
    }
}

@Composable
internal fun SignInScreen(vm: AppViewModel, onSignedIn: () -> Unit, onPreview: () -> Unit, onPrivacy: () -> Unit) {
    val activity = requireNotNull(LocalActivity.current)
    val scope = rememberCoroutineScope()
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val configured = vm.auth.isConfigured()
    fun attempt(fallback: String, action: suspend () -> Unit) {
        scope.launch {
            working = true; error = null
            try { withTimeout(60_000) { action() }; vm.refreshAuth(); onSignedIn() }
            catch (_: TimeoutCancellationException) { error = "Sign-in took too long. Please try again." }
            catch (t: CancellationException) { throw t }
            catch (t: Exception) { error = t.message ?: fallback }
            finally { working = false }
        }
    }
    ScrollScreen(
        AppColors.Background, PaddingValues(start = 24.dp, top = 23.dp, end = 24.dp, bottom = 0.dp),
        decor = { Blob(340.dp, AppColors.Lavender, Modifier.align(Alignment.TopEnd).offset(120.dp, (-140).dp)) }
    ) {
        BrandMark(size = 56.dp, corner = 15.dp, glyph = 31.dp)
        Spacer(Modifier.height(56.dp))
        Text("Welcome", style = DisplayStyle, fontSize = 32.sp, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(10.dp))
        Text("Sign in to keep your preferences across devices, or just start talking as a guest.", color = AppColors.Muted, fontSize = 16.sp, lineHeight = 24.sp)
        Spacer(Modifier.height(72.dp))
        OutlinePillButton("Continue with Google", enabled = vm.auth.isGoogleConfigured() && !working, leading = { GoogleBadge() }, onClick = {
            attempt("Google sign-in didn't complete") { vm.auth.signInGoogle(activity) }
        })
        Spacer(Modifier.height(14.dp))
        PillButton("Continue as guest", enabled = configured && !working, onClick = {
            attempt("Guest sign-in failed") { vm.auth.signInGuest() }
        })
        Spacer(Modifier.height(14.dp))
        Text(
            "Guest mode needs no account. You can link Google later in Settings and keep your preferences.",
            Modifier.fillMaxWidth(), color = AppColors.Muted, fontSize = 13.sp, lineHeight = 19.sp, textAlign = TextAlign.Center
        )
        if (working) {
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).size(28.dp), color = AppColors.Primary, strokeWidth = 3.dp)
        }
        error?.let { Spacer(Modifier.height(14.dp)); Text(it, color = AppColors.Danger, fontSize = 14.sp, lineHeight = 20.sp) }
        if (!configured) {
            Spacer(Modifier.height(20.dp))
            Column(Modifier.fillMaxWidth().background(AppColors.Lavender, RoundedCornerShape(20.dp)).padding(start = 16.dp, top = 14.dp, end = 8.dp, bottom = 4.dp)) {
                Text("Sign-in is unavailable until Firebase is configured. You can still look around in preview.", fontSize = 14.sp, lineHeight = 20.sp)
                LinkButton("Explore in preview", AppColors.Primary, onPreview, Modifier.align(Alignment.End))
            }
        } else if (!vm.auth.isGoogleConfigured()) {
            Spacer(Modifier.height(20.dp))
            Text("Google sign-in is still being set up. You can continue as a guest.", color = AppColors.Muted, fontSize = 13.sp, lineHeight = 19.sp)
        }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(32.dp))
        val cardShape = RoundedCornerShape(20.dp)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 93.dp).softShadow(AppColors.Ink, cardShape, 3.dp).background(AppColors.Surface, cardShape).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(AppIcons.ShieldCheck, contentDescription = null, tint = AppColors.Green, modifier = Modifier.size(24.dp))
            Text("We never see your Google password. Sign-in\nonly shares your name and email with\nLet It Go.", fontSize = 13.sp, lineHeight = 18.sp)
        }
        Spacer(Modifier.height(16.dp))
        val linkStyle = TextLinkStyles(SpanStyle(color = AppColors.Primary, fontWeight = FontWeight.Bold))
        Text(
            buildAnnotatedString {
                append("By continuing you agree to the ")
                withLink(LinkAnnotation.Clickable("terms", linkStyle) { onPrivacy() }) { append("Terms") }
                append(" and ")
                withLink(LinkAnnotation.Clickable("privacy", linkStyle) { onPrivacy() }) { append("Privacy Policy") }
                append(". Let It Go is for adults 18 and older.")
            },
            Modifier.fillMaxWidth(), color = AppColors.Muted, fontSize = 11.sp, lineHeight = 16.sp, textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GoogleBadge() {
    Box(Modifier.size(26.dp).border(2.dp, AppColors.Primary, CircleShape), contentAlignment = Alignment.Center) {
        Text("G", fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = AppColors.Primary)
    }
}
