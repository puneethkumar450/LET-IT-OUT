# LET IT OUT

**A mood-aware Android AI listening companion (v0.1.0 developer starter).**

> **Status: developer starter, not a release.** On 22 October 2026 this branch gave the companion a warmer, more human voice, replaced the old hard-stop safety reply with a gentle support card, and added features that help people come back (a feeling check, on-device "felt lighter" stats and an opt-in daily check-in). The backend tests pass. The Android unit tests have only been run in a plain-JVM harness, and **the Android app has not been built since these changes**, because the build environment could not reach Google's Maven repository. Before anything else, run `./gradlew :app:assembleDebug` and `./gradlew :app:testDebugUnitTest` and fix whatever they report (see [Verification status](#verification-status)). The earlier redesign did build and run on an emulator on 14 October 2026. Google Sign-In, real AI replies, voice, notifications and real-device behavior still need testing. This is **not** ready for real users or public distribution.

**Android application ID:** `com.letitout.app`
**Display name:** `LET IT OUT`
**Repository:** https://github.com/puneethkumar450/LET-IT-OUT.git (branches and merge notes: [docs/GIT_SETUP.md](docs/GIT_SETUP.md))

## What has been implemented in source

- Native Jetpack Compose welcome screen, Google/guest/preview entry, seven-mood home screen, AI chat view, settings and privacy & support screen, with Android Back navigation. People who are already signed in open straight to home. `MainActivity.kt` holds only the theme and back stack; each screen has its own file in `ui/screens/`, and shared pieces are in `ui/components/Common.kt`.
- Consistent warm Material 3 theme, custom line icons for moods, and muted mood colours, with selection persisted through DataStore.
- Firebase Credential Manager Google sign-in and Firebase anonymous (guest) sign-in integration points (requires your own Firebase project). If a guest upgrades to a Google account that already exists, the app switches to that account. Closing the account picker is not treated as an error. Signing out as a guest removes the anonymous account (best effort).
- **A human companion voice.** The server-side persona (`backend/src/main/kotlin/com/letitout/server/Persona.kt`) talks like a caring close friend with a good therapist's ear. It reflects the feeling underneath, asks at most one natural question, keeps replies short with no lists or markdown, and mirrors English, Hindi, Hinglish or Telugu. It suggests seeing a doctor only when a struggle sounds persistent or serious. If someone sincerely asks whether they are talking to a real person, it answers honestly that it is an AI companion.
- **Gentle support instead of a crisis mode.** There is no canned hard-stop reply and no alarming wording in app copy or replies. When recent messages sound very heavy, the companion gets extra-care guidance. It stays warm, says talking to a doctor is the best option and mentions Tele-MANAS **14416**. The backend returns `safetyState: "SUPPORT_SUGGESTED"` and the chat shows a dismissible support card (**Call 14416** / **Not now**). Details and limits are in [docs/SAFETY.md](docs/SAFETY.md).
- **Coming back.** Leaving a chat after at least two companion replies asks "How are you feeling now?" (Lighter / About the same / Heavier / Skip). Answers stay on the phone. Home then shows how many recent talks left the person feeling lighter, worded gently and never as a score, and greets signed-in people by time of day. Settings has an opt-in daily check-in notification at 9 AM, 2 PM, 8 PM or 10 PM, scheduled with WorkManager. Turning it on asks for notification permission on Android 13+. There are no streaks and no guilt.
- Ktor backend exposing `/health` and authenticated `/v1/chat`. It verifies Firebase ID tokens, validates and trims input, limits per-user and overall traffic, forwards recent context to Groq, tidies the reply, and returns a short friendly JSON error for every failure.
- Text conversation states (loading, friendly errors, retry without repeating the message, clear). If the person changes mood or clears the chat while a reply is on its way, the late reply is dropped. Also native Android push-to-talk transcript capture, optional text-to-speech playback, and a **Report** option under each reply (sent to the backend's `POST /v1/report`).
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
webClientId=YOUR_FIREBASE_WEB_OAUTH_CLIENT_ID.apps.googleusercontent.com
```

`webClientId` is a public OAuth identifier (not a secret). To test on a physical Android device, use an HTTPS URL reachable by that device instead of `10.0.2.2`. Release builds intentionally disallow plain HTTP; cleartext is debug-only.

## 2. Configure Google and guest sign-in

Follow [docs/FIREBASE_SETUP.md](docs/FIREBASE_SETUP.md). Until configured, **Explore in preview** is available. You can view and draft in chat, but sending is disabled. Authentication is required for `/v1/chat` to avoid open, unbounded API spending.

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

Optional settings (defaults in brackets): `GROQ_MAX_COMPLETION_TOKENS` [2048], `GROQ_REASONING_EFFORT` = `low` | `medium` | `high` [medium], `GLOBAL_REQUESTS_PER_MINUTE` [60], `REPORTS_FILE` [reports.jsonl], `PORT` [8080]. See `backend/.env.example` and [docs/AI_PROVIDER.md](docs/AI_PROVIDER.md).

Check `http://localhost:8080/health`. The `/v1/chat` endpoint returns 503 when Firebase admin or Groq is unconfigured. Never place `GROQ_API_KEY` in the Android application.

Run the backend tests with `cd backend && ../gradlew test`.

⚠️ **Free-tier warning:** Groq account rate limits and model availability can change; do not assume free API access is unlimited. Verify pricing and privacy terms before collecting real emotional conversations.

## 4. Backend request format

Authorization header: `Bearer <Firebase ID token>`

```json
{
  "mood": "ANGRY",
  "messages": [{"role":"user","content":"I had a bad day"}]
}
```

- `mood` is one of `ANGRY`, `SAD`, `STRESSED`, `ANXIOUS`, `LONELY`, `HAPPY`, `TALK`.
- `messages` has 1 to 12 entries. Each `role` is `user` or `assistant`, each `content` is 1 to 2,000 characters, and the last message must be from the user. The whole body may be at most 64,000 characters.
- The app sends the mood's opening question as the first `assistant` message, then the conversation, keeping the last 12.

Response:

```json
{"reply":"Ugh, one of those days. What happened?","safetyState":"NORMAL"}
```

`safetyState` is `NORMAL` or `SUPPORT_SUGGESTED`. `SUPPORT_SUGGESTED` means the app should show the gentle support card. Every error returns `{"error": "<short friendly sentence>"}` with status 400 (invalid request), 401 (sign in again), 413 (too long), 429 (hourly limit, or too busy right now), 502 (no reply from the AI) or 503 (chat unavailable). The app shows its own wording for most statuses and the server's sentence for 429 and 503.

## 5. Existing limitations

- **The Android app was not built after the 22 October changes.** These files have not been compiled anywhere: `auth/AuthManager.kt`, `data/ChatApi.kt`, `data/Preferences.kt`, `data/AppModule.kt`, `reminder/ReminderNotifications.kt`, `reminder/CheckInReminderWorker.kt`, `reminder/WorkManagerReminderScheduler.kt`, `reminder/ReminderModule.kt`, `AndroidManifest.xml`, `app/build.gradle.kts`, `res/drawable/ic_notification.xml` and `res/xml/data_extraction_rules.xml`.
- Real sign-in, AI replies, voice recognition, notifications and physical-device behavior remain unverified. Doze and battery savers can delay the daily check-in. A check-in that would arrive more than 2 hours late is skipped.
- **Reports go to a local file.** `POST /v1/report` (signed-in users, 20 per hour) appends the flagged reply, the reason, a timestamp and a one-way hash of the reporter to `REPORTS_FILE` as JSON lines. Nothing else from the conversation is sent. Before a beta, move this to durable, access-controlled storage, decide how long reports are kept, and set up someone to review them.
- Account deletion may require recent Firebase reauthentication; the app then asks the person to sign out, sign in again and retry. For a production service, implement complete user-data erasure wherever data is retained.
- Guest-to-Google account upgrade uses Firebase credential linking, with a fallback to signing in when that Google account already exists. Both paths need testing on a device.
- Local chat remains only in ViewModel memory; changing mood intentionally clears it. Leaving the process also clears it. Not end-to-end encrypted. Feeling check-ins and reminder settings are stored in DataStore on the phone and cleared on sign-out or account deletion.
- Extra-care detection uses a prototype phrase list (English, Hinglish, and a little Hindi and Telugu) plus the AI model's own judgement. It can miss heavy moments or trigger on ordinary ones. Do not release publicly until it is replaced or reviewed by appropriate experts.
- Built-in Android TTS voice quality and STT languages vary by device. STT might send audio to the device's selected speech-service provider. The app's own UI text is English only.
- No ad SDK is integrated. Acquisition planning is in `docs/MARKETING_PLAN.md`.
- Website and iOS app are future milestones.
- The sample Ktor backend is not production-hardened: rate limits live in one server's memory and reset on restart, there is no distributed usage enforcement, and safety routing has not been clinically reviewed.

## Verification status

What was checked on 22 October 2026, in an environment without the Android SDK:

- **Backend:** `cd backend && ../gradlew test` ran and passed 61 tests (care phrases, persona text, Groq parsing and reply polishing, rate limiter, and `/v1/chat` and `/v1/report` route tests with a fake token verifier, fake AI provider and in-memory report store).
- **Android unit tests** (`app/src/test`, 48 tests: ViewModel, error mapping, models, reminder timing and copy) passed **only in a throwaway plain-JVM harness**. That harness compiled `data/Models.kt`, `data/Gateways.kt`, `data/ChatErrors.kt`, `ui/AppViewModel.kt`, `reminder/ReminderScheduler.kt`, `reminder/ReminderTiming.kt` and `reminder/ReminderCopy.kt` with small stand-ins for the Android and Hilt classes they reference. They have not been run through the Android Gradle plugin.
- **UI screens** were only type-checked against desktop Compose with Android APIs stubbed. That catches Compose API mistakes, not real Android behavior.
- **Not done:** an Android build, an emulator or device run, and a real Groq conversation.

Owner checklist, in order: `./gradlew :app:assembleDebug` → `./gradlew :app:testDebugUnitTest` → sign-in (Google, guest, guest to Google, sign-out, delete) → a real chat against the backend → support card and **Call 14416** dialer → the feeling check when leaving a chat → the daily check-in on Android 13+ (permission prompt, notification, tap opens the app).

## 6. Next tasks

See [ROADMAP.md](ROADMAP.md). Priority sequence: Android build and unit-test verification → auth smoke test → backend smoke test with real Groq (tone, reply length, support card) → device test of the support card, feeling check and daily check-in → a review process for reported replies → expert-reviewed safety/privacy → limited closed beta. **Do not launch publicly from this starter.**
