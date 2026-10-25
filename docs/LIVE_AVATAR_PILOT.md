# Talking avatar pilot

This opt-in pilot animates **only the woman/adult companion** during AI replies. The regular portrait and Android voice remain available if the service is unavailable. Support-resource replies stay on the regular voice path.

## Set up the avatar

1. Create a custom Simli avatar using the neutral, upper-left portrait in [`companion_woman_adult.png`](../app/src/main/res/drawable-nodpi/companion_woman_adult.png). Simli needs a single portrait, not the entire 3 × 3 expression sheet. See [Simli's avatar guide](https://docs.simli.com/api-reference/create-avatar).
2. Put the resulting face ID and API key in the ignored `backend/.env` file:

   ```text
   SIMLI_API_KEY=your-secret-key
   SIMLI_FACE_ID_WOMAN_ADULT=your-face-id
   ```

3. Run the normal backend and build the app with the pilot enabled:

   ```sh
   ./backend/run-local.sh
   ./gradlew :app:assembleDebug -PliveAvatarPilot=true
   ```

4. Sign in (or continue as a guest), choose the woman/adult companion, open chat, and select **More options → Try talking avatar**. Confirm the audio-sharing prompt. The menu item is absent in ordinary builds.

The backend checks Firebase authentication, limits avatar sessions to three starts per user and 60 globally per hour, and mints a [Simli session token](https://docs.simli.com/api-reference/compose-session-token). It never returns the Simli API key. Sessions are capped at five minutes and close after one idle minute. These are pilot cost controls; review them before a public rollout.

## How replies play

Android TTS produces PCM audio chunks without playing them locally. The app converts those chunks to 16 kHz mono PCM16 and sends them through the bundled Simli WebRTC client. Simli returns synchronized video and audio for the round portrait. Sending a new message or tapping **Stop** clears both TTS and the avatar buffer. On a connection failure, the view returns to the portrait and later replies use device voice.

The JavaScript bridge source is in `avatar-bridge/`; its checked-in bundle is in `app/src/main/assets/live-avatar/bridge.js`. After changing the bridge, run `cd avatar-bridge && npm ci && npm run build`.

## Pilot limits

- A Simli account and custom face are still required. The repository has no provider credentials, so this integration has been compiled and unit tested but not tested against a live Simli session.
- The Android WebView needs a working WebRTC-capable network. A failed connection leaves chat usable with the portrait.
- The current Android TTS engine must provide PCM16 synthesis callbacks for lip sync. Other formats fall back to regular device voice.
- Only the selected companion's spoken AI reply audio goes to Simli after the user opts in. App chat text is still handled by the existing chat backend.
