package com.letitout.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme

/** Debug-only host so instrumentation can check both release end screens. */
class ReleaseRitualHostActivity : ComponentActivity() {
    companion object {
        const val EXTRA_HAD_SUPPORT = "had_support"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val hadSupport = intent.getBooleanExtra(EXTRA_HAD_SUPPORT, false)
        setContent {
            MaterialTheme {
                ReleaseRitualScreen(hadSupport = hadSupport, onTalkAgain = {}, onHome = {})
            }
        }
    }
}
