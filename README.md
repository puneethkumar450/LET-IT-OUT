# LET IT OUT

**A mood-aware Android AI listening companion (v0.1.0 developer starter).**

> **Status: developer starter, not a release.** On 23 October 2026 the branch `claude/lavender-companion` brought the companion work from `claude/human-companion-fixes` onto the lavender redesign (`ui-changes`). It adds a warmer, more human companion voice, a gentle support note in place of the old hard-stop safety reply, ways to report a reply, and features that help people come back: a feeling check, on-device "felt lighter" stats, a greeting and an opt-in daily check-in. The backend tests pass. The Android unit tests have only run in a plain-JVM harness, the screens were only type-checked, and **the Android app has not been built on this branch**, because the build environment could not reach Google's Maven repository. Before anything else, run `./gradlew :app:assembleDebug :app:testDebugUnitTest` and fix whatever it reports (see [Verification status](#verification-status)). The lavender design on `ui-changes` did build, and on 19 October 2026 its guest sign-in and guest-to-Google linking worked on an emulator. Fresh Google sign-in, real AI replies, voice, notifications and real-device behavior still need testing. This is **not** ready for real users or public distribution.

**Android application ID:** `com.letitout.app`
**Display name:** `LET IT OUT`
**Repository:** https://github.com/puneethkumar450/LET-IT-OUT.git (branches and merge notes: [docs/GIT_SETUP.md](docs/GIT_SETUP.md))

## What has been implemented in source

- Native Jetpack Compose splash, three-page onboarding, Google/guest/preview sign-in, seven-mood home screen with expressive emoji stickers, mood check-in with an optional 1–10 intensity rating, AI chat view, settings and Privacy & Help screen, with Android Back navigation. People who are signed in, or have finished onboarding, go from the splash straight to home.
- Lavender Material 3 theme (Fredoka headings, Nunito body via Google Fonts), custom line icons, and per-mood colour themes that can be switched off in Settings (**Always calm**). Mood, speech, theme, check-in and reminder settings persist through DataStore.
- Firebase Credential Manager Google sign-in and Firebase anonymous (guest) sign-in, connected to the development Firebase project `let-it-out-d661a` through the ignored `app/google-services.json`. A guest can link Google from Settings (**Link** on the account card) without losing anything on the phone; if that Google account already exists, the app switches to it. Closing the account picker is not treated as an error. Signing out as a guest removes the anonymous account (best effort).
- **A human companion voice.** The server-side persona (`backend/src/main/kotlin/com/letitout/server/Persona.kt`) talks like a caring close friend with a good therapist's ear. It reflects the feeling underneath, asks at most one natural question, keeps replies short with no lists or markdown, and mirrors English, Hindi, Hinglish or Telugu. It suggests seeing a doctor only when a struggle sounds persistent or serious. If someone sincerely asks whether they are talking to a real person, it answers honestly that it is an AI companion. The optional intensity rating reaches the model only as a hint for tone.
- **Gentle support, with no alarming wording.** There is no canned hard-stop reply, and neither the app copy nor the replies use alarming words. When recent messages sound very heavy, the companion gets extra-care guidance: it stays warm, says talking to a doctor is the best option and mentions Tele-MANAS **14416**. The backend then returns `safetyState: "SUPPORT_SUGGESTED"`. In chat that reply carries a soft note ("Talking to a doctor really is the best next step. If you'd like to talk to someone today, Tele-MANAS is free and open 24x7.", left out when the reply already mentions a doctor) and one **Tele-MANAS · 14416** row whose **Call** button opens the dialer. The note shows at most once every 4 companion replies. For the rest of that conversation the chat uses the calm lavender colours and shows a small **Talk to someone** chip that opens Privacy & Help, the only screen that mentions 112. Details and limits are in [docs/SAFETY.md](docs/SAFETY.md).
- **Coming back.** After at least two companion replies, leaving a chat (back arrow, system Back, the mood chip, **End conversation**) or starting a **New conversation** asks "How are you feeling now?" (Lighter / About the same / Heavier / Skip). Answers stay on the phone. Home then shows how many recent talks left the person feeling lighter, worded gently and never as a score, and greets signed-in people by time of day (with their first name when Google provides one). Settings → **Daily check-in** has an opt-in "Gentle daily note" at 9 AM, 2 PM, 8 PM or 10 PM, scheduled with WorkManager. Turning it on asks for notification permission on Android 13+. There are no streaks and no guilt.
- **Report a reply.** **Report** under each companion reply offers four reasons (Hurtful or offensive, Advice that could cause harm, Wrong or made up, Something else) and sends only that reply and the reason to the backend's `POST /v1/report`.
- Ktor backend exposing `/health`, authenticated `/v1/chat` and authenticated `/v1/report`. It verifies Firebase ID tokens, validates and trims input, limits per-user and overall traffic, forwards recent context to Groq, tidies the reply, and returns a short friendly JSON error for every failure.
- Text conversation states (typing, friendly errors, retry without repeating the message, edit after a failed send, offline notice, new/end conversation). If the person changes mood or starts over while a reply is on its way, the late reply is dropped. Also native Android push-to-talk transcript capture, and read-aloud replies with a Stop control.
- The splash, onboarding, the chat's "Today · AI companion · Nothing here is saved" line and Privacy & Help all say it is an AI companion.
- Data kept on the phone is excluded from cloud backup and device transfer, and the app hides its Recents (app switcher) preview on Android 13+.
- No AdMob, analytics trackers, WebRTC, background microphone, or large downloadable AI models.

