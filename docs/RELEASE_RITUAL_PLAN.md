# Release ritual plan

Status: proposal, not built. Drafted 28 October 2026.

## The idea

After talking to Ari, the user taps **Let it go**. The conversation dissolves in a short animation and is cleared from the app. The app's name becomes something the user does, and the "chat isn't saved" promise becomes something they can see.

An optional share card lets them post "I let go of something today" without any of their words. It gives the app free word of mouth and material for Shorts and Reels.

## Why

- The app name and the action match, so people remember it.
- In the review research (`replica/fixes.md`), competitors were criticised for paywalls, for not feeling heard, and for privacy. A free, visible "it's gone" moment answers all three.
- It is mostly front-end work on top of what already exists. `AppViewModel.newConversation()` already clears the in-memory chat, and chat is never written to disk.

## What the user sees

1. In chat, **End conversation** in the More options menu becomes **Let it go**. The **End chat** chip in live mode gets the same option.
2. A confirmation sheet: "Ready to let this go? This clears the conversation from the app." Buttons: **Let it go** and **Not yet**.
3. A 2–3 second animation. The message bubbles fade, drift up and break into particles over a calm gradient in the mood palette. Haptic feedback plays at the end.
4. An end screen: "Gone. You don't have to carry that." Buttons: **Share** (optional), **Talk again** and **Home**.
5. A **Reduce motion** setting, or the system animation setting, replaces the animation with a simple fade.

## Share card

- The image is drawn on the device from a fixed template: an abstract animation frame, "I let go of something today", and the LET IT GO name and website. **It never contains message text, mood, intensity or Ari's replies.**
- It is shared through the Android share sheet (`ACTION_SEND` with a `FileProvider` URI). The image is written to the cache folder and deleted after sharing.
- Nothing is uploaded by the app itself.

## Safety rules

- When the conversation has a crisis reply (`UiState.supportMode`), **Let it go** must not hide the helplines. The end screen in that case shows Tele-MANAS 14416 and 112 first, and the share card is not offered.
- The copy must stay honest. Release clears the conversation **from the app**. Messages that were already sent to the backend and Groq follow those services' retention rules. Check what the backend logs and what Groq keeps before writing "deleted forever" anywhere.
- No streaks, counters or guilt-based reminders tied to releasing.

## Build steps

| Step | Size | Where |
| --- | --- | --- |
| Rename the menu item and live chip; add the confirmation sheet | S | `ui/ChatScreen.kt` |
| `ReleaseRitualScreen` with the particle animation and reduce-motion fallback | M | new `ui/ReleaseRitualScreen.kt`, nav route in `MainActivity.kt` |
| Clear the chat once the animation has started: stop live mode and TTS, then call `vm.newConversation()` | S | `ChatScreen.kt`, `AppViewModel.kt` |
| Supportive end screen; crisis variant shows helplines and no share | S | `ReleaseRitualScreen.kt` |
| Share card bitmap, `FileProvider` setup, share sheet, cache cleanup | M | new `ui/ShareCard.kt`, `AndroidManifest.xml`, `res/xml/` |
| Reduce motion setting | S | `data/Preferences.kt`, `ui/SettingsScreens.kt` |
| Privacy text: explain what release clears and what it doesn't | S | Privacy & Help screen, `docs/DATA_SAFETY_REVIEW.md` |

Estimated total: about 3–5 days.

## Testing

- Unit test: after release, `messages` is empty and a late reply from a cancelled request doesn't come back (`replyGeneration` already handles this; add a test).
- UI test: the crisis conversation path shows helplines on the end screen and has no Share button.
- Check on a device: animation smoothness on a low-end phone, reduce-motion fallback, TalkBack reads the end screen, and live mode and TTS stop before the animation.
- Open the shared image and confirm that none of the user's words appear in it.

## Measuring it

There is no analytics in the app today. If analytics are added, follow the privacy-safe event rules in `docs/MARKETING_PLAN.md`. Count only `release_completed` and `share_card_shared`, with no content, mood or timing details. Success means more people come back for a second conversation within 7 days, not just more releases.

## Later ideas built on this

- **Unsent letters**: write to someone, then release or keep the letter.
- **2-minute night unload**: park worries before bed and check in the next morning.
- Different release styles (burn, float, wash away), possibly as a small paid pack once the core ritual is free and stable.
