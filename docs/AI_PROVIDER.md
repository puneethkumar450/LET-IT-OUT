# AI integration: free tier first

- **Initial model:** Groq `openai/gpt-oss-120b` via `https://api.groq.com/openai/v1/chat/completions`, overridable with `GROQ_MODEL`.
- **Fallback candidate:** `openai/gpt-oss-20b`, only after response-quality and safety comparisons.
- **Request settings** (`Groq.kt`):
  - temperature 0.8.
  - `max_completion_tokens` comes from `GROQ_MAX_COMPLETION_TOKENS`: default 2048, values from 64 to 65,536 accepted.
  - `reasoning_effort` comes from `GROQ_REASONING_EFFORT` (`low`, `medium` or `high`; default `medium`). It is sent only to `openai/gpt-oss*` models, because other Groq models reject it.
  - gpt-oss spends part of the completion budget on hidden reasoning. That is why the old 220-token cap could produce empty or cut-off replies. Only the visible `content` is used; any `reasoning` field is ignored. If a reply still hits the limit, only its complete sentences are kept and the server logs a hint to raise the budget.
  - Timeouts: 7 s to connect, 30 s per request.
- **Budget controls:**
  - Prompt history is capped at 12 messages of 2,000 characters each; the raw request body at 64,000 characters.
  - Per signed-in Firebase UID: 20 normal requests per hour. Heavy-distress turns (and softer, possibly heavy ones) have a separate 30 per hour and can borrow from the normal quota. After that they get a canned warm reply with no provider call.
  - Across all users: at most `GLOBAL_REQUESTS_PER_MINUTE` provider calls per minute (default 60), so a crowd of guest accounts cannot run up unbounded spend.
  - A request that ends without a reply gives its quota back. All limits live in one server's memory.
- **Persona** (`Persona.kt`, server-side only):
  - A base voice: a close friend with a good therapist's ear. It reflects first, asks at most one question, and usually keeps replies to 1–3 short sentences, with no markdown, lists or emojis. It avoids stock phrases such as "As an AI" or "I understand how you feel" and mirrors the person's language and register.
  - Per-mood guidance, plus an extra-care note on heavy turns, three fallback replies in English and three each in Hindi and Telugu (picked by the script of the person's last message; the translations still need a native speaker's review), and a short neutral line.
  - When the app sends a self-rated intensity from 1 to 10, the prompt gets "Self-rated intensity: N/10. Treat it as a hint, not a measurement." just before the final mood line. Other values are left out without an error, and the rating never adds the extra-care note.
  - It suggests a doctor only for persistent or serious struggles. It closes conversations warmly and invites people back, without making itself their only support.
- **Reply clean-up:** strips wrapping quotes, `**`, `*emphasis*`, `__`, backticks, bullets, numbered-list markers and headings, collapses runs of blank lines, and caps long replies at about 1,800 characters on a sentence boundary.
- **Wording check:** some replies contain words the app must never use (crisis, emergency, suicide, die, death, danger and similar). The server regenerates once with a nudge (on heavy turns, with the extra-care note), but only if the first call finished within 10 seconds and the overall cap allows, so two calls still fit inside the app's 45 s read timeout. If a word still slips through, or there was no time for a rewrite, the server drops the sentences that contain it. If too little is left, a heavy turn gets a fallback reply and an ordinary turn a short neutral line, rather than an error. A first draft that brings up self-harm (suicide, ending your life, self-harm) on a turn the phrase list missed turns extra care on for that reply.
- **Security:** backend-only Groq key; verified Firebase auth; never log prompts, replies or provider bodies. Only HTTP status codes and configuration hints are logged.
- **Privacy:** review Groq's current privacy/retention policies and obtain appropriate disclosures and consent before real emotional support use.
- **Safety:** the extra-care phrases are *illustrative*; see [SAFETY.md](SAFETY.md). Introduce expert-reviewed detection, multi-turn handling, post-response safety validation, and testing before closed/public beta.
- **Provider failures:** report a short friendly error and never print response bodies from Groq: 429 when Groq rate-limits us, 502 for anything else. Heavy turns get a warm fallback reply instead of an error. UI has manual retry.
- **Later:** assess more expressive real-time speech and offline Gemma/LiteRT-LM only after V1 usage data.

References: https://console.groq.com/docs/api-reference and https://console.groq.com/docs/models
