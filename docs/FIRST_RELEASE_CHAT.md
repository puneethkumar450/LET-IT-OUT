# First release: chat design

Decided 29 October 2026.

## Decision

The first public release ships **text chat with voice**, not the live talking avatar.

- **Start talking** opens the regular chat screen: Ari's portrait in the header, message bubbles, a typing box, and a push-to-talk microphone.
- Ari's replies can be read aloud with the device voice. Each reply has **Listen** and **Stop**, and Settings › Read replies aloud turns automatic reading on or off.
- Ending a chat with messages runs the release ritual (`docs/RELEASE_RITUAL_PLAN.md`).
- The live avatar stays a debug-only pilot. Release builds already set `LIVE_AVATAR_PILOT` to `false`, so no code change is needed to keep it out.

## Why not the live avatar yet

- **Reliability.** The first test on a real phone (moto g45, 29 October 2026) found that the avatar never got past "Connecting avatar…". The JavaScript bridge was attached after the page had loaded, so no frames reached the app. That is now fixed, but the avatar has worked on only one device so far, and a session also disconnected while the phone was being unlocked.
- **Cost.** Every live minute is billed by Simli, and there is no paid plan or per-user budget yet. Text chat costs only the AI reply.
- **Privacy.** Live mode sends Ari's spoken reply audio to another provider. That needs its own line in the privacy policy and the Play data safety form before a public launch.
- **Network.** WebRTC video needs a steady connection. Text chat works on weak mobile data.

## Before the avatar can ship (target: a later update)

1. It works on at least three real phones, including a low-end one, through: connect, speak, reply, lip sync, leave, return.
2. A session survives screen lock and unlock, or reconnects cleanly.
3. Captions show the latest exchange over the video, and **Show chat** returns to the full conversation (built 29 October 2026; needs a device check).
4. The cost per active user is measured, with a monthly cap.
5. The privacy policy and Play data safety answers name Simli.
