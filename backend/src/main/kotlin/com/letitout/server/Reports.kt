package com.letitout.server

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.security.MessageDigest

@Serializable data class ReportRequest(val reason: String, val replyText: String)

/** What is kept for review: the flagged reply, the reason, when, and a one-way hash of the reporter. Never their own messages. */
@Serializable data class StoredReport(val at: String, val reason: String, val replyText: String, val reporter: String)

internal val reportReasons = setOf("hurtful", "unsafe", "wrong", "other")

fun interface ReportSink { fun save(report: StoredReport) }

/**
 * Appends one JSON line per report and stops (throws) once the file would pass [maxBytes]. Point REPORTS_FILE at durable,
 * access-controlled storage in production.
 */
class FileReportSink(private val file: File, private val maxBytes: Long = DEFAULT_MAX_BYTES) : ReportSink {
    private val json = Json { encodeDefaults = true }
    override fun save(report: StoredReport) {
        val bytes = (json.encodeToString(StoredReport.serializer(), report) + "\n").toByteArray()
        synchronized(this) {
            file.absoluteFile.parentFile?.mkdirs()
            if (file.length() + bytes.size > maxBytes) throw IOException("reports file full")
            file.appendBytes(bytes)
        }
    }

    companion object {
        const val DEFAULT_MAX_BYTES = 50L * 1024 * 1024

        fun fromEnvironment(env: (String) -> String? = System::getenv): FileReportSink = FileReportSink(
            File(env("REPORTS_FILE")?.takeIf { it.isNotBlank() } ?: "reports.jsonl"),
            env("REPORTS_MAX_BYTES")?.trim()?.toLongOrNull()?.takeIf { it > 0 } ?: DEFAULT_MAX_BYTES
        )
    }
}

internal fun reporterId(uid: String): String =
    MessageDigest.getInstance("SHA-256").digest(uid.toByteArray()).joinToString("") { "%02x".format(it) }.take(16)
