package com.letitout.app.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.letitout.app.ui.AppColors
import com.letitout.app.ui.AppViewModel
import com.letitout.app.ui.BrandMark
import com.letitout.app.ui.components.PageBackButton
import kotlinx.coroutines.launch

@Composable
internal fun AuthScreen(vm: AppViewModel, onBack: () -> Unit, onContinue: () -> Unit) {
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
        Text("Choose how you'd like to enter your space. You can always look around before signing in.", color = AppColors.Muted, fontSize = 16.sp, lineHeight = 23.sp)
        Spacer(Modifier.height(36.dp))
        Button(
            onClick = {
                scope.launch {
                    working = true; error = null
                    try {
                        val signedIn = vm.auth.signInGoogle(activity)
                        if (signedIn) { vm.refreshAuth(); onContinue() }
                    } catch (t: Exception) {
                        vm.refreshAuth() // a failed switch from a guest can leave nobody signed in
                        error = t.message ?: "Google sign-in didn't go through. Please try again."
                    } finally {
                        working = false
                    }
                }
            }, enabled = !working && configured, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp)
        ) { Icon(Icons.Default.AccountCircle, null); Spacer(Modifier.width(10.dp)); Text("Continue with Google", fontWeight = FontWeight.SemiBold) }
        // A guest arriving from Settings is here to switch to Google, so don't offer a second guest session.
        if (!vm.auth.isGuest()) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    scope.launch {
                        working = true; error = null
                        try { vm.auth.signInGuest(); vm.refreshAuth(); onContinue() }
                        catch (t: Exception) { error = t.message ?: "Couldn't start a guest session just now. Please try again." }
                        finally { working = false }
                    }
                }, enabled = !working && configured, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp)
            ) { Text("Continue as guest", fontWeight = FontWeight.SemiBold) }
        }
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
        error?.let { Spacer(Modifier.height(14.dp)); Text(it, color = AppColors.Error, fontSize = 13.sp, lineHeight = 19.sp) }
        Spacer(Modifier.height(31.dp))
        Text("AI can make mistakes and is not a replacement for professional care.", fontSize = 12.sp, lineHeight = 18.sp, color = AppColors.Muted)
    }
}
