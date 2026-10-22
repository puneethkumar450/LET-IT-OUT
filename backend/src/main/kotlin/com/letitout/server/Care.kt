package com.letitout.server

import java.text.Normalizer

// Prototype phrase list. It only decides when the companion gets the gentler extra-care guidance and the app shows the
// support card; it is not a clinical screening tool. Patterns are first-person and self-directed so everyday idioms
// ("my parents will kill me", "this traffic is killing me", "dying to see it") do not trigger it.

private fun normalizeForCare(text: String): String =
    Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
        .replace("\u093C", "")
        .replace(Regex("[\u2018\u2019\u02BC'`]"), "")
        .replace(Regex("[.!?\u0964\n]+"), " . ")
        .replace(Regex("[^\\p{L}\\p{M}\\p{N}.\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

private val benignSpans = listOf(
    "\\b(die|dying|died|dead) (of|laughing)\\b",
    "\\b(kill|killed|killing) myself laughing\\b",
    "\\b(never|wont|will not|would not|wouldnt|not going to|not gonna) (ever )?(hurt|harm|kill|cut) myself\\b",
    "\\bsuicide squad\\b",
    "\\b(accidentally|by mistake) (cut|hurt|burnt|burned|injured) myself\\b",
    "\\b(cut|hurt|burnt|burned|injured) myself (by accident|accidentally|playing|shaving|cooking|chopping|lifting|running|" +
        "working out|exercising|at the gym|at gym|in the gym|in the kitchen|in (a|the) (match|game)|" +
        "during (the |a )?(match|game|practice|workout)|while (cooking|shaving|chopping|playing|lifting|running|working out|exercising)|" +
        "on (a|the) (knife|glass|stairs|door))\\b"
).map(::Regex)

private val carePatterns = listOf(
    "\\b(kill|killing|killed|hurt|hurting|harm|harming|cut|cutting|hang|hanging|end|ending) myself\\b",
    "\\b(want|wanna|wanted) (to )?die\\b",
    "\\bwish i (was|were) dead\\b",
    "\\bwish i could (just )?die\\b",
    "\\bwish i (was|were|had) never (been )?born\\b",
    "\\bi deserve to die\\b",
    "\\b(dont|do not|didnt|no longer) (want|wanna) (to )?(exist|be alive)\\b",
    "\\b(dont|do not|no longer) (want|wanna) (to )?(live|be here|go on|wake up|continue living) ?(anymore|any more)\\b",
    "\\b(dont|do not) (want|wanna) (to )?(live like this|wake up again|wake up ever again)\\b",
    "\\b(dont|do not|no longer) (want|wanna) (to )?(live|go on) ?(\\.|$)",
    "\\b(dont feel like living|no will to live|tired of living|tired of being alive|sick of living)\\b",
    "\\bbetter off (without me|dead)\\b",
    "\\bbetter off (if )?i (was|were|am) (dead|gone|not (here|around|alive))\\b",
    "\\bbetter off if i (wasnt|werent|didnt exist|never existed|disappeared)\\b",
    "\\bno reason (left )?to (live|go on|keep going|be alive|exist)\\b",
    "\\bnothing (left )?to live for\\b",
    "\\bno (point|use|sense) (in )?(living|being alive|going on|existing)\\b",
    "\\bwhats the point (of|in) (living|being alive|going on|existing)\\b",
    "\\b(end|ending) (it all|my life)\\b",
    "\\b(take|taking) my (own )?life\\b",
    "\\bsuicid(e|al)\\b",
    "\\bself ?harm(ing)?\\b",
    "\\bunalive\\b",
    "\\b(slit|slitting|cut|cutting) my wrists?\\b",
    "\\b(sleep|go to sleep|fall asleep) and (never|not) wake up\\b",
    "\\bjump(ing)? (off|from) (a|the|this|my) (building|bridge|roof|terrace|balcony|cliff)\\b",
    // Hinglish / Hindi and Telugu in Roman script
    "\\b(jeena|jina) (nahi|nahin|nhi) (chahta|chahti|hai)\\b",
    "\\b(nahi|nahin|nhi) (jeena|jina)\\b",
    "\\bjeene ka (man|mann|mood) (nahi|nahin|nhi)\\b",
    "\\bjeene ki (koi )?(wajah|vajah|ichha|icchha) (nahi|nahin|nhi)\\b",
    "\\b(marna|mar jana|mar jaana|mar jaun|mar jau) (chahta|chahti)\\b",
    "\\bmujhe marna hai\\b",
    "\\bkaash (main )?mar (jaun|jau|jata|jati)\\b",
    "\\bkhud ko (khatam|khtm|maar|mar|hurt|nuksan|nuksaan|chot)\\b",
    "\\bapni jaan (le|lena|lene)\\b",
    "\\bzinda (nahi|nahin|nhi) (rehna|rahna)\\b",
    "\\b(khudkushi|aatmahatya|atmahatya|aatmhatya)\\b",
    "\\b(chanipovalani|chachipovalani|chanipovali|chachipovali)\\b",
    "\\b(bratakalani|brathakalani|batakalani) (ledu|ledhu)\\b"
).map(::Regex)

private val careNativePhrases = listOf(
    "मरना चाहता", "मरना चाहती", "मर जाना चाहता", "मर जाना चाहती", "जीना नहीं चाहता", "जीना नहीं चाहती",
    "जीने का मन नहीं", "खुद को खत्म", "खुद को मार", "आत्महत्या", "खुदकुशी", "अपनी जान ले",
    "చనిపోవాలని", "చచ్చిపోవాలని", "ఆత్మహత్య", "బ్రతకాలని లేదు", "బతకాలని లేదు"
).map(::normalizeForCare)

internal fun needsExtraCare(recentUserMessages: List<String>): Boolean = recentUserMessages.any { message ->
    val text = benignSpans.fold(normalizeForCare(message)) { acc, span -> span.replace(acc, " . ") }
    carePatterns.any { it.containsMatchIn(text) } || careNativePhrases.any { it in text }
}

private val professionalHelp = Regex(
    "\\b(doctors?|counsell?ors?|counsell?ing|therapists?|psychiatrists?|psychologists?)\\b|\\btele[\\s-]?manas\\b|\\b14416\\b",
    RegexOption.IGNORE_CASE
)

internal fun mentionsProfessionalHelp(reply: String): Boolean =
    professionalHelp.containsMatchIn(reply) || listOf("डॉक्टर", "डाक्टर", "డాక్టర్").any { it in reply }
