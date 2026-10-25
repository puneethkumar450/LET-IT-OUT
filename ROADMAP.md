# LET IT GO — versioned delivery plan

Legend: ✅ source scaffold exists · 🔧 unverified/in progress · ⬜ not implemented

## v0.1.0 — Android developer MVP

- ✅ Native Compose/M3 screens: welcome, auth, home, mood, chat, settings, privacy/help
- ✅ Dynamic mood-colour selection and DataStore persistence
- ✅ Firebase Google and guest auth source integration (requires project credentials)
- ✅ Ktor + Groq API request path source integration, verification of Firebase ID tokens
- ✅ Simple user-editable STT transcripts + opt-in TTS playback
- ✅ Error/loading/retry/clear handling, limited prototype safety routing
- ✅ Basic privacy and support resources; no analytics or advertisements
- 🔧 Build and test in a connected Android Studio environment; fix issues found
- 🔧 Configure Firebase Android and backend Admin SDK; verify sign-in and guest upgrade
- 🔧 Configure Groq and test safe/unsafe fictional conversations
- 🔧 Improve user experience, onboarding retention, accessibility and dark mode
- 🔧 Add ViewModel/repository UI unit tests and meaningful instrumentation tests
- 🔧 Implement real account lifecycle, reauthentication and complete deletion policy
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

- ⬜ iOS app, App Store links; LET IT GO custom domain
- ✅ Single-page website preview in `web/` (not an app launch or custom-domain deployment)
- ⬜ Instagram/Facebook, YouTube/Google, region-appropriate social ad creative
- ⬜ Ad attribution without sensitive mood or chat data
- ⬜ Monetization only after considering well-being and data privacy impacts