## 1. Open Android project

Open this **root folder** in Android Studio. Use JDK 17 for the Android build; the backend needs JDK 21. Android Studio's Gradle download and dependency sync require internet.

Versions are deliberately conservative for interoperability: AGP `8.11.1`, Kotlin `2.2.21`, Gradle `8.13`, Compose BOM `2025.06.01`. Upgrade only after a successful local build, not all at once.

```bash
cd LET-IT-OUT
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

The Gradle 8.13 wrapper is included. Do not commit `local.properties` or API keys.

For an emulator, create `local.properties`:

```properties
sdk.dir=/Users/YOUR_USER/Library/Android/sdk
apiBaseUrl=http://10.0.2.2:8080/
```

The app reads the Web OAuth client ID from Firebase's generated `default_web_client_id` after you add `app/google-services.json`. If necessary, `webClientId=YOUR_FIREBASE_WEB_OAUTH_CLIENT_ID.apps.googleusercontent.com` can be set in `local.properties` as a fallback. It is a public identifier, not a secret. To test on a physical Android device, use an HTTPS URL reachable by that device instead of `10.0.2.2`. Release builds intentionally disallow plain HTTP; cleartext is debug-only.

### Build variants and signing

`debug` uses Android's local debug keystore. `release` uses `app/let-it-out-release.keystore` when these values are present in the root `local.properties`:

```properties
releaseStoreFile=app/let-it-out-release.keystore
releaseStorePassword=YOUR_STORE_PASSWORD
releaseKeyAlias=puneeth
releaseKeyPassword=YOUR_KEY_PASSWORD
```

Build the signed APK with `./gradlew :app:assembleRelease` and check both variants with `./gradlew :app:signingReport`. The keystore and `local.properties` are ignored by Git. Keep a secure backup of both: an APK signed with this key requires the same signing identity for future updates. The current release build still needs a real HTTPS chat server before it can serve AI replies.

## 2. Configure Google and guest sign-in

The development Firebase project is configured as recorded in [docs/FIREBASE_SETUP.md](docs/FIREBASE_SETUP.md). If the ignored `app/google-services.json` is missing from a fresh checkout, download it from the Firebase console. **Explore in preview** is available only when Firebase is absent. Without signing in you can look around and draft in chat, but sending is disabled. Authentication is required for `/v1/chat` and `/v1/report` to avoid open, unbounded API spending; the separate backend and AI provider still need their own setup.

## 3. Start backend

In a terminal with JDK 21 and Gradle 8.13 available:

```bash
cd backend
export GOOGLE_APPLICATION_CREDENTIALS="/absolute/path/service-account.json"
export FIREBASE_PROJECT_ID="YOUR_PROJECT_ID"
export GROQ_API_KEY="YOUR_GROQ_KEY"
export GROQ_MODEL="openai/gpt-oss-120b"
../gradlew run
```

Optional settings (defaults in brackets): `GROQ_MAX_COMPLETION_TOKENS` [2048], `GROQ_REASONING_EFFORT` = `low` | `medium` | `high` [medium], `GLOBAL_REQUESTS_PER_MINUTE` [60], `REPORTS_FILE` [reports.jsonl], `REPORTS_MAX_BYTES` [52428800, i.e. 50 MB], `PORT` [8080]. See `backend/.env.example` and [docs/AI_PROVIDER.md](docs/AI_PROVIDER.md). Git ignores `reports*.jsonl`.

Check `http://localhost:8080/health`. The `/v1/chat` endpoint returns 503 when Firebase admin or Groq is unconfigured; `/v1/report` needs only Firebase admin. Never place `GROQ_API_KEY` in the Android application.

