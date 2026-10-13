# AI integration: free tier first

- **Initial model:** Groq `openai/gpt-oss-120b` via `https://api.groq.com/openai/v1/chat/completions`.
- **Fallback candidate:** `openai/gpt-oss-20b`, only after response-quality and safety comparisons.
- **Budget controls:** cap prompt history to 12 entries, 2,000 characters per message, output 220 tokens, and at most 20 requests per signed-in Firebase UID per hour in the prototype.
- **Security:** backend-only Groq key; verified Firebase auth; never log prompts or responses.
- **Privacy:** review Groq's current privacy/retention policies and obtain appropriate disclosures and consent before real emotional support use.
- **Safety:** initial phrases are *illustrative*. Introduce expert-reviewed detection, multi-turn handling, post-response safety validation, and testing before closed/public beta.
- **Provider failures:** report a safe generic error, do not print response bodies from Groq. UI has manual retry.
- **Later:** assess more expressive real-time speech and offline Gemma/LiteRT-LM only after V1 usage data.

References: https://console.groq.com/docs/api-reference and https://console.groq.com/docs/models
