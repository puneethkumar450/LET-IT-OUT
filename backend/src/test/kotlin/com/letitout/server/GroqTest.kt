package com.letitout.server

import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroqTest {
    @Test fun parsesVisibleContent() {
        assertEquals("Hey there.", parseGroqContent("""{"choices":[{"message":{"role":"assistant","content":"  Hey there.  "}}]}"""))
    }

    @Test fun ignoresReasoningField() {
        val body = """{"choices":[{"finish_reason":"stop","message":{"role":"assistant","reasoning":"They seem low. Keep it short.","content":"Oh no. What happened?"}}]}"""
        assertEquals("Oh no. What happened?", parseGroqContent(body))
    }

    @Test fun blankOrMissingContentIsNull() {
        assertNull(parseGroqContent("""{"choices":[{"message":{"role":"assistant","content":"   "}}]}"""))
        assertNull(parseGroqContent("""{"choices":[{"message":{"role":"assistant","reasoning":"thinking only"}}]}"""))
        assertNull(parseGroqContent("""{"choices":[{"message":{"role":"assistant","content":null}}]}"""))
        assertNull(parseGroqContent("""{"choices":[]}"""))
        assertNull(parseGroqContent("""{"error":{"message":"bad"}}"""))
        assertNull(parseGroqContent("not json"))
    }

    @Test fun polishesReplies() {
        assertEquals("Hey, I'm here.", polishReply("  \"Hey, I'm here.\"  "))
        assertEquals("You called it “a joke”, and that hurt.", polishReply("“You called it “a joke”, and that hurt.”"))
        assertEquals("\"Never again\" is what you said, and \"never\" is a big word.",
            polishReply("\"Never again\" is what you said, and \"never\" is a big word."))
        assertEquals("That was huge for you.", polishReply("That was **huge** for you."))
        assertEquals("one thing\nanother thing", polishReply("- one thing\n- another thing"))
        assertEquals("first\n\nsecond", polishReply("first\n\n\n\nsecond"))
    }

    @Test fun capsLongRepliesAtASentence() {
        val long = polishReply("This is one calm sentence. ".repeat(200))
        assertTrue(long.length <= 1_800)
        assertTrue(long.endsWith("."))
        val noSentences = polishReply("word ".repeat(600))
        assertTrue(noSentences.length <= 1_801)
        assertTrue(noSentences.endsWith("…"))
    }

    @Test fun requestUsesConfiguredBudget() {
        val provider = GroqProvider.fromEnvironment { mapOf("GROQ_API_KEY" to "test-key")[it] }!!
        val body = Json.parseToJsonElement(provider.requestBody("system text", listOf(Message("user", "hi")))).jsonObject
        assertEquals("openai/gpt-oss-120b", body["model"]!!.jsonPrimitive.content)
        assertEquals(0.8, body["temperature"]!!.jsonPrimitive.double)
        assertEquals(1024, body["max_completion_tokens"]!!.jsonPrimitive.int)
        assertEquals("medium", body["reasoning_effort"]!!.jsonPrimitive.content)
        val messages = body["messages"]!!.jsonArray
        assertEquals("system", messages[0].jsonObject["role"]!!.jsonPrimitive.content)
        assertEquals("system text", messages[0].jsonObject["content"]!!.jsonPrimitive.content)
        assertEquals("hi", messages[1].jsonObject["content"]!!.jsonPrimitive.content)
    }

    @Test fun environmentOverridesAndFallbacks() {
        assertNull(GroqProvider.fromEnvironment { null })
        assertNull(GroqProvider.fromEnvironment { if (it == "GROQ_API_KEY") "  " else null })
        val custom = GroqProvider.fromEnvironment {
            mapOf("GROQ_API_KEY" to "k", "GROQ_MAX_COMPLETION_TOKENS" to "2048", "GROQ_REASONING_EFFORT" to "HIGH")[it]
        }!!
        val customBody = Json.parseToJsonElement(custom.requestBody("s", emptyList())).jsonObject
        assertEquals(2048, customBody["max_completion_tokens"]!!.jsonPrimitive.int)
        assertEquals("high", customBody["reasoning_effort"]!!.jsonPrimitive.content)
        val invalid = GroqProvider.fromEnvironment {
            mapOf("GROQ_API_KEY" to "k", "GROQ_MAX_COMPLETION_TOKENS" to "lots", "GROQ_REASONING_EFFORT" to "max")[it]
        }!!
        val invalidBody = Json.parseToJsonElement(invalid.requestBody("s", emptyList())).jsonObject
        assertEquals(1024, invalidBody["max_completion_tokens"]!!.jsonPrimitive.int)
        assertEquals("medium", invalidBody["reasoning_effort"]!!.jsonPrimitive.content)
        val llama = GroqProvider.fromEnvironment { mapOf("GROQ_API_KEY" to "k", "GROQ_MODEL" to "llama-3.3-70b-versatile")[it] }!!
        assertFalse("reasoning_effort" in Json.parseToJsonElement(llama.requestBody("s", emptyList())).jsonObject)
    }
}
