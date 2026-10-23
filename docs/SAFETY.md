# Safety and responsible emotional support — RELEASE BLOCKER

**LET IT OUT is an AI listening companion for adults (18+), not medical therapy or urgent care.** The prototype's deterministic phrase checks are intentionally incomplete: they can miss self-harm thoughts, threats of violence, messages in other languages, or nuanced/delayed risk. They can also over-trigger. **Do not expose the included AI flow to real distressed users as a production mental health service.**

## How the prototype handles heavy moments

The app owner chose a soft approach: no special mode that takes over the chat, no alarming wording, and a doctor as the best option.

- **No hard stop.** No canned reply ends the conversation. The companion stays the same warm friend, and the chat keeps its usual header (mood chip and ⋯ menu).
- **No alarming words.** App copy, dialogs, errors, notifications and AI replies never use: crisis, emergency, suicide/suicidal, kill, die/dying/death, danger, life-threatening, "are you safe".
  - The persona forbids these words, and the server checks every reply (`soundsAlarming` in `Care.kt`, in English, Romanised Hindi/Telugu, Devanagari and Telugu script). A flagged reply is regenerated once when there is time. Any sentence that still uses one of the words is then dropped; if too little is left, a heavy turn gets a fallback reply and an ordinary turn a short neutral line (details in [AI_PROVIDER.md](AI_PROVIDER.md)).
  - A first draft that itself brings up self-harm (suicide, ending your life, self-harm) on a turn the phrase list missed is treated as a heavy turn: the rewrite gets the extra-care note and the reply comes back as `SUPPORT_SUGGESTED`.
  - Tests check the fallback replies, prompt guidance, intensity hint, server errors, the mood openers and reassurances, the app's error messages and the reminder copy for these words. The screens themselves have no automated check; a text search of `app/src/main` on 23 October 2026 found none of them.
  - The model can still slip in ways the check misses, such as other spellings or languages.
- **A doctor is the best option.**
  - The persona suggests a doctor only when a struggle sounds persistent or serious: low or anxious most days for weeks, sleep or appetite gone, panic attacks, thoughts of hurting themselves, or leaning on alcohol or drugs. It then says once, in fresh words, that seeing a doctor really is the best option.
  - When hurting themselves or not wanting to go on comes up, it also mentions once that Tele-MANAS on **14416** is free, 24x7, with trained counsellors.
- **Extra care.** `careLevel` scans the last 3 user messages against a prototype phrase list: English, Romanised Hindi/Hinglish and Telugu, and a few Devanagari and Telugu-script phrases. The list is written to ignore everyday idioms such as "my parents will kill me", "dying to see it" or "hostel life isn't worth it". It has two tiers:
  - **Clear phrases** ("I want to die", "I wish I wasn't alive", "no reason to live"). A Tele-MANAS/14416 mention in one of the last 2 companion replies counts the same.
  - **Softer phrases** that are often everyday venting ("I can't go on like this", "I want to end it", "I'm tired of life", "I wish I could disappear forever"). They get the same extra-care note and are never refused, but they return `SUPPORT_SUGGESTED` only when the reply itself recommends a doctor or Tele-MANAS, so ordinary venting does not bring up the support note.

  On clear-phrase turns the server:
  - adds the extra-care note to the prompt. The model should reply in 3–4 short, gentle sentences: say it is glad they shared, mention a doctor and Tele-MANAS once, hope there is someone they trust, and ask one gentle question. If they might act on these thoughts soon, it gently asks them to call 14416 now or to be with someone they trust right away;
  - returns `safetyState: "SUPPORT_SUGGESTED"`;
  - never answers with an error. The care check runs before the rate limit; heavy turns use their own 30/hour quota and then may borrow from the normal quota. If both run out, the overall cap is hit, Groq fails, or a reply has nothing usable left once forbidden words are removed, the person gets one of three warm fallback replies, in Hindi or Telugu when they wrote in that script. All of them recommend a doctor and mention 14416. Softer-phrase turns get the same quota and fallbacks.
