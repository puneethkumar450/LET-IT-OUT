# Architecture — Ari test build

```
MainActivity / Compose Screens (Ari welcome → live avatar / chat summary)
          |
      AppViewModel (StateFlow)
          |
      ChatRepository ------ MoodPreferences(DataStore)
          |
        Retrofit
          |
         Ktor (/v1/chat, /v1/avatar/session) -- Firebase ID token verification
          |                   |
   Phrase + model safety       Validation / shared rate limits
          |
      Groq Chat Completions + Simli live face
```

- One Android `:app` module, feature UI currently grouped into a small number of Kotlin files. Refactor only as the app grows.
- Hilt is used for ViewModel/data provisioning; `AuthManager` owns Firebase auth and Credential Manager calls.
- Groq credentials never reach the device. Firebase ID tokens are retrieved asynchronously immediately before request submission.
- The server controls the AI system prompt; the client sends recent messages and a mood field retained for API compatibility. The current Android flow does not ask users to pick a mood.
- Production uses Firestore transactions for per-user and global hourly counters across Cloud Run instances. Local development uses an in-memory limiter. Production chat defaults to disabled until the release safety review is complete.
- In-memory conversation only; DataStore persists speech settings and legacy mood data. No Room, WorkManager, advertisements or background audio in this iteration.
- Ari's welcome screen warms Firebase auth, `/health`, Android TTS, and the local Simli WebView bundle without opening a live session. Live mode starts on user action. In live mode a completed Android STT transcript sends automatically; in summary mode the field stays editable. Android TTS provides spoken replies and PCM chunks for Simli lip sync.
- Safety uses a phrase check plus a model classifier, still **not clinically validated or adequate for production**. No medical claims or public launch until independently reviewed.

## Network flow

Android → Firebase Auth `getIdToken` → `Authorization: Bearer` → Ktor `FirebaseAuth.verifyIdToken` → in-memory limits / validation → safety handling → Groq HTTPS request → JSON reply → Android UI. Production must use HTTPS for Android→Ktor too.
