# Architecture — small MVP

```
MainActivity → LetItOutApp (theme + saved back stack: splash, onboarding, signin, home, mood, chat, settings, privacy)
          |
   ui/IntroScreens.kt (Splash, Onboarding, SignIn) · ui/HomeScreens.kt (Home, MoodCheckIn) · ui/ChatScreen.kt
   ui/SettingsScreens.kt (Settings, PrivacyHelp) · shared: ui/Components.kt, ui/AppVisuals.kt
          |
      AppViewModel (StateFlow<UiState>)
          |
   AuthGateway      ChatGateway       UserPreferences             ReminderScheduler
   AuthManager      ChatRepository    DataStoreUserPreferences    WorkManagerReminderScheduler
       |                 |            (mood, settings, check-ins,      |
   Firebase Auth     Retrofit/OkHttp   reminder; on the phone only)  CheckInReminderWorker
   Credential Mgr        |                                          → ReminderNotifications
                         |
        Ktor (/v1/chat, /v1/report) -- Firebase ID token verification
                         |
        Body cap / validation → care check → per-user + global limits
                         |
        Persona prompt (+ extra care, + intensity hint) → Groq Chat Completions (free-tier quota)
                         |
        Reply polish + wording check → {"reply", "safetyState": NORMAL | SUPPORT_SUGGESTED}
```

