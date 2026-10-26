# Closed-test launch TODO

Goal: a Play Store closed test that strangers can join from a public post.

- [x] 1. Better crisis detection (backend)
  - [x] Free Groq model (`openai/gpt-oss-20b`) checks the last 6 messages against a self-harm policy (Llama Guard was retired by Groq; `gpt-oss-safeguard-20b` is only 3 requests/min on the free tier)
  - [x] Wider phrase fallback (incl. common Hindi phrases) in case the classifier is unavailable
  - [x] System prompt asks Ari to check on safety gently when it notices distress
  - [x] Tests, plus a live spot check: 13/13 sample messages classified correctly
- [x] 2. "Test version" notice in the app (Home + top of every chat; text in `TEST_BUILD_NOTICE`, remove before public release)
  - "This is a test version of an AI companion. It's not a substitute for professional help. If you're in crisis, call 14416 or 112."
- [ ] 3. Put the backend online (Cloud Run free tier, ₹0–100 budget alert) — needs your Google Cloud account
- [ ] 4. Privacy policy page on let-it-go.in (Play requires a public URL)
- [x] 5. $25 Play developer account and closed test
- [ ] 6. Post the opt-in link (Reddit / Instagram / LinkedIn), aim for 20+ testers for 14 days
