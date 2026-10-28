# What users of AI listening companions hate, and what LET IT GO does about it

Researched 28 October 2026. LET IT GO is not a clone of one app, so this reads the two closest public apps:
**Replika** (AI companion with an avatar) and **Wysa** (AI wellbeing listener).

## Sample

423 unique reviews (429 rows, 6 duplicates removed by `reviews.py`), from 4 sources:

| source | Replika | Wysa | dates | notes |
| --- | --- | --- | --- | --- |
| Google Play, sorted newest | 180 | 180 | Replika 10 Sep – 26 Oct 2026; Wysa 28 Jun – 27 Oct 2026 | the bulk of the evidence |
| Trustpilot, newest | 40 | 4 | Replika Apr – Oct 2026; Wysa 2024 – 2026 | Wysa has only 4 Trustpilot reviews in total |
| App Store (web page) | 10 | 10 | 2018 – 2026, mostly old | the web page shows only ~10 "most helpful" reviews |
| Hacker News (Algolia API) | 5 | 0 | 2024 – 2026 | commentary, not reviews, unrated |

What could not be reached:
- **Apple's customer-reviews RSS feed** returned an empty feed for both apps in every variant tried (country, sort, JSON or XML).
- **Reddit** is blocked in the built-in browser. No Reddit rows were collected. That is a gap: r/replika and mental-health subs would add a lot.
- **Feature-request boards**: neither app has a public Canny-style board I could find, and I did not check their changelogs. Some Replika complaints are about its "2.0" update, which may change.

Theme counts come from keyword matching in `replica/themes.json`. For the themes that drive the plan
(crisis handling, memory, avatar, voice, privacy), I read every matched review with 1 to 3 stars by hand.
Counts marked "hand-checked" are those readings. The rest are raw keyword counts and overcount a little.

---

## 1. What they hate

