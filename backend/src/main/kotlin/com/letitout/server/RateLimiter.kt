package com.letitout.server

import java.util.concurrent.ConcurrentHashMap

/** Sliding-window limiter per key. Keys with no hits left in the window are dropped, so memory stays bounded. */
class RateLimiter(
    private val limit: Int,
    private val windowMillis: Long,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val hits = ConcurrentHashMap<String, ArrayDeque<Long>>()
    @Volatile private var lastSweep = clock()

    internal val trackedKeys: Int get() = hits.size

    fun tryAcquire(key: String): Boolean {
        val now = clock()
        sweep(now)
        var allowed = false
        hits.compute(key) { _, existing ->
            val times = existing ?: ArrayDeque()
            times.dropExpired(now)
            allowed = times.size < limit
            if (allowed) times.addLast(now)
            times.takeIf { it.isNotEmpty() }
        }
        return allowed
    }

    /** Gives back the latest hit for [key], for a request that never got a reply. */
    fun release(key: String) {
        hits.computeIfPresent(key) { _, times -> times.removeLastOrNull(); times.takeIf { it.isNotEmpty() } }
    }

    private fun sweep(now: Long) {
        if (now - lastSweep < windowMillis) return
        lastSweep = now
        for (key in hits.keys) hits.computeIfPresent(key) { _, times -> times.dropExpired(now); times.takeIf { it.isNotEmpty() } }
    }

    private fun ArrayDeque<Long>.dropExpired(now: Long) {
        while (isNotEmpty() && now - first() >= windowMillis) removeFirst()
    }
}
