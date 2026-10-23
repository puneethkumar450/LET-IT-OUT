package com.letitout.server

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ReportRouteTest {
    private val goodToken = "good-token"
    private val hour = 60 * 60 * 1000L
    private val verifier: suspend (String) -> String? = { if (it == goodToken) "uid-1" else null }

    private class MemorySink(var failure: Exception? = null) : ReportSink {
        val saved = mutableListOf<StoredReport>()
        override fun save(report: StoredReport) {
            failure?.let { throw it }
            saved += report
        }
    }

    private fun deps(
        sink: ReportSink,
        verifyToken: (suspend (String) -> String?)? = verifier,
        reportLimiter: RateLimiter = RateLimiter(20, hour),
        reportGlobalLimiter: RateLimiter = RateLimiter(300, hour)
    ) = ChatDeps(verifyToken, provider = null, reportSink = sink, reportLimiter = reportLimiter, reportGlobalLimiter = reportGlobalLimiter)

    private fun reportJson(reason: String = "hurtful", replyText: String = "That reply felt dismissive.") =
        Json.encodeToString(ReportRequest.serializer(), ReportRequest(reason, replyText))

    private suspend fun ApplicationTestBuilder.report(body: String, token: String? = goodToken): HttpResponse =
        client.post("/v1/report") {
            if (token != null) header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(body)
        }

    private suspend fun HttpResponse.assertFriendlyError(status: HttpStatusCode) {
        assertEquals(status, this.status)
        val error = Json.parseToJsonElement(bodyAsText()).jsonObject["error"]!!.jsonPrimitive.content
        assertTrue(error.isNotBlank() && error.endsWith("."), error)
        assertNoBannedWords(error)
    }

    @Test fun reportIsStoredWithHashedReporterAndNeedsNoProvider() = testApplication {
        val sink = MemorySink()
        application { module(deps(sink)) }
        val response = report(reportJson("unsafe", "  Just ignore your doctor.  "))
        assertEquals(HttpStatusCode.NoContent, response.status)
        val saved = sink.saved.single()
        assertEquals("unsafe", saved.reason)
        assertEquals("Just ignore your doctor.", saved.replyText)
        assertEquals(reporterId("uid-1"), saved.reporter)
        assertNotEquals("uid-1", saved.reporter)
        assertEquals(16, saved.reporter.length)
    }

    @Test fun unconfiguredFirebaseReturns503() = testApplication {
        application { module(deps(MemorySink(), verifyToken = null)) }
        report(reportJson()).assertFriendlyError(HttpStatusCode.ServiceUnavailable)
    }

    @Test fun badOrMissingTokenReturns401() = testApplication {
        val sink = MemorySink()
        application { module(deps(sink)) }
        report(reportJson(), token = null).assertFriendlyError(HttpStatusCode.Unauthorized)
        report(reportJson(), token = "nope").assertFriendlyError(HttpStatusCode.Unauthorized)
        assertTrue(sink.saved.isEmpty())
    }

    @Test fun invalidReportsReturn400() = testApplication {
        val sink = MemorySink()
        application { module(deps(sink)) }
        for (body in listOf(
            reportJson(reason = "spam"),
            reportJson(replyText = "   "),
            reportJson(replyText = "a".repeat(2001)),
            "{\"reason\":\"hurtful\"}",
            "not json"
        )) report(body).assertFriendlyError(HttpStatusCode.BadRequest)
        assertTrue(sink.saved.isEmpty())
    }

    @Test fun oversizedBodyReturns413() = testApplication {
        application { module(deps(MemorySink())) }
        report(reportJson(replyText = "a".repeat(MAX_REPORT_CHARS))).assertFriendlyError(HttpStatusCode.PayloadTooLarge)
    }

    @Test fun twentyFirstReportInAnHourGets429() = testApplication {
        val sink = MemorySink()
        application { module(deps(sink)) }
        repeat(20) { assertEquals(HttpStatusCode.NoContent, report(reportJson()).status) }
        report(reportJson()).assertFriendlyError(HttpStatusCode.TooManyRequests)
        assertEquals(20, sink.saved.size)
    }

    @Test fun overallCapStopsReportsFromEveryoneAndGivesThePersonalSlotBack() = testApplication {
        var now = 0L
        val sink = MemorySink()
        val verifyAny: suspend (String) -> String? = { it }
        val global = RateLimiter(2, hour) { now }
        application { module(deps(sink, verifyToken = verifyAny, reportLimiter = RateLimiter(1, hour), reportGlobalLimiter = global)) }
        assertEquals(HttpStatusCode.NoContent, report(reportJson(), token = "uid-a").status)
        assertEquals(HttpStatusCode.NoContent, report(reportJson(), token = "uid-b").status)
        report(reportJson(), token = "uid-c").assertFriendlyError(HttpStatusCode.TooManyRequests)
        assertEquals(2, sink.saved.size)
        // Once the overall window moves on, uid-c still has its own report left.
        now = hour
        assertEquals(HttpStatusCode.NoContent, report(reportJson(), token = "uid-c").status)
    }

    @Test fun storageFailureReturns503AndGivesTheQuotaBack() = testApplication {
        val sink = MemorySink(failure = java.io.IOException("disk full"))
        val limiter = RateLimiter(1, hour)
        application { module(deps(sink, reportLimiter = limiter, reportGlobalLimiter = RateLimiter(1, hour))) }
        report(reportJson()).assertFriendlyError(HttpStatusCode.ServiceUnavailable)
        sink.failure = null
        assertEquals(HttpStatusCode.NoContent, report(reportJson()).status)
    }

    @Test fun fileSinkAppendsOneJsonLinePerReport() {
        val file = File(Files.createTempDirectory("reports").toFile(), "nested/reports.jsonl")
        val sink = FileReportSink(file)
        sink.save(StoredReport("2026-10-22T10:00:00Z", "wrong", "Line one\nwith a quote \"here\"", "abc"))
        sink.save(StoredReport("2026-10-22T10:01:00Z", "other", "Second", "def"))
        val lines = file.readLines()
        assertEquals(2, lines.size)
        val first = Json.decodeFromString(StoredReport.serializer(), lines[0])
        assertEquals("Line one\nwith a quote \"here\"", first.replyText)
        assertEquals("other", Json.decodeFromString(StoredReport.serializer(), lines[1]).reason)
    }

    @Test fun fileSinkStopsAtItsSizeCap() {
        val file = File(Files.createTempDirectory("reports").toFile(), "reports.jsonl")
        val report = StoredReport("2026-10-22T10:00:00Z", "wrong", "మీరు బాగున్నారా", "abc")
        val lineBytes = (Json.encodeToString(StoredReport.serializer(), report) + "\n").toByteArray().size.toLong()
        val sink = FileReportSink(file, maxBytes = lineBytes * 2)
        sink.save(report)
        sink.save(report)
        assertFailsWith<java.io.IOException> { sink.save(report) }
        assertEquals(lineBytes * 2, file.length())
    }
}
