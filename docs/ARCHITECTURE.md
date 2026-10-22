# Architecture — small MVP

```
MainActivity → LetItOutApp (theme + back stack; starts at "home" when signed in)
          |
   ui/screens/* (Welcome, Auth, Home, Chat, Settings, Privacy) + ui/components/Common.kt
          |
      AppViewModel (StateFlow<UiState>)
          |
   AuthGateway      ChatGateway       UserPreferences             ReminderScheduler
   AuthManager      ChatRepository    DataStoreUserPreferences    WorkManagerReminderScheduler
       |                 |            (mood, check-ins, reminder;      |
   Firebase Auth     Retrofit/OkHttp   on the phone only)           CheckInReminderWorker
   Credential Mgr        |                                          → ReminderNotifications
                         |
        Ktor (/v1/chat) -- Firebase ID token verification
                         |
        Body cap / validation → care check → per-user + global limits
                         |
        Persona prompt (+ extra care) → Groq Chat Completions (free-tier quota)
                         |
        Reply polish + wording check → {"reply", "safetyState": NORMAL | SUPPORT_SUGGESTED}
```

- One Android `:app` module. `MainActivity.kt` keeps only `MainActivity` and `LetItOutApp` (theme and a simple saved back stack). Each screen has its own file in `ui/screens/`. Shared pieces (`PageBackButton`, `SettingsItem`, `InformationCard`, the Tele-MANAS dialer helper) live in `ui/components/Common.kt`.
- `AppViewModel` depends on small interfaces, not on Firebase, Retrofit, DataStore or WorkManager directly: `AuthGateway`, `ChatGateway` and `UserPreferences` in `data/Gateways.kt`, and `ReminderScheduler` in `reminder/ReminderScheduler.kt`. Unit tests use fakes. Hilt wires the real ones: `AppModule` binds `AuthGateway`→`AuthManager`, `ChatGateway`→`ChatRepository` and `UserPreferences`→`DataStoreUserPreferences`; `ReminderModule` binds `ReminderScheduler`→`WorkManagerReminderScheduler`; `NetworkModule` provides OkHttp and Retrofit.
- OkHttp timeouts: connect 15 s, read 45 s, whole call 60 s. They sit above the backend's 30 s provider timeout, so a slow reply is not cut off on the phone.
- `friendlyMessage()` in `data/ChatErrors.kt` turns every failure into a short human sentence. It uses the server's own sentence for 429 and 503 and the app's wording for everything else.
- Starting a new conversation (changing mood or clearing) cancels the request in flight and bumps a generation counter. A late reply or error from the old conversation is dropped. `retry()` asks again for the last unanswered message without adding it twice.
- The support card shows when a reply comes back with `SUPPORT_SUGGESTED`. After **Not now** it stays hidden for the next 4 companion replies. The feeling check becomes due after 2 companion replies and is asked once per conversation, when leaving by the top-bar or system Back, or before **Clear** empties the chat.
- Groq credentials never reach the device. Firebase ID tokens are retrieved asynchronously immediately before request submission.
- The server controls the AI system prompt. The client sends only the selected mood, the mood's opening question and recent messages (the last 12, each trimmed to 2,000 characters). `chatRequestFor` and `toChatReply` in `data/Models.kt` hold that shaping, so it is unit-tested.
- Sign-out and account deletion run in `AppViewModel` (not the screen's scope), so leaving Settings halfway cannot skip clearing check-ins, the reminder and the conversation. A guest can switch to Google from Settings without signing out.
- In-memory conversation only. DataStore (file `preferences`, existing key `selected_mood`) persists the selected mood, the last 20 feeling check-ins (home stats use the last 10) and the reminder settings. None of it leaves the phone. Backup and device transfer exclude it (`res/xml/data_extraction_rules.xml`), and it is cleared on sign-out or account deletion. No Room, advertisements or background audio.
- WorkManager is used only for the opt-in daily check-in: one unique periodic job whose initial delay runs to the chosen time. Each run pins the next one back to that time, and skips the notification if it is running more than 2 hours late. Tapping the notification opens `MainActivity`.
- Android STT populates an editable text field; speech is not sent automatically. Android TTS is optional per AI reply.
- Safety: the extra-care phrase check is a demonstrator, **not an adequate production classifier**. See [SAFETY.md](SAFETY.md). No medical claims or launch until independently reviewed.

## Backend files

`backend/src/main/kotlin/com/letitout/server/`:

- `Main.kt`: server start-up, routing (`/health`, `/v1/chat`, `/v1/report`), the chat and report handlers and `ChatDeps` (token verifier, AI provider, rate limiters, report store), so tests can swap in fakes.
- `Care.kt`: the extra-care phrase check (clear and softer phrases), the "recommends a doctor / Tele-MANAS" check, and the check for words replies must never use, including dropping the sentences that use them.
- `Persona.kt`: system prompt, per-mood guidance, extra-care note, fallback replies (English, Hindi and Telugu) and a short neutral line.
- `Reports.kt`: the reply-report request, the stored record and the size-capped JSON-lines report sink.
- `Groq.kt`: Groq client, response parsing and reply polishing.
- `RateLimiter.kt`: a sliding-window limiter that drops idle keys, so memory stays bounded.

## Network flow

Android → Firebase Auth `getIdToken` → `Authorization: Bearer` → Ktor:

1. 503 if Firebase admin or Groq is unconfigured.
2. `FirebaseAuth.verifyIdToken`: 401 for a missing or rejected token; 503 if verification itself fails, so people are not sent to sign out over an outage.
3. 413 if the body is over 64,000 characters; 400 if it is invalid.
4. Care check on the last 3 user messages (or a Tele-MANAS mention in one of the last 2 replies): clear phrases, softer phrases, or neither.
5. Limits. Normal turns: 20 per user per hour, then 429. Heavy and softer turns: their own 30 per hour first, then the normal quota. They never get an error; once both run out they get a warm fallback reply. An overall per-minute cap across all users protects the Groq key.
6. Groq HTTPS request with the persona prompt (plus the extra-care note on heavy turns).
7. Reply polish and wording check (one rewrite when there is time, then any sentence that still uses a forbidden word is dropped) → JSON reply with `safetyState` → Android UI.

Production must use HTTPS for Android→Ktor too.

The Report button calls `POST /v1/report` with `{"reason": "hurtful|unsafe|wrong|other", "replyText": "..."}`. The backend checks the Firebase token, allows 20 reports per user per hour and 300 per hour across everyone, and appends the reason, the reply text, a timestamp and a SHA-256-based reporter hash (never the uid or the person's own messages) to `REPORTS_FILE` as one JSON line, refusing new reports once the file reaches `REPORTS_MAX_BYTES` (50 MB by default). It answers 204.
