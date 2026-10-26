package com.letitout.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import java.util.concurrent.atomic.AtomicBoolean

/** Debug-only host so instrumentation can exercise first-time companion setup. */
class CompanionSetupHostActivity : ComponentActivity() {
    companion object {
        val confirmed = AtomicBoolean(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CompanionSetupScreen(
                    onContinue = { confirmed.set(true) },
                    onBack = {}
                )
            }
        }
    }
}
