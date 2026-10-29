package com.letitout.app.ui

import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** Checks the release end screen through the accessibility tree, as a screen reader would see it. */
@RunWith(AndroidJUnit4::class)
class ReleaseRitualScreenTest {
    @Test fun crisisReleaseLeadsWithHelplinesAndHasNoShare() = withScreen(hadSupport = true) {
        assertNotNull("Helplines must be offered", awaitText("Call Tele-MANAS · 14416"))
        assertNotNull(awaitText("Call emergency · 112"))
        assertNull("A crisis release must not offer the share card", findText("Share"))
    }

    @Test fun ordinaryReleaseOffersShare() = withScreen(hadSupport = false) {
        assertNotNull(awaitText("Gone."))
        assertNotNull("The share card should be offered", awaitText("Share"))
        assertNull(findText("Call Tele-MANAS · 14416"))
    }

    private fun withScreen(hadSupport: Boolean, block: () -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val intent = Intent(instrumentation.targetContext, ReleaseRitualHostActivity::class.java)
            .putExtra(ReleaseRitualHostActivity.EXTRA_HAD_SUPPORT, hadSupport)
        val scenario = ActivityScenario.launch<ReleaseRitualHostActivity>(intent)
        try {
            instrumentation.waitForIdleSync()
            block()
        } finally {
            scenario.close()
        }
    }

    private fun awaitText(text: String): AccessibilityNodeInfo? {
        repeat(40) {
            findText(text)?.let { return it }
            SystemClock.sleep(100)
        }
        return null
    }

    private fun findText(text: String): AccessibilityNodeInfo? =
        InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow?.let { root ->
            findDescendant(root) { it.text?.toString() == text }
        }

    private fun findDescendant(root: AccessibilityNodeInfo, matches: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (matches(root)) return root
        for (index in 0 until root.childCount) {
            val child = root.getChild(index) ?: continue
            findDescendant(child, matches)?.let { return it }
        }
        return null
    }
}