Run the backend tests with `cd backend && ../gradlew test`.

⚠️ **Free-tier warning:** Groq account rate limits and model availability can change; do not assume free API access is unlimited. Verify pricing and privacy terms before collecting real emotional conversations.

## 4. Backend request format

Authorization header: `Bearer <Firebase ID token>`

```json
{
  "mood": "ANGRY",
  "messages": [{"role":"user","content":"I had a bad day"}],
  "intensity": 7
}
```

- `mood` is one of `ANGRY`, `SAD`, `STRESSED`, `ANXIOUS`, `LONELY`, `HAPPY`, `TALK`.
- `messages` has 1 to 12 entries. Each `role` is `user` or `assistant`, each `content` is 1 to 2,000 characters, and the last message must be from the user. The whole body may be at most 64,000 characters.
- `intensity` is optional: the person's own 1–10 rating from the mood check-in. The server adds it to the prompt as a hint for tone ("Self-rated intensity: 7/10. Treat it as a hint, not a measurement."). Values outside 1–10, or `null`, are ignored rather than rejected, and the rating never turns on extra care.
- The app sends the mood's opening question as the first `assistant` message, then the conversation, keeping the last 12. **Just want to talk** on home and **Skip the rating** send no intensity.

Response:

```json
{"reply":"Ugh, one of those days. What happened?","safetyState":"NORMAL"}
```

`safetyState` is `NORMAL` or `SUPPORT_SUGGESTED`. `SUPPORT_SUGGESTED` means the app should show the gentle support note. Every error returns `{"error": "<short friendly sentence>"}` with status 400 (invalid request), 401 (sign in again), 413 (too long), 429 (hourly limit, or too busy right now), 502 (no reply from the AI) or 503 (chat unavailable). The app shows its own wording for most statuses and the server's sentence for 429 and 503.

Reporting a reply uses the same header:

```json
POST /v1/report
{"reason": "hurtful", "replyText": "The reply being reported"}
```

`reason` is `hurtful`, `unsafe`, `wrong` or `other`; `replyText` is 1 to 2,000 characters after trimming; the body may be at most 8,000 characters. A stored report answers 204. Errors use the same JSON shape: 400, 401, 413, 429 (report limit) or 503 (Firebase admin unconfigured, or the report could not be stored).

## 5. Existing limitations

