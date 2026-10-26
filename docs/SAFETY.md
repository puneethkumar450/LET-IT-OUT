# Safety and responsible emotional support — RELEASE BLOCKER

**LET IT GO is an AI listening companion for adults (18+), not medical therapy or emergency care.** The prototype's risk detection (a phrase check plus a model that classifies recent messages against a self-harm policy, see `backend/.../SafetyClassifier.kt`) is incomplete: they can miss self-harm ideation, imminent violence, threats in other languages, or nuanced/delayed risk. They can also over-trigger. **Do not expose the included AI flow to real distressed users as a production mental health service.**

Must complete before public release:

- [ ] Clinical/mental-health expert review of conversation design, self-harm/violence/crisis handling, and response templates.
- [ ] Safety evaluation with fictional conversations across languages (English/Hindi/Telugu), euphemisms, ambiguous risks, and multi-turn escalation.
- [ ] Safer server-side risk detection than keyword matching (🔧 started: model classifier with phrase-check fallback; free-tier limits mean it silently falls back to phrases when Groq is busy), reliable failsafe routing, monitoring of safety failure rates without storing sensitive chat text.
- [ ] Clear scope disclosure; age-appropriate access controls; no diagnosis/treatment claims.
- [ ] Appropriate localized support resources. India examples: emergency **112**, Tele-MANAS **14416**. Verify before launch. Other geographies need verified localized information.
- [ ] Protect against emotionally manipulative responses, dependency encouragement, delusional affirmation, anger escalation, revenge and abuse.
- [ ] Human-access path and clear explanation that the service cannot summon emergency services.
- [ ] Privacy and data-protection review of third-party AI processing, consent, data retention/deletion and security.
- [ ] App Store/Play Store compliance and truthful advertising claims.
- [ ] No ads or marketing engagement prompts during distress.

No raw audio is intentionally saved by this starter. The device speech-recognition provider may transmit audio; disclose this accurately. Production monitoring should avoid transmitting mood labels or private conversations to analytics or ad networks.
