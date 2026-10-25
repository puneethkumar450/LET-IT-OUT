package com.letitout.server

import com.google.cloud.Timestamp
import com.google.firebase.cloud.FirestoreClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.time.Instant

internal interface RateLimiter {
    suspend fun allow(uid: String): Boolean
}

/** Useful for local development only: a restart or second instance resets this limit. */
internal class LocalRateLimiter(private val perUserLimit: Int, private val globalLimit: Int) : RateLimiter {
    private var hour = -1L
    private var globalCount = 0
    private val userCounts = mutableMapOf<String, Int>()

    override suspend fun allow(uid: String): Boolean = synchronized(this) {
        val currentHour = Instant.now().epochSecond / 3600
        if (hour != currentHour) {
            hour = currentHour
            globalCount = 0
            userCounts.clear()
        }
        val userCount = userCounts[uid] ?: 0
        if (userCount >= perUserLimit || globalCount >= globalLimit) false
        else {
            userCounts[uid] = userCount + 1
            globalCount++
            true
        }
    }
}

/** Atomic shared limits across Cloud Run instances. No raw UID or chat text is stored. */
internal class FirestoreRateLimiter(
    private val perUserLimit: Int,
    private val globalLimit: Int,
    collectionName: String = "chat_rate_limits"
) : RateLimiter {
    private val firestore = FirestoreClient.getFirestore()
    private val collection = firestore.collection(collectionName)

    override suspend fun allow(uid: String): Boolean = withContext(Dispatchers.IO) {
        val hour = Instant.now().epochSecond / 3600
        val digest = MessageDigest.getInstance("SHA-256").digest(uid.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        val userDoc = collection.document("u_${hour}_$digest")
        val globalDoc = collection.document("g_$hour")
        val expiresAt = Timestamp.ofTimeSecondsAndNanos((hour + 48) * 3600, 0)
        firestore.runTransaction { transaction ->
            val userCount = transaction.get(userDoc).get().getLong("count") ?: 0L
            val globalCount = transaction.get(globalDoc).get().getLong("count") ?: 0L
            if (userCount >= perUserLimit || globalCount >= globalLimit) false
            else {
                transaction.set(userDoc, mapOf("count" to userCount + 1, "expiresAt" to expiresAt))
                transaction.set(globalDoc, mapOf("count" to globalCount + 1, "expiresAt" to expiresAt))
                true
            }
        }.get()
    }
}
