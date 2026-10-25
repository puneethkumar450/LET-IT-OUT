package com.letitout.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import java.util.concurrent.atomic.AtomicReference

/** Debug-only host so instrumentation can exercise the picker without app account or preferences. */
class CompanionPickerHostActivity : ComponentActivity() {
    companion object {
        val confirmed = AtomicReference<Pair<String, String>?>(null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CompanionPickerScreen(
                    selectedGender = "woman",
                    selectedAge = "adult",
                    onSelect = { gender, age -> confirmed.set(gender to age) },
                    onBack = {}
                )
            }
        }
    }
}
