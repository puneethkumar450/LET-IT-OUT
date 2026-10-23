# LET IT OUT — versioned delivery plan

Legend: ✅ source scaffold exists · 🔧 unverified/in progress · ⬜ not implemented

## v0.1.0 — Android developer MVP

- ✅ Native Compose/M3 screens: splash, three-page onboarding, sign-in, home with mood stickers, mood check-in with an optional intensity rating, chat, settings, Privacy & Help; signed-in or onboarded people start at home
- ✅ Lavender theme, per-mood colour themes (can be switched off) and DataStore persistence
- ✅ Firebase Google and guest auth connected to the development project, with a fallback when a guest links a Google account that already exists and guest clean-up on sign-out
- ✅ Local release signing from ignored `local.properties` values
- ✅ Ktor + Groq API request path source integration, verification of Firebase ID tokens, friendly JSON errors for every failure
- ✅ Human companion persona on the server: a friend with a good therapist's ear, short replies, mirrors the person's language, honest about being an AI if sincerely asked; the self-rated intensity is passed on as a hint only
- ✅ Gentle support with no hard stop: extra-care prompt, `SUPPORT_SUGGESTED`, a doctor note and one Tele-MANAS 14416 row in chat (at most once every 4 replies), a "Talk to someone" chip; 112 only on Privacy & Help
- ✅ "How are you feeling now?" check when leaving a chat, on-device "felt lighter" stats and a greeting on home
- ✅ Opt-in daily check-in reminder (WorkManager, `POST_NOTIFICATIONS` on Android 13+)
- ✅ Simple user-editable STT transcripts + opt-in read-aloud with a Stop control
- ✅ Error/loading/retry/edit/clear handling; late replies are dropped after a mood change or a new conversation
- ✅ Basic privacy and support resources; no analytics or advertisements
- 🔧 Build and test in a connected Android Studio environment; fix issues found. **`claude/lavender-companion` has not been built yet:** run `./gradlew :app:assembleDebug :app:testDebugUnitTest`
- 🔧 Firebase: guest sign-in and guest-to-Google linking worked on an emulator on `ui-changes` (19 October 2026). Verify fresh Google sign-in, the existing-account fallback, guest sign-out and deletion on this branch and on a device; register release and Google Play signing fingerprints
- 🔧 Configure Groq and test everyday and heavy fictional conversations: tone, reply length, support note, no alarming words, with and without an intensity rating
- 🔧 Report a reply: app + `POST /v1/report` store flagged replies in a JSON-lines file; needs durable storage, retention and a review process
- 🔧 Verify daily check-in delivery on real devices (permission prompt, Doze, battery savers)
- 🔧 Improve user experience, onboarding retention, accessibility and dark mode
- 🔧 Unit tests exist for the ViewModel, error mapping, models, reminder timing/copy and the backend routes. The Android ones have only run in a JVM harness so far; add meaningful instrumentation tests
- 🔧 Implement real account lifecycle, reauthentication and complete deletion policy
- 🔧 Add production-grade safety evaluation with mental-health expert

## v0.2.0 — Wellness UX

- 🔧 Before/after mood rating: an optional intensity rating before a chat and a feeling check after it exist; nothing compares them yet
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
- ⬜ Production HTTPS hosting, Play App Signing, Play Store listing, crash/ANR monitoring (a local release keystore exists)
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
