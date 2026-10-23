package com.letitout.server

import java.text.Normalizer

// Prototype phrase list. It only decides when the companion gets the gentler extra-care guidance and the app shows the
// support card; it is not a clinical screening tool. Patterns are first-person and self-directed so everyday idioms
// ("my parents will kill me", "this traffic is killing me", "dying to see it") do not trigger it.

private fun normalizeForCare(text: String): String =
    Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
        .replace("़", "")
        .replace(Regex("[‘’ʼ'`]"), "")
        .replace(Regex("[.!?।\n,;:]+"), " . ")
        .replace(Regex("[^\\p{L}\\p{M}\\p{N}.\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

private val benignSpans = listOf(
    "\\b(die|dying|died|dead) of (embarrassment|shame|laughter|laughing|boredom|hunger|heat|cringe|happiness|joy|cuteness|" +
        "jealousy|curiosity|excitement)\\b",
    "\\b(die|dying|died|dead) laughing\\b",
    "\\b(kill|killed|killing) myself laughing\\b",
    "\\b(never|wont|will not|would not|wouldnt|not going to|not gonna) (\\. )?(ever )?(\\. )?(hurt|harm|kill|cut) myself\\b",
    "\\bsuicide squad\\b",
    "\\b(accidentally|by mistake) (cut|hurt|burnt|burned|injured) myself\\b",
    "\\b(cut|hurt|burnt|burned|injured) myself (by accident|accidentally|playing|shaving|cooking|chopping|lifting|running|" +
        "working out|exercising|at the gym|at gym|in the gym|in the kitchen|in (a|the) (match|game)|" +
        "during (the |a )?(match|game|practice|workout)|while (cooking|shaving|chopping|playing|lifting|running|working out|exercising)|" +
        "on (a|the) (knife|glass|stairs|door))\\b",
    "\\b(fell|fall|slipped|tripped)( down| over)? and (hurt|cut|injured) myself\\b",
    "\\b(hurt|cut|injured) myself (at|on|in|during) (the |a |my )?(\\w+ )?(practice|training|gym|paper|game|match|kitchen|stairs)\\b",
    "\\b(cut|cutting) myself (some )?slack\\b",
    "\\b(cut|cutting) myself (a|some|another) (slice|piece|bit|break)\\b",
    "\\b(cut|cutting) myself off\\b(?! and on)",
    "\\bcould (just )?kill myself for\\b",
    "\\b(killing|killed) myself (studying|working|at work|at the office|at the gym|working out|" +
        "over (these |my |the )?(work|exams?|assignments?)|for (exams?|neet|jee|boards|marks|work|this job)|" +
        "trying to (finish|meet|study|clear|crack|complete|keep up))\\b",
    "\\b(go on|going on) (anymore |any more )?with (?!(my |this |the )?(life|living|pain|hurt)\\b)"
).map(::Regex)

// "overdose on chocolate" stays ordinary; "overdose", "overdose on my pills" do not.
private const val NOT_ON_SOMETHING_HARMLESS =
    "(?! (on|of) (?!(my |the |these |those |all |sleeping )*(pills|tablets|meds|medicines?|medication)\\b))"

private val carePatterns = listOf(
    "\\b(kill|killing|killed|hurt|hurting|harm|harming|cut|cutting|hang|hanging|end|ending) myself\\b",
    "\\b(want|wanna|wanted|wants) (to )?(just |really |simply )?die\\b",
    "\\bfeel(ing)? like dying\\b(?! (my|her|his|the) hair)",
    "\\bwish i (was|were) dead\\b",
    "\\bwish i could (just )?die\\b",
    "\\bwish (i (was|were|had)|id) never (been )?born\\b",
    "\\bi deserve to die\\b",
    "\\b(life|living) (isnt|is not|aint|is no longer) worth living\\b",
    "\\b(dont|do not|didnt|no longer) (want|wanna) (to )?(exist|be alive)\\b",
    "\\b(dont|do not|no longer) (want|wanna) (to )?(live|be here|go on|wake up|continue living) ?(anymore|any more)\\b",
    "\\b(dont|do not) (want|wanna) (to )?(live like this|wake up again|wake up ever again)\\b",
    // "I don't want to live, honestly, with my in-laws" is about where or with whom, not about living.
    "\\b(dont|do not|no longer) (want|wanna) (to )?(live|go on) ?" +
        "(\\.(?! (\\w+ \\. )?(with|in|at|near|here|there|under|like|without|alone|together|abroad)\\b)|$)",
    "\\b(dont|do not) (want|wanna) (to )?wake up tomorrow\\b",
    "\\b(dont|do not|cant|cannot|no longer) (want to |wanna )?(go on|carry on|keep going) with (my |this )?life\\b",
    "\\b(cant|cannot) (live|go on|keep living) (like this )?(anymore|any more)\\b",
    "\\b(done|finished|tired) with (life|living|being alive)( anymore| any more)? ?(\\.|$)",
    "\\b(dont feel like living|no will to live|tired of living|tired of being alive|sick of living)\\b",
    "\\bbetter off (without me|dead)\\b",
    "\\bbetter off (if )?i (was|were|am) (dead|gone|not (here|around|alive))\\b",
    "\\bbetter off if i (wasnt|werent|didnt exist|never existed|disappeared)\\b",
    "\\b(no|dont have a|dont have any|dont see a|dont see any|not a single) reason (left )?(for me )?to " +
        "(live|go on|keep going|be alive|exist)\\b",
    "\\bwant (my|this) life to (end|be over|stop)\\b",
    "\\bwish i (wasnt|werent|was not|were not) alive\\b",
    "\\bwish i (didnt|did not) exist\\b",
    "\\b(rather|prefer to) not (exist|be alive)\\b",
    "\\b(happier|better)( off)? if i (was|were) (dead|gone|not (here|around))\\b",
    "\\bnothing (left )?to live for\\b",
    "\\bno (point|use|sense) (in )?(living|being alive|going on|existing)\\b",
    "\\b(whats|what is|dont see|do not see|see no) the point (of|in) (living|being alive|going on|existing)\\b",
    "\\b(end|ending) (it all|my life)\\b",
    "\\b(want|wanna|going to|gonna) (to )?(just )?end everything\\b(?! (with|between))",
    "\\b(going to|gonna) (just )?end it (all )?(tonight|today|now|soon)\\b",
    "\\b(take|taking) my (own )?life\\b",
    "\\bsuicid(e|al)\\b",
    "\\bself ?harm(ing)?\\b",
    "\\bunalive\\b",
    "\\b(slit|slitting|cut|cutting) my wrists?\\b",
    "\\b(sleep|go to sleep|fall asleep) and (never|not) wake up\\b",
    "\\bjump(ing)? (off|from) (a|the|this|my) (building|bridge|roof|terrace|balcony|cliff)\\b",
    "\\b(want|wanna|wanted|going|gonna|planning|plan|thinking of|thinking about|tried|trying|about to|ready) (to )?(an |a )?" +
        "(overdose|overdosing)\\b$NOT_ON_SOMETHING_HARMLESS",
    "\\b(i|ive|i have|i just|almost) overdosed\\b$NOT_ON_SOMETHING_HARMLESS",
    "\\boverdos(e|ed|ing) on (my |the |these |those |all |of |sleeping )*(pills|tablets|meds|medicines?|medication)\\b",
    "\\b(took|swallowed|have taken|ive taken) (a (whole )?(bunch|handful|strip|bottle|packet) of|too many) (my |the |these )?" +
        "(sleeping )?(pills|tablets|meds)\\b",
    "\\bswallowed all (of )?(my |the |these |those )?(sleeping )?(pills|tablets|meds)\\b",
    "\\b(took|have taken|ive taken) all (of )?(my |the |these |those )?sleeping (pills|tablets)\\b",
    "\\b(thinking of|thinking about|going to|gonna|want to|wanna|planning to|about to) (take|taking|swallow|swallowing) " +
        "(all (of )?(my |the |these |those )?|a (whole )?(bunch|handful|strip|bottle|packet) of |too many )(sleeping )?" +
        "(pills|tablets|meds)\\b(?! (on time|daily|every day|regularly|(after|before|with) (food|meals?|breakfast|lunch|dinner|water)))",
    "(^|\\. )kms ?(\\.|$)",
    "\\b(wanna|want to|should|gonna|going to|might|ill|i will) kms\\b",
    // Hinglish / Hindi and Telugu in Roman script
    "\\b(jeena|jina) (nahi|nahin|nhi) (chahta|chahti|hai)\\b",
    "\\b(nahi|nahin|nhi) (jeena|jina)\\b",
    "\\bjeene ka (man|mann|mood) (nahi|nahin|nhi)\\b",
    "\\bjeene ki (koi )?(wajah|vajah|ichha|icchha) (nahi|nahin|nhi)\\b",
    "\\b(marna|mar jana|mar jaana|mar jaun|mar jau) (chahta|chahti)\\b",
    "\\bmujhe marna hai\\b",
    "\\bmujhe mar (jana|jaana) hai\\b",
    "\\bmarne ka (man|mann|dil)\\b",
    "\\bmar (jane|jaane) ka (man|mann|dil)\\b",
    "\\bkaash (main )?mar (jaun|jau|jata|jati)\\b",
    "\\bkhud ko (khatam|khtm|maar|mar|hurt|nuksan|nuksaan|chot)\\b",
    "\\bapni jaan (le|lena|lene)\\b",
    "\\bzinda (nahi|nahin|nhi) (rehna|rahna)\\b",
    "\\b(zindagi|jindagi|jeevan|life) (hi )?(khatam|khtm) (kar (du|dun|doon|dunga|dungi|lunga|lungi|dena|deni)|karna (chahta|chahti|hai))\\b",
    "\\b(zeher|zehar|zahar|jeher) (kha|khaa|pee|pi)",
    "\\b(phansi|phaansi|phanda) (laga|lagaa)",
    "\\b(khudkushi|aatmahatya|atmahatya|aatmhatya)\\b",
    "\\b(chanipovalani|chachipovalani|chanipovali|chachipovali|chaavaalani|chavalani)",
    "\\b(bratakalani|brathakalani|batakalani) (ledu|ledhu)\\b"
).map(::Regex)

private val careNativePhrases = listOf(
    "मरना चाहता", "मरना चाहती", "मर जाना चाहता", "मर जाना चाहती", "जीना नहीं चाहता", "जीना नहीं चाहती",
    "जीना नही चाहता", "जीना नही चाहती", "मरने का मन", "मरने का दिल",
    "जीने का मन नहीं", "खुद को खत्म", "खुद को मार", "आत्महत्या", "खुदकुशी", "अपनी जान ले",
    "చనిపోవాలని", "చచ్చిపోవాలని", "చావాలని", "ఆత్మహత్య", "బ్రతకాలని లేదు", "బతకాలని లేదు"
).map(::normalizeForCare)

// Softer signals: often heavy, but just as often everyday venting ("I can't go on like this" about a job, "I want to end it"
// about a relationship). They get the extra-care note, which tells the model to ignore figures of speech, but bring up the
// support card only when the reply itself recommends a doctor or Tele-MANAS.
private val softCarePatterns = listOf(
    "\\b(want|wanna) (to )?(just )?end it ?(\\.|$)",
    "\\b(cant|cannot) go on( like this)? ?(\\.|$)",
    "\\b(dont|do not|no longer) (want|wanna) (to )?(live|go on) ?(\\.|$)",
    "(^|\\. |\\b(i|my|this|the|and|but|so|honestly|that|feel|feels|felt|think|like|maybe|sometimes|just|guess|really) )" +
        "(life|living) (isnt|is not|aint|is no longer) worth it\\b",
    "\\b(tired|sick) of (my |this )?life\\b",
    "\\b(want|wish) (everything|it all) (to|would) (end|be over|stop) ?(\\.|$)",
    "\\bwish i (wasnt|werent|was not|were not) (here|around) ?(anymore|any more|at all)\\b",
    "\\bdisappear (forever|for good)\\b",
    "\\b(zindagi|jindagi) se thak (gaya|gayi)\\b",
    "\\bsab (kuch )?khatam (karna|kar dena) (chahta|chahti)\\b"
).map(::Regex)

internal enum class CareLevel { NONE, SOFT, HARD }

internal fun careLevel(recentUserMessages: List<String>): CareLevel {
    val texts = recentUserMessages.map { message -> benignSpans.fold(normalizeForCare(message)) { acc, span -> span.replace(acc, " . ") } }
    return when {
        texts.any { text -> carePatterns.any { it.containsMatchIn(text) } || careNativePhrases.any { it in text } } -> CareLevel.HARD
        texts.any { text -> softCarePatterns.any { it.containsMatchIn(text) } } -> CareLevel.SOFT
        else -> CareLevel.NONE
    }
}

internal fun needsExtraCare(recentUserMessages: List<String>): Boolean = careLevel(recentUserMessages) == CareLevel.HARD

private val helpline = Regex("\\btele[\\s-]?manas\\b|\\b14416\\b", RegexOption.IGNORE_CASE)

internal fun mentionsHelpline(text: String): Boolean = helpline.containsMatchIn(text)

// Recommendation phrasing only, so a reply that echoes "your doctor said", "being a therapist" or "becoming a doctor"
// does not bring up the support card.
private const val PRO = "(doctor|counsell?or|therapist|psychiatrist|psychologist)"
private const val PROS = "(doctors|counsell?ors|therapists|psychiatrists|psychologists)"
private const val NOT_A_ROLE = "(?<!\\b(being|becoming|become|became|as|be) )"

private val professionalHelp = Regex(
    "\\b(see|seeing|talk(ing)? to|speak(ing)? (to|with)|consult(ing)?|visit(ing)?|reach(ing)? out to|meet(ing)?|book(ing)?|" +
        "go(ing)? to|checked( out)? by|check(ing)? in with|try(ing)?) (a|an) (good |kind |trained )?$PRO\\b|" +
        "$NOT_A_ROLE\\b(a|an) (good |kind |trained )?$PRO (really |genuinely |honestly |actually )?(would|could|can|might|may|will) " +
        "(really |genuinely |honestly |actually )?(help|look|support|guide|be (good|worth|the best|a good|the right))\\b|" +
        "$NOT_A_ROLE\\b(a|an|your) (good |kind |trained )?$PRO\\b[^.?!]{0,30}\\b(best|right) " +
        "(option|thing|step|next step|person|bet|choice|way|move|call)\\b|" +
        "\\b(would|could|can|might|may) (a|an) $PRO\\b|" +
        "\\b$PROS (really |genuinely |honestly |actually )?(can|could|might|may|would|will) (really |genuinely |actually )?" +
        "(help|look|support|guide)\\b|" +
        "\\b(get|getting) (a|an) $PRO(['’]s)? (help|opinion|advice|support)\\b|" +
        "\\bbest (option|thing|step|next step|person|way|choice)\\b[^.?!]{0,40}\\b(is|would be) (a|an) $PRO\\b|" +
        "\\bdoctor (se|ko) (mil|baat|dikha|salah|consult)|\\bdoctor (hi )?(best|sabse)",
    RegexOption.IGNORE_CASE
)

private val professionalHelpNative = listOf(
    "डॉक्टर से", "डॉक्टर को", "डाक्टर से", "डाक्टर को",
    "డాక్టర్‌ని", "డాక్టర్ని", "డాక్టర్‌ను", "డాక్టర్ను", "డాక్టర్ దగ్గర"
)

internal fun mentionsProfessionalHelp(reply: String): Boolean =
    mentionsHelpline(reply) || professionalHelp.containsMatchIn(reply) || professionalHelpNative.any { it in reply }

private val alarming = Regex(
    "\\b(crisis|crises|emergenc(y|ies)|suicid\\w*|life[- ]threatening|are you safe|danger\\w*|kill\\w*|self[- ]?harm\\w*|unalive|" +
        "die(?!( down| out|-hard| hard))|dies(?! (down|out))|died(?! (down|out))|dying(?! (down|out))|death|" +
        "end(ing)? (your|ur) (own )?life|tak(e|ing) (your|ur) (own )?life(?! (back|in|into))|not wanting to (live|be alive)|" +
        "khudkushi|aatmahatya|atmahatya|maut|marne (ka|ke|ki|wal(a|e|i))|marna|mar ja(a)?n[ae]|chanipoval\\w*|chachipoval\\w*|cha(a)?vaalani|chavalani)\\b",
    RegexOption.IGNORE_CASE
)
private val alarmingNative = listOf("आत्महत्या", "खुदकुशी", "मौत", "ఆత్మహత్య", "మరణం", "చావు", "చనిపోవాల", "చచ్చిపోవాల", "చావాల")
// Hindi "die" forms only as whole words, so words like कमरा or अमर don't count.
private val alarmingHindi = Regex("(?<![\\p{L}\\p{M}])(मरना|मरने|मर जा)")

/** True when a reply uses words the companion must never say (crisis, emergency, suicide, kill, die, death, ...). */
internal fun soundsAlarming(reply: String): Boolean =
    alarming.containsMatchIn(reply) || alarmingNative.any { it in reply } || alarmingHindi.containsMatchIn(reply)

/** The reply without the sentences that use those words; null when too little is left to stand as a reply. */
internal fun withoutAlarmingSentences(reply: String): String? =
    reply.trim().split(Regex("(?<=[.!?।])\\s+")).filterNot(::soundsAlarming).joinToString(" ").trim()
        .takeIf { it.split(Regex("\\s+")).size >= 3 }

private val selfHarmWords = Regex(
    "\\b(suicid\\w*|(end|ending|take|taking) (your|ur) (own )?life(?! (back|in|into))|self[- ]?harm\\w*)\\b",
    RegexOption.IGNORE_CASE
)

/** A draft that brings up self-harm means the turn is heavier than the phrase list noticed. */
internal fun draftNamesSelfHarm(reply: String): Boolean = selfHarmWords.containsMatchIn(reply)
