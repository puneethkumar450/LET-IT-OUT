# Architecture — small MVP

```
MainActivity / Compose Screens
          |
      AppViewModel (StateFlow)
          |
      ChatRepository ------ MoodPreferences(DataStore)
          |
        Retrofit
          |
         Ktor (/v1/chat) -- Firebase ID token verification
          |                   |
     Basic prototype safety   Validation / shared rate limits
          |
      Groq Chat Completions (free-tier quota)
```

- One Android `:app` module, feature UI currently grouped into a small number of Kotlin files. Refactor only as the app grows.
- Hilt is used for ViewModel/data provisioning; `AuthManager` owns Firebase auth and Credential Manager calls.
- Groq credentials never reach the device. Firebase ID tokens are retrieved asynchronously immediately before request submission.
- The server controls the AI system prompt; the client sends only user-selected mood and recent messages.
- Production uses Firestore transactions for per-user and global hourly counters across Cloud Run instances. Local development uses an in-memory limiter. Production chat defaults to disabled until the release safety review is complete.
- In-memory conversation only; DataStore persists selected mood. No Room, WorkManager, advertisements or background audio in this iteration.
- Android STT populates an editable text field; speech is not sent automatically. Android TTS is optional per AI reply.
- Safety: the provided keyword check is a demonstrator **not an adequate production classifier**. No medical claims or launch until independently reviewed.

## Network flow

Android → Firebase Auth `getIdToken` → `Authorization: Bearer` → Ktor `FirebaseAuth.verifyIdToken` → in-memory limits / validation → safety handling → Groq HTTPS request → JSON reply → Android UI. Production must use HTTPS for Android→Ktor too.
