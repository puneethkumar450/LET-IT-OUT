# LET IT OUT

**A mood-aware Android AI listening companion (v0.1.0 developer starter).**

> **Status: developer starter, not a release.** The Android debug build and backend unit tests passed locally. On 14 October 2026, the redesigned app launched on an emulator and its preview screens and Android Back navigation were checked. Google Sign-In, real AI replies, and voice recognition still need Firebase/network setup and real-device testing. This is **not** ready for real users or public distribution.

**Android application ID:** `com.letitout.app`
**Display name:** `LET IT OUT`
**Intended repository:** https://github.com/puneethkumar450/LET-IT-OUT.git

## What has been implemented in source

- Native Jetpack Compose welcome screen, Google/guest/preview entry, seven-mood home screen, AI chat view, settings and privacy/help screen, with Android Back navigation.
- Consistent warm Material 3 theme, custom line icons for moods, and muted mood colours, with selection persisted through DataStore.
- Firebase Credential Manager Google sign-in and Firebase anonymous sign-in integration points (requires your own Firebase project).
- Ktor backend exposing `/health` and authenticated `/v1/chat`, verifying Firebase ID tokens, forwarding trimmed conversation context to Groq, limiting input and per-user traffic.
- Text conversation states (loading/errors/retry/clear), native Android push-to-talk transcript capture and optional text-to-speech playback.
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

Check `http://localhost:8080/health`. The `/v1/chat` endpoint returns 503 when Firebase admin or Groq is unconfigured. Never place `GROQ_API_KEY` in the Android application.

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

- The debug build and emulator preview were verified locally, but real sign-in, AI replies, voice recognition, and physical-device behavior remain unverified.
- Account deletion may require recent Firebase reauthentication. For a production service, implement complete user-data erasure wherever data is retained.
- Guest-to-Google account upgrade is implemented using Firebase credential linking; it needs collision-handling tests.
- Local chat remains only in ViewModel memory; changing mood intentionally clears it. Leaving the process also clears it. Not end-to-end encrypted.
- Prototype safety screening uses a few phrases and can miss many crisis situations. Do not release publicly until replaced/reviewed by appropriate experts.
- Built-in Android TTS voice quality and STT languages vary by device. STT might send audio to the device's selected speech-service provider.
- No ad SDK is integrated. Acquisition planning is in `docs/MARKETING_PLAN.md`.
- Website and iOS app are future milestones.
- The sample Ktor backend is not production-hardened (in-memory rate limits, no distributed usage enforcement or clinically reviewed safety routing).

## 6. Next tasks

See [ROADMAP.md](ROADMAP.md). Priority sequence: build verification → auth smoke test → backend smoke test → conversation/voice UX → expert-reviewed safety/privacy → limited closed beta. **Do not launch publicly from this starter.**
