package com.letitout.server

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SafetySignalsTest {
    @Test fun checksExplicitRiskPhrases() {
        assertTrue(mayNeedImmediateHelp("I want to die"))
        assertTrue(mayNeedImmediateHelp("I will kill myself"))
    }
    @Test fun doesNotLabelOrdinaryWorkplaceComplaints() {
        assertFalse(mayNeedImmediateHelp("My manager upset me at work"))
    }
}
