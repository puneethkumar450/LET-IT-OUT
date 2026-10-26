# LET IT GO — versioned delivery plan

Legend: ✅ implemented in source (verification noted separately) · 🔧 incomplete or not fully verified · ⬜ not implemented. This is a developer plan, not a public-release checklist.

## v0.1.0 — Android developer MVP

- ✅ Native Compose/M3 screens: welcome, auth, home, mood, chat, settings, privacy/help
- ✅ Dynamic mood-colour selection and DataStore persistence
- ✅ Firebase Google and guest auth source integration (requires project credentials)
- ✅ Ktor + Groq API request path source integration, verification of Firebase ID tokens
- ✅ Simple user-editable STT transcripts + opt-in TTS playback
- ✅ Error/loading/retry/clear handling, limited prototype safety routing
- ✅ Basic privacy and support resources; no analytics or advertisements
- ✅ Single Ari companion with a shared generated portrait across static Android views; debug Simli talking-avatar pilot
- ✅ Local debug/release builds and Pixel 9 Pro emulator visual check of chat, Ari setup, and keyboard layout
- ✅ Development Firebase Android/Admin credentials configured; guest sign-in, guest upgrade, and an AI reply previously verified on an emulator
- ✅ Local backend health/readiness/auth rejection checks and HTTP route tests
- 🔧 Fresh Google sign-in, account collision handling, and complete auth flow on a physical device
- 🔧 Voice recognition and read-aloud behavior across devices
- 🔧 Broader safe/unsafe fictional conversation testing with Groq; phrase check plus model classifier passed a 13-message spot check, but is not a validated safety system
- 🔧 Improve user experience, onboarding retention, accessibility and dark mode
- ✅ Android repository HTTP contract tests and emulator tests for preference reset and Ari setup
- ✅ ViewModel emulator tests for stale-reply cancellation and retry/edit behavior
- 🔧 Broader chat/onboarding interaction and accessibility tests
- 🔧 Complete account lifecycle, reauthentication and deletion policy; local preferences are cleared after confirmed account deletion
- 🔧 Add production-grade safety evaluation with mental-health expert

## v0.2.0 — Wellness UX

- ⬜ Before/after optional mood rating; guided breathing and grounding
- ⬜ Hindi/Telugu UI and voice language coverage, tested per device
- ⬜ Better voice interruptions and user-controlled speech preferences
- ⬜ Accessible reduced-motion animation, contrast, responsive layouts

## v0.3.0 — Trust and personalization

- ⬜ User-controlled saved chat (Room, retention/deletion policies)
- ⬜ Journaling, local export/delete, opt-in personalisation
- ⬜ Abuse protections, distributed usage quotas, better monitoring without sensitive payloads

## v1.0.0 — Public release (blocked on professional safety/privacy review)

- ⬜ Clinically informed safety validation; privacy/terms/account deletion webpages
- ⬜ Production HTTPS hosting, app signing, Play Store listing, crash/ANR monitoring
- ⬜ Closed testing, accessibility and store compliance, gradual rollout

## v1.1.0 — Premium voice (evaluate cost first)

- ⬜ Test Gemini Live/OpenAI Realtime vs native STT/TTS; compare per-session economics
- ⬜ More natural interruption and turn-taking; always respect stop/permission controls

## v2.0.0 — Device AI (optional)

- ⬜ Explore Gemma/LiteRT-LM, hardware detection, storage/RAM/battery impact
- ⬜ Evaluate safety and multilingual support before enabling offline responses

## v2.1.0 — iOS and marketing

- ⬜ iOS app and App Store links
- ✅ LET IT GO landing page at `https://let-it-go.in/` over HTTPS
- ✅ Single-page website preview in `web/` (not a functional browser app)
- ⬜ Instagram/Facebook, YouTube/Google, region-appropriate social ad creative
- ⬜ Ad attribution without sensitive mood or chat data
- ⬜ Monetization only after considering well-being and data privacy impacts
