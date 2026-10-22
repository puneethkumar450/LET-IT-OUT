# LET IT OUT — versioned delivery plan

Legend: ✅ source scaffold exists · 🔧 unverified/in progress · ⬜ not implemented

## v0.1.0 — Android developer MVP

- ✅ Native Compose/M3 screens: welcome, auth, home, mood, chat, settings, privacy/help (one file per screen in `ui/screens/`); signed-in people start at home
- ✅ Dynamic mood-colour selection and DataStore persistence
- ✅ Firebase Google and guest auth source integration (requires project credentials), with a fallback when a guest upgrades to an existing Google account and guest clean-up on sign-out
- ✅ Ktor + Groq API request path source integration, verification of Firebase ID tokens, friendly JSON errors for every failure
- ✅ Human companion persona on the server: a friend with a good therapist's ear, short replies, mirrors the person's language, honest about being an AI if sincerely asked
- ✅ Gentle support instead of crisis mode: extra-care prompt, `SUPPORT_SUGGESTED`, dismissible doctor / Tele-MANAS 14416 support card
- ✅ "How are you feeling now?" check when leaving a chat, on-device "felt lighter" stats and a greeting on home
- ✅ Opt-in daily check-in reminder (WorkManager, `POST_NOTIFICATIONS` on Android 13+)
- ✅ Simple user-editable STT transcripts + opt-in TTS playback
- ✅ Error/loading/retry/clear handling; late replies are dropped after a mood change or clear
- ✅ Basic privacy and support resources; no analytics or advertisements
- 🔧 Build and test in a connected Android Studio environment; fix issues found. **The 22 October changes have not been built yet:** run `:app:assembleDebug` and `:app:testDebugUnitTest`
- 🔧 Configure Firebase Android and backend Admin SDK; verify sign-in, guest upgrade (including the existing-account fallback) and guest sign-out on a device
- 🔧 Configure Groq and test everyday and heavy fictional conversations: tone, reply length, support card, no alarming words
- 🔧 Report a reply: app + `POST /v1/report` store flagged replies in a JSON-lines file; needs durable storage, retention and a review process
- 🔧 Verify daily check-in delivery on real devices (permission prompt, Doze, battery savers)
- 🔧 Improve user experience, onboarding retention, accessibility and dark mode
- 🔧 Unit tests exist for the ViewModel, error mapping, models, reminder timing/copy and the backend routes. The Android ones have only run in a JVM harness so far; add meaningful instrumentation tests
- 🔧 Implement real account lifecycle, reauthentication and complete deletion policy
- 🔧 Add production-grade safety evaluation with mental-health expert

## v0.2.0 — Wellness UX

- 🔧 Before/after optional mood rating: the after-conversation feeling check exists; an optional "before" rating does not
- ⬜ Guided breathing and grounding
- 🔧 Hindi/Telugu: the companion already mirrors the person's language in replies; UI text and voice language coverage still need work, tested per device
- ⬜ Better voice interruptions and user-controlled speech preferences
- ⬜ Accessible reduced-motion animation, contrast, responsive layouts

## v0.3.0 — Trust and personalization

- ⬜ User-controlled saved chat (Room, retention/deletion policies)
- ⬜ Journaling, local export/delete, opt-in personalisation
- 🔧 Abuse protections: per-user hourly limits and an overall per-minute cap exist, in one server's memory
- ⬜ Distributed usage quotas, better monitoring without sensitive payloads

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

- ⬜ iOS app, App Store links; single-page `letitout.co.in`
- ⬜ Instagram/Facebook, YouTube/Google, region-appropriate social ad creative
- ⬜ Ad attribution without sensitive mood or chat data
- ⬜ Monetization only after considering well-being and data privacy impacts
