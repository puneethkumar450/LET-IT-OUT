package com.letitout.server

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

@Serializable data class ReportRequest(val reason: String, val replyText: String)

/** What is kept for review: the flagged reply, the reason, when, and a one-way hash of the reporter. Never their own messages. */
@Serializable data class StoredReport(val at: String, val reason: String, val replyText: String, val reporter: String)

internal val reportReasons = setOf("hurtful", "unsafe", "wrong", "other")

fun interface ReportSink { fun save(report: StoredReport) }

/** Appends one JSON line per report. Point REPORTS_FILE at durable, access-controlled storage in production. */
class FileReportSink(private val file: File) : ReportSink {
    private val json = Json { encodeDefaults = true }
    override fun save(report: StoredReport) {
        val line = json.encodeToString(StoredReport.serializer(), report) + "\n"
        synchronized(this) {
            file.absoluteFile.parentFile?.mkdirs()
            file.appendText(line)
        }
    }
}

internal fun reporterId(uid: String): String =
    MessageDigest.getInstance("SHA-256").digest(uid.toByteArray()).joinToString("") { "%02x".format(it) }.take(16)
