package com.letitout.server

import kotlin.test.assertFalse

internal val bannedWords = Regex(
    "\\b(crisis|emergency|emergencies|suicid\\w*|kill\\w*|die|dies|dying|death|danger\\w*|life-threatening|safe)\\b",
    RegexOption.IGNORE_CASE
)

internal fun assertNoBannedWords(text: String) =
    assertFalse(bannedWords.containsMatchIn(text), "Banned word '${bannedWords.find(text)?.value}' in: $text")

internal class FakeProvider(var reply: String = "Oh no. That sounds like a really long day.", var failure: Exception? = null) :
    ReplyProvider {
    val calls = mutableListOf<Pair<String, List<Message>>>()
    /** Replies handed out first, one per call, before falling back to [reply]. */
    val queued = ArrayDeque<String>()
    override suspend fun reply(systemPrompt: String, messages: List<Message>): String {
        calls += systemPrompt to messages
        failure?.let { throw it }
        return queued.removeFirstOrNull() ?: reply
    }
}
