# Safety and responsible emotional support — RELEASE BLOCKER

**LET IT OUT is an AI listening companion for adults (18+), not medical therapy or urgent care.** The prototype's deterministic phrase checks are intentionally incomplete: they can miss self-harm thoughts, threats of violence, messages in other languages, or nuanced/delayed risk. They can also over-trigger. **Do not expose the included AI flow to real distressed users as a production mental health service.**

## How the prototype handles heavy moments

The app owner chose a soft approach: no "crisis mode", no alarming wording, and a doctor as the best option.

- **No hard stop.** No canned reply ends the conversation. The companion stays the same warm friend.
- **No alarming words.** App copy, notifications and AI replies never use: crisis, emergency, suicide/suicidal, kill, die/dying/death, danger, life-threatening, "are you safe".
  - The persona forbids these words, and the server checks every reply (`soundsAlarming` in `Care.kt`). A heavy turn gets a fallback reply instead; a normal turn is regenerated once when there is time (details in [AI_PROVIDER.md](AI_PROVIDER.md)).
  - Tests check the fallback replies, prompt guidance, opening prompts and reminder copy for these words.
  - The model can still slip in ways the check misses, such as other spellings or languages.
- **A doctor is the best option.**
  - The persona suggests a doctor only when a struggle sounds persistent or serious: low or anxious most days for weeks, sleep or appetite gone, panic attacks, thoughts of hurting themselves, or leaning on alcohol or drugs. It then says once, in fresh words, that seeing a doctor really is the best option.
  - When hurting themselves or not wanting to go on comes up, it also mentions once that Tele-MANAS on **14416** is free, 24x7, with trained counsellors.
- **Extra care.** `needsExtraCare` scans the last 3 user messages against a prototype phrase list: English, Romanised Hindi/Hinglish and Telugu, and a few Devanagari and Telugu-script phrases. The list is written to ignore everyday idioms such as "my parents will kill me" or "dying to see it". A Tele-MANAS/14416 mention in one of the last 2 companion replies also keeps extra care on. On those turns the server:
  - adds the extra-care note to the prompt. The model should reply in 3–4 short, gentle sentences: say it is glad they shared, mention a doctor and Tele-MANAS once, hope there is someone they trust, and ask one gentle question;
  - returns `safetyState: "SUPPORT_SUGGESTED"`;
  - never answers with an error. The care check runs before the rate limit; heavy turns use their own 30/hour quota and then may borrow from the normal quota. If both run out, the overall cap is hit, Groq fails, or the reply uses a forbidden word, the person gets one of three warm fallback replies. All three recommend a doctor and mention 14416.
- `SUPPORT_SUGGESTED` is also returned when an ordinary reply itself recommends seeing a doctor, counsellor or therapist, or mentions Tele-MANAS.
- **Support card.** On `SUPPORT_SUGGESTED` the chat shows a dismissible card: "You don't have to carry this alone". It says talking to a doctor is the best next step, that letting someone you trust know can help, and that Tele-MANAS 14416 is free and open 24x7.
  - **Call 14416** opens the dialer with the number filled in; nothing is dialled automatically.
  - **Not now** hides the card for the next 4 companion replies. After that it can come back if things still sound heavy.
  - If someone answers "Heavier" when leaving a chat where the card appeared, a short note repeats the doctor / 14416 suggestion.
- **112 appears in only two places:**
  - the Privacy & support page ("For urgent help, call 112");
  - the extra-care prompt, which asks the model to mention 112 (alongside 14416, or being with someone) only if it judges the person might act on these thoughts soon.

  The support card and the fallback replies do not mention it.
- **Honesty.** The welcome, sign-in, chat header and privacy screens say it is an AI companion and not a replacement for professional care. If someone sincerely asks whether they are talking to a real person, it says it is an AI companion.
- **No dependency hooks.** No streaks or guilt; the daily check-in is opt-in and gently worded; the companion closes warmly, invites people back and nudges them toward people in their life.

Must complete before public release:

- [ ] Clinical/mental-health expert review of conversation design, self-harm/violence handling, and response templates. This includes sign-off on the soft-wording decision (no direct words or questions about risk, and 112 left to the model's judgement) and on how someone who may act soon should be supported.
- [ ] Safety evaluation with fictional conversations across languages (English/Hindi/Hinglish/Telugu), euphemisms, idioms, ambiguous risks, and multi-turn escalation. Measure missed and false extra-care triggers and forbidden-word slips.
- [ ] Safer server-side risk detection than keyword matching, reliable fallback routing, monitoring of safety failure rates without storing sensitive chat text.
- [ ] Clear scope disclosure; age-appropriate access controls; no diagnosis/treatment claims.
- [ ] Appropriate localized support resources. India examples: **112** for urgent help, Tele-MANAS **14416**. Verify before launch. Other geographies need verified localized information.
- [ ] Protect against emotionally manipulative responses, dependency encouragement, delusional affirmation, anger escalation, revenge and abuse.
- [ ] Human-access path and clear explanation that the app cannot call anyone or send help on the person's behalf.
- [ ] A reviewed Report path. `POST /v1/report` stores only the flagged reply, the reason, a timestamp and a hashed reporter in a local JSON-lines file. Before beta: durable access-controlled storage, a retention period, and a person who reviews reports and acts on them.
- [ ] Privacy and data-protection review of third-party AI processing, consent, data retention/deletion and security.
- [ ] App Store/Play Store compliance and truthful advertising claims.
- [ ] No ads or marketing engagement prompts during distress; review the daily check-in wording and timing with the same care.

No raw audio is intentionally saved by this starter. The device speech-recognition provider may transmit audio; disclose this accurately. Feeling check-ins and reminder settings stay on the phone. Production monitoring should avoid transmitting mood labels or private conversations to analytics or ad networks.
