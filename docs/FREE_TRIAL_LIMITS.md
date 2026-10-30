# Free trial limits

Built 29 October 2026 on `feature/release-ritual`. The screen designs are on a private design canvas; the app keeps its own backgrounds, colours and mood themes and uses the designs only as a guide for the new pieces.

## The limits

| | Limit | Setting |
| --- | --- | --- |
| Text and voice chat | 3 chats a day, up to 15 minutes each | `TRIAL_CHATS_PER_DAY`, `TRIAL_CHAT_MINUTES` |
| Live preview | 3 minutes, once every 7 days, taken from that chat's 15 minutes; counted only once Ari's video appears | `TRIAL_LIVE_SECONDS`, `TRIAL_LIVE_EVERY_DAYS` |
| Live attempts | 3 session tokens every 7 days, so failed connections can be retried but not without limit | `TRIAL_LIVE_ATTEMPTS` |
| Daily reset | Midnight, India time | `TRIAL_TIMEZONE` (default `Asia/Kolkata`) |

The existing hourly limits stay as a second layer: `CHAT_USER_HOURLY_LIMIT` (now 40, so two back-to-back chats fit in an hour) and `CHAT_GLOBAL_HOURLY_LIMIT` (300).

## Why these numbers

Checked 29 October 2026; recheck before launch.

- **Groq free plan:** about 8,000 tokens a minute and 200,000 a day for each model, shared by the whole account ([rate limits](https://console.groq.com/docs/rate-limits); the page doesn't clearly say whether these are free-plan or Developer-plan figures, so check the console's limits page). One reply uses about 1,200 tokens, so the daily token limit allows about 165 replies a day across all users: enough for a closed beta of about 15 people.
- **Groq pay-as-you-go:** `openai/gpt-oss-120b` costs $0.15 per million input tokens and $0.60 per million output tokens ([model page](https://console.groq.com/docs/model/openai/gpt-oss-120b)). That's an estimated $0.005 for a 12-message chat.
- **Simli free plan:** 50 minutes a month and 1 session at a time ([pricing](https://www.simli.com/pricing)), so about 16 three-minute previews a month. The Hobby plan is $10 a month for 1,000 minutes and 2 sessions at a time.

Before a public launch, move Groq to pay-as-you-go and Simli to Hobby. Release builds still have `LIVE_AVATAR_PILOT=false`, so the live preview only appears in debug builds.

## How it behaves

- A chat counts when its first message arrives (or when a live preview opens in it). Opening and leaving an empty chat is free.
- The server keeps the real count and time; the app shows what the server says. Each chat has a random ID (`sessionId`) that changes with every new conversation.
- The header shows whole minutes left. In the last 2 minutes the pill turns warm and a short note appears above the composer.
- At zero the composer locks. Ari finishes any reply already on its way, then the app asks for a goodbye (`closing: true`), allowed once within 2 minutes after the end. If that fails, the app shows a fixed goodbye. A card then offers **Let it go**, **Start a new chat · N left today**, or **Back to home**.
- The live preview starts only after the intro sheet's **Start live preview**. Its clock starts at the first video frame and shows m:ss. At 30 seconds left a note appears; at zero live mode closes and the chat carries on in text. When Ari's video first appears, the app calls `/v1/avatar/started` and only then does the week's preview count, so a connection that fails is given back. Reconnecting in the same chat after that continues the same preview, timed from when the video appeared. Because the app sends that confirmation, a modified app could skip it, so the server caps session tokens at 3 every 7 days. The Simli session is capped at the preview length plus 20 seconds.
- **Crisis support:** the local phrase check runs before every limit, so a crisis message is always answered, even when today's chats are used. A chat that opens support has no timer and doesn't count.
- **Busy:** when the global hourly limit or Groq's own limit is hit, the server answers `503` with `code: busy` and the app shows "Ari is taking a short break" with helplines. It doesn't use up the user's chats.
- Home shows the bars and "N of 3 free chats left today". With none left (and no chat in progress) it says when Ari is back and offers Tele-MANAS.

## API

- `GET /v1/allowance` returns `chatsPerDay`, `chatsLeft`, `chatMinutes`, `resetsAt`, `liveSeconds`, `liveAvailable`, `nextLiveAt` and `liveEnabled`. Times are epoch milliseconds.
- `POST /v1/chat` takes `sessionId` (required) and `closing`, and returns `secondsLeft` (null when the chat has no limit) and `ended`.
- `POST /v1/avatar/session?mood=…&sessionId=…` returns `sessionToken` and `seconds`.
- `POST /v1/avatar/started?sessionId=…` counts the preview once the video appears: `204`, or `404` when no preview was started in that chat.
- Refusals are `403` with `code` `daily_limit`, `session_ended` or `live_used` (with `nextLiveAt`), or `503` with `code: busy`.

## Storage

Production keeps one Firestore document per user in `trial_allowances`, keyed by a SHA-256 digest of the UID. It holds today's chat IDs, their start times, the last live preview time and recent live attempt times; no chat text. Add a TTL policy on `trial_allowances`, field `expiresAt` (set to 8 days after the last change). Local development keeps the counts in memory, so restarting the backend resets them.

## Not done yet

- Checked on a Pixel 6 emulator, 29 October 2026, with short test limits (2 chats of 3 minutes): Home allowance, the minutes-left label, the 2-minute note, Ari's goodbye and the time-up card, the daily limit in chat and on Home, crisis support with no chats left, the live intro sheet, and the busy screen. The live video itself didn't connect on that emulator (its WebView is version 91), so the live countdown and its 30-second note still need a phone.
- Not yet tried on a phone.
- No paid plan: the designs' "LET IT GO Plus" card was left out until there is a price and a way to sign up.
- A user can get new allowances by signing out of a guest account and starting another. App Check and abuse monitoring are still on the launch list.