- One Android `:app` module. `MainActivity.kt` keeps `MainActivity` (edge-to-edge; Recents preview turned off on Android 13+) and `LetItOutApp`: the theme and a simple saved back stack of route names. The splash goes to home when someone is signed in or has finished onboarding, otherwise to onboarding. After sign-in the app returns to where sign-in was opened from (Settings, a chat), or starts at home.
- Screens are grouped by area: `ui/IntroScreens.kt` (splash, onboarding, sign-in), `ui/HomeScreens.kt` (home, mood check-in with the optional intensity slider), `ui/ChatScreen.kt`, and `ui/SettingsScreens.kt` (settings, Privacy & Help). Shared pieces live in `ui/Components.kt` (`ScrollScreen`, `PillButton`, `ChipButton`, `PageHeader`, `SettingsGroup`, `SettingsRow`, `ToggleRow`, and `dialNumber`, which opens the dialer with the number filled in and never dials by itself). `ui/AppVisuals.kt` holds `AppColors`, the Fredoka/Nunito Google Fonts, `AppTypography`, the line icons in `AppIcons`, `Mood.icon` and the brand mark. The moods and their four-colour `MoodPalette` are in `data/Models.kt`.
- `AppViewModel` depends on small interfaces, not on Firebase, Retrofit, DataStore or WorkManager directly: `AuthGateway`, `ChatGateway` and `UserPreferences` in `data/Gateways.kt`, and `ReminderScheduler` in `reminder/ReminderScheduler.kt`. Unit tests use fakes. Hilt wires the real ones: `AppModule` binds `AuthGateway`→`AuthManager`, `ChatGateway`→`ChatRepository` and `UserPreferences`→`DataStoreUserPreferences`; `ReminderModule` binds `ReminderScheduler`→`WorkManagerReminderScheduler`; `NetworkModule` provides OkHttp and Retrofit.
- `data/Models.kt`, `data/Gateways.kt`, `data/ChatErrors.kt` and `ui/AppViewModel.kt` stay free of Android framework imports apart from `android.app.Activity` (in the auth gateway), Compose `Color` (in the mood palette), `androidx.lifecycle` and Hilt annotations, so their tests run on a plain JVM. `AppSettings` (read aloud, voice input, follow mood theme, onboarded) lives in `data/Models.kt` for the same reason.
- OkHttp timeouts: connect 15 s, read 45 s, whole call 60 s. They sit above the backend's 30 s provider timeout, so a slow reply is not cut off on the phone.
- `friendlyMessage()` in `data/ChatErrors.kt` turns every failure into a short human sentence. It uses the server's own sentence for 429 and 503 and the app's wording for everything else.
- Starting a new conversation (choosing a mood, **New conversation**, **End conversation**, clearing data, signing out) cancels the request in flight and bumps a generation counter. A late reply or error from the old conversation is dropped. `retry()` asks again for the last unanswered message without adding it twice, and `editLastMessage()` takes an unsent message back into the text box.
- The mood check-in's optional 1–10 rating goes to `chooseMood(mood, intensity)`, stays in `UiState` for that conversation (and for **New conversation**), and is sent with every chat request. **Just want to talk** on home and **Skip the rating** send none.
- A reply that comes back with `SUPPORT_SUGGESTED` is marked `supportResources` unless one of the previous 3 companion replies already was, so the doctor note and Tele-MANAS row show at most once every 4 replies. `UiState.supportMode` (any marked reply in this conversation) switches the chat to the calm lavender palette and adds the **Talk to someone** chip.
- The feeling check becomes due after 2 companion replies (or while the second is on its way) and is asked once per conversation: when leaving by the back arrow, system Back or the mood chip, or before **End conversation** or **New conversation**. Dismissing the dialog stays in the chat.
- **Report** under a companion reply calls `ChatGateway.report`, which posts to `/v1/report`; `UiState.reportNotice` shows a short thank-you or a gentle retry note.
- Groq credentials never reach the device. Firebase ID tokens are retrieved asynchronously immediately before request submission.
- The server controls the AI system prompt. The client sends only the selected mood, the optional intensity, the mood's opening question and recent messages (the last 12, each trimmed to 2,000 characters). `chatRequestFor` and `toChatReply` in `data/Models.kt` hold that shaping, so it is unit-tested.
- Sign-out and account deletion run in `AppViewModel` (not the screen's scope), so leaving Settings halfway cannot skip clearing check-ins, the reminder and the conversation. A guest can link Google from Settings without signing out.
- In-memory conversation only. DataStore (file `preferences`) persists the selected mood (`selected_mood`), the Settings toggles, whether onboarding is done, the last 20 feeling check-ins (home stats use the last 10) and the reminder settings. Writes are best effort and a corrupt file is reset. None of it leaves the phone: `allowBackup` is off, backup and device transfer exclude it (`res/xml/data_extraction_rules.xml`), and check-ins and the reminder are cleared on sign-out or account deletion. No Room, advertisements or background audio.
- WorkManager is used only for the opt-in daily check-in: one unique periodic job whose initial delay runs to the chosen time. Each run pins the next one back to that time, and skips the notification if it is running more than 2 hours late. Tapping the notification opens `MainActivity`.
- Android STT populates an editable text field; speech is not sent automatically. Android TTS reads new replies aloud when **Read replies aloud** is on, with a Stop control.
- Safety: the extra-care phrase check is a demonstrator, **not an adequate production classifier**. See [SAFETY.md](SAFETY.md). No medical claims or launch until independently reviewed.

## Backend files

`backend/src/main/kotlin/com/letitout/server/`:

- `Main.kt`: server start-up, routing (`/health`, `/v1/chat`, `/v1/report`), the chat and report handlers and `ChatDeps` (token verifier, AI provider, rate limiters, report store), so tests can swap in fakes.
- `Care.kt`: the extra-care phrase check (clear and softer phrases), the "recommends a doctor / Tele-MANAS" check, and the check for words replies must never use, including dropping the sentences that use them.
- `Persona.kt`: system prompt, per-mood guidance, extra-care note, intensity hint, fallback replies (English, Hindi and Telugu) and a short neutral line.
- `Reports.kt`: the reply-report request, the stored record and the size-capped JSON-lines report sink.
- `Groq.kt`: Groq client, response parsing and reply polishing.
- `RateLimiter.kt`: a sliding-window limiter that drops idle keys, so memory stays bounded.

## Network flow

Android → Firebase Auth `getIdToken` → `Authorization: Bearer` → Ktor:

1. 503 if Firebase admin or Groq is unconfigured.
2. `FirebaseAuth.verifyIdToken`: 401 for a missing or rejected token; 503 if verification itself fails, so people are not sent to sign out over an outage.
3. 413 if the body is over 64,000 characters; 400 if it is invalid. An `intensity` outside 1–10 is ignored, not rejected.
4. Care check on the last 3 user messages (or a Tele-MANAS mention in one of the last 2 replies): clear phrases, softer phrases, or neither. The intensity plays no part in it.
5. Limits. Normal turns: 20 per user per hour, then 429. Heavy and softer turns: their own 30 per hour first, then the normal quota. They never get an error; once both run out they get a warm fallback reply. An overall per-minute cap across all users protects the Groq key.
6. Groq HTTPS request with the persona prompt (plus the extra-care note on heavy turns, and the intensity hint when one was sent).
7. Reply polish and wording check (one rewrite when there is time, then any sentence that still uses a forbidden word is dropped) → JSON reply with `safetyState` → Android UI.

Production must use HTTPS for Android→Ktor too.

The Report button calls `POST /v1/report` with `{"reason": "hurtful|unsafe|wrong|other", "replyText": "..."}`. The backend checks the Firebase token (Groq is not needed), caps the body at 8,000 characters, allows 20 reports per user per hour and 300 per hour across everyone, and appends the reason, the reply text, a timestamp and a SHA-256-based reporter hash (never the uid or the person's own messages) to `REPORTS_FILE` as one JSON line, refusing new reports once the file reaches `REPORTS_MAX_BYTES` (50 MB by default). It answers 204.
