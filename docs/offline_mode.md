# Offline Ari mode — implementation plan

## Goal

Add an optional **Offline Ari** mode to LET IT GO. When the phone supports an
on-device model and offline voice, the user can have a short conversation
without sending message text to the LET IT GO backend. The existing cloud chat
and Simli live-avatar paths remain available as fallbacks; this plan does not
remove authentication, safety disclosures, or the current text-summary flow.

## Product boundary

Offline mode is a private, best-effort companion—not a medical or crisis
service. It must show a clear status such as `On-device` or `Cloud fallback`.
It must never claim that every Android phone supports local inference, that
offline voice is always available, or that local generation is clinically safe.

## Recommended first implementation

**Started on `feature/offline-mode-plan`:** the first vertical slice now has a
user-selectable Offline Ari preview, a local deterministic fallback runtime,
offline/no-account chat entry, and tests for normal and obvious-risk responses.
The Gemma runtime is now wired behind the same seam; it becomes active when the
user has downloaded `files/offline-models/ari-gemma.task`.

1. Detect Gemini Nano through ML Kit GenAI/AICore at runtime.
2. Use Gemini Nano only when the model status is `AVAILABLE`.
3. If the model is `DOWNLOADABLE`, offer a user-controlled model download over
   Wi-Fi; do not download silently on first launch.
4. If the status is `UNAVAILABLE`, route to the existing Ktor/Groq flow.
5. Keep a deterministic local fallback for no-network/no-model states: a short
   supportive response, the current typed input, and human-support links.
6. Use Android `TextToSpeech` with the user's installed offline voice when
   available. If no offline voice exists, keep text mode and explain why audio
   is unavailable.
7. Animate the bundled Ari portrait locally with a small set of mouth, blink,
   and idle frames. Do not attempt photorealistic video generation on-device in
   this phase.

## Runtime flow

```text
User opens chat
  → check local model status on a background coroutine
  → AVAILABLE: local Ari response
  → DOWNLOADABLE: ask before downloading, then local Ari response
  → UNAVAILABLE/error/quota: cloud fallback or offline canned response
  → local Android TTS + lightweight mouth animation
```

The local conversation should remain in the existing ViewModel/DataStore
boundary. Do not persist raw emotional messages by default. Do not run local
generation in the background; it is a foreground interaction and may be
blocked or quota-limited by AICore.

## Model options and cost

### Gemini Nano / ML Kit GenAI

- No per-request cloud token charge because inference runs on the device.
- Supported-device coverage is limited and changes with AICore/model versions.
- AICore can return busy and battery-quota errors; implement backoff and
  fallback.
- ML Kit may send API performance/usage metrics to Google; update privacy and
  Data Safety disclosures.
- Verify the API is production-eligible before release; do not ship an
  experimental/preview dependency as the only chat path.

### Gemma via Google AI Edge / MediaPipe

- Local inference has no per-token API charge.
- Use a small, quantized mobile model and prefer an optional download rather
  than embedding a large model in the base APK.
- Include the required Gemma license/notice and prohibited-use terms when
  distributing the model.
- Benchmark RAM, startup latency, tokens/second, battery, and thermal behavior
  on a low-end Android device before choosing a model.

The first adapter uses MediaPipe `tasks-genai` because it compiles with the
project's current Kotlin toolchain. Google marks this API maintenance-only and
recommends migrating to LiteRT-LM; that migration is blocked until the project
upgrades to a compatible Kotlin toolchain. The model itself is not checked into
Git or bundled in the APK. A future model download screen must verify the
Gemma license/notice, free storage, Wi-Fi consent, checksum, cancellation, and
resume behavior.

### Gemma licensing and policy boundary

Gemma is **not MIT-licensed**. This app must use the exact Gemma Terms of Use
and Google's Gemma Prohibited Use Policy that apply to the selected model
version. The app includes `app/src/main/assets/gemma_notice.txt` with the
required redistribution notice and links. If a Gemma model or model derivative
is distributed to users, the distribution must also pass through the applicable
use restrictions and provide the terms to recipients. Do not describe Gemma as
MIT, claim Google endorsement, or present Ari as a medical, legal, financial, or
emergency service. The app must retain its safety checks, disclose that replies
are AI-generated, and provide human/emergency support routing.

Before publishing a model download or bundling weights, verify the selected
model's current license, notice requirements, model-card limitations, and
Google Play policy with legal counsel. The official terms can change.

### Costs that remain

Play developer fees, model hosting/download bandwidth, support, testing, and
cloud fallback costs remain. Simli is still a paid/network feature and is not
part of Offline Ari mode.

## Android design

Add a small `OfflineAiRuntime` abstraction rather than coupling Compose screens
to ML Kit:

```kotlin
interface OfflineAiRuntime {
    suspend fun status(): OfflineModelStatus
    suspend fun reply(history: List<ChatMessage>): OfflineReply
}
```

Suggested states:

```text
Checking, Available, Downloadable, Downloading, Unavailable,
Busy, BatteryQuotaExceeded, Failed
```

Use lifecycle-aware coroutines from the existing ViewModel. Keep the runtime
lazy: do not load a model or request a download on splash/home. Warm it only
after the user enables Offline Ari or opens chat. Cancel generation when the
conversation leaves the foreground.

## Safety requirements

- Keep the current crisis-support UI and explicit disclosure in offline mode.
- Add a conservative local phrase gate for obvious self-harm/violence phrases,
  but do not describe it as complete safety detection.
- For ambiguous or high-risk text, show human-support guidance and ask the user
  to use the connected mode if they want a full server-side check.
- Evaluate local and cloud prompts separately; different model versions can
  produce different outputs.
- Add an in-app report/flag action for AI-generated replies before public
  release.
- Never use the local model to claim diagnosis, treatment, or emergency
  intervention.

## Privacy and Play Store work

- Update the Privacy Policy to explain that local prompts/responses normally
  stay on-device, while ML Kit metrics and cloud fallback may still involve
  third parties.
- Update Data Safety answers for both local and cloud paths.
- Label Ari clearly as AI-generated.
- Keep Terms of Service and the 18+ boundary visible.
- Test account deletion, cloud fallback, model download cancellation, and data
  clearing after process death.
- Publish an Android App Bundle and measure the delivered size on supported and
  unsupported devices.

## Milestones

### M1 — capability probe

- Add a non-chat diagnostic that reports model status and offline TTS status.
- Test on one supported Pixel/Samsung device and one unsupported device.
- No user message is sent during the probe.

### M2 — local text reply

- Add `OfflineAiRuntime` and a ViewModel-controlled mode switch.
- Stream a short local reply into the existing chat summary.
- Handle unavailable, busy, quota, cancellation, and model-download errors.

### M3 — local voice and portrait animation

- Use offline Android TTS where available.
- Add mouth/idle/blink animation synchronized to speaking state.
- Keep typed input and text replies usable when audio fails.

### M4 — evaluation and release gate

- Compare local and cloud replies on a fictional safety/regression set.
- Profile latency, memory, battery, and thermal impact.
- Complete privacy/Data Safety review and closed-device testing.
- Enable the feature behind a controlled flag before public rollout.

## Definition of done

- The app never crashes when Gemini Nano/AICore is missing.
- A supported device can complete a short local text conversation with airplane
  mode enabled after the model is installed.
- An unsupported device cleanly uses cloud chat or the offline fallback.
- Offline TTS failure leaves the text conversation usable.
- No provider API key is placed in the Android app.
- Tests cover status mapping, fallback selection, cancellation, and quota/error
  handling.
- Privacy Policy, Terms, Data Safety, and Play listing accurately describe both
  local and cloud behavior.
