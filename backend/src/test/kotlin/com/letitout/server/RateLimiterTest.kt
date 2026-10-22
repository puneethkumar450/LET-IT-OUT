package com.letitout.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RateLimiterTest {
    @Test fun allowsUpToLimitWithinWindow() {
        var now = 0L
        val limiter = RateLimiter(2, 1_000) { now }
        assertTrue(limiter.tryAcquire("a"))
        now = 400
        assertTrue(limiter.tryAcquire("a"))
        assertFalse(limiter.tryAcquire("a"))
        assertTrue(limiter.tryAcquire("b"))
        now = 999
        assertFalse(limiter.tryAcquire("a"))
        now = 1_000
        assertTrue(limiter.tryAcquire("a"))
        assertFalse(limiter.tryAcquire("a"))
        now = 1_400
        assertTrue(limiter.tryAcquire("a"))
    }

    @Test fun evictsIdleKeys() {
        var now = 0L
        val limiter = RateLimiter(5, 1_000) { now }
        repeat(100) { limiter.tryAcquire("user-$it") }
        assertEquals(100, limiter.trackedKeys)
        now = 1_500
        assertTrue(limiter.tryAcquire("someone-new"))
        assertEquals(1, limiter.trackedKeys)
    }

    @Test fun zeroLimitNeverStoresKeys() {
        val limiter = RateLimiter(0, 1_000) { 0L }
        assertFalse(limiter.tryAcquire("a"))
        assertEquals(0, limiter.trackedKeys)
    }
}