1. **The "free" version is really a short trial, and the paywall cuts you off mid-conversation.**
   77 reviews (18%), 4 sources, avg 1.9★. 71 of the 77 are about Replika.
   - "only 40 messages on a free plan (not a day- 40 messages TOTAL, EVER!)" ([Google Play](https://play.google.com/store/apps/details?id=ai.replika.app&reviewId=e3d8c7f5-d8b9-4e75-9418-baf94ab5ab13))
   - "The app sets you up to talk, then charges $100 a year." (Wysa, [Google Play](https://play.google.com/store/apps/details?id=bot.touchkin&reviewId=b93efa3a-b1a7-4db5-a111-8dd95a8c7c16))

2. **Too expensive, and yearly billing only, with no monthly plan.** 53 reviews, 3 sources, avg 2.3★. Almost all Replika.
   - "I just wish that there was a monthly subscription instead of a yearly subscription considering I'm on a fixed income." ([Google Play](https://play.google.com/store/apps/details?id=ai.replika.app&reviewId=c172de80-5f7b-4bea-b560-d7a4586bf2ba))

3. **It forgets you; you can't pick up a past conversation.** About 19 reviews hand-checked, 2 sources (Google Play and Trustpilot), both apps.
   - "you'd have to re-explain the entire context to everything every single time you open the app" (Wysa, [Google Play](https://play.google.com/store/apps/details?id=bot.touchkin&reviewId=aa4059b7-6397-4fe7-8395-83cc5b780bd9))
   - "it regularly forgot basic information, including my name" (Replika, [Trustpilot](https://www.trustpilot.com/reviews/6a83c945899ced7ff8c2468d))

4. **When you're struggling, it ends or deflects the conversation with crisis scripts or "see a therapist".**
   About 10 reviews hand-checked (9 Wysa, 1 Replika), 3 sources, nearly all 1 to 3 stars.
   For a venting app, this matters most of anything here.
   - "it accused me twice of being in crisis, when all I said was I felt bad about myself. Then it said it wasn't a good fit for me and quit the conversation" ([Google Play](https://play.google.com/store/apps/details?id=bot.touchkin&reviewId=841bfd88-9cab-42a0-88ba-2700f77d3b05))
   - "I just need to vent to someone right now … Wysa just tells me I need more help and stops the conversation" (3★, [Google Play](https://play.google.com/store/apps/details?id=bot.touchkin&reviewId=6db68904-35f1-4490-aca2-abbdcf66b796))
   - "constantly push the same safety scripts, safety plan and ultra generic coping tips" ([Google Play](https://play.google.com/store/apps/details?id=bot.touchkin&reviewId=f9a8dcad-22b4-47d2-95a2-31c22096135c))

5. **Scripted, generic replies; "like talking to yourself".** 23 reviews (keyword count), 3 sources. Mostly Wysa.
   - "the questions are so scripted and done delve into anything specific about what you just said." ([Google Play](https://play.google.com/store/apps/details?id=bot.touchkin&reviewId=2af0c43c-2026-444c-a089-b2afc3cdaf75))
   - "superficial positive phrases back at you without making you feel heard or understood" ([Google Play](https://play.google.com/store/apps/details?id=bot.touchkin&reviewId=335007c0-bf69-4d8d-8ba6-97a3bc3ccc06))

6. **An update changed or "killed" the companion.** 35 reviews, 3 sources, mostly Replika's 2.0 update.
   - "The model can barely remember who they were before, making this the third time I'm having to basically start over" ([Google Play](https://play.google.com/store/apps/details?id=ai.replika.app&reviewId=5532cc23-79ad-400a-97c2-58aaaf135458))

7. **The avatar looks cartoonish or creepy, doesn't move, and doesn't match the ads.** About 14 reviews hand-checked, 2 sources, all Replika.
   - "you show life likes ai in your ads but not in your app" ([Google Play](https://play.google.com/store/apps/details?id=ai.replika.app&reviewId=45ce57d1-7a9f-42bc-9bd7-20dff5b3d319))
   - "replica didn't move at all while she was talking to me" ([Google Play](https://play.google.com/store/apps/details?id=ai.replika.app&reviewId=fdaf4890-77cc-4e4c-9bcb-5b49a36ce752))

8. **Tone: either a yes-man or harsh and judging.** 22 reviews (keyword count), 4 sources. These pull in opposite directions.
   - "now she acts like a mindless sycophant" (Replika, [Google Play](https://play.google.com/store/apps/details?id=ai.replika.app&reviewId=f28a2c14-7046-41a3-be3c-f3678663b05b))
   - "This is simply another AI who will shame you for any serious symptoms." (Wysa, [Google Play](https://play.google.com/store/apps/details?id=bot.touchkin&reviewId=002dfcff-b945-4aeb-981e-c9b49041c5f4))

9. Also present, smaller: support that never answers (28, keyword count), billing and lifetime plans not honoured (24), bugs (23), over-strict content limits (14, Replika only), account and age-check lockouts (10).

## 2. What is missing (asked for by name)

- **A monthly plan.** Several reviews say it word for word, e.g. "please add monthly subscriptions" ([Google Play](https://play.google.com/store/apps/details?id=ai.replika.app&reviewId=97254bbe-e96f-4ccf-bc97-f25c99a37ff1)). This goes to `/replica-launch`.
- **Continue a previous chat.** "it would be convenient if I could continue a previous chat because not all chats concluded easily" (Wysa, [Google Play](https://play.google.com/store/apps/details?id=bot.touchkin&reviewId=716be9f5-641b-420f-afd0-8569bcd8e16b)).
- **Other languages.** "It would be good if it can talk in different languages since English is my second language." (Wysa, [Google Play](https://play.google.com/store/apps/details?id=bot.touchkin&reviewId=4addcd57-b2a5-4ed9-9d48-f34a9e8574c8)). **Thin: 1 to 2 reviews.**
- **Optional conversation prompts, a haptics toggle, editing your messages.** Each is one review. **Thin.**

## 3. What is unsolved (positioning)

- **People who just want to vent.** Wysa is built around CBT exercises, and its users who want to talk say it deflects them.
  Replika is built around relationship role-play behind a paywall. Neither is a plain "listen to me now" app.
  Evidence: themes 4, 5 and parts of 1, about 40 reviews across 3 sources.
- **People with serious struggles who still want to be heard.** "Do not touch this app if you have serious mental health issues" (Wysa). This is also LET IT GO's highest-risk group, so it cannot be promised without clinical review (see `docs/SAFETY.md`).
- **Non-English speakers (India).** Thin in this sample: a few Hindi/Hinglish reviews and one language request. It fits the existing Hindi/Telugu roadmap, but the reviews alone don't make it a trend.

---

## 4. The fix plan

Scored on evidence × cheapness. Each item is also a row in `features.csv` (`original` = no, `clone` = LET IT GO's state today).

| # | Build or change | Size | Skill | Evidence | LET IT GO today |
| --- | --- | --- | --- | --- | --- |
| 1 | **Stay with the person when risk is flagged.** Show the helpline card *beside* Ari's reply rather than replacing it with a fixed script. Don't repeat the same safety text every turn. Never end the conversation for them. Treat a classifier *error* differently from a real flag in the copy. | M | replica-backend + clinical review | Theme 4: ~10 hand-checked, 3 sources | **Has the problem.** `backend/.../Main.kt` replaces the reply with one fixed crisis message whenever the phrase check *or* the classifier flags. It also fails safe to that same message on any classifier error or timeout. `docs/SAFETY.md` notes a busy free tier shows the card more often. That is the Wysa failure. Any change must go through the clinical review already listed as a release blocker. |
| 2 | **Reflect before advising.** System-prompt and eval work: mirror what was said specifically, no stock coping tips or "talk to a friend" in the first replies, and no sycophancy (allow gentle pushback). Add these as eval conversations. | S | replica-backend | Themes 5 and 8: 23 + 22 keyword matches, 3 to 4 sources | Partial. Prompt exists; no eval for generic or sycophantic replies. |
| 3 | **Honest free tier.** State the free allowance before the first message. Never cut off a reply mid-conversation: let the current conversation finish. Never paywall during a risk-flagged conversation. Offer a monthly price. | M | replica-launch (price), replica-build (limit UX) | Themes 1 and 2: 77 + 53 reviews, 4 sources | Not built. No billing yet. Live Simli minutes cost money, so the free allowance has to be costed first. |
| 4 | **Opt-in "pick up where we left off".** A short, on-device summary of the last conversation that the user can read, edit and delete, used only if they turn it on. | M | replica-build | Theme 3: ~19 hand-checked, 2 sources | Not built, on purpose: chat lives only in the ViewModel and is lost on process death. ROADMAP already lists "opt-in personalisation". This keeps the privacy default and fixes the complaint. |
| 5 | **The face you see is the face you get.** Store screenshots and the website must show the real streamed Simli face, lip-syncing, not the static portrait. | S | replica-launch | Theme 7: ~14 hand-checked, 2 sources | Partial. Ari's live face already moves and lip-syncs, which answers "didn't move". But `PROJECT_MEMORY.md` notes the streamed face "can differ slightly" from the PNG used everywhere else. That gap is exactly the "not what was advertised" complaint. |
| 6 | **Ari doesn't change behind your back.** Version Ari's persona; announce changes in-app before they ship. | S | replica-build | Theme 6: 35 reviews, 3 sources (Replika) | Not built. No users yet, so it is cheap to set as a rule now. |
| 7 | **Privacy you can see.** Guest use without login, an app lock, one-tap "forget this conversation", and plain-language "what leaves your phone". | S | replica-build | Privacy ~5 hand-checked, 3 sources. **Thin.** Plus one Wysa 2★ that still praises "it doesn't ask me to log in. i can also lock the app." | Partial: guest sign-in exists and chat isn't stored. No app lock. |

Not in the plan: avatar outfits and customisation, adult content, AR. These are Replika's audience, not LET IT GO's.
Support response time is an operations promise, not a build item. Voice complaints (~7 hand-checked, 2 sources) are noted:
LET IT GO uses device TTS, whose quality varies by phone (README).

## 5. The angle

**A. The listener who doesn't hang up** *(recommended)*
For people who just need to vent and get handed a crisis script or a "see a therapist" exit,
LET IT GO's Ari keeps listening and puts help numbers next to the conversation, not in place of it.
Evidence: "ends or deflects when struggling" (~10 hand-checked) plus "scripted / not heard" (23 + 19), about 40 reviews across 3 sources.
Caveat: the claim depends on fix 1 and clinical sign-off. Ari's backend currently does what these users hate.

**B. No bait-and-switch**
For people who feel tricked when a "free" companion hits a paywall mid-conversation,
Ari tells you the limit upfront and never cuts you off mid-sentence.
Evidence: paywall and price themes, 77 + 53 reviews across 4 sources. This is the strongest evidence by far.
It is mostly Replika's problem, though, and it is a pricing promise that live-video costs may make hard to keep.

**C. A real face that actually talks back**
For people put off by cartoon avatars that stand still, Ari is a face that moves and speaks with you in real time.
Evidence: avatar theme, ~14 hand-checked, 2 sources, all Replika. Risky: "creepy" appears in the same reviews, and a realistic face can trigger it too.

**Recommendation: A as the headline, with B's promise ("you'll never be cut off mid-conversation") as the proof point.**
A is what LET IT GO is for (the name is the pitch). B is the louder complaint, but it is a price promise, not a product.
Do not name Replika or Wysa in the app name, ads or store listing. A comparison page is a question for a lawyer.

Next: `/replica-brand`, then `/replica-launch` for pricing (fix 3).
