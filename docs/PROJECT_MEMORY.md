# LET IT GO project memory

Updated 26 October 2026. This is a navigation aid for future work; check the current source and Git status before changing code.

## Product and flow

- LET IT GO is an Android AI listening companion for adults 18+. The companion is Ari. It is a test build, not a therapist or crisis service.
- First run: splash → four-page onboarding → full-screen Ari introduction → Google or guest sign-in → Ari welcome screen. Later launches return to the Ari welcome screen.
- The welcome screen uses `app/src/main/res/drawable-nodpi/ari_onboarding_fullscreen.png`, the second generated Ari portrait. **Start talking** opens chat; signed-in debug builds request live Simli video. **Read chat summary** appears when this process has messages. Back from live video shows the text summary; Back from the summary returns home.
- Ending a chat that has messages runs the release ritual: a confirmation, a dissolve animation over an in-memory copy of the last bubbles (the ViewModel is cleared first), then the `release` end screen. A conversation with a crisis-support reply ends on helplines with no share card. The optional share card is a fixed, content-free image written to the cache `share/` folder and shared through a `FileProvider`. See `docs/RELEASE_RITUAL_PLAN.md`.
- Free trial (`docs/FREE_TRIAL_LIMITS.md`): 3 chats a day of up to 15 minutes, and a 3-minute live preview once a week. The server keeps the counts per chat `sessionId`; the app shows a minutes-left pill, a 2-minute note, Ari's goodbye and a time-up card. Crisis chats have no timer and don't count. Live no longer starts automatically on Start talking; it starts from **See Ari live** and an intro sheet. Not yet checked on a phone.
- The old mood grid, age/gender picker, and mood check-in are no longer in the navigation flow. `Mood` and related DataStore fields remain in source for backend compatibility and older data; avoid presenting them as current choices.
- Static Ari imagery across Android comes from the same generated portrait. The actual moving face is streamed by Simli and can differ slightly. Do not imply that changing the local PNG changes the hosted live face.
- `TEST_BUILD_NOTICE` in `app/src/main/java/com/letitout/app/data/Models.kt` appears on Home and in chat; keep it visible in test builds.

## Runtime and data

- `MainActivity.kt` owns Compose navigation and activity-scoped warm resources. `AppViewModel.kt` owns `StateFlow` and the in-memory conversation. `ChatApi.kt` uses Retrofit and a Firebase ID token for authenticated chat and avatar-session calls.
- On the welcome screen, lifecycle-aware coroutines warm Firebase auth and `/health`, initialize a shared Android `TextToSpeech`, and preload the bundled avatar WebView/JavaScript. This does **not** mint a Simli token or open a billable live stream. A new live session starts only when the user chooses to talk. The preloaded WebView is reused until the Activity is destroyed.
- `ChatScreen.kt` retains the text summary and handles voice input, Android TTS, retries, and live mode. `LiveAvatarView.kt` owns WebView/WebRTC; `avatar-bridge/src/bridge.js` is its source, bundled to `app/src/main/assets/live-avatar/bridge.js` with `npm run build` inside `avatar-bridge/`.
- The local conversation lives only in the ViewModel; process death clears it. DataStore persists settings and legacy mood data. Typed messages and speech transcripts reach the Ktor backend/Groq; spoken AI reply audio reaches Simli while the live view is open for lip sync. The backend must not return provider API keys to the app.

## Repositories and service

- The Android/backend repository root is this directory, currently on `feature/live-avatar-pilot`. `web/` is a separate nested Git repository for the marketing site and Ari preview. Do not assume a root commit includes `web/`.
- The Ktor service in `backend/` exposes `/health`, `/ready`, authenticated `/v1/chat`, authenticated `/v1/avatar/session`, and a restricted website hello endpoint. Local setup is in `backend/.env.example` and `docs/LIVE_AVATAR_PILOT.md`. Actual keys, Firebase files, keystores, and `local.properties` are ignored; never add their contents to memory or Git.
- Production chat defaults to disabled. The debug Android live-avatar pilot is enabled by default; release builds disable it. See `docs/PRODUCTION_BACKEND.md`, `docs/SAFETY.md`, and `TODO.md` before any public rollout.
- The website Ari preview is separate from Android chat. Its hello is a recorded MP4 (`web/dist/characters/ari-hello.mp4`), with no live visitor session, speech input, or AI chat. `web/` remains a separate Git repository.

- The first public release ships text chat with voice; the live avatar stays a debug pilot. See `docs/FIRST_RELEASE_CHAT.md`.
- Device check on a moto g45 (Android 15), 29 October 2026: live avatar connects and streams, captions and **Show chat** work, the chat follows the keyboard, "Speaking…" clears after speech, and the release ritual (reduced and full animation, share sheet, crisis end screen, empty chat) works. The avatar's live face differs visibly from the Ari still. Not yet checked: the live-mode **Let it go** chip, lip sync with sound on, TalkBack, a low-end phone.

## Known release blockers

- `TODO.md` tracks the closed test. The backend is not yet deployed to Cloud Run; the Google Cloud CLI/account setup is pending. The privacy policy is published at `https://let-it-go.in/privacy/` with `support@let-it-go.in`. Android Settings and Privacy & Help now link to it, including the pre-sign-in Privacy & Help route. Play Console entries remain unverified; see `docs/DATA_SAFETY_REVIEW.md` for the source-based audit and policy discrepancies.
- The phrase plus model safety check is a prototype. Clinical review, multilingual crisis evaluation, privacy review, and physical-device voice/live-video checks remain open. Production chat is disabled by default.

## Verification and owner preferences

- Android check: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin`. Backend check: `./gradlew -p backend test`. Run focused connected tests when an emulator is available.
- The Android build and unit tests passed locally on 26 October 2026. No emulator was attached during this memory update, so the latest welcome-screen and WebView reuse changes still need a device visual and live-session check. Do not claim measured startup improvement until timed on a device.
- The website hello video is committed in the nested `web/` repository, and the Android/backend root replaced the unused website live-session route. Check both Git repositories independently before release.
- The owner prefers minimal screen sharing because it slows their computer; use code inspection and static ADB captures when needed. They previously asked to keep the local backend running during development, but verify `/health` rather than assuming a prior shell session survived.
- Live Simli sessions can consume account minutes. Preload only local/auth resources before Start talking; keep the live connection tied to visible, user-started chat.