- **The Android app was not built on this branch.** These files have not been compiled anywhere: `auth/AuthManager.kt`, `data/ChatApi.kt`, `data/Preferences.kt`, `data/AppModule.kt`, `reminder/ReminderNotifications.kt`, `reminder/CheckInReminderWorker.kt`, `reminder/WorkManagerReminderScheduler.kt`, `reminder/ReminderModule.kt`, `AndroidManifest.xml`, `app/build.gradle.kts`, `res/drawable/ic_notification.xml` and `res/xml/data_extraction_rules.xml`. The screens were only type-checked, and the ViewModel, models and reminder timing were tested only in a plain-JVM harness (see [Verification status](#verification-status)).
- Sign-in on this branch, AI replies, voice recognition, notifications and physical-device behavior remain unverified. Doze and battery savers can delay the daily check-in. A check-in that would arrive more than 2 hours late is skipped.
- **Reports go to a local file.** `POST /v1/report` (signed-in users, 20 per hour each and 300 per hour overall) appends the flagged reply, the reason, a timestamp and a one-way hash of the reporter to `REPORTS_FILE` as JSON lines, and stops accepting reports once that file reaches `REPORTS_MAX_BYTES`. Nothing else from the conversation is sent. Before a beta, move this to durable, access-controlled storage, decide how long reports are kept, and set up someone to review them.
- Account deletion may require recent Firebase reauthentication; the app then asks the person to sign out, sign in again and retry. For a production service, implement complete user-data erasure wherever data is retained.
- Guest-to-Google account upgrade uses Firebase credential linking, with a fallback to signing in when that Google account already exists. Plain linking worked on an emulator on `ui-changes`; the fallback and this branch's version of `AuthManager.kt` still need testing on a device.
- Local chat remains only in ViewModel memory; changing mood intentionally clears it. Leaving the process also clears it. Not end-to-end encrypted. Feeling check-ins and reminder settings are stored in DataStore on the phone. Sign-out and account deletion clear the check-ins and turn off the reminder; **Clear check-in history** clears them any time, and **Delete conversation data on this device** clears the conversation, the saved mood and the check-ins.
- Extra-care detection uses a prototype phrase list (English, Hinglish, and a little Hindi and Telugu) plus the AI model's own judgement. It can miss heavy moments or trigger on ordinary ones. Do not release publicly until it is replaced or reviewed by appropriate experts.
- Built-in Android TTS voice quality and STT languages vary by device. STT might send audio to the device's selected speech-service provider. The app's own UI text is English only.
- No ad SDK is integrated. Acquisition planning is in `docs/MARKETING_PLAN.md`.
- Website and iOS app are future milestones.
- The sample Ktor backend is not production-hardened: rate limits live in one server's memory and reset on restart, there is no distributed usage enforcement, and safety routing has not been clinically reviewed.

## Verification status

What was checked on 23 October 2026, in an environment without the Android SDK:

- **Backend:** `cd backend && ../gradlew test` ran and passed 79 tests (care phrases, persona text including the intensity hint, Groq parsing and reply polishing, rate limiter, and `/v1/chat` and `/v1/report` route tests with a fake token verifier, fake AI provider and in-memory report store).
- **Android unit tests** (`app/src/test`, 64 tests: ViewModel, error mapping, models, reminder timing and copy) passed **only in a throwaway plain-JVM harness**. That harness compiled `data/Models.kt`, `data/Gateways.kt`, `data/ChatErrors.kt`, `ui/AppViewModel.kt`, `reminder/ReminderScheduler.kt`, `reminder/ReminderTiming.kt` and `reminder/ReminderCopy.kt` with small stand-ins for the Android, lifecycle and Hilt classes they reference. They have not been run through the Android Gradle plugin.
- **UI:** `MainActivity.kt` and the `ui/` screens were only type-checked against desktop Compose (JetBrains Compose 1.8.2) with Android APIs stubbed. The stand-ins were checked by compiling the unchanged `ui-changes` sources with them too. This catches Compose API mistakes, not real Android behavior.
- **Not done:** an Android build, an emulator or device run, and a real Groq conversation.

Owner checklist, in order: `./gradlew :app:assembleDebug :app:testDebugUnitTest` → sign-in (Google, guest, guest to Google from Settings → **Link**, a Google account that already exists, closing the picker, sign-out, delete) → a real chat against the backend, with and without an intensity rating → a heavy fictional conversation: the gentle note, **Call** on the Tele-MANAS row, **Talk to someone** → the feeling check when leaving a chat → **Report** on a reply → the daily check-in on Android 13+ (permission prompt, notification, tap opens the app) → `./gradlew :app:assembleRelease`.

## 6. Next tasks

See [ROADMAP.md](ROADMAP.md). Priority sequence: Android build and unit-test verification → auth smoke test → backend smoke test with real Groq (tone, reply length, support note) → device test of the support note, feeling check, report and daily check-in → a review process for reported replies → expert-reviewed safety/privacy → limited closed beta. **Do not launch publicly from this starter.**
