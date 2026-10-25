package com.letitout.app.ui

import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the actual picker through Android accessibility actions, without changing saved app data. */
@RunWith(AndroidJUnit4::class)
class CompanionPickerInteractionTest {
    @Test fun choosingMatureManUpdatesPortraitAndConfirmsSelection() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        CompanionPickerHostActivity.confirmed.set(null)
        val scenario = ActivityScenario.launch(CompanionPickerHostActivity::class.java)
        try {
            instrumentation.waitForIdleSync()

            tap("Man")
            tap("Mature")

            val portrait = awaitNode { root ->
                findDescendant(root) { it.contentDescription?.toString() == "Mature man companion portrait" }
            }
            assertNotNull("Portrait should reflect both choices", portrait)

            val matureNode = awaitNode { root -> findExactText(root, "Mature") }
            assertNotNull("Mature option should remain visible", matureNode)
            val matureAncestors = ancestors(matureNode!!).toList()
            assertTrue(
                "Mature option should expose selected state: " + matureAncestors.map {
                    "${it.className}: selected=${it.isSelected}, checked=${it.isChecked}, state=${it.stateDescription}"
                },
                matureAncestors.any { it.isSelected || it.isChecked || it.stateDescription?.contains("selected", ignoreCase = true) == true }
            )

            tap("Use this companion")
            assertEquals("Confirmed choice should match the visible portrait", "man" to "mature", CompanionPickerHostActivity.confirmed.get())
        } finally {
            scenario.close()
            CompanionPickerHostActivity.confirmed.set(null)
        }
    }

    private fun tap(text: String) {
        val node = awaitNode { root -> findExactText(root, text) }
        assertNotNull("Could not find $text in the picker", node)
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
