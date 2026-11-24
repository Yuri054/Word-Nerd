package com.wordnerd.dictionary.data

/**
 * Loanwords that English keeps close to their original pronunciation. The speaker button
 * reads these with the phone's voice for [locale] (using [native] text where the language
 * uses another script) when that voice is installed.
 *
 * Every other word, and these too when the voice isn't installed, is read from its normal
 * spelling by the English voice, which knows most dictionary words. (Feeding it invented
 * respellings made it read them chunk by chunk, so we don't.)
 */
data class Spoken(val locale: String, val native: String? = null)

fun spokenFor(word: Word): Spoken? = SPOKEN[word.id]

/** Keyed by [Word.id] (the lowercase headword). */
internal val SPOKEN: Map<String, Spoken> = mapOf(
    // German
    "schadenfreude" to Spoken("de-DE"),
    "weltschmerz" to Spoken("de-DE"),
    "sehnsucht" to Spoken("de-DE"),
    "gemütlichkeit" to Spoken("de-DE"),
    "torschlusspanik" to Spoken("de-DE"),
    "zeitgeist" to Spoken("de-DE"),
    "weltanschauung" to Spoken("de-DE"),
    "gestalt" to Spoken("de-DE"),
    "fernweh" to Spoken("de-DE"),
    "bildungsroman" to Spoken("de-DE"),
    // French
    "ennui" to Spoken("fr-FR"),
    "joie de vivre" to Spoken("fr-FR"),
    "frisson" to Spoken("fr-FR"),
    "sangfroid" to Spoken("fr-FR"),
    "élan" to Spoken("fr-FR"),
    "bon vivant" to Spoken("fr-FR"),
    "enfant terrible" to Spoken("fr-FR"),
    "éminence grise" to Spoken("fr-FR"),
    "flâneur" to Spoken("fr-FR"),
    "dérive" to Spoken("fr-FR"),
    "mise en place" to Spoken("fr-FR"),
    "mise en abyme" to Spoken("fr-FR"),
    "amuse-bouche" to Spoken("fr-FR"),
    "digestif" to Spoken("fr-FR"),
    "terroir" to Spoken("fr-FR"),
    "contretemps" to Spoken("fr-FR"),
    "esprit de l'escalier" to Spoken("fr-FR"),
    "jamais vu" to Spoken("fr-FR"),
    "folie à deux" to Spoken("fr-FR"),
    "roman à clef" to Spoken("fr-FR"),
    "sillage" to Spoken("fr-FR"),
    // Italian
    "fata morgana" to Spoken("it-IT"),
    "dolce far niente" to Spoken("it-IT"),
    "sotto voce" to Spoken("it-IT"),
    "sprezzatura" to Spoken("it-IT"),
    // Spanish
    "querencia" to Spoken("es-ES"),
    "duende" to Spoken("es-ES"),
    "mañana" to Spoken("es-ES"),
    "sobremesa" to Spoken("es-ES"),
    // Portuguese (Brazil)
    "saudade" to Spoken("pt-BR"),
    // Danish
    "hygge" to Spoken("da-DK"),
    // Swedish
    "lagom" to Spoken("sv-SE"),
    // Finnish
    "sisu" to Spoken("fi-FI"),
    // Russian
    "toska" to Spoken("ru-RU", "тоска"),
    // Greek
    "meraki" to Spoken("el-GR", "μεράκι"),
    // Japanese (native script; a Japanese voice cannot read romaji reliably)
    "komorebi" to Spoken("ja-JP", "木漏れ日"),
    "tsundoku" to Spoken("ja-JP", "積ん読"),
    "wabi-sabi" to Spoken("ja-JP", "侘び寂び"),
    "mono no aware" to Spoken("ja-JP", "もののあわれ"),
    "ikigai" to Spoken("ja-JP", "生きがい"),
    "omakase" to Spoken("ja-JP", "おまかせ"),
)
