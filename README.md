# LET IT GO

**A mood-aware Android AI listening companion (v0.1.0 developer starter).**

> **Status: developer starter, not a public release.** Android debug/release builds and backend tests pass locally. Guest sign-in and real AI chat replies have worked on an emulator. Voice recognition, physical-device behavior, production infrastructure, and independent safety review still need verification before real users are invited.

**Android application ID:** `com.letitout.app`
**Display name:** `LET IT GO`
**Intended repository:** https://github.com/puneethkumar450/LET-IT-OUT.git

The app display name and website brand are LET IT GO. The existing Android application ID, Firebase project, backend artifact names, and Git repository URL retain their original identifiers so existing integrations remain connected.

## What has been implemented in source

- Native Jetpack Compose splash with companion portrait, four-page onboarding, adult companion portrait picker, Google/guest/preview sign-in, seven-mood home screen with expressive emoji stickers, mood check-in with an optional intensity rating, AI chat view, settings and Privacy & Help screen, with Android Back navigation.
- Lavender Material 3 theme (Fredoka headings, Nunito body via Google Fonts), custom line icons, and per-mood colour themes that can be switched off in Settings. Mood, companion portrait, speech and theme settings persist through DataStore.
- Firebase Credential Manager Google sign-in and Firebase anonymous sign-in, connected to the development Firebase project `let-it-out-d661a` through the ignored `app/google-services.json`.
- Ktor backend exposing `/health`, `/ready`, and authenticated `/v1/chat`, verifying Firebase ID tokens, forwarding trimmed conversation context to Groq, and limiting input and traffic. Production mode uses Firestore shared counters and starts with chat disabled.
- Text conversation states (typing, retry or edit after a failed send, offline notice, new/end conversation), a crisis-support card with Tele-MANAS and 112 dial buttons when the backend flags a reply, native Android push-to-talk transcript capture, and read-aloud replies with a Stop control.
- An intentionally limited **prototype-only** crisis keyword guard plus configurable support resources. **Not a clinically validated or sufficient production safety system.**
- No AdMob, analytics trackers, WebRTC, background microphone, or large downloadable AI models.

## 1. Open Android project

Open this **root folder** in Android Studio. Use JDK 17 for the Android build; the backend needs JDK 21. Android Studio's Gradle download and dependency sync require internet.

Versions are deliberately conservative for interoperability: AGP `8.11.1`, Kotlin `2.2.21`, Gradle `8.13`, Compose BOM `2025.06.01`. Upgrade only after a successful local build, not all at once.

```bash
cd LET-IT-OUT
./gradlew :app:assembleDebug
```

The Gradle 8.13 wrapper is included. Do not commit `local.properties` or API keys.

For an emulator, create `local.properties` with the Android SDK path. The debug build defaults to `http://10.0.2.2:8080/`:

```properties
sdk.dir=/Users/YOUR_USER/Library/Android/sdk
```

The app reads the Web OAuth client ID from Firebase's generated `default_web_client_id` after you add `app/google-services.json`. To override the chat server for any build, pass `-PapiBaseUrl=https://YOUR_SERVER_URL/` to Gradle. For a physical Android device, use an HTTPS URL reachable by that device instead of `10.0.2.2`. Release builds disallow plain HTTP; cleartext is debug-only.

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

The development Firebase project is configured as recorded in [docs/FIREBASE_SETUP.md](docs/FIREBASE_SETUP.md). If the ignored `app/google-services.json` is missing from a fresh checkout, download it from the Firebase console. **Explore in preview** is available only when Firebase is absent. Authentication is required for `/v1/chat` to avoid open, unbounded API spending; the separate backend and AI provider still need their own setup.

## 3. Start backend

For local development, copy `backend/.env.example` to the ignored `backend/.env`, set `GROQ_API_KEY`, and point `GOOGLE_APPLICATION_CREDENTIALS` at the ignored `backend/firebase-admin.json` file. Then run:

```bash
./backend/run-local.sh
```

Check `http://localhost:8080/health` for server liveness and `http://localhost:8080/ready` for chat configuration. The script stops with a clear message if either credential is missing. The `/v1/chat` endpoint returns 503 when Firebase Admin or Groq is unconfigured. Never place `GROQ_API_KEY` or the Firebase Admin JSON in the Android application.

For Cloud Run packaging, service identity, Secret Manager, Firestore limits, and staged rollout, see [docs/PRODUCTION_BACKEND.md](docs/PRODUCTION_BACKEND.md). This machine does not currently have the Google Cloud CLI or Docker installed, so no cloud deployment has been performed.

⚠️ **Free-tier warning:** Groq account rate limits and model availability can change; do not assume free API access is unlimited. Verify pricing and privacy terms before collecting real emotional conversations.

## 4. Backend request format

Authorization header: `Bearer <Firebase ID token>`

```json
{
  "mood": "ANGRY",
  "messages": [{"role":"user","content":"I had a bad day"}]
}
```

Response:

```json
{"reply":"That sounds frustrating. What happened?","safetyState":"NORMAL"}
```

## 5. Existing limitations

- Local verification on 25 October 2026: debug and release APK builds passed; three Android chat HTTP contract tests, one emulator preference persistence/reset test, and backend route tests passed. The backend answered `/health` and `/ready` locally and rejected an unauthenticated chat request. This does not verify production hosting or safety.
- The debug build, anonymous guest sign-in, guest-to-Google linking, and real AI replies were verified on an emulator. Fresh Google sign-in, voice recognition, and physical-device behavior remain unverified.
- After confirmed Firebase account deletion, the app clears in-memory conversation data and all local preferences. Deletion may still require recent Firebase reauthentication. For a production service, implement a complete user-data erasure policy wherever data is retained.
- Guest-to-Google account upgrade is implemented using Firebase credential linking; it needs collision-handling tests.
- Local chat remains only in ViewModel memory; changing mood intentionally clears it. Leaving the process also clears it. Not end-to-end encrypted.
- Prototype safety screening uses a few phrases and can miss many crisis situations. Do not release publicly until replaced/reviewed by appropriate experts.
- Built-in Android TTS voice quality and STT languages vary by device. STT might send audio to the device's selected speech-service provider.
- Companion portraits are static expression images; lip sync and live facial animation are not implemented.
- No ad SDK is integrated. Acquisition planning is in `docs/MARKETING_PLAN.md`.
- A static landing page lives in `web/` and is served at `https://let-it-go.in/`; iOS is a future milestone. The website's mood interaction is illustrative and does not connect to AI chat.
- Production backend packaging and shared limits are prepared, but the clinically reviewed safety routing and privacy review are still outstanding. Production chat remains disabled by default.

## 6. Next tasks

See [ROADMAP.md](ROADMAP.md). Priority sequence: build verification → auth smoke test → backend smoke test → conversation/voice UX → expert-reviewed safety/privacy → limited closed beta. **Do not launch publicly from this starter.**