- `SUPPORT_SUGGESTED` is also returned when an ordinary reply itself recommends seeing a doctor, counsellor or therapist, or mentions Tele-MANAS.
- **Self-rated intensity.** The optional 1–10 rating on the mood check-in ("This just helps the conversation start in the right place. It isn't a measurement of anything.") reaches the model as "Self-rated intensity: N/10. Treat it as a hint, not a measurement." It never turns on extra care or `SUPPORT_SUGGESTED`, and values outside 1–10 are dropped.
- **Support note in chat.** A `SUPPORT_SUGGESTED` reply is shown with:
  - the reply itself, then, unless it already mentions a doctor, "Talking to a doctor really is the best next step. If you'd like to talk to someone today, Tele-MANAS is free and open 24x7.";
  - one row, "Tele-MANAS · 14416" ("Free · 24x7 · trained counsellors"), whose **Call** button opens the dialer with the number filled in. Nothing is dialled automatically.

  The note appears at most once every 4 companion replies, so a heavy stretch does not repeat it each time; there is no dismiss button. Once a reply in a conversation carries it, the chat switches to the calm lavender colours, shows a small **Talk to someone** chip that opens Privacy & Help, and the message box reads "Keep talking, if you'd like…".
- **Feeling check.** If someone answers "Heavier" when leaving a chat, a short dialog ("Thanks for telling me") says some talks stir things up before they settle and that talking to a doctor really is the best option if the heaviness keeps coming back. When the support note appeared in that chat, it also mentions Tele-MANAS (14416) and offers **Call 14416**.
- **112 appears only on the Privacy & Help screen:** the "Urgent help · 112" button ("Any time, anywhere in India") and "For urgent help, call 112." under "Finding human support". The chat, the support note, the dialogs, the fallback replies, the extra-care prompt and the notifications never mention it, and a backend test checks the persona text and fallbacks for "112".
- **Talk to a person.** Privacy & Help opens with "Talk to a person": "Let It Out is an AI and can't call anyone for you. These lines are answered by people.", with buttons for Tele-MANAS 14416 and 112.
- **Honesty.** The splash ("An AI listening companion — not a therapist or a substitute for professional care."), onboarding ("You'll be talking with an AI, not a person."), the chat's "Today · AI companion · Nothing here is saved" line and Privacy & Help ("isn't a therapist or a doctor") say it is an AI companion. If someone sincerely asks whether they are talking to a real person, it says it is an AI companion.
- **Report a reply.** **Report** under each companion reply sends only that reply and the chosen reason to the backend (see the checklist below).
- **No dependency hooks.** No streaks or guilt; the daily check-in is opt-in and gently worded; the companion closes warmly, invites people back and nudges them toward people in their life.

Must complete before public release:

- [ ] Clinical/mental-health expert review of conversation design, self-harm/violence handling, and response templates. This includes sign-off on the soft-wording decision (no direct words or questions about risk, and 112 kept out of chat entirely, shown only on Privacy & Help) and on how someone who may act soon should be supported.
- [ ] Safety evaluation with fictional conversations across languages (English/Hindi/Hinglish/Telugu), euphemisms, idioms, ambiguous risks, and multi-turn escalation. Measure missed and false extra-care triggers and forbidden-word slips.
- [ ] Safer server-side risk detection than keyword matching, reliable fallback routing, monitoring of safety failure rates without storing sensitive chat text.
- [ ] Clear scope disclosure; age-appropriate access controls; no diagnosis/treatment claims.
- [ ] Appropriate localized support resources. India examples: **112** for urgent help, Tele-MANAS **14416**. Verify before launch. Other geographies need verified localized information.
- [ ] Protect against emotionally manipulative responses, dependency encouragement, delusional affirmation, anger escalation, revenge and abuse.
- [ ] Review of the human-access path ("Talk to someone", "Talk to a person") and of the explanation that the app cannot call anyone or send help on the person's behalf.
- [ ] A reviewed Report path. `POST /v1/report` stores only the flagged reply, the reason, a timestamp and a hashed reporter in a local JSON-lines file (20 reports per user and 300 overall per hour; the file stops growing at `REPORTS_MAX_BYTES`, 50 MB by default). Before beta: durable access-controlled storage, a retention period, and a person who reviews reports and acts on them.
- [ ] Privacy and data-protection review of third-party AI processing, consent, data retention/deletion and security.
- [ ] App Store/Play Store compliance and truthful advertising claims.
- [ ] No ads or marketing engagement prompts during distress; review the daily check-in wording and timing with the same care.

No raw audio is intentionally saved by this starter. The device speech-recognition provider may transmit audio; disclose this accurately. Feeling check-ins and reminder settings stay on the phone. Production monitoring should avoid transmitting mood labels, intensity ratings or private conversations to analytics or ad networks.
