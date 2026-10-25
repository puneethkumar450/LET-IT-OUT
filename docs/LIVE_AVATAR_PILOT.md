# Talking avatar pilot

This opt-in pilot is available from the **woman/adult companion** chat. For the first live test it uses a Simli template face, which looks different from the Android portrait; the consent prompt says so. The regular portrait and Android voice remain available if the service is unavailable. Support-resource replies stay on the regular voice path.

## Set up the avatar

1. For the free pilot, use the selected Simli template face ID. To switch to a matching custom face later, upload the ready-to-use [woman/adult neutral portrait](avatars/woman-adult-live-source.png) through [Simli's avatar flow](https://docs.simli.com/api-reference/create-avatar); custom face generation requires an eligible plan.
2. Put the face ID and API key in the ignored `backend/.env` file. Do not paste the API key into chat or commit it:

   ```text
   SIMLI_API_KEY=your-secret-key
   SIMLI_FACE_ID_PILOT=66d1f5af-9b9b-49e2-a97f-6f5f3d59a2d5/92f24a0c-f046-45df-8df0-af7449c04571
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

- A Simli account and API key are required. The free template pilot does not require a custom face. The repository has no provider credentials, so this integration has been compiled and unit tested but not tested against a live Simli session.
- The Android WebView needs a working WebRTC-capable network. A failed connection leaves chat usable with the portrait.
- The current Android TTS engine must provide PCM16 synthesis callbacks for lip sync. Other formats fall back to regular device voice.
- Only the selected companion's spoken AI reply audio goes to Simli after the user opts in. App chat text is still handled by the existing chat backend.
