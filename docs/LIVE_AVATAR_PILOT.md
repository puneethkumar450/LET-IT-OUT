# Talking avatar pilot

In debug builds, Start talking on the Ari welcome screen opens the full-screen Simli avatar for signed-in users. First-time setup, the welcome screen, sign-in, splash, the chat summary, and the live connection placeholder all use the same AI-generated Ari portrait. There is no gender, age, mood, or portrait picker in the current Android flow. Spoken AI replies are sent as audio to Simli for lip sync; a short disclosure appears in setup and the details are in Privacy & Help. The still and Android voice remain available if the service is unavailable. Support-resource replies stay on the regular voice path.

## Set up the avatar

1. Use the existing selected Simli Trinity-2 face ID for this pilot. `app/src/main/res/drawable-nodpi/ari_onboarding_fullscreen.png` is the generated 9:16 Ari portrait. `CompanionPortrait` crops that image around the face for small UI placements without starting a Simli session. The live video itself still comes from the selected Simli face and can differ slightly from the generated still.
2. Put the face ID and API key in the ignored `backend/.env` file. Upload the 9:16 images in `backend/assets/avatar-backgrounds/` as [Simli backgrounds](https://docs.simli.com/api-reference/upload-background-or-foreground), then add the returned IDs for each mood. Do not paste the API key into chat or commit it:

   ```text
   SIMLI_API_KEY=your-secret-key
   SIMLI_FACE_ID_PILOT=66d1f5af-9b9b-49e2-a97f-6f5f3d59a2d5/92f24a0c-f046-45df-8df0-af7449c04571
   SIMLI_BACKGROUND_ID_ANGRY=your-angry-background-id
   SIMLI_BACKGROUND_ID_SAD=your-sad-background-id
   SIMLI_BACKGROUND_ID_STRESSED=your-stressed-background-id
   SIMLI_BACKGROUND_ID_ANXIOUS=your-anxious-background-id
   SIMLI_BACKGROUND_ID_LONELY=your-lonely-background-id
   SIMLI_BACKGROUND_ID_HAPPY=your-happy-background-id
   SIMLI_BACKGROUND_ID_TALK=your-talk-background-id
   ```

3. Run the normal backend and build the debug app. The pilot is enabled by default in debug builds; use `-PliveAvatarPilot=false` to hide it. Release builds keep it disabled.

   ```sh
   ./backend/run-local.sh
   ./gradlew :app:assembleDebug
   ```

4. During first-time setup, meet Ari and sign in or continue as a guest. The Ari welcome screen shows **Start talking**; tapping it opens chat and starts live video for signed-in users. Tap **Show chat** (or Back) to read the full conversation, then use **More options → Return to live avatar** to reopen it. The welcome screen also offers **Read chat summary** when this process has messages. Without an authenticated or guest session, chat stays in preview mode.

The welcome screen uses lifecycle-aware coroutines to fetch a Firebase ID token and warm the backend `/health` connection, initialize Android TTS, and load the local avatar JavaScript in a reusable WebView. This does not request a Simli token or start streaming; those happen after Start talking. The effect on device startup time has not yet been measured.

The backend checks Firebase authentication, limits avatar sessions to three starts per user and 60 globally per hour, and mints a [Simli session token](https://docs.simli.com/api-reference/compose-session-token). It never returns the Simli API key. Sessions are capped at five minutes and close after two idle minutes. These are pilot cost controls; review them before a public rollout.

## How replies play

The pilot opens a full-screen video view. A caption card over the video shows your last message and Ari's latest reply; **Show chat** opens the full conversation. Type in the composer or tap the microphone; a completed voice transcript sends automatically. Spoken AI replies play automatically while the view is open. The back button also returns to the full conversation.

Android TTS produces PCM audio chunks without playing them locally. The app converts those chunks to 16 kHz mono PCM16 and sends them through the bundled Simli WebRTC client. The token requests a portrait video stream with Simli's default camera framing and the background for the selected mood. The Android app displays decoded frames edge to edge, with the back button and composer over the video. Sending a new message or leaving the view clears both TTS and the avatar buffer. On a connection or rendering failure, the view returns to the regular chat with portrait and device voice.

The JavaScript bridge source is in `avatar-bridge/`; its checked-in bundle is in `app/src/main/assets/live-avatar/bridge.js`. After changing the bridge, run `cd avatar-bridge && npm ci && npm run build`.

The bridge's JavaScript interface (`AndroidAvatar`) is attached in `LiveAvatarController.preload()`, before the page loads. WebView only exposes an interface added after load on the next reload, so attaching it later leaves the page unable to report frames and the view stuck on "Connecting avatar…". Each live view sets `LiveAvatarController.listener` for its session.

## Pilot limits

- A Simli account and API key are required. The existing Simli face avoids creating a second custom face. The repository has no provider credentials. The local pilot has connected to Simli and returned an AI reply; visible lip sync still needs confirmation on the emulator or a device.
- The Android WebView needs a working WebRTC-capable network. A failed connection leaves chat usable with the portrait.
- The current Android TTS engine must provide PCM16 synthesis callbacks for lip sync. Other formats fall back to regular device voice.
- Spoken AI reply audio goes to Simli while live mode is open. App chat text is still handled by the existing chat backend.

## Website hello video

The Ari page on let-it-go.in plays `web/dist/characters/ari-hello.mp4`: Ari's recorded greeting, lip-synced once by Simli and saved as a video. Visitors only stream a static file, so the website needs no backend, token, or Simli session.

To re-record it after changing the voice or script:

1. Put the new voice in place with `web/scripts/make-greeting.sh path/to/recording.wav`.
2. Run `python3 web/scripts/record-hello.py` and open the `http://localhost:4174/#…` link it prints in Chrome (the part after `#` is a per-run token; the server rejects requests without it). It reads the Simli key and face from `backend/.env`, mints one 60-second session, and records Ari while she speaks.
3. Click **Record hello**. When Ari finishes, the page uploads the recording and the script writes the MP4 with ffmpeg.

The recorder's Simli client is `avatar-bridge/src/web.js`; rebuild it with `cd avatar-bridge && npm run build:web`.
