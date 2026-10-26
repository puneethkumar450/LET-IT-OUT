package com.letitout.app.ui

import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises first-time companion setup through Android accessibility actions. */
@RunWith(AndroidJUnit4::class)
class CompanionSetupInteractionTest {
    @Test fun firstTimeSetupShowsAriPortraitWithoutGenderChoices() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        CompanionSetupHostActivity.confirmed.set(false)
        val scenario = ActivityScenario.launch(CompanionSetupHostActivity::class.java)
        try {
            instrumentation.waitForIdleSync()

            val portrait = awaitNode { root ->
                findDescendant(root) { it.contentDescription?.toString() == "Ari, your AI companion" }
            }
            assertNotNull("Setup should show the generated Ari portrait", portrait)
            val root = instrumentation.uiAutomation.rootInActiveWindow
            assertNull("Gender selection must be hidden", root?.let { findExactText(it, "Woman") })
            assertNull("Gender selection must be hidden", root?.let { findExactText(it, "Man") })
            assertNotNull(
                "Setup should disclose live voice sharing",
                root?.let { findExactText(it, "Spoken replies go to Simli for lip sync. Your chat summary is one tap away.") }
            )

            tap("Continue with Ari")
            assertTrue("Continue should finish companion setup", CompanionSetupHostActivity.confirmed.get())
        } finally {
            scenario.close()
            CompanionSetupHostActivity.confirmed.set(false)
        }
    }

    private fun tap(text: String) {
        val node = awaitNode { root -> findExactText(root, text) }
        assertNotNull("Could not find $text in setup", node)
        val target = ancestors(node!!).firstOrNull { it.isClickable }
        assertNotNull("$text has no clickable ancestor", target)
        target!!.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.id)
        assertTrue("Could not tap $text", target.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private fun awaitNode(search: (AccessibilityNodeInfo) -> AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        repeat(40) {
            automation.rootInActiveWindow?.let { root -> search(root)?.let { return it } }
            SystemClock.sleep(100)
        }
        return null
    }

    private fun findExactText(root: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? =
        findDescendant(root) { it.text?.toString() == text }

    private fun ancestors(start: AccessibilityNodeInfo): Sequence<AccessibilityNodeInfo> =
        generateSequence(start) { it.parent }

    private fun findDescendant(
        root: AccessibilityNodeInfo,
        matches: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        if (matches(root)) return root
        for (index in 0 until root.childCount) {
            val child = root.getChild(index) ?: continue
            findDescendant(child, matches)?.let { return it }
        }
        return null
    }
}
